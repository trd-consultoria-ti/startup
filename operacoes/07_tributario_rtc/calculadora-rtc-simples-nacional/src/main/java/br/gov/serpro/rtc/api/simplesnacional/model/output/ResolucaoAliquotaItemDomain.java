package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonSerialize;

import br.gov.serpro.rtc.api.aliquotasimples.model.MotivoResolucaoAliquota;
import br.gov.serpro.rtc.api.aliquotasimples.serializer.BigDecimalTextoIntegralSerializer;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegraAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.TipoAliquotaSimplesEnum;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Resolução das alíquotas efetivas de UM item (bloco
 * {@code resolucaoAliquotas.itens}, nos endpoints não assistido e
 * assistido): o anexo aplicado, o tipo de alíquota e a regra que o escolheu,
 * as alíquotas do par, o motivo do desfecho ({@link MotivoResolucaoAliquota})
 * e os avisos do item.
 *
 * <p>Inclusão {@code ALWAYS}: {@code anexoAplicado}, {@code tipoAliquota},
 * {@code regraAliquota}, {@code pIBSSN} e {@code pCBSSN} saem como
 * {@code null} quando não resolvidos — o consumidor distingue "não resolvido"
 * de "zero" ({@code "0"}, caso da atividade sem anexo). As alíquotas são as
 * RECEBIDAS ou LIDAS, com a precisão integral, antes da compra governamental e
 * do arredondamento do leiaute (2 casas na NFS-e), e permanecem presentes
 * mesmo quando a matriz de regime as suprime na saída do cálculo (ex.: IBS no
 * excesso de sublimite). A memória de cálculo mostra a alíquota efetivamente
 * usada.
 *
 * <p>Alíquotas em pontos percentuais, como texto (ex.:
 * {@code "1.6839379378389463610262954"}): números JSON de 28 dígitos perderiam
 * dígitos em leitores que os convertem em {@code double}.
 *
 */
@JsonInclude(ALWAYS)
@JsonPropertyOrder({ "numero", "cAtivSN", "anexoAplicado", "tipoAliquota", "regraAliquota", "pIBSSN", "pCBSSN",
        "motivo", "avisos" })
public record ResolucaoAliquotaItemDomain(

        @Schema(description = "Número do item na operação", example = "1")
        Integer numero,

        @JsonProperty("cAtivSN")
        @Schema(name = "cAtivSN", description = "Código de atividade do item, normalizado (sem espaços; um único "
                + "dígito ganha o zero à esquerda: \"7\" ⇒ \"07\")", example = "09", nullable = true)
        String cAtivSN,

        @Schema(description = "Anexo aplicado ao item, em numeral romano (I..V); null quando a atividade não "
                + "possui anexo (código 90) ou não foi resolvida", example = "III", nullable = true)
        String anexoAplicado,

        @Schema(description = "Tipo da alíquota aplicada: SEM_ALIQUOTA (código 90, zero), PAR_UNICO (anexos I e "
                + "II), SEM_REDISTRIBUICAO ou COM_REDISTRIBUICAO do ISS (anexos III, IV e V); null quando o par "
                + "não foi escolhido (atividade desconhecida ou não admitida no documento)",
                example = "COM_REDISTRIBUICAO", nullable = true)
        TipoAliquotaSimplesEnum tipoAliquota,

        @Schema(description = "Regra que escolheu o par: DOCUMENTO, "
                + "ATIVIDADE_NAO_TRIBUTADA, ANEXO_SEM_ISS, ISS_FORA_DO_SIMPLES, ATIVIDADE_SEM_ISS, ISS_NAO_INCIDE, "
                + "ISENCAO_MUNICIPAL, ISS_RETIDO ou PADRAO; null quando a atividade não foi resolvida",
                example = "PADRAO", nullable = true)
        RegraAliquotaSimplesEnum regraAliquota,

        @JsonProperty("pIBSSN")
        @JsonSerialize(using = BigDecimalTextoIntegralSerializer.class)
        @Schema(name = "pIBSSN", type = "string", description = "Alíquota efetiva de IBS do par aplicado, com a "
                + "precisão integral recebida ou lida (pontos percentuais, texto), antes da compra governamental e "
                + "do arredondamento do leiaute; \"0\" para atividade sem anexo; null quando não resolvida ou "
                + "quando a linha não traz IBS (impedimento pelo sublimite)",
                example = "0.01867380622521988789135487", nullable = true)
        BigDecimal pIBSSN,

        @JsonProperty("pCBSSN")
        @JsonSerialize(using = BigDecimalTextoIntegralSerializer.class)
        @Schema(name = "pCBSSN", type = "string", description = "Alíquota efetiva de CBS do par aplicado, com a "
                + "precisão integral recebida ou lida (pontos percentuais, texto), antes da compra governamental e "
                + "do arredondamento do leiaute; \"0\" para atividade sem anexo; null quando não resolvida",
                example = "1.6839379378389463610262954", nullable = true)
        BigDecimal pCBSSN,

        @Schema(description = "Desfecho da resolução: RESOLVIDA, ATIVIDADE_SEM_ANEXO (código 90, alíquotas zero), "
                + "ATIVIDADE_DESCONHECIDA (cAtivSN nulo/desconhecido/fora de vigência: item NAO_CALCULADO), "
                + "ATIVIDADE_INADMISSIVEL_NO_DOCUMENTO (item NAO_CALCULADO), ANEXO_SEM_ALIQUOTA (a linha não tem a "
                + "CBS do par), FATOR_R_AUSENTE (VSN-040) ou ITEM_NAO_IMPLEMENTADO (VSN-027)",
                example = "RESOLVIDA")
        MotivoResolucaoAliquota motivo,

        @ArraySchema(arraySchema = @Schema(description = "Avisos do item, que não bloqueiam o cálculo (ex.: campo "
                + "do ISS desconsiderado; par sem redistribuição ausente na linha). Lista vazia quando não há"),
                schema = @Schema(example = "tpBM desconsiderado: a atividade 07 não admite benefício municipal "
                        + "do ISS (regra E0536 da NFS-e)"))
        List<String> avisos) {

    public ResolucaoAliquotaItemDomain {
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }
}
