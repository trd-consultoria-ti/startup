package app

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"danfse-service/internal/domain/enums"
	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
	"danfse-service/internal/service"
)

type applicationTestRenderer struct{}

func (applicationTestRenderer) Renderizar(context.Context, presentation.DocumentoDanfse) ([]byte, error) {
	return []byte("%PDF-test"), nil
}

type applicationTestPresenter struct{}

func (applicationTestPresenter) Apresentar(models.NFSe) presentation.DocumentoDanfse {
	return presentation.DocumentoDanfse{}
}

func TestNewPdfServiceWithDependencies(t *testing.T) {
	resolver := func(int) (string, string, bool, error) { return "", "", false, nil }

	got := NewPdfServiceWithDependencies(resolver, applicationTestRenderer{}, applicationTestPresenter{})
	if got == nil {
		t.Fatal("NewPdfServiceWithDependencies retornou nil")
	}

}

func TestNewPdfServiceWithDependenciesCustomLimit(t *testing.T) {
	resolver := func(int) (string, string, bool, error) { return "", "", false, nil }

	// Valor customizado deve chegar ao preparador: entrada acima do limite
	// repassado é rejeitada antes da apresentação/renderização.
	got := NewPdfServiceWithDependencies(resolver, applicationTestRenderer{}, applicationTestPresenter{}, 8)
	if got == nil {
		t.Fatal("NewPdfServiceWithDependencies retornou nil")
	}

	_, err := got.Executar(context.Background(), []byte("1234567890"), enums.VALIDA, true)
	if err == nil {
		t.Fatal("esperava erro para entrada acima do limite customizado repassado ao preparador")
	}
}

func TestNewHandlerCustomBodyLimit(t *testing.T) {
	handler, err := NewHandler(64)
	if err != nil {
		t.Fatalf("NewHandler falhou: %v", err)
	}

	// O valor escolhido deve chegar ao BodyLimit do router (64 bytes).
	body := bytes.Repeat([]byte("x"), 65)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("esperava 413 para corpo acima do limite customizado, obteve %d; body: %s", rec.Code, rec.Body.String())
	}
}

var _ service.RenderizadorPdfDireto = applicationTestRenderer{}
var _ service.ApresentadorDanfse = applicationTestPresenter{}
