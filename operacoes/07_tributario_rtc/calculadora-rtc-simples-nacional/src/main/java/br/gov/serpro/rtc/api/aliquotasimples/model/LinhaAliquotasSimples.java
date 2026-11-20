package br.gov.serpro.rtc.api.aliquotasimples.model;

import java.util.Objects;

/**
 * A LINHA DE ALÍQUOTAS do contribuinte no período de apuração, como valor do
 * núcleo do cálculo, independente de onde veio: do corpo da requisição (não
 * assistido) ou da Tabela de Alíquotas Efetivas do Simples Nacional
 * (assistido). Mandada no corpo, a mesma linha produz o mesmo resultado.
 *
 * <p>
 * Alíquotas em PONTOS PERCENTUAIS, com a precisão integral (alíquota recebida em
 * fração é convertida deslocando a vírgula, sem arredondar). Cada anexo
 * pode faltar ({@code null}): só é exigido o que algum item usa.
 *
 * <p>
 * ISOLADO. Valor do núcleo, sem acesso a banco.
 *
 * @param fatorR               {@code true} = fator "r" igual ou superior a 28%; {@code null} quando não
 *                             informado (só é exigido para atividade sujeita ao fator "r" — VSN-040)
 * @param impedimentoSublimite {@code true} = impedido de recolher IBS e ISS pelo Simples por exceder o
 *                             sublimite
 * @param anexoI               alíquotas do Anexo I
 * @param anexoII              alíquotas do Anexo II
 * @param anexoIII             alíquotas do Anexo III
 * @param anexoIV              alíquotas do Anexo IV
 * @param anexoV               alíquotas do Anexo V
 * @param origem               de onde veio a linha
 */
public record LinhaAliquotasSimples(
        Boolean fatorR,
        boolean impedimentoSublimite,
        AliquotasAnexoSimples anexoI,
        AliquotasAnexoSimples anexoII,
        AliquotasAnexoSimples anexoIII,
        AliquotasAnexoSimples anexoIV,
        AliquotasAnexoSimples anexoV,
        OrigemLinhaAliquotasEnum origem) {

    public LinhaAliquotasSimples {
        Objects.requireNonNull(origem, "origem é obrigatória");
    }

    /**
     * Alíquotas do anexo informado em numeral romano ({@code I..V}); {@code null}
     * quando a linha não traz o anexo.
     *
     * @throws IllegalArgumentException para anexo fora de {@code I..V}
     */
    public AliquotasAnexoSimples anexo(String anexo) {
        return switch (anexo) {
            case "I" -> anexoI;
            case "II" -> anexoII;
            case "III" -> anexoIII;
            case "IV" -> anexoIV;
            case "V" -> anexoV;
            default -> throw new IllegalArgumentException("Anexo inválido: " + anexo);
        };
    }

    /** {@code true} para os anexos com ISS na repartição (III, IV e V), os únicos com dois pares. */
    public static boolean anexoComIss(String anexo) {
        return "III".equals(anexo) || "IV".equals(anexo) || "V".equals(anexo);
    }
}
