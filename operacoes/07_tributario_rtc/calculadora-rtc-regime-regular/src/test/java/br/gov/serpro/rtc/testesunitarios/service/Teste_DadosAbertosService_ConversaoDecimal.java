package br.gov.serpro.rtc.testesunitarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.serpro.rtc.domain.service.AliquotaPadraoService;
import br.gov.serpro.rtc.domain.service.dadosabertos.DadosAbertosService;

/**
 * Conversão dos números lidos do banco nos dados abertos: o número lido é o número gravado, dígito
 * a dígito. Um {@link Float} é convertido pelo texto do número, sem passar por {@code double} —
 * pelo {@code double}, 0,365 viraria 0,36500000953674316.
 */
@ExtendWith(MockitoExtension.class)
class Teste_DadosAbertosService_ConversaoDecimal {

    @Mock
    private AliquotaPadraoService aliquotaPadraoService;

    @InjectMocks
    private DadosAbertosService dadosAbertosService;

    @Test
    void floatEConvertidoPeloTextoDoNumero() throws Exception {
        assertEquals(new BigDecimal("0.365"), converter(Float.valueOf(0.365f)));
        assertEquals(new BigDecimal("123.4567"), converter(Float.valueOf(123.4567f)));
        assertEquals(new BigDecimal("60.0"), converter(Float.valueOf(60f)));
    }

    @Test
    void demaisTiposPreservamOValor() throws Exception {
        assertEquals(new BigDecimal("0.365"), converter(Double.valueOf(0.365d)));
        assertEquals(new BigDecimal("0.365"), converter(new BigDecimal("0.365")));
        assertEquals(new BigDecimal("0.365"), converter("0.365"));
        assertEquals(new BigDecimal("60"), converter(Integer.valueOf(60)));
        assertEquals(new BigDecimal("60"), converter(Long.valueOf(60L)));
        assertNull(converter(null));
        assertNull(converter("não é número"));
    }

    private BigDecimal converter(Object valor) throws Exception {
        final Method metodo = DadosAbertosService.class.getDeclaredMethod("convertToBigDecimal", Object.class);
        metodo.setAccessible(true);
        return (BigDecimal) metodo.invoke(dadosAbertosService, valor);
    }
}
