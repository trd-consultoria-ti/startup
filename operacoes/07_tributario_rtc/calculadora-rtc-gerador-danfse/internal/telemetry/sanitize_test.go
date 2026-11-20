package telemetry

import (
	"bytes"
	"context"
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/labstack/echo-otel/v5"
	"github.com/labstack/echo/v5"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/codes"
	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	metricdata "go.opentelemetry.io/otel/sdk/metric/metricdata"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.40.0"
	"go.opentelemetry.io/otel/trace"
)

// TestFilterAllowlisted verifica que a allowlist de atributos mantém exatamente
// as seis chaves configuradas e descarta toda chave controlada pelo chamador ou
// pesada em cardinalidade.
func TestFilterAllowlisted(t *testing.T) {
	allowed := []attribute.Key{
		semconv.HTTPRequestMethodKey,
		semconv.HTTPRouteKey,
		semconv.HTTPResponseStatusCodeKey,
		semconv.URLSchemeKey,
		semconv.NetworkProtocolVersionKey,
		semconv.ErrorTypeKey,
	}
	leaks := []attribute.Key{
		semconv.URLPathKey,
		semconv.ServerAddressKey,
		semconv.ServerPortKey,
		semconv.UserAgentOriginalKey,
		semconv.ClientAddressKey,
		semconv.NetworkPeerAddressKey,
		semconv.NetworkPeerPortKey,
		semconv.HTTPRequestMethodOriginalKey,
		semconv.NetworkProtocolNameKey,
	}

	all := make([]attribute.KeyValue, 0, len(allowed)+len(leaks))
	for _, k := range allowed {
		all = append(all, k.String("keep"))
	}
	for _, k := range leaks {
		all = append(all, k.String("leak"))
	}

	got := filterAllowlisted(all)
	if len(got) != len(allowed) {
		t.Fatalf("filterAllowlisted kept %d attributes, want %d: %v", len(got), len(allowed), got)
	}
	for _, kv := range got {
		if _, ok := allowlistedKeys[kv.Key]; !ok {
			t.Errorf("non-allowlisted key %q survived filtering", kv.Key)
		}
	}
}

// TestMetricAttributesAllowlist verifica que o callback de atributos de métrica
// aplica a mesma allowlist aos defaults do extractor e continua não vazio
// (assim o recorder do echo-otel não cai de volta para os defaults não
// filtrados).
func TestMetricAttributesAllowlist(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "http://example.com/api/danfse?token=QUERYSECRET", nil)
	req.Host = "client.host.invalid"
	req.Header.Set("User-Agent", "UA-LEAK-AGENT")
	req.RemoteAddr = "198.51.100.7:5555"

	v := &echootel.Values{}
	if err := v.ExtractRequest(req); err != nil {
		t.Fatalf("ExtractRequest error = %v", err)
	}
	v.HTTPRoute = "/api/danfse"

	got := metricAttributes(nil, v)
	if len(got) == 0 {
		t.Fatal("metric attributes are empty; recorder would fall back to unfiltered defaults")
	}
	set := attribute.NewSet(got...)
	for _, key := range []attribute.Key{
		semconv.HTTPRequestMethodKey,
		semconv.URLSchemeKey,
		semconv.HTTPRouteKey,
	} {
		if _, ok := set.Value(key); !ok {
			t.Errorf("allowlisted metric key %q missing", key)
		}
	}
	for _, key := range []attribute.Key{
		semconv.NetworkProtocolNameKey,       // atributo de métrica default, fora da allowlist
		semconv.URLPathKey,                   // não é produzido por MetricAttributes, mas nunca pode passar
		semconv.UserAgentOriginalKey,         // escolhido pelo chamador
		semconv.ServerAddressKey,             // vindo do Host escolhido pelo chamador
		semconv.ClientAddressKey,             // IP
		semconv.NetworkPeerAddressKey,        // IP
		semconv.HTTPRequestMethodOriginalKey, // método escolhido pelo chamador
	} {
		if _, ok := set.Value(key); ok {
			t.Errorf("non-allowlisted metric key %q present", key)
		}
	}
}

// recordingSpanExporter é um sdktrace.SpanExporter em memória.
type recordingSpanExporter struct {
	mu       sync.Mutex
	spans    []sdktrace.ReadOnlySpan
	shutdown int
}

func (e *recordingSpanExporter) ExportSpans(_ context.Context, spans []sdktrace.ReadOnlySpan) error {
	e.mu.Lock()
	defer e.mu.Unlock()
	e.spans = append(e.spans, spans...)
	return nil
}

func (e *recordingSpanExporter) Shutdown(context.Context) error {
	e.mu.Lock()
	defer e.mu.Unlock()
	e.shutdown++
	return nil
}

// TestSanitizingTraceExporterClearsStatusDescription executa um span real do
// SDK cuja description de status carrega uma mensagem sensível através do
// exporter sanitizante e verifica que a description é descartada na exportação,
// preservando o code de status (e campos delegados como o nome).
func TestSanitizingTraceExporterClearsStatusDescription(t *testing.T) {
	rec := &recordingSpanExporter{}
	san := &sanitizingTraceExporter{SpanExporter: rec}

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithResource(resource.Empty()),
		sdktrace.WithBatcher(san),
	)
	defer func() {
		_ = tp.Shutdown(context.Background())
	}()

	_, span := tp.Tracer("test").Start(
		context.Background(), "GET /api/danfse/:id",
		trace.WithAttributes(attribute.String("http.route", "/api/danfse/:id")),
	)
	span.SetStatus(codes.Error, "SENSITIVE-STATUS-DESC-77")
	span.End()
	if err := tp.Shutdown(context.Background()); err != nil {
		t.Fatalf("provider Shutdown error = %v", err)
	}

	rec.mu.Lock()
	exported := append([]sdktrace.ReadOnlySpan(nil), rec.spans...)
	rec.mu.Unlock()
	if len(exported) != 1 {
		t.Fatalf("exported %d spans, want 1", len(exported))
	}
	st := exported[0].Status()
	if st.Code != codes.Error {
		t.Errorf("status code not preserved: %v", st.Code)
	}
	if st.Description != "" {
		t.Errorf("sensitive status description leaked: %q", st.Description)
	}
	if exported[0].Name() != "GET /api/danfse/:id" {
		t.Errorf("delegation broken: span name = %q", exported[0].Name())
	}
}

// failingSpanExporter retorna um erro cru cujo texto embute uma URL de
// collector e um segredo; o sanitizador deve substituí-lo por uma mensagem
// estável.
type failingSpanExporter struct {
	recordingSpanExporter
}

func (f *failingSpanExporter) ExportSpans(context.Context, []sdktrace.ReadOnlySpan) error {
	return errors.New("collector://admin:SECRET@198.51.100.9:4317 marker-trace-error")
}

func TestSanitizingTraceExporterSanitizesExportError(t *testing.T) {
	san := &sanitizingTraceExporter{SpanExporter: &failingSpanExporter{}}
	err := san.ExportSpans(context.Background(), nil)
	if err == nil {
		t.Fatal("expected a sanitized export error")
	}
	msg := err.Error()
	if msg != "telemetry: trace export failed" {
		t.Errorf("unexpected export error message: %q", msg)
	}
	for _, leak := range []string{"SECRET", "marker-trace-error", "198.51.100.9"} {
		if strings.Contains(msg, leak) {
			t.Errorf("raw exporter error leaked %q: %q", leak, msg)
		}
	}
}

// TestSanitizingTraceExporterShutdownDelegates verifica que o Shutdown chega
// ao exporter envolvido intacto.
func TestSanitizingTraceExporterShutdownDelegates(t *testing.T) {
	rec := &recordingSpanExporter{}
	san := &sanitizingTraceExporter{SpanExporter: rec}
	if err := san.Shutdown(context.Background()); err != nil {
		t.Fatalf("Shutdown error = %v", err)
	}
	rec.mu.Lock()
	defer rec.mu.Unlock()
	if rec.shutdown != 1 {
		t.Fatalf("Shutdown not delegated: calls = %d", rec.shutdown)
	}
}

// failingMetricExporter embute (e nunca usa) a interface e só sobrescreve
// Export com um erro cru carregando um segredo.
type failingMetricExporter struct {
	sdkmetric.Exporter
}

func (f *failingMetricExporter) Export(context.Context, *metricdata.ResourceMetrics) error {
	return errors.New("metrics://user:TOPSECRET@[::1]:4318 marker-metrics-error")
}

func TestSanitizingMetricExporterSanitizesExportError(t *testing.T) {
	san := &sanitizingMetricExporter{Exporter: &failingMetricExporter{}}
	err := san.Export(context.Background(), &metricdata.ResourceMetrics{})
	if err == nil {
		t.Fatal("expected a sanitized export error")
	}
	msg := err.Error()
	if msg != "telemetry: metrics export failed" {
		t.Errorf("unexpected export error message: %q", msg)
	}
	for _, leak := range []string{"TOPSECRET", "marker-metrics-error"} {
		if strings.Contains(msg, leak) {
			t.Errorf("raw exporter error leaked %q: %q", leak, msg)
		}
	}
}

// TestShutdownSanitizesErrors verifica que Runtime.Shutdown substitui um erro
// cru de shutdown (que pode embutir uma URL de exporter) por uma mensagem
// estável baseada em rótulo.
func TestShutdownSanitizesErrors(t *testing.T) {
	const raw = "collector://user:LEAKSECRET@10.0.0.5:4317 marker-shutdown-error"
	rt := &Runtime{enabled: true, shut: []shutdownFunc{
		{label: "trace", fn: func(context.Context) error { return errors.New(raw) }},
	}}
	err := rt.Shutdown(context.Background())
	if err == nil {
		t.Fatal("expected Shutdown to report the failure")
	}
	msg := err.Error()
	if strings.Contains(msg, raw) || strings.Contains(msg, "LEAKSECRET") {
		t.Fatalf("Runtime.Shutdown leaked the raw error: %q", msg)
	}
	if !strings.Contains(msg, "shutdown trace provider failed") {
		t.Errorf("unexpected shutdown message: %q", msg)
	}
}

// TestFakeOTLPDoesNotExportSensitiveMarkers aciona o Runtime completo sobre um
// collector OTLP fake em loopback e verifica, nos payloads protobuf exportados,
// que valores controlados pelo chamador nunca aparecem enquanto os atributos de
// baixa cardinalidade da allowlist aparecem.
func TestFakeOTLPDoesNotExportSensitiveMarkers(t *testing.T) {
	markers := []string{
		"SENSITIVE-HANDLER-ERR-42", // texto cru do erro do handler (span status description)
		"UA-LEAK-AGENT",            // valor de user_agent.original
		"evil-host.invalid",        // server.address resolvido do cabeçalho Host
		"RAWTOKEN7f9f",             // url.path (caminho cru da requisição)
		"QUERYSECRET1313",          // url.query (nunca capturada)
		"198.51.100.77",            // network.peer.address / client.address (IP)
	}

	var mu sync.Mutex
	var received []string // uma entrada por payload exportado, prefixada pelo path
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		mu.Lock()
		received = append(received, r.URL.Path+"\x00"+string(b))
		mu.Unlock()
		w.Header().Set("Content-Type", "application/x-protobuf")
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	t.Setenv("OTEL_SDK_DISABLED", "false")
	t.Setenv("OTEL_EXPORTER_OTLP_ENDPOINT", srv.URL)
	t.Setenv("OTEL_TRACES_EXPORTER", "otlp")
	t.Setenv("OTEL_METRICS_EXPORTER", "otlp")

	rt, err := New(context.Background())
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}

	e := echo.New()
	e.Use(rt.Middleware())
	e.GET("/api/danfse/:id", func(c *echo.Context) error {
		return echo.NewHTTPError(http.StatusInternalServerError, "SENSITIVE-HANDLER-ERR-42")
	})

	req := httptest.NewRequest(http.MethodGet, "http://example.com/api/danfse/RAWTOKEN7f9f?token=QUERYSECRET1313", nil)
	req.Host = "evil-host.invalid"
	req.RemoteAddr = "198.51.100.77:4321"
	req.Header.Set("User-Agent", "UA-LEAK-AGENT")
	rec := httptest.NewRecorder()
	e.ServeHTTP(rec, req)

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := rt.Shutdown(ctx); err != nil {
		t.Fatalf("Shutdown() error = %v", err)
	}

	mu.Lock()
	ok := len(received) >= 1
	traces := joinPaths(received, "/v1/traces")
	metrics := joinPaths(received, "/v1/metrics")
	mu.Unlock()
	if !ok {
		t.Fatal("no export reached the fake collector")
	}

	for _, m := range markers {
		if strings.Contains(traces, m) {
			t.Errorf("sensitive marker %q leaked into the traces payload", m)
		}
		if strings.Contains(metrics, m) {
			t.Errorf("sensitive marker %q leaked into the metrics payload", m)
		}
	}

	// A allowlist também deve ser aplicada no nível de chave: nomes de chaves de
	// atributo restritas nunca devem aparecer em nenhum payload exportado.
	for _, key := range []string{
		"user_agent.original",
		"url.path",
		"server.address",
		"client.address",
		"network.peer.address",
		"http.request.method_original",
		"network.protocol.name",
	} {
		if strings.Contains(traces, key) {
			t.Errorf("restricted attribute key %q present in traces payload", key)
		}
		if strings.Contains(metrics, key) {
			t.Errorf("restricted attribute key %q present in metrics payload", key)
		}
	}

	// Valores na allowlist ainda devem ser exportados.
	for _, want := range []string{"GET", "/api/danfse/:id", "http.request.method"} {
		if !strings.Contains(traces, want) {
			t.Errorf("allowlisted value %q not found in traces payload", want)
		}
	}
	if !strings.Contains(metrics, "http.request.method") {
		t.Errorf("allowlisted http.request.method not found in metrics payload")
	}
	if !strings.Contains(metrics, "/api/danfse/:id") {
		t.Errorf("allowlisted http.route not found in metrics payload")
	}
}

// joinPaths retorna a concatenação dos registros de payload cujo prefixo
// corresponde a path, sem o prefixo.
func joinPaths(records []string, path string) string {
	var b bytes.Buffer
	for _, rec := range records {
		p, body, _ := strings.Cut(rec, "\x00")
		if p != path {
			continue
		}
		b.WriteString(body)
	}
	return b.String()
}
