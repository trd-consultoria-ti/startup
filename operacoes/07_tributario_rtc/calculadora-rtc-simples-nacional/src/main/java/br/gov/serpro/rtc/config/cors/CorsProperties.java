/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.config.cors;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mapeia as propriedades de CORS da aplicação a partir do prefixo
 * {@code app.cors}. Centraliza as origens permitidas (externalizadas por
 * ambiente, sem hardcode de produção), métodos, headers e parâmetros de cache
 * do preflight.
 *
 * <p>Record imutável populado por constructor binding do
 * {@code @ConfigurationProperties}: por isso NÃO é {@code @Component} — o bean
 * é habilitado via {@code @EnableConfigurationProperties} em
 * {@link CorsConfig}.
 *
 * @param allowedOrigins       origens exatas permitidas (ex.: {@code http://localhost:4200}).
 * @param allowedOriginPatterns padrões de origem permitidos (ex.: {@code https://*.estaleiro.serpro.gov.br}),
 *                              úteis quando há subdomínios previsíveis por ambiente.
 * @param allowedMethods       métodos HTTP permitidos.
 * @param allowedHeaders       headers de requisição permitidos.
 * @param exposedHeaders       headers de resposta expostos ao browser além dos "simples".
 * @param allowCredentials     se envia/aceita cookies/credenciais — deve ser {@code false}.
 * @param maxAge               tempo de cache do preflight pelo browser.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedOriginPatterns,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        Duration maxAge) {
}
