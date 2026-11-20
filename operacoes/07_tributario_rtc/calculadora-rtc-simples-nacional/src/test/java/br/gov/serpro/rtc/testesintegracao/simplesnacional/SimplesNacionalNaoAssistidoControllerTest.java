package br.gov.serpro.rtc.testesintegracao.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Testes de integração do cálculo NÃO ASSISTIDO do Simples Nacional
 * ({@code POST calculadora/observabilidade/simples-nacional/nao-assistido}),
 * contra a base real: as combinações de regime das duas famílias, CRT e NFS-e
 * (inclusive os erros), {@code SOMENTE_RECEITA} nas duas famílias, cada
 * regra do par de alíquotas, o par "sem" nulo, a exigência condicional de
 * {@code aliquotasEfetivas} e de {@code fatorR}, os campos do ISS, as
 * diferenças da NFS-e (1 item, sem IS, alíquota de 2 casas, tpRBSN) e cada
 * erro novo (VSN-030 a 040).
 *
 * <p>A linha de alíquotas padrão dos testes ({@link #linha()}) é a da fixture
 * do assistido para {@code 5YPK4YVG} em 202703, em pontos percentuais: Anexo I
 * 1.1612/0.0129; II 1.1184/0.0121; III 2.2154/0.0257 (sem redistribuição
 * 1.99/0.0231); IV 2.0119/0.022 (sem 1.85/0.0202); V 3.2534/0.0363 (sem o par
 * sem); fator r atingido; sem impedimento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class SimplesNacionalNaoAssistidoControllerTest {

    private static final String ENDPOINT = "/calculadora/observabilidade/simples-nacional/nao-assistido";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    // ─── Montadores de requisição ──────────────────────────────────────────────

    private static Map<String, Object> mapa(Object... chavesEValores) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            if (chavesEValores[i + 1] != null) {
                mapa.put((String) chavesEValores[i], chavesEValores[i + 1]);
            }
        }
        return mapa;
    }

    static Map<String, Object> par(String cbs, String ibs) {
        return mapa("pCBSSN", cbs, "pIBSSN", ibs);
    }

    static Map<String, Object> anexoComIss(String cbs, String ibs, Map<String, Object> sem) {
        Map<String, Object> anexo = par(cbs, ibs);
        if (sem != null) {
            anexo.put("semRedistribuicaoISS", sem);
        }
        return anexo;
    }

    /** Linha padrão dos testes (fixture de 5YPK4YVG em 202703, em pontos percentuais). */
    static Map<String, Object> linha() {
        return mapa("fatorR", true, "impedimentoSublimite", false,
                "anexoI", par("1.1612", "0.0129"),
                "anexoII", par("1.1184", "0.0121"),
                "anexoIII", anexoComIss("2.2154", "0.0257", par("1.99", "0.0231")),
                "anexoIV", anexoComIss("2.0119", "0.022", par("1.85", "0.0202")),
                "anexoV", anexoComIss("3.2534", "0.0363", null));
    }

    static Map<String, Object> item(Object... campos) {
        Map<String, Object> item = mapa("numero", 1, "cClassTrib", "000001", "baseCalculo", "10000.00");
        item.putAll(mapa(campos));
        return item;
    }

    /** Operação da NFS-e (tpDoc 91) com o regime no vocabulário da NFS-e. */
    static Map<String, Object> nfse(String opSimpNac, String regApIBSCBSSN, String regApTribSN,
            Map<String, Object> aliquotas, Map<String, Object> item) {
        Map<String, Object> operacao = mapa("id", "op-1", "versao", "3.0.0", "data", "2027-03-15", "tpDoc", "91",
                "opSimpNac", opSimpNac, "regApIBSCBSSN", regApIBSCBSSN, "regApTribSN", regApTribSN,
                "aliquotasEfetivas", aliquotas);
        operacao.put("itens", List.of(item));
        return operacao;
    }

    /** Operação de documento com CRT. */
    @SafeVarargs
    static Map<String, Object> crt(String tpDoc, String crt, String tpRegime, Map<String, Object> aliquotas,
            Map<String, Object>... itens) {
        Map<String, Object> operacao = mapa("id", "op-1", "versao", "3.0.0", "data", "2027-03-15", "tpDoc", tpDoc,
                "CRT", crt, "tpRegimeIBSCBS", tpRegime, "aliquotasEfetivas", aliquotas);
        operacao.put("itens", Arrays.asList(itens));
        return operacao;
    }

    /** Item da família CRT (IS obrigatório). */
    static Map<String, Object> itemCrt(int numero, String cAtivSN, String cClassTrib, String base) {
        return mapa("numero", numero, "cAtivSN", cAtivSN, "cClassTrib", cClassTrib, "baseCalculo", base,
                "valorImpostoSeletivo", "0.00");
    }

    private record Resposta(int status, JsonNode corpo) {

        JsonNode objeto(int i) {
            return corpo.path("objetos").get(i);
        }

        JsonNode trib(int i) {
            return objeto(i).path("tribCalc");
        }

        JsonNode resolucao(int i) {
            return corpo.path("resolucaoAliquotas").path("itens").get(i);
        }

        String ponteiroErro() {
            return corpo.path("errors").get(0).path("pointer").asText();
        }

        List<String> codigosErro() {
            List<String> codigos = new ArrayList<>();
            corpo.path("errors").forEach(erro -> codigos.add(erro.path("code").asText()));
            return codigos;
        }

        String texto(String caminho) {
            return corpo.at(caminho).asText();
        }
    }

    private Resposta enviar(Map<String, Object> operacao) throws Exception {
        MvcResult resultado = mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(operacao))).andReturn();
        return new Resposta(resultado.getResponse().getStatus(),
                JSON.readTree(resultado.getResponse().getContentAsByteArray()));
    }

    private Resposta enviarNfsePadrao(String cAtivSN, Object... camposIss) throws Exception {
        Object[] campos = new Object[camposIss.length + 2];
        campos[0] = "cAtivSN";
        campos[1] = cAtivSN;
        System.arraycopy(camposIss, 0, campos, 2, camposIss.length);
        return enviar(nfse("3", "1", "1", linha(), item(campos)));
    }

    // ─── Tabela 3.2: família CRT ───────────────────────────────────────────────

    @ParameterizedTest(name = "CRT {0} + tpRegimeIBSCBS {1} ⇒ {2}")
    @CsvSource(nullValues = "-", value = {
            "1, -, SN_PLENO",
            "2, -, SN_SUBLIMITE",
            "1, 1, SOMENTE_RECEITA",
            "2, 1, SOMENTE_RECEITA",
            "3, 2, PENDENTE" })
    void tabela32_combinacoesQueLevamAUmRegime(String crt, String tpRegime, String regime) throws Exception {
        Resposta resposta = enviar(crt("55", crt, tpRegime, linha(), itemCrt(1, "01", "000001", "1000.00")));
        assertEquals(200, resposta.status(), resposta.corpo()::toString);
        assertEquals(regime, resposta.texto("/oper/regimeApuracao"));
        assertEquals(crt, resposta.texto("/oper/CRT"));
    }

    @ParameterizedTest(name = "CRT {0} + tpRegimeIBSCBS {1} ⇒ {2}")
    @CsvSource(nullValues = "-", value = {
            "1, 2, VSN-018",
            "2, 2, VSN-018",
            "3, -, VSN-019",
            "3, 1, VSN-018",
            "4, -, VSN-019",
            "4, 1, VSN-018",
            "4, 2, VSN-018",
            "-, -, VSN-016",
            "5, -, VSN-016",
            "1, 3, VSN-017" })
    void tabela32_erros(String crt, String tpRegime, String codigo) throws Exception {
        Resposta resposta = enviar(crt("55", crt, tpRegime, linha(), itemCrt(1, "01", "000001", "1000.00")));
        assertEquals(422, resposta.status());
        assertEquals(List.of(codigo), resposta.codigosErro());
    }

    @Test
    void tabela32_crt4_mensagemNoVocabularioRecebido() throws Exception {
        Resposta resposta = enviar(crt("55", "4", null, linha(), itemCrt(1, "01", "000001", "1000.00")));
        assertEquals("#/CRT", resposta.ponteiroErro());
        assertTrue(resposta.corpo().path("errors").get(0).path("detail").asText().contains("MEI (CRT=4)"));
    }

    // ─── Tabela 3.3: família NFS-e ─────────────────────────────────────────────

    @ParameterizedTest(name = "opSimpNac {0} + regApIBSCBSSN {1} ⇒ {2}")
    @CsvSource({
            "3, 1, SN_PLENO",
            "3, 2, SN_SUBLIMITE",
            "3, 3, SOMENTE_RECEITA",
            "4, 1, PENDENTE",
            "4, 2, PENDENTE_SUBLIMITE",
            "4, 3, SOMENTE_RECEITA" })
    void tabela33_combinacoesQueLevamAUmRegime(String opSimpNac, String regAp, String regime) throws Exception {
        Resposta resposta = enviar(nfse(opSimpNac, regAp, "1", linha(),
                item("cAtivSN", "07", "tribISSQN", "1", "tpRetISSQN", "1")));
        assertEquals(200, resposta.status(), resposta.corpo()::toString);
        assertEquals(regime, resposta.texto("/oper/regimeApuracao"));
        assertEquals(opSimpNac, resposta.texto("/oper/opSimpNac"));
        assertEquals(regAp, resposta.texto("/oper/regApIBSCBSSN"));
        assertEquals("1", resposta.texto("/oper/regApTribSN"));
    }

    @ParameterizedTest(name = "opSimpNac {0} + regApIBSCBSSN {1} + regApTribSN {2} ⇒ {3}")
    @CsvSource(nullValues = "-", value = {
            "1, -, -, VSN-019",
            "2, -, -, VSN-019",
            "1, 1, -, VSN-031",
            "2, 2, -, VSN-031",
            "1, 3, -, VSN-031",
            "3, -, 1, VSN-031",
            "4, -, 1, VSN-031",
            "3, 4, 1, VSN-031",
            "3, 1, -, VSN-032",
            "4, 1, -, VSN-032",
            "1, -, 1, VSN-032",
            "3, 1, 4, VSN-032",
            "-, 1, 1, VSN-030",
            "5, 1, 1, VSN-030" })
    void tabela33_erros(String opSimpNac, String regAp, String regApTrib, String codigo) throws Exception {
        Resposta resposta = enviar(nfse(opSimpNac, regAp, regApTrib, linha(),
                item("cAtivSN", "07", "tribISSQN", "1", "tpRetISSQN", "1")));
        assertEquals(422, resposta.status());
        assertEquals(List.of(codigo), resposta.codigosErro());
    }

    @Test
    void vsn033_campoDeRegimeDoOutroVocabulario() throws Exception {
        Map<String, Object> nfseComCrt = nfse("3", "1", "1", linha(), item("cAtivSN", "07"));
        nfseComCrt.put("CRT", "1");
        Resposta resposta = enviar(nfseComCrt);
        assertEquals(422, resposta.status());
        assertEquals(List.of("VSN-033"), resposta.codigosErro());
        assertEquals("#/CRT", resposta.ponteiroErro());

        Map<String, Object> nfeComOpSimpNac = crt("55", "1", null, linha(), itemCrt(1, "01", "000001", "100.00"));
        nfeComOpSimpNac.put("opSimpNac", "3");
        nfeComOpSimpNac.put("regApTribSN", "1");
        assertEquals(List.of("VSN-033", "VSN-033"), enviar(nfeComOpSimpNac).codigosErro());
    }

    // ─── SOMENTE_RECEITA nas duas famílias ─────────────────────────────────────

    @Test
    void somenteReceita_nfe_naoCalcula_semAliquotasEfetivas_nemCAtivSN() throws Exception {
        Resposta resposta = enviar(crt("55", "1", "1", null, itemCrt(1, null, "000001", "1000.00"),
                itemCrt(2, "01", "410001", "500.00")));
        assertEquals(200, resposta.status(), resposta.corpo()::toString);
        assertEquals("SOMENTE_RECEITA", resposta.texto("/oper/regimeApuracao"));
        assertTrue(resposta.corpo().path("resolucaoAliquotas").isMissingNode());
        assertEquals("NAO_CALCULADO", resposta.objeto(0).path("estadoItem").asText());
        assertEquals("1000.00", resposta.trib(0).path("vRBSNitem").asText());
        assertEquals("1", resposta.trib(0).path("tpRBSN").asText());
        assertTrue(resposta.trib(0).path("vBC").isMissingNode());
        assertEquals("0.00", resposta.trib(1).path("vRBSNitem").asText());
        assertEquals("1000.00", resposta.texto("/total/vRBSNtot"));
        assertTrue(resposta.corpo().path("total").path("vCBSSNtot").isMissingNode());
    }

    @Test
    void somenteReceita_nfse_semCamposDoIss_eCClassTribObrigatorio() throws Exception {
        Resposta resposta = enviar(nfse("4", "3", "1", null, item("cAtivSN", "07")));
        assertEquals(200, resposta.status(), resposta.corpo()::toString);
        assertEquals("SOMENTE_RECEITA", resposta.texto("/oper/regimeApuracao"));
        assertEquals("NAO_CALCULADO", resposta.objeto(0).path("estadoItem").asText());
        assertEquals("10000.00", resposta.trib(0).path("vRBSNitem").asText());

        Resposta semCClassTrib = enviar(nfse("3", "3", "1", null, mapa("numero", 1, "baseCalculo", "10.00")));
        assertEquals("INCONSISTENCIA_ENTRADA", semCClassTrib.objeto(0).path("estadoItem").asText());
        assertEquals("VSN-020", semCClassTrib.objeto(0).path("erros").get(0).path("code").asText());
    }

    // ─── Exigência condicional de aliquotasEfetivas e de fatorR ────────────────

    @Test
    void vsn039_aliquotasEfetivasAusente() throws Exception {
        Resposta resposta = enviar(nfse("3", "1", "1", null, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals(422, resposta.status());
        assertEquals(List.of("VSN-039"), resposta.codigosErro());
        assertEquals("#/aliquotasEfetivas", resposta.ponteiroErro());
    }

    @Test
    void vsn040_fatorRAusenteComAtividade09() throws Exception {
        Map<String, Object> semFatorR = linha();
        semFatorR.remove("fatorR");
        Resposta resposta = enviar(nfse("3", "1", "1", semFatorR, item("cAtivSN", "09", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals(200, resposta.status());
        JsonNode erro = resposta.objeto(0).path("erros").get(0);
        assertEquals("VSN-040", erro.path("code").asText());
        assertEquals("#/aliquotasEfetivas/fatorR", erro.path("pointer").asText());
        assertEquals("FATOR_R_AUSENTE", resposta.resolucao(0).path("motivo").asText());
        // Atividade sem fator r: o fatorR não é exigido.
        Resposta outra = enviar(nfse("3", "1", "1", semFatorR, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals("CALCULADO", outra.objeto(0).path("estadoItem").asText());
        assertTrue(outra.corpo().at("/resolucaoAliquotas/fatorR").isNull());
    }

    @Test
    void parQueNenhumItemUsa_podeFaltar_masSeVierTemOFormatoValidado() throws Exception {
        // Só o Anexo III é usado: os demais podem faltar.
        Map<String, Object> soAnexoIII = mapa("fatorR", false,
                "anexoIII", anexoComIss("2.2154", "0.0257", par("1.99", "0.0231")));
        assertEquals("CALCULADO", enviar(nfse("3", "1", "1", soAnexoIII, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1"))).objeto(0).path("estadoItem").asText());

        // Anexo V (não usado) fora do formato: erro global, com o caminho completo.
        Map<String, Object> invalida = linha();
        invalida.put("anexoV", anexoComIss("3,25", "-0.03", null));
        Resposta resposta = enviar(nfse("3", "1", "1", invalida, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals(422, resposta.status());
        assertEquals(List.of("VSN-005", "VSN-007"), resposta.codigosErro());
        assertEquals("#/aliquotasEfetivas/anexoV/pCBSSN", resposta.ponteiroErro());
        assertTrue(resposta.corpo().path("errors").get(1).path("detail").asText().contains("negativo"));
    }

    @Test
    void vsn004e006_valorDoParEscolhidoAusente_ponteiroParaDentroDeAliquotasEfetivas() throws Exception {
        // Par sem do Anexo III presente, mas sem a CBS: imunidade (tribISSQN 2) usa o par sem.
        Map<String, Object> semCbs = linha();
        semCbs.put("anexoIII", anexoComIss("2.2154", "0.0257", par(null, "0.0231")));
        Resposta resposta = enviar(nfse("3", "1", "1", semCbs, item("cAtivSN", "07", "tribISSQN", "2")));
        JsonNode erro = resposta.objeto(0).path("erros").get(0);
        assertEquals("VSN-004", erro.path("code").asText());
        assertEquals("#/aliquotasEfetivas/anexoIII/semRedistribuicaoISS/pCBSSN", erro.path("pointer").asText());

        // Par com do Anexo III sem o IBS, com regime que gera IBS.
        Map<String, Object> semIbs = linha();
        semIbs.put("anexoIII", anexoComIss("2.2154", null, par("1.99", "0.0231")));
        JsonNode erroIbs = enviar(nfse("3", "1", "1", semIbs, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1"))).objeto(0).path("erros").get(0);
        assertEquals("VSN-006", erroIbs.path("code").asText());
        assertEquals("#/aliquotasEfetivas/anexoIII/pIBSSN", erroIbs.path("pointer").asText());

        // Regime que não gera IBS (3+2): o IBS ausente não é exigido.
        assertEquals("CALCULADO", enviar(nfse("3", "2", "1", semIbs, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1"))).objeto(0).path("estadoItem").asText());
    }

    // ─── Regras do par de alíquotas ────────────────────────────────────────────

    @Test
    void regra1_documento_atividadeNaoAdmitidaNaNfse() throws Exception {
        Resposta resposta = enviarNfsePadrao("01");
        assertEquals("NAO_CALCULADO", resposta.objeto(0).path("estadoItem").asText());
        assertEquals("DOCUMENTO", resposta.resolucao(0).path("regraAliquota").asText());
        assertTrue(resposta.resolucao(0).path("tipoAliquota").isNull());
        assertEquals("ATIVIDADE_INADMISSIVEL_NO_DOCUMENTO", resposta.resolucao(0).path("motivo").asText());
    }

    @Test
    void regra2_atividadeNaoTributada_nasDuasFamilias() throws Exception {
        Resposta nfse = enviar(nfse("3", "1", "1", linha(), item("cAtivSN", "90", "cClassTrib", "200019")));
        assertEquals("SEM_ALIQUOTA", nfse.resolucao(0).path("tipoAliquota").asText());
        assertEquals("ATIVIDADE_NAO_TRIBUTADA", nfse.resolucao(0).path("regraAliquota").asText());
        assertEquals("0", nfse.resolucao(0).path("pCBSSN").asText());
        assertEquals("CALCULADO", nfse.objeto(0).path("estadoItem").asText());
        assertEquals("0.00", nfse.trib(0).path("vCBSSN").asText());

        Resposta nfe = enviar(crt("55", "1", null, linha(), itemCrt(1, "90", "410001", "100.00")));
        assertEquals("SEM_ALIQUOTA", nfe.resolucao(0).path("tipoAliquota").asText());
    }

    @Test
    void regra3_anexoSemIss_parUnico_familiaCrt() throws Exception {
        Resposta resposta = enviar(crt("55", "1", null, linha(), itemCrt(1, "01", "000001", "1000.00"),
                itemCrt(2, "03", "000001", "500.00")));
        assertEquals("PAR_UNICO", resposta.resolucao(0).path("tipoAliquota").asText());
        assertEquals("ANEXO_SEM_ISS", resposta.resolucao(0).path("regraAliquota").asText());
        assertEquals("I", resposta.resolucao(0).path("anexoAplicado").asText());
        assertEquals("1.1612", resposta.resolucao(0).path("pCBSSN").asText());
        // 1000 × 1,1612% = 11,612 ⇒ 11,61; 1000 × 0,0129% = 0,129 ⇒ 0,13.
        assertEquals("11.61", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("0.13", resposta.trib(0).path("vIBSSN").asText());
        assertEquals("II", resposta.resolucao(1).path("anexoAplicado").asText());
    }

    @ParameterizedTest(name = "regApIBSCBSSN {0} + regApTribSN {1}")
    @CsvSource({ "1, 2", "1, 3", "2, 2" })
    void regra4_issForaDoSimples_nfse(String regAp, String regApTrib) throws Exception {
        Resposta resposta = enviar(nfse("3", regAp, regApTrib, linha(), item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals("ISS_FORA_DO_SIMPLES", resposta.resolucao(0).path("regraAliquota").asText());
        assertEquals("SEM_REDISTRIBUICAO", resposta.resolucao(0).path("tipoAliquota").asText());
        assertEquals("1.99", resposta.resolucao(0).path("pCBSSN").asText());
        // NFS-e: 1,99% com 2 casas; 10000 × 1,99% = 199,00.
        assertEquals("199.00", resposta.trib(0).path("vCBSSN").asText());
    }

    @Test
    void regra4_impedimentoPeloSublimite_daLinha() throws Exception {
        Map<String, Object> impedida = linha();
        impedida.put("impedimentoSublimite", true);
        Resposta resposta = enviar(nfse("3", "2", "2", impedida, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals("ISS_FORA_DO_SIMPLES", resposta.resolucao(0).path("regraAliquota").asText());
        assertTrue(resposta.corpo().at("/resolucaoAliquotas/indSublimite").asBoolean());
        assertTrue(resposta.corpo().at("/resolucaoAliquotas/avisos").isEmpty());
    }

    @Test
    void regra4_familiaCrt_crt2NoCte() throws Exception {
        // CT-e, atividade 05 (Anexo III, sem ISS): com CRT 2 decide a regra 4, antes da 5.
        Resposta sublimite = enviar(crt("57", "2", null, linha(), itemCrt(1, "05", "000001", "1000.00")));
        assertEquals(200, sublimite.status(), sublimite.corpo()::toString);
        assertEquals("ISS_FORA_DO_SIMPLES", sublimite.resolucao(0).path("regraAliquota").asText());
        assertEquals("SEM_REDISTRIBUICAO", sublimite.resolucao(0).path("tipoAliquota").asText());
        // 1000 × 1,99% = 19,90 (família CRT: precisão integral).
        assertEquals("19.90", sublimite.trib(0).path("vCBSSN").asText());
        assertTrue(sublimite.trib(0).path("vIBSSN").isMissingNode());
    }

    @Test
    void regra5_atividadeSemIss_nasDuasFamilias() throws Exception {
        Resposta nfse = enviarNfsePadrao("11");
        assertEquals("ATIVIDADE_SEM_ISS", nfse.resolucao(0).path("regraAliquota").asText());
        assertEquals("SEM_REDISTRIBUICAO", nfse.resolucao(0).path("tipoAliquota").asText());

        Resposta nfcom = enviar(crt("62", "1", null, linha(), itemCrt(1, "06", "000001", "1000.00")));
        assertEquals(200, nfcom.status(), nfcom.corpo()::toString);
        assertEquals("ATIVIDADE_SEM_ISS", nfcom.resolucao(0).path("regraAliquota").asText());
        assertEquals("1.99", nfcom.resolucao(0).path("pCBSSN").asText());
    }

    @ParameterizedTest(name = "tribISSQN {0}")
    @CsvSource({ "2", "3", "4" })
    void regra6_issNaoIncide(String tribISSQN) throws Exception {
        Resposta resposta = enviarNfsePadrao("14", "tribISSQN", tribISSQN);
        assertEquals("ISS_NAO_INCIDE", resposta.resolucao(0).path("regraAliquota").asText());
        assertEquals("IV", resposta.resolucao(0).path("anexoAplicado").asText());
        assertEquals("1.85", resposta.resolucao(0).path("pCBSSN").asText());
    }

    @Test
    void regra7_isencaoMunicipal() throws Exception {
        Resposta resposta = enviarNfsePadrao("12", "tribISSQN", "1", "tpRetISSQN", "1", "tpBM", "1");
        assertEquals("ISENCAO_MUNICIPAL", resposta.resolucao(0).path("regraAliquota").asText());
        assertEquals("SEM_REDISTRIBUICAO", resposta.resolucao(0).path("tipoAliquota").asText());
    }

    @ParameterizedTest(name = "tpRetISSQN {0}")
    @CsvSource({ "2", "3" })
    void regra8_issRetido(String tpRetISSQN) throws Exception {
        Resposta resposta = enviarNfsePadrao("07", "tribISSQN", "1", "tpRetISSQN", tpRetISSQN);
        assertEquals("ISS_RETIDO", resposta.resolucao(0).path("regraAliquota").asText());
        assertEquals("SEM_REDISTRIBUICAO", resposta.resolucao(0).path("tipoAliquota").asText());
    }

    @Test
    void regra9_padrao_comRedistribuicao() throws Exception {
        Resposta resposta = enviarNfsePadrao("13", "tribISSQN", "1", "tpRetISSQN", "1", "tpBM", "2");
        assertEquals("PADRAO", resposta.resolucao(0).path("regraAliquota").asText());
        assertEquals("COM_REDISTRIBUICAO", resposta.resolucao(0).path("tipoAliquota").asText());
        assertEquals("2.0119", resposta.resolucao(0).path("pCBSSN").asText());
        assertEquals("RESOLVIDA", resposta.resolucao(0).path("motivo").asText());
    }

    @Test
    void parSemNulo_valeOParCom_comAviso() throws Exception {
        // Atividade 09 com fator r < 28% ⇒ Anexo V, que não tem o par sem; imunidade pede o par sem.
        Map<String, Object> linha = linha();
        linha.put("fatorR", false);
        Resposta resposta = enviar(nfse("3", "1", "1", linha, item("cAtivSN", "09", "tribISSQN", "2")));
        JsonNode resolucao = resposta.resolucao(0);
        assertEquals("V", resolucao.path("anexoAplicado").asText());
        assertEquals("SEM_REDISTRIBUICAO", resolucao.path("tipoAliquota").asText());
        assertEquals("ISS_NAO_INCIDE", resolucao.path("regraAliquota").asText());
        assertEquals("3.2534", resolucao.path("pCBSSN").asText());
        assertTrue(resolucao.path("avisos").get(0).asText().contains("Par sem redistribuição do ISS ausente"));
        assertEquals("CALCULADO", resposta.objeto(0).path("estadoItem").asText());
    }

    @Test
    void fatorR_atividade09_anexoIII() throws Exception {
        Resposta resposta = enviarNfsePadrao("09", "tribISSQN", "1", "tpRetISSQN", "1");
        assertEquals("III", resposta.resolucao(0).path("anexoAplicado").asText());
        assertTrue(resposta.corpo().at("/resolucaoAliquotas/fatorR").asBoolean());
    }

    // ─── Campos do ISS ─────────────────────────────────────────────────────────

    @Test
    void camposDoIss_familiaCrt_desconsideradosComAviso() throws Exception {
        Map<String, Object> item = itemCrt(1, "01", "000001", "1000.00");
        item.put("tribISSQN", "2");
        item.put("tpRetISSQN", "2");
        Resposta resposta = enviar(crt("55", "1", null, linha(), item));
        assertEquals("CALCULADO", resposta.objeto(0).path("estadoItem").asText());
        assertTrue(resposta.resolucao(0).path("avisos").get(0).asText().contains("tribISSQN, tpRetISSQN"));
    }

    @Test
    void vsn036_037_038_camposDoIssNaNfse() throws Exception {
        Resposta semTrib = enviarNfsePadrao("07");
        assertEquals("VSN-036", semTrib.objeto(0).path("erros").get(0).path("code").asText());
        assertEquals("#/itens/0/tribISSQN", semTrib.objeto(0).path("erros").get(0).path("pointer").asText());

        Resposta semRet = enviarNfsePadrao("07", "tribISSQN", "1");
        assertEquals("VSN-037", semRet.objeto(0).path("erros").get(0).path("code").asText());

        Resposta bmInvalido = enviarNfsePadrao("12", "tribISSQN", "1", "tpRetISSQN", "1", "tpBM", "7");
        assertEquals("VSN-038", bmInvalido.objeto(0).path("erros").get(0).path("code").asText());
    }

    @Test
    void camposDoIss_desconsideradosNaNfse_comAviso() throws Exception {
        Resposta retencaoComImunidade = enviarNfsePadrao("07", "tribISSQN", "2", "tpRetISSQN", "2");
        assertEquals("ISS_NAO_INCIDE", retencaoComImunidade.resolucao(0).path("regraAliquota").asText());
        assertTrue(retencaoComImunidade.resolucao(0).path("avisos").get(0).asText().contains("E0580"));

        Resposta bmForaDas12e13 = enviarNfsePadrao("07", "tribISSQN", "1", "tpRetISSQN", "1", "tpBM", "1");
        assertEquals("PADRAO", bmForaDas12e13.resolucao(0).path("regraAliquota").asText());
        assertTrue(bmForaDas12e13.resolucao(0).path("avisos").get(0).asText().contains("E0536"));

        Resposta isencaoComRetencao = enviarNfsePadrao("12", "tribISSQN", "1", "tpRetISSQN", "2", "tpBM", "1");
        assertEquals("ISENCAO_MUNICIPAL", isencaoComRetencao.resolucao(0).path("regraAliquota").asText());
        assertTrue(isencaoComRetencao.resolucao(0).path("avisos").get(0).asText().contains("E0594"));
    }

    // ─── NFS-e: 1 item, IS, tpRBSN e alíquota de 2 casas ───────────────────────

    @Test
    void vsn034_nfseComMaisDeUmItem() throws Exception {
        Map<String, Object> operacao = nfse("3", "1", "1", linha(), item("cAtivSN", "07"));
        operacao.put("itens", List.of(item("cAtivSN", "07"), item("numero", 2, "cAtivSN", "07")));
        Resposta resposta = enviar(operacao);
        assertEquals(422, resposta.status());
        assertEquals(List.of("VSN-034"), resposta.codigosErro());
    }

    @Test
    void vsn035_impostoSeletivoNaNfse_eGrupoISForaDaResposta() throws Exception {
        Resposta comIs = enviarNfsePadrao("07", "tribISSQN", "1", "tpRetISSQN", "1", "valorImpostoSeletivo", "5.00");
        assertEquals("VSN-035", comIs.objeto(0).path("erros").get(0).path("code").asText());

        Resposta zero = enviarNfsePadrao("07", "tribISSQN", "1", "tpRetISSQN", "1", "valorImpostoSeletivo", "0.00");
        assertEquals("CALCULADO", zero.objeto(0).path("estadoItem").asText());
        assertTrue(zero.trib(0).path("IS").isMissingNode());
        assertTrue(zero.corpo().path("total").path("totalIs").isMissingNode());
    }

    @Test
    void nfse_aliquotaDe2Casas() throws Exception {
        Resposta resposta = enviarNfsePadrao("07", "tribISSQN", "1", "tpRetISSQN", "1");
        // Par com do Anexo III: 2,2154 ⇒ 2,22 e 0,0257 ⇒ 0,03.
        assertEquals("2.2154", resposta.resolucao(0).path("pCBSSN").asText());
        assertEquals("2.22", resposta.trib(0).path("pCBSSN").asText());
        assertEquals("0.03", resposta.trib(0).path("pIBSSN").asText());
        assertEquals("222.00", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("3.00", resposta.trib(0).path("vIBSSN").asText());
        assertEquals("225.00", resposta.trib(0).path("totalItem").asText());
        assertTrue(resposta.trib(0).path("memoriaCalculo").path("cbs").asText().contains("2,22%"));
    }

    @Test
    void nfse_aliquotaDe2Casas_depoisDaCompraGovernamental() throws Exception {
        Map<String, Object> operacao = nfse("3", "1", "1", linha(), item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1"));
        operacao.put("tpEnteGov", "1");
        Resposta resposta = enviar(operacao);
        // União: pCBS' = 2,2154 + 0,0257 = 2,2411 ⇒ 2,24 (arredondando antes daria 2,22 + 0,03 = 2,25).
        assertEquals("2.24", resposta.trib(0).path("pCBSSN").asText());
        assertEquals("224.00", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("0.00", resposta.trib(0).path("vIBSSN").asText());
    }

    @Test
    void familiaCrt_precisaoIntegral() throws Exception {
        Map<String, Object> linha = linha();
        linha.put("anexoI", par("1.16123456789", "0.0129"));
        Resposta resposta = enviar(crt("55", "1", null, linha, itemCrt(1, "01", "000001", "10000.00")));
        // 10000 × 1,16123456789% = 116,123456789 ⇒ 116,12; apresentação com 4 casas.
        assertEquals("116.12", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("1.1612", resposta.trib(0).path("pCBSSN").asText());
        assertEquals("1.16123456789", resposta.resolucao(0).path("pCBSSN").asText());
    }

    @ParameterizedTest(name = "cClassTrib {0} (tpRBSN {1})")
    @CsvSource({ "200019, 0, 0.00", "410008, 2, 10000.00", "200001, 3, 10000.00" })
    void nfse_tpRBSNDiferenteDe1_valoresZerados(String cClassTrib, String tpRBSN, String receita) throws Exception {
        Resposta resposta = enviarNfsePadrao("07", "tribISSQN", "1", "tpRetISSQN", "1", "cClassTrib", cClassTrib);
        assertEquals(200, resposta.status(), resposta.corpo()::toString);
        assertEquals(tpRBSN, resposta.trib(0).path("tpRBSN").asText());
        assertEquals(receita, resposta.trib(0).path("vRBSNitem").asText());
        assertEquals("0.00", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("0.00", resposta.trib(0).path("vCBSPendSusp").asText());
    }

    @Test
    void familiaCrt_tpRBSN4_pendenteSuspenso() throws Exception {
        Resposta resposta = enviar(crt("55", "1", null, linha(), itemCrt(1, "01", "550001", "1000.00")));
        assertEquals("4", resposta.trib(0).path("tpRBSN").asText());
        assertEquals("0.00", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("11.61", resposta.trib(0).path("vCBSPendSusp").asText());
    }

    // ─── PENDENTE e PENDENTE_SUBLIMITE ─────────────────────────────────────────

    @Test
    void pendente_nfse_valoresPendentesSuspensos() throws Exception {
        Resposta resposta = enviar(nfse("4", "1", "1", linha(), item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals("PENDENTE", resposta.texto("/oper/regimeApuracao"));
        assertEquals("0.00", resposta.trib(0).path("vCBSSN").asText());
        assertEquals("222.00", resposta.trib(0).path("vCBSPendSusp").asText());
        assertEquals("3.00", resposta.trib(0).path("vIBSPendSusp").asText());
        assertEquals("0.00", resposta.trib(0).path("totalItem").asText());
    }

    @Test
    void pendenteSublimite_nfse_soACbs() throws Exception {
        Resposta resposta = enviar(nfse("4", "2", "2", linha(), item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertEquals("PENDENTE_SUBLIMITE", resposta.texto("/oper/regimeApuracao"));
        assertTrue(resposta.trib(0).path("vIBSPendSusp").isMissingNode());
        assertTrue(resposta.trib(0).path("pIBSSN").isMissingNode());
        // regApTribSN 2 ⇒ par sem: 1,99% ⇒ 199,00 pendente/suspenso.
        assertEquals("199.00", resposta.trib(0).path("vCBSPendSusp").asText());
        assertEquals("ISS_FORA_DO_SIMPLES", resposta.resolucao(0).path("regraAliquota").asText());
    }

    // ─── Avisos da operação e saída ────────────────────────────────────────────

    @Test
    void avisos_regApIBSCBSSN2ComRegApTribSN1_eDivergenciaComImpedimento() throws Exception {
        Resposta incoerente = enviar(nfse("3", "2", "1", linha(), item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        JsonNode avisos = incoerente.corpo().at("/resolucaoAliquotas/avisos");
        assertEquals(2, avisos.size(), avisos::toString);
        assertTrue(avisos.get(0).asText().contains("SN_SUBLIMITE"));
        assertTrue(avisos.get(1).asText().contains("incoerente"));
        // regApIBSCBSSN 2 também decide a regra 4.
        assertEquals("ISS_FORA_DO_SIMPLES", incoerente.resolucao(0).path("regraAliquota").asText());

        Map<String, Object> impedida = linha();
        impedida.put("impedimentoSublimite", true);
        impedida.put("anexoIII", anexoComIss("2.2154", null, par("1.99", null)));
        Resposta pleno = enviar(nfse("3", "1", "1", impedida, item("cAtivSN", "07", "tribISSQN", "1",
                "tpRetISSQN", "1")));
        assertTrue(pleno.corpo().at("/resolucaoAliquotas/avisos/0").asText().contains("SN_PLENO"));
        assertEquals("VSN-006", pleno.objeto(0).path("erros").get(0).path("code").asText());
    }

    @Test
    void saida_cabecalhoDoNaoAssistido_eCAtivSNNormalizado() throws Exception {
        Resposta resposta = enviarNfsePadrao("7", "tribISSQN", "1", "tpRetISSQN", "1");
        JsonNode bloco = resposta.corpo().path("resolucaoAliquotas");
        assertTrue(bloco.path("fatorR").asBoolean());
        assertFalse(bloco.path("indSublimite").asBoolean());
        assertTrue(bloco.path("niContribuinte").isMissingNode());
        assertTrue(bloco.path("nsu").isMissingNode());
        assertEquals("07", resposta.resolucao(0).path("cAtivSN").asText());
        assertEquals("CALCULADO", resposta.objeto(0).path("estadoItem").asText());
        // Ordem dos blocos: oper, resolucaoAliquotas, objetos, total.
        List<String> campos = new ArrayList<>();
        campos.addAll(resposta.corpo().propertyNames());
        assertEquals(List.of("oper", "resolucaoAliquotas", "objetos", "total"), campos);
    }

    @Test
    void itemNaoImplementado_listadoNaResolucao() throws Exception {
        Resposta resposta = enviar(crt("55", "1", null, linha(), itemCrt(1, "01", "620001", "100.00")));
        assertEquals("NAO_IMPLEMENTADO", resposta.objeto(0).path("estadoItem").asText());
        assertEquals("ITEM_NAO_IMPLEMENTADO", resposta.resolucao(0).path("motivo").asText());
    }

    @Test
    void openApi_documentaONaoAssistido_eMarcaOClassicoComoObsoleto() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api-docs")).andReturn();
        assertEquals(200, resultado.getResponse().getStatus());
        JsonNode documento = JSON.readTree(resultado.getResponse().getContentAsByteArray());
        JsonNode caminhos = documento.path("paths");
        JsonNode naoAssistido = caminhos.path("/calculadora/observabilidade/simples-nacional/nao-assistido").path("post");
        assertFalse(naoAssistido.isMissingNode(), "rota do não assistido no OpenAPI");
        assertFalse(naoAssistido.path("deprecated").asBoolean());
        JsonNode classico = caminhos.path("/calculadora/observabilidade/simples-nacional").path("post");
        assertTrue(classico.path("deprecated").asBoolean(), "clássico marcado como deprecated");
        assertTrue(classico.path("description").asText().contains("nao-assistido"));
        JsonNode esquemas = documento.path("components").path("schemas");
        assertFalse(esquemas.path("AliquotasEfetivasInput").isMissingNode());
        assertFalse(esquemas.path("ResolucaoAliquotasNaoAssistido").isMissingNode());
    }

    @Test
    void catalogoDeErros_incluiOsCodigosNovos() throws Exception {
        for (int numero = 30; numero <= 40; numero++) {
            String codigo = "VSN-0" + numero;
            MvcResult resultado = mockMvc.perform(get("/calculadora/observabilidade/simples-nacional/erros")
                    .param("codigo", codigo)).andReturn();
            assertEquals(200, resultado.getResponse().getStatus(), codigo);
            JsonNode corpo = JSON.readTree(resultado.getResponse().getContentAsByteArray());
            assertEquals(codigo, corpo.path("codigo").asText());
            assertFalse(corpo.path("titulo").asText().isBlank());
        }
    }

    @Test
    void cAtivSNDesconhecido_naoCalculado() throws Exception {
        Resposta resposta = enviar(crt("55", "1", null, linha(), itemCrt(1, "77", "000001", "100.00")));
        assertEquals("NAO_CALCULADO", resposta.objeto(0).path("estadoItem").asText());
        assertEquals("ATIVIDADE_DESCONHECIDA", resposta.resolucao(0).path("motivo").asText());
        assertTrue(resposta.resolucao(0).path("regraAliquota").isNull());
    }
}
