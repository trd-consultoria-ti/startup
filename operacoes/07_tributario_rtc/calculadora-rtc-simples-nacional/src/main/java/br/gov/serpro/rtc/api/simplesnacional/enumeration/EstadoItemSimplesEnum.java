package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 *
 * Enumera os estados possíveis de um item processado pelo endpoint do Simples Nacional.
 *
 * Duplicado deliberadamente da enumeração análoga {@code EstadoItemEnum} do regime
 * geral, para preservar o isolamento entre os pacotes (os estados dos dois cálculos
 * evoluem de forma independente).
 *
 * <p>Valores: {@code CALCULADO}, {@code NAO_CALCULADO}, {@code NAO_IMPLEMENTADO},
 * {@code INCONSISTENCIA_ENTRADA} e {@code FALHA_TECNICA}.
 *
 * <p>{@code NAO_CALCULADO} (NT 2026.005, seções 5.3.3 e 5.3.4, Observação 2):
 * o cálculo de IBS/CBS do item não é realizado (NÃO é erro) e o
 * {@code tribCalc} traz apenas {@code vRBSNitem} e {@code tpRBSN}. O item diz
 * por quê em {@code motivoEstado} ({@link MotivoNaoCalculoSimplesEnum}): regime
 * {@code SOMENTE_RECEITA}; {@code cAtivSN} ausente, desconhecido ou fora de
 * vigência, ou inadmissível para o tpDoc; ou anexo sem a CBS na Tabela de
 * Alíquotas Efetivas do Simples Nacional (cálculo assistido).
 *
 * <p>{@code NAO_IMPLEMENTADO}: o {@code cClassTrib} do item está vinculado, na
 * data do fato gerador, a tratamento tributário marcado como ainda não
 * implementado na Calculadora (indicador de ajuste {@code TRTR_IN_POSSUI_AJUSTE},
 * mesma semântica do CAL-007 do regime geral). É verificado com PRIORIDADE —
 * primeira consulta de banco do item — e encerra o processamento: sem
 * {@code tribCalc}, com o erro VSN-027 e fora de todos os totais.
 *
 */
public enum EstadoItemSimplesEnum {
    CALCULADO,
    NAO_CALCULADO,
    NAO_IMPLEMENTADO,
    INCONSISTENCIA_ENTRADA,
    FALHA_TECNICA
}
