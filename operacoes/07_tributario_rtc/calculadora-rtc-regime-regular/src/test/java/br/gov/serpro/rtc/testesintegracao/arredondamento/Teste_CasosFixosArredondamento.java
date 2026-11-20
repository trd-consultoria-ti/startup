/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.arredondamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Casos fixos da política de arredondamento do regime geral, com a saída esperada campo a campo
 * (texto igual, sem tolerância). Os pedidos ficam em {@code entradas/arredondamento/}.
 * <p>
 * Regras conferidas: o valor é a base vezes a alíquota publicada, arredondado uma vez; o
 * {@code vIBS} é a soma do {@code vIBSUF} e do {@code vIBSMun} publicados; cada total é a soma dos
 * valores publicados nos itens; no empate exato, arredonda-se para o par.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
@DisplayName("Casos fixos de arredondamento do regime geral")
class Teste_CasosFixosArredondamento {

    private static final String ITEM = "/objetos/%d/tribCalc/IBSCBS/gIBSCBS";
    private static final String TOTAL = "/total/tribCalc/IBSCBSTot";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("T1: base 25,00 em 2026 — empate exato vai para o par")
    void t1EmpateParaOPar() throws Exception {
        final JsonNode r = calcular("T1");
        assertThat(texto(r, ITEM.formatted(0) + "/vBC")).isEqualTo("25.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/pCBS")).isEqualTo("0.90");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/vCBS")).isEqualTo("0.22");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/pIBSUF")).isEqualTo("0.10");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/vIBSUF")).isEqualTo("0.02");
        assertThat(texto(r, ITEM.formatted(0) + "/vIBS")).isEqualTo("0.02");
        assertThat(texto(r, TOTAL + "/gCBS/vCBS")).isEqualTo("0.22");
        assertThat(texto(r, TOTAL + "/gIBS/vIBS")).isEqualTo("0.02");
    }

    @Test
    @DisplayName("T2: base 12,00 em 2027, IBS 0,05% + 0,05% — vIBS é a soma das partes publicadas")
    void t2VIbsSomaDasPartesPublicadas() throws Exception {
        final JsonNode r = calcular("T2");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/vIBSUF")).isEqualTo("0.01");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSMun/vIBSMun")).isEqualTo("0.01");
        assertThat(texto(r, ITEM.formatted(0) + "/vIBS")).isEqualTo("0.02");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/vCBS")).isEqualTo("1.06");
        assertThat(texto(r, TOTAL + "/gIBS/gIBSUF/vIBSUF")).isEqualTo("0.01");
        assertThat(texto(r, TOTAL + "/gIBS/gIBSMun/vIBSMun")).isEqualTo("0.01");
        assertThat(texto(r, TOTAL + "/gIBS/vIBS")).isEqualTo("0.02");
        assertThat(texto(r, TOTAL + "/gCBS/vCBS")).isEqualTo("1.06");
    }

    @Test
    @DisplayName("T3: dois itens de 14,00 em 2026 — totais são a soma dos itens publicados")
    void t3TotaisSomaDosItensPublicados() throws Exception {
        final JsonNode r = calcular("T3");
        for (int i = 0; i < 2; i++) {
            assertThat(texto(r, ITEM.formatted(i) + "/gCBS/vCBS")).isEqualTo("0.13");
            assertThat(texto(r, ITEM.formatted(i) + "/gIBSUF/vIBSUF")).isEqualTo("0.01");
            assertThat(texto(r, ITEM.formatted(i) + "/vIBS")).isEqualTo("0.01");
        }
        assertThat(texto(r, TOTAL + "/vBCIBSCBS")).isEqualTo("28.00");
        assertThat(texto(r, TOTAL + "/gCBS/vCBS")).isEqualTo("0.26");
        assertThat(texto(r, TOTAL + "/gIBS/gIBSUF/vIBSUF")).isEqualTo("0.02");
        assertThat(texto(r, TOTAL + "/gIBS/vIBS")).isEqualTo("0.02");
    }

    @Test
    @DisplayName("T4: 1.000.000,00, CBS 8,8123% com redução de 60% — o valor usa a alíquota efetiva publicada")
    void t4ValorPelaAliquotaEfetivaPublicada() throws Exception {
        final JsonNode r = calcular("T4");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/pCBS")).isEqualTo("8.8123");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/gRed/pRedAliq")).isEqualTo("60.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/gRed/pAliqEfet")).isEqualTo("3.5249");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/vCBS")).isEqualTo("35249.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/gRed/pAliqEfet")).isEqualTo("0.02");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/vIBSUF")).isEqualTo("200.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSMun/gRed/pAliqEfet")).isEqualTo("0.02");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSMun/vIBSMun")).isEqualTo("200.00");
        assertThat(texto(r, ITEM.formatted(0) + "/vIBS")).isEqualTo("400.00");
        assertThat(texto(r, TOTAL + "/gCBS/vCBS")).isEqualTo("35249.00");
        assertThat(texto(r, TOTAL + "/gIBS/vIBS")).isEqualTo("400.00");
    }

    @Test
    @DisplayName("T5: compra da União, base 12,00 em 2027 — grupo principal pela alíquota redistribuída")
    void t5CompraDaUniao() throws Exception {
        final JsonNode r = calcular("T5");
        assertThat(texto(r, "/oper/gCompraGov/pRedutor")).isEqualTo("0.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/pCBS")).isEqualTo("8.80");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/gRed/pAliqEfet")).isEqualTo("8.90");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/vCBS")).isEqualTo("1.07");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/gRed/pAliqEfet")).isEqualTo("0.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/vIBSUF")).isEqualTo("0.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSMun/gRed/pAliqEfet")).isEqualTo("0.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSMun/vIBSMun")).isEqualTo("0.00");
        assertThat(texto(r, ITEM.formatted(0) + "/vIBS")).isEqualTo("0.00");
        assertThat(texto(r, ITEM.formatted(0) + "/gTribCompraGov/pAliqCBS")).isEqualTo("8.80");
        assertThat(texto(r, ITEM.formatted(0) + "/gTribCompraGov/vTribCBS")).isEqualTo("1.06");
        assertThat(texto(r, ITEM.formatted(0) + "/gTribCompraGov/pAliqIBSUF")).isEqualTo("0.05");
        assertThat(texto(r, ITEM.formatted(0) + "/gTribCompraGov/vTribIBSUF")).isEqualTo("0.01");
        assertThat(texto(r, ITEM.formatted(0) + "/gTribCompraGov/pAliqIBSMun")).isEqualTo("0.05");
        assertThat(texto(r, ITEM.formatted(0) + "/gTribCompraGov/vTribIBSMun")).isEqualTo("0.01");
        assertThat(texto(r, TOTAL + "/gCBS/vCBS")).isEqualTo("1.07");
    }

    @Test
    @DisplayName("Issue: itens de 2.315,79 e 690,52 em 2026 — total da CBS é a soma dos itens")
    void issueTotalCbsSomaDosItens() throws Exception {
        final JsonNode r = calcular("issue_total_cbs");
        assertThat(texto(r, ITEM.formatted(0) + "/gCBS/vCBS")).isEqualTo("20.84");
        assertThat(texto(r, ITEM.formatted(1) + "/gCBS/vCBS")).isEqualTo("6.21");
        assertThat(texto(r, ITEM.formatted(0) + "/gIBSUF/vIBSUF")).isEqualTo("2.32");
        assertThat(texto(r, ITEM.formatted(1) + "/gIBSUF/vIBSUF")).isEqualTo("0.69");
        assertThat(texto(r, TOTAL + "/vBCIBSCBS")).isEqualTo("3006.31");
        assertThat(texto(r, TOTAL + "/gCBS/vCBS")).isEqualTo("27.05");
        assertThat(texto(r, TOTAL + "/gIBS/gIBSUF/vIBSUF")).isEqualTo("3.01");
        assertThat(texto(r, TOTAL + "/gIBS/vIBS")).isEqualTo("3.01");
    }

    private JsonNode calcular(String caso) throws Exception {
        final String pedido = StreamUtils.copyToString(
                new ClassPathResource("entradas/arredondamento/" + caso + ".json").getInputStream(),
                StandardCharsets.UTF_8);
        final String resposta = mockMvc.perform(post("/calculadora/regime-geral")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pedido))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resposta);
    }

    private static String texto(JsonNode raiz, String ponteiro) {
        final JsonNode no = raiz.at(ponteiro);
        assertThat(no.isMissingNode()).as("campo %s presente na resposta", ponteiro).isFalse();
        assertThat(no.isString()).as("campo %s publicado como texto", ponteiro).isTrue();
        return no.asString();
    }
}
