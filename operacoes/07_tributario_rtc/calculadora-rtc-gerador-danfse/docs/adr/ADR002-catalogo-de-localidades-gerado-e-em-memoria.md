# ADR002 - Catálogo de Localidades Gerado e em Memória

## Data

> 02/10/2026

## Status

> Aceito

## Contexto

Para preparar o DANFSe, o serviço precisa obter o nome e a UF dos municípios a partir
do código IBGE presente no XML. Essa necessidade existe tanto no servidor HTTP quanto
no WASM no navegador. Depender de um banco de dados ou de um serviço externo para essa
consulta adicionaria infraestrutura e uma dependência de rede a cada execução.

## Decisão

Incluir o catálogo de localidades no próprio artefato distribuído, em vez de consultar
um banco ou serviço externo. O catálogo é gerado previamente, incluído na compilação e fica
disponível na memória durante a execução, o que torna o serviço autônomo nos dois
ambientes.

## Consequências

Positivas:

- O serviço opera sem banco de dados e sem consulta de rede para localidades.
- A consulta é local e rápida, no servidor e no navegador.

Negativas:

- Atualizar o catálogo exige gerar e recompilar o artefato e redistribuí-lo.
- O catálogo passa a ocupar espaço no artefato e memória durante a execução.

## Notas

ADRs relacionados: [ADR005](./ADR005-nucleo-compartilhado-entre-http-e-webassembly.md).
