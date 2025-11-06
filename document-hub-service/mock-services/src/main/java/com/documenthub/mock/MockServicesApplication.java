package com.documenthub.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mock Services Application.
 * Provides mock endpoints for Customer Service, Account Service, and Transaction Service.
 * Used for testing the Document Hub Service without external dependencies.
 */
@SpringBootApplication
public class MockServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockServicesApplication.class, args);
    }
}
