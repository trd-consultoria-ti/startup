package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 * Vocabulário em que o documento fiscal informa o regime do emitente,
 * parametrizado por documento na coluna {@code TDCS_VOCABULARIO_REGIME} da
 * tabela {@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}:
 * <ul>
 * <li>{@link #CRT} — família CRT (NF-e, NFC-e, CT-e, CT-e OS, BP-e, BP-e TM e
 * NFCom): campos {@code CRT} e {@code tpRegimeIBSCBS};</li>
 * <li>{@link #NFSE} — família NFS-e: campos {@code opSimpNac},
 * {@code regApIBSCBSSN} e {@code regApTribSN} (DPS, grupo
 * {@code prest/regTrib}).</li>
 * </ul>
 * O vocabulário também seleciona as linhas específicas da tabela de efeitos do
 * tpRBSN ({@code TRBS_VOCABULARIO_REGIME}) e as linhas da tabela de
 * enquadramento do regime.
 */
public enum VocabularioRegimeSimplesEnum {
    CRT,
    NFSE
}
