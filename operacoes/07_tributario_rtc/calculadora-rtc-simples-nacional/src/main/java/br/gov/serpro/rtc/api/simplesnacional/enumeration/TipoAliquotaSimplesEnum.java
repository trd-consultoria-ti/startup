package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 * Tipo da alíquota aplicada ao item (campo {@code tipoAliquota} de
 * {@code resolucaoAliquotas.itens}):
 * <ul>
 * <li>{@link #SEM_ALIQUOTA} — atividade sem anexo (código 90): alíquotas
 * zero;</li>
 * <li>{@link #PAR_UNICO} — anexo I ou II, que só têm um par de alíquotas;</li>
 * <li>{@link #SEM_REDISTRIBUICAO} — par sem redistribuição do ISS (anexos III,
 * IV e V);</li>
 * <li>{@link #COM_REDISTRIBUICAO} — par com redistribuição do ISS (anexos III,
 * IV e V).</li>
 * </ul>
 */
public enum TipoAliquotaSimplesEnum {
    SEM_ALIQUOTA,
    PAR_UNICO,
    SEM_REDISTRIBUICAO,
    COM_REDISTRIBUICAO
}
