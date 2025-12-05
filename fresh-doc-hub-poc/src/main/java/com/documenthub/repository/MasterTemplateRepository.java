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
           "WHERE active_flag::boolean = true " +
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
           "WHERE active_flag::boolean = true " +
           "AND shared_document_flag::boolean = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveSharedTemplates(Long currentDate);
}
