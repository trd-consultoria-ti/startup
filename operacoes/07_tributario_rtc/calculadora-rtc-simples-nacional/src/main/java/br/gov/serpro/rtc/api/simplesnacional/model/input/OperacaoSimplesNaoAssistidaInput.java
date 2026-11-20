package br.gov.serpro.rtc.api.simplesnacional.model.input;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entrada do cálculo NÃO ASSISTIDO do Simples Nacional
 * ({@code POST calculadora/observabilidade/simples-nacional/nao-assistido}):
 * o mesmo contrato para todos os documentos fiscais, com o regime no
 * vocabulário do próprio documento e a linha de alíquotas do contribuinte em
 * {@code aliquotasEfetivas}.
 *
 * <p>"Proibido" = informado ⇒ erro global VSN-033. Campos recebidos como texto
 * para validação tolerante; campos desconhecidos são ignorados.
 */
public record OperacaoSimplesNaoAssistidaInput(

        @Schema(name = "id", description = "Identificador da operação", example = "op-1")
        String id,

        @Schema(name = "versao", description = "Versão do payload", example = "3.0.0")
        String versao,

        @Schema(name = "data", description = "Data do fato gerador (YYYY-MM-DD). Rege todas as vigências",
                example = "2027-03-15")
        String data,

        @Schema(name = "tpDoc", description = "Tipo de documento fiscal: 55, 65, 57, 67, 63, 93 ou 62 (documentos com "
                + "CRT) ou 91 (NFS-e). Habilitação, vocabulário do regime, precisão da alíquota, máximo de itens e "
                + "Imposto Seletivo parametrizados em banco por documento", example = "91")
        String tpDoc,

        @SuppressWarnings("java:S116") // Nomenclatura oficial VERBATIM do leiaute DF-e/NT 2026.005.
        @JsonProperty("CRT")
        @Schema(name = "CRT", description = "Documentos com CRT: obrigatório, 1 (Simples), 2 (excesso de sublimite), "
                + "3 (regime normal) ou 4 (MEI). Proibido na NFS-e", example = "1", nullable = true)
        String CRT,

        @Schema(name = "tpRegimeIBSCBS", description = "Documentos com CRT: opcional, 1 (optou pelo regime regular do "
                + "IBS e da CBS: SOMENTE_RECEITA com CRT 1 ou 2) ou 2 (opção pelo Simples pendente: PENDENTE com "
                + "CRT 3). Proibido na NFS-e", example = "2", nullable = true)
        String tpRegimeIBSCBS,

        @Schema(name = "opSimpNac", description = "NFS-e: obrigatório, 1 (não optante), 2 (MEI), 3 (ME/EPP) ou 4 "
                + "(opção pendente). Proibido nos documentos com CRT", example = "3", nullable = true)
        String opSimpNac,

        @JsonProperty("regApIBSCBSSN")
        @Schema(name = "regApIBSCBSSN", description = "NFS-e: 1 (IBS e CBS pelo Simples), 2 (CBS pelo Simples e IBS "
                + "pelo regime regular) ou 3 (os dois pelo regime regular). Obrigatório com opSimpNac 3 ou 4; "
                + "proibido com 1 ou 2 (VSN-031)", example = "1", nullable = true)
        String regApIBSCBSSN,

        @JsonProperty("regApTribSN")
        @Schema(name = "regApTribSN", description = "NFS-e: 1 (tributos federais e ISS pelo Simples), 2 (federais pelo "
                + "Simples e ISS fora dele) ou 3 (federais e ISS fora). Obrigatório com opSimpNac 3 ou 4; proibido "
                + "com 1 ou 2 (VSN-032). Não muda o regime do IBS e da CBS: só entra na escolha do par de alíquotas",
                example = "1", nullable = true)
        String regApTribSN,

        @Schema(name = "tpEnteGov", description = "Tipo de ente governamental adquirente (compra governamental): 1 "
                + "(União), 2 (Estado), 3 (Distrito Federal) ou 4 (Município). Vale para a operação inteira",
                example = "2", nullable = true)
        String tpEnteGov,

        @Schema(name = "aliquotasEfetivas", description = "Linha de alíquotas efetivas do contribuinte no período "
                + "(pontos percentuais, texto). Obrigatória, exceto no regime SOMENTE_RECEITA (VSN-039)",
                nullable = true)
        AliquotasEfetivasInput aliquotasEfetivas,

        @Schema(name = "itens", description = "Itens da operação: 1 ou mais; na NFS-e, exatamente 1 (VSN-034)")
        List<ItemSimplesUnificadoInput> itens) {
}
