package com.example.droolspoc.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Drools Configuration for Reactive Integration
 *
 * Sets up:
 * 1. KieContainer - Compiles and holds DRL rules
 * 2. Scheduler - Dedicated thread pool for Drools execution
 *
 * The Scheduler ensures Drools blocking operations don't block the reactive event loop.
 */
@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Value("${drools.thread-pool.core-size:20}")
    private int threadPoolSize;

    @Value("${drools.thread-pool.thread-name-prefix:drools-}")
    private String threadNamePrefix;

    @Value("${drools.thread-pool.ttl-seconds:60}")
    private int ttlSeconds;

    /**
     * Create dedicated Scheduler for Drools rule execution.
     *
     * Sizing formula: (Requests/sec) × (Avg execution time in seconds)
     * Example: 1000 req/sec × 0.015s = 15 threads + 33% buffer = 20 threads
     */
    @Bean(name = "droolsScheduler")
    public Scheduler droolsScheduler() {
        log.info("Creating Drools Scheduler with {} threads", threadPoolSize);

        return Schedulers.newBoundedElastic(
            threadPoolSize,              // Thread cap
            Integer.MAX_VALUE,           // Queue size (unbounded)
            threadNamePrefix,            // Thread name prefix
            ttlSeconds,                  // TTL for idle threads
            true                         // Daemon threads
        );
    }

    /**
     * Create KieContainer that compiles and holds all DRL rules.
     *
     * Rules loaded from classpath:rules/*.drl at startup.
     * Compilation happens once, execution many times (fast).
     */
    @Bean
    public KieContainer kieContainer() {
        log.info("Initializing Drools KieContainer...");

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        try {
            // Load all .drl files from classpath:rules/
            PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:rules/*.drl");

            if (resources.length == 0) {
                throw new RuntimeException(
                    "No DRL rule files found in classpath:rules/. " +
                    "Add at least one .drl file to src/main/resources/rules/"
                );
            }

            log.info("Loading {} DRL rule file(s)", resources.length);

            // Add each DRL file to Kie file system
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                log.info("  - Loading rule file: {}", filename);

                kieFileSystem.write(
                    "src/main/resources/rules/" + filename,
                    kieServices.getResources()
                        .newInputStreamResource(resource.getInputStream())
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load Drools rule files", e);
        }

        // Build rules
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        // Check for compilation errors
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException(
                "Failed to compile Drools rules:\n" +
                kieBuilder.getResults().toString()
            );
        }

        // Log warnings
        if (kieBuilder.getResults().hasMessages(Message.Level.WARNING)) {
            log.warn("Drools rule compilation warnings:\n{}",
                kieBuilder.getResults().toString());
        }

        log.info("Drools rules compiled successfully");

        return kieServices.newKieContainer(
            kieServices.getRepository().getDefaultReleaseId()
        );
    }
}
