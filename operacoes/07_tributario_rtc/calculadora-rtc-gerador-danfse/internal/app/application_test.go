package app

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"testing"
)

// TestNewHandlerWithOptions_AplicaMaxInputSizeNoBodyLimit garante que o limite
// configurado (XML_MAX_SIZE_BYTES) repassado via WithMaxInputSize chega ao
// BodyLimit do router: corpo acima do limite customizado (mas bem abaixo do
// default de 5 MiB) recebe 413, enquanto um corpo abaixo passa do BodyLimit.
func TestNewHandlerWithOptions_AplicaMaxInputSizeNoBodyLimit(t *testing.T) {
	handler, err := NewHandlerWithOptions(WithMaxInputSize(100))
	if err != nil {
		t.Fatal(err)
	}

	// Acima do limite customizado => 413 (não poderia ser rejeitado pelo
	// default de 5 MiB, provando que a opção chegou ao handler).
	large := bytes.Repeat([]byte("a"), 101)
	rec := servePOST(handler, large)
	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("esperava 413 para corpo acima do limite customizado, obteve %d; body: %s", rec.Code, rec.Body.String())
	}

	// Abaixo do limite => passa pelo BodyLimit e chega ao serviço (que rejeita
	// o XML genérico com 400), provando que o limite não rejeita tudo.
	small := bytes.Repeat([]byte("a"), 50)
	rec2 := servePOST(handler, small)
	if rec2.Code == http.StatusRequestEntityTooLarge {
		t.Fatalf("corpo abaixo do limite não deveria ser rejeitado por BodyLimit; body: %s", rec2.Body.String())
	}
}

// TestNewHandler_VariadicMaxInputSizeTambemVale garante que o construtor
// variádico existente (NewHandler(maxInputSize ...int)) também propaga o
// limite configurado ao BodyLimit do handler.
func TestNewHandler_VariadicMaxInputSizeTambemVale(t *testing.T) {
	handler, err := NewHandler(100)
	if err != nil {
		t.Fatal(err)
	}

	rec := servePOST(handler, bytes.Repeat([]byte("a"), 101))
	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("esperava 413 via NewHandler variádico, obteve %d; body: %s", rec.Code, rec.Body.String())
	}
}

func servePOST(handler http.Handler, body []byte) *httptest.ResponseRecorder {
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/xml")
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)
	return rec
}
