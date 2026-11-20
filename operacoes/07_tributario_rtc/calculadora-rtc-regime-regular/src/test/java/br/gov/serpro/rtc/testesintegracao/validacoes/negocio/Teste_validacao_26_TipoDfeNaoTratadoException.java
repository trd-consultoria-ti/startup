/*
* Versão de Homologação/Testes
*/
package br.gov.serpro.rtc.testesintegracao.validacoes.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import br.gov.serpro.rtc.api.exceptionhandler.enumeration.ProblemType;
import br.gov.serpro.rtc.api.model.input.OperacaoInput;
import br.gov.serpro.rtc.domain.service.CalculadoraService;
import br.gov.serpro.rtc.domain.service.ObservabilidadeService;
import br.gov.serpro.rtc.domain.service.collector.ErrosCalculoException;
import br.gov.serpro.rtc.domain.service.exception.TipoDfeNaoTratadoException;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class Teste_validacao_26_TipoDfeNaoTratadoException {
	
    // ===== Constantes de Códigos de DFE =====
	// Documentos tratados pela calculadora
    private static final int CODIGO_NFE = 55;
    private static final int CODIGO_CTE = 57;
    private static final int CODIGO_NFCOM = 62;
    private static final int CODIGO_BPE = 63;
    private static final int CODIGO_NFCE = 65;
    private static final int CODIGO_NF3E = 66;
    private static final int CODIGO_CTEOS = 67;
    private static final int CODIGO_NFAG = 75;
    private static final int CODIGO_NFGAS = 76;
    private static final int CODIGO_BPETA = 83;
    private static final int CODIGO_NFSE = 91;
    //private static final int CODIGO_NFSEVIA = 92;
    //private static final int CODIGO_BPETM = 93;
    //private static final int CODIGO_CTESIMPLIFICADO = 97;
    
    // Documentos não tratados pela calculadora
    private static final int CODIGO_NFEABI = 77;
    private static final int CODIGO_DERE = 94;
    private static final int CODIGO_DIR = 95;
    private static final int CODIGO_DUIMP = 96;
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CalculadoraService calculadoraService;

    @Autowired
    private ObservabilidadeService observabilidadeService;
    
    private OperacaoInput operacao;
    
    private static final ProblemType ERRO = ProblemType.TIPO_DFE_NAO_TRATADO;

    @BeforeEach
    void beforeEach(
            final @Value("classpath:entradas/validacoes/Teste_validacao_26_TipoDfeNaoTratadoException.json") Resource resourceFile)
            throws Exception {
        operacao = objectMapper.readValue(resourceFile.getInputStream(), OperacaoInput.class);
    }

    @ParameterizedTest
    @MethodSource("cenariosDFeTratados")
    @DisplayName("Serviço: Deve calcular tributos com sucesso para DFe tratado (tpDoc: {0})")
    void servicoDeveCalcularTributosComSucessoParaDFeTratado(int tpDoc) {
    	operacao.setTpDoc(tpDoc);
    	assertThatNoException().isThrownBy(() -> calculadoraService.calcularTributos(operacao));
    }

    @ParameterizedTest
    @MethodSource("cenariosDFeNaoTratados")
    @DisplayName("Serviço: Deve lançar TipoDfeNaoTratadoException para DFe não tratado (tpDoc: {0})")
    void servicoDeveLancarExcecaoParaDFeNaoTratado(int tpDoc) {
        operacao.setTpDoc(tpDoc);
        assertThatThrownBy(() -> calculadoraService.calcularTributos(operacao))
                .isExactlyInstanceOf(TipoDfeNaoTratadoException.class);
    }
    
    @ParameterizedTest
    @MethodSource("cenariosDFeTratados")
    @DisplayName("API: Deve retornar 200 OK para DFe tratado (tpDoc: {0})")
    void apiDeveRetornarOkParaDFeTratado(int tpDoc) throws Exception {
    	operacao.setTpDoc(tpDoc);
        final String jsonContent = objectMapper.writeValueAsString(operacao);
        mockMvc.perform(post("/calculadora/regime-geral")
                .contentType(APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().is(OK.value()));
    }

    @ParameterizedTest
    @MethodSource("cenariosDFeNaoTratados")
    @DisplayName("API: Deve retornar 422 UNPROCESSABLE_ENTITY para DFe não tratado (tpDoc: {0})")
    void apiDeveRetornarErro422ParaDFeNaoTratado(int tpDoc) throws Exception {
    	operacao.setTpDoc(tpDoc);
        final String jsonContent = objectMapper.writeValueAsString(operacao);
        mockMvc.perform(post("/calculadora/regime-geral")
                .contentType(APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().is(UNPROCESSABLE_ENTITY.value()))
                .andExpect(jsonPath("$.title").value(ERRO.getTitulo()));
    }
    
    // ===== Testes de Observabilidade =====

    @ParameterizedTest
    @MethodSource("cenariosDFeTratados")
    @DisplayName("Observabilidade - Serviço: Deve processar com sucesso para DFe tratado (tpDoc: {0})")
	void observabilidadeServicoDeveProcessarComSucessoParaDFeTratado(int tpDoc) {
    	operacao.setTpDoc(tpDoc);
    	assertThatNoException().isThrownBy(() -> observabilidadeService.processarOperacao(operacao, "http://localhost:8080"));
	}

    @ParameterizedTest
    @MethodSource("cenariosDFeNaoTratados")
    @DisplayName("Observabilidade - Serviço: Deve lançar ErrosCalculoException para DFe não tratado (tpDoc: {0})")
    void observabilidadeServicoDeveLancarExcecaoParaDFeNaoTratado(int tpDoc) {
        operacao.setTpDoc(tpDoc);
        assertThatThrownBy(() -> observabilidadeService.processarOperacao(operacao, "http://localhost:8080"))
                .isExactlyInstanceOf(ErrosCalculoException.class).satisfies(ex -> {
					ErrosCalculoException errosEx = (ErrosCalculoException) ex;
					assertThat(errosEx.getErros()).isNotNull().hasSize(1).
						filteredOn(erro -> erro.code().equals(ERRO.getCodigoErro().codigo())).isNotEmpty().
						filteredOn(erro -> erro.title().equals(ERRO.getTitulo())).isNotEmpty();
				});
    }
    
    @ParameterizedTest
    @MethodSource("cenariosDFeTratados")
    @DisplayName("Observabilidade - API: Deve retornar 200 OK para DFe tratado (tpDoc: {0})")
    void observabilidadeApiDeveRetornarOkParaDFeTratado(int tpDoc) throws Exception {
        operacao.setTpDoc(tpDoc);
        final String jsonContent = objectMapper.writeValueAsString(operacao);
        
        mockMvc.perform(post("/calculadora/observabilidade/regime-geral")
                .contentType(APPLICATION_JSON)
                .content(jsonContent))
        		.andExpect(status().is(OK.value()));      
    }

    @ParameterizedTest
    @MethodSource("cenariosDFeNaoTratados")
    @DisplayName("Observabilidade - API: Deve retornar 422 UNPROCESSABLE_ENTITY para DFe não tratado (tpDoc: {0})")
    void observabilidadeApiDeveRetornarErro422ParaDFeNaoTratado(int tpDoc) throws Exception {
        operacao.setTpDoc(tpDoc);
        final String jsonContent = objectMapper.writeValueAsString(operacao);
        
        mockMvc.perform(post("/calculadora/observabilidade/regime-geral")
                .contentType(APPLICATION_JSON)
                .content(jsonContent))
        		.andExpect(status().is(UNPROCESSABLE_ENTITY.value()))
                .andExpect(jsonPath("$.errors[0].title").value(ERRO.getTitulo()));      
    }
    
    static Stream<Arguments> cenariosDFeTratados() {
    	return Stream.of(
    			arguments(CODIGO_NFE), arguments(CODIGO_CTE), arguments(CODIGO_NFCOM), arguments(CODIGO_BPE),
    			arguments(CODIGO_NFCE), arguments(CODIGO_NF3E), arguments(CODIGO_CTEOS), arguments(CODIGO_NFAG),
    			arguments(CODIGO_NFGAS), arguments(CODIGO_BPETA), arguments(CODIGO_NFSE)
    			/*, arguments(CODIGO_NFSEVIA), arguments(CODIGO_BPETM), arguments(CODIGO_CTESIMPLIFICADO)*/);
    }
    
    static Stream<Arguments> cenariosDFeNaoTratados() {
        return Stream.of(arguments(CODIGO_NFEABI), arguments(CODIGO_DERE), arguments(CODIGO_DIR), arguments(CODIGO_DUIMP));
    }    

}
