package br.gov.serpro.rtc.api.simplesnacional.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * A linha de alíquotas efetivas do contribuinte no período de apuração,
 * informada no corpo do cálculo NÃO ASSISTIDO: a mesma linha que o PGDAS
 * publica na Tabela de Alíquotas Efetivas do Simples Nacional.
 *
 * <p>Alíquotas em PONTOS PERCENTUAIS (como {@code aliquotasNominais} na
 * api-regime-geral e o {@code tribCalc}), como TEXTO — o JavaScript perde
 * dígitos com as 28 casas do PGDAS —, com até 3 dígitos inteiros e 34 casas,
 * sem sinal e sem expoente. Quem tem a alíquota em fração desloca a vírgula
 * duas casas no texto, sem aritmética de ponto flutuante.
 *
 * <p>Só é exigido o que algum item usa; o que vier tem o formato validado
 * (VSN-005/VSN-007, erro global).
 *
 */
public record AliquotasEfetivasInput(

        @Schema(name = "fatorR", description = "true = fator \"r\" igual ou superior a 28% (Anexo III para a atividade "
                + "09); false = inferior (Anexo V). Exigido quando algum item tem a atividade 09 (VSN-040)",
                example = "false", nullable = true)
        Boolean fatorR,

        @Schema(name = "impedimentoSublimite", description = "true = impedido de recolher IBS e ISS pelo Simples por "
                + "exceder o sublimite (o IBS da linha vem nulo e os anexos III a V usam o par sem redistribuição "
                + "do ISS). Padrão false", example = "false", nullable = true)
        Boolean impedimentoSublimite,

        @JsonProperty("anexoI")
        @Schema(name = "anexoI", description = "Alíquotas do Anexo I", nullable = true)
        AnexoParUnicoInput anexoI,

        @JsonProperty("anexoII")
        @Schema(name = "anexoII", description = "Alíquotas do Anexo II", nullable = true)
        AnexoParUnicoInput anexoII,

        @JsonProperty("anexoIII")
        @Schema(name = "anexoIII", description = "Alíquotas do Anexo III, com o par sem redistribuição do ISS",
                nullable = true)
        AnexoComIssInput anexoIII,

        @JsonProperty("anexoIV")
        @Schema(name = "anexoIV", description = "Alíquotas do Anexo IV, com o par sem redistribuição do ISS",
                nullable = true)
        AnexoComIssInput anexoIV,

        @JsonProperty("anexoV")
        @Schema(name = "anexoV", description = "Alíquotas do Anexo V, com o par sem redistribuição do ISS",
                nullable = true)
        AnexoComIssInput anexoV) {

    /** Par de alíquotas (pontos percentuais, texto). */
    public record ParAliquotasInput(

            @JsonProperty("pCBSSN")
            @Schema(name = "pCBSSN", description = "Alíquota efetiva da CBS, em pontos percentuais (texto)",
                    example = "2.3591", nullable = true)
            String pCBSSN,

            @JsonProperty("pIBSSN")
            @Schema(name = "pIBSSN", description = "Alíquota efetiva do IBS, em pontos percentuais (texto); nula com "
                    + "impedimento pelo sublimite", example = "0.0260", nullable = true)
            String pIBSSN) {
    }

    /** Anexo com um só par de alíquotas (anexos I e II, sem ISS na repartição). */
    public record AnexoParUnicoInput(

            @JsonProperty("pCBSSN")
            @Schema(name = "pCBSSN", description = "Alíquota efetiva da CBS, em pontos percentuais (texto)",
                    example = "1.1612", nullable = true)
            String pCBSSN,

            @JsonProperty("pIBSSN")
            @Schema(name = "pIBSSN", description = "Alíquota efetiva do IBS, em pontos percentuais (texto); nula com "
                    + "impedimento pelo sublimite", example = "0.0129", nullable = true)
            String pIBSSN) {
    }

    /** Anexo com ISS na repartição (III, IV e V): o par com redistribuição e o par sem. */
    public record AnexoComIssInput(

            @JsonProperty("pCBSSN")
            @Schema(name = "pCBSSN", description = "Alíquota efetiva da CBS com redistribuição do ISS, em pontos "
                    + "percentuais (texto)", example = "2.3874", nullable = true)
            String pCBSSN,

            @JsonProperty("pIBSSN")
            @Schema(name = "pIBSSN", description = "Alíquota efetiva do IBS com redistribuição do ISS, em pontos "
                    + "percentuais (texto); nula com impedimento pelo sublimite", example = "0.0263", nullable = true)
            String pIBSSN,

            @JsonProperty("semRedistribuicaoISS")
            @Schema(name = "semRedistribuicaoISS", description = "Par sem redistribuição do ISS (usado quando o ISS "
                    + "não entra no cálculo do Simples). Ausente: vale o par com "
                    + "redistribuição, com aviso (os dois coincidem quando o ISS não passa do teto)", nullable = true)
            ParAliquotasInput semRedistribuicaoISS) {
    }
}
