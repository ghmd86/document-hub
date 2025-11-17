package com.example.eligibility.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration
 *
 * Configures Caffeine cache for caching:
 * - Rule engine configuration
 * - Data sources
 * - Eligibility rules
 *
 * Cache is invalidated when database configuration changes.
 */
@Configuration
public class CacheConfig {

    /**
     * Cache Manager Bean
     *
     * Configures Caffeine as the cache provider.
     *
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "ruleEngineConfiguration",
                "dataSources",
                "eligibilityRules"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());

        return cacheManager;
    }
}
