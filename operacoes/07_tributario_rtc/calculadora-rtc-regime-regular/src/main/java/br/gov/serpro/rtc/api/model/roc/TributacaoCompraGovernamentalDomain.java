package br.gov.serpro.rtc.api.model.roc;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonSerialize;

import br.gov.serpro.rtc.api.model.SerializationVisibility;
import br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental;
import br.gov.serpro.rtc.config.serializer.BigDecimalTDec0302_04Serializer;
import br.gov.serpro.rtc.config.serializer.BigDecimalTDec1302Serializer;
import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Getter;
import lombok.Setter;

/**
 * Detalha a composição do IBS e da CBS em compras governamentais, incluindo
 * alíquotas, valores por ente e regras de cálculo dos valores efetivos.
 */
@Getter
@Setter
@Builder
@Jacksonized
@JsonInclude(NON_NULL)
@JsonPropertyOrder({ "pAliqIBSUF", "vTribIBSUF", "pAliqIBSMun", "vTribIBSMun", "pAliqCBS", "vTribCBS" })
public class TributacaoCompraGovernamentalDomain implements SerializationVisibility {

    @JsonSerialize(using = BigDecimalTDec0302_04Serializer.class)
    @Schema(description = "Alíquota do IBS de competência do Estado")
    @Builder.Default
    private BigDecimal pAliqIBSUF = ZERO;

    @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
    @Schema(description = "Valor do Tributo do IBS da UF calculado")
    @Builder.Default
    private BigDecimal vTribIBSUF = ZERO;

    @JsonSerialize(using = BigDecimalTDec0302_04Serializer.class)
    @Schema(description = "Alíquota do IBS de competência do Município")
    @Builder.Default
    private BigDecimal pAliqIBSMun = ZERO;

    @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
    @Schema(description = "Valor do Tributo do IBS do Município calculado")
    @Builder.Default
    private BigDecimal vTribIBSMun = ZERO;

    @JsonSerialize(using = BigDecimalTDec0302_04Serializer.class)
    @Schema(description = "Alíquota da CBS")
    @Builder.Default
    private BigDecimal pAliqCBS = ZERO;

    @JsonSerialize(using = BigDecimalTDec1302Serializer.class)
    @Schema(description = "Valor do Tributo da CBS calculado")
    @Builder.Default
    private BigDecimal vTribCBS = ZERO;
    
    public void zerarValores() {
		this.setPAliqIBSUF(ZERO);
		this.setVTribIBSUF(ZERO);
		this.setPAliqIBSMun(ZERO);
		this.setVTribIBSMun(ZERO);
		this.setPAliqCBS(ZERO);
		this.setVTribCBS(ZERO);
	}
    
    /**
     * Calcula a alíquota e o valor que cada ente federativo efetivamente recebe em compra
     * governamental (art. 473 da LC 214/2025).
     * <p>
     * Este objeto é o detalhe ({@code gTribCompraGov}): o que seria devido sem a redistribuição.
     * O resultado é o que vai ao grupo principal:
     * <ul>
     *   <li>as alíquotas redistribuídas são publicadas com 4 casas. Quando parte da alíquota da
     *       CBS passa ao IBS (Estado ou Município comprador), a alíquota de quem cede é
     *       arredondada a 4 casas e a de quem recebe é obtida por diferença, de modo que a soma
     *       das alíquotas publicadas seja igual à soma das alíquotas originais;</li>
     *   <li>o valor de cada tributo é {@code arred2(vBC × alíquota publicada ÷ 100)}. Ele não é a
     *       soma dos valores do detalhe: a diferença entre o detalhe e o grupo principal pode
     *       chegar a 0,04 e é aceita na validação do documento.</li>
     * </ul>
     *
     * @param tpEnteGov tipo de ente governamental que realizou a operacao
     * @param pTransferenciaCBS percentual de transferencia de CBS para outros entes
     * @param dataFatoGerador data da operacao
     * @param vBC base de cálculo publicada do item
     * @return alíquotas e valores do grupo principal; {@code null} sem ente governamental
     */
    @JsonIgnore
    public TributacaoCompraGovernamentalDomain getValoresEfetivos(TipoEnteGovernamental tpEnteGov,
            BigDecimal pTransferenciaCBS, LocalDate dataFatoGerador, BigDecimal vBC) {
        if (tpEnteGov == null) {
            return null;
        }

        Objects.requireNonNull(dataFatoGerador, "Data do fato gerador obrigatória");
        Objects.requireNonNull(pTransferenciaCBS, "Percentual de transferência da CBS obrigatória");

        if (dataFatoGerador.isBefore(LocalDate.of(2027, 1, 1))) {
            // antes de 2027, cada um fica com os seus proprios valores
            return this;
        }

        final var somaAliquotas = pAliqCBS.add(pAliqIBSUF).add(pAliqIBSMun);

        if (tpEnteGov.isUniao()) {
            // se uniao compra, fica com tudo - estados e municipios ficam com zero
            return comAliquotas(somaAliquotas, ZERO, ZERO, vBC);
        }

        // Estado ou Município compra: fica com o IBS do outro ente e (possivelmente) com parte da
        // CBS. Quem cede (CBS) tem a alíquota arredondada; quem recebe fecha pela diferença:
        //
        // pAliqCBS = arred4(pAliqCBS - (pAliqCBS * pTransferenciaCBS / 100))
        // pAliq de quem recebe = (pAliqCBS + pAliqIBSUF + pAliqIBSMun) - pAliqCBS já arredondada
        final var pAliqCBSFinal = ArredondamentoUtils.arredondarAliquotaPublicada(
                pAliqCBS.subtract(pAliqCBS.multiply(ArredondamentoUtils.percentualParaFracao(pTransferenciaCBS))));
        final var pAliqRecebedor = somaAliquotas.subtract(pAliqCBSFinal);

        if (tpEnteGov.isEstado()) {
            return comAliquotas(pAliqCBSFinal, pAliqRecebedor, ZERO, vBC);
        }
        if (tpEnteGov.isMunicipio()) {
            return comAliquotas(pAliqCBSFinal, ZERO, pAliqRecebedor, vBC);
        }
        return comAliquotas(pAliqCBSFinal, ZERO, ZERO, vBC);
    }

    /** Grupo principal: alíquotas publicadas e valor de cada tributo pela alíquota publicada. */
    private static TributacaoCompraGovernamentalDomain comAliquotas(BigDecimal pAliqCBS, BigDecimal pAliqIBSUF,
            BigDecimal pAliqIBSMun, BigDecimal vBC) {
        return TributacaoCompraGovernamentalDomain.builder()
                .pAliqCBS(pAliqCBS)
                .vTribCBS(valorPelaAliquotaPublicada(vBC, pAliqCBS))
                .pAliqIBSUF(pAliqIBSUF)
                .vTribIBSUF(valorPelaAliquotaPublicada(vBC, pAliqIBSUF))
                .pAliqIBSMun(pAliqIBSMun)
                .vTribIBSMun(valorPelaAliquotaPublicada(vBC, pAliqIBSMun))
                .build();
    }

    private static BigDecimal valorPelaAliquotaPublicada(BigDecimal vBC, BigDecimal aliquota) {
        return ArredondamentoUtils.arredondarValorFiscal(
                ArredondamentoUtils.multiplicar(vBC, ArredondamentoUtils.percentualParaFracao(aliquota)));
    }
}
