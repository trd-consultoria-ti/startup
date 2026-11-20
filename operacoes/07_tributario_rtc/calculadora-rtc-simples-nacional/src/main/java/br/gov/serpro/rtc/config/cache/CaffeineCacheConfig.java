/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.config.cache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Configura os caches Caffeine da aplicação e registra métricas de utilização.
 */
@EnableCaching
@EnableConfigurationProperties(CacheSpecs.class)
@Configuration
public class CaffeineCacheConfig {

	@Bean
	CacheManager cacheManager(CacheSpecs cacheSpec, MeterRegistry meterRegistry) {
	    final CaffeineCacheManager manager = new CaffeineCacheManager();
        cacheSpec
                .specs()
                .parallelStream()
                .forEach(t -> buildCache(manager, t, meterRegistry));
        return manager;
    }

	private static void buildCache(CaffeineCacheManager manager, CacheSpecs.CacheSpec cacheConfig, MeterRegistry meterRegistry) {
		final Cache<Object, Object> cache = Caffeine.newBuilder()
				.expireAfterAccess(cacheConfig.expireAfterAccess())
				.initialCapacity(cacheConfig.initialCapacity())
				.maximumSize(cacheConfig.maximumSize())
				.recordStats()
				.build();
		manager.registerCustomCache(cacheConfig.name(), cache);

		// Registra a métrica customizada de ocupação percentual
        Gauge.builder(
                "cache_occupancy_percent",
                cache,
                c -> {
                    long size = c.estimatedSize();
                    long maxSize = cacheConfig.maximumSize();
                    return maxSize > 0 ? (size * 100.0) / maxSize : 0.0;
                })
			.description("Percentual de ocupação do cache em relação ao tamanho máximo configurado")
			.tag("cache", cacheConfig.name())
			.tag("cache_manager", "cacheManager")
			.tag("name", cacheConfig.name())
			.register(meterRegistry);

		// Registra a métrica com o tamanho máximo configurado do cache
		Gauge.builder(
				"cache_max_size",
				cacheConfig,
				CacheSpecs.CacheSpec::maximumSize)
			.description("Tamanho máximo configurado do cache")
			.tag("cache", cacheConfig.name())
			.tag("cache_manager", "cacheManager")
			.tag("name", cacheConfig.name())
			.register(meterRegistry);
	}
}
