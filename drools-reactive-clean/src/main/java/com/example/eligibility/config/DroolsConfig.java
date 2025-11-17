package com.example.eligibility.config;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Drools Configuration
 *
 * Configures Drools rule engine components.
 *
 * Key Concepts:
 * - KieServices: Entry point to Drools API
 * - KieContainer: Container for compiled rules
 * - Scheduler: Thread pool for rule evaluation (blocking operation)
 */
@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Value("${drools.thread-pool-size:10}")
    private int threadPoolSize;

    /**
     * KieServices Bean
     *
     * Provides access to Drools API.
     *
     * @return KieServices instance
     */
    @Bean
    public KieServices kieServices() {
        log.info("Initializing Drools KieServices");
        return KieServices.Factory.get();
    }

    /**
     * KieContainer Bean
     *
     * Container for compiled Drools rules.
     * In this application, rules are dynamically generated from database configuration,
     * so we use the default KieContainer.
     *
     * @param kieServices KieServices instance
     * @return KieContainer instance
     */
    @Bean
    public KieContainer kieContainer(KieServices kieServices) {
        log.info("Creating Drools KieContainer");
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        log.info("KieContainer created successfully");
        return kieContainer;
    }

    /**
     * Drools Scheduler Bean
     *
     * Thread pool for executing rule evaluation (blocking operation).
     * Allows reactive services to offload blocking Drools calls to a separate thread pool.
     *
     * @return Bounded elastic scheduler for Drools
     */
    @Bean(name = "droolsScheduler")
    public Scheduler droolsScheduler() {
        log.info("Creating Drools scheduler with thread pool size: {}", threadPoolSize);
        return Schedulers.newBoundedElastic(
                threadPoolSize,
                Integer.MAX_VALUE,
                "drools-eval",
                60,
                true
        );
    }
}
