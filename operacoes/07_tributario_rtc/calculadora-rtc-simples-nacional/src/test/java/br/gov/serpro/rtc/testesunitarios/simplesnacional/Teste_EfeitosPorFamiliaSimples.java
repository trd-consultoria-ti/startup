package br.gov.serpro.rtc.testesunitarios.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoDfeCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.CodigoRegimeApuracaoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.DestinoValorSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.EstadoItemSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.MotivoNaoCalculoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.model.input.ItemSimplesUnificadoInput;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObjetoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.TributosSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.repository.AtividadeSimplesCalculoRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TipoDfeCalculoSimplesRepository;
import br.gov.serpro.rtc.domain.service.ValidacaoNomenclaturaService;
import br.gov.serpro.rtc.domain.service.NcmService;
import br.gov.serpro.rtc.domain.service.NbsService;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaObrigatoriaNaoInformadaException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaIncompativelTipoDfeException;
import br.gov.serpro.rtc.domain.model.enumeration.SiglasDFeEnum;
import br.gov.serpro.rtc.api.simplesnacional.service.CalculoSimplesUnificadoService;
import br.gov.serpro.rtc.api.simplesnacional.service.CalculoSimplesUnificadoService.DadosOperacaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService;
import br.gov.serpro.rtc.api.simplesnacional.service.ParametroCalculoSimplesService;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.adapter.ValidacaoNomenclaturaAdapter;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.AliquotasItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ContextoItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.EntradaItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ResolvedorAliquotasItem;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ItemAProcessar;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService.InsumoCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService.ResultadoCalculoSimples;
import br.gov.serpro.rtc.shared.exceptionhandler.ErrosCalculoException;

/**
 * Testes unitários das diferenças de cálculo por família de documento e do
 * regime {@code SOMENTE_RECEITA}: efeito do tpRBSN na NFS-e, alíquota de 2
 * casas DEPOIS da compra governamental, Imposto Seletivo, um item e cAtivSN
 * de um dígito.
 */
@ExtendWith(MockitoExtension.class)
class Teste_EfeitosPorFamiliaSimples {

    private static final LocalDate DATA = LocalDate.of(2027, 3, 15);
    private static final String BASE_URL = "http://localhost/api";

    private final SimplesNacionalCalculoService calculo = new SimplesNacionalCalculoService();

    @Mock
    private ParametroCalculoSimplesService parametroService;

    @Mock
    private ClassificacaoTributariaSimplesRepository classificacaoRepository;

    @Mock
    private AtividadeSimplesCalculoRepository atividadeRepository;

    @Mock private ValidacaoNomenclaturaService validacaoNomenclaturaService;
    @Mock private TipoDfeCalculoSimplesRepository tipoDfeRepository;
    @Mock private NcmService ncmService;
    @Mock private NbsService nbsService;

    private ProcessadorItemSimples processador;
    private ValidacaoNomenclaturaAdapter validacaoSimples;

    @BeforeEach
    void setup() {
        validacaoSimples = new ValidacaoNomenclaturaAdapter(ncmService, nbsService, validacaoNomenclaturaService);
        processador = new ProcessadorItemSimples(calculo, parametroService, classificacaoRepository,
                atividadeRepository, validacaoSimples);
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    /** Linha da NFS-e do efeito do tpRBSN (V0067): só o código 1 gera valor, sem pendente/suspenso. */
    static TipoReceitaBrutaSimples tipoReceitaNfse(int codigo) {
        TipoReceitaBrutaSimples tipo = Teste_SimplesNacionalCalculoService.tipoReceita(codigo);
        tipo.setVocabularioRegime(VocabularioRegimeSimplesEnum.NFSE);
        tipo.setFatorReceita(codigo == 1 ? BigDecimal.ONE : BigDecimal.ZERO);
        tipo.setPendenteSuspenso(false);
        tipo.setExigeAliquota(codigo == 1);
        return tipo;
    }

    private static RegimeApuracaoSimples regime(CodigoRegimeApuracaoSimplesEnum codigo) {
        RegimeApuracaoSimples regime = new RegimeApuracaoSimples();
        regime.setCodigo(codigo);
        regime.setDescricao(codigo.name());
        switch (codigo) {
            case SN_PLENO -> preencher(regime, true, DestinoValorSimplesEnum.DEVIDO, DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
            case SN_SUBLIMITE -> preencher(regime, false, DestinoValorSimplesEnum.DEVIDO,
                    DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
            case PENDENTE -> preencher(regime, true, DestinoValorSimplesEnum.PENDENTE_SUSPENSO, DestinoValorSimplesEnum.ZERO);
            case PENDENTE_SUBLIMITE -> preencher(regime, false, DestinoValorSimplesEnum.PENDENTE_SUSPENSO,
                    DestinoValorSimplesEnum.ZERO);
            case SOMENTE_RECEITA -> regime.setCalculaTributos(false);
        }
        return regime;
    }

    private static void preencher(RegimeApuracaoSimples regime, boolean geraIbs, DestinoValorSimplesEnum devido,
            DestinoValorSimplesEnum pendente) {
        regime.setCalculaTributos(true);
        regime.setGeraIbs(geraIbs);
        regime.setDestinoDevido(devido);
        regime.setDestinoPendenteSuspenso(pendente);
    }

    private TributosSimplesDomain calcular(RegimeApuracaoSimples regime, TipoReceitaBrutaSimples tipo, String base,
            String vIs, String pIbs, String pCbs, Integer tpEnteGov, String fator, Integer casas) {
        ResultadoCalculoSimples resultado = calculo.calcularItem(new InsumoCalculoSimples(regime, tipo, null,
                new BigDecimal(base), vIs == null ? null : new BigDecimal(vIs), new BigDecimal(pIbs),
                new BigDecimal(pCbs), tpEnteGov, fator == null ? null : new BigDecimal(fator), casas));
        return resultado.tribCalc();
    }

    private static void assertValor(String esperado, BigDecimal atual) {
        assertNotNull(atual);
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                () -> String.format("esperado %s, obtido %s", esperado, atual));
    }

    // ─── Alíquota de 2 casas (NFS-e) ───────────────────────────────────────────

    @Test
    void nfse_aliquotaDe2Casas_valorSaiDaBaseVezesAAliquotaArredondada() {
        TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), tipoReceitaNfse(1),
                "10000.00", null, "0.0263", "2.3874", null, null, 2);
        assertValor("2.39", trib.pCBSSN());
        assertValor("0.03", trib.pIBSSN());
        assertValor("239.00", trib.vCBSSN());
        assertValor("3.00", trib.vIBSSN());
        // Com a precisão integral seriam 238,74 e 2,63.
        TributosSimplesDomain integral = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO),
                tipoReceitaNfse(1), "10000.00", null, "0.0263", "2.3874", null, null, null);
        assertValor("238.74", integral.vCBSSN());
        assertValor("2.63", integral.vIBSSN());
    }

    @Test
    void nfse_arredondamentoDepoisDaCompraGovernamental_uniao() {
        // pCBS' = 2,3874 + 0,0263 = 2,4137 ⇒ 2,41 (arredondando antes daria 2,39 + 0,03 = 2,42).
        TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), tipoReceitaNfse(1),
                "10000.00", null, "0.0263", "2.3874", 1, null, 2);
        assertValor("2.41", trib.pCBSSN());
        assertValor("0.00", trib.pIBSSN());
        assertValor("241.00", trib.vCBSSN());
        assertValor("0.00", trib.vIBSSN());
    }

    @Test
    void nfse_arredondamentoDepoisDaCompraGovernamental_enteComFator() {
        // pIBS' = 0,0263 + 0,10 × 2,3874 = 0,26504 ⇒ 0,27; pCBS' = 0,90 × 2,3874 = 2,14866 ⇒ 2,15.
        TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), tipoReceitaNfse(1),
                "10000.00", null, "0.0263", "2.3874", 2, "0.10", 2);
        assertValor("2.15", trib.pCBSSN());
        assertValor("0.27", trib.pIBSSN());
        assertValor("215.00", trib.vCBSSN());
        assertValor("27.00", trib.vIBSSN());
    }

    @Test
    void nfse_arredondamentoNoModoDoAdr001_meioParaOPar() {
        TributosSimplesDomain par = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), tipoReceitaNfse(1),
                "100.00", null, "0.125", "2.385", null, null, 2);
        assertValor("2.38", par.pCBSSN());
        assertValor("0.12", par.pIBSSN());
        TributosSimplesDomain impar = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), tipoReceitaNfse(1),
                "100.00", null, "0.135", "2.395", null, null, 2);
        assertValor("2.40", impar.pCBSSN());
        assertValor("0.14", impar.pIBSSN());
    }

    // ─── Efeito do tpRBSN na NFS-e ─────────────────────────────────────────────

    @Test
    void nfse_tpRBSN4e5_zeradosSemPendenteSuspenso() {
        for (int tpRBSN : List.of(4, 5)) {
            TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO),
                    tipoReceitaNfse(tpRBSN), "1000.00", null, "0.0263", "2.3874", null, null, 2);
            assertValor("1000.00", trib.vRBSNitem());
            assertValor("0.00", trib.vBC());
            assertValor("0.00", trib.vCBSSN());
            assertValor("0.00", trib.vCBSPendSusp());
            assertValor("0.00", trib.vIBSSN());
            assertValor("0.00", trib.vIBSPendSusp());
        }
        // Na família CRT, o mesmo tpRBSN 4 fica pendente/suspenso.
        TributosSimplesDomain crt = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO),
                Teste_SimplesNacionalCalculoService.tipoReceita(4), "1000.00", "0.00", "0.0263", "2.3874", null, null,
                null);
        assertValor("23.87", crt.vCBSPendSusp());
    }

    @Test
    void nfse_pendente_valorVaiParaPendenteSuspenso() {
        TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.PENDENTE), tipoReceitaNfse(1),
                "1000.00", null, "0.0263", "2.3874", null, null, 2);
        assertValor("0.00", trib.vCBSSN());
        assertValor("23.90", trib.vCBSPendSusp());
        assertValor("0.30", trib.vIBSPendSusp());
    }

    @Test
    void pendenteSublimite_soACbs_pendenteSuspensa() {
        TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.PENDENTE_SUBLIMITE),
                tipoReceitaNfse(1), "1000.00", null, "0.0263", "2.3874", null, null, 2);
        assertNull(trib.pIBSSN());
        assertNull(trib.vIBSSN());
        assertNull(trib.vIBSPendSusp());
        assertValor("23.90", trib.vCBSPendSusp());
        assertValor("0.00", trib.totalItem());
    }

    // ─── Imposto Seletivo e SOMENTE_RECEITA no cálculo ─────────────────────────

    @Test
    void semImpostoSeletivo_grupoISForaDaResposta() {
        TributosSimplesDomain trib = calcular(regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), tipoReceitaNfse(1),
                "1000.00", null, "0.0263", "2.3874", null, null, 2);
        assertNull(trib.IS());
        assertValor("24.20", trib.totalItem());
    }

    @Test
    void somenteReceita_naoCalcula_soAReceitaBruta() {
        ResultadoCalculoSimples resultado = calculo.calcularItem(new InsumoCalculoSimples(
                regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), Teste_SimplesNacionalCalculoService.tipoReceita(1),
                null, new BigDecimal("1000.00"), BigDecimal.ZERO, null, null, null, null, null));
        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, resultado.estadoItem());
        assertEquals(MotivoNaoCalculoSimplesEnum.REGIME_SOMENTE_RECEITA, resultado.motivoEstado().codigo());
        assertValor("1000.00", resultado.tribCalc().vRBSNitem());
        assertEquals(1, resultado.tribCalc().tpRBSN());
        assertNull(resultado.tribCalc().vBC());
        assertNull(resultado.tribCalc().vCBSSN());
    }

    // ─── Processador: Imposto Seletivo, cAtivSN e SOMENTE_RECEITA ──────────────

    private void stubClassificacao(int tpRBSN) {
        ClassificacaoTributariaSimples classificacao = new ClassificacaoTributariaSimples();
        classificacao.setId(1L);
        classificacao.setCodigo("000001");
        classificacao.setTpRBSN(tpRBSN);
        lenient().when(classificacaoRepository.buscarVigentes(eq("000001"), any())).thenReturn(List.of(classificacao));
        lenient().when(classificacaoRepository.contarVinculosComTpDoc(eq("000001"), anyInt(), any())).thenReturn(1L);
        lenient().when(parametroService.buscarTipoReceitaBruta(eq(tpRBSN), eq(VocabularioRegimeSimplesEnum.NFSE), any()))
                .thenReturn(Optional.of(tipoReceitaNfse(tpRBSN)));
    }

    private static ContextoItemSimples contextoNfse(RegimeApuracaoSimples regime) {
        return new ContextoItemSimples(DATA, 91, regime, null, null, VocabularioRegimeSimplesEnum.NFSE, true, false,
                2, "NFSe", false);
    }

    private static ContextoItemSimples contextoNfse(RegimeApuracaoSimples regime, boolean permiteNcmZero) {
        return new ContextoItemSimples(DATA, 91, regime, null, null, VocabularioRegimeSimplesEnum.NFSE, true, false,
                2, "NFSe", permiteNcmZero);
    }

    private static final ResolvedorAliquotasItem PAR_FIXO = (dados, collector) -> new AliquotasItemSimples(
            new BigDecimal("0.0263"), new BigDecimal("2.3874"), false, null);

    private ObjetoSimplesDomain processar(String cAtivSN, String vIs, RegimeApuracaoSimples regime) {
        return processador.processarItem(new EntradaItemSimples(1, cAtivSN, "000001", "1000.00", vIs, null, null), 0,
                contextoNfse(regime), PAR_FIXO, BASE_URL).objeto();
    }

    private ObjetoSimplesDomain processarNomenclatura(String ncm, String nbs) {
        return processador.processarItem(new EntradaItemSimples(1, "07", "000001", "1000.00", null, ncm, nbs), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)), PAR_FIXO, BASE_URL).objeto();
    }

    @Test
    void nomenclaturaAusente_eValidadaQuandoClassificacaoExigeCodigo() {
        stubClassificacao(1);
        doThrow(new NomenclaturaObrigatoriaNaoInformadaException("000001", "NFSe"))
                .when(validacaoNomenclaturaService).validar(any(), any(), any(), any(), any(), any(), any(), any());

        ObjetoSimplesDomain item = processarNomenclatura(null, null);

        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, item.estadoItem());
        assertEquals("VSN-044", item.erros().get(0).code());
        verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"), eq(null), eq(null), eq(DATA),
                eq("CBS e IBS"), any());
    }

    @Test
    void nomenclaturasSimultaneas_mapeiaPrimeiraFalhaEConservaMensagem() {
        stubClassificacao(1);
        ObjetoSimplesDomain item = processarNomenclatura("22030000", "114051000");
        assertEquals("VSN-041", item.erros().get(0).code());
        assertEquals("Não é permitido informar NCM e NBS simultaneamente para um item", item.erros().get(0).detail());
        verifyNoInteractions(ncmService, nbsService, validacaoNomenclaturaService);
    }

    @Test
    void nomenclaturaInexistente_mapeiaMensagemDaExcecao() {
        stubClassificacao(1);
        when(ncmService.existeNcm("22030000", DATA)).thenReturn(false);
        ObjetoSimplesDomain item = processarNomenclatura("22030000", null);
        assertEquals("VSN-049", item.erros().get(0).code());
        assertEquals("NCM de código 22030000 não encontrada para a data " + DATA, item.erros().get(0).detail());
        verifyNoInteractions(validacaoNomenclaturaService);
    }

    @Test
    void nbsInexistente_mapeiaMensagemDaExcecao() {
        stubClassificacao(1);
        when(nbsService.existeNbs("114051000", DATA)).thenReturn(false);
        ObjetoSimplesDomain item = processarNomenclatura(null, "114051000");
        assertEquals("VSN-050", item.erros().get(0).code());
        assertEquals("NBS de código 114051000 não encontrada para a data " + DATA, item.erros().get(0).detail());
        verifyNoInteractions(validacaoNomenclaturaService);
    }

    @Test
    void nomenclaturaValida_eProcessadaSemErro() {
        stubClassificacao(1);
        when(ncmService.existeNcm("22030000", DATA)).thenReturn(true);
        ObjetoSimplesDomain item = processarNomenclatura("22030000", null);
        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, item.estadoItem());
        assertNull(item.erros());
        verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"), eq("22030000"), eq(null), eq(DATA),
                eq("CBS e IBS"), any());
        verify(classificacaoRepository, times(1)).buscarVigentes("000001", DATA);
    }

    @Test
    void ncmZeroPermitido_converteSomenteOsDoisCodigosExatosAntesDasDuasValidacoes() {
        stubClassificacao(1);
        when(nbsService.existeNbs("001234567", DATA)).thenReturn(true);
        for (String recebido : List.of("00", "00000000")) {
            ObjetoSimplesDomain item = processador.processarItem(
                    new EntradaItemSimples(1, "07", "000001", "1000.00", null, recebido, null), 0,
                    contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), true), PAR_FIXO,
                    BASE_URL).objeto();
            assertNull(item.erros());
        }
        verify(validacaoNomenclaturaService, times(2)).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"),
                eq(null), eq(null), eq(DATA), eq("CBS e IBS"), any());
        // A regra também permite NBS simultâneo com NCM convertido: NCM deixa de ser informado.
        ObjetoSimplesDomain comNbs = processador.processarItem(
                new EntradaItemSimples(1, "07", "000001", "1000.00", null, "00", "001234567"), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), true), PAR_FIXO,
                BASE_URL).objeto();
        assertNull(comNbs.erros());
        verify(nbsService, times(1)).existeNbs("001234567", DATA);
        verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"), eq(null),
                eq("001234567"), eq(DATA), eq("CBS e IBS"), any());
    }

    @Test
    void ncmZeroNaoPermitidoPreservaLiteraisEValoresQuaseCorrespondentes() {
        stubClassificacao(1);
        for (String recebido : List.of("00000000")) {
            when(ncmService.existeNcm(recebido, DATA)).thenReturn(true);
            ObjetoSimplesDomain item = processador.processarItem(
                    new EntradaItemSimples(1, "07", "000001", "1000.00", null, recebido, null), 0,
                    contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), false), PAR_FIXO,
                    BASE_URL).objeto();
            assertNull(item.erros());
            verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"),
                    eq(recebido), eq(null), eq(DATA), eq("CBS e IBS"), any());
        }
        for (String recebido : List.of("00", "000000000", " 00 ")) {
            ObjetoSimplesDomain item = processador.processarItem(
                    new EntradaItemSimples(1, "07", "000001", "1000.00", null, recebido, null), 0,
                    contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), false), PAR_FIXO,
                    BASE_URL).objeto();
            assertEquals("VSN-051", item.erros().get(0).code());
        }
    }

    @Test
    void conversaoDeNcmZeroAindaPodeExigirNomenclatura() {
        stubClassificacao(1);
        doThrow(new NomenclaturaObrigatoriaNaoInformadaException("000001", "NFSe"))
                .when(validacaoNomenclaturaService).validar(any(), any(), any(), any(), any(), any(), any(), any());
        ObjetoSimplesDomain item = processador.processarItem(
                new EntradaItemSimples(1, "07", "000001", "1000.00", null, "00", null), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), true), PAR_FIXO,
                BASE_URL).objeto();
        assertEquals("VSN-044", item.erros().get(0).code());
        verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"), eq(null),
                eq(null), eq(DATA), eq("CBS e IBS"), any());
    }

    @Test
    void valoresAusentesWhitespaceNbsEZerosNbsNaoSaoNormalizados() {
        stubClassificacao(1);
        when(nbsService.existeNbs("000000000", DATA)).thenReturn(true);
        for (String ncm : Arrays.asList(null, "")) {
            ObjetoSimplesDomain item = processador.processarItem(
                    new EntradaItemSimples(1, "07", "000001", "1000.00", null, ncm, "000000000"), 0,
                    contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), true), PAR_FIXO,
                    BASE_URL).objeto();
            assertNull(item.erros());
            verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"), eq(ncm),
                    eq("000000000"), eq(DATA), eq("CBS e IBS"), any());
        }
        ObjetoSimplesDomain whitespace = processador.processarItem(
                new EntradaItemSimples(1, "07", "000001", "1000.00", null, null, "         "), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA), true), PAR_FIXO,
                BASE_URL).objeto();
        assertEquals("VSN-051", whitespace.erros().get(0).code());
    }

    @Test
    void nomenclaturaIncompativel_apontaCampoInformadoEPreservaMensagem() {
        stubClassificacao(1);
        when(nbsService.existeNbs("114051000", DATA)).thenReturn(true);
        doThrow(new NomenclaturaIncompativelTipoDfeException("NBS", "000001", "NFSe"))
                .when(validacaoNomenclaturaService).validar(any(), any(), any(), any(), any(), any(), any(), any());

        ObjetoSimplesDomain item = processarNomenclatura(null, "114051000");

        assertEquals("VSN-045", item.erros().get(0).code());
         assertEquals("#/itens/0/nbs", item.erros().get(0).pointer());
        assertEquals("NBS não permitida para a Classificação Tributária de código 000001 no tipo de documento NFSe",
                item.erros().get(0).detail());
    }

    @Test
    void prioridadeVsn027_precedeEstruturaNomenclatura() {
        when(classificacaoRepository.contarTratamentosNaoImplementados("000001", DATA)).thenReturn(1L);
        ObjetoSimplesDomain item = processador.processarItem(
                new EntradaItemSimples(1, "07", "000001", "1000.00", null, "22030000", "114051000"), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)), PAR_FIXO, BASE_URL).objeto();
        assertEquals("VSN-027", item.erros().get(0).code());
        verifyNoInteractions(ncmService, nbsService, validacaoNomenclaturaService);
    }

    @Test
    void baseEIsValidamAntesDaEstruturaNomenclatura() {
        stubClassificacao(1);
        ObjetoSimplesDomain item = processador.processarItem(
                new EntradaItemSimples(1, "07", "000001", null, null, "22030000", "114051000"), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)), PAR_FIXO, BASE_URL).objeto();
        assertEquals("VSN-001", item.erros().get(0).code());
        assertEquals("VSN-041", item.erros().get(1).code());
    }

    @Test
    void erroAnteriorSuprimeObrigatoriedadeDeNomenclaturaAusente() {
        stubClassificacao(1);

        ObjetoSimplesDomain item = processador.processarItem(
                new EntradaItemSimples(1, "07", "000001", null, null, null, null), 0,
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)), PAR_FIXO, BASE_URL).objeto();

        assertEquals("VSN-001", item.erros().get(0).code());
        assertEquals(1, item.erros().size());
        verifyNoInteractions(validacaoNomenclaturaService);
    }

    @Test
    void falhaTecnicaEstrutural_ficaNoItemSemInterromperProcessamento() {
        stubClassificacao(1);
        when(ncmService.existeNcm("22030000", DATA)).thenThrow(new IllegalStateException("banco indisponível"));
        List<ProcessadorItemSimples.ItemProcessadoSimples> resultados = processador.processarItens(List.of(
                new ItemAProcessar(new EntradaItemSimples(1, "07", "000001", "1000.00", null,
                        "22030000", null), PAR_FIXO),
                new ItemAProcessar(new EntradaItemSimples(2, "07", "000001", "1000.00", null,
                        null, null), PAR_FIXO)),
                contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)), BASE_URL);
        assertEquals(EstadoItemSimplesEnum.FALHA_TECNICA, resultados.get(0).objeto().estadoItem());
        assertEquals("VSN-999", resultados.get(0).objeto().erros().get(0).code());
        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, resultados.get(1).objeto().estadoItem());
    }

    @Test
    void nfse_impostoSeletivoDiferenteDeZero_vsn035() {
        stubClassificacao(1);
        ObjetoSimplesDomain objeto = processar("07", "10.00", regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO));
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertEquals("VSN-035", objeto.erros().get(0).code());
        assertEquals("#/itens/0/valorImpostoSeletivo", objeto.erros().get(0).pointer());
    }

    @Test
    void nfse_impostoSeletivoAusenteOuZero_aceito() {
        stubClassificacao(1);
        when(atividadeRepository.buscarVigentesPorCAtivSN(eq("07"), any()))
                .thenReturn(List.of(Teste_EscolhaParAliquotasService.atividade("07")));
        when(atividadeRepository.contarVinculosComTpDoc(anyLong(), eq(91), any())).thenReturn(1L);
        for (String vIs : Arrays.asList(null, "0.00")) {
            ObjetoSimplesDomain objeto = processar("07", vIs, regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO));
            assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem(), "vIS " + vIs);
            assertNull(objeto.tribCalc().IS());
            assertValor("23.90", objeto.tribCalc().vCBSSN());
        }
    }

    @Test
    void cAtivSNDeUmDigito_completadoComZero() {
        stubClassificacao(1);
        when(atividadeRepository.buscarVigentesPorCAtivSN(eq("07"), any()))
                .thenReturn(List.of(Teste_EscolhaParAliquotasService.atividade("07")));
        when(atividadeRepository.contarVinculosComTpDoc(anyLong(), eq(91), any())).thenReturn(1L);
        ObjetoSimplesDomain objeto = processar("7", null, regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO));
        assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem());
        verify(atividadeRepository).buscarVigentesPorCAtivSN(eq("07"), any());
        assertEquals("07", ProcessadorItemSimples.normalizarCAtivSN(" 7 ", true));
        // O clássico não normaliza.
        assertEquals("7", ProcessadorItemSimples.normalizarCAtivSN("7", false));
    }

    @Test
    void somenteReceita_naoConsultaAtividadeNemAliquotas() {
        stubClassificacao(1);
        ResolvedorAliquotasItem semChamada = (dados, collector) -> {
            throw new AssertionError("o resolvedor não deve ser chamado no SOMENTE_RECEITA");
        };
        ObjetoSimplesDomain objeto = processador.processarItem(new EntradaItemSimples(1, "07", "000001", "1000.00",
                null, null, null), 0, contextoNfse(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)), semChamada, BASE_URL)
                .objeto();
        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, objeto.estadoItem());
        assertValor("1000.00", objeto.tribCalc().vRBSNitem());
        verifyNoInteractions(atividadeRepository);
    }

    // ─── Um item na NFS-e ──────────────────────────────────────────────────────

    @Test
    void nfse_maisDeUmItem_vsn034() {
        TipoDfeCalculoSimples nfse = new TipoDfeCalculoSimples();
        nfse.setVocabularioRegime(VocabularioRegimeSimplesEnum.NFSE);
        nfse.setTipoDfeId(910L);
        nfse.setMaximoItens(1);
        nfse.setCasasAliquotaCalculo(2);
        nfse.setAdmiteImpostoSeletivo(false);
        when(parametroService.buscarTipoDfeCalculo(91, DATA)).thenReturn(Optional.of(nfse));
        CalculoSimplesUnificadoService unificado = new CalculoSimplesUnificadoService(parametroService, processador,
                new EscolhaParAliquotasService(), tipoDfeRepository);
        ItemSimplesUnificadoInput item = new ItemSimplesUnificadoInput(1, "07", "000001", null, null, "1000.00", null,
                "1", "1", null);
        DadosOperacaoSimples operacao = new DadosOperacaoSimples("op", "3.0.0", DATA.toString(), "91", null, null, "3",
                "1", "1", null, List.of(item, item));
        ErrosCalculoException erro = assertThrows(ErrosCalculoException.class,
                () -> unificado.processar(operacao, null, BASE_URL));
        assertEquals("VSN-034", erro.getErros().get(0).code());
        assertEquals("#/itens", erro.getErros().get(0).pointer());
    }

    @Test
    void naoAssistido_resolveSiglaDaLinhaDeDocumentoSelecionada() {
        TipoDfeCalculoSimples nfse = new TipoDfeCalculoSimples();
        nfse.setTipoDfeId(910L);
        nfse.setVocabularioRegime(VocabularioRegimeSimplesEnum.NFSE);
        nfse.setMaximoItens(1);
        nfse.setCasasAliquotaCalculo(2);
        nfse.setAdmiteImpostoSeletivo(false);
        when(parametroService.buscarTipoDfeCalculo(91, DATA)).thenReturn(Optional.of(nfse));
        when(tipoDfeRepository.buscarSiglaVigentePorId(910L, DATA)).thenReturn(Optional.of("NFSe"));
        when(tipoDfeRepository.permiteNcmZeroVigentePorId(910L, DATA)).thenReturn(Optional.of(true));
        when(parametroService.buscarRegimeApuracao(eq(VocabularioRegimeSimplesEnum.NFSE), any(), any(), eq(3),
                eq(1), eq(DATA)))
                .thenReturn(Optional.of(regime(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA)));
        stubClassificacao(1);
        CalculoSimplesUnificadoService unificado = new CalculoSimplesUnificadoService(parametroService, processador,
                new EscolhaParAliquotasService(), tipoDfeRepository);
        ItemSimplesUnificadoInput item = new ItemSimplesUnificadoInput(1, "07", "000001", null, null, "1000.00", null,
                null, null, null);
        DadosOperacaoSimples operacao = new DadosOperacaoSimples("op", "3.0.0", DATA.toString(), "91", null, null,
                "3", "1", "1", null, List.of(item));

        unificado.processar(operacao, null, BASE_URL);

        verify(tipoDfeRepository).buscarSiglaVigentePorId(910L, DATA);
        verify(tipoDfeRepository).permiteNcmZeroVigentePorId(910L, DATA);
        verify(validacaoNomenclaturaService).validar(eq(SiglasDFeEnum.NFSE), eq(1L), eq("000001"), eq(null), eq(null),
                eq(DATA), eq("CBS e IBS"), any());
    }

    @Test
    void contextoClassico_comportamentoDeSempre() {
        ContextoItemSimples classico = ContextoItemSimples.classico(DATA, 91,
                regime(CodigoRegimeApuracaoSimplesEnum.SN_PLENO), null, null, "NFSe", false);
        assertNull(classico.vocabularioTipoReceita());
        assertNull(classico.casasAliquotaCalculo());
        assertEquals(true, classico.admiteImpostoSeletivo());
        assertEquals(false, classico.normalizarCAtivSN());
        verifyNoInteractions(parametroService);
    }
}
