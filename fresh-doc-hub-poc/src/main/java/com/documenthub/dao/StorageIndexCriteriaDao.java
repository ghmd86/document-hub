package com.documenthub.dao;

import com.documenthub.dto.DocumentQueryParamsDto;
import com.documenthub.dto.StorageIndexDto;
import com.documenthub.entity.StorageIndexEntity;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Criteria-based DAO for StorageIndex queries.
 *
 * Uses R2dbcEntityTemplate with Criteria API for dynamic query building.
 * This approach is more flexible than @Query for queries with many optional filters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageIndexCriteriaDao {

    private final R2dbcEntityTemplate template;

    /**
     * Find documents using dynamic criteria based on query parameters.
     * Supports all query modes: account-specific, shared, reference key, auto-discover.
     */
    public Flux<StorageIndexDto> findDocuments(DocumentQueryParamsDto params) {
        Criteria criteria = buildCriteria(params);
        Query query = Query.query(criteria);

        log.debug("Executing criteria query for template: {}",
            params.getTemplate().getTemplateType());

        return template.select(StorageIndexEntity.class)
            .matching(query)
            .all()
            .map(this::toDto);
    }

    /**
     * Find account-specific documents (non-shared).
     */
    public Flux<StorageIndexDto> findAccountDocuments(DocumentQueryParamsDto params) {
        Criteria criteria = Criteria.empty()
            .and(baseCriteria())
            .and(templateCriteria(params))
            .and(validityPeriodCriteria(params))
            .and(postedDateCriteria(params))
            .and("account_key").is(params.getAccountId())
            .and("shared_flag").is(false);

        log.debug("Finding account documents: accountId={}, template={}",
            params.getAccountId(), params.getTemplate().getTemplateType());

        return template.select(StorageIndexEntity.class)
            .matching(Query.query(criteria))
            .all()
            .map(this::toDto);
    }

    /**
     * Find shared documents by template.
     */
    public Flux<StorageIndexDto> findSharedDocuments(DocumentQueryParamsDto params) {
        Criteria criteria = Criteria.empty()
            .and(baseCriteria())
            .and(templateCriteria(params))
            .and(validityPeriodCriteria(params))
            .and(postedDateCriteria(params))
            .and("shared_flag").is(true);

        log.debug("Finding shared documents: template={}",
            params.getTemplate().getTemplateType());

        return template.select(StorageIndexEntity.class)
            .matching(Query.query(criteria))
            .all()
            .map(this::toDto);
    }

    /**
     * Find documents by reference key (extracted or direct mode).
     */
    public Flux<StorageIndexDto> findByReferenceKey(
            String referenceKey,
            String referenceKeyType,
            DocumentQueryParamsDto params) {

        Criteria criteria = Criteria.empty()
            .and(baseCriteria())
            .and(templateCriteria(params))
            .and(validityPeriodCriteria(params))
            .and(postedDateCriteria(params))
            .and("reference_key").is(referenceKey)
            .and("reference_key_type").is(referenceKeyType);

        log.debug("Finding by reference key: key={}, type={}, template={}",
            referenceKey, referenceKeyType, params.getTemplate().getTemplateType());

        return template.select(StorageIndexEntity.class)
            .matching(Query.query(criteria))
            .all()
            .map(this::toDto);
    }

    /**
     * Find documents by reference key type only (auto-discover mode).
     * Does not filter by specific reference key value.
     */
    public Flux<StorageIndexDto> findByReferenceKeyType(
            String referenceKeyType,
            DocumentQueryParamsDto params) {

        Criteria criteria = Criteria.empty()
            .and(baseCriteria())
            .and(templateCriteria(params))
            .and(validityPeriodCriteria(params))
            .and(postedDateCriteria(params))
            .and("reference_key_type").is(referenceKeyType);

        log.debug("Finding by reference key type (auto-discover): type={}, template={}",
            referenceKeyType, params.getTemplate().getTemplateType());

        return template.select(StorageIndexEntity.class)
            .matching(Query.query(criteria))
            .all()
            .map(this::toDto);
    }

    /**
     * Find documents by customer key.
     */
    public Flux<StorageIndexDto> findByCustomerKey(
            UUID customerKey,
            DocumentQueryParamsDto params) {

        Criteria criteria = Criteria.empty()
            .and(baseCriteria())
            .and(templateCriteria(params))
            .and(validityPeriodCriteria(params))
            .and(postedDateCriteria(params))
            .and("customer_key").is(customerKey);

        log.debug("Finding by customer key: customerId={}, template={}",
            customerKey, params.getTemplate().getTemplateType());

        return template.select(StorageIndexEntity.class)
            .matching(Query.query(criteria))
            .all()
            .map(this::toDto);
    }

    // ========================================================================
    // Criteria Builders
    // ========================================================================

    /**
     * Build complete criteria from query parameters.
     */
    private Criteria buildCriteria(DocumentQueryParamsDto params) {
        Criteria criteria = Criteria.empty()
            .and(baseCriteria())
            .and(templateCriteria(params))
            .and(validityPeriodCriteria(params))
            .and(postedDateCriteria(params));

        // Add optional filters based on params
        if (params.getAccountId() != null) {
            criteria = criteria.and("account_key").is(params.getAccountId());
        }

        if (params.getRequestReferenceKey() != null) {
            criteria = criteria.and("reference_key").is(params.getRequestReferenceKey());
        }

        if (params.getRequestReferenceKeyType() != null) {
            criteria = criteria.and("reference_key_type").is(params.getRequestReferenceKeyType());
        }

        return criteria;
    }

    /**
     * Base criteria applied to all queries.
     * Only returns accessible documents.
     */
    private Criteria baseCriteria() {
        return Criteria.where("accessible_flag").is(true);
    }

    /**
     * Template type and version criteria.
     */
    private Criteria templateCriteria(DocumentQueryParamsDto params) {
        Criteria criteria = Criteria.where("template_type")
            .is(params.getTemplate().getTemplateType());

        if (params.getTemplate().getTemplateVersion() != null) {
            criteria = criteria.and("template_version")
                .is(params.getTemplate().getTemplateVersion());
        }

        return criteria;
    }

    /**
     * Document validity period criteria (start_date/end_date).
     * Ensures document is currently valid.
     */
    private Criteria validityPeriodCriteria(DocumentQueryParamsDto params) {
        Long currentDate = System.currentTimeMillis();

        // (start_date IS NULL OR start_date <= currentDate)
        // AND (end_date IS NULL OR end_date >= currentDate)
        Criteria startCriteria = Criteria.where("start_date").isNull()
            .or("start_date").lessThanOrEquals(currentDate);

        Criteria endCriteria = Criteria.where("end_date").isNull()
            .or("end_date").greaterThanOrEquals(currentDate);

        return startCriteria.and(endCriteria);
    }

    /**
     * Posted date range criteria (doc_creation_date).
     * Filters by when document was created/posted.
     */
    private Criteria postedDateCriteria(DocumentQueryParamsDto params) {
        Criteria criteria = Criteria.empty();

        if (params.getPostedFromDate() != null) {
            criteria = criteria.and("doc_creation_date")
                .greaterThanOrEquals(params.getPostedFromDate());
        }

        if (params.getPostedToDate() != null) {
            criteria = criteria.and("doc_creation_date")
                .lessThanOrEquals(params.getPostedToDate());
        }

        return criteria;
    }

    // ========================================================================
    // Entity <-> DTO Converter
    // ========================================================================

    private StorageIndexDto toDto(StorageIndexEntity entity) {
        return StorageIndexDto.builder()
            .storageIndexId(entity.getStorageIndexId())
            .masterTemplateId(entity.getMasterTemplateId())
            .templateVersion(entity.getTemplateVersion())
            .templateType(entity.getTemplateType())
            .storageVendor(entity.getStorageVendor())
            .referenceKey(entity.getReferenceKey())
            .referenceKeyType(entity.getReferenceKeyType())
            .accountKey(entity.getAccountKey())
            .customerKey(entity.getCustomerKey())
            .storageDocumentKey(entity.getStorageDocumentKey())
            .fileName(entity.getFileName())
            .docCreationDate(entity.getDocCreationDate())
            .accessibleFlag(entity.getAccessibleFlag())
            .docMetadata(jsonToString(entity.getDocMetadata()))
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .sharedFlag(entity.getSharedFlag())
            .generationVendorId(entity.getGenerationVendorId())
            .createdBy(entity.getCreatedBy())
            .createdTimestamp(entity.getCreatedTimestamp())
            .updatedBy(entity.getUpdatedBy())
            .updatedTimestamp(entity.getUpdatedTimestamp())
            .archiveIndicator(entity.getArchiveIndicator())
            .archiveTimestamp(entity.getArchiveTimestamp())
            .versionNumber(entity.getVersionNumber())
            .recordStatus(entity.getRecordStatus())
            .build();
    }

    private String jsonToString(Json json) {
        return json != null ? json.asString() : null;
    }
}
