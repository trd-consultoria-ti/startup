/*
* Versão de Homologação/Testes
*/
package br.gov.serpro.rtc.testesintegracao.validacoes.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import tools.jackson.databind.ObjectMapper;

import br.gov.serpro.rtc.api.exceptionhandler.enumeration.ProblemType;
import br.gov.serpro.rtc.api.model.input.OperacaoInput;
import br.gov.serpro.rtc.domain.model.enumeration.EstadoItemEnum;
import br.gov.serpro.rtc.domain.service.CalculadoraService;
import br.gov.serpro.rtc.domain.service.ObservabilidadeService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class Teste_validacao_22_ErroFaltaImplementacaoException {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CalculadoraService calculadoraService;

    @Autowired
    private ObservabilidadeService observabilidadeService;
    
    private OperacaoInput operacao; 
    
    private static final ProblemType ERRO = ProblemType.ERRO_FALTA_IMPLEMENTACAO;

    @BeforeEach
    void beforeEach(
            final @Value("classpath:entradas/validacoes/Teste_validacao_22_ErroFaltaImplementacaoException.json") Resource resourceFile)
            throws Exception {
        operacao = objectMapper.readValue(resourceFile.getInputStream(), OperacaoInput.class);
    }

    @Test
    @DisplayName("Deve lançar ErroFaltaImplementacaoException ao calcular tributos no serviço")
    void deveLancarErroFaltaImplementacaoNoServico() {
        assertThatThrownBy(() -> calculadoraService.calcularTributos(operacao))
                .isExactlyInstanceOf(ERRO.getClasseErro());
    }

    @Test
    @DisplayName("Deve retornar erro ao calcular tributos na API")
    void deveRetornarErroNoController() throws Exception {
        final String jsonContent = objectMapper.writeValueAsString(operacao);
        mockMvc.perform(post("/calculadora/regime-geral")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.title").value(ERRO.getTitulo()));
    }
    
    @Test
    @DisplayName("Deve processar operação com estado NAO_IMPLEMENTADO no serviço de observabilidade")
    void deveProcessarComEstadoNaoImplementadoNoServicoObservabilidade() {
    	final var roc = observabilidadeService.processarOperacao(operacao, "http://localhost:8080");

        assertThat(roc).isNotNull();

        final var objetos = roc.getObjetos();
        assertThat(objetos).isNotNull().hasSize(1);
        assertThat(objetos.get(0).getEstadoItem()).hasToString(EstadoItemEnum.NAO_IMPLEMENTADO.name());
        assertThat(objetos.get(0).getErros()).isNotNull().hasSize(1)
        	.filteredOn(erro -> erro.code().equals(ERRO.getCodigoErro().codigo())).isNotEmpty()
        	.filteredOn(erro -> erro.title().equals(ERRO.getTitulo())).isNotEmpty();        
    }

    @Test
    @DisplayName("Deve retornar operação com estado NAO_IMPLEMENTADO no controller de observabilidade")
    void deveRetornarComEstadoNaoImplementadoNoControllerObservabilidade() throws Exception {
        final String jsonContent = objectMapper.writeValueAsString(operacao);
        mockMvc.perform(post("/calculadora/observabilidade/regime-geral")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.objetos[0].estadoItem").value(EstadoItemEnum.NAO_IMPLEMENTADO.name()))
                .andExpect(jsonPath("$.objetos[0].erros[0].code").value(ERRO.getCodigoErro().codigo()))
                .andExpect(jsonPath("$.objetos[0].erros[0].title").value(ERRO.getTitulo()));        
    }

}
