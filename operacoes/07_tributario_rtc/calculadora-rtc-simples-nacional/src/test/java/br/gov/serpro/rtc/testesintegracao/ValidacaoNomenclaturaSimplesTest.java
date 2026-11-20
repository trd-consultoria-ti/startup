package br.gov.serpro.rtc.testesintegracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Fronteira HTTP/SQLite para as validações de nomenclatura do Simples Nacional. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class ValidacaoNomenclaturaSimplesTest {
    private static final String NAO_ASSISTIDO = "/calculadora/observabilidade/simples-nacional/nao-assistido";
    private static final String CLASSICO = "/calculadora/observabilidade/simples-nacional";
    private static final ObjectMapper JSON = new ObjectMapper();
    @Autowired private MockMvc mockMvc;

    private JsonNode enviar(String rota, String corpo) throws Exception {
        var resposta = mockMvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo)).andReturn();
        assertEquals(200, resposta.getResponse().getStatus(), resposta.getResponse().getContentAsString());
        return JSON.readTree(resposta.getResponse().getContentAsByteArray());
    }

    private static String item(String codigo, String nomenclatura) {
        return """
                {"numero":1,"cAtivSN":"01","cClassTrib":"%s","baseCalculo":"500.00",
                 "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75"%s}
                """.formatted(codigo, nomenclatura == null ? "" : "," + nomenclatura);
    }

    private static String naoAssistido(String primeiroItem) {
        return """
                {"id":"nomenclatura","versao":"3.0.0","data":"2027-03-15","tpDoc":"55","CRT":"1",
                 "aliquotasEfetivas":{"fatorR":true,"impedimentoSublimite":false,
                 "anexoI":{"pCBSSN":"1.1612","pIBSSN":"0.0129"}},"itens":[%s,
                 {"numero":2,"cAtivSN":"01","cClassTrib":"000001","baseCalculo":"1000.00",
                  "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75"}]}
                """.formatted(primeiroItem);
    }

    private static String classico(String primeiroItem) {
        return """
                {"id":"nomenclatura","versao":"2.0.0","data":"2027-03-15","tpDoc":"55","CRT":"1",
                 "itens":[%s,{"numero":2,"cAtivSN":"01","cClassTrib":"000001","baseCalculo":"1000.00",
                  "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75"}]}
                """.formatted(primeiroItem);
    }

    private static String operacaoClassica(String tpDoc, String primeiroItem) {
        return """
                {"id":"nomenclatura","versao":"2.0.0","data":"2027-03-15","tpDoc":"%s","CRT":"1",
                 "itens":[%s]}
                """.formatted(tpDoc, primeiroItem);
    }

    private void assertFalhaDeItem(JsonNode corpo, String code, String pointer) {
        assertEquals("INCONSISTENCIA_ENTRADA", corpo.at("/objetos/0/estadoItem").asText());
        assertEquals(code, corpo.at("/objetos/0/erros/0/code").asText());
        assertEquals(pointer, corpo.at("/objetos/0/erros/0/pointer").asText());
        assertEquals("CALCULADO", corpo.at("/objetos/1/estadoItem").asText());
        assertEquals("1000.00", corpo.at("/total/vRBSNtot").asText());
        assertEquals("1000.00", corpo.at("/total/vBC").asText());
    }

    @Test
    void ausenciaObrigatoriaNcmNoNaoAssistido() throws Exception {
        assertFalhaDeItem(enviar(NAO_ASSISTIDO, naoAssistido(item("200003", null))), "VSN-042",
                "#/itens/0/ncm");
    }

    @Test
    void ncmInexistenteNoBancoRealNoNaoAssistido() throws Exception {
        JsonNode corpo = enviar(NAO_ASSISTIDO, naoAssistido(item("200003", "\"ncm\":\"99999999\"")));
        assertFalhaDeItem(corpo, "VSN-049", "#/itens/0/ncm");
        assertTrue(corpo.at("/objetos/0/erros/0/detail").asText().contains("99999999"));
    }

    @Test
    void formatoEstruturalNcmNbsEValidadoAntesDaExistenciaEImpedeCalculo() throws Exception {
        for (String value : new String[] {"1234567", "123456789", "1234ABCD", "1234-678", "1234 678", "        ", "１２３４５６７８"}) {
            JsonNode corpo = enviar(CLASSICO, classico(item("000001", "\"ncm\":\"" + value + "\"")));
            assertFalhaDeItem(corpo, "VSN-051", "#/itens/0/ncm");
        }
        for (String value : new String[] {"12345678", "12345678x", "12345 789", "1234-6789", "         ", "１２３４５６７８９"}) {
            JsonNode corpo = enviar(CLASSICO, classico(item("000001", "\"nbs\":\"" + value + "\"")));
            assertFalhaDeItem(corpo, "VSN-051", "#/itens/0/nbs");
        }
        JsonNode semNomenclatura = enviar(CLASSICO, classico(item("000001", null)));
        assertEquals("CALCULADO", semNomenclatura.at("/objetos/0/estadoItem").asText());
    }

    @Test
    void nomenclaturasSimultaneasNoClassico() throws Exception {
        JsonNode corpo = enviar(CLASSICO, classico(item("200003", "\"ncm\":\"10062010\",\"nbs\":\"114051000\"")));
        assertFalhaDeItem(corpo, "VSN-041", "#/itens/0/ncm");
    }

    @Test
    void ncmVigenteEAplicavelNoBancoRealCalculaNosDoisEndpoints() throws Exception {
        JsonNode novo = enviar(NAO_ASSISTIDO, naoAssistido(item("200003", "\"ncm\":\"10062010\"")));
        assertEquals("CALCULADO", novo.at("/objetos/0/estadoItem").asText());
        assertTrue(novo.at("/objetos/0/erros").isMissingNode());
        assertEquals("1500.00", novo.at("/total/vRBSNtot").asText());
        assertEquals("1500.00", novo.at("/total/vBC").asText());

        JsonNode classicoValido = enviar(CLASSICO, classico(item("200003", "\"ncm\":\"10062010\"")));
        assertEquals("CALCULADO", classicoValido.at("/objetos/0/estadoItem").asText());
        assertTrue(classicoValido.at("/objetos/0/erros").isMissingNode());
        assertEquals("1500.00", classicoValido.at("/total/vRBSNtot").asText());
        assertFalse(classicoValido.path("errors").isArray());
    }

    @Test
    void aliasMaiusculoNcmContinuaAceitoPorCompatibilidade() throws Exception {
        JsonNode corpo = enviar(CLASSICO, classico(item("200003", "\"NCM\":\"10062010\"")));
        assertEquals("CALCULADO", corpo.at("/objetos/0/estadoItem").asText());
        assertTrue(corpo.at("/objetos/0/erros").isMissingNode());
    }

    @Test
    void ncmZeroExatoEhEquivalenteAAusenciaQuandoPermitidoNaNfeENfce() throws Exception {
        // TPDF do SQLite de testes: NF-e (55) e NFC-e (65) permitem NCM zero desde 2026-01-01.
        for (String tpDoc : new String[] {"55", "65"}) {
            JsonNode ausente = enviar(CLASSICO, operacaoClassica(tpDoc, item("200003", null)));
            JsonNode doisDigitos = enviar(CLASSICO,
                    operacaoClassica(tpDoc, item("200003", "\"ncm\":\"00\"")));
            JsonNode oitoDigitos = enviar(CLASSICO,
                    operacaoClassica(tpDoc, item("200003", "\"ncm\":\"00000000\"")));
            assertEquals("VSN-042", ausente.at("/objetos/0/erros/0/code").asText());
            assertEquals(ausente.at("/objetos/0/erros").toString(), doisDigitos.at("/objetos/0/erros").toString());
            assertEquals(ausente.at("/objetos/0/erros").toString(), oitoDigitos.at("/objetos/0/erros").toString());
        }
    }

    @Test
    void ncmZeroNoNaoAssistidoEhEquivalenteAAusencia() throws Exception {
        JsonNode ausente = enviar(NAO_ASSISTIDO, naoAssistido(item("000001", null)));
        JsonNode zerado = enviar(NAO_ASSISTIDO, naoAssistido(item("000001", "\"ncm\":\"00000000\"")));
        assertEquals("CALCULADO", ausente.at("/objetos/0/estadoItem").asText());
        assertEquals(ausente.at("/objetos/0"), zerado.at("/objetos/0"));
        assertEquals(ausente.at("/total"), zerado.at("/total"));
    }

    @Test
    void ncmZeroEhNormalizadoAntesDaValidacaoDeNomenclaturaSimultanea() throws Exception {
        // NF-e/NFC-e permitem NCM zero; o NBS pode ser incompatível com esses DF-e,
        // mas a presença do zero não pode restaurar o erro estrutural VSN-041.
        for (String tpDoc : new String[] {"55", "65"}) {
            String itemComNbs = item("200003", "\"nbs\":\"122012000\"");
            String itemComNcmZeroENbs = item("200003", "\"ncm\":\"00\",\"nbs\":\"122012000\"");
            JsonNode nbsSozinho = enviar(CLASSICO, operacaoClassica(tpDoc, itemComNbs));
            JsonNode ncmZeroENbs = enviar(CLASSICO, operacaoClassica(tpDoc, itemComNcmZeroENbs));
            assertEquals(nbsSozinho.at("/objetos/0/estadoItem").asText(),
                    ncmZeroENbs.at("/objetos/0/estadoItem").asText());
            assertEquals(nbsSozinho.at("/objetos/0/erros").toString(),
                    ncmZeroENbs.at("/objetos/0/erros").toString());
            assertFalse(ncmZeroENbs.at("/objetos/0/erros").toString().contains("VSN-041"));
        }
    }

    @Test
    void ncmZeroNaoEhAceitoPorDocumentoSemPermissaoEValoresNaoExatosNaoSaoNormalizados() throws Exception {
        // CT-e (57) tem tratamento ativo no catálogo, mas não permite NCM zero.
        JsonNode semPermissao = enviar(CLASSICO, operacaoClassica("57", item("200003", "\"ncm\":\"00\"")));
        assertEquals("INCONSISTENCIA_ENTRADA", semPermissao.at("/objetos/0/estadoItem").asText());
        assertEquals("VSN-051", semPermissao.at("/objetos/0/erros/0/code").asText());

        for (String ncm : new String[] {"000000000", " 00 "}) {
            JsonNode naoExato = enviar(CLASSICO,
                    operacaoClassica("55", item("200003", "\"ncm\":\"" + ncm + "\"")));
            assertEquals("INCONSISTENCIA_ENTRADA", naoExato.at("/objetos/0/estadoItem").asText(), ncm);
            assertFalse(naoExato.at("/objetos/0/erros").isMissingNode());
            assertFalse("VSN-042".equals(naoExato.at("/objetos/0/erros/0/code").asText()), ncm);
        }
    }

    @Test
    void NbsInformadoEmNfeEhIncompativelComDocumentoEApontaNbs() throws Exception {
        JsonNode corpo = enviar(CLASSICO, classico(item("000001", "\"nbs\":\"122012000\"")));
        assertFalhaDeItem(corpo, "VSN-045", "#/itens/0/nbs");
    }

    @Test
    void NcmInformadoEmNfseEhIncompativelComDocumentoEApontaNcm() throws Exception {
        String itemNfse = """
                {"numero":1,"cAtivSN":"07","cClassTrib":"000001","baseCalculo":"500.00",
                 "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75",
                 "ncm":"10062010","tribISSQN":"1","tpRetISSQN":"1"}
                """;
        JsonNode corpo = enviar(CLASSICO, operacaoClassica("91", itemNfse));
        assertEquals("INCONSISTENCIA_ENTRADA", corpo.at("/objetos/0/estadoItem").asText());
        assertEquals("VSN-045", corpo.at("/objetos/0/erros/0/code").asText());
        assertEquals("#/itens/0/ncm", corpo.at("/objetos/0/erros/0/pointer").asText());
    }

    @Test
    void baseNegativaENomenclaturasSimultaneas_ApontamBaseAntesDoErroEstrutural() throws Exception {
        String itemInvalido = """
                {"numero":1,"cAtivSN":"01","cClassTrib":"200003","baseCalculo":"-1.00",
                 "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75",
                  "ncm":"10062010","nbs":"122012000"}
                """;
        JsonNode corpo = enviar(CLASSICO, operacaoClassica("55", itemInvalido));
        JsonNode erros = corpo.at("/objetos/0/erros");
        assertEquals("INCONSISTENCIA_ENTRADA", corpo.at("/objetos/0/estadoItem").asText());
        assertTrue(erros.size() >= 2, erros::toString);
        assertEquals("VSN-002", erros.get(0).path("code").asText());
        assertEquals("#/itens/0/baseCalculo", erros.get(0).path("pointer").asText());
        assertEquals("VSN-041", erros.get(1).path("code").asText());
        assertEquals("#/itens/0/ncm", erros.get(1).path("pointer").asText());
    }

    @Test
    void nbsVigenteEAplicavelEmNfseCalculaNoBancoReal() throws Exception {
        String itemNfse = """
                {"numero":1,"cAtivSN":"07","cClassTrib":"200028","baseCalculo":"500.00",
                 "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75",
                  "nbs":"122012000","tribISSQN":"1","tpRetISSQN":"1"}
                """;
        JsonNode resposta = enviar(CLASSICO, operacaoClassica("91", itemNfse));
        assertEquals("CALCULADO", resposta.at("/objetos/0/estadoItem").asText());
        assertTrue(resposta.at("/objetos/0/erros").isMissingNode());
        assertEquals("500.00", resposta.at("/total/vRBSNtot").asText());
    }

    @Test
    void aliasMaiusculoNbsEmNfseProduzMesmoResultadoQueNomeCanonico() throws Exception {
        String itemNfseMinusculo = """
                {"numero":1,"cAtivSN":"07","cClassTrib":"200028","baseCalculo":"500.00",
                 "valorImpostoSeletivo":"0.00","pIBSSN":"1.25","pCBSSN":"2.75",
                 "nbs":"122012000","tribISSQN":"1","tpRetISSQN":"1"}
                """;
        String itemNfseMaiusculo = itemNfseMinusculo.replace("\"nbs\"", "\"NBS\"");

        JsonNode minusculo = enviar(CLASSICO, operacaoClassica("91", itemNfseMinusculo));
        JsonNode maiusculo = enviar(CLASSICO, operacaoClassica("91", itemNfseMaiusculo));

        assertEquals("CALCULADO", minusculo.at("/objetos/0/estadoItem").asText());
        assertEquals("CALCULADO", maiusculo.at("/objetos/0/estadoItem").asText());
        assertEquals(minusculo.at("/objetos/0"), maiusculo.at("/objetos/0"));
        assertEquals(minusculo.at("/total"), maiusculo.at("/total"));
    }
}
