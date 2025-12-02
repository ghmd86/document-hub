package com.documenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Document Hub POC - Spring Boot Application
 *
 * This POC demonstrates a dynamic rule-based document retrieval system
 * that supports both account-specific and shared documents with
 * configurable access control rules.
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class DocumentHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentHubApplication.class, args);
    }
}
