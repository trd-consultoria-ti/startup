package br.gov.serpro.rtc.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class AssertUtils {

    /**
     * Compara o valor obtido com o esperado pelo valor numérico, sem arredondar o obtido. O objeto
     * de resposta guarda o número publicado, já arredondado no cálculo; o teste confere esse
     * número como ele é.
     */
    public static void isEqualByComparingTo(BigDecimal actual, String expected) {
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualByComparingTo(new BigDecimal(expected));
    }

    public static void isEqualByComparingTo(BigDecimal actual, BigDecimal expected) {
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualByComparingTo(expected);
    }

}
