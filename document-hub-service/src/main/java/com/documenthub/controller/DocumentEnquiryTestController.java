package com.documenthub.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simple test controller for document enquiry without complex dependencies
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class DocumentEnquiryTestController {

    private final DatabaseClient databaseClient;

    @PostMapping("/documents-enquiry")
    public Mono<Map<String, Object>> getDocuments(@RequestBody Map<String, Object> request) {
        log.info("Document enquiry request received: {}", request);

        UUID accountId = UUID.fromString((String) request.get("accountId"));
        UUID customerId = UUID.fromString((String) request.get("customerId"));

        // Query account-specific documents
        String accountDocsQuery = """
            SELECT
                si.storage_index_id::text as document_id,
                mt.template_name as display_name,
                mt.category,
                si.doc_type,
                si.doc_creation_date as date_posted,
                false as is_shared
            FROM storage_index si
            JOIN master_template_definition mt ON si.template_id = mt.template_id
            WHERE si.account_key = :accountId
              AND si.customer_key = :customerId
              AND si.archive_indicator = false
              AND si.is_accessible = true
            ORDER BY si.doc_creation_date DESC
            """;

        // Query shared documents
        String sharedDocsQuery = """
            SELECT
                template_id::text as document_id,
                template_name as display_name,
                category,
                doc_type,
                true as is_shared,
                sharing_scope
            FROM master_template_definition
            WHERE is_shared_document = true
              AND archive_indicator = false
              AND sharing_scope IN ('all', 'credit_card_account_only')
            ORDER BY effective_date DESC
            """;

        // Execute both queries
        Mono<List<Map<String, Object>>> accountDocs = databaseClient.sql(accountDocsQuery)
                .bind("accountId", accountId)
                .bind("customerId", customerId)
                .fetch()
                .all()
                .collectList();

        Mono<List<Map<String, Object>>> sharedDocs = databaseClient.sql(sharedDocsQuery)
                .fetch()
                .all()
                .collectList();

        // Combine results
        return Mono.zip(accountDocs, sharedDocs)
                .map(tuple -> {
                    List<Map<String, Object>> accountList = tuple.getT1();
                    List<Map<String, Object>> sharedList = tuple.getT2();

                    Map<String, Object> response = new HashMap<>();

                    // Capture original counts before merging
                    int accountSpecificCount = accountList.size();
                    int sharedCount = sharedList.size();

                    // Combine document lists
                    accountList.addAll(sharedList);
                    response.put("documentList", accountList);

                    // Add summary
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("totalDocuments", accountList.size());
                    summary.put("accountSpecificDocuments", accountSpecificCount);
                    summary.put("sharedDocuments", sharedCount);
                    response.put("summary", summary);

                    return response;
                });
    }

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP", "service", "document-enquiry-test"));
    }
}
