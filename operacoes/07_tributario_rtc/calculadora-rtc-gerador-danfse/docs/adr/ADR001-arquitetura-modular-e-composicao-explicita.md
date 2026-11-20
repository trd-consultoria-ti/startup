# ADR001 - Arquitetura Modular e Composição Explícita

## Data

> 02/10/2026

## Status

> Aceito

## Contexto

O serviço de geração de DANFSe recebe um XML bruto da NFS-e nacional e devolve o PDF
correspondente. O trabalho envolve responsabilidades distintas, como interpretar os
dados, preparar o documento e produzir o arquivo final. Sem uma separação clara, a
lógica tende a se concentrar em poucas funções grandes, o que prejudica os testes, a
substituição de partes isoladas e o reaproveitamento do serviço em outros ambientes.

## Decisão

Adotar uma arquitetura modular, em que as responsabilidades ficam em camadas separadas
e a ligação entre elas é feita de forma explícita e manual, sem container de injeção de
dependências. Cada parte recebe o que precisa pelo construtor e a aplicação monta a
composição em um ponto definido.

A opção pela montagem manual evita a complexidade de um container e mantém a criação
das dependências visível e fácil de acompanhar.

## Consequências

Positivas:

- Fronteiras claras entre as responsabilidades, o que facilita testes isolados.
- Cada parte pode ser trocada sem afetar as demais.
- As mesmas regras podem ser reutilizadas em ambientes diferentes, como servidor e
  navegador.

Negativas:

- Toda mudança de dependência exige ajustes manuais na composição.
- A camada de entrada HTTP fica acoplada ao framework adotado, reduzindo a
  portabilidade dessa parte.

## Notas

ADRs relacionados: [ADR003](./ADR003-renderizacao-direta-de-pdf-com-folio.md),
[ADR005](./ADR005-nucleo-compartilhado-entre-http-e-webassembly.md).
