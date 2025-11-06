package com.documenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Main application class for Document Hub Service.
 * This service provides reactive endpoints for document enquiry with advanced custom rule engine.
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class DocumentHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentHubApplication.class, args);
    }
}
