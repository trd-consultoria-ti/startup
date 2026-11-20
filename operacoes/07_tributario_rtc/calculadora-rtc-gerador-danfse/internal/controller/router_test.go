package controller

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"danfse-service/internal/domain/enums"
	"danfse-service/internal/service"
)

// mockPdfService implements service.ServicoGeracaoPdf for testing.
type mockPdfService struct {
	pdfData []byte
	err     error
}

func (m *mockPdfService) Executar(_ context.Context, _ []byte, _ enums.SituacaoNotaTipo, _ bool) ([]byte, error) {
	return m.pdfData, m.err
}

func TestRouter_PdfDispatch(t *testing.T) {
	expectedPdf := []byte("%PDF-1.4 mock pdf content")
	mockPdf := &mockPdfService{pdfData: expectedPdf}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller)
	handler := router.Inicializar()

	xmlBody := []byte(`<?xml version="1.0" encoding="UTF-8"?><NFSe><Id></Id></NFSe>`)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(xmlBody))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d; body: %s", rec.Code, rec.Body.String())
	}
	if ct := rec.Header().Get("Content-Type"); ct != "application/pdf" {
		t.Fatalf("expected Content-Type application/pdf, got %q", ct)
	}
	if !bytes.Contains(rec.Body.Bytes(), expectedPdf) {
		t.Fatalf("expected PDF body to contain %q, got %q", expectedPdf, rec.Body.Bytes())
	}
}

func TestRouter_OldVersionedPath404(t *testing.T) {
	mockPdf := &mockPdfService{}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller)
	handler := router.Inicializar()

	xmlBody := []byte(`<?xml version="1.0" encoding="UTF-8"?><NFSe><Id></Id></NFSe>`)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse/v1/pdf", bytes.NewReader(xmlBody))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected status 404, got %d; body: %s", rec.Code, rec.Body.String())
	}
}

func TestRouter_UnknownRoute404(t *testing.T) {
	mockPdf := &mockPdfService{}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller)
	handler := router.Inicializar()

	xmlBody := []byte(`<?xml version="1.0" encoding="UTF-8"?><NFSe><Id></Id></NFSe>`)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse/v1/html", bytes.NewReader(xmlBody))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected status 404, got %d; body: %s", rec.Code, rec.Body.String())
	}
}

func TestRouter_LegacyAlias404(t *testing.T) {
	mockPdf := &mockPdfService{}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller)
	handler := router.Inicializar()

	xmlBody := []byte(`<?xml version="1.0" encoding="UTF-8"?><NFSe><Id></Id></NFSe>`)
	req := httptest.NewRequest(http.MethodPost, "/danfse-service/v1/pdf", bytes.NewReader(xmlBody))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected status 404, got %d; body: %s", rec.Code, rec.Body.String())
	}
}

func TestRouter_SwaggerUIAvailable(t *testing.T) {
	mockPdf := &mockPdfService{}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller)
	handler := router.Inicializar()

	// /api/swagger-ui redirects to /api/swagger-ui/index.html
	req := httptest.NewRequest(http.MethodGet, "/api/swagger-ui", nil)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)
	if rec.Code != http.StatusMovedPermanently {
		t.Fatalf("expected status 301, got %d; body: %s", rec.Code, rec.Body.String())
	}

	// /api/swagger-ui/ also redirects to /api/swagger-ui/index.html
	req3 := httptest.NewRequest(http.MethodGet, "/api/swagger-ui/", nil)
	rec3 := httptest.NewRecorder()
	handler.ServeHTTP(rec3, req3)
	if rec3.Code != http.StatusMovedPermanently {
		t.Fatalf("expected status 301, got %d; body: %s", rec3.Code, rec3.Body.String())
	}

	// /api/swagger-ui/index.html serves the Swagger UI
	req2 := httptest.NewRequest(http.MethodGet, "/api/swagger-ui/index.html", nil)
	rec2 := httptest.NewRecorder()
	handler.ServeHTTP(rec2, req2)
	if rec2.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d; body: %s", rec2.Code, rec2.Body.String())
	}
}

func TestRouter_BodyOverMaxXMLInputSizeRejected(t *testing.T) {
	mockPdf := &mockPdfService{pdfData: []byte("%PDF-1.4 mock")}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller)
	handler := router.Inicializar()

	// Body larger than service.MaxXMLInputSize
	largeBody := make([]byte, service.MaxXMLInputSize+1)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(largeBody))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code == http.StatusOK {
		t.Fatalf("expected non-200 status for oversized body, got %d; body: %s", rec.Code, rec.Body.String())
	}
}

func TestRouter_CustomBodyLimitContentLength(t *testing.T) {
	mockPdf := &mockPdfService{pdfData: []byte("%PDF-1.4 mock")}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller, 100)
	handler := router.Inicializar()

	// Abaixo do limite customizado: a requisição chega ao serviço mock (200).
	smallBody := bytes.Repeat([]byte("a"), 99)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(smallBody))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("esperava 200 para corpo abaixo do limite customizado, obteve %d; body: %s", rec.Code, rec.Body.String())
	}

	// Acima do limite customizado, ainda abaixo do default (5 MiB):
	// o BodyLimit via Content-Length deve responder 413.
	largeBody := bytes.Repeat([]byte("a"), 101)
	req2 := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(largeBody))
	req2.Header.Set("Content-Type", "application/xml")
	rec2 := httptest.NewRecorder()
	handler.ServeHTTP(rec2, req2)
	if rec2.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("esperava 413 para corpo acima do limite customizado, obteve %d; body: %s", rec2.Code, rec2.Body.String())
	}
}

func TestRouter_CustomBodyLimitChunked(t *testing.T) {
	mockPdf := &mockPdfService{}

	controller := NewDanfseController(mockPdf)
	router := NewRouter(controller, 100)
	handler := router.Inicializar()

	// Simula requisição sem Content-Length (Transfer-Encoding: chunked), forçando
	// o BodyLimit a contar o corpo durante a leitura em vez do cabeçalho.
	largeBody := bytes.Repeat([]byte("b"), 5000)
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(largeBody))
	req.Header.Set("Content-Type", "application/xml")
	req.ContentLength = -1
	req.TransferEncoding = []string{"chunked"}
	rec := httptest.NewRecorder()

	handler.ServeHTTP(rec, req)

	if rec.Code == http.StatusOK {
		t.Fatalf("esperava status != 200 para corpo chunked acima do limite customizado, obteve 200; body: %s", rec.Body.String())
	}
}
