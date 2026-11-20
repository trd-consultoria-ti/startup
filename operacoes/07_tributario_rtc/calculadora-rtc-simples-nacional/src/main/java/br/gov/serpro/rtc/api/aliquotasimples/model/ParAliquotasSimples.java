package br.gov.serpro.rtc.api.aliquotasimples.model;

import java.math.BigDecimal;

/**
 * Um par de alíquotas efetivas do Simples Nacional — CBS e IBS — em PONTOS
 * PERCENTUAIS, com a precisão integral recebida ou lida. Qualquer um pode ser
 * nulo: o IBS vem nulo, legitimamente, quando o contribuinte está impedido
 * pelo sublimite.
 *
 * <p>
 * ISOLADO. Valor do núcleo, sem acesso a banco.
 *
 * @param pCBSSN alíquota da CBS (pontos percentuais), ou {@code null}
 * @param pIBSSN alíquota do IBS (pontos percentuais), ou {@code null}
 */
public record ParAliquotasSimples(BigDecimal pCBSSN, BigDecimal pIBSSN) {

    /** Par de alíquotas zero (atividade sem anexo, código 90). */
    public static final ParAliquotasSimples ZERO = new ParAliquotasSimples(BigDecimal.ZERO, BigDecimal.ZERO);
}
