# ADR011 - Modelo de Estado do Fator R

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

Algumas atividades do Simples Nacional têm dois anexos: um sem Fator R e outro com Fator R. A escolha
entre eles depende da configuração de anexos informada na consulta. A API precisa informar se a decisão
do Fator R é obrigatória, opcional, indisponível ou não aplicável.

## Decisão

- O estado é representado pelo enum [ModoFatorR](../../src/main/java/br/gov/serpro/rtc/api/atividadesimples/dto/ModoFatorR.java)
  com os valores `NAO_APLICAVEL`, `OPCIONAL`, `INDISPONIVEL` e `OBRIGATORIO`.
- A derivação é feita em [DerivacaoAnexosSimples](../../src/main/java/br/gov/serpro/rtc/api/atividadesimples/service/DerivacaoAnexosSimples.java),
  com as regras:
  - sem `anexoComFatorR`: `NAO_APLICAVEL`;
  - com `anexoComFatorR` configurado e `anexoSemFatorR` também configurado: `OPCIONAL`;
  - com `anexoComFatorR` configurado e `anexoSemFatorR` ausente da configuração: `OBRIGATORIO`;
  - com `anexoComFatorR` ausente da configuração: `INDISPONIVEL`.
- A derivação também calcula `disponivel` (ao menos um anexo possível está configurado) e `anexosFaltantes`.
- Uma atividade com `anexoComFatorR` preenchido e `anexoSemFatorR` vazio é rejeitada com `IllegalStateException`.
- O campo `sujeitaFatorR` de [AtividadeSimplesDomain](../../src/main/java/br/gov/serpro/rtc/api/atividadesimples/dto/AtividadeSimplesDomain.java)
  é derivado de `anexoComFatorR != null` no construtor.
- O pacote `atividadesimples` é isolado do pacote `simplesnacional`, conforme a Javadoc de `ModoFatorR`.

## Consequências

Positivas:
- Regra centralizada em uma única classe de derivação
- Estado explícito permite ao cliente saber se precisa informar o Fator R

Negativas:
- A regra depende de a base de atividades (ver ADR001) estar consistente; atividades inconsistentes geram exceção
- Mudanças na legislação do Fator R exigem alterar a derivação e os testes correspondentes

## Notas

- Introdução de `ModoFatorR` e `DerivacaoAnexosSimples`: 24/07/2026.
- Javadoc de `ModoFatorR.NAO_APLICAVEL` cita `sujeitaFatorR == false`; esse campo é derivado de `anexoComFatorR == null`, portanto a descrição é equivalente ao código.
- A Javadoc de `ModoFatorR` e o código foram conferidos entre si; o texto de `OBRIGATORIO` e `INDISPONIVEL` corresponde às regras acima.
