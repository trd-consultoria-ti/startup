/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.api.versao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Contrato com as versões publicadas da aplicação, do banco de dados e do
 * ambiente.
 *
 * <p>É usado em duas direções: como resposta do endpoint {@code /versao} desta
 * instalação (montada a partir do banco e das propriedades da aplicação) e como
 * alvo de desserialização da versão REMOTA consultada no {@code /versao} da
 * instalação online ({@code versao.url}) — por isso os campos são todos
 * anuláveis e a inclusão é {@code NON_NULL}.
 *
 * <p>Mantém {@code @Builder} (6 campos, construção parcial possível quando a
 * origem não publica todos os campos).
 */
@Builder
@JsonInclude(Include.NON_NULL)
public record VersaoDomain(

        @Schema(name = "versaoApp", description = "Versão do aplicativo", example = "0.0.0-SNAPSHOT")
        String versaoApp,

        @Schema(name = "versaoDb", description = "Versão do banco de dados", example = "1.0.0")
        String versaoDb,

        @Schema(name = "descricaoVersaoDb", description = "Descrição da versão do banco de dados", example = "Versão de homologação")
        String descricaoVersaoDb,

        @Schema(name = "dataVersaoDb", description = "Data da versão do banco de dados", example = "2026-01-01")
        String dataVersaoDb,

        @Schema(name = "arquivoDb", description = "Arquivo do banco de dados em uso: calculadora-pro.db (vigências "
                + "corretas) ou calculadora-nonpro.db (vigências retroativas)", example = "calculadora-pro.db")
        String arquivoDb,

        @Schema(name = "ambiente", description = "Ambiente da aplicação", example = "Online")
        String ambiente) {
}
