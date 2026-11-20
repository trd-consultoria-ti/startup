package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 * Código NEUTRO do regime de apuração do IBS e da CBS no Simples Nacional,
 * coluna {@code RASN_CD} da tabela {@code REGIME_APURACAO_SIMPLES_NACIONAL}. O
 * regime chega no vocabulário do documento (CRT ou NFS-e) e é convertido neste
 * código pela tabela de enquadramento; daí em diante o cálculo não sabe de que
 * documento veio.
 *
 * <p>
 * O comportamento de cada código (calcula ou não, gera IBS, destino dos
 * valores) é parametrizado em banco.
 */
public enum CodigoRegimeApuracaoSimplesEnum {

    /** IBS e CBS pelo Simples (CRT 1; NFS-e 3+1). */
    SN_PLENO,

    /** Só a CBS pelo Simples, por excesso de sublimite (CRT 2; NFS-e 3+2). */
    SN_SUBLIMITE,

    /** Opção pelo Simples pendente: tudo fica pendente/suspenso (CRT 3 + tpRegimeIBSCBS 2; NFS-e 4+1). */
    PENDENTE,

    /** Opção pendente, só a CBS (NFS-e 4+2). Hipótese, ainda sem confirmação. */
    PENDENTE_SUBLIMITE,

    /**
     * Optou pelo regime regular do IBS e da CBS: só a receita bruta, sem
     * cálculo de tributos (CRT 1 ou 2 + tpRegimeIBSCBS 1; NFS-e 3+3 e 4+3).
     */
    SOMENTE_RECEITA
}
