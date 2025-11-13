package com.example.droolspoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Drools Reactive POC Application
 *
 * This POC demonstrates how to integrate Drools Rule Engine
 * with Spring WebFlux reactive architecture.
 *
 * Key concepts:
 * - Drools executes on dedicated thread pool (not event loop)
 * - Reactive data fetching with Mono/Flux
 * - Non-blocking request/response
 *
 * @author Document Hub Team
 * @since 2025-11-12
 */
@SpringBootApplication
public class DroolsReactivePocApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroolsReactivePocApplication.class, args);
    }
}
