package app

import (
	"danfse-service/internal/domain/localidade"
	"danfse-service/internal/renderer"
	"danfse-service/internal/service"
)

// NewPdfServiceWithDependencies cria o serviço de geração de PDF a partir de
// dependências já construídas (catalogo, renderizador, apresentador).
// Útil para WASM que reutiliza esses objetos entre chamadas.
// O parâmetro variádico maxInputSize, quando fornecido, é repassado ao
// preparador como limite máximo (em bytes) da entrada XML; se omitido, usa
// o padrão compartilhado (service.MaxXMLInputSize, 5 MiB).
func NewPdfServiceWithDependencies(resolver service.ResolverMunicipio, renderizador service.RenderizadorPdfDireto, apresentador service.ApresentadorDanfse, maxInputSize ...int) *service.ServicoGeracaoPdfDanfseDireto {
	preparador := service.NovoServicoPreparacaoDanfse(resolver, maxInputSize...)
	return service.NovoServicoGeracaoPdfDanfseDireto(preparador, apresentador, renderizador)
}

// NewPdfService constrói a cadeia completa de geração de PDF:
// catálogo → preparador → renderizador → apresentador → serviço de geração.
// O parâmetro variádico maxInputSize, quando fornecido, define o limite máximo
// (em bytes) da entrada XML repassado ao preparador; se omitido, usa o padrão
// compartilhado (service.MaxXMLInputSize, 5 MiB).
func NewPdfService(maxInputSize ...int) (service.ServicoGeracaoPdf, error) {
	cat, err := localidade.Carregar()
	if err != nil {
		return nil, err
	}

	renderizador, err := renderer.NewRenderer()
	if err != nil {
		return nil, err
	}

	apresentador := service.NovoServicoApresentacaoDanfse()

	return NewPdfServiceWithDependencies(cat.BuscarMunicipio, renderizador, apresentador, maxInputSize...), nil
}
