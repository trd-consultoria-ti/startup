# ADR010 - Processamento Paralelo com Threads Virtuais

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

Uma requisição do Simples Nacional pode conter vários itens independentes, cada um com seu próprio cálculo.
Processá-los em sequência aumenta a latência da resposta.

## Decisão

- Em [SimplesNacionalObservabilidadeService](../../src/main/java/br/gov/serpro/rtc/api/simplesnacional/service/SimplesNacionalObservabilidadeService.java),
  cada item é processado em `CompletableFuture.supplyAsync` com um executor criado por
  `Executors.newVirtualThreadPerTaskExecutor()`, dentro de `try-with-resources`.
- Os resultados são coletados com `join()` e ordenados por `nObj`.
- Só depois da coleta ocorre a totalização: itens `CALCULADO` e `NAO_CALCULADO` possuem `tribCalc`, sendo que `NAO_CALCULADO` contribui apenas para `vRBSNtot`; itens com erro ou `NAO_IMPLEMENTADO` não contribuem (comentário em `SimplesNacionalObservabilidadeService`).
- Erros de item são reportados individualmente, sem interromper os demais (ver ADR004).
- As threads virtuais são habilitadas globalmente por `spring.threads.virtual.enabled: true` (ver ADR005).

## Consequências

Positivas:
- Itens independentes são calculados em paralelo sem gerenciar um pool de threads de plataforma
- Ordenação final determinística pelo número do item

Negativas:
- A totalização depende de todos os itens; um item lento atrasa a resposta

## Notas

- Introdução do processamento com threads virtuais: 24/07/2026.
