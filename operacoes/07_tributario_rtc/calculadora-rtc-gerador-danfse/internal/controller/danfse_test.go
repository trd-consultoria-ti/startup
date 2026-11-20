package controller

import (
	"context"
	"danfse-service/internal/domain/enums"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"danfse-service/internal/service"
	"github.com/labstack/echo/v5"
)

type mockGerarPdf struct {
	conteudo []byte
	err      error
}

func (m *mockGerarPdf) Executar(_ context.Context, _ []byte, _ enums.SituacaoNotaTipo, _ bool) ([]byte, error) {
	return m.conteudo, m.err
}

func novoContexto(url string, body string) (*echo.Context, *httptest.ResponseRecorder) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, url, strings.NewReader(body))
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	return c, rec
}

func TestDanfseController_GerarPdfSucesso(t *testing.T) {
	pdf := []byte("%PDF-fake")
	ctrl := NewDanfseController(&mockGerarPdf{conteudo: pdf})

	c, rec := novoContexto("/api/danfse", "<xml>ok</xml>")

	if err := ctrl.GerarPdf(c); err != nil {
		t.Fatalf("esperava sucesso, obteve erro: %v", err)
	}

	if rec.Code != http.StatusOK {
		t.Fatalf("esperava status 200, obteve %d", rec.Code)
	}
	if ct := rec.Header().Get(echo.HeaderContentType); ct != "application/pdf" {
		t.Fatalf("esperava content-type application/pdf, obteve %q", ct)
	}
	if cd := rec.Header().Get(echo.HeaderContentDisposition); cd != `attachment; filename="danfse.pdf"` {
		t.Fatalf("content-disposition inesperado: %q", cd)
	}
	if rec.Body.String() != string(pdf) {
		t.Fatalf("corpo da resposta diferente do PDF gerado")
	}
}

func TestDanfseController_GerarPdfXmlVazio(t *testing.T) {
	ctrl := NewDanfseController(&mockGerarPdf{})

	c, _ := novoContexto("/api/danfse", "   ")

	err := ctrl.GerarPdf(c)

	var httpErr *echo.HTTPError
	if !errors.As(err, &httpErr) || httpErr.Code != http.StatusBadRequest {
		t.Fatalf("esperava HTTPError 400, obteve: %v", err)
	}
	if httpErr.Message != "Xml não informado." {
		t.Fatalf("mensagem inesperada: %v", httpErr.Message)
	}
}

func TestDanfseController_GerarPdfErroNoUseCase(t *testing.T) {
	ctrl := NewDanfseController(&mockGerarPdf{err: errors.New("falha no template")})

	c, _ := novoContexto("/api/danfse", "<xml>ok</xml>")

	err := ctrl.GerarPdf(c)

	var httpErr *echo.HTTPError
	if !errors.As(err, &httpErr) || httpErr.Code != http.StatusBadRequest {
		t.Fatalf("esperava HTTPError 400, obteve: %v", err)
	}
	if httpErr.Message != "falha no template" {
		t.Fatalf("mensagem inesperada: %v", httpErr.Message)
	}
}

func TestDanfseController_GerarPdfSituacaoInvalida(t *testing.T) {
	ctrl := NewDanfseController(&mockGerarPdf{})
	c, _ := novoContexto("/api/danfse?stc=3", "<xml>ok</xml>")

	err := ctrl.GerarPdf(c)
	var httpErr *echo.HTTPError
	if !errors.As(err, &httpErr) || httpErr.Code != http.StatusBadRequest {
		t.Fatalf("esperava HTTPError 400, obteve: %v", err)
	}
}

func TestDanfseController_ResolverSituacao(t *testing.T) {
	casos := []struct {
		nome     string
		query    string
		esperado enums.SituacaoNotaTipo
	}{
		{"sem parâmetros", "", enums.VALIDA},
		{"stc=1 substituída", "?stc=1", enums.SUBSTITUIDA},
		{"stc=2 cancelada", "?stc=2", enums.CANCELADA},
		{"stc=0 válido", "?stc=0", enums.VALIDA},
	}

	for _, tc := range casos {
		t.Run(tc.nome, func(t *testing.T) {
			e := echo.New()
			req := httptest.NewRequest(http.MethodPost, "/"+tc.query, nil)
			c := e.NewContext(req, httptest.NewRecorder())

			if got, err := resolverSituacao(c); err != nil || got != tc.esperado {
				t.Fatalf("esperava situação %q, obteve %q (erro: %v)", tc.esperado, got, err)
			}
		})
	}
}

func TestDanfseController_ResolverSituacaoRejeitaValorInvalido(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/?stc=3", nil)
	c := e.NewContext(req, httptest.NewRecorder())

	if _, err := resolverSituacao(c); err == nil {
		t.Fatal("esperava erro para stc=3")
	}
}

func TestDanfseController_ResolverCanhoto(t *testing.T) {
	casos := []struct {
		nome     string
		query    string
		esperado bool
		erro     bool
	}{
		{"ausente", "", true, false},
		{"true", "?canhoto=true", true, false},
		{"false", "?canhoto=false", false, false},
		{"1 inválido", "?canhoto=1", true, true},
		{"sim inválido", "?canhoto=sim", true, true},
	}

	for _, tc := range casos {
		t.Run(tc.nome, func(t *testing.T) {
			e := echo.New()
			req := httptest.NewRequest(http.MethodPost, "/"+tc.query, nil)
			c := e.NewContext(req, httptest.NewRecorder())

			got, err := resolverCanhoto(c)
			if tc.erro {
				if err == nil {
					t.Fatal("esperava erro, obteve nil")
				}
				return
			}
			if err != nil {
				t.Fatalf("esperava sucesso, obteve erro: %v", err)
			}
			if got != tc.esperado {
				t.Fatalf("esperava %v, obteve %v", tc.esperado, got)
			}
		})
	}
}

func TestDanfseController_GerarPdfCanhotoAusente(t *testing.T) {
	pdf := []byte("%PDF-fake")
	ctrl := NewDanfseController(&mockGerarPdf{conteudo: pdf})

	c, rec := novoContexto("/api/danfse", "<xml>ok</xml>")

	if err := ctrl.GerarPdf(c); err != nil {
		t.Fatalf("esperava sucesso, obteve erro: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("esperava status 200, obteve %d", rec.Code)
	}
}

func TestDanfseController_GerarPdfCanhotoFalse(t *testing.T) {
	pdf := []byte("%PDF-fake")
	ctrl := NewDanfseController(&mockGerarPdf{conteudo: pdf})

	c, rec := novoContexto("/api/danfse?canhoto=false", "<xml>ok</xml>")

	if err := ctrl.GerarPdf(c); err != nil {
		t.Fatalf("esperava sucesso, obteve erro: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("esperava status 200, obteve %d", rec.Code)
	}
}

func TestDanfseController_GerarPdfCanhotoInvalido(t *testing.T) {
	ctrl := NewDanfseController(&mockGerarPdf{})

	c, _ := novoContexto("/api/danfse?canhoto=1", "<xml>ok</xml>")

	err := ctrl.GerarPdf(c)

	var httpErr *echo.HTTPError
	if !errors.As(err, &httpErr) || httpErr.Code != http.StatusBadRequest {
		t.Fatalf("esperava HTTPError 400, obteve: %v", err)
	}
	if !strings.Contains(httpErr.Message, "canhoto") {
		t.Fatalf("mensagem deveria mencionar canhoto: %v", httpErr.Message)
	}
}

// Ensure mockPdfService implements service.ServicoGeracaoPdf.
var _ service.ServicoGeracaoPdf = (*mockPdfService)(nil)
