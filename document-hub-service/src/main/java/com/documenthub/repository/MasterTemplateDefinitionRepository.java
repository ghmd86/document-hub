package com.documenthub.repository;

import com.documenthub.model.entity.MasterTemplateDefinition;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for MasterTemplateDefinition entity.
 * Provides methods to query template definitions and sharing configurations.
 */
@Repository
public interface MasterTemplateDefinitionRepository extends R2dbcRepository<MasterTemplateDefinition, UUID> {

    /**
     * Find all shared documents.
     */
    Flux<MasterTemplateDefinition> findByIsSharedDocument(Boolean isSharedDocument);

    /**
     * Find shared documents by sharing scope.
     */
    Flux<MasterTemplateDefinition> findByIsSharedDocumentAndSharingScope(
            Boolean isSharedDocument,
            String sharingScope
    );

    /**
     * Find template by category and doc type.
     */
    Flux<MasterTemplateDefinition> findByCategoryAndDocType(String category, String docType);

    /**
     * Find template by line of business.
     */
    Flux<MasterTemplateDefinition> findByLineOfBusiness(String lineOfBusiness);

    /**
     * Find shared documents with specific sharing scopes.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_shared_document = true " +
           "AND sharing_scope IN (:scopes) " +
           "AND template_status = 'Approved'")
    Flux<MasterTemplateDefinition> findSharedDocumentsByScopes(
            @Param("scopes") String[] scopes
    );

    /**
     * Find all approved shared documents for evaluation.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_shared_document = true " +
           "AND template_status = 'Approved' " +
           "AND (valid_until IS NULL OR valid_until > :currentTime)")
    Flux<MasterTemplateDefinition> findActiveSharedDocuments(
            @Param("currentTime") Long currentTime
    );

    /**
     * Find template by ID with effective date validation.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE template_id = :templateId " +
           "AND effective_date <= :currentTime " +
           "AND (valid_until IS NULL OR valid_until > :currentTime)")
    Mono<MasterTemplateDefinition> findActiveTemplateById(
            @Param("templateId") UUID templateId,
            @Param("currentTime") Long currentTime
    );

    /**
     * Find templates by category.
     */
    Flux<MasterTemplateDefinition> findByCategory(String category);

    /**
     * Find templates by doc type.
     */
    Flux<MasterTemplateDefinition> findByDocType(String docType);

    /**
     * Find shared documents for custom rule evaluation.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_shared_document = true " +
           "AND sharing_scope = 'custom_rule' " +
           "AND template_status = 'Approved' " +
           "AND data_extraction_schema IS NOT NULL")
    Flux<MasterTemplateDefinition> findSharedDocumentsWithCustomRules();
}
