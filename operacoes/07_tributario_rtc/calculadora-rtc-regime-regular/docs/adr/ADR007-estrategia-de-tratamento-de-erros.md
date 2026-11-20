# ADR007 - Estratégia de Tratamento de Erros

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API RTC Calculadora processa requisições que podem falhar de formas diferentes: validação de
entrada (JSON malformado, parâmetros inválidos), regra de negócio (classificação tributária não
encontrada, dados inválidos), falhas internas (erro inesperado durante processamento).

Tratar exceções localmente em cada controller gera duplicação, inconsistência entre endpoints e
mistura o fluxo HTTP com lógica de erro. Sem um contrato de erro padronizado, integrações
externas precisam lidar com formatos de resposta divergentes.

## Decisão

Usar `@RestControllerAdvice` global com `ProblemDetail` conforme RFC 7807.

O handler estende `ResponseEntityExceptionHandler` e cobre:
- Erros de validação de entrada → 422
- Entidade não encontrada → 404
- Inconsistências estruturais e erros internos → 500
- JSON malformado e argumentos inválidos → 400
- `Exception` genérica como fallback → 500

Controllers não precisam de try-catch para exceções tratadas pelo handler global.

## Consequências

Positivas:
- Tratamento centralizado em um único componente
- Contrato de erro padronizado (`ProblemDetail`) para todos os clientes
- Controllers focados no fluxo da requisição, sem lógica de erro
- Fácil de estender com novos tipos de exceção

Negativas:
- O fluxo de erro não é visível diretamente no controller (indireção)
- Ordem e especificidade dos handlers precisam ser mantidas com cuidado
- Novos tipos de exceção exigem atualização do handler ou de `ProblemType`

## Notas

Referências:
- [RFC 7807: Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [Spring ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
