# ADR006 - Documentação de API com SpringDoc OpenAPI

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API RTC Calculadora é consumida por sistemas de NFS-e, ERPs, integradores fiscais e
desenvolvedores que precisam validar payloads e respostas. O projeto foi desenvolvido com
código Java como ponto de partida: os controllers, modelos de entrada e saída existem como
classes Java, e o contrato da API deve ser derivado desse código, não o inverso.

## Decisão

Usar SpringDoc OpenAPI com Swagger UI, adotando abordagem code-first: o código Java é
a fonte da verdade e o SpringDoc gera o contrato OpenAPI a partir das anotações em tempo de
execução.

Para manter os controllers concretos limpos, as anotações OpenAPI ficam em interfaces
separadas implementadas pelos controllers. A dependência usada é
`springdoc-openapi-starter-webmvc-ui`.

## Consequências

Positivas:
- Documentação sempre sincronizada com o código (gerada a partir dele)
- Swagger UI permite testes exploratórios sem ferramentas externas
- Interfaces `*OpenApi` mantêm os controllers concretos focados no mapeamento de rotas
- Sem processo adicional de geração de código ou manutenção de spec YAML separado

Negativas:
- O contrato reflete o que está implementado; não há design prévio independente do código
- Anotações `@Operation`, `@Tag`, `@ApiResponse` exigem manutenção nas interfaces
- A exposição do Swagger UI precisa ser avaliada por ambiente de implantação

## Notas

Referências:
- [SpringDoc OpenAPI](https://springdoc.org/)
- [OpenAPI Specification 3.1](https://spec.openapis.org/oas/latest.html)
