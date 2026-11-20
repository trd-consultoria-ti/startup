/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.arredondamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Random;

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

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Casos fixos de arredondamento do pedágio e das bases unificadas, conferidos na resposta
 * serializada. Os pedidos ficam em {@code entradas/arredondamento/}.
 * <p>
 * Pedágio: o total de cada tributo é a soma exata das parcelas dos trechos, arredondada uma vez;
 * cada trecho recebe a parcela truncada no centavo e os centavos restantes vão aos trechos de
 * maior resto (empate: menor número de trecho); as parcelas somam o total; a alíquota efetiva do
 * trecho é informativa, com 4 casas. Bases unificadas: resultado com 2 casas, arredondado uma vez.
 * <p>
 * Os campos do pedágio e das bases unificadas são números JSON (o tipo não mudou); por isso o
 * teste lê os números como decimais e compara valor e casas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
@DisplayName("Casos fixos de arredondamento do pedágio e das bases unificadas")
class Teste_CasosFixosPedagioBasesArredondamento {

    private static final String[] TRIBUTOS = { "cbs", "ibsEstadual", "ibsMunicipal" };

    /** Municípios válidos usados no teste de propriedade, com a UF de cada um. */
    private static final String[][] MUNICIPIOS = {
            { "3550308", "SP" }, { "3509502", "SP" }, { "3304557", "RJ" }, { "4313409", "RS" }, { "1200013", "AC" } };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("T6: pedágio de 200,00 em trechos de 10, 13 e 10 km — parcelas somam o total")
    void t6PedagioParcelasSomamOTotal() throws Exception {
        final JsonNode r = enviar("/calculadora/pedagio", pedido("T6_pedagio"));

        // CBS: 1,80 → 0,5454… + 0,7090… + 0,5454…; truncadas 0,54 + 0,70 + 0,54 = 1,78; os dois
        // centavos vão ao trecho 2 (maior resto) e ao trecho 1 (empate com o 3: menor número)
        assertThat(r.at("/total/cbsTotal/valorTributo").asString()).isEqualTo("1.80");
        assertNumero(r, "/trechos/0/cbs/tributoCalculado", "0.55");
        assertNumero(r, "/trechos/1/cbs/tributoCalculado", "0.71");
        assertNumero(r, "/trechos/2/cbs/tributoCalculado", "0.54");

        // IBS estadual: 0,20 → 0,06 + 0,07 + 0,06 = 0,19; o centavo vai ao trecho 2
        assertThat(r.at("/total/ibsEstadualTotal/valorTributo").asString()).isEqualTo("0.20");
        assertNumero(r, "/trechos/0/ibsEstadual/tributoCalculado", "0.06");
        assertNumero(r, "/trechos/1/ibsEstadual/tributoCalculado", "0.08");
        assertNumero(r, "/trechos/2/ibsEstadual/tributoCalculado", "0.06");

        // IBS municipal: zero com 2 casas, sem expoente
        assertThat(r.at("/total/ibsMunicipalTotal/valorTributo").asString()).isEqualTo("0.00");
        for (int i = 0; i < 3; i++) {
            assertNumero(r, "/trechos/" + i + "/ibsMunicipal/tributoCalculado", "0.00");
        }

        // alíquota efetiva do trecho: informativa, com 4 casas
        assertNumero(r, "/trechos/0/cbs/aliquotaEfetiva", "0.2727");
        assertNumero(r, "/trechos/1/cbs/aliquotaEfetiva", "0.3545");
        assertNumero(r, "/trechos/2/cbs/aliquotaEfetiva", "0.2727");
        assertThat(r.at("/trechos/0/cbs/aliquota").decimalValue()).isEqualByComparingTo("0.9");
        assertThat(r.at("/trechos/0/ibsEstadual/aliquota").decimalValue()).isEqualByComparingTo("0.1");
    }

    @Test
    @DisplayName("Pedágio: em pedidos sorteados, as parcelas dos trechos somam o total de cada tributo")
    void pedagioParcelasSomamOTotalEmPedidosSorteados() throws Exception {
        final Random random = new Random(GeradorLoteArredondamento.SEMENTE_PADRAO);
        for (int caso = 0; caso < 300; caso++) {
            final int quantidade = 1 + random.nextInt(8);
            final StringBuilder sb = new StringBuilder();
            sb.append("{\"dataHoraEmissao\":\"2026-10-20T10:00:00-03:00\",\"codigoMunicipioOrigem\":3550308,")
                    .append("\"ufMunicipioOrigem\":\"SP\",\"cst\":\"000\",\"cClassTrib\":\"000001\",\"baseCalculo\":")
                    .append(BigDecimal.valueOf(1 + random.nextInt(50_000_000), 2).toPlainString()).append(",\"trechos\":[");
            for (int n = 1; n <= quantidade; n++) {
                final String[] municipio = MUNICIPIOS[random.nextInt(MUNICIPIOS.length)];
                sb.append(n > 1 ? "," : "").append("{\"numero\":").append(n).append(",\"municipio\":").append(municipio[0])
                        .append(",\"uf\":\"").append(municipio[1]).append("\",\"extensao\":")
                        .append(BigDecimal.valueOf(1 + random.nextInt(500_000), random.nextInt(4)).toPlainString()).append('}');
            }
            sb.append("]}");
            final JsonNode r = enviar("/calculadora/pedagio", sb.toString());

            for (String tributo : TRIBUTOS) {
                BigDecimal soma = BigDecimal.ZERO;
                for (JsonNode trecho : r.at("/trechos")) {
                    final BigDecimal parcela = trecho.at("/" + tributo + "/tributoCalculado").decimalValue();
                    assertThat(parcela.scale()).as("casas da parcela em %s", sb).isEqualTo(2);
                    assertThat(trecho.at("/" + tributo + "/aliquotaEfetiva").decimalValue().scale())
                            .as("casas da alíquota efetiva em %s", sb).isEqualTo(4);
                    soma = soma.add(parcela);
                }
                final String total = r.at("/total/" + tributo + "Total/valorTributo").asString();
                assertThat(soma).as("soma das parcelas de %s em %s", tributo, sb).isEqualByComparingTo(total);
            }
        }
    }

    @Test
    @DisplayName("T10: base unificada com vFrete 10,005 — bases com 2 casas, empate para o par")
    void t10BaseUnificadaComDuasCasas() throws Exception {
        final JsonNode r = enviar("/calculadora/base-calculo/base-calculo-unificada", pedido("T10_base_unificada"));
        // 954,505 e 1010,005: empate exato, arredondado para o par
        assertNumero(r, "/baseRG", "954.50");
        assertNumero(r, "/baseIS", "954.50");
        assertNumero(r, "/baseSN", "1010.00");
    }

    @Test
    @DisplayName("N1: base unificada — bases com 2 casas")
    void n1BaseUnificadaComDuasCasas() throws Exception {
        final JsonNode r = enviar("/calculadora/base-calculo/base-calculo-unificada", pedido("N1_base_unificada"));
        assertNumero(r, "/baseRG", "954.50");
        assertNumero(r, "/baseIS", "954.50");
        assertNumero(r, "/baseSN", "1010.00");
    }

    @Test
    @DisplayName("RG-12: base unificada da NFS-e, locação com copropriedade de 33,33% — 333,32")
    void rg12BaseUnificadaNfse() throws Exception {
        final JsonNode r = enviar("/calculadora/base-calculo/base-calculo-unificada-nfse",
                pedido("RG12_base_unificada_nfse"));
        // 1.000,05 × 33,33% = 333,316665 → 333,32
        assertNumero(r, "/baseRG", "333.32");
        assertThat(r.at("/baseSN").isNull()).isTrue();
    }

    private static String pedido(String caso) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource("entradas/arredondamento/" + caso + ".json").getInputStream(),
                StandardCharsets.UTF_8);
    }

    private JsonNode enviar(String url, String corpo) throws Exception {
        final String resposta = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(resposta).as("resposta sem número com expoente").doesNotContainPattern("[0-9][eE][-+]?[0-9]");
        return objectMapper.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(resposta);
    }

    /** O campo é um número JSON com exatamente o valor e as casas do esperado. */
    private static void assertNumero(JsonNode raiz, String ponteiro, String esperado) {
        final JsonNode no = raiz.at(ponteiro);
        assertThat(no.isNumber()).as("campo %s publicado como número", ponteiro).isTrue();
        assertThat(no.decimalValue()).as("campo %s", ponteiro).isEqualTo(new BigDecimal(esperado));
    }
}
