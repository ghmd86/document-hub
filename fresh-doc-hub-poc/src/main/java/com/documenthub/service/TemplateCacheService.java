package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.repository.MasterTemplateRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for caching template definitions to reduce database queries.
 * Templates are cached by type+version key with configurable TTL.
 */
@Slf4j
@Service
public class TemplateCacheService {

    private final MasterTemplateRepository templateRepository;
    private final Cache<String, MasterTemplateDefinitionEntity> templateCache;

    public TemplateCacheService(MasterTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        this.templateCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build();
    }

    /**
     * Get template by type and version, using cache if available.
     * Returns empty Mono if template not found.
     *
     * @param templateType Template type
     * @param templateVersion Template version
     * @return Cached or freshly loaded template
     */
    public Mono<MasterTemplateDefinitionEntity> getTemplate(String templateType, Integer templateVersion) {
        String cacheKey = buildCacheKey(templateType, templateVersion);

        // Check cache first
        MasterTemplateDefinitionEntity cached = templateCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Template cache hit: key={}", cacheKey);
            return Mono.just(cached);
        }

        log.debug("Template cache miss: key={}", cacheKey);
        return templateRepository.findByTemplateTypeAndVersion(templateType, templateVersion)
            .doOnNext(template -> {
                templateCache.put(cacheKey, template);
                log.debug("Template cached: key={}", cacheKey);
            });
    }

    /**
     * Invalidate a specific template from cache
     */
    public void invalidate(String templateType, Integer templateVersion) {
        String cacheKey = buildCacheKey(templateType, templateVersion);
        templateCache.invalidate(cacheKey);
        log.info("Template cache invalidated: key={}", cacheKey);
    }

    /**
     * Invalidate all cached templates
     */
    public void invalidateAll() {
        templateCache.invalidateAll();
        log.info("Template cache cleared");
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getStats() {
        var stats = templateCache.stats();
        return new CacheStats(
            templateCache.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate()
        );
    }

    private String buildCacheKey(String templateType, Integer templateVersion) {
        return templateType + ":" + templateVersion;
    }

    /**
     * Cache statistics for monitoring
     */
    public record CacheStats(long size, long hitCount, long missCount, double hitRate) {}
}
