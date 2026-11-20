package service

import (
	"context"

	"danfse-service/internal/domain/enums"
)

// ServicoGeracaoPdfDanfseDireto gera PDF a partir do modelo NFSe enriquecido,
// sem criar um documento HTML intermediário.
type ServicoGeracaoPdfDanfseDireto struct {
	preparador   PreparadorDanfse
	apresentador ApresentadorDanfse
	renderizador RenderizadorPdfDireto
}

func (g *ServicoGeracaoPdfDanfseDireto) Executar(ctx context.Context, xmlData []byte, situacao enums.SituacaoNotaTipo, canhoto bool) ([]byte, error) {
	nota, err := g.preparador.Executar(ctx, xmlData, situacao)
	if err != nil {
		return nil, err
	}
	documento := g.apresentador.Apresentar(nota)
	documento.Canhoto.Visivel = canhoto
	return g.renderizador.Renderizar(ctx, documento)
}

func NovoServicoGeracaoPdfDanfseDireto(preparador PreparadorDanfse, apresentador ApresentadorDanfse, renderizador RenderizadorPdfDireto) *ServicoGeracaoPdfDanfseDireto {
	return &ServicoGeracaoPdfDanfseDireto{preparador: preparador, apresentador: apresentador, renderizador: renderizador}
}
