# ADR003 - Estratégia de Precisão Numérica

## Data

> 17/11/2026

## Status

> Aceito

## Contexto

Cálculos de CBS, IBS e Imposto Seletivo envolvem operações em cascata (base de cálculo →
alíquota → reduções → valor → somas do item → totais da operação). O resultado é transcrito pelo
contribuinte para o documento fiscal, e a autorização do documento confere cada número contra os
outros números do próprio documento:

- o valor de cada tributo contra a base e a alíquota publicadas (com tolerância de 0,01);
- o `vIBS` contra a soma do `vIBSUF` e do `vIBSMun`, e cada total contra a soma dos itens, sem
  tolerância.

Na primeira versão desta decisão, as contas corriam com precisão interna de 8 casas e cada campo
era arredondado sozinho, a 2 casas, só na serialização. As somas e as multiplicações usavam
valores que não apareciam na resposta, e a resposta podia não fechar consigo mesma: um total
diferente da soma dos itens exibidos, um `vIBS` diferente de `vIBSUF + vIBSMun`, um valor
diferente de base × alíquota efetiva exibida. Uma nota montada com esses valores era rejeitada na
autorização (por exemplo, rejeição 1091, "Total de CBS difere da soma dos itens").

Arredondar cada passo a 8 casas também descartava dígitos: 8 casas de uma fração pequena preservam
só 4 ou 5 dígitos significativos.

## Decisão

Toda a política numérica fica em
[ArredondamentoUtils](../../src/main/java/br/gov/serpro/rtc/shared/util/ArredondamentoUtils.java),
que é a mesma classe, byte a byte, da `api-simples-nacional`.

1. **Modo único**: `RoundingMode.HALF_EVEN`, na constante `MODO` (ABNT NBR 5891: no empate exato,
   o algarismo par). É convenção do projeto; não há dispositivo legal que fixe o modo de
   arredondamento dos valores de IBS e CBS. O art. 349, § 14, da LC 214/2025 trata da fixação das
   alíquotas de referência e não se aplica ao cálculo por operação.
2. **Conta interna exata**: soma, subtração, multiplicação e conversão entre percentual e fração
   não arredondam. Só a divisão que pode não terminar usa `CONTEXTO_DIVISAO` (34 dígitos
   significativos). Não há precisão interna de escala fixa. O
   [AvaliadorExpressaoAritmetica](../../src/main/java/br/gov/serpro/rtc/domain/service/calculotributo/AvaliadorExpressaoAritmetica.java)
   segue a mesma regra e não arredonda o resultado das expressões.
3. **Cada número publicado é arredondado uma vez, no cálculo**, na casa do leiaute: valor
   monetário com 2 casas (`arredondarValorFiscal`); alíquota com até 4
   (`arredondarAliquotaPublicada`). O objeto de resposta guarda o número final. Os serializers
   apenas formatam.
4. **O número publicado é o que entra na conta seguinte**
   ([CalculoCbsIbsService](../../src/main/java/br/gov/serpro/rtc/domain/service/calculotributo/CalculoCbsIbsService.java),
   [CalculoImpostoSeletivoService](../../src/main/java/br/gov/serpro/rtc/domain/service/calculotributo/CalculoImpostoSeletivoService.java)):
   - a base (`vBC`) é arredondada a 2 casas antes de multiplicar;
   - a alíquota efetiva (`pAliqEfet`, e a efetiva da tributação regular) é arredondada a 4 casas
     e é ela que multiplica a base: `valor = arred2(vBC × alíquota publicada ÷ 100)`;
   - com diferimento, `vDif = arred2(vBC × alíquota publicada ÷ 100 × pDif ÷ 100)` e o valor
     devido é `arred2(vBC × alíquota publicada ÷ 100) − vDif`, descontado uma única vez;
   - no Imposto Seletivo, `vBCIS`, `pIS` e `adRemIS` publicados entram na conta, e é o `vIS`
     publicado que compõe a base do IBS e da CBS.
5. **Somas usam os valores publicados**: `vIBS = vIBSUF + vIBSMun` no item; cada total é a soma
   do campo correspondente dos itens; o `vIBS` total é a soma dos `vIBS` dos itens. O mesmo vale
   para monofasia, Imposto Seletivo, diferimento, devolução e crédito presumido.
6. **Compra governamental**
   ([TributacaoCompraGovernamentalDomain](../../src/main/java/br/gov/serpro/rtc/api/model/roc/TributacaoCompraGovernamentalDomain.java)):
   - `gTribCompraGov` traz o que seria devido sem a redistribuição: cada valor é
     `arred2(vBC × alíquota do tributo ÷ 100)`, líquido do diferimento quando houver;
   - no grupo principal, as alíquotas redistribuídas são publicadas com 4 casas e o valor de cada
     tributo é `arred2(vBC × alíquota publicada ÷ 100)`. Ele não é a soma dos valores do detalhe:
     a diferença entre o detalhe e o grupo principal pode chegar a 0,04 e é aceita na validação
     do documento;
   - quando parte da alíquota da CBS passa ao IBS (Estado ou Município comprador), a alíquota de
     quem cede é arredondada a 4 casas e a de quem recebe é obtida por diferença, de modo que a
     soma das alíquotas publicadas seja igual à soma das alíquotas originais.
7. **Pedágio**
   ([PedagioService](../../src/main/java/br/gov/serpro/rtc/domain/service/pedagio/PedagioService.java)):
   o total de cada tributo é a soma exata das parcelas dos trechos, arredondada uma vez. Cada
   trecho recebe a sua parcela truncada no centavo, e os centavos que faltam para o total vão, um
   a um, aos trechos de maior resto (empate: menor número de trecho), por `ratearValorFiscal`. As
   parcelas somam o total. A alíquota efetiva do trecho é informativa, com 4 casas.
8. **Bases unificadas** (`base-calculo-unificada` e `base-calculo-unificada-nfse`): resultado com
   2 casas, arredondado uma vez pela classe. A validação da base declarada contra os componentes
   do item compara os valores exatos.
9. **Só a classe arredonda**: fora de `ArredondamentoUtils` não há `setScale`, `RoundingMode` nem
   `MathContext` no código de produção, e um `setRoundingMode` só é aceito com o `MODO` da classe.
   A memória de cálculo mostra os números publicados.

Proteções no código:

- `Teste_ArredondamentoUtilsCopiaUnica` confere o SHA-256 da classe (e do teste dela) contra
  constantes iguais nos dois projetos. Alterar a classe exige alterar a cópia da
  `api-simples-nacional` e a constante.
- `Teste_ArquiteturaArredondamento` falha se houver arredondamento fora da classe.
- `Teste_LoteArredondamento` gera notas com semente fixa e confere, na resposta serializada, que
  cada identidade acima fecha exatamente; `Teste_SerializacaoNaoAlteraNumero` confere que o valor
  guardado no objeto de resposta é igual ao texto serializado.

## Consequências

Positivas:
- A resposta fecha consigo mesma: valor contra base e alíquota publicadas, partes contra o todo,
  itens contra totais
- Política decimal única, igual em todas as APIs de cálculo, em um só arquivo
- Cálculos reproduzíveis: quem refaz a conta com os números da resposta chega aos mesmos valores
- A mudança do modo de arredondamento, se vier a ser determinada, altera uma constante

Negativas:
- A adoção muda centavos em respostas já publicadas: totais e `vIBS` passam a ser somas dos
  valores exibidos, e valores com alíquota efetiva de mais de 4 casas passam a usar a alíquota
  exibida (o `CHANGELOG.md`, seção 1.7.1, descreve o efeito para quem integra)
- Na compra governamental, o detalhe e o grupo principal podem diferir em até 0,04
- A cópia da classe é mantida à mão nos dois repositórios; o teste do SHA-256 acusa a diferença,
  mas não a corrige
- Qualquer alteração na política altera os resultados e exige nova validação nos dois projetos

## Notas

Histórico:
- 26/05/2026: primeira versão desta decisão. Precisão interna de 8 casas com
  `RoundingMode.HALF_EVEN` em todas as operações (`multiplicar`, `dividir`, `subtrair`,
  `arredondarInterno`), repassada ao avaliador de expressões, e arredondamento a 2 casas apenas na
  serialização.
- 17/11/2026: decisão substituída pela descrita acima (conta exata, um arredondamento por número
  publicado, somas sobre os valores publicados e classe única compartilhada com a
  `api-simples-nacional`). A classe mudou do pacote `core.util` para `shared.util`.

ADR relacionada: ADR003 da `api-simples-nacional` (mesma classe, com as regras próprias do
Simples Nacional).

Referências:
- NT 2025.002-RTC, regras de validação UB35-10, UB54-10 e UB67-10 (valor = base × alíquota, com
  tolerância de 0,01), UB54a-10 (`vIBS` = `vIBSUF` + `vIBSMun`), W35-10 a W59g-10 (total = soma
  dos itens), UB28-10, UB47-10 e UB66-10 (alíquota efetiva com 4 casas) e UB82a-20 (compra
  governamental, tolerância de 0,04)
- ABNT NBR 5891:2014
