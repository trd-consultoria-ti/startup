# ADR006 - Validação e Proteção da Entrada XML

## Data

> 09/10/2026

## Status

> Aceito

## Contexto

O serviço recebe o XML da NFS-e como entrada. Como ele será interpretado, é preciso
proteger o processamento contra entradas grandes demais, declarações indesejadas e
conteúdo malformado, sem quebrar a compatibilidade com XMLs antigos que trazem `&`
não escapados.

## Decisão

Adotar uma política comum de validação da entrada de XML nos dois ambientes, servidor e
navegador: impor um tamanho máximo (5 MiB), rejeitar declarações de DTD e entidades,
validar uma estrutura mínima e aceitar a correção controlada de `&` não escapados para
manter a compatibilidade com entradas antigas.

A política combina restrições de tamanho e conteúdo com tolerância a um problema
específico de arquivos legados: o uso de `&` não escapados.

## Consequências

Positivas:

- Entradas grandes e declarações indesejadas são rejeitadas antes do processamento.
- A compatibilidade é preservada para XMLs antigos com `&` não escapados.

Negativas:

- A correção do `&` altera a entrada recebida, ainda que de forma controlada.
- A validação mínima não substitui uma validação completa por schema e não elimina
  todos os riscos de entrada.

## Notas

O limite de tamanho (5 MiB por padrão) é configurável via `XML_MAX_SIZE_BYTES`
(em bytes), com teto de 32 MiB para evitar a desativação acidental da proteção.
O valor configurado é aplicado tanto ao limite de corpo HTTP quanto à validação
interna do XML, antes e depois da sanitização do `&`. A política de conteúdo
(recusa de DTD/entidades e correção controlada do `&`) permanece fixa.

ADRs relacionados: [ADR004](./ADR004-interface-http-e-tratamento-de-erros.md),
[ADR005](./ADR005-nucleo-compartilhado-entre-http-e-webassembly.md).
