package br.gov.serpro.rtc.api.simplesnacional.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Item de entrada dos endpoints NÃO ASSISTIDO e ASSISTIDO do Simples Nacional,
 * o mesmo para todos os documentos fiscais. Sem alíquotas: elas vêm da linha
 * de alíquotas do contribuinte (no corpo, em {@code aliquotasEfetivas}, ou da
 * Tabela de Alíquotas Efetivas) e o par é escolhido pela Calculadora, item a
 * item.
 *
 * <p>Os campos do ISS ({@code tribISSQN}, {@code tpRetISSQN} e {@code tpBM})
 * são fatos do serviço e valem só na NFS-e; nos documentos com CRT são
 * desconsiderados, com aviso. Valores recebidos como texto para validação
 * tolerante. Campos desconhecidos são ignorados (configuração do Jackson).
 *
 */
public record ItemSimplesUnificadoInput(

        @Schema(name = "numero", description = "Número do item na operação (obrigatório, VSN-010)", example = "1")
        Integer numero,

        @JsonProperty("cAtivSN")
        @Schema(name = "cAtivSN", description = "Código oficial de Atividade do Simples Nacional (na DPS da NFS-e o "
                + "campo se chama cAtvSN). Um único dígito é completado com zero à esquerda (\"7\" ⇒ \"07\"). "
                + "Ausente, desconhecido, fora de vigência ou não admitido no tpDoc: o item não é calculado "
                + "(estado NAO_CALCULADO), sem erro. Não exigido no regime SOMENTE_RECEITA",
                example = "12", nullable = true)
        String cAtivSN,

        @JsonProperty("cClassTrib")
        @Schema(name = "cClassTrib", description = "Código de Classificação Tributária do IBS e da CBS (6 dígitos). "
                + "Obrigatório: resolve internamente o Tipo de Receita Bruta do Simples Nacional (tpRBSN)",
                example = "000001")
        String cClassTrib,

        @JsonProperty("nbs")
        @JsonAlias("NBS")
        @Schema(name = "nbs", description = "Código da Nomenclatura Brasileira de Serviços. A obrigatoriedade e a "
                + "validação dependem da classificação tributária e do tipo de documento; não pode ser informado "
                + "junto com NCM", example = "114051000", nullable = true)
        String NBS,

        @JsonProperty("ncm")
        @JsonAlias("NCM")
        @Schema(name = "ncm", description = "Código da Nomenclatura Comum do Mercosul. A obrigatoriedade e a "
                + "validação dependem da classificação tributária e do tipo de documento; não pode ser informado "
                + "junto com NBS", example = "22030000", nullable = true)
        String NCM,

        @Schema(name = "baseCalculo", description = "Base de cálculo do item, formato 13v2. Nos documentos com CRT: "
                + "vProd + vFrete + vSeg + vOutro - vDesc, sem o vIS. Na NFS-e: o vReceitaBrutaSN (vServ - "
                + "descIncond - vCalcAjusteBCIBSCBS - vCalcAjusteBCISSQN no profissional parceiro), calculado por "
                + "quem chama", example = "10000.00")
        String baseCalculo,

        @Schema(name = "valorImpostoSeletivo", description = "Valor do Imposto Seletivo já calculado (vIS), formato "
                + "13v2. Documentos com CRT: obrigatório (\"0.00\" quando não há IS), repassado na saída. NFS-e: "
                + "ausente ou zero (a NFS-e não tem IS; outro valor ⇒ VSN-035)", example = "0.00", nullable = true)
        String valorImpostoSeletivo,

        @JsonProperty("tribISSQN")
        @Schema(name = "tribISSQN", description = "NFS-e: tributação do ISSQN — 1 operação tributável; 2 imunidade; "
                + "3 exportação de serviço; 4 não incidência. Obrigatório nas atividades com ISS no Simples (07, 09, "
                + "10, 12, 13 e 14) quando o regime calcula tributos (VSN-036). Valores 2, 3 e 4 levam ao par sem "
                + "redistribuição do ISS", example = "1", nullable = true)
        String tribISSQN,

        @JsonProperty("tpRetISSQN")
        @Schema(name = "tpRetISSQN", description = "NFS-e: retenção do ISSQN — 1 não retido; 2 retido pelo tomador; "
                + "3 retido pelo intermediário. Obrigatório com tribISSQN = 1 nas mesmas atividades (VSN-037); com "
                + "tribISSQN 2, 3 ou 4 é desconsiderado, com aviso (E0580). Retenção (2 ou 3) leva ao par sem "
                + "redistribuição (Cosit 298/2019)", example = "1", nullable = true)
        String tpRetISSQN,

        @JsonProperty("tpBM")
        @Schema(name = "tpBM", description = "NFS-e: tipo de benefício municipal do ISS — 1 isenção; 2 redução da base "
                + "em %; 3 redução da base em R$; 4 alíquota diferenciada. Opcional: ausente = sem benefício "
                + "municipal. Fora do domínio ⇒ VSN-038. Só vale nas atividades 12 e 13 (E0536); nas demais é "
                + "desconsiderado, com aviso. Isenção (1) leva ao par sem redistribuição", example = "1",
                nullable = true)
        String tpBM) {
}
