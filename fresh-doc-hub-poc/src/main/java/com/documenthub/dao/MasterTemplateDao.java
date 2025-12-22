package com.documenthub.dao;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.repository.MasterTemplateRepository;
import com.documenthub.service.TemplateCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for MasterTemplateDefinition operations.
 * Provides a layer of abstraction over the repository with caching support.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterTemplateDao {

    private final MasterTemplateRepository repository;
    private final TemplateCacheService cacheService;

    /**
     * Find template by ID
     */
    public Mono<MasterTemplateDefinitionEntity> findById(UUID templateId) {
        log.debug("Finding template by id: {}", templateId);
        return repository.findById(templateId);
    }

    /**
     * Find template by type and version (uses cache)
     */
    public Mono<MasterTemplateDefinitionEntity> findByTypeAndVersion(String templateType, Integer templateVersion) {
        log.debug("Finding template by type and version: type={}, version={}", templateType, templateVersion);
        return cacheService.getTemplate(templateType, templateVersion);
    }

    /**
     * Find active templates by line of business
     */
    public Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusiness(
            String lineOfBusiness,
            Long currentDate) {
        log.debug("Finding active templates by LOB: {}", lineOfBusiness);
        return repository.findActiveTemplatesByLineOfBusiness(lineOfBusiness, currentDate);
    }

    /**
     * Find active templates with filters
     */
    public Flux<MasterTemplateDefinitionEntity> findActiveTemplatesWithFilters(
            String lineOfBusiness,
            Boolean messageCenterDocFlag,
            String communicationType,
            Long currentDate) {
        log.debug("Finding templates with filters: lob={}, msgCenter={}, commType={}",
            lineOfBusiness, messageCenterDocFlag, communicationType);
        return repository.findActiveTemplatesWithFilters(
            lineOfBusiness, messageCenterDocFlag, communicationType, currentDate);
    }

    /**
     * Find active templates by line of business and types
     */
    public Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusinessAndTypes(
            String lineOfBusiness,
            List<String> templateTypes,
            Long currentDate) {
        log.debug("Finding templates by LOB and types: lob={}, types={}", lineOfBusiness, templateTypes);
        return repository.findActiveTemplatesByLineOfBusinessAndTypes(lineOfBusiness, templateTypes, currentDate);
    }

    /**
     * Find active templates with all filters
     */
    public Flux<MasterTemplateDefinitionEntity> findActiveTemplatesWithAllFilters(
            String lineOfBusiness,
            List<String> templateTypes,
            Boolean messageCenterDocFlag,
            String communicationType,
            Long currentDate) {
        log.debug("Finding templates with all filters: lob={}, types={}, msgCenter={}, commType={}",
            lineOfBusiness, templateTypes, messageCenterDocFlag, communicationType);
        return repository.findActiveTemplatesWithAllFilters(
            lineOfBusiness, templateTypes, messageCenterDocFlag, communicationType, currentDate);
    }

    /**
     * Find active shared templates by line of business
     */
    public Flux<MasterTemplateDefinitionEntity> findActiveSharedTemplatesByLineOfBusiness(
            String lineOfBusiness,
            Long currentDate) {
        log.debug("Finding shared templates by LOB: {}", lineOfBusiness);
        return repository.findActiveSharedTemplatesByLineOfBusiness(lineOfBusiness, currentDate);
    }

    /**
     * Find all templates by type
     */
    public Flux<MasterTemplateDefinitionEntity> findByTemplateType(String templateType) {
        log.debug("Finding templates by type: {}", templateType);
        return repository.findByTemplateType(templateType);
    }

    /**
     * Invalidate cache for a template
     */
    public void invalidateCache(String templateType, Integer templateVersion) {
        cacheService.invalidate(templateType, templateVersion);
    }

    /**
     * Invalidate all cached templates
     */
    public void invalidateAllCache() {
        cacheService.invalidateAll();
    }
}
