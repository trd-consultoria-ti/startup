package controller

import (
	"net/http"
	"sync/atomic"

	"github.com/labstack/echo/v5"
)

// Health endpoints reconhecidos como probes de liveness/readiness.
const (
	HealthLivePath  = "/health/live"
	HealthReadyPath = "/health/ready"
)

// isProbePath informa se o caminho é um probe de health.
func isProbePath(path string) bool {
	return path == HealthLivePath || path == HealthReadyPath
}

// HealthStatus é o corpo de resposta dos probes de health.
type HealthStatus struct {
	// Status é "ok" quando o serviço está de pé/pronto e "unavailable" durante
	// o graceful shutdown (aguardando as requisições em andamento).
	Status string `json:"status" example:"ok"`
}

// Readiness controla o estado de readiness do serviço de forma thread-safe.
//
// Regra de ciclo de vida: o HTTP server deve marcar o estado como NÃO pronto
// (SetReady(false)) ANTES de iniciar o graceful shutdown, para que novos
// requests passem a receber 503 enquanto as requisições em andamento são
// atendidas.
type Readiness struct {
	ready atomic.Bool
}

// NewReadiness cria um estado de readiness com o valor inicial informado.
func NewReadiness(ready bool) *Readiness {
	r := &Readiness{}
	r.ready.Store(ready)
	return r
}

// SetReady define o estado pronto/em graceful shutdown. Thread-safe.
func (r *Readiness) SetReady(ready bool) {
	if r == nil {
		return
	}
	r.ready.Store(ready)
}

// IsReady retorna true enquanto o serviço estiver pronto para receber tráfego.
func (r *Readiness) IsReady() bool {
	if r == nil {
		return true
	}
	return r.ready.Load()
}

// handlerLive implementa o probe de liveness: responde 200 com {"status":"ok"}
// enquanto o processo estiver de pé, sem checar dependências externas nem gerar
// PDF.
//
// @Summary      Consulta o liveness do serviço.
// @Description  Responde 200 com {"status":"ok"} enquanto o processo estiver de pé, sem checar dependências externas nem gerar PDF.
// @Tags         Health
// @Produce      application/json
// @Success      200  {object}  controller.HealthStatus  "Serviço de pé."
// @Router       /health/live [get]
func handlerLive(_ *Readiness) echo.HandlerFunc {
	return func(c *echo.Context) error {
		return c.JSON(http.StatusOK, HealthStatus{Status: "ok"})
	}
}

// handlerReady implementa o probe de readiness: responde 200 com
// {"status":"ok"} enquanto o serviço estiver pronto para receber tráfego e 503
// com {"status":"unavailable"} durante o graceful shutdown (readiness marcado
// como não pronto ANTES do Shutdown).
//
// @Summary      Consulta o readiness do serviço.
// @Description  Responde 200 com {"status":"ok"} enquanto o serviço estiver pronto para receber tráfego e 503 com {"status":"unavailable"} durante o graceful shutdown.
// @Tags         Health
// @Produce      application/json
// @Success      200  {object}  controller.HealthStatus  "Serviço pronto para receber tráfego."
// @Failure      503  {object}  controller.HealthStatus  "Serviço em graceful shutdown (não pronto)."
// @Router       /health/ready [get]
func handlerReady(readiness *Readiness) echo.HandlerFunc {
	return func(c *echo.Context) error {
		if readiness == nil || readiness.IsReady() {
			return c.JSON(http.StatusOK, HealthStatus{Status: "ok"})
		}
		return c.JSON(http.StatusServiceUnavailable, HealthStatus{Status: "unavailable"})
	}
}

// handlerRedirectSwagger redireciona os caminhos de entrada do Swagger UI para
// o index canônico.
func handlerRedirectSwagger() echo.HandlerFunc {
	return func(c *echo.Context) error {
		return c.Redirect(http.StatusMovedPermanently, "/api/swagger-ui/index.html")
	}
}
