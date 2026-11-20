package br.gov.serpro.rtc.config.serializer;

import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Serializador para o tipo TDec_1104OpRTC (quantidade na RTC).
 * <p>
 * Padrão: 0\.[1-9]{1}[0-9]{3}|0\.[0-9]{3}[1-9]{1}|0\.[0-9]{2}[1-9]{1}[0-9]{1}|0\.[0-9]{1}[1-9]{1}[0-9]{2}|[1-9]{1}[0-9]{0,10}(\.[0-9]{4})?
 * <p>
 * Regras:
 * <ul>
 *   <li>Números inteiros: 1 a 11 dígitos, sem zeros à esquerda (ex: 1, 1000, 99999999999)</li>
 *   <li>Números decimais: exatamente 4 casas decimais (ex: 55.5555, 55.5500, 0.0001)</li>
 * </ul>
 *
 * Thread-safe: delega para {@link ArredondamentoUtils#formatarQuantidade(BigDecimal)}, que não guarda estado.
 * O serializer apenas formata a quantidade.
 */
public class BigDecimalTDec1104OpRTCSerializer extends ValueSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else if (value.signum() < 0) {
            throw new IllegalArgumentException("Números negativos não são permitidos para TDec_1104OpRTC");
        } else {
            gen.writeString(ArredondamentoUtils.formatarQuantidade(value));
        }
    }
}
