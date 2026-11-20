package br.gov.serpro.rtc.config.serializer;

import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Serializador para o tipo TDec_0302_04 (até 3 inteiros, de 2 a 4 decimais).
 * Exemplo válido: 0.01, 0.1234, 123.12, 999.9999
 *
 * Thread-safe: delega para {@link ArredondamentoUtils#formatarAliquota(BigDecimal)}, que não guarda estado.
 * A alíquota chega do cálculo já arredondada a 4 casas; o serializer apenas a formata, com 2 a 4 casas.
 */
public class BigDecimalTDec0302_04Serializer extends ValueSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else if (value.signum() < 0) {
            throw new IllegalArgumentException("Números negativos não são permitidos para TDec_0302_04");
        } else {
            gen.writeString(ArredondamentoUtils.formatarAliquota(value));
        }
    }
}