package br.gov.serpro.rtc.testesunitarios.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;

/**
 * Política numérica da Calculadora RTC: aritmética interna exata, um único arredondamento por
 * número publicado (valor fiscal com 2 casas, alíquota com até 4, HALF_EVEN), somas sobre os
 * valores publicados e formatação que não altera o número já arredondado.
 * <p>
 * Este teste é idêntico na {@code api-regime-geral} e na {@code api-simples-nacional}, como a
 * classe que ele confere.
 */
class Teste_ArredondamentoUtils {

    private static final BigDecimal CBS_INTEGRAL = new BigDecimal("1.6839379378389463610262954");

    @Test
    void modoUnico_eHalfEven() {
        assertEquals(RoundingMode.HALF_EVEN, ArredondamentoUtils.MODO);
        assertEquals(RoundingMode.HALF_EVEN, ArredondamentoUtils.CONTEXTO_DIVISAO.getRoundingMode());
    }

    @Test
    void multiplicarESubtrair_saoExatos_semEscalaFixa() {
        // 0,9 × alíquota integral: nenhuma casa perdida.
        assertEquals(new BigDecimal("1.51554414405505172492366586"),
                ArredondamentoUtils.multiplicar(new BigDecimal("0.9"), CBS_INTEGRAL));
        assertEquals(new BigDecimal("0.90"),
                ArredondamentoUtils.subtrair(BigDecimal.ONE, new BigDecimal("0.10")));
        assertNull(ArredondamentoUtils.multiplicar(null, BigDecimal.ONE));
        assertNull(ArredondamentoUtils.subtrair(BigDecimal.ONE, null));
    }

    @Test
    void conversaoPercentualFracao_eExataNosDoisSentidos() {
        BigDecimal fracao = ArredondamentoUtils.percentualParaFracao(CBS_INTEGRAL);
        assertEquals(new BigDecimal("0.016839379378389463610262954"), fracao);
        assertEquals(CBS_INTEGRAL, ArredondamentoUtils.fracaoParaPercentual(fracao));
        assertNull(ArredondamentoUtils.percentualParaFracao(null));
        assertNull(ArredondamentoUtils.fracaoParaPercentual(null));
    }

    @Test
    void valorFiscal_arredondaUmaVezA2Casas_halfEven() {
        assertEquals(new BigDecimal("2.00"), ArredondamentoUtils.arredondarValorFiscal(new BigDecimal("2.005")));
        assertEquals(new BigDecimal("2.02"), ArredondamentoUtils.arredondarValorFiscal(new BigDecimal("2.015")));
        assertEquals(new BigDecimal("16631.49"), ArredondamentoUtils.arredondarValorFiscal(
                ArredondamentoUtils.multiplicar(new BigDecimal("987654.32"),
                        ArredondamentoUtils.percentualParaFracao(CBS_INTEGRAL))));
        assertNull(ArredondamentoUtils.arredondarValorFiscal(null));
    }

    @Test
    void valorFiscal_contaExataNaoSobeQuaseEmpate() {
        // 16.451,365 × 1,3663% = 224,774999995: a conta exata fica abaixo do meio centavo.
        BigDecimal exato = ArredondamentoUtils.multiplicar(
                ArredondamentoUtils.multiplicar(new BigDecimal("32902.73"), new BigDecimal("0.5")),
                ArredondamentoUtils.percentualParaFracao(new BigDecimal("1.3663")));
        assertEquals(new BigDecimal("224.774999995"), exato);
        assertEquals(new BigDecimal("224.77"), ArredondamentoUtils.arredondarValorFiscal(exato));
    }

    @Test
    void aliquotaPublicada_arredondaUmaVezA4Casas_halfEven() {
        // 8,8123% com redução de 60%: 3,52492 → 3,5249.
        assertEquals(new BigDecimal("3.5249"), ArredondamentoUtils.arredondarAliquotaPublicada(
                ArredondamentoUtils.multiplicar(new BigDecimal("8.8123"), new BigDecimal("0.4"))));
        assertEquals(new BigDecimal("0.1234"), ArredondamentoUtils.arredondarAliquotaPublicada(new BigDecimal("0.12345")));
        assertEquals(new BigDecimal("0.1236"), ArredondamentoUtils.arredondarAliquotaPublicada(new BigDecimal("0.12355")));
        assertEquals(new BigDecimal("8.8000"), ArredondamentoUtils.arredondarAliquotaPublicada(new BigDecimal("8.8")));
        assertNull(ArredondamentoUtils.arredondarAliquotaPublicada(null));
    }

    @Test
    void aliquotaDoLeiaute_arredondaNasCasasDoDocumento() {
        assertEquals(new BigDecimal("1.68"), ArredondamentoUtils.arredondarAliquota(CBS_INTEGRAL, 2));
        assertEquals(CBS_INTEGRAL, ArredondamentoUtils.arredondarAliquota(CBS_INTEGRAL, null));
        assertNull(ArredondamentoUtils.arredondarAliquota(null, 2));
    }

    @Test
    void totalizar_somaExata() {
        assertEquals(new BigDecimal("6.41"),
                ArredondamentoUtils.totalizar(Stream.of(new BigDecimal("2.00"), new BigDecimal("4.41"))));
        assertEquals(BigDecimal.ZERO, ArredondamentoUtils.totalizar(Stream.empty()));
    }

    @Test
    void dividir_usa34DigitosSignificativos() {
        BigDecimal terco = ArredondamentoUtils.dividir(BigDecimal.ONE, new BigDecimal("3"));
        assertEquals(34, terco.precision());
        // Dígitos significativos, não casas: um número pequeno preserva 34 dígitos.
        assertEquals(34, ArredondamentoUtils.dividir(new BigDecimal("0.0001"), new BigDecimal("3")).precision());
        assertNull(ArredondamentoUtils.dividir(null, BigDecimal.ONE));
    }

    @Test
    void ratear_parcelasSomamOTotal_centavosAoMaiorResto() {
        // 1,80 em trechos de 10, 13 e 10 de 33: 0,5454…, 0,7090… e 0,5454…
        BigDecimal total = new BigDecimal("1.80");
        BigDecimal extensao = new BigDecimal("33");
        List<BigDecimal> exatas = Stream.of("10", "13", "10")
                .map(e -> ArredondamentoUtils.dividir(total.multiply(new BigDecimal(e)), extensao))
                .toList();
        // truncadas: 0,54 + 0,70 + 0,54 = 1,78; os 2 centavos vão ao maior resto e, no empate,
        // à parcela de menor posição
        assertEquals(List.of(new BigDecimal("0.55"), new BigDecimal("0.71"), new BigDecimal("0.54")),
                ArredondamentoUtils.ratearValorFiscal(exatas));

        BigDecimal totalIbs = new BigDecimal("0.20");
        List<BigDecimal> exatasIbs = Stream.of("10", "13", "10")
                .map(e -> ArredondamentoUtils.dividir(totalIbs.multiply(new BigDecimal(e)), extensao))
                .toList();
        assertEquals(List.of(new BigDecimal("0.06"), new BigDecimal("0.08"), new BigDecimal("0.06")),
                ArredondamentoUtils.ratearValorFiscal(exatasIbs));
    }

    @Test
    void ratear_casosDeBorda() {
        assertEquals(List.of(), ArredondamentoUtils.ratearValorFiscal(List.of()));
        assertEquals(List.of(new BigDecimal("0.00"), new BigDecimal("0.00")),
                ArredondamentoUtils.ratearValorFiscal(List.of(BigDecimal.ZERO, BigDecimal.ZERO)));
        // parcela única: é o próprio valor fiscal
        assertEquals(List.of(new BigDecimal("2.00")),
                ArredondamentoUtils.ratearValorFiscal(List.of(new BigDecimal("2.005"))));
        // três terços de um centavo: o total é 0,01 e vai à primeira parcela
        BigDecimal terco = new BigDecimal("0.0033333333");
        assertEquals(List.of(new BigDecimal("0.01"), new BigDecimal("0.00"), new BigDecimal("0.00")),
                ArredondamentoUtils.ratearValorFiscal(List.of(terco, terco, terco)));
    }

    @Test
    void ratear_propriedade_somaIgualAoTotalEParcelaAMenosDeUmCentavoDaExata() {
        Random random = new Random(20261117L);
        BigDecimal centavo = new BigDecimal("0.01");
        for (int caso = 0; caso < 2000; caso++) {
            int quantidade = 1 + random.nextInt(12);
            List<BigDecimal> exatas = Stream.generate(() -> BigDecimal.valueOf(random.nextInt(5_000_000), 6))
                    .limit(quantidade).toList();
            List<BigDecimal> parcelas = ArredondamentoUtils.ratearValorFiscal(exatas);
            assertEquals(ArredondamentoUtils.arredondarValorFiscal(ArredondamentoUtils.totalizar(exatas.stream())),
                    ArredondamentoUtils.totalizar(parcelas.stream()));
            for (int i = 0; i < quantidade; i++) {
                assertEquals(2, parcelas.get(i).scale());
                assertEquals(-1, parcelas.get(i).subtract(exatas.get(i)).abs().compareTo(centavo));
            }
        }
    }

    @Test
    void formatarMoeda_naoAlteraValorFiscalJaArredondado() {
        Random random = new Random(20261117L);
        for (int caso = 0; caso < 2000; caso++) {
            BigDecimal valor = ArredondamentoUtils.arredondarValorFiscal(
                    BigDecimal.valueOf(Math.abs(random.nextLong() % 1_000_000_000_000L), 5));
            assertEquals(0, valor.compareTo(new BigDecimal(ArredondamentoUtils.formatarMoeda(valor))));
        }
        assertEquals("804.37", ArredondamentoUtils.formatarMoeda(new BigDecimal("804.37")));
        assertEquals("0.00", ArredondamentoUtils.formatarMoeda(BigDecimal.ZERO));
        assertEquals("0.00", ArredondamentoUtils.formatarMoeda(new BigDecimal("0E-8")));
    }

    @Test
    void formatarAliquota_apresentaDe2a4Casas() {
        assertEquals("1.6839", ArredondamentoUtils.formatarAliquota(CBS_INTEGRAL));
        assertEquals("0.0187", ArredondamentoUtils.formatarAliquota(new BigDecimal("0.01867380622521988789135487")));
        assertEquals("2.878", ArredondamentoUtils.formatarAliquota(new BigDecimal("2.8780")));
        assertEquals("0.00", ArredondamentoUtils.formatarAliquota(BigDecimal.ZERO));
        assertEquals("60.00", ArredondamentoUtils.formatarAliquota(new BigDecimal("60.0000")));
    }

    @Test
    void formatarAliquota_naoAlteraAliquotaPublicada() {
        Random random = new Random(20261117L);
        for (int caso = 0; caso < 2000; caso++) {
            BigDecimal aliquota = ArredondamentoUtils.arredondarAliquotaPublicada(
                    BigDecimal.valueOf(random.nextInt(100_000_000), 6));
            assertEquals(0, aliquota.compareTo(new BigDecimal(ArredondamentoUtils.formatarAliquota(aliquota))));
        }
    }

    @Test
    void formatarQuantidade_inteiroOu4Casas() {
        assertEquals("1000", ArredondamentoUtils.formatarQuantidade(new BigDecimal("1000.000")));
        assertEquals("55.5500", ArredondamentoUtils.formatarQuantidade(new BigDecimal("55.55")));
        assertEquals("0.0001", ArredondamentoUtils.formatarQuantidade(new BigDecimal("0.0001")));
    }
}
