package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Memória de cálculo textual da CBS e do IBS do item do Simples Nacional.
 *
 */
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "cbs", "ibs" })
public record MemoriaCalculoSimplesDomain(

        @Schema(description = "Memória de cálculo da CBS")
        String cbs,

        @Schema(description = "Memória de cálculo do IBS. Suprimida para CRT=2")
        String ibs) {
}
