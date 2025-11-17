package com.example.eligibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main Application Class
 *
 * Drools Reactive Eligibility System
 *
 * This application determines which documents a customer is eligible to receive
 * based on database-driven rules and external API data.
 *
 * Features:
 * - Reactive (Spring WebFlux + R2DBC)
 * - Database-backed configuration (PostgreSQL JSONB)
 * - Drools rule engine for complex business logic
 * - Chained external API calls
 * - Caching for performance
 *
 * @author System
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
public class EligibilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(EligibilityApplication.class, args);
        System.out.println("\n===========================================");
        System.out.println("Drools Reactive Eligibility System Started");
        System.out.println("===========================================\n");
        System.out.println("API: http://localhost:8080/api/v1/eligibility");
        System.out.println("Health: http://localhost:8080/actuator/health\n");
    }
}
