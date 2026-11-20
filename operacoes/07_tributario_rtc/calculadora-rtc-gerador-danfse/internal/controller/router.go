package controller

import (
	"log/slog"
	"net/http"

	_ "danfse-service/api/swagger"
	"danfse-service/internal/service"
	"github.com/labstack/echo/v5"
	echo_middleware "github.com/labstack/echo/v5/middleware"
	echo_swagger "github.com/swaggo/echo-swagger/v2"
)

// RouterOption configura opções de construção do Router.
type RouterOption func(*routerOptions)

type routerOptions struct {
	logger      *slog.Logger
	middlewares []echo.MiddlewareFunc
	readiness   *Readiness
	bodyLimit   int64
}

// WithLogger injeta o logger JSON estruturado usado pelo middleware nativo de
// observabilidade HTTP (RequestLogger do Echo) e pelo echo.
func WithLogger(logger *slog.Logger) RouterOption {
	return func(o *routerOptions) { o.logger = logger }
}

// WithMiddleware acrescenta middlewares que devem rodar DENTRO do middleware de
// observabilidade externo (ex.: instrumentação OTel do runtime de telemetria).
func WithMiddleware(middlewares ...echo.MiddlewareFunc) RouterOption {
	return func(o *routerOptions) { o.middlewares = append(o.middlewares, middlewares...) }
}

// WithReadiness fornece o estado de readiness usado pelos health checks.
func WithReadiness(readiness *Readiness) RouterOption {
	return func(o *routerOptions) { o.readiness = readiness }
}

// WithBodyLimit define o limite máximo (em bytes) do corpo da requisição.
func WithBodyLimit(bytes int64) RouterOption {
	return func(o *routerOptions) { o.bodyLimit = bytes }
}

type Router struct {
	controladorDanfse *DanfseController
	opts              routerOptions
}

func (r *Router) Inicializar() http.Handler {

	e := echo.New()

	logger := r.opts.logger
	if logger == nil {
		logger = DiscardLogger()
	}
	e.Logger = logger

	e.HTTPErrorHandler = TratarErrosHTTP

	// Correlação: request ID é SEMPRE gerado pelo servidor. O cabeçalho
	// X-Request-ID enviado pelo cliente é descartado antes do middleware nativo
	// RequestID gerar um novo (identificadores do cliente não são confiáveis —
	// chars inválidos/compridos ou conteúdo fiscal — e nunca são ecoados).
	e.Use(stripExternalRequestIDHeader)
	e.Use(echo_middleware.RequestID())

	// Middleware de logging EXTERNO aos demais, usando o RequestLogger NATIVO do
	// Echo: registrado antes da cadeia interna (telemetria, recover, CORS, body
	// limit) e com HandleError, processa o error handler antes de logar o request
	// completion com status real.
	e.Use(LogRequests(logger))

	// Middlewares adicionais (ex.: instrumentação OTel) rodam dentro do logging.
	for _, mw := range r.opts.middlewares {
		e.Use(mw)
	}

	e.Use(echo_middleware.Recover())
	e.Use(echo_middleware.CORSWithConfig(echo_middleware.CORSConfig{
		AllowOrigins: []string{"*"},
		AllowMethods: []string{http.MethodPost, http.MethodOptions},
	}))
	e.Use(echo_middleware.BodyLimit(r.opts.bodyLimit))

	// Health checks de ciclo de vida (sem dependência externa ou geração de PDF).
	readiness := r.opts.readiness
	if readiness == nil {
		readiness = NewReadiness(true)
	}
	e.GET(HealthLivePath, handlerLive(readiness))
	e.GET(HealthReadyPath, handlerReady(readiness))

	// Canonical business routes.
	e.POST("/api/danfse", r.controladorDanfse.GerarPdf)

	// Canonical Swagger UI routes.
	swaggerHandler := echo_swagger.EchoWrapHandler()
	e.GET("/api/swagger-ui", handlerRedirectSwagger())
	e.GET("/api/swagger-ui/", handlerRedirectSwagger())
	e.GET("/api/swagger-ui/*", swaggerHandler)

	return e
}

// NewRouter constrói o Router. O parâmetro variádico bodyLimitBytes, quando
// fornecido e positivo, define o limite do corpo da requisição (BodyLimit);
// se omitido ou não positivo, usa service.MaxXMLInputSize (5 MiB) como padrão.
// Esta é a única opção aceita.
func NewRouter(controladorDanfse *DanfseController, bodyLimitBytes ...int) *Router {
	o := defaultRouterOptions()
	if len(bodyLimitBytes) > 0 && bodyLimitBytes[0] > 0 {
		o.bodyLimit = int64(bodyLimitBytes[0])
	}
	return &Router{controladorDanfse: controladorDanfse, opts: o}
}

// NewRouterWithOptions constrói o Router com opções explícitas de logger,
// middlewares adicionais, readiness e body limit. Preserva o construtor
// variádico NewRouter para compatibilidade.
func NewRouterWithOptions(controladorDanfse *DanfseController, opts ...RouterOption) *Router {
	o := defaultRouterOptions()
	for _, apply := range opts {
		apply(&o)
	}
	return &Router{controladorDanfse: controladorDanfse, opts: o}
}

func defaultRouterOptions() routerOptions {
	return routerOptions{
		bodyLimit: int64(service.MaxXMLInputSize),
		readiness: NewReadiness(true),
	}
}
