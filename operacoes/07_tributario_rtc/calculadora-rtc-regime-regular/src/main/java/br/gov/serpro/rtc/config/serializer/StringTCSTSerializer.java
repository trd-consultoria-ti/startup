package br.gov.serpro.rtc.config.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.DatabindException;

/**
 * Serializador para o tipo TCST (Código Situação Tributária do IBS/CBS, exatamente 3 dígitos).
 */
public class StringTCSTSerializer extends ValueSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else if (!value.matches("\\d{3}")) {
            throw DatabindException.from(gen, "TCST deve ser uma string de exatamente 3 dígitos numéricos");
        } else {
            gen.writeString(value);
        }
    }
}
