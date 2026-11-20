package br.gov.serpro.rtc.config.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.DatabindException;

/**
 * Serializador para o elemento uTrib (unidade de medida), baseado no tipo TStringRTC,
 * com tamanho mínimo 1 e máximo 6 caracteres.
 */
public class StringUTribSerializer extends ValueSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else if (value.isEmpty() || value.length() > 6) {
            throw DatabindException.from(gen, "uTrib deve ter entre 1 e 6 caracteres");
        } else {
            gen.writeString(value);
        }
    }
}
