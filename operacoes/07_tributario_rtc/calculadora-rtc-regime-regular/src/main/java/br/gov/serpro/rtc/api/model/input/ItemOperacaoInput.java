/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.api.model.input;

import java.math.BigDecimal;

import br.gov.serpro.rtc.api.model.SerializationVisibility;
import br.gov.serpro.rtc.api.model.input.basecalculo.ComponentesBaseCalculoInput;
import br.gov.serpro.rtc.domain.model.entity.TipoDfe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entrada de um item da operação.
 *
 * Concentra a classificação tributária, a base de cálculo e os grupos opcionais
 * de Imposto Seletivo e tributação regular do item.
 */
@ToString
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public final class ItemOperacaoInput implements SerializationVisibility {

    @NotNull
    @Min(1)
    @Max(9999999)
    @EqualsAndHashCode.Include
    @Schema(name = "numero", description = "Número do Item", example = "1")
    private Integer numero;

    @Schema(name = "ncm", description = "Código NCM", example = "24021000")
    private String ncm;

    @Schema(name = "nbs", description = "Código NBS", example = "109052100")
    private String nbs;
    
    @NotNull
    @Pattern(regexp = "\\d+", message = "Informar somente dígitos")
    @Size(min = 3, max = 3)
    @Schema(name = "cst", description = "Código de situação tributária", example = "000")
    private String cst;

    @NotNull
    @Pattern(regexp = "\\d+", message = "Informar somente dígitos")
    @Size(min = 6, max = 6)
    @Schema(name = "cClassTrib", description = "Código de classificação tributária", example = "000001")
    private String cClassTrib;

    // Obrigatoriedade condicional (validada em service):
    // - sem impostoSeletivo e sem gComponentesBC → obrigatória
    // - com impostoSeletivo, sem gComponentesBC → opcional (derivada de BC(IS) + IS)
    // - com gComponentesBC → opcional (calculada automaticamente ou validada cruzada)
    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    @Schema(name = "baseCalculo", description = "Base de cálculo de CBS e IBS. Obrigatória quando impostoSeletivo e gComponentesBC não são informados; opcional com impostoSeletivo (derivada) ou com gComponentesBC (calculada automaticamente)", example = "200.00")
    private BigDecimal baseCalculo;

    @Digits(integer = 11, fraction = 4)
    @Schema(name = "quantidade", description = "Quantidade", example = "1")
    private BigDecimal quantidade;
    
    @Schema(name = "unidade", description = "Unidade de medida", example = "LT")
    private String unidade;

    @Valid // para validar os campos dentro do objeto impostoSeletivo
    @Schema(name = "impostoSeletivo", description = "Informações do Imposto Seletivo")
    private ImpostoSeletivoInput impostoSeletivo;

    @Valid // para validar os campos dentro do objeto desoneração
    @Schema(name = "tributacaoRegular", description = "Informações sobre tributação regular")
    private TributacaoRegularInput tributacaoRegular;

    @Valid
    @Schema(name = "gComponentesBC", description = "Grupo de componentes para cálculo/validação da Base de Cálculo. Quando informado, permite o cálculo automático da BC ou a validação cruzada com a BC declarada")
    private ComponentesBaseCalculoInput gComponentesBC;

	@Valid
	@Schema(name = "aliquotasNominais", description = "Alíquotas nominais aplicáveis à operação. Obrigatório para operações a partir de 01/01/2027 e proibido antes dessa data")
	private AliquotasNominaisInput aliquotasNominais;
	
	/**
	 * Ajusta o NCM do item de operação para {@code null} quando o tipo de documento
	 * fiscal permite NCM zerado e o NCM informado representa zero.
	 *
	 * @param tipoDfe o tipo de documento fiscal ({@code tpDoc}) da operação
	 */
	public void ajustarNcm(TipoDfe tipoDfe) {
	    if (!deveAnularNcm(tipoDfe)) {
	        return;
	    }

	    this.ncm = null;
	}

	/**
	 * Verifica se o NCM desta instância deve ser limpo.
	 *
	 * @param tipoDfe o tipo de documento fiscal
	 * @return true se o NCM deve ser limpo, false caso contrário
	 */
	private boolean deveAnularNcm(TipoDfe tipoDfe) {
	    return tipoDfe != null
	            && tipoDfe.isPermiteNcmZero()
	            && possuiNcmZero();
	}

	/**
	 * Verifica se o NCM desta instância representa um valor zerado.
	 *
	 * @return true se o NCM é "00" ou "00000000", false caso contrário
	 */
	private boolean possuiNcmZero() {
	    return "00".equals(this.ncm) || "00000000".equals(this.ncm);
	}

}