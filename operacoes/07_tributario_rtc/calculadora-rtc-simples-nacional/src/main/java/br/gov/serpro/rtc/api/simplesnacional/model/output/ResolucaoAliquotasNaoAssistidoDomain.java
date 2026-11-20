package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Bloco {@code resolucaoAliquotas} do cálculo NÃO ASSISTIDO: o cabeçalho é o
 * da linha recebida em {@code aliquotasEfetivas} ({@code fatorR} e
 * {@code indSublimite}), seguido da resolução de cada item e dos avisos da
 * operação.
 *
 */
@JsonInclude(ALWAYS)
@JsonPropertyOrder({ "fatorR", "indSublimite", "itens", "avisos" })
@Schema(name = "ResolucaoAliquotasNaoAssistido", description = "Resolução das alíquotas do cálculo não assistido: a "
        + "linha recebida em aliquotasEfetivas")
public record ResolucaoAliquotasNaoAssistidoDomain(

        @Schema(description = "fatorR recebido em aliquotasEfetivas (true = fator r igual ou superior a 28%); null "
                + "quando não informado", example = "false", nullable = true)
        Boolean fatorR,

        @Schema(description = "impedimentoSublimite recebido em aliquotasEfetivas (padrão false)", example = "false")
        boolean indSublimite,

        @ArraySchema(arraySchema = @Schema(description = "Resolução das alíquotas de cada item, na ordem dos "
                + "itens de entrada"))
        List<ResolucaoAliquotaItemDomain> itens,

        @ArraySchema(arraySchema = @Schema(description = "Avisos da operação, que não bloqueiam o cálculo. Lista "
                + "vazia quando não há"), schema = @Schema(example = "regApIBSCBSSN 2 (excesso de sublimite) com "
                        + "regApTribSN 1: incoerente — quem excede o sublimite tem o ISS fora do Simples Nacional"))
        List<String> avisos) implements ResolucaoAliquotasDomain {

    public ResolucaoAliquotasNaoAssistidoDomain {
        itens = itens == null ? List.of() : List.copyOf(itens);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }
}
