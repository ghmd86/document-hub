package com.example.eligibility.service;

import com.example.eligibility.entity.DataSourceEntity;
import com.example.eligibility.entity.EligibilityRuleEntity;
import com.example.eligibility.repository.DataSourceRepository;
import com.example.eligibility.repository.EligibilityRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Configuration Loader Service
 *
 * Loads configuration from database (JSONB columns).
 *
 * Features:
 * - Loads data sources (external API configurations)
 * - Loads eligibility rules
 * - Caches results for performance
 * - Reactive (non-blocking)
 *
 * Cache is automatically invalidated when configuration changes.
 */
@Service
public class ConfigurationLoaderService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationLoaderService.class);

    private final DataSourceRepository dataSourceRepository;
    private final EligibilityRuleRepository ruleRepository;

    public ConfigurationLoaderService(
            DataSourceRepository dataSourceRepository,
            EligibilityRuleRepository ruleRepository
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.ruleRepository = ruleRepository;
    }

    /**
     * Load all enabled data sources from database
     *
     * Results are cached for performance.
     *
     * @return Mono of list of data source entities
     */
    @Cacheable("dataSources")
    public Mono<List<DataSourceEntity>> loadDataSources() {
        log.debug("Loading data sources from database");

        return dataSourceRepository.findByEnabledTrue()
                .collectList()
                .doOnSuccess(sources ->
                        log.info("Loaded {} data sources", sources.size())
                )
                .doOnError(error ->
                        log.error("Error loading data sources: {}", error.getMessage())
                );
    }

    /**
     * Load all enabled eligibility rules from database
     *
     * Results are cached for performance.
     * Rules are ordered by priority (higher priority first).
     *
     * @return Mono of list of eligibility rule entities
     */
    @Cacheable("eligibilityRules")
    public Mono<List<EligibilityRuleEntity>> loadEligibilityRules() {
        log.debug("Loading eligibility rules from database");

        return ruleRepository.findByEnabledTrueOrderByPriorityDesc()
                .collectList()
                .doOnSuccess(rules ->
                        log.info("Loaded {} eligibility rules", rules.size())
                )
                .doOnError(error ->
                        log.error("Error loading eligibility rules: {}", error.getMessage())
                );
    }
}
