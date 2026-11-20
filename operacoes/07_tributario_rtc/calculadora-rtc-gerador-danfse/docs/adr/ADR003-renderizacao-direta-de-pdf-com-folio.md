# ADR003 - Renderização Direta de PDF com Folio

## Data

> 02/10/2026

## Status

> Aceito

## Contexto

O serviço precisa transformar a NFS-e preparada em um arquivo PDF. Uma alternativa
é gerar uma página HTML e usar um navegador para convertê-la em PDF, o que
adiciona uma etapa intermediária e depende de ferramentas externas de renderização.

## Decisão

Gerar o PDF diretamente em Go com a biblioteca Folio, sem passar por HTML e sem usar
navegador. Assim, a mesma lógica compilada para servidor e para WebAssembly produz o
PDF da mesma forma, o que simplifica a execução nos dois ambientes.

## Consequências

Positivas:

- Execução mais simples, sem ferramentas ou etapas externas de renderização.
- O PDF é produzido pela mesma lógica no servidor e no navegador.

Negativas:

- O layout é construído manualmente no código; mudanças visuais exigem ajustes e
  conferência.
- Há acoplamento com a biblioteca de desenho adotada; migrar para outra exigiria
  reescrever essa camada.
- A paginação precisa ser tratada manualmente para documentos mais longos.

## Notas

ADRs relacionados: [ADR001](./ADR001-arquitetura-modular-e-composicao-explicita.md),
[ADR005](./ADR005-nucleo-compartilhado-entre-http-e-webassembly.md).
