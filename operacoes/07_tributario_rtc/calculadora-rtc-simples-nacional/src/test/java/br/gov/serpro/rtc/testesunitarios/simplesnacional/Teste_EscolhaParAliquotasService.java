package br.gov.serpro.rtc.testesunitarios.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.gov.serpro.rtc.api.aliquotasimples.model.AliquotasAnexoSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.LinhaAliquotasSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.MotivoResolucaoAliquota;
import br.gov.serpro.rtc.api.aliquotasimples.model.OrigemLinhaAliquotasEnum;
import br.gov.serpro.rtc.api.aliquotasimples.model.ParAliquotasSimples;
import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.simplesnacional.collector.ItemCollectorSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.SituacaoIssParAliquotaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegraAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.TipoAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService.AvaliacaoIssItem;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService.CamposIssItem;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService.EscolhaParAliquotas;

/**
 * Testes unitários da escolha do par de alíquotas: cada uma das 9 regras, a
 * escolha do anexo pelo fator "r", o par "sem" nulo, a ordem de avaliação e a
 * avaliação dos campos do ISS. As situações do ISS espelham a carga da V0067.
 */
class Teste_EscolhaParAliquotasService {

    private final EscolhaParAliquotasService escolha = new EscolhaParAliquotasService();

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    /** Situações do ISS da carga oficial (V0067, SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL). */
    static List<SituacaoIssParAliquotaSimples> situacoesDaCarga() {
        return List.of(
                situacao(1, "CRT", 2, RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, 4),
                situacao(2, "regApIBSCBSSN", 2, RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, 4),
                situacao(3, "regApTribSN", 2, RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, 4),
                situacao(4, "regApTribSN", 3, RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, 4),
                situacao(5, "tribISSQN", 2, RegraAliquotaSimplesEnum.ISS_NAO_INCIDE, 6),
                situacao(6, "tribISSQN", 3, RegraAliquotaSimplesEnum.ISS_NAO_INCIDE, 6),
                situacao(7, "tribISSQN", 4, RegraAliquotaSimplesEnum.ISS_NAO_INCIDE, 6),
                situacao(8, "tpBM", 1, RegraAliquotaSimplesEnum.ISENCAO_MUNICIPAL, 7),
                situacao(9, "tpRetISSQN", 2, RegraAliquotaSimplesEnum.ISS_RETIDO, 8),
                situacao(10, "tpRetISSQN", 3, RegraAliquotaSimplesEnum.ISS_RETIDO, 8));
    }

    private static SituacaoIssParAliquotaSimples situacao(long id, String campo, int valor,
            RegraAliquotaSimplesEnum regra, int ordem) {
        SituacaoIssParAliquotaSimples situacao = new SituacaoIssParAliquotaSimples();
        situacao.setId(id);
        situacao.setCampo(campo);
        situacao.setValor(valor);
        situacao.setRegra(regra);
        situacao.setTipoAliquota(TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO);
        situacao.setOrdem(ordem);
        situacao.setInicioVigencia(LocalDate.of(2027, 1, 1));
        return situacao;
    }

    /** Atividade do catálogo com os indicadores de ISS da V0067. */
    static AtividadeSimplesNacional atividade(String cAtivSN) {
        AtividadeSimplesNacional atividade = new AtividadeSimplesNacional();
        atividade.setId(Long.parseLong(cAtivSN));
        atividade.setCAtivSN(cAtivSN);
        atividade.setNaoTributavel("90".equals(cAtivSN));
        switch (cAtivSN) {
            case "01", "02" -> atividade.setAnexoSemFatorR("I");
            case "03", "04" -> atividade.setAnexoSemFatorR("II");
            case "13", "14" -> atividade.setAnexoSemFatorR("IV");
            case "09" -> {
                atividade.setAnexoSemFatorR("V");
                atividade.setAnexoComFatorR("III");
            }
            case "90" -> { }
            default -> atividade.setAnexoSemFatorR("III");
        }
        atividade.setIssNoSimples(List.of("07", "09", "10", "12", "13", "14").contains(cAtivSN));
        atividade.setAdmiteBeneficioMunicipal(List.of("12", "13").contains(cAtivSN));
        return atividade;
    }

    private static final ParAliquotasSimples PAR_I = par("1.1612", "0.0129");
    private static final ParAliquotasSimples PAR_III_COM = par("2.2154", "0.0257");
    private static final ParAliquotasSimples PAR_III_SEM = par("1.99", "0.0231");
    private static final ParAliquotasSimples PAR_IV_COM = par("2.0119", "0.022");
    private static final ParAliquotasSimples PAR_IV_SEM = par("1.85", "0.0202");
    private static final ParAliquotasSimples PAR_V_COM = par("3.2534", "0.0363");

    private static ParAliquotasSimples par(String cbs, String ibs) {
        return new ParAliquotasSimples(new BigDecimal(cbs), new BigDecimal(ibs));
    }

    /** Linha completa, com o par sem nos anexos III e IV e SEM o par sem no anexo V. */
    private static LinhaAliquotasSimples linha(Boolean fatorR, boolean impedimento) {
        return new LinhaAliquotasSimples(fatorR, impedimento,
                new AliquotasAnexoSimples(PAR_I, null),
                new AliquotasAnexoSimples(par("1.1184", "0.0121"), null),
                new AliquotasAnexoSimples(PAR_III_COM, PAR_III_SEM),
                new AliquotasAnexoSimples(PAR_IV_COM, PAR_IV_SEM),
                new AliquotasAnexoSimples(PAR_V_COM, null),
                OrigemLinhaAliquotasEnum.REQUISICAO);
    }

    private EscolhaParAliquotas escolher(String cAtivSN, Map<String, Integer> fatos) {
        return escolha.escolher(atividade(cAtivSN), true, linha(false, false), fatos, situacoesDaCarga());
    }

    private static void assertEscolha(EscolhaParAliquotas atual, String anexo, TipoAliquotaSimplesEnum tipo,
            RegraAliquotaSimplesEnum regra, ParAliquotasSimples par) {
        assertEquals(anexo, atual.anexo(), "anexo");
        assertEquals(tipo, atual.tipoAliquota(), "tipoAliquota");
        assertEquals(regra, atual.regra(), "regraAliquota");
        assertEquals(par, atual.par(), "par");
    }

    // ─── As 9 regras ───────────────────────────────────────────────────────────

    @Test
    void regra1_documento_atividadeNaoAdmitidaNoTpDoc() {
        EscolhaParAliquotas atual = escolha.escolher(atividade("07"), false, linha(false, false), Map.of(),
                situacoesDaCarga());
        assertEscolha(atual, null, null, RegraAliquotaSimplesEnum.DOCUMENTO, null);
        assertEquals(MotivoResolucaoAliquota.ATIVIDADE_INADMISSIVEL_NO_DOCUMENTO, atual.motivo());
    }

    @Test
    void regra2_atividadeNaoTributada_semAliquotaZero() {
        EscolhaParAliquotas atual = escolher("90", Map.of("tpRetISSQN", 2));
        assertEscolha(atual, null, TipoAliquotaSimplesEnum.SEM_ALIQUOTA,
                RegraAliquotaSimplesEnum.ATIVIDADE_NAO_TRIBUTADA, ParAliquotasSimples.ZERO);
        assertEquals(MotivoResolucaoAliquota.ATIVIDADE_SEM_ANEXO, atual.motivo());
    }

    @Test
    void regra3_anexoSemIss_parUnico_mesmoComFatosDoIss() {
        // CRT 2 decidiria a regra 4, mas os anexos I e II têm um só par.
        EscolhaParAliquotas atual = escolher("01", Map.of("CRT", 2));
        assertEscolha(atual, "I", TipoAliquotaSimplesEnum.PAR_UNICO, RegraAliquotaSimplesEnum.ANEXO_SEM_ISS, PAR_I);
        assertEquals(MotivoResolucaoAliquota.RESOLVIDA, atual.motivo());
        assertEquals("anexoI", atual.caminhoPar());
    }

    @Test
    void regra4_impedimentoPeloSublimite_vemDaLinha() {
        EscolhaParAliquotas atual = escolha.escolher(atividade("07"), true, linha(false, true), Map.of(),
                situacoesDaCarga());
        assertEscolha(atual, "III", TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO,
                RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, PAR_III_SEM);
        assertEquals("anexoIII/semRedistribuicaoISS", atual.caminhoPar());
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({ "CRT, 2", "regApIBSCBSSN, 2", "regApTribSN, 2", "regApTribSN, 3" })
    void regra4_issForaDoSimples_pelosCamposDoDocumento(String campo, int valor) {
        assertEscolha(escolher("07", Map.of(campo, valor)), "III", TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO,
                RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, PAR_III_SEM);
    }

    @ParameterizedTest(name = "atividade {0}")
    @CsvSource({ "05", "06", "08", "11" })
    void regra5_atividadeSemIssNoSimples(String cAtivSN) {
        assertEscolha(escolher(cAtivSN, Map.of()), "III", TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO,
                RegraAliquotaSimplesEnum.ATIVIDADE_SEM_ISS, PAR_III_SEM);
    }

    @ParameterizedTest(name = "tribISSQN = {0}")
    @CsvSource({ "2", "3", "4" })
    void regra6_issNaoIncide(int tribISSQN) {
        assertEscolha(escolher("14", Map.of("tribISSQN", tribISSQN)), "IV", TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO,
                RegraAliquotaSimplesEnum.ISS_NAO_INCIDE, PAR_IV_SEM);
    }

    @Test
    void regra7_isencaoMunicipal() {
        assertEscolha(escolher("12", Map.of("tribISSQN", 1, "tpRetISSQN", 1, "tpBM", 1)), "III",
                TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO, RegraAliquotaSimplesEnum.ISENCAO_MUNICIPAL, PAR_III_SEM);
    }

    @ParameterizedTest(name = "tpRetISSQN = {0}")
    @CsvSource({ "2", "3" })
    void regra8_issRetido(int tpRetISSQN) {
        assertEscolha(escolher("07", Map.of("tribISSQN", 1, "tpRetISSQN", tpRetISSQN)), "III",
                TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO, RegraAliquotaSimplesEnum.ISS_RETIDO, PAR_III_SEM);
    }

    @ParameterizedTest(name = "tpBM = {0}")
    @CsvSource({ "2", "3", "4" })
    void regra9_padrao_reducaoDaBaseEAliquotaDiferenciada(int tpBM) {
        assertEscolha(escolher("13", Map.of("tribISSQN", 1, "tpRetISSQN", 1, "tpBM", tpBM)), "IV",
                TipoAliquotaSimplesEnum.COM_REDISTRIBUICAO, RegraAliquotaSimplesEnum.PADRAO, PAR_IV_COM);
    }

    @Test
    void regra9_padrao_semFatosDoIss() {
        EscolhaParAliquotas atual = escolher("10", Map.of("tribISSQN", 1, "tpRetISSQN", 1, "regApTribSN", 1));
        assertEscolha(atual, "III", TipoAliquotaSimplesEnum.COM_REDISTRIBUICAO, RegraAliquotaSimplesEnum.PADRAO,
                PAR_III_COM);
        assertEquals("anexoIII", atual.caminhoPar());
    }

    // ─── Ordem de avaliação ────────────────────────────────────────────────────

    @Test
    void ordem_regra4AntesDaRegra5() {
        // Atividade sem ISS (11) e regApTribSN 3: decide a regra 4.
        assertEquals(RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES,
                escolher("11", Map.of("regApTribSN", 3)).regra());
    }

    @Test
    void ordem_regras6a8_formamUmOu_aPrimeiraDecide() {
        // As três situações ao mesmo tempo: o tipo é o mesmo; a regra informada é a 6.
        EscolhaParAliquotas atual = escolher("12", Map.of("tribISSQN", 2, "tpBM", 1, "tpRetISSQN", 2));
        assertEquals(RegraAliquotaSimplesEnum.ISS_NAO_INCIDE, atual.regra());
        assertEquals(TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO, atual.tipoAliquota());
        // Sem a 6, decide a 7.
        assertEquals(RegraAliquotaSimplesEnum.ISENCAO_MUNICIPAL,
                escolher("12", Map.of("tribISSQN", 1, "tpBM", 1, "tpRetISSQN", 2)).regra());
    }

    // ─── Anexo pelo fator "r" e linha incompleta ───────────────────────────────

    @Test
    void fatorR_atividade09_anexoIIIouV() {
        EscolhaParAliquotas comFatorR = escolha.escolher(atividade("09"), true, linha(true, false),
                Map.of("tribISSQN", 1, "tpRetISSQN", 1), situacoesDaCarga());
        assertEscolha(comFatorR, "III", TipoAliquotaSimplesEnum.COM_REDISTRIBUICAO, RegraAliquotaSimplesEnum.PADRAO,
                PAR_III_COM);
        EscolhaParAliquotas semFatorR = escolha.escolher(atividade("09"), true, linha(false, false),
                Map.of("tribISSQN", 1, "tpRetISSQN", 1), situacoesDaCarga());
        assertEscolha(semFatorR, "V", TipoAliquotaSimplesEnum.COM_REDISTRIBUICAO, RegraAliquotaSimplesEnum.PADRAO,
                PAR_V_COM);
    }

    @Test
    void fatorR_ausente_comAtividade09_semPar() {
        EscolhaParAliquotas atual = escolha.escolher(atividade("09"), true, linha(null, false), Map.of(),
                situacoesDaCarga());
        assertEquals(MotivoResolucaoAliquota.FATOR_R_AUSENTE, atual.motivo());
        assertNull(atual.anexo());
        assertNull(atual.par());
        // Para as demais atividades o fatorR não importa.
        assertEquals(MotivoResolucaoAliquota.RESOLVIDA, escolha.escolher(atividade("07"), true, linha(null, false),
                Map.of(), situacoesDaCarga()).motivo());
    }

    @Test
    void parSemNulo_valeOParCom_comAviso() {
        // Anexo V sem o par sem; regApTribSN 2 pede o par sem.
        EscolhaParAliquotas atual = escolha.escolher(atividade("09"), true, linha(false, false),
                Map.of("regApTribSN", 2), situacoesDaCarga());
        assertEscolha(atual, "V", TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO,
                RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, PAR_V_COM);
        assertTrue(atual.parSemRedistribuicaoAusente());
        assertEquals("anexoV", atual.caminhoPar());
        assertTrue(EscolhaParAliquotasService.avisoParSemRedistribuicaoAusente("V").contains("aplicado o par com"));
    }

    @Test
    void anexoAusenteNaLinha_motivoAnexoSemAliquota() {
        LinhaAliquotasSimples semAnexoIV = new LinhaAliquotasSimples(false, false, null, null, null, null, null,
                OrigemLinhaAliquotasEnum.REQUISICAO);
        EscolhaParAliquotas atual = escolha.escolher(atividade("14"), true, semAnexoIV,
                Map.of("tribISSQN", 1, "tpRetISSQN", 1), situacoesDaCarga());
        assertEquals("IV", atual.anexo());
        assertNull(atual.par());
        assertEquals(MotivoResolucaoAliquota.ANEXO_SEM_ALIQUOTA, atual.motivo());
    }

    // ─── Campos do ISS ─────────────────────────────────────────────────────────

    private AvaliacaoIssItem avaliar(VocabularioRegimeSimplesEnum vocabulario, String cAtivSN, String trib,
            String ret, String bm, ItemCollectorSimples collector) {
        return escolha.avaliarCamposIss(vocabulario, atividade(cAtivSN), new CamposIssItem(trib, ret, bm), collector);
    }

    private static ItemCollectorSimples coletor() {
        return new ItemCollectorSimples(0, "http://localhost/api");
    }

    @Test
    void camposIss_familiaCrt_desconsideradosComAviso() {
        ItemCollectorSimples collector = coletor();
        AvaliacaoIssItem atual = avaliar(VocabularioRegimeSimplesEnum.CRT, "05", "2", "2", "1", collector);
        assertNull(atual.tribISSQN());
        assertNull(atual.tpRetISSQN());
        assertNull(atual.tpBM());
        assertEquals(1, atual.avisos().size());
        assertTrue(atual.avisos().get(0).contains("tribISSQN, tpRetISSQN, tpBM"));
        assertFalse(collector.hasErros());
    }

    @Test
    void camposIss_tribIssqnObrigatorioNaAtividadeComIss() {
        ItemCollectorSimples collector = coletor();
        avaliar(VocabularioRegimeSimplesEnum.NFSE, "07", null, null, null, collector);
        assertEquals(List.of("VSN-036"), codigos(collector));
        assertEquals("#/itens/0/tribISSQN", collector.getErros().get(0).pointer());
        // Atividade sem ISS no Simples: não exigido.
        ItemCollectorSimples semIss = coletor();
        avaliar(VocabularioRegimeSimplesEnum.NFSE, "11", null, null, null, semIss);
        assertFalse(semIss.hasErros());
    }

    @Test
    void camposIss_foraDoDominio() {
        ItemCollectorSimples collector = coletor();
        avaliar(VocabularioRegimeSimplesEnum.NFSE, "12", "5", "4", "9", collector);
        assertEquals(List.of("VSN-036", "VSN-037", "VSN-038"), codigos(collector));
    }

    @Test
    void camposIss_tpRetIssqnObrigatorioComTribIssqn1() {
        ItemCollectorSimples collector = coletor();
        avaliar(VocabularioRegimeSimplesEnum.NFSE, "14", "1", null, null, collector);
        assertEquals(List.of("VSN-037"), codigos(collector));
    }

    @Test
    void camposIss_tpRetIssqnComImunidade_desconsideradoComAviso() {
        ItemCollectorSimples collector = coletor();
        AvaliacaoIssItem atual = avaliar(VocabularioRegimeSimplesEnum.NFSE, "07", "2", "2", null, collector);
        assertFalse(collector.hasErros());
        assertEquals(2, atual.tribISSQN());
        assertNull(atual.tpRetISSQN());
        assertTrue(atual.avisos().get(0).contains("E0580"));
    }

    @Test
    void camposIss_tpBmAusente_semBeneficio_nuncaErro() {
        ItemCollectorSimples collector = coletor();
        AvaliacaoIssItem atual = avaliar(VocabularioRegimeSimplesEnum.NFSE, "12", "1", "1", null, collector);
        assertFalse(collector.hasErros());
        assertNull(atual.tpBM());
        assertTrue(atual.avisos().isEmpty());
    }

    @Test
    void camposIss_tpBmForaDas12e13_desconsideradoComAviso() {
        ItemCollectorSimples collector = coletor();
        AvaliacaoIssItem atual = avaliar(VocabularioRegimeSimplesEnum.NFSE, "07", "1", "1", "1", collector);
        assertFalse(collector.hasErros());
        assertNull(atual.tpBM());
        assertTrue(atual.avisos().get(0).contains("E0536"));
    }

    @Test
    void camposIss_isencaoComRetencao_avisoE0594() {
        ItemCollectorSimples collector = coletor();
        AvaliacaoIssItem atual = avaliar(VocabularioRegimeSimplesEnum.NFSE, "13", "1", "2", "1", collector);
        assertFalse(collector.hasErros());
        assertEquals(1, atual.tpBM());
        assertEquals(2, atual.tpRetISSQN());
        assertTrue(atual.avisos().get(0).contains("E0594"));
    }

    private static List<String> codigos(ItemCollectorSimples collector) {
        return collector.getErros().stream().map(erro -> erro.code()).toList();
    }
}
