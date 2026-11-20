package br.gov.serpro.rtc.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class MemoriaCalculoServiceTest {

    private static String formatar(String metodo, BigDecimal valor) {
        return ReflectionTestUtils.invokeMethod(MemoriaCalculoService.class, metodo, (Object) valor);
    }

    @ParameterizedTest
    @CsvSource({ "247.30000000, '247,30'", "1234.5, '1.234,50'" })
    void deveFormatarMoedaEmPtBr(BigDecimal valor, String esperado) {
        assertThat(formatar("formatarMoeda", valor)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({ "8.4, '8,40'", "0.05, '0,05'", "0.0375, '0,0375'" })
    void deveFormatarPercentualEmPtBr(BigDecimal valor, String esperado) {
        assertThat(formatar("formatarPercentual", valor)).isEqualTo(esperado);
    }

    @Test
    void deveFormatarNuloComoZero() {
        assertThat(formatar("formatarMoeda", null)).isEqualTo("0,00");
        assertThat(formatar("formatarPercentual", null)).isEqualTo("0,00");
    }

}
