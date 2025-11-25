package io.swagger.repository;

import io.swagger.entity.MasterTemplateDefinitionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MasterTemplateDefinitionRepository
    extends R2dbcRepository<MasterTemplateDefinitionEntity, UUID> {

    Flux<MasterTemplateDefinitionEntity> findByIsActiveTrue();

    Flux<MasterTemplateDefinitionEntity> findByTemplateType(String templateType);

    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_active = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplates(Long currentDate);

    @Query("SELECT * FROM master_template_definition " +
           "WHERE master_template_id = :templateId " +
           "AND template_version = :version")
    Mono<MasterTemplateDefinitionEntity> findByIdAndVersion(
        UUID templateId,
        Integer version
    );
}
