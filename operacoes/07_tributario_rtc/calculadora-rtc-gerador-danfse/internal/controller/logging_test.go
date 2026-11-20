package controller

import (
	"bytes"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v5"
	echo_middleware "github.com/labstack/echo/v5/middleware"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
)

func parseLogLines(t *testing.T, buf *bytes.Buffer) []map[string]any {
	t.Helper()
	raw := buf.String()
	if strings.TrimSpace(raw) == "" {
		t.Fatalf("nenhuma linha de log emitida (buffer vazio)")
	}
	var lines []map[string]any
	for _, line := range strings.Split(strings.TrimSpace(raw), "\n") {
		var m map[string]any
		if err := json.Unmarshal([]byte(line), &m); err != nil {
			t.Fatalf("linha de log inválida %q: %v", line, err)
		}
		lines = append(lines, m)
	}
	return lines
}

// completionLines retorna apenas as linhas de conclusão de request (separa de
// qualquer ruído eventual do framework no mesmo buffer).
func completionLines(t *testing.T, buf *bytes.Buffer) []map[string]any {
	t.Helper()
	var out []map[string]any
	for _, line := range parseLogLines(t, buf) {
		if line["event"] == "request_completed" {
			out = append(out, line)
		}
	}
	return out
}

// newEchoWithLogging monta um Echo com a mesma pilha de observabilidade do
// router real: request ID SEMPRE gerado pelo servidor + RequestLogger nativo.
func newEchoWithLogging(buf *bytes.Buffer) *echo.Echo {
	e := echo.New()
	e.Use(stripExternalRequestIDHeader)
	e.Use(echo_middleware.RequestID())
	e.Use(LogRequests(slog.New(slog.NewJSONHandler(buf, &slog.HandlerOptions{Level: slog.LevelDebug}))))
	e.Use(echo_middleware.Recover())
	return e
}

func serve(e *echo.Echo, req *http.Request) *httptest.ResponseRecorder {
	rec := httptest.NewRecorder()
	e.ServeHTTP(rec, req)
	return rec
}

func TestLogRequests_CompletionLineSucesso(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.GET("/api/teste", func(c *echo.Context) error { return c.String(http.StatusOK, "ok") })

	req := httptest.NewRequest(http.MethodGet, "/api/teste", nil)
	rec := serve(e, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, esperava 200", rec.Code)
	}
	if rid := rec.Header().Get(echo.HeaderXRequestID); rid == "" {
		t.Fatalf("cabeçalho %s ausente na resposta", echo.HeaderXRequestID)
	}

	lines := completionLines(t, &buf)
	if len(lines) != 1 {
		t.Fatalf("esperava UMA linha de conclusão, obteve %d: %v", len(lines), lines)
	}
	line := lines[0]
	if line["event"] != "request_completed" {
		t.Fatalf("event inesperado: %v", line["event"])
	}
	if line["method"] != "GET" {
		t.Fatalf("method inesperado: %v", line["method"])
	}
	if line["route"] != "/api/teste" {
		t.Fatalf("route inesperada: %v", line["route"])
	}
	if float64(line["status"].(float64)) != 200 {
		t.Fatalf("status inesperado: %v", line["status"])
	}
	if line["request_id"] != rec.Header().Get(echo.HeaderXRequestID) {
		t.Fatalf("request_id no log (%v) difere do cabeçalho (%q)", line["request_id"], rec.Header().Get(echo.HeaderXRequestID))
	}
	if _, ok := line["duration"]; !ok {
		t.Fatal("duração ausente no log")
	}
	if _, ok := line["timestamp"]; !ok {
		t.Fatalf("timestamp ausente (linha: %v)", line)
	}
}

func TestLogRequests_RotaSemMatchFallback(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.GET("/api/teste", func(c *echo.Context) error { return c.String(http.StatusOK, "ok") })

	req := httptest.NewRequest(http.MethodPost, "/rota/inexistente", nil)
	rec := serve(e, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, esperava 404", rec.Code)
	}
	lines := completionLines(t, &buf)
	line := lines[len(lines)-1]
	if line["route"] != "unmatched" {
		t.Fatalf("route deveria ser o fallback constant \"unmatched\", obteve %v", line["route"])
	}
	if float64(line["status"].(float64)) != 404 {
		t.Fatalf("status inesperado no log: %v", line["status"])
	}
	if line["error_type"] != "http_error" {
		t.Fatalf("error_type inesperado: %v", line["error_type"])
	}
}

func TestLogRequests_RequestIDSempreGeradoPeloServidor(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.GET("/api/teste", func(c *echo.Context) error { return c.String(http.StatusOK, "ok") })

	casos := []string{
		"",                       // sem header
		"meu-id-externo-42",      // externo válido
		"<xml>../segredo</xml>",  // externo inválido
		strings.Repeat("a", 200), // externo gigante
	}
	for _, externo := range casos {
		req := httptest.NewRequest(http.MethodGet, "/api/teste", nil)
		if externo != "" {
			req.Header.Set(echo.HeaderXRequestID, externo)
		}
		rec := serve(e, req)

		got := rec.Header().Get(echo.HeaderXRequestID)
		if got == "" || got == externo {
			t.Fatalf("request ID deveria ser SEMPRE novo (ignorando cliente %q), obteve %q", externo, got)
		}
		// Log usa o ID gerado pelo servidor (cabeçalho da resposta).
		lines := completionLines(t, &buf)
		if len(lines) != 1 {
			t.Fatalf("esperava 1 request_completed, obteve %d: %v", len(lines), lines)
		}
		if lines[0]["request_id"] != got {
			t.Fatalf("request_id no log (%v) difere do cabeçalho de resposta (%q)", lines[0]["request_id"], got)
		}
		buf.Reset()
	}
}

func TestLogRequests_ErroHandlerAntesLog500Generico(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.GET("/api/boom", func(c *echo.Context) error {
		return echo.NewHTTPError(http.StatusInternalServerError, "segredo interno <NFSe><Numero>1</Numero></NFSe>")
	})

	req := httptest.NewRequest(http.MethodGet, "/api/boom", nil)
	rec := serve(e, req)

	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, esperava 500", rec.Code)
	}
	lines := completionLines(t, &buf)
	line := lines[len(lines)-1]
	if float64(line["status"].(float64)) != 500 {
		t.Fatalf("status esperado no log = 500, obteve %v", line["status"])
	}
	if line["error_type"] != "http_error" {
		t.Fatalf("error_type inesperado: %v", line["error_type"])
	}
	// Redação: mensagem crua com XML não pode vazar no log.
	if strings.Contains(buf.String(), "segredo interno") || strings.Contains(buf.String(), "NFSe") {
		t.Fatalf("log vazou conteúdo sensível: %s", buf.String())
	}
}

func TestLogRequests_PanicRecuperadoSemBody(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.GET("/api/panic", func(c *echo.Context) error {
		panic("pânico com dados fiscais <CNPJ>123</CNPJ>")
	})

	req := httptest.NewRequest(http.MethodGet, "/api/panic", nil)
	rec := serve(e, req)

	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, esperava 500 recuperado", rec.Code)
	}
	lines := completionLines(t, &buf)
	line := lines[len(lines)-1]
	if line["error_type"] != "panic" {
		t.Fatalf("error_type deveria ser panic, obteve %v", line["error_type"])
	}
	if float64(line["status"].(float64)) != 500 {
		t.Fatalf("status inesperado: %v", line["status"])
	}
	// Recuperação não vaza body/dados sensíveis (o erro do Recover nativo
	// carrega o valor do panic + stack; nada disso vai para o log).
	if strings.Contains(buf.String(), "CNPJ") || strings.Contains(buf.String(), "123</") {
		t.Fatalf("log de panic vazou dados sensíveis: %s", buf.String())
	}
}

func TestLogRequests_RedacaoNuncaLogaBodyOuQuery(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.POST("/api/danfse", func(c *echo.Context) error {
		body, _ := io.ReadAll(c.Request().Body)
		return c.String(http.StatusOK, string(body))
	})

	xmlBody := `<NFSe><CNPJ>12345678000199</CNPJ><Numero>557</Numero></NFSe>`
	req := httptest.NewRequest(http.MethodPost, "/api/danfse?stc=1&chaveAc=muito-secreta", strings.NewReader(xmlBody))
	rec := serve(e, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, esperava 200", rec.Code)
	}
	for _, token := range []string{"NFSe", "CNPJ", "12345678000199", "557", "stc=", "muito-secreta"} {
		if strings.Contains(buf.String(), token) {
			t.Fatalf("log vazou token sensível %q: %s", token, buf.String())
		}
	}
}

func TestLogRequests_TraceESpanIDPresentes(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)

	// Rota que introduz um span real no contexto (simula middleware OTel interno).
	e.Use(func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c *echo.Context) error {
			req := c.Request()
			tr := sdktrace.NewTracerProvider()
			ctx, span := tr.Tracer("test").Start(req.Context(), "span.teste")
			defer span.End()
			c.SetRequest(req.WithContext(ctx))
			return next(c)
		}
	})
	e.GET("/api/teste", func(c *echo.Context) error { return c.String(http.StatusOK, "ok") })

	req := httptest.NewRequest(http.MethodGet, "/api/teste", nil)
	if rec := serve(e, req); rec.Code != http.StatusOK {
		t.Fatalf("status = %d, esperava 200", rec.Code)
	}

	lines := completionLines(t, &buf)
	line := lines[len(lines)-1]
	traceID, _ := line["trace_id"].(string)
	spanID, _ := line["span_id"].(string)
	if traceID == "" || spanID == "" {
		t.Fatalf("esperava trace_id/span_id preenchidos no log, obteve: %v", line)
	}
}

func TestLogRequests_ProbeMarcadoComKind(t *testing.T) {
	var buf bytes.Buffer
	e := newEchoWithLogging(&buf)
	e.GET(HealthLivePath, func(c *echo.Context) error { return c.NoContent(http.StatusOK) })

	req := httptest.NewRequest(http.MethodGet, HealthLivePath, nil)
	if rec := serve(e, req); rec.Code != http.StatusOK {
		t.Fatalf("status = %d, esperava 200", rec.Code)
	}
	lines := completionLines(t, &buf)
	line := lines[len(lines)-1]
	if line["kind"] != "probe" {
		t.Fatalf("esperava kind=probe para health check, obteve: %v", line["kind"])
	}
}

func TestParseLevel(t *testing.T) {
	casos := []struct {
		nome  string
		level string
		want  slog.Level
	}{
		{"vazio cai para info", "", slog.LevelInfo},
		{"info", "info", slog.LevelInfo},
		{"INFO maiúsculo", "INFO", slog.LevelInfo},
		{"debug", "debug", slog.LevelDebug},
		{"warn", "warn", slog.LevelWarn},
		{"error", "error", slog.LevelError},
		{"desconhecido cai para info", "verbose", slog.LevelInfo},
	}
	for _, tc := range casos {
		t.Run(tc.nome, func(t *testing.T) {
			if got := ParseLevel(tc.level); got != tc.want {
				t.Fatalf("ParseLevel(%q) = %v, esperava %v", tc.level, got, tc.want)
			}
		})
	}
}

func TestIsValidLevel(t *testing.T) {
	for _, ok := range []string{"debug", "info", "warn", "error", "INFO", " WARN "} {
		if !IsValidLevel(ok) {
			t.Fatalf("esperava %q válido", ok)
		}
	}
	for _, bad := range []string{"", "verbose", "2"} {
		if IsValidLevel(bad) {
			t.Fatalf("esperava %q inválido", bad)
		}
	}
}

func TestDiscardLoggerESilencioso(t *testing.T) {
	discard := DiscardLogger()
	if discard == nil {
		t.Fatal("DiscardLogger não deveria retornar nil")
	}
	discard.Info("não deve aparecer em nenhum lugar", "k", "v")
}
