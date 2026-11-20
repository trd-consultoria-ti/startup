package br.gov.serpro.rtc.testesintegracao.simplesnacional;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integração do endpoint de observabilidade do Simples Nacional
 * (NT 2026.005), contra a base de catálogo real (cClassTrib 000001 com
 * tpRBSN=1 e cAtivSN '01' admissível para NF-e/tpDoc 55). A associativa
 * atividade × documento tem semântica de whitelist: atividade admissível
 * somente nos documentos com vínculo vigente ('01'–'04' em NF-e/NFC-e,
 * '07'–'14' em NFS-e, '90' nos três; '05' e '06' em documento nenhum).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class SimplesNacionalControllerTest {

    private static final String ENDPOINT = "/calculadora/observabilidade/simples-nacional";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCalcularCaminhoFelizCrt1ComImpostoSeletivoForaDaReceitaBruta() throws Exception {
        // Caso dourado 2: base 950 + vIS 50 -> vRBSNitem 950 (o vIS
        // NÃO compõe a receita bruta em nenhuma vigência); o IS segue
        // repassado e entra no totalItem. 950 × 1.25% = 11.875 -> 11.88 e
        // 950 × 2.75% = 26.125 -> 26.12 (HALF_EVEN na serialização).
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "950.00",
                      "valorImpostoSeletivo": "50.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.oper.data").value("2027-03-15"))
            .andExpect(jsonPath("$.oper.tpDoc").value("55"))
            .andExpect(jsonPath("$.oper.CRT").value("1"))
            .andExpect(jsonPath("$.oper.tpRegimeIBSCBS").doesNotExist())

            .andExpect(jsonPath("$.objetos[0].nObj").value(1))
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("950.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(1))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSNDescricao").value("Receita bruta - interna"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vBC").value("950.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pIBSSN").value("1.25"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("11.88"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSPendSusp").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pCBSSN").value("2.75"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("26.12"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSPendSusp").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.IS.valor").value("50.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.totalItem").value("88.00"))

            .andExpect(jsonPath("$.total.vRBSNtot").value("950.00"))
            .andExpect(jsonPath("$.total.vBC").value("950.00"))
            .andExpect(jsonPath("$.total.vIBSSNtot").value("11.88"))
            .andExpect(jsonPath("$.total.vIBSSNtotPendSusp").value("0.00"))
            .andExpect(jsonPath("$.total.vCBSSNtot").value("26.12"))
            .andExpect(jsonPath("$.total.vCBSSNtotPendSusp").value("0.00"))
            .andExpect(jsonPath("$.total.totalIs").value("50.00"))
            .andExpect(jsonPath("$.total.totalTributos").value("88.00"));
    }

    @Test
    void deveManterImpostoSeletivoForaDaReceitaBrutaTambemAPartirDe2028() throws Exception {
        // O vIS fica fora da receita bruta em TODAS as vigências: a partir de
        // 2028 o resultado é o MESMO das demais vigências — base 950 + vIS 50
        // -> vRBSNitem 950.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2028-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "950.00",
                      "valorImpostoSeletivo": "50.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.oper.data").value("2028-03-15"))

            .andExpect(jsonPath("$.objetos[0].nObj").value(1))
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("950.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(1))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSNDescricao").value("Receita bruta - interna"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vBC").value("950.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pIBSSN").value("1.25"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("11.88"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSPendSusp").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pCBSSN").value("2.75"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("26.12"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSPendSusp").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.IS.valor").value("50.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.totalItem").value("88.00"))

            .andExpect(jsonPath("$.total.vRBSNtot").value("950.00"))
            .andExpect(jsonPath("$.total.vBC").value("950.00"))
            .andExpect(jsonPath("$.total.vIBSSNtot").value("11.88"))
            .andExpect(jsonPath("$.total.vIBSSNtotPendSusp").value("0.00"))
            .andExpect(jsonPath("$.total.vCBSSNtot").value("26.12"))
            .andExpect(jsonPath("$.total.vCBSSNtotPendSusp").value("0.00"))
            .andExpect(jsonPath("$.total.totalIs").value("50.00"))
            .andExpect(jsonPath("$.total.totalTributos").value("88.00"));
    }

    @Test
    void deveIgnorarCampoDesconhecidoPRedutorEAplicarApenasODeslocamento() throws Exception {
        // Caso dourado 33: campo extra pRedutor é ignorado pelo binding e o
        // resultado é IDÊNTICO ao caso 15 (tpEnteGov=2, 2029: f=0.10) — nenhum
        // redutor aplicado. tpEnteGov (e o pRedutor ignorado) são dados da
        // OPERAÇÃO, espelhando o grupo gCompraGov da identificação da nota
        // (grupo B/ide do leiaute do DF-e).
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2029-06-01",
                  "tpDoc": "55",
                  "CRT": "1",
                  "tpEnteGov": "2",
                  "pRedutor": "0.50",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.oper.tpEnteGov").value("2"))
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pIBSSN").value("1.525"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("15.25"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pCBSSN").value("2.475"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("24.75"));
    }

    @Test
    void deveRetornar422ParaTpEnteGovInvalido() throws Exception {
        // tpEnteGov é campo global da operação: valor fora do domínio {1..4}
        // rejeita a operação inteira (VSN-024), sem processar nenhum item.
        String json = """
                {
                  "id": "abc",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "tpEnteGov": "7",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].code").value("VSN-024"));
    }

    @Test
    void deveRetornarNaoCalculadoParaCAtivSNSemLinhaNoCatalogo() throws Exception {
        // Caso dourado 19: código '99' não existe no catálogo de atividades -> NAO_CALCULADO.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "99",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("NAO_CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("1000.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(1))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSNDescricao").value("Receita bruta - interna"))
            // NAO_CALCULADO: o vBC é suprimido (o cálculo não ocorreu).
            .andExpect(jsonPath("$.objetos[0].tribCalc.vBC").doesNotExist())
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").doesNotExist())
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").doesNotExist())
            .andExpect(jsonPath("$.objetos[0].tribCalc.IS").doesNotExist())
            // Contribui apenas para vRBSNtot; sem item calculado, sem vBC total.
            .andExpect(jsonPath("$.total.vRBSNtot").value("1000.00"))
            .andExpect(jsonPath("$.total.vBC").doesNotExist())
            .andExpect(jsonPath("$.total.vIBSSNtot").doesNotExist())
            .andExpect(jsonPath("$.total.totalTributos").doesNotExist());
    }

    @Test
    void atividadeSemVinculoAlgum_naoCalculadaEmNenhumDocumentoHabilitado() throws Exception {
        // Whitelist (mundo fechado): as atividades '05' e '06' constam do
        // catálogo mas não têm vínculo com documento algum na associativa —
        // em TODO documento habilitado o item resulta em NAO_CALCULADO, sem
        // erro (NT 2026.005, §5.3.4, Observação 2).
        for (String cAtivSN : List.of("05", "06")) {
            for (String tpDoc : List.of("55", "65", "91")) {
                String json = """
                        {
                          "id": "507f1f77bcf86cd799439011",
                          "versao": "2.0.0",
                          "data": "2027-03-15",
                          "tpDoc": "%s",
                          "CRT": "1",
                          "itens": [
                            {
                              "numero": 1,
                              "cAtivSN": "%s",
                              "cClassTrib": "000001",
                              "baseCalculo": "1000.00",
                              "valorImpostoSeletivo": "0.00",
                              "pIBSSN": "1.25",
                              "pCBSSN": "2.75"
                            }
                          ]
                        }
                        """.formatted(tpDoc, cAtivSN);

                mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.objetos[0].estadoItem").value("NAO_CALCULADO"))
                    .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("1000.00"))
                    .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").doesNotExist())
                    .andExpect(jsonPath("$.objetos[0].erros").doesNotExist());
            }
        }
    }

    @Test
    void atividadeVinculadaCalculaNoDocumentoCertoENaoCalculaNoErrado() throws Exception {
        // A atividade '07' é vinculada somente à NFS-e (91): calcula lá e
        // resulta NAO_CALCULADO em NF-e (55).
        String operacaoNfse = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "91",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "07",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(operacaoNfse))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("12.50"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("27.50"));

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content(operacaoNfse.replace("\"tpDoc\": \"91\"", "\"tpDoc\": \"55\"")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("NAO_CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].erros").doesNotExist());
    }

    @Test
    void codigo90CalculaEmNfeNfceENfse() throws Exception {
        // O '90' é vinculado aos três documentos habilitados: com cClassTrib de
        // tpRBSN=0 (não vedado para não tributáveis), CALCULA em todos, com
        // tudo zero.
        for (String tpDoc : List.of("55", "65", "91")) {
            String json = """
                    {
                      "id": "507f1f77bcf86cd799439011",
                      "versao": "2.0.0",
                      "data": "2027-03-15",
                      "tpDoc": "%s",
                      "CRT": "1",
                      "itens": [
                        {
                          "numero": 1,
                          "cAtivSN": "90",
                          "cClassTrib": "410001",
                          "baseCalculo": "1000.00",
                          "valorImpostoSeletivo": "0.00"
                        }
                      ]
                    }
                    """.formatted(tpDoc);

            mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
                .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("0.00"))
                .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(0));
        }
    }

    @Test
    void deveResolverTpRBSNPelaLinhaDaCbsQuandoCodigoColideComImpostoSeletivo() throws Exception {
        // Caso R-5 com dados reais: o CLTR_CD '000001' existe nos dois grupos da
        // CLASSIFICACAO_TRIBUTARIA — CBS (tpRBSN=1, vigente desde 2026) e
        // Imposto Seletivo (tpRBSN=0 inerte, vigente desde 2027). Em 2027, sem o
        // filtro de tributo, a linha mais recente (IS) venceria e zeraria o
        // cálculo; a resolução DEVE usar a linha da CBS.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(1))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("12.50"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("27.50"));
    }

    @Test
    void deveCalcularCAtivSN90ComTpRBSN0() throws Exception {
        // Caso R-4 com dados reais: '90' (Operações não Tributáveis, linha 16 do
        // catálogo, restrita a NF-e/NFC-e) + cClassTrib 410001 (tpRBSN=0) ->
        // CALCULADO com vRBSNitem=0 e tributos zero.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "90",
                      "cClassTrib": "410001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(0))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSNDescricao").value("Não é receita bruta"))
            // Fator de receita 0 (item CALCULADO): vBC presente, com 0.00.
            .andExpect(jsonPath("$.objetos[0].tribCalc.vBC").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("0.00"));
    }

    @Test
    void deveAcumularErroDeValidacaoNoItemEIgnorarNaTotalizacao() throws Exception {
        String json = """
                {
                  "id": "abc",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "-10.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    },
                    {
                      "numero": 2,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    },
                    {
                      "numero": 3,
                      "cAtivSN": "99",
                      "cClassTrib": "000001",
                      "baseCalculo": "500.00",
                      "valorImpostoSeletivo": "0.00"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].nObj").value(1))
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("INCONSISTENCIA_ENTRADA"))
            .andExpect(jsonPath("$.objetos[0].erros[0].code").value("VSN-002"))
            .andExpect(jsonPath("$.objetos[0].tribCalc").doesNotExist())
            .andExpect(jsonPath("$.objetos[1].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[2].estadoItem").value("NAO_CALCULADO"))
            // vRBSNtot soma calculado + não calculado; os demais totais (vBC
            // inclusive), só o calculado.
            .andExpect(jsonPath("$.total.vRBSNtot").value("1500.00"))
            .andExpect(jsonPath("$.total.vBC").value("1000.00"))
            .andExpect(jsonPath("$.total.vIBSSNtot").value("12.50"))
            .andExpect(jsonPath("$.total.totalTributos").value("40.00"));
    }

    @Test
    void deveRetornarNaoImplementadoParaCClassTribComTratamentoSemImplementacao() throws Exception {
        // 620001 (tributação monofásica sobre combustíveis) está vinculado, na
        // base, a tratamento tributário marcado como "sem implementação"
        // (TRTR_IN_POSSUI_AJUSTE = 1 — mesmo indicador que leva o regime geral
        // ao CAL-007). Mesmo com cAtivSN e alíquotas válidos, o item sai
        // NAO_IMPLEMENTADO com VSN-027, sem tribCalc e sem total.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "620001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].nObj").value(1))
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("NAO_IMPLEMENTADO"))
            .andExpect(jsonPath("$.objetos[0].erros.length()").value(1))
            .andExpect(jsonPath("$.objetos[0].erros[0].code").value("VSN-027"))
            .andExpect(jsonPath("$.objetos[0].erros[0].pointer").value("#/itens/0/cClassTrib"))
            .andExpect(jsonPath("$.objetos[0].erros[0].type",
                    endsWith("/calculadora/observabilidade/simples-nacional/erros?codigo=VSN-027")))
            .andExpect(jsonPath("$.objetos[0].tribCalc").doesNotExist())
            .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    void naoImplementadoTemPrioridadeSobreValidacoesDoItemENaoAfetaOsDemais() throws Exception {
        // Item 1: cClassTrib sem implementação E base negativa E alíquotas
        // ausentes — a verificação de implementação é a primeira consulta do
        // item e encerra o processamento: apenas VSN-027 é reportado. Item 2
        // calcula normalmente e os totais vêm só dele. Item 3: cAtivSN
        // desconhecido (que sozinho levaria a NAO_CALCULADO) com 800001
        // (transferência de crédito), também sem implementação — a prioridade
        // prevalece sobre a regra do NAO_CALCULADO.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "620001",
                      "baseCalculo": "-10.00",
                      "valorImpostoSeletivo": "0.00"
                    },
                    {
                      "numero": 2,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    },
                    {
                      "numero": 3,
                      "cAtivSN": "99",
                      "cClassTrib": "800001",
                      "baseCalculo": "500.00",
                      "valorImpostoSeletivo": "0.00"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("NAO_IMPLEMENTADO"))
            .andExpect(jsonPath("$.objetos[0].erros.length()").value(1))
            .andExpect(jsonPath("$.objetos[0].erros[0].code").value("VSN-027"))
            .andExpect(jsonPath("$.objetos[0].tribCalc").doesNotExist())
            .andExpect(jsonPath("$.objetos[1].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[2].estadoItem").value("NAO_IMPLEMENTADO"))
            .andExpect(jsonPath("$.objetos[2].erros[0].code").value("VSN-027"))
            .andExpect(jsonPath("$.objetos[2].erros[0].pointer").value("#/itens/2/cClassTrib"))
            // Totais apenas do item calculado.
            .andExpect(jsonPath("$.total.vRBSNtot").value("1000.00"))
            .andExpect(jsonPath("$.total.vBC").value("1000.00"))
            .andExpect(jsonPath("$.total.vIBSSNtot").value("12.50"))
            .andExpect(jsonPath("$.total.vCBSSNtot").value("27.50"))
            .andExpect(jsonPath("$.total.totalTributos").value("40.00"));
    }

    @Test

    void tributacaoEmDocumentoEspecificoCalculaEmNfseSemVsn027() throws Exception {
        // 820001 é admissível em NFS-e, mas o tratamento 32 não possui ajuste
        // na carga vigente. Portanto, o cálculo segue normalmente.

        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "91",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "07",
                      "cClassTrib": "820001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].erros").doesNotExist())

            .andExpect(jsonPath("$.objetos[0].tribCalc.vRBSNitem").value("1000.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSN").value(9))
            .andExpect(jsonPath("$.objetos[0].tribCalc.tpRBSNDescricao")
                    .value("Fornecimento incompatível com SN"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vBC").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("0.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("0.00"))
            .andExpect(jsonPath("$.total.vRBSNtot").value("1000.00"))

            .andExpect(jsonPath("$.total.vBC").value("0.00"))
            .andExpect(jsonPath("$.total.vIBSSNtot").value("0.00"))
            .andExpect(jsonPath("$.total.vCBSSNtot").value("0.00"))
            .andExpect(jsonPath("$.total.totalTributos").value("0.00"));
    }

    @Test
    void naoImplementadoTambemEmNfse() throws Exception {
        // 800001 (transferência de crédito) é admissível em NFS-e e mantém
        // TRTR_IN_POSSUI_AJUSTE=1, preservando a cobertura VSN-027 nesse doc.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "91",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "07",
                      "cClassTrib": "800001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("NAO_IMPLEMENTADO"))
            .andExpect(jsonPath("$.objetos[0].erros[0].code").value("VSN-027"))
            .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    void cClassTribEncerradoEm2026NaoEhNaoImplementado_ehForaDeVigencia() throws Exception {
        // 220001 estava vinculado a tratamento "sem implementação", mas a
        // classificação e o vínculo encerraram em 2026-01-01: em 2027 a
        // verificação prioritária conta zero e o fluxo normal reporta VSN-021
        // (desconhecido/fora de vigência), não VSN-027.
        String json = """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "220001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("INCONSISTENCIA_ENTRADA"))
            .andExpect(jsonPath("$.objetos[0].erros[0].code").value("VSN-021"));
    }

    @Test
    void deveRetornar422ParaRegimeRegular() throws Exception {
        // Caso dourado 23: tpRegimeIBSCBS=1 (regime regular) está fora de escopo.
        String json = """
                {
                  "id": "abc",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "tpRegimeIBSCBS": "1",
                  "itens": [
                    {
                      "numero": 1,
                      "cAtivSN": "01",
                      "cClassTrib": "000001",
                      "baseCalculo": "1000.00",
                      "valorImpostoSeletivo": "0.00",
                      "pIBSSN": "1.25",
                      "pCBSSN": "2.75"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].code").value("VSN-019"));
    }

    @Test
    void deveRetornar422ParaTpDocNaoSuportado() throws Exception {
        // Documentos sem NT publicada não têm vínculo na tabela de documentos
        // do cálculo: a operação inteira é rejeitada com VSN-015. Vale para o
        // CT-e Simplificado (97) e para o BP-e TA (83), não abrangidos pelas
        // NTs 2026.003 (que habilitaram CT-e, CT-e OS, BP-e, BP-e TM e NFCom
        // na V0045).
        for (String tpDoc : List.of("97", "83")) {
            String json = """
                    {
                      "id": "abc",
                      "versao": "2.0.0",
                      "data": "2027-03-15",
                      "tpDoc": "%s",
                      "CRT": "1",
                      "itens": [
                        {
                          "numero": 1,
                          "cClassTrib": "000001",
                          "baseCalculo": "1000.00",
                          "valorImpostoSeletivo": "0.00"
                        }
                      ]
                    }
                    """.formatted(tpDoc);

            mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("VSN-015"));
        }
    }

    @Test
    void deveRetornar422ParaTpDocSemTratamento() throws Exception {
        for (String tpDoc : List.of("77", "94", "95", "96")) {
            String json = """
                    {
                      "id": "abc",
                      "versao": "2.0.0",
                      "data": "2027-03-15",
                      "tpDoc": "%s",
                      "CRT": "1",
                      "itens": [
                        {
                          "numero": 1,
                          "cAtivSN": "01",
                          "cClassTrib": "000001",
                          "baseCalculo": "1000.00",
                          "valorImpostoSeletivo": "0.00",
                          "pIBSSN": "1.25",
                          "pCBSSN": "2.75"
                        }
                      ]
                    }
                    """.formatted(tpDoc);

            mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("VSN-015"))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/tpDoc"));
        }
    }

    @Test
    void deveRetornar422QuandoListaDeItensVazia() throws Exception {
        String json = """
                {
                  "id": "abc",
                  "versao": "2.0.0",
                  "data": "2027-03-15",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": []
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].code").value("VSN-011"));
    }

    @Test
    void deveRetornar422ParaDataAnteriorATodaVigencia() throws Exception {
        // A parametrização do Simples Nacional vigora a partir de 2027-01-01.
        // Qualquer data anterior (inclusive em 2026) não tem documento
        // habilitado na tabela TIPO_DFE_CALCULO_SIMPLES_NACIONAL.
        for (String data : List.of("2025-06-01", "2025-12-31", "2026-06-01", "2026-12-31")) {
            String json = """
                    {
                      "id": "abc",
                      "versao": "2.0.0",
                      "data": "%s",
                      "tpDoc": "55",
                      "CRT": "1",
                      "itens": [
                        {
                          "numero": 1,
                          "cAtivSN": "01",
                          "cClassTrib": "000001",
                          "baseCalculo": "1000.00",
                          "valorImpostoSeletivo": "0.00",
                          "pIBSSN": "1.25",
                          "pCBSSN": "2.75"
                        }
                      ]
                    }
                    """.formatted(data);

            mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors[0].code").value("VSN-015"))
                    .andExpect(jsonPath("$.errors[0].pointer").value("#/tpDoc"));
        }
    }

    @Test
    void deveConsultarCatalogoDeErros() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/erros")
                .param("codigo", "VSN-025"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value("VSN-025"))
            .andExpect(jsonPath("$.titulo").value("Parametrização de compra governamental indisponível para a data"));
    }

    @Test
    void deveConsultarErroDeParametrizacaoIndisponivel() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/erros")
                .param("codigo", "VSN-026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value("VSN-026"))
            .andExpect(jsonPath("$.titulo")
                    .value("Parametrização do cálculo do Simples Nacional indisponível para a data"));
    }

    @Test
    void deveConsultarErroDeClassificacaoNaoImplementada() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/erros")
                .param("codigo", "VSN-027"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value("VSN-027"))
            .andExpect(jsonPath("$.titulo")
                    .value("cClassTrib sem tratamento implementado no cálculo do Simples Nacional"));
    }

    @Test
    void deveRetornar404ParaCodigoDeErroDesconhecido() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/erros")
                .param("codigo", "VSN-008"))
            .andExpect(status().isNotFound());
    }

    // ─── Precisão: entrada integral, valores fiscais e totais ──────────────────

    @Test
    void aliquotaIntegralNaEntrada_eUsadaComTodasAsCasas_eApresentadaCom4() throws Exception {
        // 987.654,32 × 1,6839379378389463610262954% = 16.631,49 (com 1,6839 seria 16.631,11).
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content(operacao("2027-04-15", item(1, "987654.32", "0.00", "0.01867380622521988789135487",
                        "1.6839379378389463610262954"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pCBSSN").value("1.6839"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("16631.49"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.pIBSSN").value("0.0187"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("184.43"));
    }

    @Test
    void totaisSaoASomaDosValoresFiscaisDosItens() throws Exception {
        // Itens 1 e 2: 160,40 × 1,25% = 2,005 → 2,00 (HALF_EVEN) e 160,40 × 2,75% =
        // 4,411 → 4,41; totalItem = 2,00 + 4,41 = 6,41. Item 3: 100,01 → 1,25 e 2,75.
        // Os totais fecham com a soma dos itens exibidos (5,25, não 5,26).
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content(operacao("2027-04-15", item(1, "160.40", "0.00", "1.25", "2.75") + ","
                        + item(2, "160.40", "0.00", "1.25", "2.75") + "," + item(3, "100.01", "0.00", "1.25", "2.75"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].tribCalc.vIBSSN").value("2.00"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.vCBSSN").value("4.41"))
            .andExpect(jsonPath("$.objetos[0].tribCalc.totalItem").value("6.41"))
            .andExpect(jsonPath("$.objetos[2].tribCalc.totalItem").value("4.00"))
            .andExpect(jsonPath("$.total.vIBSSNtot").value("5.25"))
            .andExpect(jsonPath("$.total.vCBSSNtot").value("11.57"))
            .andExpect(jsonPath("$.total.totalTributos").value("16.82"));
    }

    @Test
    void entradaForaDoFormato_eErroDeItem_semConverterNotacaoCientifica() throws Exception {
        // Notação científica com expoente gigante esgotaria a memória se fosse
        // convertida: a validação de formato a rejeita antes.
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content(operacao("2027-04-15",
                        item(1, "1000.00", "0.00", "1.25", "1E+100000000") + ","
                                + item(2, "1E+100000000", "0.00", "1.25", "2.75") + ","
                                + item(3, "1000.005", "0.00", "1.25", "2.75") + ","
                                + item(4, "1000.00", "0.001", "1.25", "2.75") + ","
                                + item(5, "1000.00", "0.00", "1234.5", "2.75") + ","
                                + item(6, "1000.00", "0.00", "1.25", "-2.75") + ","
                                + item(7, "1000.00", "0.00", "1.25", "2." + "1".repeat(34)) + ","
                                + item(8, "1000.00", "0.00", "1.25", "2." + "1".repeat(35)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objetos[0].erros[0].code").value("VSN-005"))
            .andExpect(jsonPath("$.objetos[0].erros[0].detail",
                    org.hamcrest.Matchers.containsString("sem notação científica")))
            .andExpect(jsonPath("$.objetos[1].erros[0].code").value("VSN-002"))
            .andExpect(jsonPath("$.objetos[2].erros[0].code").value("VSN-002"))
            .andExpect(jsonPath("$.objetos[3].erros[0].code").value("VSN-003"))
            .andExpect(jsonPath("$.objetos[4].erros[0].code").value("VSN-007"))
            .andExpect(jsonPath("$.objetos[5].erros[0].code").value("VSN-005"))
            .andExpect(jsonPath("$.objetos[5].erros[0].detail",
                    org.hamcrest.Matchers.containsString("não pode ser negativo")))
            .andExpect(jsonPath("$.objetos[6].estadoItem").value("CALCULADO"))
            .andExpect(jsonPath("$.objetos[7].erros[0].code").value("VSN-005"));
    }

    private static String operacao(String data, String itens) {
        return """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "versao": "2.0.0",
                  "data": "%s",
                  "tpDoc": "55",
                  "CRT": "1",
                  "itens": [ %s ]
                }
                """.formatted(data, itens);
    }

    private static String item(int numero, String base, String vIs, String pIbs, String pCbs) {
        return """
                { "numero": %d, "cAtivSN": "01", "cClassTrib": "000001", "baseCalculo": "%s",
                  "valorImpostoSeletivo": "%s", "pIBSSN": "%s", "pCBSSN": "%s" }
                """.formatted(numero, base, vIs, pIbs, pCbs);
    }
}
