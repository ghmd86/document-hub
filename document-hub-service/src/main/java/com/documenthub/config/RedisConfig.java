package com.documenthub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Redis caching.
 * Enables caching for customer profile, account details, and other frequently accessed data.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Configure Redis cache manager with different TTL for different caches.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Customer profile cache: 30 minutes TTL
        cacheConfigurations.put("customerProfile", createCacheConfiguration(Duration.ofMinutes(30)));

        // Customer segment cache: 60 minutes TTL
        cacheConfigurations.put("customerSegment", createCacheConfiguration(Duration.ofMinutes(60)));

        // Customer type cache: 60 minutes TTL
        cacheConfigurations.put("customerType", createCacheConfiguration(Duration.ofMinutes(60)));

        // Account details cache: 60 minutes TTL
        cacheConfigurations.put("accountDetails", createCacheConfiguration(Duration.ofMinutes(60)));

        // Account balance cache: 5 minutes TTL (more volatile)
        cacheConfigurations.put("accountBalance", createCacheConfiguration(Duration.ofMinutes(5)));

        // Account arrangements cache: 60 minutes TTL
        cacheConfigurations.put("accountArrangements", createCacheConfiguration(Duration.ofMinutes(60)));

        // Account product cache: 60 minutes TTL
        cacheConfigurations.put("accountProduct", createCacheConfiguration(Duration.ofMinutes(60)));

        // Account type cache: 60 minutes TTL
        cacheConfigurations.put("accountType", createCacheConfiguration(Duration.ofMinutes(60)));

        // Account LOB cache: 60 minutes TTL
        cacheConfigurations.put("accountLOB", createCacheConfiguration(Duration.ofMinutes(60)));

        // Transaction summary cache: 15 minutes TTL
        cacheConfigurations.put("transactionSummary", createCacheConfiguration(Duration.ofMinutes(15)));

        // Default cache configuration: 30 minutes TTL
        RedisCacheConfiguration defaultCacheConfig = createCacheConfiguration(Duration.ofMinutes(30));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Create cache configuration with specified TTL.
     */
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper())));
    }

    /**
     * Reactive Redis template for manual cache operations.
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .key(new StringRedisSerializer())
                .value(new GenericJackson2JsonRedisSerializer(objectMapper()))
                .hashKey(new StringRedisSerializer())
                .hashValue(new GenericJackson2JsonRedisSerializer(objectMapper()))
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    /**
     * ObjectMapper for Redis serialization.
     */
    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return objectMapper;
    }
}
