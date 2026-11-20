# ADR007 - Estratégia de Cache com Caffeine

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

Parte das consultas da API retorna dados que mudam raramente. Um cache em memória evita
consultas repetidas ao banco e permite observar a ocupação de cada cache.

## Decisão

- Implementação em memória com Caffeine (`com.github.ben-manes.caffeine:caffeine`) através de `CaffeineCacheManager`.
- A classe [CaffeineCacheConfig](../../src/main/java/br/gov/serpro/rtc/config/cache/CaffeineCacheConfig.java)
  cria cada cache a partir das propriedades de [CacheSpecs](../../src/main/java/br/gov/serpro/rtc/config/cache/CacheSpecs.java)
  (`@ConfigurationProperties(prefix = "cache")`, propriedade `cache.specs`).
- Estatísticas habilitadas com `recordStats()`.
- Métricas publicadas por cache: `cache_occupancy_percent` e `cache_max_size`.
- Configuração atual em [application-offline.yml](../../src/main/resources/application-offline.yml): um cache `VersaoBaseDadosService.getUltimaVersao`,
  com `expireAfterAccess: P30D`, `initialCapacity: 1` e `maximumSize: 1`.

## Consequências

Positivas:
- Configuração declarativa por cache, sem recompilar
- Ocupação dos caches visível no endpoint de métricas (ver ADR008)

Negativas:
- Cache em memória não é compartilhado entre instâncias; cada instância mantém sua própria cópia
- Valores podem ficar desatualizados até a expiração (30 dias no cache atual)

## Notas

- Introdução de `CaffeineCacheConfig` e `CacheSpecs`: 24/07/2026.
