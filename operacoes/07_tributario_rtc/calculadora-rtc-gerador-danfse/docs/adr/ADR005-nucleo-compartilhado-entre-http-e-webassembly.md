# ADR005 - Núcleo Compartilhado entre HTTP e WebAssembly

## Data

> 09/10/2026

## Status

> Aceito

## Contexto

O mesmo serviço de geração de DANFSe precisa rodar como servidor HTTP e como artefato
WebAssembly no navegador. Se cada ponto de entrada tivesse a própria implementação,
as regras de geração poderiam divergir e haveria código duplicado.

## Decisão

Manter a lógica de geração em um núcleo único, compartilhado pelos dois pontos de
entrada, que ficam enxutos e apenas adaptam a entrada e a saída ao ambiente. Isso evita
repetir regras e reduz o risco de uma versão do serviço se comportar diferente da outra.

## Consequências

Positivas:

- Uma única origem para as regras de geração, com menos risco de divergência.
- Pontos de entrada pequenos, focados apenas nas diferenças de cada ambiente.

Negativas:

- As dependências do núcleo precisam funcionar também em WebAssembly, o que limita a
  escolha de bibliotecas.
- Servidor e navegador exigem compilações separadas do mesmo núcleo.

## Notas

ADRs relacionados: [ADR001](./ADR001-arquitetura-modular-e-composicao-explicita.md),
[ADR002](./ADR002-catalogo-de-localidades-gerado-e-em-memoria.md),
[ADR004](./ADR004-interface-http-e-tratamento-de-erros.md).
