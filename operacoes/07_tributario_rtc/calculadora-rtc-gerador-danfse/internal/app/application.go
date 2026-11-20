package app

import (
	"log/slog"
	"net/http"

	"danfse-service/internal/controller"
	"github.com/labstack/echo/v5"
)

// Option configura a construção do handler HTTP (app.NewHandlerWithOptions).
type Option func(*options)

type options struct {
	logger       *slog.Logger
	middlewares  []echo.MiddlewareFunc
	readiness    *controller.Readiness
	maxInputSize []int
}

// WithLogger injeta o logger JSON estruturado usado na camada HTTP.
func WithLogger(logger *slog.Logger) Option {
	return func(o *options) { o.logger = logger }
}

// WithMiddleware acrescenta middlewares Echo (ex.: instrumentação OTel do
// runtime de telemetria) que rodam DENTRO do middleware de observabilidade
// externo.
func WithMiddleware(middlewares ...echo.MiddlewareFunc) Option {
	return func(o *options) { o.middlewares = append(o.middlewares, middlewares...) }
}

// WithReadiness fornece o estado de readiness usado pelos health checks.
func WithReadiness(readiness *controller.Readiness) Option {
	return func(o *options) { o.readiness = readiness }
}

// WithMaxInputSize define o limite máximo (em bytes) do XML de entrada,
// repassado ao construtor atual (NewPdfService variádico) e aplicado tanto ao
// BodyLimit do router quanto ao preparador. Quando desnecessário (default),
// usa service.MaxXMLInputSize (5 MiB).
func WithMaxInputSize(maxBytes int) Option {
	return func(o *options) { o.maxInputSize = []int{maxBytes} }
}

// NewHandler monta manualmente a aplicação e devolve o http.Handler pronto.
// O repositório de municípios é carregado a partir do catálogo em memória embutido.
// O parâmetro variádico maxInputSize, quando fornecido e positivo, define o
// limite máximo (em bytes) da entrada XML, aplicado tanto ao BodyLimit do router
// quanto ao preparador; se omitido, usa o padrão compartilhado
// (service.MaxXMLInputSize, 5 MiB). Esta é a única opção aceita.
//
// Para injetar logger/middleware/readiness, use NewHandlerWithOptions.
func NewHandler(maxInputSize ...int) (http.Handler, error) {
	return newHandler(options{maxInputSize: maxInputSize})
}

// NewHandlerWithOptions constrói o handler HTTP com opções explícitas de
// logger, middlewares adicionais e readiness, preservando o construtor
// variádico existente (NewHandler) intacto.
func NewHandlerWithOptions(userOpts ...Option) (http.Handler, error) {
	o := options{}
	for _, apply := range userOpts {
		apply(&o)
	}
	return newHandler(o)
}

func newHandler(o options) (http.Handler, error) {
	servicoGeracaoPdf, err := NewPdfService(o.maxInputSize...)
	if err != nil {
		return nil, err
	}

	controladorDanfse := controller.NewDanfseController(servicoGeracaoPdf)

	var opts []controller.RouterOption
	if o.logger != nil {
		opts = append(opts, controller.WithLogger(o.logger))
	}
	if len(o.middlewares) > 0 {
		opts = append(opts, controller.WithMiddleware(o.middlewares...))
	}
	if o.readiness != nil {
		opts = append(opts, controller.WithReadiness(o.readiness))
	}
	if len(o.maxInputSize) > 0 && o.maxInputSize[0] > 0 {
		opts = append(opts, controller.WithBodyLimit(int64(o.maxInputSize[0])))
	}

	router := controller.NewRouterWithOptions(controladorDanfse, opts...)
	return router.Inicializar(), nil
}
