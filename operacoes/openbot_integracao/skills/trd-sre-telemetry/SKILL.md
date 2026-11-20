---
name: trd-sre-telemetry
description: Padronização de observabilidade, telemetria e runbooks SRE para a infraestrutura da TRD Consultoria. Governa criação de endpoints /healthz e /readyz, estruturação de logs JSON com x-request-id, coleta de métricas de latência e cálculo automatizado de SLA mensal (99,5% a 99,9%).
---

# TRD SRE & Telemetria — Observabilidade de Alta Disponibilidade

## Governança de Telemetria e Monitoramento de Produção
O agente Observabilidade utiliza esta skill para monitorar a saúde das aplicações e alertar antes de qualquer impacto ao cliente.

### 1. Endpoints de Verificação Sintética
- `/healthz` (Liveness): Responde `200 OK` se o processo Node.js estiver rodando e respondendo ao event loop.
- `/readyz` (Readiness): Executa verificação profunda:
  - Conexão e query simples no PostgreSQL (`SELECT 1`).
  - Conectividade com container da Evolution API.
  - Se qualquer dependência falhar, retorna `503 Service Unavailable` com JSON indicando o componente degradado.

### 2. Structured Logging em JSON com Correlator
- Todo log gerado pela aplicação deve seguir formato JSON (Pino/Winston):
  `{"timestamp": "2026-11-20T00:00:00Z", "level": "info", "requestId": "uuid", "tenant": "cliente_x", "message": "...", "durationMs": 12}`
- Middleware propaga cabeçalho `X-Request-Id` do Nginx até a resposta final.

### 3. Cálculo Automatizado de SLA Mensal
- Monitoramento de uptime 24/7 (via Uptime Kuma interno).
- Cálculo de disponibilidade:
  $$\text{Disponibilidade (\%)} = \frac{\text{Tempo Total} - \text{Downtime Não Programado}}{\text{Tempo Total}} \times 100$$
- Geração automática de Relatório Executivo de SLA para envio aos clientes corporativos com contratos de sustentação continuada.
