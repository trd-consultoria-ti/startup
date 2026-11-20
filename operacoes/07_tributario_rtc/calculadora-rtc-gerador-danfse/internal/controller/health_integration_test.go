package controller

import (
	"bytes"
	"encoding/json"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v5"
)

func TestHealthLive_200(t *testing.T) {
	handler := NewRouterWithOptions(NewDanfseController(&mockPdfService{})).Inicializar()

	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, HealthLivePath, nil))

	if rec.Code != http.StatusOK {
		t.Fatalf("esperava 200 em %s, obteve %d; body: %s", HealthLivePath, rec.Code, rec.Body.String())
	}
	// Corpo consistente com o contrato documentado no Swagger: {"status":"ok"}.
	var st HealthStatus
	if err := json.Unmarshal(rec.Body.Bytes(), &st); err != nil || st.Status != "ok" {
		t.Fatalf("esperava body {\"status\":\"ok\"}, obteve %q; err=%v", rec.Body.String(), err)
	}
}

func TestHealthReady_200QuandoPronto(t *testing.T) {
	readiness := NewReadiness(true)
	handler := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{}),
		WithReadiness(readiness),
	).Inicializar()

	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, HealthReadyPath, nil))

	if rec.Code != http.StatusOK {
		t.Fatalf("esperava 200 (pronto), obteve %d; body: %s", rec.Code, rec.Body.String())
	}
	// Corpo consistente com o contrato documentado no Swagger: {"status":"ok"}.
	var st HealthStatus
	if err := json.Unmarshal(rec.Body.Bytes(), &st); err != nil || st.Status != "ok" {
		t.Fatalf("esperava body {\"status\":\"ok\"}, obteve %q; err=%v", rec.Body.String(), err)
	}
}

func TestHealthReady_503DuranteDraining(t *testing.T) {
	readiness := NewReadiness(true)
	handler := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{}),
		WithReadiness(readiness),
	).Inicializar()

	// Simula o início do shutdown: marcado como não-pronto ANTES do Shutdown.
	readiness.SetReady(false)

	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, HealthReadyPath, nil))

	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("esperava 503 (draining), obteve %d; body: %s", rec.Code, rec.Body.String())
	}
	// Corpo consistente com o contrato documentado no Swagger: {"status":"unavailable"}.
	var st HealthStatus
	if err := json.Unmarshal(rec.Body.Bytes(), &st); err != nil || st.Status != "unavailable" {
		t.Fatalf("esperava body {\"status\":\"unavailable\"}, obteve %q; err=%v", rec.Body.String(), err)
	}
}

func TestRouter_RequestIDSempreGeradoPeloServidor(t *testing.T) {
	handler := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{}),
		WithLogger(DiscardLogger()),
	).Inicializar()

	// Qualquer X-Request-ID do cliente é descartado: o servidor gera um novo
	// identificador (nunca ecoa dados de correlação não confiáveis).
	for _, externo := range []string{"", "correlacao-externa-1", "<xml>../segredo</xml>"} {
		req := httptest.NewRequest(http.MethodGet, HealthLivePath, nil)
		if externo != "" {
			req.Header.Set(echo.HeaderXRequestID, externo)
		}
		rec := httptest.NewRecorder()
		handler.ServeHTTP(rec, req)

		got := rec.Header().Get(echo.HeaderXRequestID)
		if got == "" || got == externo {
			t.Fatalf("esperava request ID novo (ignorando cliente %q), obteve %q", externo, got)
		}
	}
}

func TestRouter_OptionsPreflight204(t *testing.T) {
	handler := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{}),
		WithLogger(DiscardLogger()),
	).Inicializar()

	// Preflight CORS: OPTIONS com Origin + Access-Control-Request-Method.
	req := httptest.NewRequest(http.MethodOptions, "/api/danfse", nil)
	req.Header.Set("Origin", "http://example.com")
	req.Header.Set("Access-Control-Request-Method", http.MethodPost)
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Fatalf("esperava 204 para OPTIONS preflight, obteve %d; body: %s", rec.Code, rec.Body.String())
	}
	if rec.Header().Get("Access-Control-Allow-Origin") != "*" {
		t.Fatalf("esperava Access-Control-Allow-Origin=*, obteve %q", rec.Header().Get("Access-Control-Allow-Origin"))
	}
}

func TestRouter_LogCompletionUnicoComCampos(t *testing.T) {
	var buf bytes.Buffer
	logger := slog.New(slog.NewJSONHandler(&buf, &slog.HandlerOptions{Level: slog.LevelDebug}))

	handler := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{pdfData: []byte("%PDF-1.4 mock")}),
		WithLogger(logger),
	).Inicializar()

	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader([]byte(`<NFSe/>`)))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("esperava 200, obteve %d; body: %s", rec.Code, rec.Body.String())
	}

	// Exatamente UMA linha de conclusão (sem "started"/"completed" separados).
	nonEmpty := nonEmptyNonErrorLines(&buf)
	var completion []map[string]any
	for _, line := range nonEmpty {
		var m map[string]any
		if err := json.Unmarshal([]byte(line), &m); err != nil {
			continue
		}
		if m["event"] == "request_completed" {
			completion = append(completion, m)
		}
	}
	if len(completion) != 1 {
		t.Fatalf("esperava exatamente 1 request_completed, obteve %d: %s", len(completion), buf.String())
	}
	line := completion[0]
	if line["method"] != "POST" || line["route"] != "/api/danfse" {
		t.Fatalf("campos inesperados no completion: %v", line)
	}
	if v, _ := line["status"].(float64); int(v) != http.StatusOK {
		t.Fatalf("status inesperado no completion: %v", line["status"])
	}
	if line["request_id"] == nil || line["request_id"] == "" {
		t.Fatalf("request_id ausente no completion: %v", line)
	}
}

func TestRouter_Erro500GenericoSemVazamento(t *testing.T) {
	var buf bytes.Buffer
	logger := slog.New(slog.NewJSONHandler(&buf, &slog.HandlerOptions{Level: slog.LevelDebug}))

	echoInst := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{}),
		WithLogger(logger),
	).Inicializar().(*echo.Echo)
	echoInst.GET("/api/teste-erro", func(c *echo.Context) error {
		return echo.NewHTTPError(http.StatusInternalServerError, "segredo interno <CNPJ>99</CNPJ>")
	})

	rec := httptest.NewRecorder()
	echoInst.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/api/teste-erro", nil))

	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("esperava 500, obteve %d", rec.Code)
	}
	if strings.Contains(rec.Body.String(), "segredo interno") {
		t.Fatalf("corpo da resposta 500 vazou err.Error(): %s", rec.Body.String())
	}
	var resp ErroResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &resp); err == nil {
		if resp.Mensagem != http.StatusText(http.StatusInternalServerError) {
			t.Fatalf("mensagem 500 deveria ser genérica %q, obteve %q", http.StatusText(http.StatusInternalServerError), resp.Mensagem)
		}
	} else {
		t.Fatalf("esperava corpo JSON válido de erro, obteve: %s", rec.Body.String())
	}
	// Redação no log também.
	if strings.Contains(buf.String(), "segredo interno") || strings.Contains(buf.String(), "CNPJ") {
		t.Fatalf("log vazou conteúdo sensível: %s", buf.String())
	}
}

func TestRouter_PanicRecuperado500Generico(t *testing.T) {
	var buf bytes.Buffer
	logger := slog.New(slog.NewJSONHandler(&buf, &slog.HandlerOptions{Level: slog.LevelDebug}))

	echoInst := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{}),
		WithLogger(logger),
	).Inicializar().(*echo.Echo)
	echoInst.GET("/api/teste-panic", func(c *echo.Context) error {
		panic("config interno exposta")
	})

	rec := httptest.NewRecorder()
	echoInst.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/api/teste-panic", nil))

	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("esperava 500 recuperado, obteve %d; body: %s", rec.Code, rec.Body.String())
	}
	// Mensagem de pânico não pode vazar nem no corpo nem no log.
	if strings.Contains(rec.Body.String(), "config interno exposta") {
		t.Fatalf("corpo da resposta vazou pânico: %s", rec.Body.String())
	}
	if strings.Contains(buf.String(), "config interno exposta") {
		t.Fatalf("log vazou pânico: %s", buf.String())
	}
}

func TestRouter_Resposta400STCInvalidoStavel(t *testing.T) {
	handler := NewRouterWithOptions(
		NewDanfseController(&mockPdfService{pdfData: []byte("%PDF-1.4 mock")}),
	).Inicializar()

	// stc inválido → 400 com mensagem voltada ao cliente (não um 500).
	req := httptest.NewRequest(http.MethodPost, "/api/danfse?stc=abc", bytes.NewReader([]byte(`<NFSe/>`)))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("esperava 400 para stc inválido, obteve %d; body: %s", rec.Code, rec.Body.String())
	}
	var resp ErroResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &resp); err != nil {
		t.Fatalf("esperava corpo JSON de erro, obteve: %s", rec.Body.String())
	}
	if resp.Mensagem == "" || strings.Contains(resp.Mensagem, "NFSe") {
		t.Fatalf("mensagem 400 deveria ser estável e sem conteúdo sensível, obteve %q", resp.Mensagem)
	}
}

// nonEmptyNonErrorLines filtra linhas vazias e saídas não-log de nível alto
// (o echo pode emitir logs de inicialização/erro) mantendo apenas json válido.
func nonEmptyNonErrorLines(buf *bytes.Buffer) []string {
	raw := strings.TrimSpace(buf.String())
	if raw == "" {
		return nil
	}
	var lines []string
	for _, line := range strings.Split(raw, "\n") {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		// Ignora ruído textual de bibliotecas (não-JSON).
		if !strings.HasPrefix(line, "{") {
			continue
		}
		lines = append(lines, line)
	}
	return lines
}
