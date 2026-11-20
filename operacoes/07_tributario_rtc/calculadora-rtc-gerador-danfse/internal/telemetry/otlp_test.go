package telemetry

import (
	"context"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	"github.com/labstack/echo/v5"
)

// TestFakeOTLPExport verifica que o runtime habilitado exporta spans (e
// métricas) para os endpoints HTTP OTLP via servidor loopback, sem rede
// externa. Shutdown faz o flush do batch span processor e do periodic metric
// reader.
func TestFakeOTLPExport(t *testing.T) {
	var mu sync.Mutex
	var received []string

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		received = append(received, r.URL.Path)
		mu.Unlock()
		w.Header().Set("Content-Type", "application/x-protobuf")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write(nil)
	}))
	defer srv.Close()

	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_EXPORTER_OTLP_ENDPOINT", srv.URL) // exporter default = otlp

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}

	e := echo.New()
	e.Use(rt.Middleware())
	e.POST("/api/danfse", func(c *echo.Context) error {
		return c.String(http.StatusOK, "ok")
	})

	rec := doRequest(e, http.MethodPost, "/api/danfse", nil)
	if rec.Code != http.StatusOK {
		t.Fatalf("handler status = %d, want 200", rec.Code)
	}

	// Flush + exportação final acontecem no shutdown.
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := rt.Shutdown(ctx); err != nil {
		t.Fatalf("Shutdown() error = %v", err)
	}

	mu.Lock()
	paths := append([]string(nil), received...)
	mu.Unlock()

	has := func(p string) bool {
		for _, got := range paths {
			if got == p {
				return true
			}
		}
		return false
	}
	if !has("/v1/traces") {
		t.Fatalf("expected an export to /v1/traces, got %v", paths)
	}
	if !has("/v1/metrics") {
		t.Fatalf("expected an export to /v1/metrics, got %v", paths)
	}
}

// TestExportFailureDoesNotBlockRequest verifica que um collector inalcançável
// nunca bloqueia a requisição HTTP em si: o middleware retorna normalmente e a
// falha só aparece no Shutdown.
func TestExportFailureDoesNotBlockRequest(t *testing.T) {
	// Aponta o exporter para um listener fechado para que qualquer tentativa de
	// exportação falhe rapidamente.
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))
	url := srv.URL
	srv.Close() // listener closed: connection refused

	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_EXPORTER_OTLP_ENDPOINT", url)
	t.Setenv("OTEL_TRACES_EXPORTER", "otlp")
	t.Setenv("OTEL_METRICS_EXPORTER", "otlp")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}

	e := echo.New()
	e.Use(rt.Middleware())
	e.POST("/api/danfse", func(c *echo.Context) error {
		return c.String(http.StatusOK, "ok")
	})

	start := time.Now()
	rec := doRequest(e, http.MethodPost, "/api/danfse", nil)
	if rec.Code != http.StatusOK {
		t.Fatalf("handler status = %d, want 200", rec.Code)
	}
	// Mesmo com um collector morto inalcançável durante a requisição, o handler
	// não deve esperar por uma exportação.
	if elapsed := time.Since(start); elapsed > 3*time.Second {
		t.Fatalf("request blocked on telemetry export for %s", elapsed)
	}

	// O Shutdown pode reportar as falhas de exportação e deve permanecer
	// bounded.
	sctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = rt.Shutdown(sctx) // sem asserção: a falha é esperada e tolerada
}
