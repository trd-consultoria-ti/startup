package br.gov.serpro.rtc.api.aliquotasimples.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Desfecho da resolução "cAtivSN → anexo → par de alíquotas" de um item
 * (campo {@code motivo} de {@code resolucaoAliquotas.itens}).
 *
 * <p>
 *
 * ISOLADO.
 *
 * Vive sob {@code br.gov.serpro.rtc.api.aliquotasimples} e não depende do
 * pacote {@code simplesnacional} nem do domínio do regime geral.
 *
 * <p>
 * Quem decide o ESTADO do item é a engine de cálculo: a resolução apenas
 * entrega (ou não) as alíquotas. Sem cAtivSN válido o cálculo não é realizado
 * (NT 2026.005, seção 5.3.3) — daí as alíquotas nulas em
 * {@link #ATIVIDADE_DESCONHECIDA}.
 *
 */
@Schema(description = "Desfecho da resolução cAtivSN → anexo → par de alíquotas de um item.")
public enum MotivoResolucaoAliquota {

    /**
     * Atividade vigente com anexo, e o anexo possui alíquota de CBS na tabela
     * do contribuinte: alíquotas resolvidas (IBS nulo quando a tabela não o
     * traz — contribuinte impedido pelo sublimite).
     */
    RESOLVIDA,

    /**
     * Atividade vigente SEM anexo de apuração (código 90, Operações não
     * Tributáveis): alíquotas zero por definição de negócio.
     */
    ATIVIDADE_SEM_ANEXO,

    /**
     * cAtivSN nulo, desconhecido ou fora de vigência na data: alíquotas nulas
     * — a engine responde {@code NAO_CALCULADO} (NT 2026.005, seção 5.3.3).
     */
    ATIVIDADE_DESCONHECIDA,

    /**
     * Anexo resolvido, mas a linha de alíquotas não tem a CBS do par escolhido:
     * alíquotas nulas. No assistido é dado inconsistente e o item sai
     * {@code NAO_CALCULADO}; no não assistido, se a alíquota é exigida, o item
     * sai com VSN-004.
     */
    ANEXO_SEM_ALIQUOTA,

    /**
     * Atividade vigente, mas não admitida no tipo de documento da operação
     * (regra 1 do par, {@code DOCUMENTO}): o item sai {@code NAO_CALCULADO}.
     */
    ATIVIDADE_INADMISSIVEL_NO_DOCUMENTO,

    /**
     * Atividade sujeita ao fator "r" (a 09) sem o {@code fatorR} na linha de
     * alíquotas: o anexo não pode ser escolhido (VSN-040).
     */
    FATOR_R_AUSENTE,

    /**
     * cClassTrib sem tratamento implementado (VSN-027): o item não chega à
     * resolução das alíquotas.
     */
    ITEM_NAO_IMPLEMENTADO
}
