package com.documenthub.repository;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Master Template Definition
 */
@Repository
public interface MasterTemplateRepository extends R2dbcRepository<MasterTemplateDefinitionEntity, UUID> {

    /**
     * Find all active templates within date range
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplates(Long currentDate);

    /**
     * Find templates by type
     */
    Flux<MasterTemplateDefinitionEntity> findByTemplateType(String templateType);

    /**
     * Find shared templates
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND shared_document_flag = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveSharedTemplates(Long currentDate);

    /**
     * Find active templates filtered by line of business.
     * Returns templates that match the specified lineOfBusiness OR have lineOfBusiness = 'ENTERPRISE'.
     * ENTERPRISE templates are always included as they apply to all lines of business.
     *
     * This is STEP 1 of the two-step filtering:
     * 1. Filter by line_of_business (which business unit's templates)
     * 2. Then filter by sharing_scope (who can access within those templates)
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusiness(
        String lineOfBusiness,
        Long currentDate
    );

    /**
     * Find active templates filtered by line of business, only shared templates.
     * Used when querying specifically for shared documents.
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND shared_document_flag = true " +
           "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveSharedTemplatesByLineOfBusiness(
        String lineOfBusiness,
        Long currentDate
    );

    /**
     * Find active templates filtered by line of business AND template types (documentTypes).
     * The template_type field in the database corresponds to DocumentTypes in the API.
     *
     * @param lineOfBusiness The line of business filter (e.g., CREDIT_CARD, DIGITAL_BANK)
     * @param templateTypes List of template types to filter by (maps to documentTypes in API)
     * @param currentDate Current epoch time for date range validation
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
           "AND template_type IN (:templateTypes) " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusinessAndTypes(
        String lineOfBusiness,
        List<String> templateTypes,
        Long currentDate
    );

    /**
     * Find active templates filtered by line of business with additional filters.
     * Supports filtering by:
     * - messageCenterDocFlag: If true, only return templates where message_center_doc_flag = true
     * - communicationType: If not null, filter by communication_type
     *
     * This is the main query method for document-enquiry with P0 filters applied.
     *
     * @param lineOfBusiness The line of business filter (e.g., CREDIT_CARD, DIGITAL_BANK)
     * @param messageCenterDocFlag If true, filter to message center eligible templates only
     * @param communicationType Filter by communication type (LETTER, EMAIL, SMS, PUSH)
     * @param currentDate Current epoch time for date range validation
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
           "AND (:messageCenterDocFlag = false OR message_center_doc_flag = true) " +
           "AND (:communicationType IS NULL OR communication_type = :communicationType) " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplatesWithFilters(
        String lineOfBusiness,
        Boolean messageCenterDocFlag,
        String communicationType,
        Long currentDate
    );

    /**
     * Find active templates filtered by line of business, template types, and additional filters.
     * Combines all filter criteria into a single query.
     *
     * @param lineOfBusiness The line of business filter (e.g., CREDIT_CARD, DIGITAL_BANK)
     * @param templateTypes List of template types to filter by (maps to documentTypes in API)
     * @param messageCenterDocFlag If true, filter to message center eligible templates only
     * @param communicationType Filter by communication type (LETTER, EMAIL, SMS, PUSH)
     * @param currentDate Current epoch time for date range validation
     */
    @Query("SELECT * FROM document_hub.master_template_definition " +
           "WHERE active_flag = true " +
           "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
           "AND template_type IN (:templateTypes) " +
           "AND (:messageCenterDocFlag = false OR message_center_doc_flag = true) " +
           "AND (:communicationType IS NULL OR communication_type = :communicationType) " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplatesWithAllFilters(
        String lineOfBusiness,
        List<String> templateTypes,
        Boolean messageCenterDocFlag,
        String communicationType,
        Long currentDate
    );
}
