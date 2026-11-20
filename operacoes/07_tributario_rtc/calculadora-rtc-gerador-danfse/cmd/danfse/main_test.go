package main

import (
	"context"
	"net"
	"strconv"
	"testing"
	"time"

	"danfse-service/internal/controller"
	"danfse-service/internal/telemetry"
)

// TestRun_FalhaConfigInvalida cobre o caminho de erro de configuração do boot:
// o run retorna erro (sem entrar no ciclo de servidor) para LOG_LEVEL inválido.
func TestRun_FalhaConfigInvalida(t *testing.T) {
	t.Setenv("LOG_LEVEL", "verbose")
	if err := run(); err == nil {
		t.Fatal("esperava erro para LOG_LEVEL inválido")
	}
}

// TestRun_BindFalhaRetornaErroSemHang cobre o caminho de falha de listen: com a
// porta já ocupada, run() deve devolver erro imediatamente (sem hang) e, como o
// graceful shutdown nunca foi iniciado, o cleanup bounded da telemetria ocorre
// pelo defer (no-op com telemetria desabilitada por padrão).
func TestRun_BindFalhaRetornaErroSemHang(t *testing.T) {
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer ln.Close()
	port := ln.Addr().(*net.TCPAddr).Port

	t.Setenv("SERVER_PORT", strconv.Itoa(port))
	if err := run(); err == nil {
		t.Fatal("esperava erro de bind para porta já ocupada")
	}
}

// TestShutdownTelemetryBounded_Seguro com runtime nil garante que o cleanup
// bounded nunca trava (garantia de espera mesmo em caminhos de erro).
func TestShutdownTelemetryBounded_Seguro(t *testing.T) {
	done := make(chan struct{})
	go func() {
		defer close(done)
		shutdownTelemetryBounded(nil, controller.DiscardLogger())
	}()
	select {
	case <-done:
	case <-time.After(3 * time.Second):
		t.Fatal("shutdownTelemetryBounded deveria retornar imediatamente para runtime nil")
	}
}

// TestShutdownTelemetryBounded_RuntimeDesabilitadoSeguro confirma que um runtime
// desabilitado (setado explicitamente como no-op) encerra sem erro e sem demora.
func TestShutdownTelemetryBounded_RuntimeDesabilitadoSeguro(t *testing.T) {
	t.Setenv("OTEL_SDK_DISABLED", "true")

	runtime, err := telemetry.New(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	done := make(chan struct{})
	go func() {
		defer close(done)
		shutdownTelemetryBounded(runtime, controller.DiscardLogger())
	}()
	select {
	case <-done:
	case <-time.After(3 * time.Second):
		t.Fatal("shutdown de runtime desabilitado não deveria demorar")
	}
}
