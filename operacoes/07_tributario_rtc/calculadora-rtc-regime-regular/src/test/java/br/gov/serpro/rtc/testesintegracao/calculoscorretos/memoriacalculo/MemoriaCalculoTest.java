package br.gov.serpro.rtc.testesintegracao.calculoscorretos.memoriacalculo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class MemoriaCalculoTest {

    private static final String IBSCBS_1 = "$.objetos[0].tribCalc.IBSCBS";
    private static final String IBSCBS_2 = "$.objetos[1].tribCalc.IBSCBS";

    @Autowired
    private MockMvc mockMvc;

    @Value("classpath:entradas/calculoscorretos/memoria-calculo/entrada.json")
    private Resource entradaJson;

    @Test
    void deveGerarMemoriaCalculoNoGrupoIBSCBSInclusiveSemGIBSCBS() throws Exception {
        final String json = StreamUtils.copyToString(entradaJson.getInputStream(), StandardCharsets.UTF_8);

        mockMvc.perform(post("/calculadora/regime-geral")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())

            // item 1 (000001): gIBSCBS presente, sem memoriaCalculo nos grupos da NT
            .andExpect(jsonPath(IBSCBS_1 + ".gIBSCBS.gCBS").exists())
            .andExpect(jsonPath(IBSCBS_1 + ".gIBSCBS.gCBS.memoriaCalculo").doesNotExist())
            .andExpect(jsonPath(IBSCBS_1 + ".memoriaCalculo.cbs")
                    .value(allOf(containsString("R$ 200,00"), containsString("8,40%"))))
            .andExpect(jsonPath(IBSCBS_1 + ".memoriaCalculo.ibsUF").exists())
            .andExpect(jsonPath(IBSCBS_1 + ".memoriaCalculo.ibsMun").exists())

            // item 2 (410001): sem gIBSCBS, memoriaCalculo presente
            .andExpect(jsonPath(IBSCBS_2 + ".gIBSCBS").doesNotExist())
            .andExpect(jsonPath(IBSCBS_2 + ".memoriaCalculo.cbs").exists())
            .andExpect(jsonPath(IBSCBS_2 + ".memoriaCalculo.ibsUF").exists())
            .andExpect(jsonPath(IBSCBS_2 + ".memoriaCalculo.ibsMun").exists());
    }

}
