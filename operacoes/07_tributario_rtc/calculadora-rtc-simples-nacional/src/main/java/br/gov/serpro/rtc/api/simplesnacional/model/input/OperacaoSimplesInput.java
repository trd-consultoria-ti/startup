package br.gov.serpro.rtc.api.simplesnacional.model.input;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entrada principal do endpoint de cálculo do Simples Nacional com observabilidade
 * (NT 2026.005 — Reforma Tributária do Consumo, Simples Nacional).
 *
 * <p>Contém os dados globais da operação ({@code data}, {@code tpDoc}, {@code CRT},
 * {@code tpRegimeIBSCBS}, {@code tpEnteGov}) e a lista de itens. Os campos são
 * recebidos como texto para permitir validação tolerante e a emissão dos códigos
 * {@code VSN-} adequados.
 *
 * <p>O {@code tpEnteGov} é global porque espelha o leiaute do DF-e: o grupo
 * {@code gCompraGov} pertence à identificação da nota (grupo B/ide, NT
 * 2025.002-RTC) — o ente público adquirente é o destinatário do documento e não
 * pode variar por item. O deslocamento de alíquotas que ele dispara (NT
 * 2026.005, seção 5.3.3) continua aplicado item a item, sobre as alíquotas de
 * cada item.
 *
 */
public record OperacaoSimplesInput(

        @Schema(name = "id", description = "Identificador da operação", example = "507f1f77bcf86cd799439011")
        String id,

        @Schema(name = "versao", description = "Versão do endpoint", example = "2.0.0")
        String versao,

        @Schema(name = "data", description = "Data do fato gerador da operação (YYYY-MM-DD). "
                + "Todos os lookups de vigência usam esta data", example = "2027-03-15")
        String data,

        @Schema(name = "tpDoc", description = "Tipo de documento fiscal eletrônico da operação (NT 2026.005). "
                + "Os tipos suportados pelo cálculo são parametrizados em banco, pela vigência da data do fato "
                + "gerador (na carga atual: 55 - NF-e e 65 - NFC-e)", example = "55")
        String tpDoc,

        @SuppressWarnings("java:S116") // Nomenclatura oficial VERBATIM do leiaute DF-e/NT 2026.005.
        @JsonProperty("CRT")
        @Schema(name = "CRT", description = "Código de Regime Tributário do emitente: 1 (Simples Nacional), "
                + "2 (Simples Nacional - excesso de sublimite de receita bruta), 3 (Regime Normal) ou 4 (MEI). "
                + "CRT=4 e CRT=3 sem tpRegimeIBSCBS=2 estão fora do escopo do cálculo (NT 2026.005)", example = "1")
        String CRT,

        @Schema(name = "tpRegimeIBSCBS", description = "Tipo de regime de apuração do IBS/CBS (NT 2026.005): "
                + "1 (regime regular - fora do escopo deste endpoint) ou 2 (apuração por fora, admitida apenas com CRT=3). "
                + "Regras de coerência C21a-10/20/30 da NT", example = "2", nullable = true)
        String tpRegimeIBSCBS,

        @Schema(name = "tpEnteGov", description = "Tipo de ente governamental adquirente (compra governamental, "
                + "NT 2026.005, seção 5.3.3; tag gCompraGov/tpEnteGov da identificação da nota — grupo B/ide): "
                + "1 (União), 2 (Estado), 3 (Distrito Federal) ou 4 (Município). Vale para a operação inteira; "
                + "quando informado, aplica a todos os itens o deslocamento de alíquotas CBS-IBS dos arts. 372 "
                + "e 473 da LC 214/2025", example = "2", nullable = true)
        String tpEnteGov,

        @Schema(name = "itens", description = "Itens da operação")
        List<ItemSimplesInput> itens) {
}
