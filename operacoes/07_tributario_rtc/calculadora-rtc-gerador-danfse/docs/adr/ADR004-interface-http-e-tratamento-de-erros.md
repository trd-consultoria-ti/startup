# ADR004 - Interface HTTP e Tratamento Centralizado de Erros

## Data

> 02/10/2026

## Status

> Aceito

## Contexto

O serviço precisa expor um ponto de entrada HTTP para receber o XML da NFS-e e
devolver o PDF. Para que os clientes integrem com facilidade, é útil que as respostas
de erro sigam um formato único e estável.

## Decisão

Construir a interface HTTP com o framework Echo e centralizar o tratamento de erros em
um único ponto, que devolve as falhas em um formato padrão. A centralização evita que
cada rota trate erros do seu jeito e dá consistência a quem consome a API.

## Consequências

Positivas:

- Formato de erro consistente, o que simplifica a integração dos clientes.
- Um único lugar para manter a política de respostas de erro.

Negativas:

- A interface fica dependente do framework Echo; trocar de framework exigiria adaptar
  essa camada.
- É preciso cuidado na classificação dos erros para que o cliente receba respostas
  adequadas.

## Notas

ADRs relacionados: [ADR001](./ADR001-arquitetura-modular-e-composicao-explicita.md),
[ADR005](./ADR005-nucleo-compartilhado-entre-http-e-webassembly.md),
[ADR006](./ADR006-validacao-e-protecao-da-entrada-xml.md).
