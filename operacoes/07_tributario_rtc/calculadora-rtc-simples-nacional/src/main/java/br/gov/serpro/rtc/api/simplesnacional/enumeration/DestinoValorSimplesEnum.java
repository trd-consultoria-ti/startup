package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 * Destino de um valor de tributo calculado pelo Simples Nacional, parametrizado
 * por regime de apuração nas colunas {@code RASN_DESTINO_DEVIDO} e
 * {@code RASN_DESTINO_PENDENTE_SUSPENSO} da tabela
 * {@code REGIME_APURACAO_SIMPLES_NACIONAL}:
 * <ul>
 * <li>{@link #DEVIDO} — o valor sai nos campos de tributo devido
 * ({@code vIBSSN}/{@code vCBSSN}) e compõe o total do item;</li>
 * <li>{@link #PENDENTE_SUSPENSO} — o valor sai nos campos de tributo pendente
 * ou suspenso ({@code vIBSPendSusp}/{@code vCBSPendSusp}), fora do total do
 * item;</li>
 * <li>{@link #ZERO} — o valor é descartado (sai zero em ambos os campos).</li>
 * </ul>
 */
public enum DestinoValorSimplesEnum {
    DEVIDO,
    PENDENTE_SUSPENSO,
    ZERO
}
