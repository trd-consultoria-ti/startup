package br.gov.serpro.rtc.api.aliquotasimples.serializer;

import java.math.BigDecimal;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 *
 * Serializa uma alíquota com a precisão INTEGRAL como TEXTO JSON em notação
 * decimal, sem arredondar nem alterar a escala: {@code "0.000068"}, nunca
 * {@code 6.8E-5}; {@code "1.6839379378389463610262954"}.
 *
 * <p>
 * Texto, e não número: um número JSON com 28 dígitos é convertido em
 * {@code double} pelos leitores comuns ({@code JSON.parse} em JavaScript, o
 * módulo {@code json} do Python) e perde os dígitos finais. Usado nas
 * alíquotas do GET de alíquotas (fração) e em {@code resolucaoAliquotas} do
 * cálculo assistido (pontos percentuais).
 *
 * <p>
 *
 * ISOLADO. Serializer próprio do pacote {@code aliquotasimples}. Valor nulo é
 * serializado como {@code null} pelo Jackson antes de chegar aqui.
 *
 */
public class BigDecimalTextoIntegralSerializer extends ValueSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.toPlainString());
        }
    }
}
