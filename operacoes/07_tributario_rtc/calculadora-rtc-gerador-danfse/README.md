# DANFSe Service

Serviço HTTP em Go para gerar o DANFSe em PDF a partir do XML de uma NFS-e.

## Pré-requisitos

- Go 1.27+
- [Task](https://taskfile.dev/) — opcional, mas recomendado

## Executar

Com Task:

```bash
task run
```

Sem Task:

```bash
go run ./cmd/danfse
```

Por padrão, o servidor inicia na porta `8080`. Para usar outra porta:

```bash
SERVER_PORT=8081 go run ./cmd/danfse
```

Também é possível definir `SERVER_PORT` em um arquivo `.env`, que é carregado
automaticamente pelo Taskfile.

## Docker
Gerar imagem local, execute na raiz do projeto:
```bash
docker build -t danfse-go .
```

Rodar o container localmente:
```bash
docker run -p 8080:8080 danfse-go:latest
```

## Configuração

Variáveis de ambiente suportadas:

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Porta HTTP do servidor. |
| `LOG_LEVEL` | `info` | Nível mínimo de log (JSON em stderr). Valores: `debug`, `info`, `warn`, `error` (case-insensitive). Valor inválido rejeita a inicialização. |
| `XML_MAX_SIZE_BYTES` | `5242880` | Limite máximo (em bytes) do XML de entrada, aplicado tanto ao limite de corpo HTTP quanto à validação interna do XML (antes e depois da sanitização do `&`). Valores zero, negativos, não numéricos ou acima do teto de `33554432` (32 MiB) rejeitam a inicialização. |

```bash
XML_MAX_SIZE_BYTES=1048576 go run ./cmd/danfse
```

> **Telemetria (OTel):** a inicialização do SDK é **opt-in** e controlada
> exclusivamente pelas variáveis `OTEL_*` (ver [Tracing (OTLP)](#tracing-otlp-opt-in)).
> Não há variável própria de telemetria — com `OTEL_SDK_DISABLED=true` (padrão),
> o runtime é um pass-through no-op.

## Logs (JSON em stderr)

O serviço emite logs estruturados **JSON em stderr** (stdout fica reservado ao
processo), um único registro por requisição com o evento `request_completed`,
gerado pelo **RequestLogger nativo do Echo** (sem middleware próprio de
logging/status/timer):

```json
{
  "time": "2026-10-09T17:00:22.987471-03:00",
  "level": "INFO",
  "msg": "request_completed",
  "timestamp": "2026-10-09T20:00:22.987471Z",
  "event": "request_completed",
  "request_id": "43cc0078e1038f585eafcaf15f862d93",
  "method": "POST",
  "route": "/api/danfse",
  "status": 200,
  "duration": 21816
}
```

Campos relevantes:

- `request_id` — identifica a requisição e é **sempre gerado pelo servidor**:
  o header `X-Request-ID` enviado pelo cliente é descartado antes do middleware
  (um ID externo não é confiável — pode carregar chars inválidos ou conteúdo
  fiscal) e **nunca é ecoado nem propagado**. O cabeçalho de resposta
  `X-Request-ID` traz sempre um identificador novo (32 chars alfanuméricos
  aleatórios).
- `route` — rota registrada (ex.: `/api/danfse`); requisições sem rota usam o
  valor constante `"unmatched"`.
- `trace_id` / `span_id` — presentes quando o OTLP está habilitado.
- `error_type` — código estável de erro: `aborted`, `panic`, `http_error` ou
  `internal`.
- `kind: "probe"` — marcado nos health checks; probes de `/health/*` são
  logados em nível `debug` (não poluem o `info`).

**Nunca são logados**: corpo do XML, PDF, querystring, IDs fiscais ou headers de
autenticação. Respostas `5xx` são sempre genéricas e não revelam `err.Error()`.
O nível mínimo de log é controlado por `LOG_LEVEL` (ver
[Configuração](#configuração)).

No Docker, os logs ficam em stderr e podem ser consultados normalmente:

```bash
docker logs -f <container>
journalctl -u danfse-service -f   # systemd
```

O campo `msg` do JSON é sempre o evento; para filtros por correlação use
`request_id`.

## Health Checks / ciclo de vida

| Rota | Comportamento |
| --- | --- |
| `GET /health/live` | `200` com `{"status":"ok"}` enquanto o processo estiver de pé. |
| `GET /health/ready` | `200` com `{"status":"ok"}` quando pronto; `503` com `{"status":"unavailable"}` durante o graceful shutdown. |

Durante o graceful shutdown, o serviço marca o estado como **não pronto**
**ANTES** de aguardar as requisições em andamento, para que balanceadores de
carga parem de encaminhar tráfego antes do `Shutdown` HTTP. Útil como
`readinessProbe` em Kubernetes/ECS e como healthcheck do load balancer.

Os endpoints de health são documentados no Swagger sob a tag `Health`.

## Tracing (OTLP, opt-in)

A telemetria segue o padrão OpenTelemetry e é desligada por padrão. Para
habilitar:

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `OTEL_SDK_DISABLED` | `true` | `false` habilita o SDK; `true` mantém providers no-op, sem exportadores nem conexões ao Collector. |
| `OTEL_SERVICE_NAME` | `danfse-service` | Nome do serviço nos spans. |
| `OTEL_TRACES_EXPORTER` | `otlp` | Quando habilitado: `otlp` exporta traces; `none` não exporta. |
| `OTEL_METRICS_EXPORTER` | `otlp` | Quando habilitado: `otlp` exporta métricas; `none` não exporta. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | URL base do Collector OTLP HTTP/protobuf; os exporters acrescentam `/v1/traces` e `/v1/metrics`. |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | endpoint base + `/v1/traces` | URL completa opcional para traces. |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | endpoint base + `/v1/metrics` | URL completa opcional para métricas. |
| `OTEL_EXPORTER_OTLP_HEADERS` | `-` | Headers separados por vírgula, ex.: `Authorization=Bearer xyz`. |
| `OTEL_EXPORTER_OTLP_TIMEOUT` | `10000` | Timeout (ms) de cada export. |

Exemplo:

```bash
OTEL_SDK_DISABLED=false \
OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4318 \
OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer token" \
go run ./cmd/danfse
```

Comportamento:

- O transporte implementado é **OTLP HTTP/protobuf**, não gRPC. O Collector e
  o backend de armazenamento/visualização são responsabilidade de quem hospeda
  a aplicação. Logs JSON continuam em stderr; não são exportados via OTLP.
- **Configuração OTel explícita inválida falha o startup** (fail-fast): com
  `OTEL_SDK_DISABLED=false` e variáveis `OTEL_*` malformadas, o processo inicia
  e sai com erro (mensagem segura, sem ecoar URLs/credenciais do endpoint).
- Falhas de **export** são assíncronas e **não despacham erros ao responder a
  requisição** nem derrubam o processo; ao encerrar, são reportadas em log de
  nível `warn` com mensagem segura.
- Os probes `/health/live` e `/health/ready` são excluídos dos spans/telemetria.
- Span context propagado pelo HTTP é usado para preencher `trace_id`/`span_id`
  no log de conclusão quando presente.

## Nota sobre classificação de erros (`4xx` vs `5xx`)

O endpoint avalia o XML e retorna `400` com mensagem voltada ao cliente para
entradas inválidas detectadas pelo serviço (ex.: XML com erros estruturais,
`stc` inválido). **Classificação fina entre `400` e `5xx`** do serviço de
geração está na fila de melhorias como follow-up — hoje erros internos do
serviço também são mapeados para `400`; a intenção é tratar falhas de
infraestrutura/processamento como `5xx` (com corpo genérico) sem quebrar o
contrato atual do cliente.


## Gerar um PDF

Com o servidor em execução:

```bash
curl -X POST "http://localhost:8080/api/danfse" \
  -H "Content-Type: application/xml" \
  --data-binary @/caminho/para/documento.xml \
  -o danfse.pdf
```

O endpoint aceita os seguintes parâmetros opcionais:

- `stc`: situação da nota — `0` válida, `1` substituída ou `2` cancelada;
- `canhoto`: `true` por padrão; use `false` para ocultar o canhoto.

Exemplo sem canhoto:

```bash
curl -X POST "http://localhost:8080/api/danfse?canhoto=false" \
  -H "Content-Type: application/xml" \
  --data-binary @/caminho/para/documento.xml \
  -o danfse.pdf
```

O XML enviado tem, por padrão, no máximo 5 MiB; no servidor HTTP, o limite é
configurável pela variável `XML_MAX_SIZE_BYTES` (ver [Configuração](#configuração)).
O WASM mantém o padrão compartilhado de 5 MiB.

## Testes

```bash
task check
```

Executa `go fmt` e `go vet`. Para rodar os testes separadamente:

```bash
task test
```

Ou diretamente:

```bash
go test -race -count=1 ./...
```

Para executar a suíte, mostrar a cobertura de statements por função e gerar o
relatório HTML local (`coverage.html`):

```bash
task coverage
```

Abra `coverage.html` no navegador para inspecionar a cobertura. O perfil
`coverage.out` e o relatório HTML são artefatos locais e não são versionados.

## Build

```bash
task build
```

O binário será gerado em `bin/danfse-service`.

Para gerar a imagem Docker:

```bash
task docker
```

## Swagger

Com o servidor em execução, a documentação está disponível em:

<http://localhost:8080/api/swagger-ui/index.html>

A spec usa `basePath` `/` com paths absolutos: o endpoint de negócio
`/api/danfse` e os probes `/health/live` e `/health/ready`.

Para regenerar os arquivos do Swagger:

```bash
task swagger
```
