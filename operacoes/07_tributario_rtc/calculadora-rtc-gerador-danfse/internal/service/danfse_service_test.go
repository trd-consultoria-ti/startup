package service

import (
	"context"
	"errors"
	"testing"

	"danfse-service/internal/domain/enums"
	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
)

// mockPreparador implementa PreparadorDanfse para testes.
type mockPreparador struct {
	nota models.NFSe
	err  error
}

func (m *mockPreparador) Executar(_ context.Context, _ []byte, _ enums.SituacaoNotaTipo) (models.NFSe, error) {
	if m.err != nil {
		return models.NFSe{}, m.err
	}
	return m.nota, nil
}

// mockRenderizador implementa RenderizadorPdfDireto para testes.
type mockRenderizador struct {
	doc presentation.DocumentoDanfse
	pdf []byte
	err error
}

func (m *mockRenderizador) Renderizar(_ context.Context, doc presentation.DocumentoDanfse) ([]byte, error) {
	m.doc = doc
	mockRenderizadorLast.doc = doc
	if m.err != nil {
		return nil, m.err
	}
	return m.pdf, nil
}

// mockRenderizadorLast captura o último documento renderizado para asserção.
var mockRenderizadorLast struct {
	doc presentation.DocumentoDanfse
}

// mockApresentador implementa ApresentadorDanfse para testes.
type mockApresentador struct {
	doc presentation.DocumentoDanfse
}

func (m *mockApresentador) Apresentar(_ models.NFSe) presentation.DocumentoDanfse {
	return m.doc
}

func TestServicoGeracaoPdfDanfseDireto_Sucesso(t *testing.T) {
	pdf := []byte("%PDF-fake")
	apresentador := &mockApresentador{
		doc: presentation.DocumentoDanfse{},
	}
	svc := NovoServicoGeracaoPdfDanfseDireto(
		&mockPreparador{},
		apresentador,
		&mockRenderizador{pdf: pdf},
	)

	result, err := svc.Executar(context.Background(), []byte("<xml/>"), enums.VALIDA, true)
	if err != nil {
		t.Fatalf("esperava sucesso, obteve erro: %v", err)
	}
	if string(result) != string(pdf) {
		t.Fatalf("esperava %q, obteve %q", pdf, result)
	}
}

func TestServicoGeracaoPdfDanfseDireto_PreparadorErro(t *testing.T) {
	svc := NovoServicoGeracaoPdfDanfseDireto(
		&mockPreparador{err: errors.New("preparador falhou")},
		&mockApresentador{},
		&mockRenderizador{},
	)

	_, err := svc.Executar(context.Background(), []byte("<xml/>"), enums.VALIDA, true)
	if err == nil {
		t.Fatal("esperava erro do preparador, obteve nil")
	}
	if err.Error() != "preparador falhou" {
		t.Fatalf("mensagem inesperada: %v", err)
	}
}

func TestServicoGeracaoPdfDanfseDireto_RenderizadorErro(t *testing.T) {
	svc := NovoServicoGeracaoPdfDanfseDireto(
		&mockPreparador{},
		&mockApresentador{},
		&mockRenderizador{err: errors.New("render falhou")},
	)

	_, err := svc.Executar(context.Background(), []byte("<xml/>"), enums.VALIDA, true)
	if err == nil {
		t.Fatal("esperava erro do renderizador, obteve nil")
	}
	if err.Error() != "render falhou" {
		t.Fatalf("mensagem inesperada: %v", err)
	}
}

func TestServicoGeracaoPdfDanfseDireto_CanhotoFalse(t *testing.T) {
	mockRenderizadorLast.doc = presentation.DocumentoDanfse{}
	pdf := []byte("%PDF-fake")
	svc := NovoServicoGeracaoPdfDanfseDireto(
		&mockPreparador{},
		&mockApresentador{},
		&mockRenderizador{pdf: pdf},
	)

	_, err := svc.Executar(context.Background(), []byte("<xml/>"), enums.VALIDA, false)
	if err != nil {
		t.Fatalf("esperava sucesso, obteve erro: %v", err)
	}

	if mockRenderizadorLast.doc.Canhoto.Visivel {
		t.Fatal("esperava Canhoto.Visivel == false, obteve true")
	}
}

func TestServicoGeracaoPdfDanfseDireto_CanhotoTrue(t *testing.T) {
	mockRenderizadorLast.doc = presentation.DocumentoDanfse{}
	pdf := []byte("%PDF-fake")
	svc := NovoServicoGeracaoPdfDanfseDireto(
		&mockPreparador{},
		&mockApresentador{},
		&mockRenderizador{pdf: pdf},
	)

	_, err := svc.Executar(context.Background(), []byte("<xml/>"), enums.VALIDA, true)
	if err != nil {
		t.Fatalf("esperava sucesso, obteve erro: %v", err)
	}

	if !mockRenderizadorLast.doc.Canhoto.Visivel {
		t.Fatal("esperava Canhoto.Visivel == true, obteve false")
	}
}
