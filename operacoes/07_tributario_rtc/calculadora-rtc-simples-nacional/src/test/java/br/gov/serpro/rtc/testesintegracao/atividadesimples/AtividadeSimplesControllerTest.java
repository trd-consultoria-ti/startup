package br.gov.serpro.rtc.testesintegracao.atividadesimples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
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

/**
 * Testes de integração do endpoint de atividades × anexos do Simples
 * Nacional ({@code calculadora/simples-nacional/atividades}), incluindo os
 * filtros-por-flag via parâmetros {@code data} (obrigatório), {@code anexos},
 * {@code tpDoc} (admissibilidade por documento fiscal, associativa
 * {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL} com semântica de whitelist) e
 * {@code cClassTrib} (compatibilidade com a classificação tributária, espelho
 * da regra UB14-90), todos opcionais.
 *
 * <p>A data de referência ({@code data}) é query param obrigatório em todas as
 * consultas: como em todos os endpoints do Simples Nacional, os lookups de
 * vigência usam a data informada pelo cliente, nunca a data corrente do
 * servidor.
 *
 * <p>Catálogo oficial: 15 linhas — os códigos '01'..'14' e a linha do código
 * '90' (Operações não Tributáveis), sem anexos. Carga da associativa
 * (whitelist): '01'–'04' vinculadas a NF-e/NFC-e (55/65); '07'–'14' à NFS-e
 * (91); '05' aos documentos de transporte das NTs 2026.003 (CT-e 57, BP-e 63,
 * CT-e OS 67 e BP-e TM 93); '06' à NFCom (62); '90' a todos os documentos
 * habilitados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class AtividadeSimplesControllerTest {

    private static final String ENDPOINT = "/calculadora/simples-nacional/atividades";
    private static final String DATA_PADRAO = "2027-01-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveListarAsQuinzeAtividadesSemFiltro() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(15)))
                .andExpect(jsonPath("$[0].codigo").value("01"))
                .andExpect(jsonPath("$[0].ordem").value(1))
                .andExpect(jsonPath("$[0].anexoSemFatorR").value("I"))
                .andExpect(jsonPath("$[0].anexoComFatorR").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[0].sujeitaFatorR").value(false))
                .andExpect(jsonPath("$[0].rotuloAnexo").value("I"))
                .andExpect(jsonPath("$[0].rotuloCurtoAnexo").value("I"))
                .andExpect(jsonPath("$[0].disponivel").value(true))
                .andExpect(jsonPath("$[0].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[0].modoFatorR").value("NAO_APLICAVEL"))
                .andExpect(jsonPath("$[1].rotuloAnexo").value("I, menos ICMS"))
                .andExpect(jsonPath("$[1].rotuloCurtoAnexo").value("I"))
                // atividade 09 é a única sujeita ao Fator R: sem filtro, decisão em aberto
                .andExpect(jsonPath("$[8].codigo").value("09"))
                .andExpect(jsonPath("$[8].disponivel").value(true))
                .andExpect(jsonPath("$[8].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[8].modoFatorR").value("OPCIONAL"))
                .andExpect(jsonPath("$[13].codigo").value("14"))
                .andExpect(jsonPath("$[13].ordem").value(14))
                // código 90 (operações não tributáveis): sem anexo, disponível pelos anexos
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].anexoSemFatorR").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[14].anexoComFatorR").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[14].sujeitaFatorR").value(false))
                .andExpect(jsonPath("$[14].disponivel").value(true))
                .andExpect(jsonPath("$[14].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[14].modoFatorR").value("NAO_APLICAVEL"));
    }

    @Test
    void deveMarcarDisponibilidadeComFiltroDeAnexosIeV() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", "I,V"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(15)))
                // atividade 02 (anexo I): disponível, nada falta, Fator R não se aplica
                .andExpect(jsonPath("$[1].codigo").value("02"))
                .andExpect(jsonPath("$[1].disponivel").value(true))
                .andExpect(jsonPath("$[1].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[1].modoFatorR").value("NAO_APLICAVEL"))
                // atividade 09 (sem Fator R = V; com Fator R = III): com I e V configurados,
                // só o caminho SEM Fator R (Anexo V) está disponível — o caminho com
                // Fator R (Anexo III, fator r >= 28%) fica indisponível
                .andExpect(jsonPath("$[8].codigo").value("09"))
                .andExpect(jsonPath("$[8].disponivel").value(true))
                .andExpect(jsonPath("$[8].anexosFaltantes", Matchers.contains("III")))
                .andExpect(jsonPath("$[8].modoFatorR").value("INDISPONIVEL"))
                // atividade 05 (anexo III): não omitida, apenas marcada indisponível
                .andExpect(jsonPath("$[4].codigo").value("05"))
                .andExpect(jsonPath("$[4].disponivel").value(false))
                .andExpect(jsonPath("$[4].anexosFaltantes", Matchers.contains("III")))
                .andExpect(jsonPath("$[4].modoFatorR").value("NAO_APLICAVEL"));
    }

    @Test
    void deveAceitarParametroAnexosRepetido() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", "III").param("anexos", "V"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[8].codigo").value("09"))
                .andExpect(jsonPath("$[8].disponivel").value(true))
                .andExpect(jsonPath("$[8].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[8].modoFatorR").value("OPCIONAL"));
    }

    @Test
    void deveMarcarFatorRObrigatorioQuandoSoOAnexoIIIEstaConfigurado() throws Exception {
        // Só o Anexo III configurado: para a 09 é o anexo COM Fator R (fator
        // r >= 28%) — o caminho do Fator R torna-se obrigatório e o Anexo V
        // (sem Fator R) é o faltante.
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", "III"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[8].codigo").value("09"))
                .andExpect(jsonPath("$[8].disponivel").value(true))
                .andExpect(jsonPath("$[8].anexosFaltantes", Matchers.contains("V")))
                .andExpect(jsonPath("$[8].modoFatorR").value("OBRIGATORIO"));
    }

    @Test
    void deveTratarAnexosVazioComoNenhumConfigurado() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(15)))
                // atividade 09: nenhum anexo configurado, indisponível
                .andExpect(jsonPath("$[8].disponivel").value(false))
                .andExpect(jsonPath("$[8].anexosFaltantes", Matchers.contains("III", "V")))
                .andExpect(jsonPath("$[8].modoFatorR").value("INDISPONIVEL"))
                // código 90 (sem anexo): disponível pelo eixo dos anexos
                .andExpect(jsonPath("$[14].disponivel").value(true))
                .andExpect(jsonPath("$[14].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[14].modoFatorR").value("NAO_APLICAVEL"));
    }

    // ─── Admissibilidade por documento fiscal (tpDoc opcional) ─────────────────

    @Test
    void semTpDocDeveDevolverTpDocsAdmissiveisSemAdmissivelNoDocumento() throws Exception {
        // Sem tpDoc, o campo aditivo tpDocsAdmissiveis é sempre presente;
        // admissivelNoDocumento NÃO aparece.
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("01"))
                .andExpect(jsonPath("$[0].tpDocsAdmissiveis", Matchers.contains("55", "65")))
                .andExpect(jsonPath("$[0].admissivelNoDocumento").doesNotExist())
                .andExpect(jsonPath("$[0].disponivel").value(true))
                .andExpect(jsonPath("$[4].codigo").value("05"))
                .andExpect(jsonPath("$[4].tpDocsAdmissiveis", Matchers.contains("57", "63", "67", "93")))
                .andExpect(jsonPath("$[4].admissivelNoDocumento").doesNotExist())
                .andExpect(jsonPath("$[5].codigo").value("06"))
                .andExpect(jsonPath("$[5].tpDocsAdmissiveis", Matchers.contains("62")))
                .andExpect(jsonPath("$[8].codigo").value("09"))
                .andExpect(jsonPath("$[8].tpDocsAdmissiveis", Matchers.contains("91")))
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].tpDocsAdmissiveis",
                        Matchers.contains("55", "57", "62", "63", "65", "67", "91", "93")));
    }

    @Test
    void comTpDoc55DeveMarcarAdmissibilidadeConformeAWhitelist() throws Exception {
        // Em NF-e (55): '01'–'04' e '90' admissíveis; '05'/'06' (sem vínculo
        // algum) e '07'–'14' (vinculadas só à NFS-e) inadmissíveis, com
        // anexosFaltantes vazio (o motivo é só o documento).
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(15)))
                .andExpect(jsonPath("$[0].codigo").value("01"))
                .andExpect(jsonPath("$[0].admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$[0].disponivel").value(true))
                .andExpect(jsonPath("$[4].codigo").value("05"))
                .andExpect(jsonPath("$[4].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[4].disponivel").value(false))
                .andExpect(jsonPath("$[4].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[8].codigo").value("09"))
                .andExpect(jsonPath("$[8].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[8].disponivel").value(false))
                .andExpect(jsonPath("$[12].codigo").value("13"))
                .andExpect(jsonPath("$[12].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[12].disponivel").value(false))
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$[14].disponivel").value(true));
    }

    @Test
    void comTpDoc91DeveMarcarVinculadasANfeNfceComoInadmissiveis() throws Exception {
        // Em NFS-e (91): '01'–'04' inadmissíveis e indisponíveis MESMO com
        // anexos ok (anexosFaltantes vazio distingue o motivo); '07'–'14' e o
        // '90' admissíveis; '05'/'06' seguem inadmissíveis (sem vínculo algum).
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", "91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("01"))
                .andExpect(jsonPath("$[0].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[0].disponivel").value(false))
                .andExpect(jsonPath("$[0].anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$[0].modoFatorR").value("NAO_APLICAVEL"))
                .andExpect(jsonPath("$[3].codigo").value("04"))
                .andExpect(jsonPath("$[3].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[3].disponivel").value(false))
                .andExpect(jsonPath("$[4].codigo").value("05"))
                .andExpect(jsonPath("$[4].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[6].codigo").value("07"))
                .andExpect(jsonPath("$[6].admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$[6].disponivel").value(true))
                .andExpect(jsonPath("$[11].codigo").value("12"))
                .andExpect(jsonPath("$[11].admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$[12].codigo").value("13"))
                .andExpect(jsonPath("$[12].admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$[14].disponivel").value(true))
                .andExpect(jsonPath("$[14].modoFatorR").value("NAO_APLICAVEL"));
    }

    @Test
    void tpDocValidoNaoHabilitadoParaOCalculo_aceitoENenhumaAtividadeAdmissivel() throws Exception {
        // tpDoc 97 (CT-e Simplificado) existe na TIPO_DFE mas NÃO está na
        // whitelist do cálculo (não abrangido pela NT 2026.003 do CT-e) — o
        // endpoint de atividades aceita (as camadas são deliberadamente
        // distintas) e deriva a admissibilidade pela associativa: nenhuma
        // atividade tem vínculo com o 97, então todas vêm inadmissíveis
        // (whitelist: sem vínculo, não há permissão).
        String json = mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", "97"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode atividades = objectMapper.readTree(json);
        assertEquals(15, atividades.size());
        for (JsonNode atividade : atividades) {
            assertEquals(false, atividade.get("admissivelNoDocumento").asBoolean(),
                    "nenhuma atividade deveria ser admissível em tpDoc 97");
            assertEquals(false, atividade.get("disponivel").asBoolean());
        }
    }

    @Test
    void deveMarcarOsDoisMotivosQuandoInadmissivelEComAnexosFaltantes() throws Exception {
        // '01' (anexo I) consultada em NFS-e com apenas o anexo II configurado —
        // inadmissível no documento E com anexo faltante, os dois motivos
        // preenchidos e independentes.
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", "II").param("tpDoc", "91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("01"))
                .andExpect(jsonPath("$[0].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[0].anexosFaltantes", Matchers.contains("I")))
                .andExpect(jsonPath("$[0].disponivel").value(false))
                // contraprova: '13' é admissível em 91 independentemente da
                // configuração de anexos — os dois eixos não se contaminam.
                .andExpect(jsonPath("$[12].codigo").value("13"))
                .andExpect(jsonPath("$[12].admissivelNoDocumento").value(true));
    }

    @Test
    void deveRetornar400ParaTpDocMalFormado() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", Matchers.containsString("abc")));
    }

    @Test
    void deveRetornar400ParaTpDocSemLinhaNaTipoDfe() throws Exception {
        // '99' é numérico mas não existe em TIPO_DFE.TPDF_TIPO.
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", Matchers.containsString("99")));
    }

    @Test
    void buscaPorCodigoDeveReceberOMesmoTratamentoDeTpDoc() throws Exception {
        // O GET /{codigo} recebe tpDoc opcional + os mesmos campos.
        mockMvc.perform(get(ENDPOINT + "/01").param("data", DATA_PADRAO).param("tpDoc", "91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("01"))
                .andExpect(jsonPath("$.tpDocsAdmissiveis", Matchers.contains("55", "65")))
                .andExpect(jsonPath("$.admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$.disponivel").value(false))
                .andExpect(jsonPath("$.anexosFaltantes", Matchers.empty()));

        mockMvc.perform(get(ENDPOINT + "/01").param("data", DATA_PADRAO).param("tpDoc", "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissivelNoDocumento").value(true))
                .andExpect(jsonPath("$.disponivel").value(true));

        // Sem tpDoc: campo aditivo presente, admissivelNoDocumento ausente.
        mockMvc.perform(get(ENDPOINT + "/05").param("data", DATA_PADRAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tpDocsAdmissiveis", Matchers.contains("57", "63", "67", "93")))
                .andExpect(jsonPath("$.admissivelNoDocumento").doesNotExist());

        mockMvc.perform(get(ENDPOINT + "/01").param("data", DATA_PADRAO).param("tpDoc", "99"))
                .andExpect(status().isBadRequest());
    }

    // ─── Compatibilidade com a classificação tributária (cClassTrib opcional) ──

    @Test
    void cClassTribQueDeclaraReceita_marcaSomenteONaoTributavelIndisponivel() throws Exception {
        // 000001 tem tpRBSN=1 (declara receita, vedado para não tributáveis):
        // somente o '90' vem incompatível e indisponível — as demais atividades
        // não são afetadas (nenhuma regra além da UB14-90).
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("cClassTrib", "000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(15)))
                .andExpect(jsonPath("$[0].codigo").value("01"))
                .andExpect(jsonPath("$[0].compativelComClassTrib").value(true))
                .andExpect(jsonPath("$[0].disponivel").value(true))
                .andExpect(jsonPath("$[4].codigo").value("05"))
                .andExpect(jsonPath("$[4].compativelComClassTrib").value(true))
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].compativelComClassTrib").value(false))
                .andExpect(jsonPath("$[14].disponivel").value(false))
                .andExpect(jsonPath("$[14].anexosFaltantes", Matchers.empty()));
    }

    @Test
    void cClassTribDeTpRBSN0e9_naoMarcaAtividadeAlguma() throws Exception {
        // tpRBSN 0 (410001, não é receita bruta) e 9 (010001, fornecimento
        // incompatível com SN) não são vedados para não tributáveis: nenhuma
        // atividade fica indisponível — nem o '90'.
        for (String cClassTrib : List.of("410001", "010001")) {
            String json = mockMvc
                    .perform(get(ENDPOINT).param("data", DATA_PADRAO).param("cClassTrib", cClassTrib))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode atividades = objectMapper.readTree(json);
            assertEquals(15, atividades.size());
            for (JsonNode atividade : atividades) {
                assertEquals(true, atividade.get("compativelComClassTrib").asBoolean(),
                        () -> "cClassTrib " + cClassTrib + " não deveria marcar a atividade "
                                + atividade.get("codigo").asText());
                assertEquals(true, atividade.get("disponivel").asBoolean());
            }
        }
    }

    @Test
    void motivosDeDocumentoEDeClassificacaoCombinados() throws Exception {
        // tpDoc 97 (sem vínculo com atividade alguma) + cClassTrib 000001
        // (tpRBSN=1): o '90' vem indisponível pelos DOIS motivos, distinguíveis
        // e independentes.
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", "97")
                .param("cClassTrib", "000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[14].compativelComClassTrib").value(false))
                .andExpect(jsonPath("$[14].disponivel").value(false))
                // '01' só pelo documento: compatível com a classificação.
                .andExpect(jsonPath("$[0].admissivelNoDocumento").value(false))
                .andExpect(jsonPath("$[0].compativelComClassTrib").value(true))
                .andExpect(jsonPath("$[0].disponivel").value(false));
    }

    @Test
    void cClassTribResolvidoPelaLinhaDaCbsQuandoCodigoColideComImpostoSeletivo() throws Exception {
        // Em 2027, o código 000001 existe nos DOIS grupos da classificação
        // tributária (CBS com tpRBSN=1 e Imposto Seletivo com tpRBSN inerte 0).
        // A resolução é restrita ao grupo da CBS: o '90' DEVE vir marcado
        // incompatível (se a linha do IS vencesse, o tpRBSN seria 0 e nada
        // seria marcado).
        mockMvc.perform(get(ENDPOINT).param("data", "2027-03-15").param("cClassTrib", "000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[14].codigo").value("90"))
                .andExpect(jsonPath("$[14].compativelComClassTrib").value(false))
                .andExpect(jsonPath("$[14].disponivel").value(false));
    }

    @Test
    void semCClassTrib_respostaNaoTemCampoDeCompatibilidade() throws Exception {
        String json = mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode atividade : objectMapper.readTree(json)) {
            assertEquals(false, atividade.has("compativelComClassTrib"),
                    "sem cClassTrib o campo não deve aparecer");
        }
    }

    @Test
    void deveRetornar400ParaCClassTribInexistente() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("cClassTrib", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", Matchers.containsString("999999")));
    }

    @Test
    void deveRetornar400ParaCClassTribForaDeVigenciaNaData() throws Exception {
        // 000001 só vige a partir de 2027-01-01: em data anterior, o filtro não
        // pode ser resolvido e a consulta falha (nunca é ignorado em silêncio).
        mockMvc.perform(get(ENDPOINT).param("data", "2025-12-31").param("cClassTrib", "000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void buscaPorCodigoDeveReceberOMesmoTratamentoDeCClassTrib() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/90").param("data", DATA_PADRAO).param("cClassTrib", "000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("90"))
                .andExpect(jsonPath("$.compativelComClassTrib").value(false))
                .andExpect(jsonPath("$.disponivel").value(false));

        mockMvc.perform(get(ENDPOINT + "/01").param("data", DATA_PADRAO).param("cClassTrib", "000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compativelComClassTrib").value(true))
                .andExpect(jsonPath("$.disponivel").value(true));
    }

    // ─── Regras gerais do endpoint ─────────────────────────────────────────────

    @Test
    void deveListarVazioParaDataAnteriorATodaVigencia() throws Exception {
        // A vigência do catálogo começa em 2027-01-01: consulta anterior à
        // vigência retorna lista vazia (nunca se usa a data corrente do servidor).
        mockMvc.perform(get(ENDPOINT).param("data", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    void deveRetornar400QuandoDataNaoInformada() throws Exception {
        // O query param data é obrigatório em todos os endpoints do Simples.
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ParaDataMalFormada() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", "01/01/2027"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ComAnexoForaDoDominio() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", "I,X"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ComAnexoMinusculo() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("anexos", "i"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExporAtividade9SujeitaAFatorR() throws Exception {
        // Fator r >= 28% => Anexo III (anexoComFatorR); fator r < 28% => Anexo V
        // (anexoSemFatorR) — LC 123/2006, art. 18, §§ 5º-J e 5º-M; Resolução CGSN
        // 190/2026 (carga corrigida pela migração V0061).
        mockMvc.perform(get(ENDPOINT + "/09").param("data", DATA_PADRAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("09"))
                .andExpect(jsonPath("$.ordem").value(9))
                .andExpect(jsonPath("$.anexoSemFatorR").value("V"))
                .andExpect(jsonPath("$.anexoComFatorR").value("III"))
                .andExpect(jsonPath("$.sujeitaFatorR").value(true))
                .andExpect(jsonPath("$.rotuloAnexo").value("III ou V"))
                .andExpect(jsonPath("$.rotuloCurtoAnexo").value("III ou V"))
                // derivados preenchidos como "sem filtro"
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$.modoFatorR").value("OPCIONAL"));
    }

    @Test
    void deveExporLinhaDoCodigo90SemAnexos() throws Exception {
        // A linha do código 90 (Operações não Tributáveis) aparece na listagem
        // como item normal, com ambos os anexos nulos (decisão deliberada).
        mockMvc.perform(get(ENDPOINT + "/90").param("data", DATA_PADRAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("90"))
                .andExpect(jsonPath("$.ordem").value(15))
                .andExpect(jsonPath("$.descricao").value("Operações não Tributáveis"))
                .andExpect(jsonPath("$.anexoSemFatorR").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.anexoComFatorR").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.sujeitaFatorR").value(false))
                .andExpect(jsonPath("$.rotuloAnexo").value("Não tributável"))
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.anexosFaltantes", Matchers.empty()))
                .andExpect(jsonPath("$.modoFatorR").value("NAO_APLICAVEL"));
    }

    @Test
    void deveRetornar404ParaCodigoInexistente() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/99").param("data", DATA_PADRAO))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400SemDataNaBuscaPorCodigo() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/09"))
                .andExpect(status().isBadRequest());
    }

    // ─── Coerência com o cálculo ───────────────────────────────────────────────

    /**
     * A admissibilidade devolvida por ESTE endpoint precisa coincidir com o
     * comportamento do CÁLCULO (a consulta da associativa é duplicada
     * deliberadamente entre os dois pontos — este teste é a trava de coerência
     * referida nos comentários de
     * {@code AtividadeSimplesRepository#buscarVinculosTpDocVigentes}).
     *
     * <p>Para cada tpDoc habilitado no cálculo (55, 57, 62, 63, 65, 67, 91 e
     * 93), consulta a listagem com o tpDoc e envia UMA operação de cálculo com
     * um item por atividade do catálogo: atividade inadmissível aqui ⇒ item
     * {@code NAO_CALCULADO} lá; admissível ⇒ item {@code CALCULADO}. O
     * cClassTrib usado é {@code 410999} (tpRBSN=0, "não é receita bruta"):
     * dispensa alíquotas, não colide com a regra UB14-90 do código '90' e é o
     * ÚNICO código de tpRBSN=0 admissível nos oito documentos (o BP-e TM
     * admite somente o 410999) — o estado do item fica em função apenas do
     * cAtivSN.
     */
    @Test
    void admissibilidadeCoerenteComOCalculoParaTodaACargaAtual() throws Exception {
        for (String tpDoc : List.of("55", "57", "62", "63", "65", "67", "91", "93")) {
            String atividadesJson = mockMvc.perform(get(ENDPOINT).param("data", DATA_PADRAO).param("tpDoc", tpDoc))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode atividades = objectMapper.readTree(atividadesJson);
            assertEquals(15, atividades.size());

            StringBuilder itens = new StringBuilder();
            for (int i = 0; i < atividades.size(); i++) {
                if (i > 0) {
                    itens.append(',');
                }
                itens.append("""
                        {
                          "numero": %d,
                          "cAtivSN": "%s",
                          "cClassTrib": "410999",
                          "baseCalculo": "100.00",
                          "valorImpostoSeletivo": "0.00"
                        }
                        """.formatted(i + 1, atividades.get(i).get("codigo").asText()));
            }
            String operacao = """
                    {
                      "id": "507f1f77bcf86cd799439011",
                      "versao": "2.0.0",
                      "data": "%s",
                      "tpDoc": "%s",
                      "CRT": "1",
                      "itens": [%s]
                    }
                    """.formatted(DATA_PADRAO, tpDoc, itens);

            String calculoJson = mockMvc
                    .perform(post("/calculadora/observabilidade/simples-nacional")
                            .contentType(MediaType.APPLICATION_JSON).content(operacao))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode objetos = objectMapper.readTree(calculoJson).get("objetos");
            assertEquals(atividades.size(), objetos.size());

            for (int i = 0; i < atividades.size(); i++) {
                String codigo = atividades.get(i).get("codigo").asText();
                boolean admissivel = atividades.get(i).get("admissivelNoDocumento").asBoolean();
                String estado = objetos.get(i).get("estadoItem").asText();
                assertEquals(admissivel ? "CALCULADO" : "NAO_CALCULADO", estado,
                        () -> String.format("tpDoc %s, atividade %s: admissivelNoDocumento=%s diverge do estado %s "
                                + "do cálculo", tpDoc, codigo, admissivel, estado));
            }
        }
    }
}
