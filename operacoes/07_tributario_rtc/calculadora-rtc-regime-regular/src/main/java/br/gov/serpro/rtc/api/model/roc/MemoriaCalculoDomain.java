package br.gov.serpro.rtc.api.model.roc;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import br.gov.serpro.rtc.api.model.SerializationVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reúne a memória de cálculo textual da CBS, do IBS estadual e do IBS
 * municipal do item.
 */
@Getter
@Setter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "cbs", "ibsUF", "ibsMun" })
public class MemoriaCalculoDomain implements SerializationVisibility {

    @Schema(description = "Memória de cálculo da CBS")
    private String cbs;

    @Schema(description = "Memória de cálculo do IBS estadual")
    private String ibsUF;

    @Schema(description = "Memória de cálculo do IBS municipal")
    private String ibsMun;

}
