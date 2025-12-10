package com.documenthub.repository;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

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
}
