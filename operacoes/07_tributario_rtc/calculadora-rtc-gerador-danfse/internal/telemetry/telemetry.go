// Package telemetry provê instrumentação OpenTelemetry opcional para o serviço
// HTTP DANFSe (Echo v5).
//
// O SDK vem DESLIGADO por padrão: a menos que OTEL_SDK_DISABLED seja
// explicitamente definido como "false", New retorna um Runtime de passagem
// sustentado por providers no-op explícitos (nunca os globais do processo).
// Nesse modo nenhum exporter é criado, nenhuma goroutine ou worker é iniciada e
// nenhuma requisição de saída é feita. Definir OTEL_SDK_DISABLED=false liga o
// SDK; qualquer outro valor (incluindo string vazia) é rejeitado como erro de
// configuração sem ecoar o valor da variável.
package telemetry

import (
	"context"
	"errors"
	"fmt"
	"os"
	"strconv"
	"time"

	"github.com/labstack/echo-otel/v5"
	"github.com/labstack/echo/v5"
	echo_middleware "github.com/labstack/echo/v5/middleware"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetrichttp"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/metric"
	metric_noop "go.opentelemetry.io/otel/metric/noop"
	"go.opentelemetry.io/otel/propagation"
	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	metricdata "go.opentelemetry.io/otel/sdk/metric/metricdata"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	"go.opentelemetry.io/otel/semconv/v1.40.0"
	"go.opentelemetry.io/otel/trace"
)

// DefaultServiceName é o valor usado para o atributo de recurso service.name
// quando OTEL_SERVICE_NAME não está definido.
const DefaultServiceName = "danfse-service"

// LivePath e ReadyPath são as rotas de probe de health que o middleware sempre
// pula (nunca devem ser rastreadas nem exportadas). Manter em sincronia com as
// rotas registradas no router HTTP.
const (
	LivePath  = "/health/live"
	ReadyPath = "/health/ready"
)

// Valores suportados por OTEL_TRACES_EXPORTER / OTEL_METRICS_EXPORTER.
const (
	exporterOTLP = "otlp"
	exporterNone = "none"
)

// Runtime é o dono do runtime (opcional) de telemetria.
//
// Um Runtime desabilitado expõe exatamente a mesma API, mas sustenta cada sinal
// com um provider no-op explícito: portanto nunca conecta a um collector, nunca
// inicia workers, nunca toca o estado global do OpenTelemetry e o seu Shutdown
// é um no-op.
type Runtime struct {
	enabled bool

	tp trace.TracerProvider
	mp metric.MeterProvider

	mw   echo.MiddlewareFunc
	shut []shutdownFunc
}

// shutdownFunc é um shutdown de provider vinculado a um rótulo estável. O
// rótulo (nunca o texto do erro retornado) é o único detalhe que Shutdown
// reporta, de modo que erros crus de exporter não vazam via Runtime.Shutdown.
type shutdownFunc struct {
	label string
	fn    func(context.Context) error
}

// New constrói o runtime de telemetria a partir das variáveis de ambiente
// padrão do OpenTelemetry.
//
// Configuração (veja também a spec do OpenTelemetry SDK):
//   - OTEL_SDK_DISABLED: controla o SDK. Ausente => desabilitado (padrão),
//     "true" => desabilitado, "false" => habilitado, qualquer outro valor =>
//     erro de configuração. Valores sensíveis nunca entram nas mensagens de
//     erro.
//   - OTEL_TRACES_EXPORTER / OTEL_METRICS_EXPORTER: "otlp" (padrão quando o
//     SDK está habilitado) ou "none". Qualquer outro valor => erro de
//     configuração.
//   - OTEL_SERVICE_NAME: padrão "danfse-service".
//   - OTEL_EXPORTER_OTLP_ENDPOINT, *_TRACES_ENDPOINT, *_METRICS_ENDPOINT,
//     *_HEADERS: consumidas pelos construtores padrão de exporters do SDK;
//     apenas o transporte HTTP/protobuf é usado.
//
// Com o SDK habilitado, exporters são construídos apenas para os sinais cujo
// exporter é "otlp"; "none" mantém um provider SDK em processo funcional sem
// exporter (sem goroutines, sem requisições).
//
// Se a configuração falhar no meio do caminho, todo provider/exporter já
// criado é encerrado antes de o erro ser retornado.
func New(ctx context.Context) (*Runtime, error) {
	disabled, err := sdkDisabled()
	if err != nil {
		return nil, err
	}
	if disabled {
		return newDisabledRuntime()
	}

	tracesExporter, metricsExporter, err := exporterSelection()
	if err != nil {
		return nil, err
	}

	res, err := newResource(ctx)
	if err != nil {
		return nil, err
	}

	rt := &Runtime{enabled: true}

	// Provider de trace primeiro: assim uma falha de métrica também limpa o
	// provider de trace.
	tp, err := newTraceProvider(ctx, res, tracesExporter)
	if err != nil {
		cleanup(rt.shut)
		return nil, err
	}
	rt.tp = tp
	rt.shut = append(rt.shut, shutdownFunc{label: "trace", fn: tp.Shutdown})

	mp, err := newMeterProvider(ctx, res, metricsExporter)
	if err != nil {
		cleanup(rt.shut)
		return nil, err
	}
	rt.mp = mp
	rt.shut = append(rt.shut, shutdownFunc{label: "metric", fn: mp.Shutdown})

	mw, err := buildMiddleware(rt.tp, rt.mp, propagation.TraceContext{})
	if err != nil {
		cleanup(rt.shut)
		return nil, fmt.Errorf("telemetry: build middleware: %w", err)
	}
	rt.mw = mw

	return rt, nil
}

// Middleware retorna o middleware Echo que instrumenta toda requisição que não
// pula. Providers, meters e propagators são passados via sua configuração; os
// providers globais do OpenTelemetry nunca são tocados.
func (r *Runtime) Middleware() echo.MiddlewareFunc {
	if r == nil {
		return func(next echo.HandlerFunc) echo.HandlerFunc { return next }
	}
	return r.mw
}

// Shutdown faz o flush e libera os recursos do SDK (batch span processor e
// periodic metric reader), limitado por ctx. Falha de exportação é reportada,
// mas nunca gera panics. Apenas mensagens estáveis, baseadas em rótulos, são
// reportadas: erros crus de exporter nunca são propagados. Um runtime
// desabilitado (ou nil) é um no-op.
func (r *Runtime) Shutdown(ctx context.Context) error {
	if r == nil || r.enabled == false || len(r.shut) == 0 {
		return nil
	}
	var errs []error
	for _, step := range r.shut {
		if err := step.fn(ctx); err != nil {
			errs = append(errs, fmt.Errorf("telemetry: shutdown %s provider failed", step.label))
		}
	}
	r.shut = nil
	return errors.Join(errs...)
}

// newDisabledRuntime retorna um runtime de passagem usando providers no-op
// explícitos, de modo que o caminho desabilitado nunca toca os globais do
// processo nem cria qualquer worker.
func newDisabledRuntime() (*Runtime, error) {
	tp := trace.NewNoopTracerProvider()
	mp := metric_noop.NewMeterProvider()
	mw, err := buildMiddleware(tp, mp, propagation.TraceContext{})
	if err != nil {
		return nil, fmt.Errorf("telemetry: build middleware: %w", err)
	}
	return &Runtime{tp: tp, mp: mp, mw: mw}, nil
}

// buildMiddleware monta o middleware de instrumentação com os providers
// explicitamente injetados pela configuração. Os atributos de início de span,
// fim de span e de métricas passam todos pela mesma allowlist explícita, de
// modo que nada fora desse conjunto chega ao collector.
func buildMiddleware(tp trace.TracerProvider, mp metric.MeterProvider, propagators propagation.TextMapPropagator) (echo.MiddlewareFunc, error) {
	cfg := echootel.Config{
		TracerProvider:      tp,
		MeterProvider:       mp,
		Propagators:         propagators,
		Skipper:             skipProbes,
		SpanStartAttributes: spanAttributes,
		SpanEndAttributes:   spanAttributes,
		MetricAttributes:    metricAttributes,
	}
	return cfg.ToMiddleware()
}

// skipProbes pula as rotas de health check.
func skipProbes(c *echo.Context) bool {
	switch c.Path() {
	case LivePath, ReadyPath:
		return true
	}
	return false
}

// allowlistedKeys é a allowlist explícita de atributos de span e métricas que
// podem chegar ao collector. Todo atributo fora desse conjunto é descartado:
//
//   - url.path / http.request.method_original: o caminho e o método escolhidos
//     pelo chamador. Uma chave de acesso da NFSe poderia legitimamente aparecer
//     em um caminho; http.route já carrega a rota Echo de baixa cardinalidade.
//   - user_agent.original: escolhido pelo chamador, alta cardinalidade.
//   - server.address / server.port: resolvidos a partir do cabeçalho Host
//     escolhido pelo chamador e, portanto, não confiáveis.
//   - network.peer.address / network.peer.port / client.address: endereços IP
//     do peer e do cliente (incluindo cabeçalhos de proxy forjados).
//   - network.protocol.name: não está na allowlist; network.protocol.version
//     está.
//
// url.query nunca é capturada pelo middleware (conforme sua doc: a query string
// pode conter dados sensíveis). Payloads de requisição (XML com dados fiscais)
// nunca são lidos nem logados.
var allowlistedKeys = map[attribute.Key]struct{}{
	semconv.HTTPRequestMethodKey:      {},
	semconv.HTTPRouteKey:              {},
	semconv.HTTPResponseStatusCodeKey: {},
	semconv.URLSchemeKey:              {},
	semconv.NetworkProtocolVersionKey: {},
	semconv.ErrorTypeKey:              {},
}

// filterAllowlisted retorna apenas os atributos cuja chave está em
// allowlistedKeys. O array subjacente do slice de entrada é reutilizado
// in-place.
func filterAllowlisted(attr []attribute.KeyValue) []attribute.KeyValue {
	out := attr[:0]
	for _, kv := range attr {
		if _, ok := allowlistedKeys[kv.Key]; ok {
			out = append(out, kv)
		}
	}
	return out
}

// spanAttributes aplica a allowlist aos atributos de início e fim de span.
// Ambos os callbacks compartilham a assinatura echootel.AttributesFunc.
func spanAttributes(_ *echo.Context, _ *echootel.Values, attr []attribute.KeyValue) []attribute.KeyValue {
	return filterAllowlisted(attr)
}

// metricAttributes aplica a mesma allowlist aos atributos de métrica de baixa
// cardinalidade derivados do extractor. Os defaults nunca contêm dados de host,
// IP ou user-agent, mas a allowlist garante que uma mudança nos defaults a
// montante não consiga infiltrar um atributo controlado pelo chamador em uma
// dimensão de métrica. O resultado filtrado nunca é vazio para uma requisição
// real (método, scheme, rota e versão de protocolo estão todos na allowlist),
// de modo que o recorder do echo-otel não cai de volta para os defaults não
// filtrados.
func metricAttributes(_ *echo.Context, v *echootel.Values) []attribute.KeyValue {
	return filterAllowlisted(v.MetricAttributes())
}

// sdkDisabled resolve OTEL_SDK_DISABLED. Ausente significa desabilitado (o
// default do serviço é OFF). Qualquer valor inválido é um erro de configuração
// cuja mensagem não ecoa o valor cru.
func sdkDisabled() (bool, error) {
	raw, ok := os.LookupEnv("OTEL_SDK_DISABLED")
	if !ok {
		return true, nil
	}
	v, err := strconv.ParseBool(raw)
	if err != nil {
		return false, errors.New("telemetry: invalid OTEL_SDK_DISABLED value; expected \"true\" or \"false\"")
	}
	return v, nil
}

// exporterSelection resolve OTEL_TRACES_EXPORTER e OTEL_METRICS_EXPORTER.
// Ambos têm default "otlp" quando o SDK está habilitado; "none" desabilita o
// exporter de um dado sinal; qualquer outro valor é um erro de configuração (o
// valor ofensor não é ecoado).
func exporterSelection() (traces, metrics string, err error) {
	traces, err = signalExporter("OTEL_TRACES_EXPORTER")
	if err != nil {
		return "", "", err
	}
	metrics, err = signalExporter("OTEL_METRICS_EXPORTER")
	if err != nil {
		return "", "", err
	}
	return traces, metrics, nil
}

func signalExporter(key string) (string, error) {
	raw, ok := os.LookupEnv(key)
	if !ok {
		return exporterOTLP, nil
	}
	switch raw {
	case exporterOTLP, exporterNone:
		return raw, nil
	}
	return "", fmt.Errorf("telemetry: unsupported %s value; use %q or %q", key, exporterOTLP, exporterNone)
}

// newResource constrói o resource. service.name vem de OTEL_SERVICE_NAME
// (default danfse-service); OTEL_RESOURCE_ATTRIBUTES é respeitada via
// resource.WithFromEnv.
func newResource(ctx context.Context) (*resource.Resource, error) {
	serviceName := os.Getenv("OTEL_SERVICE_NAME")
	if serviceName == "" {
		serviceName = DefaultServiceName
	}
	res, err := resource.New(ctx,
		resource.WithFromEnv(),
		resource.WithAttributes(semconv.ServiceName(serviceName)),
		resource.WithTelemetrySDK(),
	)
	if err != nil {
		// resource.New pode expor erros crus de parse de
		// OTEL_RESOURCE_ATTRIBUTES; nunca os propague (poderiam ecoar valores
		// fornecidos pelo chamador).
		return nil, errors.New("telemetry: build resource failed")
	}
	return res, nil
}

func newTraceProvider(ctx context.Context, res *resource.Resource, exporter string) (*sdktrace.TracerProvider, error) {
	switch exporter {
	case exporterNone:
		return sdktrace.NewTracerProvider(sdktrace.WithResource(res)), nil
	default: // otlp
		var (
			exp sdktrace.SpanExporter
			err error
		)
		exp, err = otlptracehttp.New(ctx)
		if err != nil {
			return nil, errors.New("telemetry: create OTLP trace exporter failed")
		}
		exp = &sanitizingTraceExporter{SpanExporter: exp}
		return sdktrace.NewTracerProvider(
			sdktrace.WithResource(res),
			sdktrace.WithBatcher(exp),
		), nil
	}
}

func newMeterProvider(ctx context.Context, res *resource.Resource, exporter string) (*sdkmetric.MeterProvider, error) {
	switch exporter {
	case exporterNone:
		return sdkmetric.NewMeterProvider(sdkmetric.WithResource(res)), nil
	default: // otlp
		var (
			exp sdkmetric.Exporter
			err error
		)
		exp, err = otlpmetrichttp.New(ctx)
		if err != nil {
			return nil, errors.New("telemetry: create OTLP metric exporter failed")
		}
		exp = &sanitizingMetricExporter{Exporter: exp}
		return sdkmetric.NewMeterProvider(
			sdkmetric.WithResource(res),
			sdkmetric.WithReader(sdkmetric.NewPeriodicReader(exp)),
		), nil
	}
}

// cleanup faz shutdown best-effort dos recursos já criados quando New falha no
// meio do caminho. Um contexto novo com timeout garante progresso mesmo que o
// contexto do chamador já tenha sido cancelado.
func cleanup(shutdowns []shutdownFunc) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	for _, step := range shutdowns {
		_ = step.fn(ctx)
	}
}

// --- sanitização de exporters ---
//
// O SDK reporta falhas de exportação através do seu error handler (global) e
// dos erros de Shutdown dos providers. Erros crus de exporter podem embutir a
// URL do collector e detalhes de conexão; por isso os exporters de trace e de
// métrica são envolvidos para substituir seus erros de exportação por mensagens
// estáveis (nunca um %w do erro cru). O exporter OTLP de trace é ainda
// envolvido para descartar a Status Description do span, que o middleware
// preenche com o texto cru do erro do handler. Um error handler global do OTel
// é propositalmente NÃO instalado: os erros sanitizados de exporter são tudo o
// que o handler default pode observar.

// sanitizingSpan é um wrapper de sdktrace.ReadOnlySpan que limpa a Status
// Description (o texto cru do erro definido pelo middleware) preservando o code
// de status e todos os demais campos do span (que são delegados).
type sanitizingSpan struct {
	sdktrace.ReadOnlySpan
}

func (s sanitizingSpan) Status() sdktrace.Status {
	st := s.ReadOnlySpan.Status()
	st.Description = ""
	return st
}

// sanitizingTraceExporter é um wrapper de sdktrace.SpanExporter que, antes de
// delegar ao exporter subjacente, envolve todo span exportado para descartar a
// Status Description e substitui qualquer erro cru de exporter por uma mensagem
// estável. Shutdown delega intacto.
type sanitizingTraceExporter struct {
	sdktrace.SpanExporter
}

func (e *sanitizingTraceExporter) ExportSpans(ctx context.Context, spans []sdktrace.ReadOnlySpan) error {
	wrapped := make([]sdktrace.ReadOnlySpan, len(spans))
	for i, s := range spans {
		wrapped[i] = sanitizingSpan{ReadOnlySpan: s}
	}
	if err := e.SpanExporter.ExportSpans(ctx, wrapped); err != nil {
		return errors.New("telemetry: trace export failed")
	}
	return nil
}

// sanitizingMetricExporter é um wrapper de sdkmetric.Exporter que apenas
// substitui o erro cru de Export por uma mensagem estável. Temporality,
// Aggregation, ForceFlush e Shutdown delegam ao exporter embutido.
type sanitizingMetricExporter struct {
	sdkmetric.Exporter
}

func (e *sanitizingMetricExporter) Export(ctx context.Context, rm *metricdata.ResourceMetrics) error {
	if err := e.Exporter.Export(ctx, rm); err != nil {
		return errors.New("telemetry: metrics export failed")
	}
	return nil
}

// Garantias de interface em tempo de compilação.
var (
	_ echo_middleware.Skipper = skipProbes
	_ sdktrace.SpanExporter   = (*sanitizingTraceExporter)(nil)
	_ sdkmetric.Exporter      = (*sanitizingMetricExporter)(nil)
)
