package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonSerialize;

import br.gov.serpro.rtc.shared.serializer.BigDecimalTDec0302_04Serializer;
import br.gov.serpro.rtc.shared.serializer.BigDecimalTDec1302Serializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 *
 * Tributos calculados de um item do Simples Nacional, espelhando o leiaute do
 * Evento de Tributação do Simples Nacional (tpEvento 412140) da NT 2026.005.
 *
 * <p>A supressão de tags exigida pela NT é feita por {@code @JsonInclude(NON_NULL)}:
 * para CRT=2 os campos de IBS ({@code pIBSSN}, {@code vIBSSN}, {@code vIBSPendSusp})
 * não são gerados; para itens {@code NAO_CALCULADO} apenas {@code vRBSNitem} e
 * {@code tpRBSN} são preenchidos.
 *
 * <p>Record com {@code @Builder} (12 componentes, construção condicional no
 * serviço de cálculo).
 *
 */
@Builder
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "vRBSNitem", "tpRBSN", "tpRBSNDescricao", "vBC", "pIBSSN", "vIBSSN", "vIBSPendSusp", "pCBSSN",
        "vCBSSN", "vCBSPendSusp", "IS", "totalItem", "memoriaCalculo" })
public record TributosSimplesDomain(

        @JsonProperty("vRBSNitem")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vRBSNitem", description = "Receita bruta do Simples Nacional do item (NT 2026.005): "
                + "a baseCalculo, ou zero quando tpRBSN=0. O valorImpostoSeletivo NÃO compõe a receita bruta "
                + "(parametrização por vigência, atualmente desligada em todas as vigências — divergência "
                + "deliberada com a fórmula do campo P26 da NT)")
        BigDecimal vRBSNitem,

        @JsonProperty("tpRBSN")
        @Schema(name = "tpRBSN", description = "Tipo de Receita Bruta do Simples Nacional, resolvido do cClassTrib "
                + "(NT 2026.005): 0, 1, 2, 3, 4, 5 ou 9")
        Integer tpRBSN,

        @JsonProperty("tpRBSNDescricao")
        @Schema(name = "tpRBSNDescricao", description = "Descrição do Tipo de Receita Bruta do Simples Nacional "
                + "(texto oficial do leiaute do evento da NT 2026.005; ex.: \"Receita bruta - interna\"), vinda da "
                + "tabela de tipos de receita bruta vigente na data do fato gerador")
        String tpRBSNDescricao,

        @JsonProperty("vBC")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vBC", description = "Base de cálculo de IBS/CBS do item: vRBSNitem × fator do tipo de "
                + "receita bruta, resolvido pela vigência da data do fato gerador (tpRBSN 1 e 4: fator 1,0; "
                + "tpRBSN 5: 0,5; tpRBSN 0, 2, 3 e 9: 0 — retorna 0.00). Suprimido para item NAO_CALCULADO: "
                + "o cálculo não ocorreu e emitir uma base de cálculo sugeriria o contrário (o contrato do "
                + "NAO_CALCULADO — apenas vRBSNitem, tpRBSN e tpRBSNDescricao — permanece intacto); o item "
                + "também não contribui para o vBC do total")
        BigDecimal vBC,

        @JsonProperty("pIBSSN")
        @JsonSerialize(using = BigDecimalTDec0302_04Serializer.class)
        @Schema(name = "pIBSSN", description = "Alíquota efetiva de IBS do Simples Nacional efetivamente aplicada, "
                + "em pontos percentuais (pós-transformação de compra governamental). Suprimida para CRT=2")
        BigDecimal pIBSSN,

        @JsonProperty("vIBSSN")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vIBSSN", description = "Valor devido de IBS do Simples Nacional. Suprimido para CRT=2")
        BigDecimal vIBSSN,

        @JsonProperty("vIBSPendSusp")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vIBSPendSusp", description = "Valor de IBS pendente/suspenso (exportação indireta ou "
                + "CRT=3 + tpRegimeIBSCBS=2). Suprimido para CRT=2")
        BigDecimal vIBSPendSusp,

        @JsonProperty("pCBSSN")
        @JsonSerialize(using = BigDecimalTDec0302_04Serializer.class)
        @Schema(name = "pCBSSN", description = "Alíquota efetiva de CBS do Simples Nacional efetivamente aplicada, "
                + "em pontos percentuais (pós-transformação de compra governamental)")
        BigDecimal pCBSSN,

        @JsonProperty("vCBSSN")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vCBSSN", description = "Valor devido de CBS do Simples Nacional")
        BigDecimal vCBSSN,

        @JsonProperty("vCBSPendSusp")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vCBSPendSusp", description = "Valor de CBS pendente/suspenso (exportação indireta ou "
                + "CRT=3 + tpRegimeIBSCBS=2)")
        BigDecimal vCBSPendSusp,

        @SuppressWarnings("java:S116") // Nomenclatura no padrão da saída de observabilidade do projeto.
        @JsonProperty("IS")
        @Schema(name = "IS", description = "Imposto Seletivo (repasse do valor recebido, sem recálculo)")
        ImpostoSeletivoSimplesDomain IS,

        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(description = "Total do item: vIBSSN + vCBSSN + IS.valor (somente os valores devidos; "
                + "os pendentes/suspensos ficam fora)")
        BigDecimal totalItem,

        @Schema(description = "Memória de cálculo da CBS e do IBS. Suprimida para item NAO_CALCULADO")
        MemoriaCalculoSimplesDomain memoriaCalculo) {
}
