# ADR008 - Estratégia de Métricas

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

A API precisa expor métricas para monitoramento durante a execução no perfil `offline`. Este ADR trata somente
das métricas desse cenário.

## Decisão

- Dependências `spring-boot-starter-actuator` e `micrometer-registry-prometheus` no [pom.xml](../../pom.xml).
- Endpoints `health` e `prometheus` expostos na porta de gerenciamento `management.server.port` (9103 em
  [application-offline.yml](../../src/main/resources/application-offline.yml)), com `base-path` `/`.
- O endpoint Prometheus é mapeado para `/metrics` (`management.endpoints.web.path-mapping.prometheus`).
- Com Spring Boot 4 ([ADR013](./ADR013-migracao-spring-boot-4-java-25.md)), as sondas `/health/liveness` e `/health/readiness` ficam
  disponíveis por padrão também fora de Kubernetes, e `/health` inclui os dois grupos.
- A ocupação dos caches é publicada como métrica, conforme o ADR007.

## Consequências

Positivas:
- Métricas disponíveis durante a execução local, sem infraestrutura externa
- Porta de métricas separada da porta HTTP da API

Negativas:
- A porta de métricas precisa ser acessível para a coleta, o que depende do ambiente
- O cabeçalho do arquivo de perfil cita porta diferente da configurada (ver ADR002)

## Notas

- Introdução das métricas Prometheus: 24/07/2026.
- A divergência de porta entre o cabeçalho e o valor configurado está registrada no ADR002.
- Atualizado em 17/11/2026: comportamento das sondas após a migração para Spring Boot 4.
