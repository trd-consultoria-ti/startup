/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.config.cors;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Registra o bean {@link CorsConfigurationSource} consumido pela
 * {@code SecurityFilterChain} (via {@code http.cors(...)}).
 *
 * <p>A configuração é totalmente externalizada em {@link CorsProperties}
 * (prefixo {@code app.cors}), permitindo liberar origens por ambiente
 * (dev/des/prod) sem recompilar. Nunca usa {@code "*"} para origem e mantém
 * {@code allowCredentials=false}.
 */
@EnableConfigurationProperties(CorsProperties.class)
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        final CorsConfiguration config = new CorsConfiguration();

        // Origens exatas (ex.: http://localhost:4200). Nunca "*".
        final List<String> origins = nonBlank(properties.allowedOrigins());
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        }
        // Padrões de origem para subdomínios previsíveis por ambiente.
        final List<String> originPatterns = nonBlank(properties.allowedOriginPatterns());
        if (!originPatterns.isEmpty()) {
            config.setAllowedOriginPatterns(originPatterns);
        }

        config.setAllowedMethods(
                orDefault(properties.allowedMethods(), List.of("GET", "POST", "OPTIONS")));
        config.setAllowedHeaders(
                orDefault(properties.allowedHeaders(), List.of("Content-Type", "Authorization")));

        if (properties.exposedHeaders() != null && !properties.exposedHeaders().isEmpty()) {
            config.setExposedHeaders(properties.exposedHeaders());
        }

        config.setAllowCredentials(properties.allowCredentials());

        if (properties.maxAge() != null) {
            config.setMaxAge(properties.maxAge().getSeconds());
        }

        // Serviço expõe apenas endpoints do Simples Nacional: aplicar globalmente é aceitável.
        // O padrão "/**" é relativo ao context path (/api), cobrindo os caminhos publicados.
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> orDefault(List<String> value, List<String> fallback) {
        final List<String> filtered = nonBlank(value);
        return !filtered.isEmpty() ? filtered : fallback;
    }

    /** Remove entradas nulas/vazias (ex.: default vazio de variável de ambiente não definida). */
    private static List<String> nonBlank(List<String> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
    }
}
