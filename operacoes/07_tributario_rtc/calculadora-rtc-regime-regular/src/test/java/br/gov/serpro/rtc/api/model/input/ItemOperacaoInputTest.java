package br.gov.serpro.rtc.api.model.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.gov.serpro.rtc.domain.model.entity.TipoDfe;

/**
 * Testes unitários para o método {@link ItemOperacaoInput#ajustarNcm(TipoDfe)}
 * e seus auxiliares {@link ItemOperacaoInput#deveLimparNcm(TipoDfe, String)} e
 * {@link ItemOperacaoInput#isNcmZero(String)}.
 * 
 * Valida a normalização de NCM zerado conforme o tipo de documento fiscal.
 */
class ItemOperacaoInputTest {

    // ===== Constantes de NCM =====
    // NCM zerado
    private static final String NCM_ZERO_CURTO = "00";           // NCM zerado (2 dígitos)
    private static final String NCM_ZERO_LONGO = "00000000";     // NCM zerado (8 dígitos)
    
    // NCM válido
    private static final String NCM_VALIDO = "24021000";         // NCM válido (exemplo real)
    
    // ===== Testes do método principal: ajustaNcm() =====

    @ParameterizedTest(name = "permiteNcmZero={0}, NCM {1} → Esperado: {2}")
    @MethodSource("fornecerCasosAjustaNcm")
    @DisplayName("Validar ajustaNcm(): respeita indicador permiteNcmZero")
    void ajustaNcm_DeveRespeitarPermiteNcmZero(boolean permiteNcmZero, String ncm, String ncmEsperado) {
        // Arrange
    	var tipoDfe = criarTipoDfeComPermissao(permiteNcmZero);

    	var item = new ItemOperacaoInput();
        item.setNcm(ncm);

        // Act
        item.ajustarNcm(tipoDfe);

        // Assert
        assertThat(item.getNcm())
            .as("permiteNcmZero=%s: NCM %s deveria resultar em %s", permiteNcmZero, ncm, ncmEsperado)
            .isEqualTo(ncmEsperado);
    }

     /**
      * Fornece os casos de teste para ajustaNcm().
      * Testa todos os tipos de DFE com diferentes valores de NCM.
      */
      static Stream<Arguments> fornecerCasosAjustaNcm() {
          return Stream.of(
              // Quando permiteNcmZero=true: deve limpar NCM zero
              arguments(true, NCM_ZERO_CURTO, null),
              arguments(true, NCM_ZERO_LONGO, null),
              // Quando permiteNcmZero=true: deve manter NCM válido
              arguments(true, NCM_VALIDO, NCM_VALIDO),
              // Quando permiteNcmZero=false: deve manter NCM zero (não limpa)
              arguments(false, NCM_ZERO_CURTO, NCM_ZERO_CURTO),
              arguments(false, NCM_ZERO_LONGO, NCM_ZERO_LONGO),
              // Quando permiteNcmZero=false: deve manter NCM válido
              arguments(false, NCM_VALIDO, NCM_VALIDO),
              // Edge case: NCM nulo permanece nulo
              arguments(true, null, null),
              arguments(false, null, null)
          );
      }

    @Test
    @DisplayName("Não deve lançar exceção quando TipoDfe é null")
    void naoDeveLimparNcmQuandoTipoDfeEhNull() {
        // Arrange - Cenário único não coberto pelo teste parametrizado
        var item = new ItemOperacaoInput();
        item.setNcm(NCM_ZERO_CURTO);

        // Act & Assert - não deve lançar exceção
        item.ajustarNcm(null);

        // Assert
        assertThat(item.getNcm()).isEqualTo(NCM_ZERO_CURTO);
    }

    /**
     * Cria uma instância de {@code TipoDfe} com permissão de NCM zero configurada.
     * 
     * @param permiteNcmZero se NCM zero é permitido para este tipo
     * @return instância configurada de TipoDfe
     */
    private TipoDfe criarTipoDfeComPermissao(boolean permiteNcmZero) {
    	var tipoDfe = new TipoDfe();
    	tipoDfe.setPermiteNcmZero(permiteNcmZero);
    	return tipoDfe;
    }
}
