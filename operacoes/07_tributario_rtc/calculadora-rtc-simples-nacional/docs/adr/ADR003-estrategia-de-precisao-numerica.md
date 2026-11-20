# ADR003 - Estratégia de Precisão Numérica

## Data

> 17/11/2026

## Status

> Aceito

## Contexto

Os cálculos do Simples Nacional envolvem alíquotas, bases e valores monetários. Um documento
fiscal montado com a resposta da API precisa fechar consigo mesmo: o total do item é a soma dos
valores exibidos, e cada total da operação é a soma dos itens exibidos. Essas somas são conferidas
sem tolerância na autorização do documento.

As alíquotas do PGDAS chegam com até 28 casas na fração. Uma precisão interna de escala fixa
(8 casas) preserva só 4 ou 5 dígitos significativos de uma fração como `0.000186738…` e muda
centavos em parte relevante dos itens.

A Calculadora tem mais de uma API de cálculo. Para que todas apliquem a mesma política, a classe
[ArredondamentoUtils](../../src/main/java/br/gov/serpro/rtc/shared/util/ArredondamentoUtils.java)
é a mesma, byte a byte, na `api-simples-nacional` e na `api-regime-geral`.

## Decisão

Toda a política numérica fica em `ArredondamentoUtils`:

- **Modo único**: `RoundingMode.HALF_EVEN`, na constante `MODO` (ABNT NBR 5891: no empate exato,
  o algarismo par). É convenção do projeto; não há dispositivo legal que fixe o modo de
  arredondamento dos valores de IBS e CBS. O art. 349, § 14, da LC 214/2025 trata da fixação das
  alíquotas de referência e não se aplica ao cálculo por operação.
- **Conta interna exata**: soma, subtração, multiplicação e conversão entre percentual e fração
  não arredondam. Só a divisão que pode não terminar usa `CONTEXTO_DIVISAO` (34 dígitos
  significativos). Não há precisão interna de escala fixa.
- **Um arredondamento por valor, no cálculo**: cada valor de item é arredondado uma vez, a 2
  casas, por `arredondarValorFiscal`
  ([SimplesNacionalCalculoService](../../src/main/java/br/gov/serpro/rtc/api/simplesnacional/service/SimplesNacionalCalculoService.java),
  `calcularItem`). O objeto de resposta guarda o valor final.
- **Alíquotas**: entram no cálculo com todas as casas, inclusive depois da transformação da
  compra governamental. Quando o documento exige um número fixo de casas (NFS-e: 2), a alíquota é
  arredondada a essas casas por `arredondarAliquota` antes de multiplicar a base. Na resposta, a
  alíquota é apresentada com 2 a 4 casas por `formatarAliquota`.
- **Somas sobre os valores publicados**: o total do item e os totais da operação somam os valores
  fiscais já arredondados (`totalizar`), sem novo arredondamento
  ([TotalSimplesDomain](../../src/main/java/br/gov/serpro/rtc/api/simplesnacional/model/output/TotalSimplesDomain.java)).
- **Serialização**: os serializers
  [BigDecimalTDec1302Serializer](../../src/main/java/br/gov/serpro/rtc/shared/serializer/BigDecimalTDec1302Serializer.java) e
  [BigDecimalTDec0302_04Serializer](../../src/main/java/br/gov/serpro/rtc/shared/serializer/BigDecimalTDec0302_04Serializer.java)
  delegam a formatação a `ArredondamentoUtils`. Para um valor fiscal, apenas formatam.
- **Limites de entrada** (dígitos inteiros e casas aceitos) são constantes da mesma classe.
- **Cópia única**: o arquivo da classe e o teste `Teste_ArredondamentoUtils` são idênticos nos dois
  projetos. O teste `Teste_ArredondamentoUtilsCopiaUnica` confere o SHA-256 dos dois arquivos
  contra constantes iguais nos dois projetos. Alterar a classe exige alterar a cópia do outro
  projeto e a constante.
- **Só a classe arredonda**: nenhum outro arquivo de produção usa `setScale`, `RoundingMode` ou
  `MathContext`, e um `setRoundingMode` só é aceito com o `MODO` da classe. O teste
  `Teste_ArquiteturaArredondamento` falha se isso deixar de valer.

## Consequências

Positivas:
- O total do item e os totais da operação fecham com os valores exibidos
- Regras de arredondamento em um único ponto, igual em todas as APIs, facilitando auditoria
- Resultados determinísticos para os mesmos insumos
- A mudança do modo de arredondamento, se vier a ser determinada, altera uma constante

Negativas:
- Qualquer alteração na política altera os resultados e exige nova validação nos dois projetos
- A cópia da classe é mantida à mão nos dois repositórios; o teste do SHA-256 acusa a diferença,
  mas não a corrige

## Notas

Histórico:
- 24/07/2026: primeira versão desta decisão, com precisão interna de 8 casas (`PRECISAO_INTERNA`)
  em todas as operações e arredondamento a 2 casas apenas na serialização.
- 17/11/2026: a decisão passa a registrar a conta interna exata e o arredondamento único no
  cálculo do item, que o código já aplicava, e a classe passa a ser a cópia única compartilhada
  com a `api-regime-geral`. O comportamento numérico do Simples Nacional não mudou nesta data.

ADR relacionada: ADR003 da `api-regime-geral` (mesma política, com as regras próprias do regime
geral).
