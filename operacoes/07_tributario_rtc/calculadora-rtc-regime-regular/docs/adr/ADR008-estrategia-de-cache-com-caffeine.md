# ADR008 - Estratégia de Cache com Caffeine

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API RTC Calculadora consulta frequentemente os mesmos dados de referência: classificações
tributárias, alíquotas, NCM, NBS, percentuais de redução, schemas XML e tokens de template.
Esses dados são predominantemente read-only em runtime, com muitas consultas repetindo as
mesmas chaves entre requisições.

Reexecutar as mesmas consultas ao SQLite local a cada requisição desperdiça processamento
para dados imutáveis durante o ciclo de vida da instância.

## Decisão

Usar Spring Cache Abstraction com Caffeine como provedor de cache local.

As especificações por cache (nome, expiração, capacidade inicial, tamanho máximo) ficam
em configuração de ambiente. O cache é aplicado declarativamente com `@Cacheable` em
repositórios e utilitários que consultam dados de referência.

Métricas de cache são registradas automaticamente no `MeterRegistry` via `CaffeineCacheMetrics`.

## Consequências

Positivas:
- Configuração declarativa com `@Cacheable`, sem lógica manual
- Sem dependência de infraestrutura externa; cache local na JVM
- Configuração independente por cache via YAML
- Métricas registradas automaticamente na configuração

Negativas:
- Cache por instância; não compartilhado entre múltiplas instâncias
- Substituições da base SQLite podem exigir reinício ou limpeza do cache local
- Limites e expiração devem ser ajustados conforme o perfil de uso real

## Notas

Referências:
- [Caffeine](https://github.com/ben-manes/caffeine)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
