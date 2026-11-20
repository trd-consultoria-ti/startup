package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonSerialize;

import br.gov.serpro.rtc.shared.serializer.BigDecimalTDec1302Serializer;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Valor do Imposto Seletivo (IS) repassado na saída do Simples Nacional.
 *
 * <p>O endpoint não recalcula o IS: apenas repassa o {@code valorImpostoSeletivo}
 * recebido na entrada para compor o total consolidado.
 *
 */
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "valor" })
public record ImpostoSeletivoSimplesDomain(

        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(description = "Valor do Imposto Seletivo")
        BigDecimal valor) {
}
