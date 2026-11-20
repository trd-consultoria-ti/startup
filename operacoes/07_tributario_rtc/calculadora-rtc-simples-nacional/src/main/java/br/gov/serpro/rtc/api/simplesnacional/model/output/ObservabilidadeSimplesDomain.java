package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Resposta do endpoint de observabilidade do Simples Nacional.
 *
 * <p>Contém:
 * <ul>
 * <li>o repasse da operação ({@code oper})</li>
 * <li>a resolução das alíquotas ({@code resolucaoAliquotas}) — nos endpoints
 * não assistido e assistido, quando o regime calcula tributos; nulo e omitido
 * no endpoint clássico (contrato existente intacto)</li>
 * <li>os itens com estado de processamento ({@code objetos})</li>
 * <li>o total consolidado dos itens calculados ({@code total})</li>
 * </ul>
 *
 */
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "oper", "resolucaoAliquotas", "objetos", "total" })
public record ObservabilidadeSimplesDomain(
        OperacaoSimplesDomain oper,

        @JsonInclude(NON_NULL)
        @Schema(description = "Resolução das alíquotas efetivas por item. Ausente no endpoint clássico e no "
                + "regime SOMENTE_RECEITA", nullable = true)
        ResolucaoAliquotasDomain resolucaoAliquotas,

        List<ObjetoSimplesDomain> objetos,
        TotalSimplesDomain total) {

    /** Resposta do endpoint clássico: sem bloco de resolução de alíquotas. */
    public ObservabilidadeSimplesDomain(OperacaoSimplesDomain oper, List<ObjetoSimplesDomain> objetos,
            TotalSimplesDomain total) {
        this(oper, null, objetos, total);
    }
}
