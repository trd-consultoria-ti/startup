/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.config.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mapeia as propriedades dos caches Caffeine da aplicação a partir do prefixo
 * {@code cache}. Centraliza nome do cache, expiração após acesso, capacidade
 * inicial e tamanho máximo de cada região de cache.
 *
 * <p>Record imutável populado por constructor binding do
 * {@code @ConfigurationProperties}: por isso NÃO é {@code @Component} — o bean
 * é habilitado via {@code @EnableConfigurationProperties} em
 * {@link CaffeineCacheConfig}.
 */
@ConfigurationProperties(prefix = "cache")
public record CacheSpecs(List<CacheSpec> specs) {

    /** Parâmetros de uma região de cache Caffeine. */
    public record CacheSpec(
            String name,
            Duration expireAfterAccess,
            int initialCapacity,
            int maximumSize) {
    }

}
