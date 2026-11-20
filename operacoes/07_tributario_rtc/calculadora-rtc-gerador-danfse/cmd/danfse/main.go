// @title DANFSe
// @version 1.0
// @description API para geração DANFSe
// @BasePath /

package main

import (
	"context"
	"errors"
	"log/slog"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"danfse-service/internal/app"
	"danfse-service/internal/config"
	"danfse-service/internal/controller"
	"danfse-service/internal/telemetry"
)

// Timeouts mínimos defensáveis do servidor HTTP. Não há WriteTimeout curto:
// a geração de PDF pode ser demorada e um limite pequeno derrubaria requests
// legítimos; conexões ociosas são cobertas pelo IdleTimeout.
const (
	readHeaderTimeout = 10 * time.Second
	idleTimeout       = 120 * time.Second

	httpShutdownTimeout      = 10 * time.Second
	telemetryShutdownTimeout = 5 * time.Second

	// safeShutdownWait é a folga usada ao aguardar a conclusão do shutdown após
	// uma falha do Serve: garante que a espera nunca seja infinita.
	safeShutdownWait = time.Second
)

func main() {
	// Logger JSON default ANTES do parse de configuração: os erros de boot
	// também saem em JSON (stderr), nunca em texto.
	slog.SetDefault(newJSONLogger(controller.ParseLevel(controller.DefaultLogLevel)))

	if err := run(); err != nil {
		slog.Error("servidor danfse finalizado com erro", "error", err.Error())
		os.Exit(1)
	}
}

// newJSONLogger cria um logger slog com saída JSON em stderr no nível pedido,
// usado pelo processo e pelo RequestLogger nativo do Echo.
func newJSONLogger(level slog.Level) *slog.Logger {
	return slog.New(slog.NewJSONHandler(os.Stderr, &slog.HandlerOptions{Level: level}))
}

func run() error {
	env := config.NewEnv()
	if err := env.Carregar(); err != nil {
		return err
	}

	// Logs JSON estruturados em stderr no nível configurado por LOG_LEVEL.
	logger := newJSONLogger(controller.ParseLevel(env.LogLevel))
	slog.SetDefault(logger)

	// Readiness inicia pronto; será marcado como false ANTES do Shutdown.
	readiness := controller.NewReadiness(true)

	// Telemetria opt-in. Configuração OTel explícita INVÁLIDA deve falhar o
	// startup (fail-fast); o erro do SDK nunca é logado/retornado porque pode
	// carregar URL/credenciais do endpoint. Falhas de EXPORTAÇÃO (assíncronas)
	// nunca derrubam o processo nem a requisição — são tratadas em um warn
	// seguro no shutdown.
	runtime, err := telemetry.New(context.Background())
	if err != nil {
		const telemetryCfgErr = "configuração de telemetria inválida (OTEL_*); corrija as variáveis e reinicie"
		logger.Error(telemetryCfgErr)
		return errors.New(telemetryCfgErr)
	}
	// Cleanup bounded garantido também em falha de boot/listen: só é pulado
	// quando o graceful shutdown (goroutine) já fez o flush da telemetria.
	telemetryShutdownPending := true
	defer func() {
		if telemetryShutdownPending {
			shutdownTelemetryBounded(runtime, logger)
		}
	}()

	// O XMLMaxSizeBytes configurado (XML_MAX_SIZE_BYTES) é repassado pelo
	// construtor atual (NewPdfService variádico), alimentando simultaneamente o
	// BodyLimit do router e o limite interno do preparador.
	server, err := app.NewHandlerWithOptions(
		app.WithLogger(logger),
		app.WithReadiness(readiness),
		app.WithMaxInputSize(env.XMLMaxSizeBytes),
		app.WithMiddleware(runtime.Middleware()),
	)
	if err != nil {
		return err
	}

	port := strconv.Itoa(env.Server.Port)
	httpServer := &http.Server{
		Addr:              ":" + port,
		Handler:           server,
		ReadHeaderTimeout: readHeaderTimeout,
		IdleTimeout:       idleTimeout,
		// Adapter seguro: erros internos do net/http saem via slog JSON em stderr.
		ErrorLog: slog.NewLogLogger(logger.Handler(), slog.LevelError),
	}

	// Bind PRIMEIRO e só então anuncia "online": evita log de sucesso quando o
	// bind (porta) falha.
	listener, err := net.Listen("tcp", httpServer.Addr)
	if err != nil {
		return err
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	// shutdownDone garante que run() NÃO retorne (e o processo NÃO saia) antes
	// do graceful shutdown HTTP (aguardando requisições em andamento) e do flush
	// de telemetria terminarem.
	shutdownDone := make(chan struct{})
	go func() {
		defer close(shutdownDone)
		<-ctx.Done()
		logger.Info("sinal de encerramento recebido; iniciando graceful shutdown")

		// Readiness é marcado como NÃO pronto ANTES do Shutdown. Atenção: apenas
		// o probe /health/ready passa a responder 503 (orquestrador para de
		// rotear novas requisições); requisições regulares em voo continuam
		// sendo atendidas até o fim do graceful shutdown.
		readiness.SetReady(false)

		// Shutdown HTTP bounded.
		srvCtx, srvCancel := context.WithTimeout(context.Background(), httpShutdownTimeout)
		defer srvCancel()
		if err := httpServer.Shutdown(srvCtx); err != nil {
			logger.Error("erro ao encerrar servidor HTTP", "error", err.Error())
		} else {
			logger.Info("servidor HTTP encerrado")
		}

		// Telemetria bounded após o graceful shutdown HTTP (requisições em
		// andamento concluídas); falhas de export viram um warn
		// com mensagem segura (o erro do SDK pode carregar URL/credenciais).
		shutdownTelemetryBounded(runtime, logger)
	}()

	logger.Info("servidor danfse online", "port", port, "log_level", env.LogLevel)
	logger.Info("swagger disponível",
		"url", "http://localhost:"+port+"/api/swagger-ui/index.html",
	)

	errServe := httpServer.Serve(listener)
	if errServe != nil && !errors.Is(errServe, http.ErrServerClosed) {
		// Falha no Serve fora do graceful shutdown: aciona o shutdown e
		// aguarda a conclusão com espera bounded (nunca infinito).
		stop()
		select {
		case <-shutdownDone:
		case <-time.After(httpShutdownTimeout + telemetryShutdownTimeout + safeShutdownWait):
		}
		telemetryShutdownPending = false
		return errServe
	}

	// Serve retornou (ErrServerClosed = a goroutine de shutdown já foi
	// disparada pelo sinal): AGUARDA a goroutine concluir o graceful shutdown
	// (aguardar requisições em andamento) + flush antes de
	// retornar, garantindo que o processo não saia prematuramente.
	<-shutdownDone
	telemetryShutdownPending = false
	return nil
}

// shutdownTelemetryBounded encerra a telemetria com contexto com timeout e loga
// apenas mensagens seguras: nunca ecoa o erro do SDK (pode conter URL de
// endpoint com credenciais). Runtime nil ou já encerrado é no-op.
func shutdownTelemetryBounded(runtime *telemetry.Runtime, logger *slog.Logger) {
	ctx, cancel := context.WithTimeout(context.Background(), telemetryShutdownTimeout)
	defer cancel()
	if err := runtime.Shutdown(ctx); err != nil {
		logger.Warn("falha ao encerrar telemetria; reportar sem detalhes de configuração")
	}
}
