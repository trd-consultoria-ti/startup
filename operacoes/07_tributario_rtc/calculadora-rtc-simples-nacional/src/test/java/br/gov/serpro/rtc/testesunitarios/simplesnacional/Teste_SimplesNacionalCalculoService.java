package br.gov.serpro.rtc.testesunitarios.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.CodigoRegimeApuracaoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.DestinoValorSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.EstadoItemSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.MotivoNaoCalculoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.model.output.MotivoEstadoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.TributosSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService.InsumoCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService.ResultadoCalculoSimples;

/**
 * Testes unitários do serviço de cálculo do Simples Nacional — casos dourados
 * da NT 2026.005 (matriz de regime × tipo de receita bruta e transformação de
 * alíquotas por compra governamental).
 *
 * <p>O serviço é puro: os testes montam em memória as linhas de parametrização
 * (efeito do tipo de receita bruta e matriz de regime) com os mesmos valores da
 * carga oficial das tabelas {@code TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL} e
 * {@code REGIME_APURACAO_SIMPLES_NACIONAL} — a fidelidade da carga real é
 * verificada por teste de integração próprio.
 *
 * <p>Salvo indicação, os casos usam baseCalculo=1000.00, vIS=0.00, pIBSSN=1.25
 * e pCBSSN=2.75, com cAtivSN válido.
 */
class Teste_SimplesNacionalCalculoService {

    private SimplesNacionalCalculoService service;

    @BeforeEach
    void setup() {
        service = new SimplesNacionalCalculoService();
    }

    // ─── Fixtures das linhas de parametrização (espelham a carga oficial) ──────

    /**
     * Linha do efeito do tpRBSN, com os valores da carga oficial da tabela
     * (o vIS NÃO compõe a receita bruta em nenhuma vigência).
     */
    static TipoReceitaBrutaSimples tipoReceita(int codigo) {
        TipoReceitaBrutaSimples tipo = new TipoReceitaBrutaSimples();
        tipo.setId((long) codigo);
        tipo.setCodigo(codigo);
        switch (codigo) {
            case 0 -> preencher(tipo, "Não é receita bruta", false, "0.0", false, false, false);
            case 1 -> preencher(tipo, "Receita bruta - interna", true, "1.0", false, true, true);
            case 2 -> preencher(tipo, "Receita bruta - interna sem cálculo de IBS e CBS", true, "0.0", false, false, true);
            case 3 -> preencher(tipo, "Receita bruta - exportação direta", true, "0.0", false, false, true);
            case 4 -> preencher(tipo, "Receita bruta - exportação indireta", true, "1.0", true, true, true);
            case 5 -> preencher(tipo, "Receita bruta - mercado interno e exportação", true, "0.5", false, true, true);
            case 9 -> preencher(tipo, "Fornecimento incompatível com SN", true, "0.0", false, false, false);
            default -> throw new IllegalArgumentException("tpRBSN sem carga: " + codigo);
        }
        return tipo;
    }

    /**
     * Linha do efeito do tpRBSN com o vIS compondo a receita bruta. SEM carga
     * correspondente hoje ({@code TRBS_IN_IS_COMPOE_RECEITA} é 0 em todas as
     * vigências): cobre a alavanca de parametrização que permanece no código
     * para uma eventual mudança legal futura, ativável só por script de
     * vigência.
     */
    static TipoReceitaBrutaSimples tipoReceitaComIsNaReceitaBruta(int codigo) {
        TipoReceitaBrutaSimples tipo = tipoReceita(codigo);
        tipo.setImpostoSeletivoCompoeReceita(true);
        return tipo;
    }

    private static void preencher(TipoReceitaBrutaSimples tipo, String descricao, boolean compoeReceita,
            String fatorReceita, boolean pendenteSuspenso, boolean exigeAliquota, boolean vedadoNaoTributavel) {
        tipo.setDescricao(descricao);
        tipo.setCompoeReceita(compoeReceita);
        tipo.setImpostoSeletivoCompoeReceita(false);
        tipo.setFatorReceita(new BigDecimal(fatorReceita));
        tipo.setPendenteSuspenso(pendenteSuspenso);
        tipo.setExigeAliquota(exigeAliquota);
        tipo.setVedadoNaoTributavel(vedadoNaoTributavel);
    }

    /** CRT=1 — Simples Nacional pleno: gera IBS; devido fica devido, suspenso fica suspenso. */
    static RegimeApuracaoSimples regimeCrt1() {
        return regime(1, null, true, DestinoValorSimplesEnum.DEVIDO, DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
    }

    /** CRT=2 — excesso de sublimite: NÃO gera o bloco de IBS na saída. */
    static RegimeApuracaoSimples regimeCrt2() {
        return regime(2, null, false, DestinoValorSimplesEnum.DEVIDO, DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
    }

    /** CRT=3 + tpRegimeIBSCBS=2: devido vira pendente/suspenso; o já suspenso vira ZERO (literal na NT). */
    static RegimeApuracaoSimples regimeCrt3TpRegime2() {
        return regime(3, 2, true, DestinoValorSimplesEnum.PENDENTE_SUSPENSO, DestinoValorSimplesEnum.ZERO);
    }

    private static RegimeApuracaoSimples regime(int crt, Integer tpRegime, boolean geraIbs,
            DestinoValorSimplesEnum destinoDevido, DestinoValorSimplesEnum destinoPendenteSuspenso) {
        RegimeApuracaoSimples regime = new RegimeApuracaoSimples();
        regime.setId((long) crt);
        // Linhas da carga oficial, identificadas pelo código neutro (a combinação
        // CRT × tpRegimeIBSCBS vive na tabela de enquadramento desde a V0067).
        regime.setCodigo(switch (crt) {
            case 1 -> CodigoRegimeApuracaoSimplesEnum.SN_PLENO;
            case 2 -> CodigoRegimeApuracaoSimplesEnum.SN_SUBLIMITE;
            default -> CodigoRegimeApuracaoSimplesEnum.PENDENTE;
        });
        regime.setCalculaTributos(true);
        regime.setDescricao("CRT " + crt + (tpRegime != null ? " + tpRegimeIBSCBS " + tpRegime : ""));
        regime.setGeraIbs(geraIbs);
        regime.setDestinoDevido(destinoDevido);
        regime.setDestinoPendenteSuspenso(destinoPendenteSuspenso);
        return regime;
    }

    // ─── Fábricas de insumo e asserções ────────────────────────────────────────

    private InsumoCalculoSimples insumo(RegimeApuracaoSimples regime, int tpRBSN,
            String base, String vIs, String pIbs, String pCbs, Integer tpEnteGov, String fatorTransicao) {
        return insumo(regime, tipoReceita(tpRBSN), base, vIs, pIbs, pCbs, tpEnteGov, fatorTransicao);
    }

    private InsumoCalculoSimples insumo(RegimeApuracaoSimples regime, TipoReceitaBrutaSimples tipoReceita,
            String base, String vIs, String pIbs, String pCbs, Integer tpEnteGov, String fatorTransicao) {
        return new InsumoCalculoSimples(regime, tipoReceita, null,
                new BigDecimal(base), new BigDecimal(vIs),
                pIbs != null ? new BigDecimal(pIbs) : null,
                pCbs != null ? new BigDecimal(pCbs) : null,
                tpEnteGov,
                fatorTransicao != null ? new BigDecimal(fatorTransicao) : null);
    }

    private InsumoCalculoSimples insumoPadrao(RegimeApuracaoSimples regime, int tpRBSN) {
        return insumo(regime, tpRBSN, "1000.00", "0.00", "1.25", "2.75", null, null);
    }

    private TributosSimplesDomain calcular(InsumoCalculoSimples insumo) {
        ResultadoCalculoSimples resultado = service.calcularItem(insumo);
        assertEquals(EstadoItemSimplesEnum.CALCULADO, resultado.estadoItem());
        assertNotNull(resultado.tribCalc());
        return resultado.tribCalc();
    }

    private void assertValor(String esperado, BigDecimal atual) {
        assertNotNull(atual);
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                () -> String.format("esperado %s, obtido %s", esperado, atual));
    }

    // ─── Casos dourados 1 a 8: CRT=1 ───────────────────────────────────────────

    @Test
    void caso01_crt1_tpRBSN1() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt1(), 1));
        assertValor("1000.00", trib.vRBSNitem());
        assertEquals(1, trib.tpRBSN());
        assertEquals("Receita bruta - interna", trib.tpRBSNDescricao());
        // Fator de receita 1.0: vBC = vRBSNitem.
        assertValor("1000.00", trib.vBC());
        assertValor("12.50", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("27.50", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
        assertValor("40.00", trib.totalItem());
    }

    @Test
    void caso02_crt1_tpRBSN1_impostoSeletivoForaDaReceitaBruta() {
        // Carga vigente (todas as vigências): vRBSNitem = 950 (o vIS NÃO compõe
        // a receita bruta), mas o IS segue repassado e entra no totalItem.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 1, "950.00", "50.00", "1.25", "2.75", null, null));
        assertValor("950.00", trib.vRBSNitem());
        // Valores fiscais (um único arredondamento, HALF_EVEN): 11,875 → 11,88 e
        // 26,125 → 26,12; o totalItem soma os valores fiscais (11,88 + 26,12 + 50).
        assertValor("11.88", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("26.12", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
        assertValor("50.00", trib.IS().valor());
        assertValor("88.00", trib.totalItem());
    }

    @Test
    void caso02b_crt1_tpRBSN1_impostoSeletivoNaReceitaBrutaQuandoParametrizado() {
        // Alavanca de parametrização SEM carga hoje (TRBS_IN_IS_COMPOE_RECEITA
        // ligado por hipótese): vRBSNitem = 950 + 50 e o IS entra no totalItem.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), tipoReceitaComIsNaReceitaBruta(1), "950.00", "50.00", "1.25", "2.75",
                        null, null));
        assertValor("1000.00", trib.vRBSNitem());
        assertValor("12.50", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("27.50", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
        assertValor("50.00", trib.IS().valor());
        assertValor("90.00", trib.totalItem());
    }

    @Test
    void caso03_crt1_tpRBSN5_metadeDaReceita() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt1(), 5));
        assertValor("1000.00", trib.vRBSNitem());
        // Fator de receita 0.5: vBC = metade do vRBSNitem.
        assertValor("500.00", trib.vBC());
        assertValor("6.25", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("13.75", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
    }

    @Test
    void caso04_crt1_tpRBSN4_exportacaoIndiretaSuspende() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt1(), 4));
        assertValor("1000.00", trib.vRBSNitem());
        assertValor("0.00", trib.vIBSSN());
        assertValor("12.50", trib.vIBSPendSusp());
        assertValor("0.00", trib.vCBSSN());
        assertValor("27.50", trib.vCBSPendSusp());
        // Pendente/suspenso fica fora do totalItem.
        assertValor("0.00", trib.totalItem());
    }

    @Test
    void caso05a07_crt1_tpRBSN2_3_9_tudoZero() {
        for (int tpRBSN : new int[] { 2, 3, 9 }) {
            TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt1(), tpRBSN));
            assertValor("1000.00", trib.vRBSNitem());
            // Fator de receita 0: vBC zerado (mas presente — o item é CALCULADO).
            assertValor("0.00", trib.vBC());
            assertValor("0.00", trib.vIBSSN());
            assertValor("0.00", trib.vIBSPendSusp());
            assertValor("0.00", trib.vCBSSN());
            assertValor("0.00", trib.vCBSPendSusp());
        }
    }

    @Test
    void caso08_crt1_tpRBSN0_receitaBrutaZerada() {
        // tpRBSN=0 (não compõe receita): vRBSNitem = 0, mesmo com base e vIS informados.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 0, "1000.00", "50.00", "1.25", "2.75", null, null));
        assertValor("0.00", trib.vRBSNitem());
        assertEquals(0, trib.tpRBSN());
        assertEquals("Não é receita bruta", trib.tpRBSNDescricao());
        assertValor("0.00", trib.vBC());
        assertValor("0.00", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("0.00", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
    }

    // ─── Casos dourados 9 e 10: CRT=2 (IBS suprimido) ──────────────────────────

    @Test
    void caso09_crt2_tpRBSN1_camposDeIbsSuprimidos() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt2(), 1));
        assertValor("1000.00", trib.vRBSNitem());
        // O vBC não depende da matriz de regime: presente também no CRT=2.
        assertValor("1000.00", trib.vBC());
        assertNull(trib.pIBSSN());
        assertNull(trib.vIBSSN());
        assertNull(trib.vIBSPendSusp());
        assertValor("27.50", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
        // totalItem sem IBS: apenas CBS + IS.
        assertValor("27.50", trib.totalItem());
    }

    @Test
    void caso10_crt2_tpRBSN4_cbsSuspensa() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt2(), 4));
        assertValor("1000.00", trib.vRBSNitem());
        assertNull(trib.vIBSSN());
        assertNull(trib.vIBSPendSusp());
        assertValor("0.00", trib.vCBSSN());
        assertValor("27.50", trib.vCBSPendSusp());
    }

    // ─── Casos dourados 11 a 13: CRT=3 + tpRegimeIBSCBS=2 ──────────────────────

    @Test
    void caso11_crt3tpReg2_tpRBSN1_tudoPendenteSuspenso() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt3TpRegime2(), 1));
        assertValor("1000.00", trib.vRBSNitem());
        assertValor("0.00", trib.vIBSSN());
        assertValor("12.50", trib.vIBSPendSusp());
        assertValor("0.00", trib.vCBSSN());
        assertValor("27.50", trib.vCBSPendSusp());
    }

    @Test
    void caso12_crt3tpReg2_tpRBSN5_metadePendenteSuspensa() {
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt3TpRegime2(), 5));
        assertValor("1000.00", trib.vRBSNitem());
        assertValor("0.00", trib.vIBSSN());
        assertValor("6.25", trib.vIBSPendSusp());
        assertValor("0.00", trib.vCBSSN());
        assertValor("13.75", trib.vCBSPendSusp());
    }

    @Test
    void caso13_crt3tpReg2_tpRBSN4_tudoZeroLiteralDaNT() {
        // Literal nas fórmulas P32/P33 da NT 2026.005: exportação indireta nesse
        // regime cai em "outro valor" — contraintuitivo e intencional. Na
        // parametrização, é o destino ZERO do valor pendente/suspenso.
        TributosSimplesDomain trib = calcular(insumoPadrao(regimeCrt3TpRegime2(), 4));
        assertValor("1000.00", trib.vRBSNitem());
        assertValor("0.00", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("0.00", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
    }

    // ─── Casos dourados 14 a 17: compra governamental (arts. 372/473) ──────────

    @Test
    void caso14_compraGovUniao_deslocaIbsParaCbs() {
        // União (tpEnteGov=1): regra estrutural, sem fator de transição.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 1, "1000.00", "0.00", "1.25", "2.75", 1, null));
        assertValor("0.00", trib.pIBSSN());
        assertValor("0.00", trib.vIBSSN());
        assertValor("0.00", trib.vIBSPendSusp());
        assertValor("4.00", trib.pCBSSN());
        assertValor("40.00", trib.vCBSSN());
        assertValor("0.00", trib.vCBSPendSusp());
        assertSomaPreservada(trib);
    }

    @Test
    void caso15_compraGovEstado_fator010() {
        // Fator 0.10 (vigência de 2029 na tabela de transição).
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 1, "1000.00", "0.00", "1.25", "2.75", 2, "0.10"));
        // pIBS' = 1.25 + 0.10 × 2.75 = 1.5250; pCBS' = 0.90 × 2.75 = 2.4750.
        assertValor("1.525", trib.pIBSSN());
        assertValor("15.25", trib.vIBSSN());
        assertValor("2.475", trib.pCBSSN());
        assertValor("24.75", trib.vCBSSN());
        assertSomaPreservada(trib);
    }

    @Test
    void caso16_compraGovDf_fator100_transicaoCompleta() {
        // Fator 1.00 (vigência a partir de 2033).
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 1, "1000.00", "0.00", "1.25", "2.75", 3, "1.00"));
        // f = 1.00: pIBS' = 1.25 + 2.75 = 4.00; pCBS' = 0.
        assertValor("4.00", trib.pIBSSN());
        assertValor("40.00", trib.vIBSSN());
        assertValor("0.00", trib.pCBSSN());
        assertValor("0.00", trib.vCBSSN());
        assertSomaPreservada(trib);
    }

    @Test
    void caso17_compraGovMunicipio_fatorZero_preTransicao() {
        // Fator 0.00 (vigência de 2027 a 2028): as alíquotas não mudam.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 1, "1000.00", "0.00", "1.25", "2.75", 4, "0.00"));
        assertValor("1.25", trib.pIBSSN());
        assertValor("12.50", trib.vIBSSN());
        assertValor("2.75", trib.pCBSSN());
        assertValor("27.50", trib.vCBSSN());
        assertSomaPreservada(trib);
    }

    /** Invariante dos casos 14-17: a transformação preserva a soma vIBSSN + vCBSSN = 40.00. */
    private void assertSomaPreservada(TributosSimplesDomain trib) {
        assertValor("40.00", trib.vIBSSN().add(trib.vCBSSN()));
    }

    // ─── Código de atividade inválido e alíquotas dispensadas ──────────────────

    @Test
    void caso18_cAtivSNInvalido_naoCalculado_somenteReceitaBrutaETipo() {
        MotivoEstadoSimplesDomain motivo = MotivoNaoCalculoSimplesEnum.CATIVSN_DESCONHECIDO.motivo("99");
        InsumoCalculoSimples semAtividade = new InsumoCalculoSimples(
                regimeCrt1(), tipoReceita(1), motivo,
                new BigDecimal("1000.00"), new BigDecimal("0.00"),
                new BigDecimal("1.25"), new BigDecimal("2.75"), null, null);

        ResultadoCalculoSimples resultado = service.calcularItem(semAtividade);

        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, resultado.estadoItem());
        // O motivo resolvido por quem montou o insumo vai para o resultado.
        assertSame(motivo, resultado.motivoEstado());
        TributosSimplesDomain trib = resultado.tribCalc();
        assertValor("1000.00", trib.vRBSNitem());
        assertEquals(1, trib.tpRBSN());
        assertEquals("Receita bruta - interna", trib.tpRBSNDescricao());
        // NAO_CALCULADO: sem vBC — o cálculo não ocorreu.
        assertNull(trib.vBC());
        assertNull(trib.pIBSSN());
        assertNull(trib.vIBSSN());
        assertNull(trib.vIBSPendSusp());
        assertNull(trib.pCBSSN());
        assertNull(trib.vCBSSN());
        assertNull(trib.vCBSPendSusp());
        assertNull(trib.IS());
        assertNull(trib.totalItem());
    }

    @Test
    void compraGovUniao_crt2_usaPIbsApenasComoInsumoDaTransformacao() {
        // CRT=2 + União: pCBS' = pCBS + pIBS, mas os campos de IBS seguem suprimidos.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt2(), 1, "1000.00", "0.00", "1.25", "2.75", 1, null));
        assertNull(trib.pIBSSN());
        assertNull(trib.vIBSSN());
        assertValor("4.00", trib.pCBSSN());
        assertValor("40.00", trib.vCBSSN());
    }

    @Test
    void compraGovUniao_crt2_semPIbs_trataInsumoComoZero() {
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt2(), 1, "1000.00", "0.00", null, "2.75", 1, null));
        assertNull(trib.pIBSSN());
        assertValor("2.75", trib.pCBSSN());
        assertValor("27.50", trib.vCBSSN());
    }

    @Test
    void aliquotasDispensadas_tpRBSN2_calculadoComTudoZero() {
        // tpRBSN=2 sem alíquotas (dispensadas pela parametrização) — cálculo com tudo zero.
        TributosSimplesDomain trib = calcular(
                insumo(regimeCrt1(), 2, "1000.00", "0.00", null, null, null, null));
        assertValor("1000.00", trib.vRBSNitem());
        assertNull(trib.pIBSSN());
        assertNull(trib.pCBSSN());
        assertValor("0.00", trib.vIBSSN());
        assertValor("0.00", trib.vCBSSN());
        assertValor("0.00", trib.totalItem());
    }
}
