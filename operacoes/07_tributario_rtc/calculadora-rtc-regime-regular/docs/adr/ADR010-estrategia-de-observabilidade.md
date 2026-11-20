# ADR010 - Observabilidade de Resultados por Item de Cálculo

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API recebe operações compostas por múltiplos itens tributáveis em uma única requisição.
Cada item passa por validação de entrada e cálculo tributário de forma independente.

Uma falha em um item não implica que os demais sejam inválidos. Interromper o processamento
e retornar erro único na primeira falha obrigaria o consumidor a resolver um item de cada vez,
sem visibilidade sobre o estado dos demais.

## Decisão

Adotar um modelo de processamento tolerante a falhas por item, com rastreamento de estado
e erros individualizados por objeto na resposta.

O processamento opera em duas fases:

1. **Validação global da operação** (campos obrigatórios, UF, município): falhas nessa
   fase interrompem o processamento e retornam 422.
2. **Processamento paralelo de cada item**: cada item é validado e calculado de forma
   independente. Falhas de validação ou de cálculo são capturadas por item, sem interromper
   os demais. Os itens são processados em paralelo por meio de threads virtuais (Project Loom).

A resposta inclui, para cada item:
- Estado do item: `CALCULADO` ou estado de erro
- Erros estruturados por item (tipo, campo, detalhe) quando aplicável
- Resultado tributário quando calculado com sucesso

Apenas itens com estado `CALCULADO` participam da totalização.

## Consequências

Positivas:
- Uma única requisição retorna o estado de todos os itens, com detalhes de falha por item
- Consumidores identificam exatamente quais itens falharam e por quê, sem reprocessar toda a operação
- Itens independentes são processados em paralelo, reduzindo latência em operações com muitos itens
- Facilita reprocessamento seletivo: apenas os itens com erro precisam ser corrigidos e reenviados

Negativas:
- A resposta é sempre estruturada, mesmo quando todos os itens falham
- HTTP 200 pode ser retornado mesmo com itens em erro; cabe ao consumidor verificar o estado de cada item
- Itens calculados com sucesso compõem a totalização; itens em erro são excluídos do total sem aviso explícito no nível da operação
