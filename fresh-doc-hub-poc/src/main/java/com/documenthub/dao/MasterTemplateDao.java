package com.documenthub.dao;

import com.documenthub.dto.MasterTemplateDto;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.repository.MasterTemplateRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for MasterTemplateDefinition operations.
 * Provides a layer of abstraction over the repository with built-in caching.
 * Returns DTOs instead of entities to maintain layer separation.
 */
@Slf4j
@Component
public class MasterTemplateDao {

    private final MasterTemplateRepository repository;
    private final Cache<String, MasterTemplateDto> templateCache;

    public MasterTemplateDao(MasterTemplateRepository repository) {
        this.repository = repository;
        this.templateCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build();
    }

    /**
     * Find template by ID
     */
    public Mono<MasterTemplateDto> findById(UUID templateId) {
        log.debug("Finding template by id: {}", templateId);
        return repository.findById(templateId).map(this::toDto);
    }

    /**
     * Find template by type and version (uses cache)
     */
    public Mono<MasterTemplateDto> findByTypeAndVersion(String templateType, Integer templateVersion) {
        log.debug("Finding template by type and version: type={}, version={}", templateType, templateVersion);
        String cacheKey = buildCacheKey(templateType, templateVersion);

        // Check cache first
        MasterTemplateDto cached = templateCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Template cache hit: key={}", cacheKey);
            return Mono.just(cached);
        }

        log.debug("Template cache miss: key={}", cacheKey);
        return repository.findByTemplateTypeAndVersion(templateType, templateVersion)
            .map(this::toDto)
            .doOnNext(template -> {
                templateCache.put(cacheKey, template);
                log.debug("Template cached: key={}", cacheKey);
            });
    }

    private String buildCacheKey(String templateType, Integer templateVersion) {
        return templateType + ":" + templateVersion;
    }

    /**
     * Find active templates by line of business
     */
    public Flux<MasterTemplateDto> findActiveTemplatesByLineOfBusiness(
            String lineOfBusiness,
            Long currentDate) {
        log.debug("Finding active templates by LOB: {}", lineOfBusiness);
        return repository.findActiveTemplatesByLineOfBusiness(lineOfBusiness, currentDate)
            .map(this::toDto);
    }

    /**
     * Find active templates with filters
     */
    public Flux<MasterTemplateDto> findActiveTemplatesWithFilters(
            String lineOfBusiness,
            Boolean messageCenterDocFlag,
            String communicationType,
            Long currentDate) {
        log.debug("Finding templates with filters: lob={}, msgCenter={}, commType={}",
            lineOfBusiness, messageCenterDocFlag, communicationType);
        return repository.findActiveTemplatesWithFilters(
            lineOfBusiness, messageCenterDocFlag, communicationType, currentDate)
            .map(this::toDto);
    }

    /**
     * Find active templates by line of business and types
     */
    public Flux<MasterTemplateDto> findActiveTemplatesByLineOfBusinessAndTypes(
            String lineOfBusiness,
            List<String> templateTypes,
            Long currentDate) {
        log.debug("Finding templates by LOB and types: lob={}, types={}", lineOfBusiness, templateTypes);
        return repository.findActiveTemplatesByLineOfBusinessAndTypes(lineOfBusiness, templateTypes, currentDate)
            .map(this::toDto);
    }

    /**
     * Find active templates with all filters
     */
    public Flux<MasterTemplateDto> findActiveTemplatesWithAllFilters(
            String lineOfBusiness,
            List<String> templateTypes,
            Boolean messageCenterDocFlag,
            String communicationType,
            Long currentDate) {
        log.debug("Finding templates with all filters: lob={}, types={}, msgCenter={}, commType={}",
            lineOfBusiness, templateTypes, messageCenterDocFlag, communicationType);
        return repository.findActiveTemplatesWithAllFilters(
            lineOfBusiness, templateTypes, messageCenterDocFlag, communicationType, currentDate)
            .map(this::toDto);
    }

    /**
     * Find active shared templates by line of business
     */
    public Flux<MasterTemplateDto> findActiveSharedTemplatesByLineOfBusiness(
            String lineOfBusiness,
            Long currentDate) {
        log.debug("Finding shared templates by LOB: {}", lineOfBusiness);
        return repository.findActiveSharedTemplatesByLineOfBusiness(lineOfBusiness, currentDate)
            .map(this::toDto);
    }

    /**
     * Find all templates by type
     */
    public Flux<MasterTemplateDto> findByTemplateType(String templateType) {
        log.debug("Finding templates by type: {}", templateType);
        return repository.findByTemplateType(templateType).map(this::toDto);
    }

    /**
     * Find the latest active template by type (efficient single query)
     */
    public Mono<MasterTemplateDto> findLatestActiveTemplateByType(String templateType, Long currentDate) {
        log.debug("Finding latest active template by type: {}", templateType);
        return repository.findLatestActiveTemplateByType(templateType, currentDate).map(this::toDto);
    }

    /**
     * Invalidate cache for a template
     */
    public void invalidateCache(String templateType, Integer templateVersion) {
        String cacheKey = buildCacheKey(templateType, templateVersion);
        templateCache.invalidate(cacheKey);
        log.info("Template cache invalidated: key={}", cacheKey);
    }

    /**
     * Invalidate all cached templates
     */
    public void invalidateAllCache() {
        templateCache.invalidateAll();
        log.info("Template cache cleared");
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getStats() {
        var stats = templateCache.stats();
        return new CacheStats(
            templateCache.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate()
        );
    }

    /**
     * Cache statistics for monitoring
     */
    public record CacheStats(long size, long hitCount, long missCount, double hitRate) {}

    /**
     * Convert entity to DTO
     */
    private MasterTemplateDto toDto(MasterTemplateDefinitionEntity entity) {
        return MasterTemplateDto.builder()
            .masterTemplateId(entity.getMasterTemplateId())
            .templateVersion(entity.getTemplateVersion())
            .legacyTemplateId(entity.getLegacyTemplateId())
            .legacyTemplateName(entity.getLegacyTemplateName())
            .templateName(entity.getTemplateName())
            .templateDescription(entity.getTemplateDescription())
            .lineOfBusiness(entity.getLineOfBusiness())
            .templateCategory(entity.getTemplateCategory())
            .templateTypeOld(entity.getTemplateTypeOld())
            .templateType(entity.getTemplateType())
            .languageCode(entity.getLanguageCode())
            .owningDept(entity.getOwningDept())
            .notificationNeeded(entity.getNotificationNeeded())
            .regulatoryFlag(entity.getRegulatoryFlag())
            .messageCenterDocFlag(entity.getMessageCenterDocFlag())
            .displayName(entity.getDisplayName())
            .activeFlag(entity.getActiveFlag())
            .sharedDocumentFlag(entity.getSharedDocumentFlag())
            .sharingScope(entity.getSharingScope())
            .documentChannelOld(jsonToString(entity.getDocumentChannelOld()))
            .templateVariables(jsonToString(entity.getTemplateVariables()))
            .dataExtractionConfig(jsonToString(entity.getDataExtractionConfig()))
            .documentMatchingConfig(jsonToString(entity.getDocumentMatchingConfig()))
            .eligibilityCriteria(jsonToString(entity.getEligibilityCriteria()))
            .accessControl(jsonToString(entity.getAccessControl()))
            .requiredFields(jsonToString(entity.getRequiredFields()))
            .templateConfig(jsonToString(entity.getTemplateConfig()))
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .createdBy(entity.getCreatedBy())
            .createdTimestamp(entity.getCreatedTimestamp())
            .updatedBy(entity.getUpdatedBy())
            .updatedTimestamp(entity.getUpdatedTimestamp())
            .archiveIndicator(entity.getArchiveIndicator())
            .archiveTimestamp(entity.getArchiveTimestamp())
            .versionNumber(entity.getVersionNumber())
            .recordStatus(entity.getRecordStatus())
            .communicationType(entity.getCommunicationType())
            .workflow(entity.getWorkflow())
            .singleDocumentFlag(entity.getSingleDocumentFlag())
            .build();
    }

    /**
     * Convert Json to String, handling null
     */
    private String jsonToString(Json json) {
        return json != null ? json.asString() : null;
    }
}
