# ADR007 - Observabilidade: Logs Estruturados, Health Checks e OTel Opcional

## Data

> 09/10/2026

## Status

> Aceito

## Contexto

Como serviço oferecido a terceiros, o danfse precisa que quem o opera consiga
diagnosticar falhas e acompanhar a saúde do processo sem ficar preso a um fornecedor
ou infraestrutura específicos. Ao mesmo tempo, por lidar com conteúdo fiscal, os
sinais emitidos não podem expor dados sensíveis.

## Decisão

Adotar observabilidade em três camadas, aproveitando o que a linguagem e o framework
HTTP já oferecem:

- **Logs estruturados (JSON) em stderr**, com identificador de correlação gerado pelo
  próprio serviço e campos técnicos e seguros, sem conteúdo fiscal, query,
  autenticação ou descrições cruas de erro.
- **Probes de liveness e readiness** que distinguem processo vivo de processo pronto
  para receber tráfego; durante o encerramento (graceful shutdown), o estado de pronto
  é revogado antes de aguardar as requisições em andamento, para que o orquestrador
  pare de encaminhar tráfego.
- **Telemetria opcional baseada em OpenTelemetry/OTLP**, desabilitada por padrão e
  emitida apenas no ponto de entrada HTTP, com backend independente do serviço. O
  núcleo compartilhado e o alvo WebAssembly permanecem intactos e não instrumentados.

A responsabilidade é dividida: o serviço emite sinais técnicos; a coleta, a retenção,
os dashboards e os alertas são da operação do ambiente.

Alternativas consideradas:

- Instrumentação própria de observabilidade: descartada por duplicar o que o framework
  e a biblioteca padrão já oferecem.
- Observabilidade obrigatória com backend ou fornecedor fixo: descartada por impor
  infraestrutura a quem só quer executar o serviço.
- Logs em arquivo gerenciados pelo serviço: descartada; a saída vai para o stderr e a
  coleta/rotação fica a cargo do ambiente.

## Consequências

Positivas:

- Diagnóstico independente de backend, coletor ou fornecedor.
- Sinais técnicos com exposição mínima de dados fiscais.
- Backend, retenção, dashboards e alertas sob controle de quem opera o ambiente.

Negativas:

- Sinais técnicos e genéricos, sem granularidade por etapa do processamento.
- O serviço emite, mas não entrega: sem a operação de coleta e de dashboards, a
  observabilidade não se concretiza.

## Notas

Detalhes de configuração opcional de logs e telemetria: ver o
[README](../../README.md).

ADRs relacionados: [ADR004](./ADR004-interface-http-e-tratamento-de-erros.md),
[ADR005](./ADR005-nucleo-compartilhado-entre-http-e-webassembly.md).
