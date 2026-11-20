// Observabilidade HTTP da camada HTTP do serviço.
//
// Logs:
//   - logger JSON estruturado (slog -> stderr), nível controlado por LOG_LEVEL;
//   - logging de requests pelo middleware NATIVO do Echo
//     (middleware.RequestLogger), com uma única linha de conclusão por
//     requisição (evento request_completed);
//
// Correlação:
//   - request ID é SEMPRE gerado pelo servidor (middleware nativo RequestID do
//     Echo). O cabeçalho X-Request-ID enviado pelo cliente é descartado antes
//     de qualquer middleware: dados de correlação oriundos do cliente não são
//     confiáveis (podem carregar chars inválidos ou conteúdo fiscal) e nunca
//     são ecoados, propagados ou logados.
//
// Privacidade: nunca são logados corpo (XML), PDF gerado, query string,
// identificadores fiscais ou cabeçalhos de autenticação.
package controller

import (
	"errors"
	"io"
	"log/slog"
	"net/http"
	"strings"

	"github.com/labstack/echo/v5"
	echo_middleware "github.com/labstack/echo/v5/middleware"
	"go.opentelemetry.io/otel/trace"
)

// Níveis de log suportados pela variável LOG_LEVEL.
const (
	LevelDebug = "debug"
	LevelInfo  = "info"
	LevelWarn  = "warn"
	LevelError = "error"
)

// DefaultLogLevel é o nível assumido quando LOG_LEVEL não é informado.
const DefaultLogLevel = LevelInfo

// routeUnmatched é o fallback constante usado quando a requisição não tem rota
// correspondente (ex.: 404), evitando logar caminhos arbitrários do cliente.
const routeUnmatched = "unmatched"

// ParseLevel converte o valor textual de LOG_LEVEL em slog.Level.
// Valores vazios ou desconhecidos caem em slog.LevelInfo (padrão).
func ParseLevel(level string) slog.Level {
	switch strings.ToLower(strings.TrimSpace(level)) {
	case LevelDebug:
		return slog.LevelDebug
	case LevelWarn:
		return slog.LevelWarn
	case LevelError:
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}

// IsValidLevel indica se level é um dos níveis aceitos por LOG_LEVEL.
func IsValidLevel(level string) bool {
	switch strings.ToLower(strings.TrimSpace(level)) {
	case LevelDebug, LevelInfo, LevelWarn, LevelError:
		return true
	}
	return false
}

// DiscardLogger retorna um logger que descarta toda a saída. É o padrão usado
// pelo Router quando nenhum logger é injetado (ex.: testes), mantendo o
// comportamento silencioso sem perder a cobertura dos middlewares.
func DiscardLogger() *slog.Logger {
	return slog.New(slog.NewJSONHandler(io.Discard, nil))
}

// stripExternalRequestIDHeader remove o cabeçalho X-Request-ID de entrada antes
// de qualquer outro middleware. O request ID é SEMPRE gerado pelo servidor
// (middleware nativo RequestID do Echo): identificadores do cliente não são
// confiáveis (chars inválidos, comprimento ou conteúdo fiscal) e nunca são
// ecoados nem propagados. O cabeçalho de resposta X-Request-ID, portanto, traz
// sempre um ID novo gerado pelo servidor.
func stripExternalRequestIDHeader(next echo.HandlerFunc) echo.HandlerFunc {
	return func(c *echo.Context) error {
		c.Request().Header.Del(echo.HeaderXRequestID)
		return next(c)
	}
}

// LogRequests retorna o middleware de observabilidade HTTP usando o
// RequestLogger NATIVO do Echo. Com HandleError=true o error handler central
// roda ANTES do log, então o status registrado reflete a resposta efetivamente
// enviada; o latch de status e a duração vêm do próprio framework (sem
// implementação própria de timer/status).
//
// A saída é UMA linha JSON de conclusão por requisição (evento
// request_completed) com: method, route (ou fallback "unmatched"), status,
// duration, request_id (gerado pelo servidor), trace_id/span_id quando o
// contexto carrega um span OTel e error_type quando há erro. Probes de health
// são logados em nível DEBUG com kind=probe.
//
// Redação: corpo (XML), PDF, query string, IDs fiscais e cabeçalhos de
// autenticação nunca são logados. Falhas são tipadas por códigos estáveis
// (error_type), sem string crua de erro.
func LogRequests(logger *slog.Logger) echo.MiddlewareFunc {
	l := logger
	if l == nil {
		l = slog.Default()
	}
	return echo_middleware.RequestLoggerWithConfig(echo_middleware.RequestLoggerConfig{
		HandleError:  true,
		LogLatency:   true,
		LogMethod:    true,
		LogRoutePath: true,
		LogStatus:    true,
		LogRequestID: true,
		LogValuesFunc: func(c *echo.Context, v echo_middleware.RequestLoggerValues) error {
			route := v.RoutePath
			if route == "" {
				route = routeUnmatched
			}

			attrs := []slog.Attr{
				slog.Time("timestamp", v.StartTime.UTC()),
				slog.String("event", "request_completed"),
				slog.String("request_id", v.RequestID),
				slog.String("method", v.Method),
				slog.String("route", route),
				slog.Int("status", v.Status),
				slog.Duration("duration", v.Latency),
			}
			// trace_id/span_id são incluídos apenas quando a instrumentação
			// OTel presente no contexto os fornecer.
			if sc := trace.SpanContextFromContext(c.Request().Context()); sc.HasTraceID() {
				attrs = append(attrs,
					slog.String("trace_id", sc.TraceID().String()),
					slog.String("span_id", sc.SpanID().String()),
				)
			}
			if v.Error != nil {
				attrs = append(attrs, slog.String("error_type", errorType(v.Error)))
			}

			level := levelForStatus(v.Status)
			if isProbeRequest(c.Request()) {
				attrs = append(attrs, slog.String("kind", "probe"))
				// Probes são ruído operacional: só aparecem em nível debug.
				level = slog.LevelDebug
			}

			l.LogAttrs(c.Request().Context(), level, "request_completed", attrs...)
			return nil
		},
	})
}

// levelForStatus escolhe o nível do log de conclusão a partir do status HTTP.
func levelForStatus(status int) slog.Level {
	switch {
	case status >= http.StatusInternalServerError:
		return slog.LevelError
	case status >= http.StatusBadRequest:
		return slog.LevelWarn
	default:
		return slog.LevelInfo
	}
}

// errorType traduz o erro em um código estável e não sensível para o log.
// Nenhuma mensagem crua de erro (que poderia conter XML/fiscal) é emitida,
// inclusive para panics recuperados (o erro do Recover carrega o valor do panic).
func errorType(err error) string {
	if err == nil {
		return ""
	}
	if errors.Is(err, http.ErrAbortHandler) {
		return "aborted"
	}
	var panicErr *echo_middleware.PanicStackError
	if errors.As(err, &panicErr) {
		return "panic"
	}
	var statusCoder interface{ StatusCode() int }
	if errors.As(err, &statusCoder) {
		return "http_error"
	}
	return "internal"
}

// isProbeRequest informa se a requisição é um probe de liveness/readiness.
func isProbeRequest(req *http.Request) bool {
	return req != nil && isProbePath(req.URL.Path)
}
