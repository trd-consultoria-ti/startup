# ADR004 - Estratégia de Tratamento de Erros

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

A API retorna erros de validação e de cálculo para o cliente, em mais de um item de uma mesma
requisição. É necessário identificar cada erro de forma estável e indicar a localização no payload.

## Decisão

- As respostas de erro usam `ProblemDetail` (formato de problem details, RFC 7807/9457), tratadas em
  [ApiExceptionHandler](../../src/main/java/br/gov/serpro/rtc/shared/exceptionhandler/ApiExceptionHandler.java).
- Na migração para Spring Framework 7 ([ADR013](./ADR013-migracao-spring-boot-4-java-25.md)), o handler passou a preencher explicitamente
  `type: "about:blank"` quando o framework não informa o tipo, preservando o campo no contrato
  JSON dos erros.
- `ErrosCalculoException` (em [ErrosCalculoException.java](../../src/main/java/br/gov/serpro/rtc/shared/exceptionhandler/ErrosCalculoException.java))
  é tratada por `handleErrosCalculoException`, com título "Erros no processamento do cálculo do Simples Nacional",
  a propriedade `errors` (lista de erros) e a propriedade `timestamp`.
- Cada erro é um `ErroDetalhe` ([ErroDetalhe.java](../../src/main/java/br/gov/serpro/rtc/shared/exceptionhandler/dto/ErroDetalhe.java)),
  record com os campos `type`, `title`, `code`, `pointer` e `detail`, serializado sem campos nulos (`@JsonInclude(NON_NULL)`).
  O campo `pointer` segue a RFC 6901 e aponta para a posição do erro no payload.
- Os códigos ficam no catálogo [CodigoValidacaoSimples](../../src/main/java/br/gov/serpro/rtc/api/simplesnacional/exceptionhandler/enumeration/CodigoValidacaoSimples.java),
  no padrão `VSN-###`.
- Há um handler específico para `MethodArgumentTypeMismatchException` e um handler genérico para `Exception`.
- Erros por item não interrompem o processamento dos demais itens (ver ADR010).

## Consequências

Positivas:
- Formato padronizado e legível por máquina
- Cada erro é rastreável por código estável e por ponteiro no payload
- Catálogo centralizado facilita documentação e manutenção

Negativas:
- Novos erros exigem atualizar o catálogo e, quando relevante, a documentação

## Notas

- Introdução do tratamento com `ProblemDetail`: 24/07/2026.
- Atualizado em 17/11/2026: preservação do `type` padrão na migração de plataforma.
- O catálogo `CodigoValidacaoSimples` contém 32 ocorrências do prefixo `VSN-`.
- Exemplo de uso: [SimplesNacionalObservabilidadeService](../../src/main/java/br/gov/serpro/rtc/api/simplesnacional/service/SimplesNacionalObservabilidadeService.java)
  lança `ErrosCalculoException` com `COMPRA_GOV_SEM_VIGENCIA` (comentário no código cita `VSN-025`) quando não há parametrização de transição vigente.
