# ADR001 - Cálculo Tributário por Expressão

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

O sistema RTC Calculadora precisa calcular CBS, IBS e Imposto Seletivo de acordo com as regras da
Reforma Tributária do Consumo. Essas regras são frequentemente revisadas pela legislação,
são complexas por combinarem alíquota, base de cálculo, reduções e tributos devidos, e variam por
contexto tributário, produto, operação e ente federativo.

As fórmulas ficam armazenadas na entidade `TratamentoTributario`, em colunas como
`TRTR_EXPRESSAO_ALIQUOTA`, `TRTR_EXPRESSAO_BASE_CALCULO` e `TRTR_EXPRESSAO_TRIBUTO_CALCULADO`.
Implementar essas regras apenas com lógica Java hardcoded duplicaria código para cálculos similares 
e dificultaria auditoria das fórmulas aplicadas.

## Decisão

Usar Apache Commons JEXL3 para avaliar as expressões aritméticas armazenadas no banco de dados.

O avaliador recebe as variáveis como mapa de `BigDecimal` e avalia a expressão em contexto
isolado por `JexlSandbox` para restringir o acesso externo.

## Consequências

Positivas:
- Regras atualizáveis nos dados de domínio sem atualização de código
- Fórmulas centralizadas, auditáveis e vinculadas ao tratamento tributário usado
- Facilita adaptação a mudanças legislativas

Negativas:
- Avaliação em tempo de execução tem custo adicional em relação a código Java compilado
- Erros em expressões podem ser mais difíceis de diagnosticar
- Requer validação das expressões durante carga ou manutenção dos dados tributários
- Sandbox deve permanecer restritiva para evitar acesso indevido

## Notas

Referências:
- [Apache Commons JEXL](https://commons.apache.org/proper/commons-jexl/)
