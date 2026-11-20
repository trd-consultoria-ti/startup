package br.gov.serpro.rtc.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.gov.serpro.rtc.config.serializer.LocalDateCustomDeserializer;
import br.gov.serpro.rtc.config.serializer.OffsetDateTimeCustomDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Configura a personalização do Jackson, registrando deserializadores de datas
 * usados pela API.
 */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer customizer(LocalDateCustomDeserializer localDateDeserializer,
            OffsetDateTimeCustomDeserializer offsetDateTimeDeserializer) {
        SimpleModule dateModule = new SimpleModule("rtc-date-deserializers")
                .addDeserializer(LocalDate.class, localDateDeserializer)
                .addDeserializer(OffsetDateTime.class, offsetDateTimeDeserializer);
        return builder -> builder.addModule(dateModule);
    }

}
