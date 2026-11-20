package telemetry

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/labstack/echo/v5"
	"go.opentelemetry.io/otel/baggage"
	"go.opentelemetry.io/otel/trace"
)

// newTestEcho retorna uma instância de Echo com o middleware do runtime
// registrado e as rotas de probe + negócio cadastradas, capturando o span visto
// pelos handlers.
func newTestEcho(t *testing.T, rt *Runtime) (*echo.Echo, *[]trace.SpanContext) {
	t.Helper()
	captured := new([]trace.SpanContext)

	e := echo.New()
	e.Use(rt.Middleware())

	handler := func(name string, s *[]trace.SpanContext) func(*echo.Context) error {
		return func(c *echo.Context) error {
			sc := trace.SpanFromContext(c.Request().Context()).SpanContext()
			*s = append(*s, sc)
			return c.String(http.StatusOK, name)
		}
	}

	e.GET(LivePath, handler("live", captured))
	e.GET(ReadyPath, handler("ready", captured))
	e.POST("/api/danfse", handler("danfse", captured))

	return e, captured
}

// doRequest executa uma requisição pelo echo sem abrir socket de rede.
func doRequest(e *echo.Echo, method, path string, header http.Header) *httptest.ResponseRecorder {
	var req *http.Request
	if method == http.MethodPost {
		req = httptest.NewRequest(method, "http://example.com"+path, strings.NewReader(`{}`))
	} else {
		req = httptest.NewRequest(method, "http://example.com"+path, nil)
	}
	for k, vs := range header {
		for _, v := range vs {
			req.Header.Add(k, v)
		}
	}
	rec := httptest.NewRecorder()
	e.ServeHTTP(rec, req)
	return rec
}

// spanIsValid informa se o contexto de span é amostrado/válido.
func spanIsValid(sc trace.SpanContext) bool {
	return sc.IsValid() && sc.IsSampled()
}

func TestNewDisabledByDefault(t *testing.T) {
	// Nenhuma env OTEL_* presente: o SDK deve estar OFF e mesmo assim New deve
	// ter sucesso.
	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	if rt.enabled != false {
		t.Fatalf("runtime not disabled by default")
	}
	if rt.Middleware() == nil {
		t.Fatalf("nil middleware for disabled runtime")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()
	if err := rt.Shutdown(ctx); err != nil {
		t.Fatalf("Shutdown() error = %v", err)
	}
}

func TestNewDisabledNeverConnects(t *testing.T) {
	// Mesmo com um endpoint OTLP configurado, o runtime desabilitado não deve
	// criar exporters nem fazer nenhuma requisição de saída.
	var hits int
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		hits++
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	t.Setenv("OTEL_EXPORTER_OTLP_ENDPOINT", srv.URL)
	t.Setenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", srv.URL+"/v1/traces")
	t.Setenv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", srv.URL+"/v1/metrics")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}

	e, captured := newTestEcho(t, rt)
	for _, p := range []string{LivePath, ReadyPath, "/api/danfse"} {
		doRequest(e, http.MethodGet, p, nil)
		doRequest(e, http.MethodPost, p, nil)
	}
	_ = captured

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()
	if err := rt.Shutdown(ctx); err != nil {
		t.Fatalf("Shutdown() error = %v", err)
	}
	if hits != 0 {
		t.Fatalf("disabled runtime made %d outbound request(s)", hits)
	}
}

func TestNewExplicitDisabledIgnoresExporterEnv(t *testing.T) {
	// Enquanto desabilitado, as env vars de seleção de exporter não são
	// consultadas (nenhum exporter é criado); apenas um OTEL_SDK_DISABLED
	// inválido é erro nesse caminho.
	t.Setenv("OTEL_SDK_DISABLED", "true")
	t.Setenv("OTEL_TRACES_EXPORTER", "bogus-collector")
	t.Setenv("OTEL_METRICS_EXPORTER", "nope")
	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	if rt.enabled != false {
		t.Fatalf("expected disabled runtime")
	}
}

func TestNewEnabledWithNoneExporters(t *testing.T) {
	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_TRACES_EXPORTER", "none")
	t.Setenv("OTEL_METRICS_EXPORTER", "none")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	if !rt.enabled {
		t.Fatalf("expected enabled runtime")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := rt.Shutdown(ctx); err != nil {
		t.Fatalf("Shutdown() error = %v", err)
	}
}

func TestNewInvalidSDKDisabledValue(t *testing.T) {
	for _, val := range []string{"bogus", "", "yes"} {
		t.Setenv("OTEL_SDK_DISABLED", val)
		_, err := New(context.Background())
		if err == nil {
			t.Fatalf("New() expected error for OTEL_SDK_DISABLED=%q", val)
		}
		if strings.Contains(err.Error(), val) && val != "" {
			t.Fatalf("error message leaked the raw env value %q: %v", val, err)
		}
	}
}

func TestNewInvalidExporterValue(t *testing.T) {
	t.Setenv("OTEL_SDK_DISABLED", "false")
	for _, env := range []string{"OTEL_TRACES_EXPORTER", "OTEL_METRICS_EXPORTER"} {
		t.Setenv(env, "jaeger")
		_, err := New(context.Background())
		if err == nil {
			t.Fatalf("New() expected error for %s=jaeger", env)
		}
		t.Setenv(env, "otlp")
	}
}

func TestShutdownBoundByContext(t *testing.T) {
	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_TRACES_EXPORTER", "none")
	t.Setenv("OTEL_METRICS_EXPORTER", "none")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	// Um contexto já cancelado não deve bloquear o Shutdown indefinidamente.
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	done := make(chan error, 1)
	go func() { done <- rt.Shutdown(ctx) }()
	select {
	case <-done:
		// tenha retornado o que tiver, ele retornou.
	case <-time.After(5 * time.Second):
		t.Fatalf("Shutdown did not honor a cancelled context")
	}
}

func TestMiddlewareSkipsHealthProbes(t *testing.T) {
	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_TRACES_EXPORTER", "none")
	t.Setenv("OTEL_METRICS_EXPORTER", "none")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	defer func() {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = rt.Shutdown(ctx)
	}()

	e, captured := newTestEcho(t, rt)

	// Probes de health devem ser puladas: nenhum span é iniciado para elas.
	doRequest(e, http.MethodGet, LivePath, nil)
	doRequest(e, http.MethodGet, ReadyPath, nil)
	// Uma rota de negócio deve ser rastreada: um span amostrado válido deve
	// estar presente.
	doRequest(e, http.MethodPost, "/api/danfse", nil)

	if len(*captured) != 3 {
		t.Fatalf("expected 3 handler invocations, got %d", len(*captured))
	}
	probes := (*captured)[0:2]
	business := (*captured)[2]
	for i, sc := range probes {
		if spanIsValid(sc) {
			t.Fatalf("probe %d unexpectedly traced: %s", i, sc.TraceID())
		}
	}
	if !spanIsValid(business) {
		t.Fatalf("business route not traced: %s", business.TraceID())
	}
}

func TestDisabledMiddlewareNeverTraces(t *testing.T) {
	rt, err := New(context.Background()) // disabled by default
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}

	e, captured := newTestEcho(t, rt)
	doRequest(e, http.MethodGet, LivePath, nil)
	doRequest(e, http.MethodGet, ReadyPath, nil)
	doRequest(e, http.MethodPost, "/api/danfse", nil)

	for i, sc := range *captured {
		if spanIsValid(sc) {
			t.Fatalf("request %d traced with disabled runtime: %s", i, sc.TraceID())
		}
	}
}

func TestMiddlewarePropagatesTraceContext(t *testing.T) {
	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_TRACES_EXPORTER", "none")
	t.Setenv("OTEL_METRICS_EXPORTER", "none")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	defer func() {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = rt.Shutdown(ctx)
	}()

	traceID := trace.TraceID{}
	traceID[0] = 0xAB
	spanID := trace.SpanID{1, 2, 3, 4, 5, 6, 7, 8}
	traceparent := "00-" + traceID.String() + "-" + spanID.String() + "-01"

	e := echo.New()
	e.Use(rt.Middleware())

	// Captura tanto o span quanto a baggage context vista pelo handler.
	var got trace.SpanContext
	var baggageLen int
	e.GET("/api/danfse", func(c *echo.Context) error {
		ctx := c.Request().Context()
		got = trace.SpanFromContext(ctx).SpanContext()
		baggageLen = baggage.FromContext(ctx).Len() // nenhum consumidor de baggage registrado
		return c.String(http.StatusOK, "ok")
	})

	header := http.Header{}
	// Os propagators são apenas TraceContext: o cabeçalho baggage não deve ser
	// lido.
	header.Set("traceparent", traceparent)
	header.Set("baggage", "user=sensitive-pii")
	doRequest(e, http.MethodGet, "/api/danfse", header)

	if got.TraceID() != traceID {
		t.Fatalf("trace ID not propagated: want %s got %s", traceID, got.TraceID())
	}
	if baggageLen != 0 {
		t.Fatalf("baggage was propagated even though only TraceContext is wired: %d entries", baggageLen)
	}
}
