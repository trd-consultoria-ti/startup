/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.arredondamento;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gerador determinístico de pedidos para o lote de arredondamento do regime geral
 * ({@code POST /calculadora/regime-geral}).
 * <p>
 * Cada nota é gerada a partir de uma semente própria, derivada da semente do lote e do índice da
 * nota. Assim, o lote de N notas é sempre o prefixo do lote de M &gt; N notas com a mesma semente,
 * e a comparação entre duas execuções pode ser feita nota a nota.
 * <p>
 * Composição:
 * <ul>
 *   <li>notas de 1 a 20 itens;</li>
 *   <li>bases de R$ 0,01 a R$ 10.000.000,00 em escala logarítmica, mais uma parcela de valores
 *       pequenos e uma parcela montada para cair em empate exato (terceira casa igual a 5) ou
 *       em quase empate no valor de um dos tributos;</li>
 *   <li>2026 com as alíquotas do banco e 2027 em diante com alíquotas informadas de até 4
 *       casas;</li>
 *   <li>classificações com redução de alíquota, redução de base, diferimento e tributação
 *       regular;</li>
 *   <li>compra governamental dos três entes (União, Estado e Município) e dos entes
 *       equivalentes, com e sem transferência da CBS;</li>
 *   <li>itens com Imposto Seletivo.</li>
 * </ul>
 * Não usa gerador aleatório externo: apenas {@link Random} com semente fixa.
 */
public final class GeradorLoteArredondamento {

    /** Semente padrão do lote. */
    public static final long SEMENTE_PADRAO = 20261117L;

    /** Tamanho padrão do lote (mantém a suíte rápida). */
    public static final int TAMANHO_PADRAO = 2000;

    private static final BigDecimal MEIO = new BigDecimal("0.5");

    /** Um pedido do lote. */
    public record Nota(int indice, String perfil, String corpo) {
    }

    /** Classificação usada pelo gerador, com o que ele precisa saber para montar empates. */
    private record Classificacao(String cst, String cClassTrib, String ncm, String nbs, String reducao,
            String fatorBase, String cstRegular, String cClassTribRegular) {

        static Classificacao ncm(String cst, String cClassTrib, String ncm, String reducao) {
            return new Classificacao(cst, cClassTrib, ncm, null, reducao, "1", null, null);
        }

        Classificacao comFatorBase(String fator) {
            return new Classificacao(cst, cClassTrib, ncm, nbs, reducao, fator, cstRegular, cClassTribRegular);
        }

        Classificacao comRegular(String cstReg, String cClassTribReg) {
            return new Classificacao(cst, cClassTrib, ncm, nbs, reducao, fatorBase, cstReg, cClassTribReg);
        }
    }

    private static final String NCM_GERAL = "84713012";

    /** Classificações vinculadas ao tpDoc 55 (NF-e). */
    private static final List<Classificacao> CLASSIFICACOES_NFE = List.of(
            Classificacao.ncm("000", "000001", NCM_GERAL, "0"),
            Classificacao.ncm("000", "000001", NCM_GERAL, "0"),
            Classificacao.ncm("000", "000001", NCM_GERAL, "0"),
            Classificacao.ncm("200", "200034", "03061200", "60"),
            Classificacao.ncm("200", "200034", "03061200", "60"),
            Classificacao.ncm("200", "200030", "39269030", "60"),
            Classificacao.ncm("200", "200038", "31010000", "60"),
            Classificacao.ncm("200", "200047", NCM_GERAL, "40"),
            Classificacao.ncm("200", "200003", "10063011", "100"),
            Classificacao.ncm("510", "510001", NCM_GERAL, "0"),
            Classificacao.ncm("515", "515001", "31010000", "60"),
            Classificacao.ncm("550", "550001", NCM_GERAL, "0").comRegular("200", "200032"),
            Classificacao.ncm("550", "550001", NCM_GERAL, "0").comRegular("000", "000001"));

    /** Classificações usadas nas notas sem tpDoc informado. */
    private static final List<Classificacao> CLASSIFICACOES_SEM_TPDOC = List.of(
            Classificacao.ncm("000", "000001", NCM_GERAL, "0"),
            Classificacao.ncm("200", "200052", NCM_GERAL, "30"),
            Classificacao.ncm("200", "200047", NCM_GERAL, "40"),
            Classificacao.ncm("200", "200046", NCM_GERAL, "50"),
            Classificacao.ncm("200", "200045", NCM_GERAL, "60"),
            Classificacao.ncm("200", "200027", NCM_GERAL, "70"),
            Classificacao.ncm("200", "200026", NCM_GERAL, "80"),
            Classificacao.ncm("222", "222001", NCM_GERAL, "0").comFatorBase("0.5"),
            Classificacao.ncm("222", "222001", NCM_GERAL, "0").comFatorBase("0.5"),
            Classificacao.ncm("510", "510001", NCM_GERAL, "0"));

    private static final String[][] LOCAIS = {
            { "3550308", "SP" },
            { "4313409", "RS" },
            { "1200013", "AC" } };

    private static final int[] ANOS_COMPRA_GOV = { 2027, 2028, 2029, 2030, 2031, 2032, 2033 };

    /**
     * Códigos de {@code tpEnteGov} sorteados: União (1), Estado (2) e Município (4) com o mesmo
     * peso, e os entes equivalentes — Distrito Federal (3), Consórcio Público (5) e Comitê Gestor
     * do IBS (6) — com peso menor.
     */
    private static final int[] ENTES_COMPRA_GOV = { 1, 1, 1, 2, 2, 2, 4, 4, 4, 3, 5, 6 };

    private GeradorLoteArredondamento() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Gera as {@code quantidade} primeiras notas do lote da semente informada. */
    public static List<Nota> gerar(long semente, int quantidade) {
        final List<Nota> notas = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            notas.add(gerarNota(semente, i));
        }
        return notas;
    }

    /** Gera a nota de índice {@code indice} do lote da semente informada. */
    public static Nota gerarNota(long semente, int indice) {
        final Random r = new Random(semente * 1_000_003L + indice);
        final int sorteio = r.nextInt(100);
        if (sorteio < 30) {
            return nota(indice, "2026-banco", r, 2026, true, CLASSIFICACOES_NFE, false, 0);
        }
        if (sorteio < 60) {
            return nota(indice, "2027-informada", r, 2027, true, CLASSIFICACOES_NFE, false, 0);
        }
        if (sorteio < 80) {
            final int ano = ANOS_COMPRA_GOV[r.nextInt(ANOS_COMPRA_GOV.length)];
            final int ente = ENTES_COMPRA_GOV[r.nextInt(ENTES_COMPRA_GOV.length)];
            return nota(indice, "compra-gov-" + ano + "-ente" + ente, r, ano, true, CLASSIFICACOES_NFE, false, ente);
        }
        if (sorteio < 90) {
            final int ano = r.nextBoolean() ? 2026 : 2027;
            return nota(indice, "sem-tpdoc-" + ano, r, ano, false, CLASSIFICACOES_SEM_TPDOC, false, 0);
        }
        return nota(indice, "imposto-seletivo-2027", r, 2027, true, CLASSIFICACOES_NFE, true, 0);
    }

    private static Nota nota(int indice, String perfil, Random r, int ano, boolean comTpDoc,
            List<Classificacao> classificacoes, boolean comImpostoSeletivo, int enteGov) {
        final boolean aliquotasInformadas = ano >= 2027;
        final String[] local = LOCAIS[r.nextInt(LOCAIS.length)];
        final int quantidadeItens = 1 + r.nextInt(20);

        // alíquotas da nota: parte das notas usa o mesmo trio em todos os itens
        final boolean trioUnico = r.nextBoolean();
        final BigDecimal[] trioNota = sortearAliquotas(r);

        final StringBuilder sb = new StringBuilder(256 + quantidadeItens * 220);
        sb.append("{\"id\":\"lote-").append(indice).append("\",\"versao\":\"0.0.1\"");
        sb.append(",\"dhFatoGerador\":\"").append(ano).append("-03-15T10:00:00-03:00\"");
        sb.append(",\"municipio\":").append(local[0]).append(",\"uf\":\"").append(local[1]).append('"');
        if (comTpDoc) {
            sb.append(",\"tpDoc\":55");
        }
        sb.append(",\"itens\":[");
        for (int n = 1; n <= quantidadeItens; n++) {
            if (n > 1) {
                sb.append(',');
            }
            final BigDecimal[] trio;
            if (!aliquotasInformadas) {
                // 2026: alíquotas do banco (CBS 0,9%; IBS UF 0,1%; IBS Mun 0%)
                trio = new BigDecimal[] { new BigDecimal("0.9"), new BigDecimal("0.1"), BigDecimal.ZERO };
            } else {
                trio = trioUnico ? trioNota : sortearAliquotas(r);
            }
            if (comImpostoSeletivo && r.nextInt(100) < 60) {
                itemComImpostoSeletivo(sb, n, r, trio);
            } else {
                final Classificacao c = classificacoes.get(r.nextInt(classificacoes.size()));
                item(sb, n, r, c, trio, aliquotasInformadas);
            }
        }
        sb.append(']');
        if (enteGov > 0) {
            sb.append(",\"gCompraGov\":{\"tpEnteGov\":").append(enteGov).append(",\"tpOperGov\":1}");
        }
        sb.append('}');
        return new Nota(indice, perfil, sb.toString());
    }

    private static void item(StringBuilder sb, int numero, Random r, Classificacao c, BigDecimal[] trio,
            boolean aliquotasInformadas) {
        final BigDecimal base = sortearBase(r, c, trio);
        sb.append("{\"numero\":").append(numero);
        sb.append(",\"cst\":\"").append(c.cst()).append("\",\"cClassTrib\":\"").append(c.cClassTrib()).append('"');
        sb.append(",\"baseCalculo\":").append(base.toPlainString());
        if (c.ncm() != null) {
            sb.append(",\"ncm\":\"").append(c.ncm()).append('"');
        }
        if (c.nbs() != null) {
            sb.append(",\"nbs\":\"").append(c.nbs()).append('"');
        }
        if (c.cstRegular() != null) {
            sb.append(",\"tributacaoRegular\":{\"cst\":\"").append(c.cstRegular()).append("\",\"cClassTrib\":\"")
                    .append(c.cClassTribRegular()).append("\"}");
        }
        if (aliquotasInformadas) {
            sb.append(",\"aliquotasNominais\":{\"cbs\":").append(trio[0].toPlainString())
                    .append(",\"ibsEstadual\":").append(trio[1].toPlainString())
                    .append(",\"ibsMunicipal\":").append(trio[2].toPlainString()).append('}');
        }
        sb.append('}');
    }

    private static void itemComImpostoSeletivo(StringBuilder sb, int numero, Random r, BigDecimal[] trio) {
        final Classificacao integral = CLASSIFICACOES_NFE.get(0);
        final BigDecimal base = sortearBase(r, integral, trio);
        final int quantidade = 1 + r.nextInt(50);
        final BigDecimal adValorem = decimalAleatorio(r, 1, 30, r.nextInt(5));
        final BigDecimal adRem = decimalAleatorio(r, 0, 25, r.nextInt(5));
        sb.append("{\"numero\":").append(numero);
        sb.append(",\"cst\":\"000\",\"cClassTrib\":\"000001\",\"ncm\":\"24021000\"");
        sb.append(",\"quantidade\":").append(quantidade).append(",\"unidade\":\"VN\"");
        sb.append(",\"baseCalculo\":").append(base.toPlainString());
        sb.append(",\"impostoSeletivo\":{\"cst\":\"000\",\"cClassTrib\":\"000001\",\"baseCalculo\":")
                .append(base.toPlainString()).append(",\"quantidade\":").append(quantidade)
                .append(",\"unidade\":\"VN\",\"impostoInformado\":0}");
        sb.append(",\"aliquotasNominais\":{\"cbs\":").append(trio[0].toPlainString())
                .append(",\"ibsEstadual\":").append(trio[1].toPlainString())
                .append(",\"ibsMunicipal\":").append(trio[2].toPlainString())
                .append(",\"impostoSeletivo\":{\"adValorem\":").append(adValorem.toPlainString())
                .append(",\"adRem\":").append(adRem.toPlainString()).append("}}");
        sb.append('}');
    }

    /** CBS, IBS estadual e IBS municipal informados (pontos percentuais, até 4 casas). */
    private static BigDecimal[] sortearAliquotas(Random r) {
        final BigDecimal cbs = switch (r.nextInt(4)) {
            case 0 -> new BigDecimal("8.8");
            case 1 -> new BigDecimal("8.8123");
            default -> decimalAleatorio(r, 0, 12, r.nextInt(5));
        };
        final BigDecimal ibsUf;
        final BigDecimal ibsMun;
        if (r.nextInt(100) < 40) {
            ibsUf = new BigDecimal("0.05");
            ibsMun = new BigDecimal("0.05");
        } else {
            ibsUf = decimalAleatorio(r, 0, 18, r.nextInt(5));
            ibsMun = r.nextInt(10) == 0 ? BigDecimal.ZERO : decimalAleatorio(r, 0, 5, r.nextInt(5));
        }
        return new BigDecimal[] { cbs, ibsUf, ibsMun };
    }

    /** Decimal positivo em [min, max) com exatamente {@code casas} casas (sem zeros à direita). */
    private static BigDecimal decimalAleatorio(Random r, int min, int max, int casas) {
        final long escala = BigInteger.TEN.pow(casas).longValueExact();
        final long faixa = (long) (max - min) * escala;
        final long sorteado = (long) min * escala + 1 + (long) (r.nextDouble() * (faixa - 1));
        return BigDecimal.valueOf(sorteado, casas).stripTrailingZeros();
    }

    /**
     * Base de cálculo do item, com 2 casas, entre R$ 0,01 e R$ 10.000.000,00.
     * <ul>
     *   <li>60%: escala logarítmica;</li>
     *   <li>25%: ajustada para que o valor de um dos tributos caia em empate exato ou no quase
     *       empate mais próximo;</li>
     *   <li>15%: valores pequenos (até R$ 20,00), em que as frações de centavo pesam nas somas.</li>
     * </ul>
     */
    private static BigDecimal sortearBase(Random r, Classificacao c, BigDecimal[] trio) {
        final int tipo = r.nextInt(100);
        if (tipo < 15) {
            return BigDecimal.valueOf(1 + r.nextInt(2000), 2);
        }
        final long centavos = centavosEscalaLogaritmica(r);
        if (tipo < 75) {
            return BigDecimal.valueOf(centavos, 2);
        }
        return BigDecimal.valueOf(centavosParaEmpate(r, centavos, c, trio), 2);
    }

    private static long centavosEscalaLogaritmica(Random r) {
        final double expoente = r.nextDouble() * 9.0; // 10^0 a 10^9 centavos
        final long centavos = Math.round(Math.pow(10.0, expoente));
        return Math.max(1L, Math.min(1_000_000_000L, centavos));
    }

    /**
     * Procura, numa janela de centavos a partir de {@code inicio}, a base cujo valor de tributo
     * (base × fator da base × alíquota × (1 − redução)) fica mais perto de um meio centavo. Quando
     * a janela contém um empate exato, ele é o escolhido.
     */
    private static long centavosParaEmpate(Random r, long inicio, Classificacao c, BigDecimal[] trio) {
        BigDecimal aliquota = trio[r.nextInt(trio.length)];
        if (aliquota.signum() == 0) {
            aliquota = trio[0];
        }
        final BigDecimal fracao = aliquota.movePointLeft(2)
                .multiply(BigDecimal.ONE.subtract(new BigDecimal(c.reducao()).movePointLeft(2)))
                .multiply(new BigDecimal(c.fatorBase()));
        if (fracao.signum() == 0) {
            return inicio;
        }
        long melhor = inicio;
        BigDecimal menorDistancia = null;
        final long limite = Math.min(1_000_000_000L, inicio + 400);
        for (long centavos = inicio; centavos <= limite; centavos++) {
            // valor do tributo em centavos (exato)
            final BigDecimal valor = BigDecimal.valueOf(centavos).multiply(fracao);
            final BigDecimal parteFracionaria = valor.subtract(new BigDecimal(valor.toBigInteger()));
            final BigDecimal distancia = parteFracionaria.subtract(MEIO).abs();
            if (menorDistancia == null || distancia.compareTo(menorDistancia) < 0) {
                menorDistancia = distancia;
                melhor = centavos;
                if (distancia.signum() == 0) {
                    break;
                }
            }
        }
        return melhor;
    }
}
