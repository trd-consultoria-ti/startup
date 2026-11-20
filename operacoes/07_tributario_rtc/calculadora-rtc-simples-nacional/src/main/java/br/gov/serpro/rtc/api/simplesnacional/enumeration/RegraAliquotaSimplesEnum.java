package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 * Regra que escolheu o par de alíquotas do item (campo {@code regraAliquota}
 * de {@code resolucaoAliquotas.itens}).
 *
 * <p>
 * A ORDEM DAS CONSTANTES É A ORDEM DE AVALIAÇÃO: a primeira regra que se
 * aplica decide. As regras 4, 6, 7 e 8 são alimentadas pela tabela
 * {@code SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL} (coluna
 * {@code SIPA_REGRA}); a ordem fica aqui, em código.
 */
public enum RegraAliquotaSimplesEnum {

    /** 1 — atividade não admitida no tpDoc: item {@code NAO_CALCULADO}, sem tipo de alíquota. */
    DOCUMENTO,

    /** 2 — atividade sem anexo (código 90): {@code SEM_ALIQUOTA}. */
    ATIVIDADE_NAO_TRIBUTADA,

    /** 3 — anexo I ou II: {@code PAR_UNICO}. */
    ANEXO_SEM_ISS,

    /** 4 — impedimento pelo sublimite, CRT 2, regApIBSCBSSN 2 ou regApTribSN 2 ou 3: sem redistribuição. */
    ISS_FORA_DO_SIMPLES,

    /** 5 — atividade sem ISS no Simples (indicador do catálogo): sem redistribuição. */
    ATIVIDADE_SEM_ISS,

    /** 6 — tribISSQN 2, 3 ou 4: sem redistribuição. */
    ISS_NAO_INCIDE,

    /** 7 — tpBM 1 (isenção municipal): sem redistribuição. */
    ISENCAO_MUNICIPAL,

    /** 8 — tpRetISSQN 2 ou 3 (ISS retido): sem redistribuição. */
    ISS_RETIDO,

    /** 9 — nenhuma das anteriores: com redistribuição. */
    PADRAO
}
