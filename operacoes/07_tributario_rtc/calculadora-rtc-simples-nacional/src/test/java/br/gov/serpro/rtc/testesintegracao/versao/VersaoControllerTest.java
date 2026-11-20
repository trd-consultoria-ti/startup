package br.gov.serpro.rtc.testesintegracao.versao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integração do endpoint de informações de versão ({@code /versao}),
 * disponível em todos os perfis, e da restrição do comparador
 * ({@code /versao/status}) ao perfil {@code offline}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testes")
class VersaoControllerTest {

    private static final String ENDPOINT = "/versao";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveInformarAsVersoesDaInstalacao() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-CALC-APP-VERSION"))
                .andExpect(header().exists("X-CALC-DB-VERSION"))
                .andExpect(jsonPath("$.versaoApp", Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.versaoDb", Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.descricaoVersaoDb", Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.dataVersaoDb", Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
                .andExpect(jsonPath("$.arquivoDb").value("calculadora-pro.db"))
                .andExpect(jsonPath("$.ambiente").value("testes"));
    }

    @Test
    void naoDeveExporOComparadorDeStatusForaDoPerfilOffline() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/status"))
                .andExpect(status().isNotFound());
    }
}
