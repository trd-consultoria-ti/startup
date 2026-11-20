package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Bloco {@code resolucaoAliquotas} do cálculo ASSISTIDO: os dados do
 * contribuinte no período de apuração (a linha da Tabela de Alíquotas Efetivas
 * do Simples Nacional usada — a vigente do período, quando há várias), a
 * resolução de cada item e os avisos da operação (ex.: regime
 * informado divergente do indicador de sublimite do contribuinte).
 *
 * <p>Valor da resposta, sem acesso a banco: vive no núcleo, como o resto da
 * saída.
 *
 */
@JsonInclude(ALWAYS)
@JsonPropertyOrder({ "niContribuinte", "pa", "indSublimite", "fatorR", "dataHoraProcessamento", "nsu",
        "itens", "avisos" })
@Schema(name = "ResolucaoAliquotasAssistido", description = "Resolução das alíquotas do cálculo assistido: a linha "
        + "da tabela de alíquotas do contribuinte no período de apuração")
public record ResolucaoAliquotasAssistidoDomain(

        @Schema(description = "Identificação do contribuinte normalizada (14 caracteres, sem máscara, maiúscula)",
                example = "5YPK4YVG000125")
        String niContribuinte,

        @Schema(description = "Período de apuração derivado da data do fato gerador (AAAAMM)", example = "202703")
        String pa,

        @Schema(description = "Indicador de impedimento por excesso de sublimite no PA (indicadorImpedimentoSublimite "
                + "da tabela de alíquotas; quando true, a tabela não traz IBS e o par sem redistribuição do ISS é "
                + "o usado nos anexos III a V)", example = "false")
        boolean indSublimite,

        @Schema(description = "true se e somente se o fator r é igual ou superior a 28% no PA (Anexo III para a "
                + "atividade 09)", example = "true")
        boolean fatorR,

        @Schema(description = "Data e hora (UTC) em que o PGDAS processou as alíquotas usadas (dataHoraProcessamento "
                + "da tabela de alíquotas). Com várias linhas no período, o cálculo usa a vigente: a de maior "
                + "dataHoraProcessamento; empate, maior NSU; depois, a gravada por último",
                example = "2026-09-30T17:54:28.868487Z")
        OffsetDateTime dataHoraProcessamento,

        @Schema(description = "NSU da linha usada: ordem de chegada da mensagem na RTC (dado de infraestrutura, "
                + "não vem no JSON do PGDAS); critério de desempate da linha vigente", example = "615")
        String nsu,

        @ArraySchema(arraySchema = @Schema(description = "Resolução das alíquotas de cada item, na ordem dos "
                + "itens de entrada"))
        List<ResolucaoAliquotaItemDomain> itens,

        @ArraySchema(arraySchema = @Schema(description = "Avisos da operação, que não bloqueiam o cálculo (ex.: "
                + "regime informado diverge do indicador de sublimite do contribuinte). Lista vazia quando não há"),
                schema = @Schema(example = "Regime de apuração SN_PLENO (CRT=1) diverge do indicador de "
                        + "impedimento por sublimite da linha de alíquotas (true)"))
        List<String> avisos) implements ResolucaoAliquotasDomain {

    public ResolucaoAliquotasAssistidoDomain {
        itens = itens == null ? List.of() : List.copyOf(itens);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }
}
