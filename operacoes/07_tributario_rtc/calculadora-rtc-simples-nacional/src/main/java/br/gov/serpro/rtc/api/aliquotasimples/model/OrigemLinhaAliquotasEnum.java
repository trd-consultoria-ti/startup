package br.gov.serpro.rtc.api.aliquotasimples.model;

/**
 * De onde veio a linha de alíquotas do contribuinte.
 * É a ÚNICA diferença entre os dois endpoints novos de cálculo:
 * <ul>
 * <li>{@link #REQUISICAO} — não assistido: {@code aliquotasEfetivas}, no corpo
 * da requisição. Valor exigido e ausente é erro de entrada (VSN-004/006), com
 * o ponteiro para dentro de {@code aliquotasEfetivas};</li>
 * <li>{@link #TABELA_ALIQUOTAS_EFETIVAS} — assistido: linha da Tabela de Alíquotas
 * Efetivas do Simples Nacional do contribuinte no período de apuração,
 * recebida pronta pelo núcleo. CBS nula no par escolhido é dado
 * inconsistente: o item sai {@code NAO_CALCULADO} (motivo
 * {@code ANEXO_SEM_ALIQUOTA}).</li>
 * </ul>
 */
public enum OrigemLinhaAliquotasEnum {
    REQUISICAO,
    TABELA_ALIQUOTAS_EFETIVAS
}
