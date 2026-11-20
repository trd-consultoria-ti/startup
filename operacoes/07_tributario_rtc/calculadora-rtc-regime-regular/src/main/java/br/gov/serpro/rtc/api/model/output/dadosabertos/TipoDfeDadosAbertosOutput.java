/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.api.model.output.dadosabertos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import br.gov.serpro.rtc.api.model.SerializationVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Saída de dados abertos que representa um tipo de documento fiscal eletrônico
 * (DFe) selecionável, com seu código (sigla normalizada), título, modelo fiscal
 * e ordem de exibição dentro do grupo, além das indicações de tratamento pela
 * calculadora ({@code suportado}) e de admissão de NCM zerado
 * ({@code permiteNcmZero}).
 */
@Getter
@Setter
@Builder
@JsonInclude(Include.NON_NULL)
public class TipoDfeDadosAbertosOutput implements SerializationVisibility {

    @Schema(name = "codigo", description = "Sigla normalizada do DFe, usada como siglaDfe nos demais endpoints", example = "NFE")
    private String codigo;

    @Schema(name = "titulo", description = "Nome do tipo de documento fiscal eletrônico", example = "Nota Fiscal Eletrônica")
    private String titulo;

    @Schema(name = "descricao", description = "Descrição curta auxiliar do tipo (pode ser omitida)", example = "Venda entre empresas, modelo 55.")
    private String descricao;

    @Schema(name = "modelo", description = "Número do modelo fiscal; nulo quando não há modelo único", example = "55")
    private String modelo;

    @Schema(name = "ordem", description = "Ordem de exibição do tipo dentro do grupo", example = "1")
    private Integer ordem;

    @Schema(name = "suportado", description = "Indica se a calculadora trata este tipo de documento nos endpoints de cálculo. "
        + "false = o cálculo é rejeitado com o erro REG-038 (tipo de documento fiscal não tratado).", example = "true")
    private Boolean suportado;

    @Schema(name = "permiteNcmZero", description = "Indica se o documento admite NCM zerado (00 ou 00000000) nos itens.", example = "true")
    private Boolean permiteNcmZero;

}
