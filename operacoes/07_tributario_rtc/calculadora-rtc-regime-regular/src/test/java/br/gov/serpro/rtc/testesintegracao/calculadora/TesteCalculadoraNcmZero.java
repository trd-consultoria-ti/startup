/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.calculadora;

import static br.gov.serpro.rtc.domain.model.enumeration.EstadoItemEnum.CALCULADO;
import static br.gov.serpro.rtc.domain.model.enumeration.EstadoItemEnum.INCONSISTENCIA_ENTRADA;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.StreamUtils;

import tools.jackson.databind.ObjectMapper;

import br.gov.serpro.rtc.api.model.input.OperacaoInput;
import br.gov.serpro.rtc.domain.model.enumeration.EstadoItemEnum;

/**
 * Testes de integração para validar o cenário de NCM zero.
 * 
 * Regra: NCM "00" ou "00000000" é permitido APENAS para tpDoc 55 (NF-e) e 65 (NFC-e).
 * Para outros tipos de documento, NCM zerado deve ser rejeitado.
 * Também, NCM zerado com tpDoc nulo não é permitido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class TesteCalculadoraNcmZero {

    // ===== Constantes de Códigos de DFE =====
    private static final Integer CODIGO_NFE = 55;
    private static final Integer CODIGO_CTE = 57;
    private static final Integer CODIGO_NFCOM = 62;
    private static final Integer CODIGO_BPE = 63;
    private static final Integer CODIGO_NFCE = 65;
    private static final Integer CODIGO_NF3E = 66;
    private static final Integer CODIGO_CTEOS = 67;
    private static final Integer CODIGO_NFAG = 75;
    private static final Integer CODIGO_NFGAS = 76;
    private static final Integer CODIGO_NFEABI = 77;
    private static final Integer CODIGO_BPETA = 83;
    private static final Integer CODIGO_NFSE = 91;
    private static final Integer CODIGO_NFSEVIA = 92;
    private static final Integer CODIGO_BPETM = 93;
    private static final Integer CODIGO_DERE = 94;
    private static final Integer CODIGO_DIR = 95;
    private static final Integer CODIGO_DUIMP = 96;
    private static final Integer CODIGO_CTESIMPLIFICADO = 97;

    // ===== Constantes de NCM =====
    private static final String NCM_ZERO_2_DIGITOS = "00";
	private static final String NCM_ZERO_8_DIGITOS = "00000000";

	@Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:entradas/calculadora/ncm-zero/base_operacao.json")
    private Resource baseOperacaoResource;

    @ParameterizedTest(name = "tpDoc={0}, NCM={1} → Esperado: {2}")
    @MethodSource("fornecerCenariosNcmZero")
    @DisplayName("Validar cenários de NCM zero conforme tpDoc")
    void teste_ncm_zero_conforme_tipo_documento(Integer tpDoc, String ncm, HttpStatus statusEsperado) throws Exception {
        // Arrange & Act
        var jsonContent = prepararOperacao(tpDoc, ncm);

        // Assert
        mockMvc.perform(post("/calculadora/regime-geral")
        		.contentType(APPLICATION_JSON)
        		.content(jsonContent))
        		.andExpect(status().is(statusEsperado.value()));
    }

    @ParameterizedTest(name = "Observabilidade - tpDoc={0}, NCM={1} → Esperado: {2} ({3})")
    @MethodSource("fornecerCenariosNcmZeroObservabilidade")
    @DisplayName("Validar cenários de NCM zero conforme tpDoc (Observabilidade)")
    void teste_ncm_zero_conforme_tipo_documento_observabilidade(Integer tpDoc, String ncm, HttpStatus statusEsperado, EstadoItemEnum estadoItemEsperado) throws Exception {
        // Arrange & Act
        var jsonContent = prepararOperacao(tpDoc, ncm);

        // Assert
        ResultActions perform = mockMvc.perform(post("/calculadora/observabilidade/regime-geral")
        		.contentType(APPLICATION_JSON)
        		.content(jsonContent));
        perform.andExpect(status().is(statusEsperado.value()));
        if (estadoItemEsperado == null) {
        	perform.andExpect(jsonPath("$.objetos[0].estadoItem").doesNotExist());
        } else {
        	perform.andExpect(jsonPath("$.objetos[0].estadoItem").value(estadoItemEsperado.name()));
        }
    }

    /**
     * Fornece os cenários de teste para NCM zero.
     * 
     * @return Stream de Arguments contendo (tpDoc, ncm, statusEsperado)
     *         - HttpStatus.OK: teste deve passar (sucesso)
     *         - HttpStatus.NOT_FOUND: teste deve falhar (NCM zerado não permitido)
     */
    static Stream<Arguments> fornecerCenariosNcmZero() {
        return Stream.of(
            // ===== Cenários de Sucesso (Status HttpStatus.OK) =====
            // NFe (tpDoc 55) permite NCM zero
            arguments(CODIGO_NFE, NCM_ZERO_2_DIGITOS, OK),
            arguments(CODIGO_NFE, NCM_ZERO_8_DIGITOS, OK),
            
            // NFCe (tpDoc 65) permite NCM zero
            arguments(CODIGO_NFCE, NCM_ZERO_2_DIGITOS, OK),
            arguments(CODIGO_NFCE, NCM_ZERO_8_DIGITOS, OK),
            
            // ===== Cenários de Falha (Status HttpStatus.NOT_FOUND) - NCM zerado NÃO permitido =====
            // CTe (tpDoc 57) NÃO permite NCM zero
            arguments(CODIGO_CTE, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_CTE, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NFCom (tpDoc 62) NÃO permite NCM zero
            arguments(CODIGO_NFCOM, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_NFCOM, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // BPe (tpDoc 63) NÃO permite NCM zero
            arguments(CODIGO_BPE, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_BPE, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NF3-e (tpDoc 66) NÃO permite NCM zero
            arguments(CODIGO_NF3E, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_NF3E, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // CT-e OS (tpDoc 67) NÃO permite NCM zero
            arguments(CODIGO_CTEOS, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_CTEOS, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NFAg (tpDoc 75) NÃO permite NCM zero
            arguments(CODIGO_NFAG, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_NFAG, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NFGas (tpDoc 76) NÃO permite NCM zero
            arguments(CODIGO_NFGAS, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_NFGAS, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NF-e ABI (tpDoc 77) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_NFEABI, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY),
            arguments(CODIGO_NFEABI, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY),
            
            // BP-e TA (tpDoc 83) NÃO permite NCM zero
            arguments(CODIGO_BPETA, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_BPETA, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NFS-e (tpDoc 91) NÃO permite NCM zero
            arguments(CODIGO_NFSE, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_NFSE, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // NFS-e Via (tpDoc 92) NÃO permite NCM zero
            arguments(CODIGO_NFSEVIA, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_NFSEVIA, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // BP-e TM (tpDoc 93) NÃO permite NCM zero
            arguments(CODIGO_BPETM, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_BPETM, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // DERE (tpDoc 94) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_DERE, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY),
            arguments(CODIGO_DERE, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY),
            
            // DIR (tpDoc 95) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_DIR, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY),
            arguments(CODIGO_DIR, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY),
            
            // Duimp (tpDoc 96) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_DUIMP, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY),
            arguments(CODIGO_DUIMP, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY),
            
            // CT-e Simplificado (tpDoc 97) NÃO permite NCM zero
            arguments(CODIGO_CTESIMPLIFICADO, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(CODIGO_CTESIMPLIFICADO, NCM_ZERO_8_DIGITOS, NOT_FOUND),
            
            // tpDoc nulo NÃO permite NCM zero
            arguments(null, NCM_ZERO_2_DIGITOS, NOT_FOUND),
            arguments(null, NCM_ZERO_8_DIGITOS, NOT_FOUND)
        );
    }
    
    /**
     * Fornece os cenários de teste para NCM zero com observabilidade.
     * @return
     */
    static Stream<Arguments> fornecerCenariosNcmZeroObservabilidade() {
        return Stream.of(
            // ===== Cenários de Sucesso (Status HttpStatus.OK) - CALCULADO =====
            // NFe (tpDoc 55) permite NCM zero
            arguments(CODIGO_NFE, NCM_ZERO_2_DIGITOS, OK, CALCULADO),
            arguments(CODIGO_NFE, NCM_ZERO_8_DIGITOS, OK, CALCULADO),
            
            // NFCe (tpDoc 65) permite NCM zero
            arguments(CODIGO_NFCE, NCM_ZERO_2_DIGITOS, OK, CALCULADO),
            arguments(CODIGO_NFCE, NCM_ZERO_8_DIGITOS, OK, CALCULADO),
            
            // ===== Cenários de Falha (Status HttpStatus.OK) - NCM zerado NÃO permitido - INCONSISTENCIA_ENTRADA =====
            // CTe (tpDoc 57) NÃO permite NCM zero
            arguments(CODIGO_CTE, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_CTE, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NFCom (tpDoc 62) NÃO permite NCM zero
            arguments(CODIGO_NFCOM, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_NFCOM, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // BPe (tpDoc 63) NÃO permite NCM zero
            arguments(CODIGO_BPE, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_BPE, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NF3-e (tpDoc 66) NÃO permite NCM zero
            arguments(CODIGO_NF3E, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_NF3E, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // CT-e OS (tpDoc 67) NÃO permite NCM zero
            arguments(CODIGO_CTEOS, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_CTEOS, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NFAg (tpDoc 75) NÃO permite NCM zero
            arguments(CODIGO_NFAG, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_NFAG, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NFGas (tpDoc 76) NÃO permite NCM zero
            arguments(CODIGO_NFGAS, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_NFGAS, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NF-e ABI (tpDoc 77) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_NFEABI, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY, null),
            arguments(CODIGO_NFEABI, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY, null),
            
            // BP-e TA (tpDoc 83) NÃO permite NCM zero
            arguments(CODIGO_BPETA, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_BPETA, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NFS-e (tpDoc 91) NÃO permite NCM zero
            arguments(CODIGO_NFSE, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_NFSE, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // NFS-e Via (tpDoc 92) NÃO permite NCM zero
            arguments(CODIGO_NFSEVIA, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_NFSEVIA, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // BP-e TM (tpDoc 93) NÃO permite NCM zero
            arguments(CODIGO_BPETM, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_BPETM, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // DERE (tpDoc 94) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_DERE, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY, null),
            arguments(CODIGO_DERE, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY, null),
            
            // DIR (tpDoc 95) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_DIR, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY, null),
            arguments(CODIGO_DIR, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY, null),
            
            // Duimp (tpDoc 96) NÃO permite NCM zero -- documento não tratado
            arguments(CODIGO_DUIMP, NCM_ZERO_2_DIGITOS, UNPROCESSABLE_ENTITY, null),
            arguments(CODIGO_DUIMP, NCM_ZERO_8_DIGITOS, UNPROCESSABLE_ENTITY, null),
            
            // CT-e Simplificado (tpDoc 97) NÃO permite NCM zero
            arguments(CODIGO_CTESIMPLIFICADO, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(CODIGO_CTESIMPLIFICADO, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            
            // tpDoc nulo NÃO permite NCM zero
            arguments(null, NCM_ZERO_2_DIGITOS, OK, INCONSISTENCIA_ENTRADA),
            arguments(null, NCM_ZERO_8_DIGITOS, OK, INCONSISTENCIA_ENTRADA)
        );
    }
    
    /**
     * Carrega a operação base, modifica tpDoc e NCM, e retorna o JSON serializado.
     */
    private String prepararOperacao(Integer tpDoc, String ncm) throws Exception {
		String jsonBase = StreamUtils.copyToString(baseOperacaoResource.getInputStream(), UTF_8);
        
        OperacaoInput operacao = objectMapper.readValue(jsonBase, OperacaoInput.class);
        operacao.setTpDoc(tpDoc);
        operacao.getItens().get(0).setNcm(ncm);
        
        return objectMapper.writeValueAsString(operacao);
    }

}