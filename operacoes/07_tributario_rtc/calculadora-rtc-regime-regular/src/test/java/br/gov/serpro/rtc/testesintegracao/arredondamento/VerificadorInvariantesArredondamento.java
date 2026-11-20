/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.arredondamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;

/**
 * Confere as invariantes de arredondamento lendo apenas a resposta serializada do
 * {@code POST /calculadora/regime-geral}. Não consulta o pedido, o banco nem os objetos de
 * resposta: o que vale é o texto que o integrador recebe.
 * <p>
 * Invariantes:
 * <ul>
 *   <li><b>I1</b>: {@code vIBS = vIBSUF + vIBSMun}, no item e no total;</li>
 *   <li><b>I2</b>: valor de cada tributo {@code = arred2(vBC × alíquota publicada ÷ 100) − vDif −
 *       vDevTrib}, exato; com redução (ou compra governamental), a alíquota publicada é a
 *       {@code pAliqEfet}; o diferimento é {@code arred2(vBC × alíquota publicada ÷ 100 × pDif ÷
 *       100)}; o mesmo para a tributação regular, o detalhe da compra governamental (líquido do
 *       diferimento, com a alíquota do próprio tributo) e o Imposto Seletivo;</li>
 *   <li><b>I3</b>: {@code pAliqEfet = arred4(alíquota × (1 − pRedAliq ÷ 100) × (1 − pRedutor ÷
 *       100))}; na compra governamental a conta é conferida no detalhe ({@code gTribCompraGov}),
 *       porque a {@code pAliqEfet} do grupo principal é a alíquota já redistribuída;</li>
 *   <li><b>I4</b>: cada total {@code =} soma do campo correspondente nos itens;</li>
 *   <li><b>I5</b>: na compra governamental, a soma das alíquotas redistribuídas do grupo principal
 *       é igual à soma das alíquotas do detalhe;</li>
 *   <li><b>I6</b>: na compra governamental, a soma dos valores do {@code gTribCompraGov} fica a até
 *       0,04 de {@code vIBSUF + vIBSMun + vCBS};</li>
 *   <li><b>I7</b>: todo valor monetário com exatamente 2 casas, toda alíquota com 2 a 4 e nenhum
 *       número com expoente.</li>
 * </ul>
 * O arredondamento do oráculo é o meio para o par (HALF_EVEN), escrito aqui de forma independente
 * do código de produção.
 */
public final class VerificadorInvariantesArredondamento {

    /** Uma violação encontrada numa resposta. */
    public record Violacao(String invariante, String caminho, String detalhe) {
        @Override
        public String toString() {
            return invariante + " " + caminho + ": " + detalhe;
        }
    }

    private static final Pattern MONETARIO = Pattern.compile("\\d{1,13}\\.\\d{2}");
    private static final Pattern ALIQUOTA = Pattern.compile("\\d{1,3}\\.\\d{2,4}");
    private static final Pattern QUANTIDADE = Pattern.compile("\\d{1,11}(\\.\\d{4})?");
    private static final Pattern EXPOENTE = Pattern.compile("[0-9][eE][-+]?[0-9]");
    private static final BigDecimal TOLERANCIA_COMPRA_GOV = new BigDecimal("0.04");

    /** Tributos do grupo gIBSCBS: grupo, alíquota, valor, alíquota e valor no detalhe e na tributação regular. */
    private static final String[][] TRIBUTOS = {
            { "gIBSUF", "pIBSUF", "vIBSUF", "pAliqIBSUF", "vTribIBSUF", "pAliqEfetRegIBSUF", "vTribRegIBSUF" },
            { "gIBSMun", "pIBSMun", "vIBSMun", "pAliqIBSMun", "vTribIBSMun", "pAliqEfetRegIBSMun", "vTribRegIBSMun" },
            { "gCBS", "pCBS", "vCBS", "pAliqCBS", "vTribCBS", "pAliqEfetRegCBS", "vTribRegCBS" } };

    private static final Map<String, String> CAMPOS_MONOFASIA_TOTAL = Map.of(
            "vIBSMono", "gMonoPadrao",
            "vCBSMono", "gMonoPadrao",
            "vIBSMonoReten", "gMonoReten",
            "vCBSMonoReten", "gMonoReten",
            "vIBSMonoRet", "gMonoRet",
            "vCBSMonoRet", "gMonoRet");

    private VerificadorInvariantesArredondamento() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Confere todas as invariantes numa resposta e devolve as violações (vazio = nenhuma). */
    public static List<Violacao> verificar(JsonNode resposta) {
        final List<Violacao> v = new ArrayList<>();
        final BigDecimal pRedutor = decimal(resposta.path("oper").path("gCompraGov").path("pRedutor"));

        final Somas somas = new Somas();
        final JsonNode objetos = resposta.path("objetos");
        for (int i = 0; i < objetos.size(); i++) {
            final String caminho = "$.objetos[" + i + "].tribCalc";
            verificarItem(objetos.get(i).path("tribCalc"), caminho, pRedutor, somas, v);
        }
        verificarTotais(resposta.path("total").path("tribCalc"), somas, v);
        verificarFormatos(resposta, "$", v);
        return v;
    }

    // ─── Item ──────────────────────────────────────────────────────────────────

    private static void verificarItem(JsonNode tribCalc, String caminho, BigDecimal pRedutor, Somas somas,
            List<Violacao> v) {
        final JsonNode is = tribCalc.path("IS");
        if (is.isObject()) {
            verificarImpostoSeletivo(is, caminho + ".IS", somas, v);
        }
        final JsonNode mono = tribCalc.path("IBSCBS").path("gIBSCBSMono");
        if (mono.isObject()) {
            CAMPOS_MONOFASIA_TOTAL.forEach((campo, grupo) -> somas.somar(campo, decimal(mono.path(grupo).path(campo))));
        }
        final JsonNode g = tribCalc.path("IBSCBS").path("gIBSCBS");
        if (!g.isObject()) {
            return;
        }
        final String cg = caminho + ".IBSCBS.gIBSCBS";
        final BigDecimal vBC = decimal(g.path("vBC"));
        somas.somar("vBCIBSCBS", vBC);

        final JsonNode detalhe = g.path("gTribCompraGov");
        final JsonNode regular = g.path("gTribRegular");
        final boolean compraGov = detalhe.isObject();

        BigDecimal somaValores = BigDecimal.ZERO;
        BigDecimal somaAliquotasPrincipal = BigDecimal.ZERO;
        BigDecimal somaAliquotasDetalhe = BigDecimal.ZERO;
        BigDecimal somaValoresDetalhe = BigDecimal.ZERO;

        for (String[] t : TRIBUTOS) {
            final JsonNode grupo = g.path(t[0]);
            if (!grupo.isObject()) {
                continue;
            }
            final String ct = cg + "." + t[0];
            final BigDecimal aliquota = decimal(grupo.path(t[1]));
            final BigDecimal valor = decimal(grupo.path(t[2]));
            final JsonNode gRed = grupo.path("gRed");
            final BigDecimal pRedAliq = decimal(gRed.path("pRedAliq"));
            final BigDecimal vDevTrib = decimal(grupo.path("gDevTrib").path("vDevTrib"));
            final JsonNode gDif = grupo.path("gDif");

            // alíquota publicada que multiplica a base
            final BigDecimal aliquotaPublicada = gRed.has("pAliqEfet") ? decimal(gRed.path("pAliqEfet")) : aliquota;

            // I3
            if (compraGov && !regular.isObject()) {
                final BigDecimal esperada = arred4(aliquota
                        .multiply(BigDecimal.ONE.subtract(pRedAliq.movePointLeft(2)))
                        .multiply(BigDecimal.ONE.subtract(pRedutor.movePointLeft(2))));
                final BigDecimal publicada = decimal(detalhe.path(t[3]));
                if (publicada.compareTo(esperada) != 0) {
                    v.add(new Violacao("I3", cg + ".gTribCompraGov." + t[3],
                            "publicada " + publicada + ", esperada " + esperada));
                }
            } else if (!compraGov && gRed.has("pAliqEfet")) {
                final BigDecimal esperada = arred4(aliquota.multiply(BigDecimal.ONE.subtract(pRedAliq.movePointLeft(2))));
                if (aliquotaPublicada.compareTo(esperada) != 0) {
                    v.add(new Violacao("I3", ct + ".gRed.pAliqEfet",
                            "publicada " + aliquotaPublicada + ", esperada " + esperada));
                }
            }

            // I2: diferimento e valor devido
            final BigDecimal bruto = arred2(vBC.multiply(aliquotaPublicada).movePointLeft(2));
            BigDecimal vDif = BigDecimal.ZERO;
            if (gDif.isObject()) {
                vDif = decimal(gDif.path("vDif"));
                final BigDecimal vDifEsperado = arred2(vBC.multiply(aliquotaPublicada).movePointLeft(2)
                        .multiply(decimal(gDif.path("pDif"))).movePointLeft(2));
                if (vDif.compareTo(vDifEsperado) != 0) {
                    v.add(new Violacao("I2", ct + ".gDif.vDif", "publicado " + vDif + ", esperado " + vDifEsperado
                            + " (vBC " + vBC + ", alíquota " + aliquotaPublicada + ")"));
                }
            }
            final BigDecimal esperado = bruto.subtract(vDif).subtract(vDevTrib);
            if (valor.compareTo(esperado) != 0) {
                v.add(new Violacao("I2", ct + "." + t[2], "publicado " + valor + ", esperado " + esperado
                        + " (vBC " + vBC + ", alíquota " + aliquotaPublicada + ")"));
            }

            // I2: tributação regular
            if (regular.isObject() && regular.has(t[6])) {
                final BigDecimal aliquotaRegular = decimal(regular.path(t[5]));
                final BigDecimal valorRegular = decimal(regular.path(t[6]));
                final BigDecimal esperadoRegular = arred2(vBC.multiply(aliquotaRegular).movePointLeft(2));
                if (valorRegular.compareTo(esperadoRegular) != 0) {
                    v.add(new Violacao("I2", cg + ".gTribRegular." + t[6], "publicado " + valorRegular
                            + ", esperado " + esperadoRegular + " (vBC " + vBC + ", alíquota " + aliquotaRegular + ")"));
                }
            }

            // I2: detalhe da compra governamental
            if (compraGov) {
                final BigDecimal aliquotaDetalhe = decimal(detalhe.path(t[3]));
                final BigDecimal valorDetalhe = decimal(detalhe.path(t[4]));
                // o detalhe traz o que seria devido sem a redistribuição: líquido do diferimento,
                // calculado com a alíquota do próprio tributo
                BigDecimal esperadoDetalhe = arred2(vBC.multiply(aliquotaDetalhe).movePointLeft(2));
                if (gDif.isObject()) {
                    esperadoDetalhe = esperadoDetalhe.subtract(arred2(vBC.multiply(aliquotaDetalhe).movePointLeft(2)
                            .multiply(decimal(gDif.path("pDif"))).movePointLeft(2)));
                }
                if (valorDetalhe.compareTo(esperadoDetalhe) != 0) {
                    v.add(new Violacao("I2", cg + ".gTribCompraGov." + t[4], "publicado " + valorDetalhe
                            + ", esperado " + esperadoDetalhe + " (vBC " + vBC + ", alíquota " + aliquotaDetalhe + ")"));
                }
                somaAliquotasDetalhe = somaAliquotasDetalhe.add(aliquotaDetalhe);
                somaValoresDetalhe = somaValoresDetalhe.add(valorDetalhe);
                somaAliquotasPrincipal = somaAliquotasPrincipal.add(aliquotaPublicada);
            }

            somaValores = somaValores.add(valor);
            somas.somar(t[2], valor);
            somas.somar(t[2] + ".vDif", vDif);
            somas.somar(t[2] + ".vDevTrib", vDevTrib);
        }

        // I1
        final BigDecimal vIBS = decimal(g.path("vIBS"));
        final BigDecimal somaIBS = decimal(g.path("gIBSUF").path("vIBSUF")).add(decimal(g.path("gIBSMun").path("vIBSMun")));
        if (vIBS.compareTo(somaIBS) != 0) {
            v.add(new Violacao("I1", cg + ".vIBS", "publicado " + vIBS + ", soma das partes " + somaIBS));
        }
        somas.somar("vIBS", vIBS);

        // I5 e I6
        if (compraGov && !regular.isObject()) {
            if (somaAliquotasPrincipal.compareTo(somaAliquotasDetalhe) != 0) {
                v.add(new Violacao("I5", cg, "soma das alíquotas do grupo principal " + somaAliquotasPrincipal
                        + ", soma das alíquotas do detalhe " + somaAliquotasDetalhe));
            }
            final BigDecimal diferenca = somaValoresDetalhe.subtract(somaValores).abs();
            if (diferenca.compareTo(TOLERANCIA_COMPRA_GOV) > 0) {
                v.add(new Violacao("I6", cg, "soma do detalhe " + somaValoresDetalhe + ", soma do grupo principal "
                        + somaValores));
            }
        }
    }

    private static void verificarImpostoSeletivo(JsonNode is, String caminho, Somas somas, List<Violacao> v) {
        final BigDecimal vIS = decimal(is.path("vIS"));
        somas.somar("vIS", vIS);
        final BigDecimal esperado = arred2(decimal(is.path("vBCIS")).multiply(decimal(is.path("pIS"))).movePointLeft(2)
                .add(decimal(is.path("qTrib")).multiply(decimal(is.path("adRemIS")))));
        if (vIS.compareTo(esperado) != 0) {
            v.add(new Violacao("I2", caminho + ".vIS", "publicado " + vIS + ", esperado " + esperado));
        }
    }

    // ─── Totais ────────────────────────────────────────────────────────────────

    private static void verificarTotais(JsonNode total, Somas somas, List<Violacao> v) {
        final String c = "$.total.tribCalc";
        final JsonNode tot = total.path("IBSCBSTot");
        if (tot.isObject()) {
            conferirTotal(v, c + ".IBSCBSTot.vBCIBSCBS", tot.path("vBCIBSCBS"), somas.de("vBCIBSCBS"));
            final JsonNode gIBS = tot.path("gIBS");
            conferirTotal(v, c + ".IBSCBSTot.gIBS.gIBSUF.vIBSUF", gIBS.path("gIBSUF").path("vIBSUF"), somas.de("vIBSUF"));
            conferirTotal(v, c + ".IBSCBSTot.gIBS.gIBSUF.vDif", gIBS.path("gIBSUF").path("vDif"), somas.de("vIBSUF.vDif"));
            conferirTotal(v, c + ".IBSCBSTot.gIBS.gIBSUF.vDevTrib", gIBS.path("gIBSUF").path("vDevTrib"), somas.de("vIBSUF.vDevTrib"));
            conferirTotal(v, c + ".IBSCBSTot.gIBS.gIBSMun.vIBSMun", gIBS.path("gIBSMun").path("vIBSMun"), somas.de("vIBSMun"));
            conferirTotal(v, c + ".IBSCBSTot.gIBS.gIBSMun.vDif", gIBS.path("gIBSMun").path("vDif"), somas.de("vIBSMun.vDif"));
            conferirTotal(v, c + ".IBSCBSTot.gIBS.gIBSMun.vDevTrib", gIBS.path("gIBSMun").path("vDevTrib"), somas.de("vIBSMun.vDevTrib"));
            conferirTotal(v, c + ".IBSCBSTot.gIBS.vIBS", gIBS.path("vIBS"), somas.de("vIBS"));
            final JsonNode gCBS = tot.path("gCBS");
            conferirTotal(v, c + ".IBSCBSTot.gCBS.vCBS", gCBS.path("vCBS"), somas.de("vCBS"));
            conferirTotal(v, c + ".IBSCBSTot.gCBS.vDif", gCBS.path("vDif"), somas.de("vCBS.vDif"));
            conferirTotal(v, c + ".IBSCBSTot.gCBS.vDevTrib", gCBS.path("vDevTrib"), somas.de("vCBS.vDevTrib"));
            final JsonNode gMono = tot.path("gMono");
            for (String campo : CAMPOS_MONOFASIA_TOTAL.keySet()) {
                conferirTotal(v, c + ".IBSCBSTot.gMono." + campo, gMono.path(campo), somas.de(campo));
            }

            // I1 no total
            final BigDecimal vIBS = decimal(gIBS.path("vIBS"));
            final BigDecimal partes = decimal(gIBS.path("gIBSUF").path("vIBSUF")).add(decimal(gIBS.path("gIBSMun").path("vIBSMun")));
            if (vIBS.compareTo(partes) != 0) {
                v.add(new Violacao("I1", c + ".IBSCBSTot.gIBS.vIBS", "publicado " + vIBS + ", soma das partes " + partes));
            }
        }
        final JsonNode isTot = total.path("ISTot");
        if (isTot.isObject()) {
            conferirTotal(v, c + ".ISTot.vIS", isTot.path("vIS"), somas.de("vIS"));
        }
    }

    private static void conferirTotal(List<Violacao> v, String caminho, JsonNode no, BigDecimal soma) {
        if (no.isMissingNode() || no.isNull()) {
            if (soma.signum() != 0) {
                v.add(new Violacao("I4", caminho, "total ausente, soma dos itens " + soma));
            }
            return;
        }
        final BigDecimal total = decimal(no);
        if (total.compareTo(soma) != 0) {
            v.add(new Violacao("I4", caminho, "total " + total + ", soma dos itens " + soma));
        }
    }

    // ─── Formatos ──────────────────────────────────────────────────────────────

    private static void verificarFormatos(JsonNode no, String caminho, List<Violacao> v) {
        if (no.isObject()) {
            for (Map.Entry<String, JsonNode> campo : no.properties()) {
                final String nome = campo.getKey();
                final JsonNode valor = campo.getValue();
                final String c = caminho + "." + nome;
                if (valor.isContainer()) {
                    verificarFormatos(valor, c, v);
                } else if (valor.isString()) {
                    final String texto = valor.asString();
                    if (ehMonetario(nome) && !MONETARIO.matcher(texto).matches()) {
                        v.add(new Violacao("I7", c, "valor monetário fora do formato de 2 casas: " + texto));
                    } else if (ehAliquota(nome) && !ALIQUOTA.matcher(texto).matches()) {
                        v.add(new Violacao("I7", c, "alíquota fora do formato de 2 a 4 casas: " + texto));
                    } else if ("qTrib".equals(nome) && !QUANTIDADE.matcher(texto).matches()) {
                        v.add(new Violacao("I7", c, "quantidade fora do formato: " + texto));
                    }
                } else if (valor.isNumber()) {
                    if (ehMonetario(nome) || ehAliquota(nome)) {
                        v.add(new Violacao("I7", c, "número publicado fora do tipo texto: " + valor));
                    }
                    if (EXPOENTE.matcher(valor.toString()).find()) {
                        v.add(new Violacao("I7", c, "número com expoente: " + valor));
                    }
                }
            }
        } else if (no.isArray()) {
            for (int i = 0; i < no.size(); i++) {
                verificarFormatos(no.get(i), caminho + "[" + i + "]", v);
            }
        }
    }

    private static boolean ehMonetario(String nome) {
        return nome.length() > 1 && nome.charAt(0) == 'v' && Character.isUpperCase(nome.charAt(1));
    }

    private static boolean ehAliquota(String nome) {
        return (nome.length() > 1 && nome.charAt(0) == 'p' && Character.isUpperCase(nome.charAt(1)))
                || "adRemIS".equals(nome);
    }

    // ─── Apoio ─────────────────────────────────────────────────────────────────

    private static BigDecimal decimal(JsonNode no) {
        if (no == null || no.isMissingNode() || no.isNull()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(no.isString() ? no.asString() : no.toString());
    }

    private static BigDecimal arred2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal arred4(BigDecimal valor) {
        return valor.setScale(4, RoundingMode.HALF_EVEN);
    }

    /** Somas dos campos publicados nos itens, por nome de campo. */
    private static final class Somas {
        private final Map<String, BigDecimal> valores = new java.util.HashMap<>();

        void somar(String campo, BigDecimal valor) {
            valores.merge(campo, valor, BigDecimal::add);
        }

        BigDecimal de(String campo) {
            return valores.getOrDefault(campo, BigDecimal.ZERO);
        }
    }
}
