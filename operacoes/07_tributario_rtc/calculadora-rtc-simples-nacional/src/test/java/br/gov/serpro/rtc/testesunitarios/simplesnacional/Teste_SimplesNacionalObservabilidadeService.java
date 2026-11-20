package br.gov.serpro.rtc.testesunitarios.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoDfeCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.EstadoItemSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegimeCalculoSimplesEnum;
import br.gov.serpro.rtc.shared.exceptionhandler.ErrosCalculoException;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import br.gov.serpro.rtc.api.simplesnacional.model.input.ItemSimplesInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.OperacaoSimplesInput;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObjetoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObservabilidadeSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.repository.AtividadeSimplesCalculoRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TipoDfeCalculoSimplesRepository;
import br.gov.serpro.rtc.domain.service.ValidacaoNomenclaturaService;
import br.gov.serpro.rtc.domain.service.NcmService;
import br.gov.serpro.rtc.domain.service.NbsService;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.ParametroCalculoSimplesService;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalObservabilidadeService;
import br.gov.serpro.rtc.api.simplesnacional.adapter.ValidacaoNomenclaturaAdapter;

/**
 * Testes unitários do serviço de observabilidade do Simples Nacional — casos
 * dourados de estado e validação da NT 2026.005 (escopo global, resolução do
 * tipo de receita bruta, validação do código de atividade, compra governamental
 * e alíquotas condicionais), com os repositórios de catálogo e a fachada de
 * parâmetros mockados.
 *
 * <p>A fachada de parâmetros devolve, por padrão, as mesmas linhas da carga
 * oficial das tabelas de parametrização (fixtures compartilhadas com
 * {@link Teste_SimplesNacionalCalculoService}); os testes de erro de
 * parametrização sobrescrevem os stubs pontualmente.
 *
 * <p>A validação do cAtivSN segue a semântica de WHITELIST (mundo fechado) da
 * associativa {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}: a atividade é
 * admissível no tpDoc da operação se e somente se existe vínculo vigente entre
 * a atividade e o tpDoc; sem vínculo vigente algum, não é admissível em
 * documento nenhum.
 */
@ExtendWith(MockitoExtension.class)
class Teste_SimplesNacionalObservabilidadeService {

    private static final String BASE_URL = "http://localhost/api";
    private static final String DATA_PADRAO = "2027-03-15";
    private static final LocalDate PRIMEIRA_VIGENCIA_COMPRA_GOV = LocalDate.of(2027, 1, 1);

    @Mock
    private ClassificacaoTributariaSimplesRepository classificacaoRepository;

    @Mock
    private AtividadeSimplesCalculoRepository atividadeCalculoRepository;

    @Mock
    private ParametroCalculoSimplesService parametroService;

    @Mock private ValidacaoNomenclaturaService validacaoNomenclaturaService;
    @Mock private TipoDfeCalculoSimplesRepository tipoDfeRepository;
    @Mock private NcmService ncmService;
    @Mock private NbsService nbsService;

    private SimplesNacionalObservabilidadeService service;

    @BeforeEach
    void setup() {
        service = new SimplesNacionalObservabilidadeService(
                new SimplesNacionalCalculoService(), parametroService,
                classificacaoRepository, atividadeCalculoRepository,
                new ValidacaoNomenclaturaAdapter(ncmService, nbsService, validacaoNomenclaturaService),
                tipoDfeRepository);

        // Comportamento padrão da fachada de parâmetros: a carga oficial.
        // (lenient: nem todo teste exercita todos os stubs.)
        lenient().when(parametroService.tpDocSuportado(anyInt(), any()))
                .thenAnswer(inv -> {
                    int tpDoc = inv.getArgument(0);
                    return tpDoc == 55 || tpDoc == 65;
                });
        lenient().when(parametroService.buscarTipoDfeCalculo(anyInt(), any()))
                .thenAnswer(inv -> {
                    int tpDoc = inv.getArgument(0);
                    TipoDfeCalculoSimples documento = new TipoDfeCalculoSimples();
                    documento.setTipoDfeId((long) tpDoc);
                    return Optional.of(documento);
                });
        lenient().when(tipoDfeRepository.buscarSiglaVigentePorId(any(), any()))
                .thenAnswer(inv -> Optional.of(switch (((Long) inv.getArgument(0)).intValue()) {
                    case 65 -> "NFCe";
                    case 91 -> "NFSe";
                    default -> "NFe";
                }));
        lenient().when(parametroService.buscarTipoReceitaBruta(anyInt(), any()))
                .thenAnswer(inv -> Optional.of(Teste_SimplesNacionalCalculoService.tipoReceita(inv.getArgument(0))));
        lenient().when(parametroService.buscarRegimeApuracao(any(), any()))
                .thenAnswer(inv -> Optional.of(switch ((RegimeCalculoSimplesEnum) inv.getArgument(0)) {
                    case CRT_1 -> Teste_SimplesNacionalCalculoService.regimeCrt1();
                    case CRT_2 -> Teste_SimplesNacionalCalculoService.regimeCrt2();
                    case CRT_3_TPREGIME_2 -> Teste_SimplesNacionalCalculoService.regimeCrt3TpRegime2();
                }));
        lenient().when(parametroService.buscarFatorTransicaoCompraGov(any()))
                .thenAnswer(inv -> {
                    LocalDate data = inv.getArgument(0);
                    return data.isBefore(PRIMEIRA_VIGENCIA_COMPRA_GOV)
                            ? Optional.empty()
                            : Optional.of(new BigDecimal("0.00"));
                });
    }

    // ─── Fábricas de entrada e stubs ───────────────────────────────────────────

    private OperacaoSimplesInput operacao(String data, String tpDoc, String crt, String tpRegime,
            ItemSimplesInput... itens) {
        return operacaoCompraGov(data, tpDoc, crt, tpRegime, null, itens);
    }

    /**
     * Operação com compra governamental: o tpEnteGov é dado GLOBAL da operação
     * (espelha a tag gCompraGov/tpEnteGov da identificação da nota — grupo
     * B/ide do leiaute do DF-e), nunca do item.
     */
    private OperacaoSimplesInput operacaoCompraGov(String data, String tpDoc, String crt, String tpRegime,
            String tpEnteGov, ItemSimplesInput... itens) {
        return new OperacaoSimplesInput("507f1f77bcf86cd799439011", "2.0.0", data, tpDoc, crt, tpRegime,
                tpEnteGov, List.of(itens));
    }

    private ItemSimplesInput item(Integer numero, String cAtivSN, String cClassTrib,
            String pIbs, String pCbs) {
        return new ItemSimplesInput(numero, cAtivSN, cClassTrib, null, null, "1000.00", "0.00", pIbs, pCbs);
    }

    private ItemSimplesInput itemPadrao(String cAtivSN, String cClassTrib) {
        return item(1, cAtivSN, cClassTrib, "1.25", "2.75");
    }

    private void stubClassTrib(String codigo, int tpRBSN) {
        ClassificacaoTributariaSimples classificacao = new ClassificacaoTributariaSimples();
        classificacao.setId(1L);
        classificacao.setCodigo(codigo);
        classificacao.setTpRBSN(tpRBSN);
        when(classificacaoRepository.buscarVigentes(eq(codigo), any())).thenReturn(List.of(classificacao));
        when(classificacaoRepository.contarVinculosComTpDoc(eq(codigo), anyInt(), any())).thenReturn(1L);
    }

    /**
     * Simula uma atividade vigente com o código informado.
     * {@code vinculosComTpDoc} reproduz a semântica de whitelist da associativa
     * de tipos de DFE por atividade: 0 vínculos vigentes com o tpDoc da
     * operação = atividade inadmissível no documento (NAO_CALCULADO).
     */
    private void stubAtividade(String cAtivSN, long atividadeId, long vinculosComTpDoc,
            boolean naoTributavel) {
        AtividadeSimplesNacional atividade = new AtividadeSimplesNacional();
        atividade.setId(atividadeId);
        atividade.setCAtivSN(cAtivSN);
        atividade.setNaoTributavel(naoTributavel);
        when(atividadeCalculoRepository.buscarVigentesPorCAtivSN(eq(cAtivSN), any()))
                .thenReturn(List.of(atividade));
        when(atividadeCalculoRepository.contarVinculosComTpDoc(eq(atividadeId), anyInt(), any()))
                .thenReturn(vinculosComTpDoc);
    }

    /** Simula uma atividade com vínculo vigente para o tpDoc da operação (calcula). */
    private void stubAtividadeComVinculo(String cAtivSN, long atividadeId) {
        stubAtividade(cAtivSN, atividadeId, 1L, false);
    }

    /** Simula código sem linha vigente no catálogo de atividades. */
    private void stubAtividadeInexistente(String cAtivSN) {
        when(atividadeCalculoRepository.buscarVigentesPorCAtivSN(eq(cAtivSN), any())).thenReturn(List.of());
    }

    private ObjetoSimplesDomain unicoObjeto(ObservabilidadeSimplesDomain resultado) {
        assertEquals(1, resultado.objetos().size());
        return resultado.objetos().get(0);
    }

    private List<String> codigosDosErros(List<ErroDetalhe> erros) {
        return erros.stream().map(ErroDetalhe::code).toList();
    }

    private List<String> codigosDoErroGlobal(OperacaoSimplesInput operacao) {
        ErrosCalculoException ex = assertThrows(ErrosCalculoException.class,
                () -> service.processarOperacao(operacao, BASE_URL));
        return codigosDosErros(ex.getErros());
    }

    // ─── cAtivSN ausente/inválido → NAO_CALCULADO (não é erro) ─────────────────

    @Test
    void caso18_cAtivSNAusente_naoCalculadoEContribuiSoParaVRBSNtot() {
        stubClassTrib("000001", 1);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao(null, "000001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, objeto.estadoItem());
        assertNotNull(objeto.tribCalc());
        assertEquals(0, new BigDecimal("1000.00").compareTo(objeto.tribCalc().vRBSNitem()));
        assertEquals(1, objeto.tribCalc().tpRBSN());
        assertEquals("Receita bruta - interna", objeto.tribCalc().tpRBSNDescricao());
        assertNull(objeto.tribCalc().vIBSSN());
        assertNull(objeto.tribCalc().vCBSSN());
        // Contribui apenas para vRBSNtot: os demais totais ficam ausentes.
        assertNotNull(resultado.total());
        assertEquals(0, new BigDecimal("1000.00").compareTo(resultado.total().vRBSNtot()));
        assertNull(resultado.total().vIBSSNtot());
        assertNull(resultado.total().vCBSSNtot());
        assertNull(resultado.total().totalIs());
        assertNull(resultado.total().totalTributos());
    }

    @Test
    void caso19_cAtivSNSemLinhaNoCatalogo_naoCalculado() {
        // Código '99' não possui linha vigente no catálogo de atividades.
        stubClassTrib("000001", 1);
        stubAtividadeInexistente("99");

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("99", "000001")), BASE_URL);

        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, unicoObjeto(resultado).estadoItem());
    }

    @Test
    void caso20_atividadeComVinculoParaOTpDoc_calculadoNormalmente() {
        // Whitelist: atividade com vínculo vigente para o tpDoc da operação —
        // o item CALCULA normalmente.
        stubClassTrib("000001", 1);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "000001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem());
        assertEquals(0, new BigDecimal("12.50").compareTo(objeto.tribCalc().vIBSSN()));
        assertEquals(0, new BigDecimal("27.50").compareTo(objeto.tribCalc().vCBSSN()));
    }

    // ─── Whitelist (mundo fechado) da associativa de tipos de DFE ──────────────

    @Test
    void atividadeSemVinculoAlgum_naoCalculadaEmTpDocNenhum() {
        // Whitelist: atividade sem vínculo vigente algum na associativa não é
        // admissível em documento nenhum — o item resulta em NAO_CALCULADO
        // (sem erro), em qualquer tpDoc habilitado.
        stubClassTrib("000001", 1);
        stubAtividade("05", 5L, 0L, false);

        for (String tpDoc : List.of("55", "65")) {
            ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                    operacao(DATA_PADRAO, tpDoc, "1", null, itemPadrao("05", "000001")), BASE_URL);

            assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, unicoObjeto(resultado).estadoItem(),
                    () -> "cAtivSN 05 sem vínculo deveria resultar NAO_CALCULADO em tpDoc " + tpDoc);
        }
    }

    @Test
    void atividadeVinculadaApenasAOutroTpDoc_naoCalculado() {
        // Atividade com vínculos na associativa, mas nenhum com o tpDoc da
        // operação: inadmissível — NAO_CALCULADO, sem erro.
        stubClassTrib("000001", 1);
        stubAtividade("12", 12L, 0L, false);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("12", "000001")), BASE_URL);

        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, unicoObjeto(resultado).estadoItem());
    }

    @Test
    void atividadeVinculadaAoTpDocDaOperacao_calculado() {
        // O inverso: com vínculo vigente para o tpDoc da operação, calcula.
        stubClassTrib("000001", 1);
        stubAtividade("01", 1L, 1L, false);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "000001")), BASE_URL);

        assertEquals(EstadoItemSimplesEnum.CALCULADO, unicoObjeto(resultado).estadoItem());
    }

    @Test
    void vinculoComVigenciaEncerrada_ehIgnorado_atividadeFicaInadmissivel() {
        // Vínculo com fim de vigência anterior à data da operação é filtrado
        // pela cláusula de vigência da consulta: a contagem de vínculos VIGENTES
        // com o tpDoc retorna zero e a atividade fica inadmissível no documento
        // (whitelist: sem vínculo vigente, não há permissão).
        stubClassTrib("000001", 1);
        stubAtividade("01", 1L, 0L, false);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "000001")), BASE_URL);

        assertEquals(EstadoItemSimplesEnum.NAO_CALCULADO, unicoObjeto(resultado).estadoItem());
    }

    // ─── Atividade não tributável × tipo de receita (regra UB14-90 da NT) ──────

    @Test
    void caso21_atividadeNaoTributavelComReceitaTributavel_erroDeItem() {
        // Atividade marcada como não tributável (código 90) com cClassTrib de
        // tpRBSN=1 (declara receita): contradição — erro VSN-023.
        stubClassTrib("000001", 1);
        stubAtividade("90", 15L, 1L, true);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("90", "000001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertTrue(codigosDosErros(objeto.erros()).contains("VSN-023"));
        assertNull(objeto.tribCalc());
        assertNull(resultado.total());
    }

    @Test
    void atividadeNaoTributavelComTpRBSN0_calculadoComTudoZero() {
        // O 90 como linha do catálogo não quebra o fluxo: com tpRBSN=0 (não é
        // receita bruta, não vedado para não tributáveis) o item CALCULA, com
        // vRBSNitem=0 e tributos zero.
        stubClassTrib("410001", 0);
        stubAtividade("90", 15L, 1L, true);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("90", "410001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem());
        assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().vRBSNitem()));
        assertEquals(0, objeto.tribCalc().tpRBSN());
        assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().vIBSSN()));
        assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().vCBSSN()));
    }

    // ─── cClassTrib sem tratamento implementado → NAO_IMPLEMENTADO (prioridade) ─
    // (indicador de ajuste TRTR_IN_POSSUI_AJUSTE do tratamento tributário vigente:
    // mesma semântica do CAL-007 do regime geral; primeira consulta do item)

    /** Simula cClassTrib vinculado, na data, a tratamento marcado como "sem implementação". */
    private void stubClassTribNaoImplementado(String codigo) {
        when(classificacaoRepository.contarTratamentosNaoImplementados(eq(codigo), any())).thenReturn(1L);
    }

    @Test
    void cClassTribNaoImplementado_estadoNaoImplementadoSemCalculoNemTotal() {
        stubClassTribNaoImplementado("620001");

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "620001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.NAO_IMPLEMENTADO, objeto.estadoItem());
        assertEquals(List.of("VSN-027"), codigosDosErros(objeto.erros()));
        ErroDetalhe erro = objeto.erros().get(0);
        assertEquals("#/itens/0/cClassTrib", erro.pointer());
        assertEquals(BASE_URL + "/calculadora/observabilidade/simples-nacional/erros?codigo=VSN-027", erro.type());
        assertTrue(erro.detail().contains("620001"));
        assertNull(objeto.tribCalc());
        // Fora de todos os totais (como item com erro).
        assertNull(resultado.total());
    }

    @Test
    void cClassTribNaoImplementado_ehAPrimeiraConsultaEEncerraOItem() {
        // Prioridade: com o indicador presente, nenhuma outra consulta de
        // catálogo é feita para o item (tpRBSN, tpDoc, atividade) e nenhuma
        // outra validação roda — base negativa e alíquotas ausentes NÃO são
        // reportadas.
        stubClassTribNaoImplementado("800001");

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null,
                        new ItemSimplesInput(1, "01", "800001", null, null, "-10.00", "0.00", null, null)), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.NAO_IMPLEMENTADO, objeto.estadoItem());
        assertEquals(List.of("VSN-027"), codigosDosErros(objeto.erros()));

        verify(classificacaoRepository).contarTratamentosNaoImplementados(eq("800001"), any());
        verify(classificacaoRepository, never()).buscarVigentes(any(), any());
        verify(classificacaoRepository, never()).contarVinculosComTpDoc(any(), anyInt(), any());
        verifyNoInteractions(atividadeCalculoRepository);
    }

    @Test
    void cClassTribNaoImplementado_prevaleceSobreNumeroAusente() {
        // O número do item é validado antes (não é consulta de banco); a falta
        // de implementação ainda assim define o estado do item.
        stubClassTribNaoImplementado("620001");

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, item(null, "01", "620001", "1.25", "2.75")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(1, objeto.nObj());
        assertEquals(EstadoItemSimplesEnum.NAO_IMPLEMENTADO, objeto.estadoItem());
        assertEquals(List.of("VSN-010", "VSN-027"), codigosDosErros(objeto.erros()));
    }

    @Test
    void cClassTribNaoImplementado_naoContaminaOsDemaisItensNemOsTotais() {
        stubClassTribNaoImplementado("620001");
        // Stub explícito (contagem zero) para o código implementado: o mock é
        // estrito e a mesma consulta é feita, com argumento distinto, pelo item 2.
        when(classificacaoRepository.contarTratamentosNaoImplementados(eq("000001"), any())).thenReturn(0L);
        stubClassTrib("000001", 1);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null,
                        item(1, "01", "620001", "1.25", "2.75"),
                        item(2, "01", "000001", "1.25", "2.75")), BASE_URL);

        assertEquals(2, resultado.objetos().size());
        assertEquals(EstadoItemSimplesEnum.NAO_IMPLEMENTADO, resultado.objetos().get(0).estadoItem());
        assertEquals(EstadoItemSimplesEnum.CALCULADO, resultado.objetos().get(1).estadoItem());
        // Totais apenas do item calculado.
        assertNotNull(resultado.total());
        assertEquals(0, new BigDecimal("1000.00").compareTo(resultado.total().vRBSNtot()));
        assertEquals(0, new BigDecimal("12.50").compareTo(resultado.total().vIBSSNtot()));
        assertEquals(0, new BigDecimal("27.50").compareTo(resultado.total().vCBSSNtot()));
    }

    @Test
    void cClassTribImplementado_segueOFluxoNormal() {
        // Indicador ausente (contagem zero, padrão do mock): o item calcula.
        stubClassTrib("000001", 1);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "000001")), BASE_URL);

        assertEquals(EstadoItemSimplesEnum.CALCULADO, unicoObjeto(resultado).estadoItem());
        verify(classificacaoRepository).contarTratamentosNaoImplementados(eq("000001"), any());
        verify(tipoDfeRepository).buscarSiglaVigentePorId(55L, LocalDate.parse(DATA_PADRAO));
        verify(validacaoNomenclaturaService).validar(eq(br.gov.serpro.rtc.domain.model.enumeration.SiglasDFeEnum.NFE),
                eq(1L), eq("000001"), eq(null), eq(null), eq(LocalDate.parse(DATA_PADRAO)), eq("CBS e IBS"), any());
    }

    @Test
    void cClassTribAusente_naoConsultaImplementacao() {
        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", " ")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertTrue(codigosDosErros(objeto.erros()).contains("VSN-020"));
        verify(classificacaoRepository, never()).contarTratamentosNaoImplementados(any(), any());
    }

    // ─── cClassTrib inexistente/incompatível/ausente ───────────────────────────

    @Test
    void caso22_cClassTribDesconhecido_erroDeItem() {
        when(classificacaoRepository.buscarVigentes(eq("999999"), any())).thenReturn(List.of());

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "999999")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertTrue(codigosDosErros(objeto.erros()).contains("VSN-021"));
    }

    @Test
    void cClassTribIncompativelComTpDoc_erroDeItem() {
        ClassificacaoTributariaSimples classificacao = new ClassificacaoTributariaSimples();
        classificacao.setTpRBSN(5);
        when(classificacaoRepository.buscarVigentes(eq("222001"), any())).thenReturn(List.of(classificacao));
        when(classificacaoRepository.contarVinculosComTpDoc(eq("222001"), anyInt(), any())).thenReturn(0L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "222001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertTrue(codigosDosErros(objeto.erros()).contains("VSN-022"));
    }

    @Test
    void cClassTribAusente_erroDeItem() {
        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", null)), BASE_URL);

        assertTrue(codigosDosErros(unicoObjeto(resultado).erros()).contains("VSN-020"));
    }

    // ─── Falha de parametrização (VSN-026) ─────────────────────────────────────

    @Test
    void tipoReceitaBrutaSemLinhaVigente_erroDeItemVsn026() {
        // O cClassTrib resolve o código tpRBSN, mas não há linha de efeito
        // vigente na tabela de tipos de receita bruta: falha de parametrização.
        stubClassTrib("000001", 1);
        doReturn(Optional.empty()).when(parametroService).buscarTipoReceitaBruta(eq(1), any());

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "000001")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertTrue(codigosDosErros(objeto.erros()).contains("VSN-026"));
    }

    @Test
    void regimeSemLinhaVigente_erroGlobalVsn026() {
        // Combinação aprovada pela validação de escopo, mas sem linha vigente na
        // matriz de regime: falha de parametrização, global (HTTP 422).
        // (doReturn evita executar o Answer padrão do setup durante o restub.)
        doReturn(Optional.empty()).when(parametroService).buscarRegimeApuracao(any(), any());

        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", "1", null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-026"));
    }

    // ─── Escopo global → HTTP 422 ──────────────────────────────────────────────

    @Test
    void caso23_tpRegimeIbsCbs1_regimeRegularForaDeEscopo() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", "1", "1", itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-019"));
    }

    @Test
    void caso24_crt4_meiForaDeEscopo() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", "4", null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-019"));
    }

    @Test
    void caso25_crt1ComTpRegime2_incompativel() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", "1", "2", itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-018"));
    }

    @Test
    void caso26_crt3SemTpRegime_regimeNormalPuroForaDeEscopo() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", "3", null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-019"));
    }

    @Test
    void caso27_tpDoc91_naoSuportado() {
        // O suporte de tpDoc vem da tabela de documentos do cálculo; o stub da
        // fachada reproduz a carga atual (apenas 55 e 65 vinculados).
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "91", "1", null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-015"));
    }

    @Test
    void tpDocSemTratamento_rejeitadoGlobalmente() {
        when(parametroService.tpDocSemTratamentoCalculadora(anyInt(), any())).thenAnswer(inv -> {
            int tpDoc = inv.getArgument(0);
            return tpDoc == 77 || tpDoc == 94 || tpDoc == 95 || tpDoc == 96;
        });

        for (String tpDoc : List.of("77", "94", "95", "96")) {
            List<String> codigos = codigosDoErroGlobal(
                    operacao(DATA_PADRAO, tpDoc, "1", null, itemPadrao("01", "000001")));
            assertTrue(codigos.contains("VSN-015"), "tpDoc " + tpDoc);
        }
    }

    @Test
    void caso28_tpDocAusente_erroGlobal() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, null, "1", null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-014"));
    }

    @Test
    void crtAusente_erroGlobal() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", null, null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-016"));
    }

    @Test
    void tpRegimeInvalido_erroGlobal() {
        List<String> codigos = codigosDoErroGlobal(
                operacao(DATA_PADRAO, "55", "3", "9", itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-017"));
    }

    @Test
    void dataInvalida_reportaApenasErroDeData_semVerificarSuporteDoTpDoc() {
        // Com a data inválida, o lookup de vigência do suporte do tpDoc não pode
        // ser executado e é omitido: apenas VSN-012 é reportado.
        List<String> codigos = codigosDoErroGlobal(
                operacao("2027-13-99", "55", "1", null, itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-012"));
        assertEquals(1, codigos.size());
    }

    // ─── Alíquotas obrigatórias condicionais ───────────────────────────────────

    @Test
    void caso29_tpRBSN1SemPCbssn_erroDeAliquotaExigida() {
        stubClassTrib("000001", 1);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, item(1, "01", "000001", "1.25", null)), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA, objeto.estadoItem());
        assertTrue(codigosDosErros(objeto.erros()).contains("VSN-004"));
    }

    @Test
    void caso30_tpRBSN2SemAliquotas_calculadoComTudoZero() {
        stubClassTrib("410008", 2);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "1", null, item(1, "01", "410008", null, null)), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem());
        assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().vIBSSN()));
        assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().vCBSSN()));
        assertNull(objeto.tribCalc().pIBSSN());
        assertNull(objeto.tribCalc().pCBSSN());
    }

    @Test
    void crt2_pIbssnNuncaExigida() {
        stubClassTrib("000001", 1);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(
                operacao(DATA_PADRAO, "55", "2", null, item(1, "01", "000001", null, "2.75")), BASE_URL);

        ObjetoSimplesDomain objeto = unicoObjeto(resultado);
        assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem());
        assertNull(objeto.tribCalc().pIBSSN());
        assertNull(objeto.tribCalc().vIBSSN());
        assertEquals(0, new BigDecimal("27.50").compareTo(objeto.tribCalc().vCBSSN()));
        // Totais de IBS suprimidos para regime que não gera IBS (CRT=2).
        assertNull(resultado.total().vIBSSNtot());
        assertNull(resultado.total().vIBSSNtotPendSusp());
    }

    // ─── Compra governamental: tpEnteGov é dado GLOBAL da operação ─────────────
    // (espelha o grupo gCompraGov da identificação da nota — grupo B/ide do
    // leiaute do DF-e: o ente adquirente não varia por item)

    @Test
    void caso31_compraGovComDataForaDasVigencias_erroGlobal() {
        // Data sem vigência na tabela de transição: como o tpEnteGov vale para a
        // operação inteira, a falha de parametrização é global (HTTP 422),
        // nunca fator zero implícito.
        List<String> codigos = codigosDoErroGlobal(operacaoCompraGov(
                "2025-06-01", "55", "1", null, "2", itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-025"));
    }

    @Test
    void tpEnteGovForaDoDominio_erroGlobal() {
        List<String> codigos = codigosDoErroGlobal(operacaoCompraGov(
                DATA_PADRAO, "55", "1", null, "5", itemPadrao("01", "000001")));
        assertTrue(codigos.contains("VSN-024"));
    }

    @Test
    void tpEnteGovDaOperacao_aplicadoATodosOsItens() {
        // União (tpEnteGov=1): pCBS' = pCBS + pIBS; pIBS' = 0 — a transformação
        // vem do contexto da operação e alcança TODOS os itens da nota.
        stubClassTrib("000001", 1);
        stubAtividadeComVinculo("01", 1L);

        ObservabilidadeSimplesDomain resultado = service.processarOperacao(operacaoCompraGov(
                DATA_PADRAO, "55", "1", null, "1",
                item(1, "01", "000001", "1.25", "2.75"),
                item(2, "01", "000001", "1.25", "2.75")), BASE_URL);

        assertEquals(2, resultado.objetos().size());
        for (ObjetoSimplesDomain objeto : resultado.objetos()) {
            assertEquals(EstadoItemSimplesEnum.CALCULADO, objeto.estadoItem());
            assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().pIBSSN()));
            assertEquals(0, BigDecimal.ZERO.compareTo(objeto.tribCalc().vIBSSN()));
            assertEquals(0, new BigDecimal("4.00").compareTo(objeto.tribCalc().pCBSSN()));
            assertEquals(0, new BigDecimal("40.00").compareTo(objeto.tribCalc().vCBSSN()));
        }
    }
}
