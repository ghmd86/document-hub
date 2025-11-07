package com.documenthub.repository;

import com.documenthub.model.entity.MasterTemplateDefinition;
import com.documenthub.model.entity.TemplateId;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reactive repository for MasterTemplateDefinition entity.
 * Provides methods to query template definitions and sharing configurations.
 * Uses composite primary key (template_id, version).
 */
@Repository
public interface MasterTemplateDefinitionRepository extends R2dbcRepository<MasterTemplateDefinition, TemplateId> {

    /**
     * Find template by composite key (template_id, version).
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE template_id = :templateId AND version = :version")
    Mono<MasterTemplateDefinition> findByTemplateIdAndVersion(
            @Param("templateId") UUID templateId,
            @Param("version") Integer version
    );

    /**
     * Find all versions of a template.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE template_id = :templateId " +
           "ORDER BY version DESC")
    Flux<MasterTemplateDefinition> findAllVersionsByTemplateId(
            @Param("templateId") UUID templateId
    );

    /**
     * Find the latest active version of a template.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE template_id = :templateId " +
           "AND template_status = 'APPROVED' " +
           "AND archive_indicator = FALSE " +
           "AND effective_date <= :referenceDate " +
           "AND (valid_until IS NULL OR valid_until > :referenceDate) " +
           "ORDER BY version DESC " +
           "LIMIT 1")
    Mono<MasterTemplateDefinition> findActiveVersionByTemplateId(
            @Param("templateId") UUID templateId,
            @Param("referenceDate") OffsetDateTime referenceDate
    );

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
     * Find template by category code and doc type.
     */
    Flux<MasterTemplateDefinition> findByCategoryCodeAndDocType(String categoryCode, String docType);

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
           "AND template_status = 'APPROVED' " +
           "AND archive_indicator = FALSE")
    Flux<MasterTemplateDefinition> findSharedDocumentsByScopes(
            @Param("scopes") String[] scopes
    );

    /**
     * Find all approved shared documents for evaluation.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_shared_document = true " +
           "AND template_status = 'APPROVED' " +
           "AND archive_indicator = FALSE " +
           "AND (valid_until IS NULL OR valid_until > :currentTime)")
    Flux<MasterTemplateDefinition> findActiveSharedDocuments(
            @Param("currentTime") OffsetDateTime currentTime
    );

    /**
     * Find templates by category code.
     */
    Flux<MasterTemplateDefinition> findByCategoryCode(String categoryCode);

    /**
     * Find templates by doc type.
     */
    Flux<MasterTemplateDefinition> findByDocType(String docType);

    /**
     * Find templates by template name.
     */
    Flux<MasterTemplateDefinition> findByTemplateName(String templateName);

    /**
     * Find shared documents for custom rule evaluation.
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_shared_document = true " +
           "AND sharing_scope = 'custom_rule' " +
           "AND template_status = 'APPROVED' " +
           "AND archive_indicator = FALSE " +
           "AND data_extraction_config IS NOT NULL")
    Flux<MasterTemplateDefinition> findSharedDocumentsWithCustomRules();

    /**
     * Find active templates (approved and within validity period).
     */
    @Query("SELECT * FROM master_template_definition " +
           "WHERE template_status = 'APPROVED' " +
           "AND archive_indicator = FALSE " +
           "AND effective_date <= :currentTime " +
           "AND (valid_until IS NULL OR valid_until > :currentTime)")
    Flux<MasterTemplateDefinition> findAllActiveTemplates(
            @Param("currentTime") OffsetDateTime currentTime
    );
}
