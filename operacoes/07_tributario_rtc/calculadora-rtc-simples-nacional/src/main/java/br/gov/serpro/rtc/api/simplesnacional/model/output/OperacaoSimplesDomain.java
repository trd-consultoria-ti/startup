package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.CodigoRegimeApuracaoSimplesEnum;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Repasse dos dados relevantes da operação do Simples Nacional na saída
 * (NT 2026.005). Os endpoints novos ecoam os campos de regime no vocabulário do
 * documento e acrescentam {@code regimeApuracao}, o código neutro; no
 * clássico esses campos ficam nulos e são omitidos.
 *
 */
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "data", "tpDoc", "CRT", "tpRegimeIBSCBS", "opSimpNac", "regApIBSCBSSN", "regApTribSN",
        "tpEnteGov", "regimeApuracao" })
public record OperacaoSimplesDomain(

        @Schema(description = "Data do fato gerador")
        String data,

        @Schema(description = "Tipo de documento fiscal eletrônico da operação")
        String tpDoc,

        @SuppressWarnings("java:S116") // Nomenclatura oficial VERBATIM do leiaute DF-e/NT 2026.005.
        @JsonProperty("CRT")
        @Schema(name = "CRT", description = "Código de Regime Tributário do emitente (documentos com CRT)")
        String CRT,

        @Schema(description = "Tipo de regime de apuração do IBS/CBS, quando informado (documentos com CRT)")
        String tpRegimeIBSCBS,

        @Schema(description = "Situação do prestador perante o Simples Nacional (NFS-e)")
        String opSimpNac,

        @JsonProperty("regApIBSCBSSN")
        @Schema(name = "regApIBSCBSSN", description = "Regime de apuração do IBS e da CBS pelo Simples Nacional (NFS-e)")
        String regApIBSCBSSN,

        @JsonProperty("regApTribSN")
        @Schema(name = "regApTribSN", description = "Regime de apuração dos tributos federais e do ISS pelo Simples "
                + "Nacional (NFS-e)")
        String regApTribSN,

        @Schema(description = "Tipo de ente governamental adquirente (compra governamental), quando informado")
        String tpEnteGov,

        @Schema(description = "Regime de apuração (código neutro) a que os campos de regime "
                + "do documento levam: SN_PLENO, SN_SUBLIMITE, PENDENTE, PENDENTE_SUBLIMITE ou SOMENTE_RECEITA "
                + "(optante pelo regime regular do IBS e da CBS: os itens saem NAO_CALCULADO, só com a receita "
                + "bruta). Ausente no endpoint clássico", example = "SN_PLENO", nullable = true)
        CodigoRegimeApuracaoSimplesEnum regimeApuracao) {

    /** Repasse do endpoint clássico (vocabulário CRT, sem o código neutro). */
    public OperacaoSimplesDomain(String data, String tpDoc, String CRT, String tpRegimeIBSCBS, String tpEnteGov) {
        this(data, tpDoc, CRT, tpRegimeIBSCBS, null, null, null, tpEnteGov, null);
    }
}
