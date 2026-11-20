package br.gov.serpro.rtc.testesintegracao.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Testes de integração do motivo do estado {@code NAO_CALCULADO}
 * ({@code motivoEstado} do item) nos endpoints de cálculo do núcleo —
 * clássico e não assistido — contra a base real, e do tpDoc sem tratamento na
 * Calculadora lido da tabela de tipos de documento (VSN-015).
 *
 * <p>Cenários de {@code motivoEstado}: cAtivSN ausente
 * ({@code CATIVSN_AUSENTE}); cAtivSN {@code 99} ({@code CATIVSN_DESCONHECIDO});
 * atividade {@code 07}, admitida só na NFS-e, enviada em NF-e
 * ({@code CATIVSN_INADMISSIVEL_NO_DOCUMENTO}); CRT 1 com
 * {@code tpRegimeIBSCBS=1} ({@code REGIME_SOMENTE_RECEITA}, só no não
 * assistido — o clássico rejeita essa combinação). O motivo
 * {@code ANEXO_SEM_ALIQUOTA_CBS}, exclusivo do cálculo assistido, é coberto
 * pelos testes do assistido. Item {@code CALCULADO} nunca traz o campo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class MotivoEstadoSimplesControllerTest {

    private static final String ENDPOINT_CLASSICO = "/calculadora/observabilidade/simples-nacional";
    private static final String ENDPOINT_NAO_ASSISTIDO = "/calculadora/observabilidade/simples-nacional/nao-assistido";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String DESCRICAO_AUSENTE = "O código de atividade do Simples Nacional (cAtivSN) não foi "
            + "informado; sem ele o IBS e a CBS do item não são calculados (NT 2026.005, §5.3.3).";
    private static final String DESCRICAO_DESCONHECIDO = "O código de atividade '99' não existe ou não está vigente "
            + "na data do fato gerador; o IBS e a CBS do item não são calculados.";
    private static final String DESCRICAO_INADMISSIVEL = "A atividade '07' não é admitida no tipo de documento NF-e "
            + "(tpDoc 55); o IBS e a CBS do item não são calculados.";
    private static final String DESCRICAO_SOMENTE_RECEITA = "O optante apura o IBS e a CBS pelo regime regular, "
            + "fora do Simples Nacional; o item leva ao Simples apenas a receita bruta.";

    @Autowired
    private MockMvc mockMvc;

    // ─── Montadores ────────────────────────────────────────────────────────────

    private static Map<String, Object> mapa(Object... chavesEValores) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            if (chavesEValores[i + 1] != null) {
                mapa.put((String) chavesEValores[i], chavesEValores[i + 1]);
            }
        }
        return mapa;
    }

    /** Item do clássico: alíquotas no próprio item. */
    private static Map<String, Object> itemClassico(int numero, String cAtivSN) {
        return mapa("numero", numero, "cAtivSN", cAtivSN, "cClassTrib", "000001", "baseCalculo", "1000.00",
                "valorImpostoSeletivo", "0.00", "pIBSSN", "0.0129", "pCBSSN", "1.1612");
    }

    /** Item do não assistido da família CRT. */
    private static Map<String, Object> itemCrt(int numero, String cAtivSN) {
        return mapa("numero", numero, "cAtivSN", cAtivSN, "cClassTrib", "000001", "baseCalculo", "1000.00",
                "valorImpostoSeletivo", "0.00");
    }

    /** Operação de documento com CRT; itens na ordem recebida. */
    private static Map<String, Object> operacao(String tpDoc, String crt, String tpRegime,
            Map<String, Object> aliquotas, List<Map<String, Object>> itens) {
        Map<String, Object> operacao = mapa("id", "op-1", "versao", "3.0.0", "data", "2027-03-15", "tpDoc", tpDoc,
                "CRT", crt, "tpRegimeIBSCBS", tpRegime, "aliquotasEfetivas", aliquotas);
        operacao.put("itens", itens);
        return operacao;
    }

    private static Map<String, Object> par(String cbs, String ibs) {
        return mapa("pCBSSN", cbs, "pIBSSN", ibs);
    }

    /** Linha de alíquotas (pontos percentuais) do não assistido. */
    private static Map<String, Object> linha() {
        Map<String, Object> anexoIII = par("2.2154", "0.0257");
        anexoIII.put("semRedistribuicaoISS", par("1.99", "0.0231"));
        Map<String, Object> anexoIV = par("2.0119", "0.022");
        anexoIV.put("semRedistribuicaoISS", par("1.85", "0.0202"));
        return mapa("fatorR", true, "impedimentoSublimite", false, "anexoI", par("1.1612", "0.0129"),
                "anexoII", par("1.1184", "0.0121"), "anexoIII", anexoIII, "anexoIV", anexoIV,
                "anexoV", par("3.2534", "0.0363"));
    }

    private JsonNode enviar(String endpoint, Map<String, Object> operacao, int statusEsperado) throws Exception {
        MvcResult resultado = mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(operacao))).andReturn();
        JsonNode corpo = JSON.readTree(resultado.getResponse().getContentAsByteArray());
        assertEquals(statusEsperado, resultado.getResponse().getStatus(), corpo::toString);
        return corpo;
    }

    private static void assertNaoCalculado(JsonNode objeto, String codigo, String descricao) {
        assertEquals("NAO_CALCULADO", objeto.path("estadoItem").asText(), objeto::toString);
        assertEquals(codigo, objeto.at("/motivoEstado/codigo").asText(), objeto::toString);
        assertEquals(descricao, objeto.at("/motivoEstado/descricao").asText());
        assertTrue(objeto.path("erros").isMissingNode());
        // Ordem do item: nObj, estadoItem, motivoEstado, tribCalc.
        List<String> campos = new ArrayList<>();
        campos.addAll(objeto.propertyNames());
        assertEquals(List.of("nObj", "estadoItem", "motivoEstado", "tribCalc"), campos);
        assertEquals("1000.00", objeto.at("/tribCalc/vRBSNitem").asText());
    }

    private static void assertCalculadoSemMotivo(JsonNode objeto) {
        assertEquals("CALCULADO", objeto.path("estadoItem").asText(), objeto::toString);
        assertTrue(objeto.path("motivoEstado").isMissingNode(), objeto::toString);
    }

    // ─── Clássico: só os motivos de cAtivSN ────────────────────────────────────

    @Test
    void classico_motivoDoNaoCalculadoPorCAtivSN_eCalculadoSemMotivo() throws Exception {
        JsonNode resposta = enviar(ENDPOINT_CLASSICO, operacao("55", "1", null, null, List.of(
                itemClassico(1, "01"),
                itemClassico(2, null),
                itemClassico(3, "99"),
                itemClassico(4, "07"))), 200);

        assertCalculadoSemMotivo(resposta.at("/objetos/0"));
        assertNaoCalculado(resposta.at("/objetos/1"), "CATIVSN_AUSENTE", DESCRICAO_AUSENTE);
        assertNaoCalculado(resposta.at("/objetos/2"), "CATIVSN_DESCONHECIDO", DESCRICAO_DESCONHECIDO);
        assertNaoCalculado(resposta.at("/objetos/3"), "CATIVSN_INADMISSIVEL_NO_DOCUMENTO", DESCRICAO_INADMISSIVEL);
    }

    // ─── Não assistido ─────────────────────────────────────────────────────────

    @Test
    void naoAssistido_cAtivSNAusente() throws Exception {
        JsonNode resposta = enviar(ENDPOINT_NAO_ASSISTIDO, operacao("55", "1", null, linha(), List.of(
                itemCrt(1, "01"), itemCrt(2, null))), 200);
        assertCalculadoSemMotivo(resposta.at("/objetos/0"));
        assertNaoCalculado(resposta.at("/objetos/1"), "CATIVSN_AUSENTE", DESCRICAO_AUSENTE);
    }

    @Test
    void naoAssistido_cAtivSNDesconhecido() throws Exception {
        JsonNode resposta = enviar(ENDPOINT_NAO_ASSISTIDO, operacao("55", "1", null, linha(), List.of(
                itemCrt(1, "99"))), 200);
        assertNaoCalculado(resposta.at("/objetos/0"), "CATIVSN_DESCONHECIDO", DESCRICAO_DESCONHECIDO);
    }

    @Test
    void naoAssistido_atividadeAdmitidaSoEmOutroDocumento() throws Exception {
        // A atividade 07 só tem vínculo com a NFS-e (tpDoc 91) na tabela de atividades por documento.
        JsonNode resposta = enviar(ENDPOINT_NAO_ASSISTIDO, operacao("55", "1", null, linha(), List.of(
                itemCrt(1, "07"))), 200);
        assertNaoCalculado(resposta.at("/objetos/0"), "CATIVSN_INADMISSIVEL_NO_DOCUMENTO", DESCRICAO_INADMISSIVEL);
    }

    @Test
    void naoAssistido_regimeSomenteReceita_temPrecedenciaSobreOCAtivSN() throws Exception {
        // CRT 1 + tpRegimeIBSCBS 1: o optante apura o IBS e a CBS pelo regime regular. Mesmo com cAtivSN
        // ausente ou desconhecido, o motivo é o do regime (precedência).
        JsonNode resposta = enviar(ENDPOINT_NAO_ASSISTIDO, operacao("55", "1", "1", null, List.of(
                itemCrt(1, "01"), itemCrt(2, null), itemCrt(3, "99"))), 200);
        assertEquals("SOMENTE_RECEITA", resposta.at("/oper/regimeApuracao").asText());
        for (int i = 0; i < 3; i++) {
            assertNaoCalculado(resposta.at("/objetos/" + i), "REGIME_SOMENTE_RECEITA", DESCRICAO_SOMENTE_RECEITA);
        }
    }

    // ─── tpDoc sem tratamento na Calculadora (coluna da tabela de documentos) ──

    @ParameterizedTest(name = "tpDoc {0} ⇒ VSN-015")
    @ValueSource(strings = { "77", "94", "95", "96" })
    void tpDocSemTratamentoNaCalculadora_vsn015(String tpDoc) throws Exception {
        String mensagem = "Tipo de documento fiscal (tpDoc) de código " + tpDoc + " ainda não possui tratamento "
                + "implementado no cálculo do Simples Nacional.";

        JsonNode naoAssistido = enviar(ENDPOINT_NAO_ASSISTIDO, operacao(tpDoc, "1", null, linha(), List.of(
                itemCrt(1, "01"))), 422);
        assertEquals("VSN-015", naoAssistido.at("/errors/0/code").asText());
        assertEquals("#/tpDoc", naoAssistido.at("/errors/0/pointer").asText());
        assertEquals(mensagem, naoAssistido.at("/errors/0/detail").asText());

        JsonNode classico = enviar(ENDPOINT_CLASSICO, operacao(tpDoc, "1", null, null, List.of(
                itemClassico(1, "01"))), 422);
        assertEquals("VSN-015", classico.at("/errors/0/code").asText());
        assertEquals(mensagem, classico.at("/errors/0/detail").asText());
    }

    @ParameterizedTest(name = "tpDoc {0} ⇒ aceito")
    @ValueSource(strings = { "55", "65", "91" })
    void tpDocComTratamentoNaCalculadora_aceito(String tpDoc) throws Exception {
        Map<String, Object> operacao;
        if ("91".equals(tpDoc)) {
            operacao = mapa("id", "op-1", "versao", "3.0.0", "data", "2027-03-15", "tpDoc", "91", "opSimpNac", "3",
                    "regApIBSCBSSN", "1", "regApTribSN", "1", "aliquotasEfetivas", linha());
            operacao.put("itens", List.of(mapa("numero", 1, "cAtivSN", "07", "cClassTrib", "000001",
                    "baseCalculo", "1000.00", "tribISSQN", "1", "tpRetISSQN", "1")));
        } else {
            operacao = operacao(tpDoc, "1", null, linha(), List.of(itemCrt(1, "01")));
        }
        JsonNode resposta = enviar(ENDPOINT_NAO_ASSISTIDO, operacao, 200);
        assertCalculadoSemMotivo(resposta.at("/objetos/0"));
    }
}
