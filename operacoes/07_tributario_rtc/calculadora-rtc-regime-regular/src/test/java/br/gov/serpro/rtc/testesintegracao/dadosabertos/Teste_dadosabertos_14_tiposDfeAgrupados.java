/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.dadosabertos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
@DisplayName("Tipos de DFe agrupados - Integration Tests")
class Teste_dadosabertos_14_tiposDfeAgrupados {

    private static final String URI = "/calculadora/dados-abertos/dfe/grupos";
    private static final String DATA_VIGENTE = "2027-01-01";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Retorna 200 com lista de grupos não-vazia para data vigente")
    void teste_dfeGrupos_dataVigente_retornaGrupos() throws Exception {
        mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    @DisplayName("Sem o parâmetro data usa a data atual e retorna grupos")
    void teste_dfeGrupos_semData_usaDataAtual() throws Exception {
        mockMvc.perform(get(URI)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    @DisplayName("Estrutura do grupo e dos tipos aninhados conforme contrato")
    void teste_dfeGrupos_estruturaAninhada() throws Exception {
        mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].titulo", notNullValue()))
                .andExpect(jsonPath("$[0].ordem", is(1)))
                .andExpect(jsonPath("$[0].tipos", notNullValue()))
                .andExpect(jsonPath("$[0].tipos.length()", greaterThan(0)))
                .andExpect(jsonPath("$[0].tipos[0].codigo", notNullValue()))
                .andExpect(jsonPath("$[0].tipos[0].titulo", notNullValue()))
                .andExpect(jsonPath("$[0].tipos[0].ordem", is(1)));
    }

    @Test
    @DisplayName("Grupos e tipos vêm ordenados (banco é fonte da verdade)")
    void teste_dfeGrupos_ordenadoEComCodigoNormalizado() throws Exception {
        mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Grupo 1 (MERCADORIAS) — ordenação sequencial por GRDF_ID
                .andExpect(jsonPath("$[0].titulo", is("MERCADORIAS")))
                .andExpect(jsonPath("$[0].ordem", is(1)))
                // codigo é a sigla normalizada usada como siglaDfe nos demais endpoints
                .andExpect(jsonPath("$[0].tipos[0].codigo", is("NFE")))
                .andExpect(jsonPath("$[0].tipos[0].modelo", is("55")))
                .andExpect(jsonPath("$[0].tipos[0].ordem", is(1)))
                .andExpect(jsonPath("$[0].tipos[1].codigo", is("NFCE")))
                .andExpect(jsonPath("$[0].tipos[1].ordem", is(2)))
                // Segundo grupo (TRANSPORTES)
                .andExpect(jsonPath("$[1].titulo", is("SERVIÇOS")))
                .andExpect(jsonPath("$[1].ordem", is(2)));
    }

    @Test
    @DisplayName("descricao ausente é omitida do payload (JsonInclude NON_NULL)")
    void teste_dfeGrupos_descricaoOmitida() throws Exception {
        mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipos[0].descricao").doesNotExist());
    }

    @Test
    @DisplayName("NF-e é suportada e permite NCM zero; CT-e é suportado sem NCM zero")
    void teste_dfeGrupos_suportadoEPermiteNcmZero_tiposSuportados() throws Exception {
        mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipos[?(@.codigo == 'NFE')].suportado", contains(true)))
                .andExpect(jsonPath("$[*].tipos[?(@.codigo == 'NFE')].permiteNcmZero", contains(true)))
                .andExpect(jsonPath("$[*].tipos[?(@.codigo == 'NFCE')].permiteNcmZero", contains(true)))
                .andExpect(jsonPath("$[*].tipos[?(@.codigo == 'CTE')].suportado", contains(true)))
                .andExpect(jsonPath("$[*].tipos[?(@.codigo == 'CTE')].permiteNcmZero", contains(false)));
    }

    @Test
    @DisplayName("NF-e ABI, DERE, DIR e Duimp (grupo 5) não são suportados pela calculadora")
    void teste_dfeGrupos_tiposNaoSuportados_grupo5() throws Exception {
        String grupo5 = "$[?(@.id == '5')].tipos";
        mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(grupo5 + "[?(@.codigo == 'NFEABI')].suportado", contains(false)))
                .andExpect(jsonPath(grupo5 + "[?(@.codigo == 'DERE')].suportado", contains(false)))
                .andExpect(jsonPath(grupo5 + "[?(@.codigo == 'DIR')].suportado", contains(false)))
                .andExpect(jsonPath(grupo5 + "[?(@.codigo == 'DUIMP')].suportado", contains(false)));
    }

    @Test
    @DisplayName("Todo tipo de todos os grupos traz suportado e permiteNcmZero preenchidos")
    void teste_dfeGrupos_flagsSemprePresentes() throws Exception {
        String json = mockMvc.perform(get(URI)
                .param("data", DATA_VIGENTE)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> tipos = JsonPath.read(json, "$[*].tipos[*]");
        assertThat(tipos).isNotEmpty().allSatisfy(tipo -> {
            assertThat(tipo).containsKeys("suportado", "permiteNcmZero");
            assertThat(tipo.get("suportado")).isInstanceOf(Boolean.class);
            assertThat(tipo.get("permiteNcmZero")).isInstanceOf(Boolean.class);
        });
    }
}
