package br.gov.serpro.rtc.api.simplesnacional.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Item de entrada do endpoint do Simples Nacional (NT 2026.005).
 *
 * <p>Recebe a base de cálculo, o valor do Imposto Seletivo já calculado (vIS),
 * o código de classificação tributária ({@code cClassTrib}, que resolve o
 * {@code tpRBSN} internamente), o código oficial de atividade ({@code cAtivSN})
 * e as alíquotas efetivas do Simples ({@code pIBSSN}/{@code pCBSSN}). Valores e
 * alíquotas são recebidos como texto para permitir validação tolerante e a
 * emissão dos códigos {@code VSN-} adequados.
 *
 * <p>O tipo de ente governamental adquirente ({@code tpEnteGov}) NÃO é campo de
 * item: no leiaute do DF-e o grupo {@code gCompraGov} pertence à identificação
 * da nota (grupo B/ide) — a tag é recebida em
 * {@link OperacaoSimplesInput#tpEnteGov()} e vale para todos os itens.
 *
 */
public record ItemSimplesInput(

        @Schema(name = "numero", description = "Número do item na operação", example = "1")
        Integer numero,

        @JsonProperty("cAtivSN")
        @Schema(name = "cAtivSN", description = "Código oficial de Atividade do Simples Nacional (2 caracteres, "
                + "com zero à esquerda; NT 2026.005), validado contra o catálogo de atividades vigente na data. "
                + "A restrição de documento é POR ATIVIDADE: atividade sem vínculo na associativa de tipos de DFE é "
                + "admissível em qualquer tpDoc suportado; com vínculos, somente nos tpDoc vinculados. Ausente, "
                + "desconhecido, fora de vigência ou inadmissível para o tpDoc: o item não é calculado (estado "
                + "NAO_CALCULADO), sem erro", example = "01", nullable = true)
        String cAtivSN,

        @JsonProperty("cClassTrib")
        @Schema(name = "cClassTrib", description = "Código de Classificação Tributária do IBS e da CBS (6 dígitos). "
                + "Obrigatório: resolve internamente o Tipo de Receita Bruta do Simples Nacional (tpRBSN, NT 2026.005)",
                example = "000001")
        String cClassTrib,

        @JsonProperty("nbs")
        @JsonAlias("NBS")
        @Schema(name = "nbs", description = "Código da Nomenclatura Brasileira de Serviços. A obrigatoriedade e a "
                + "validação dependem da classificação tributária e do tipo de documento; não pode ser informado "
                + "junto com NCM",
                example = "114051000", nullable = true)
        String NBS,

        @JsonProperty("ncm")
        @JsonAlias("NCM")
        @Schema(name = "ncm", description = "Código da Nomenclatura Comum do Mercosul. A obrigatoriedade e a "
                + "validação dependem da classificação tributária e do tipo de documento; não pode ser informado "
                + "junto com NBS",
                example = "22030000", nullable = true)
        String NCM,

        @Schema(name = "baseCalculo", description = "Base de cálculo do item: vProd + vFrete + vSeg + vOutro - vDesc, "
                + "SEM o valor do Imposto Seletivo (decimal como string). O nome é deliberado: o contrato separa "
                + "semanticamente a base sem vIS (baseCalculo) do valorImpostoSeletivo, e essa separação sobrevive "
                + "a qualquer mudança futura na composição legal da RBSNitem sem quebra de compatibilidade. "
                + "Formato 13v2: até 13 dígitos inteiros e até 2 casas decimais, sem notação científica",
                example = "950.00")
        String baseCalculo,

        @Schema(name = "valorImpostoSeletivo", description = "Valor do Imposto Seletivo já calculado (vIS). "
                + "\"0.00\" quando não há IS. NÃO compõe a receita bruta do item (vRBSNitem) — parametrização "
                + "por vigência, atualmente desligada em todas as vigências; o valor é apenas repassado na saída "
                + "e somado ao totalItem. Formato 13v2: até 13 dígitos inteiros e até 2 casas decimais",
                example = "50.00")
        String valorImpostoSeletivo,

        @JsonProperty("pIBSSN")
        @Schema(name = "pIBSSN", description = "Alíquota efetiva do Simples Nacional para o IBS, em pontos "
                + "percentuais (NT 2026.005). Exigida apenas quando o cálculo a usa (cAtivSN válido, regime que "
                + "calcula IBS e tpRBSN 1, 4 ou 5). Ignorada na saída para CRT=2. Até 3 dígitos inteiros e até 34 "
                + "casas decimais, sem notação científica: o cálculo usa todas as casas informadas, e a saída "
                + "apresenta a alíquota com 2 a 4 casas (tipo 3v2-4)", example = "1.25", nullable = true)
        String pIBSSN,

        @JsonProperty("pCBSSN")
        @Schema(name = "pCBSSN", description = "Alíquota efetiva do Simples Nacional para a CBS, em pontos "
                + "percentuais (NT 2026.005). Exigida apenas quando o cálculo a usa (cAtivSN válido e tpRBSN 1, 4 ou 5). "
                + "Até 3 dígitos inteiros e até 34 casas decimais, sem notação científica: o cálculo usa todas as "
                + "casas informadas, e a saída apresenta a alíquota com 2 a 4 casas (tipo 3v2-4)",
                example = "2.75", nullable = true)
        String pCBSSN) {
}
