package br.gov.serpro.rtc.config.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.DatabindException;

/**
 * Serializador para o tipo TcClassTrib (Código de Classificação Tributária do IBS/CBS, exatamente 6 dígitos).
 * Serializa String garantindo o padrão de 6 dígitos.
 */
public class StringTcClassTribSerializer extends ValueSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else if (!value.matches("\\d{6}")) {
            throw DatabindException.from(gen,
                    "TcClassTrib deve ser uma string de exatamente 6 dígitos numéricos");
        } else {
            gen.writeString(value);
        }
    }
}
