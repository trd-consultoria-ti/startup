package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.EstadoItemSimplesEnum;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * <p>Item do resultado do Simples Nacional com:
 * <ul>
 * <li>número do objeto ({@code nObj})</li>
 * <li>estado do processamento ({@code estadoItem})</li>
 * <li>motivo do estado ({@code motivoEstado}), somente no {@code NAO_CALCULADO}</li>
 * <li>tributos calculados ({@code tribCalc})</li>
 * <li>erros detalhados ({@code erros})</li>
 * </ul>
 *
 */
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "nObj", "estadoItem", "motivoEstado", "tribCalc", "erros" })
public record ObjetoSimplesDomain(
        Integer nObj,
        EstadoItemSimplesEnum estadoItem,
        @Schema(description = "Por que o IBS e a CBS do item não foram calculados: presente somente quando "
                + "estadoItem = NAO_CALCULADO")
        MotivoEstadoSimplesDomain motivoEstado,
        TributosSimplesDomain tribCalc,
        List<ErroDetalhe> erros) {

    /** Item sem motivo de estado (qualquer estado que não {@code NAO_CALCULADO}). */
    public ObjetoSimplesDomain(Integer nObj, EstadoItemSimplesEnum estadoItem, TributosSimplesDomain tribCalc,
            List<ErroDetalhe> erros) {
        this(nObj, estadoItem, null, tribCalc, erros);
    }
}
