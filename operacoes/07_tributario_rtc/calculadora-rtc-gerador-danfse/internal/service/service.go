package service

import (
	"context"

	"danfse-service/internal/domain/enums"
	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
)

// ServicoGeracaoPdf gera o DANFSe em PDF.
type ServicoGeracaoPdf interface {
	Executar(ctx context.Context, xmlData []byte, situacao enums.SituacaoNotaTipo, canhoto bool) ([]byte, error)
}

// PreparadorDanfse analisa e enriquece o modelo NFSe independentemente da saída.
type PreparadorDanfse interface {
	Executar(ctx context.Context, xmlData []byte, situacao enums.SituacaoNotaTipo) (models.NFSe, error)
}

// ApresentadorDanfse mapeia um modelo NFSe enriquecido para um documento de apresentação puro.
type ApresentadorDanfse interface {
	Apresentar(nota models.NFSe) presentation.DocumentoDanfse
}

// RenderizadorPdfDireto renderiza um presentation.DocumentoDanfse diretamente em PDF.
type RenderizadorPdfDireto interface {
	Renderizar(ctx context.Context, doc presentation.DocumentoDanfse) ([]byte, error)
}
