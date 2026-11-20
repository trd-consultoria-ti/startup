package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonSerialize;

import br.gov.serpro.rtc.shared.serializer.BigDecimalTDec1302Serializer;
import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 *
 * Total consolidado do Simples Nacional, espelhando o grupo de totais do
 * Evento de Tributação do Simples Nacional (NT 2026.005).
 *
 * <p>Regras de totalização: itens {@code CALCULADO} contribuem para todos os
 * totais; itens {@code NAO_CALCULADO} contribuem APENAS para {@code vRBSNtot}
 * (os demais campos ficam ausentes no {@code tribCalc} deles e são ignorados);
 * itens com erro não contribuem para nada.
 *
 * Todas as somas são feitas via {@link ArredondamentoUtils}, sobre os VALORES
 * FISCAIS dos itens (já arredondados a 2 casas, uma única vez, no cálculo do
 * item): a soma é exata e o total sempre fecha com a soma dos itens exibidos
 * — os totais da NT (P35 a P39) são o somatório dos valores atribuídos aos
 * itens.
 *
 * <p>Record com {@code @Builder} (8 componentes, construção condicional na
 * fábrica {@link #create(List)}).
 *
 */
@Builder
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "vRBSNtot", "vBC", "vIBSSNtot", "vIBSSNtotPendSusp", "vCBSSNtot", "vCBSSNtotPendSusp", "totalIs",
        "totalTributos" })
public record TotalSimplesDomain(

        @JsonProperty("vRBSNtot")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vRBSNtot", description = "Soma de vRBSNitem dos itens CALCULADO e NAO_CALCULADO (NT 2026.005)")
        BigDecimal vRBSNtot,

        @JsonProperty("vBC")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vBC", description = "Soma de vBC (base de cálculo de IBS/CBS) dos itens CALCULADO. "
                + "Itens NAO_CALCULADO não têm vBC e ficam fora desta soma; suprimido quando nenhum item o tem")
        BigDecimal vBC,

        @JsonProperty("vIBSSNtot")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vIBSSNtot", description = "Soma de vIBSSN dos itens calculados. Suprimido quando nenhum item gera IBS (CRT=2)")
        BigDecimal vIBSSNtot,

        @JsonProperty("vIBSSNtotPendSusp")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vIBSSNtotPendSusp", description = "Soma de vIBSPendSusp dos itens calculados. Suprimido quando nenhum item gera IBS (CRT=2)")
        BigDecimal vIBSSNtotPendSusp,

        @JsonProperty("vCBSSNtot")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vCBSSNtot", description = "Soma de vCBSSN dos itens calculados")
        BigDecimal vCBSSNtot,

        @JsonProperty("vCBSSNtotPendSusp")
        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(name = "vCBSSNtotPendSusp", description = "Soma de vCBSPendSusp dos itens calculados")
        BigDecimal vCBSSNtotPendSusp,

        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(description = "Soma de IS.valor dos itens calculados (conveniência do projeto: o IS não integra o evento da NT)")
        BigDecimal totalIs,

        @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
        @Schema(description = "vIBSSNtot + vCBSSNtot + totalIs (somente os valores devidos)")
        BigDecimal totalTributos) {

    /**
     * Total da operação: consolida os itens que têm {@code tribCalc}
     * ({@code CALCULADO} e {@code NAO_CALCULADO}); itens com erro ou
     * {@code NAO_IMPLEMENTADO} não contribuem. {@code null} quando nenhum item
     * tem {@code tribCalc}.
     */
    public static TotalSimplesDomain totalizar(List<ObjetoSimplesDomain> objetos) {
        List<ObjetoSimplesDomain> comTribCalc = objetos.stream()
                .filter(objeto -> objeto.tribCalc() != null)
                .toList();
        return comTribCalc.isEmpty() ? null : create(comTribCalc);
    }

    /**
     *
     * Consolida os totais a partir dos itens com {@code tribCalc}
     * ({@code CALCULADO} e {@code NAO_CALCULADO}).
     *
     * <p>Cada campo soma apenas os valores presentes nos itens; campo sem
     * nenhum valor presente fica nulo (suprimido na serialização) — é assim que
     * os itens {@code NAO_CALCULADO} contribuem apenas para {@code vRBSNtot} e
     * que os totais de IBS somem para CRT=2.
     *
     * Toda totalização passa por {@link ArredondamentoUtils#totalizar(Stream)}.
     *
     * @param objetos itens com estado {@code CALCULADO} ou {@code NAO_CALCULADO} (com {@code tribCalc})
     * @return total consolidado
     *
     */
    public static TotalSimplesDomain create(List<ObjetoSimplesDomain> objetos) {
        BigDecimal vIBSSNtot = somarPresentes(objetos, TributosSimplesDomain::vIBSSN);
        BigDecimal vCBSSNtot = somarPresentes(objetos, TributosSimplesDomain::vCBSSN);
        BigDecimal totalIs = somarPresentes(objetos,
                trib -> trib.IS() != null ? trib.IS().valor() : null);

        BigDecimal totalTributos = null;
        if (vIBSSNtot != null || vCBSSNtot != null || totalIs != null) {
            totalTributos = ArredondamentoUtils.totalizar(
                    Stream.of(vIBSSNtot, vCBSSNtot, totalIs).filter(Objects::nonNull));
        }

        return TotalSimplesDomain.builder()
                .vRBSNtot(somarPresentes(objetos, TributosSimplesDomain::vRBSNitem))
                .vBC(somarPresentes(objetos, TributosSimplesDomain::vBC))
                .vIBSSNtot(vIBSSNtot)
                .vIBSSNtotPendSusp(somarPresentes(objetos, TributosSimplesDomain::vIBSPendSusp))
                .vCBSSNtot(vCBSSNtot)
                .vCBSSNtotPendSusp(somarPresentes(objetos, TributosSimplesDomain::vCBSPendSusp))
                .totalIs(totalIs)
                .totalTributos(totalTributos)
                .build();
    }

    /**
     * Soma os valores não nulos do campo extraído; retorna {@code null} quando
     * nenhum item apresenta o campo (para a supressão de tags da NT).
     */
    private static BigDecimal somarPresentes(List<ObjetoSimplesDomain> objetos,
            Function<TributosSimplesDomain, BigDecimal> campo) {
        List<BigDecimal> valores = objetos.stream()
                .map(o -> campo.apply(o.tribCalc()))
                .filter(Objects::nonNull)
                .toList();
        return valores.isEmpty() ? null : ArredondamentoUtils.totalizar(valores.stream());
    }

}
