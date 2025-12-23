package com.documenthub.dao;

import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.repository.StorageIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Data Access Object for StorageIndex operations.
 * Provides a layer of abstraction over the repository for data access.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageIndexDao {

    private final StorageIndexRepository repository;

    /**
     * Save a storage index entry
     */
    public Mono<StorageIndexEntity> save(StorageIndexEntity entity) {
        log.debug("Saving storage index: id={}, templateType={}",
            entity.getStorageIndexId(), entity.getTemplateType());
        return repository.save(entity);
    }

    /**
     * Find storage index by ID
     */
    public Mono<StorageIndexEntity> findById(UUID storageIndexId) {
        log.debug("Finding storage index by id: {}", storageIndexId);
        return repository.findById(storageIndexId);
    }

    /**
     * Find documents by account and template with date range
     */
    public Flux<StorageIndexEntity> findAccountDocuments(
            UUID accountId,
            String templateType,
            Integer templateVersion,
            Long fromDate,
            Long toDate,
            Long currentDate) {
        log.debug("Finding account documents: accountId={}, templateType={}, version={}",
            accountId, templateType, templateVersion);
        return repository.findAccountSpecificDocumentsWithDateRange(
            accountId, templateType, templateVersion, fromDate, toDate, currentDate);
    }

    /**
     * Find shared documents by template with date range
     */
    public Flux<StorageIndexEntity> findSharedDocuments(
            String templateType,
            Integer templateVersion,
            Long fromDate,
            Long toDate,
            Long currentDate) {
        log.debug("Finding shared documents: templateType={}, version={}", templateType, templateVersion);
        return repository.findSharedDocumentsWithDateRange(
            templateType, templateVersion, fromDate, toDate, currentDate);
    }

    /**
     * Find documents by customer key
     */
    public Flux<StorageIndexEntity> findByCustomerKey(
            UUID customerKey,
            String templateType,
            Integer templateVersion,
            Long currentDate) {
        log.debug("Finding documents by customer: customerId={}, templateType={}",
            customerKey, templateType);
        return repository.findByCustomerKey(customerKey, templateType, templateVersion, currentDate);
    }

    /**
     * Find documents by reference key and template
     */
    public Flux<StorageIndexEntity> findByReferenceKeyAndTemplate(
            String referenceKey,
            String referenceKeyType,
            String templateType,
            Integer templateVersion,
            Long currentDate) {
        log.debug("Finding documents by reference key: templateType={}, referenceKey={}",
            templateType, referenceKey);
        return repository.findByReferenceKeyAndTemplate(
            referenceKey, referenceKeyType, templateType, templateVersion, currentDate);
    }

    /**
     * Find documents by reference key and template with date range
     */
    public Flux<StorageIndexEntity> findByReferenceKeyAndTemplateWithDateRange(
            String referenceKey,
            String referenceKeyType,
            String templateType,
            Integer templateVersion,
            Long postedFromDate,
            Long postedToDate,
            Long currentDate) {
        log.debug("Finding documents by reference key with date range: templateType={}, referenceKey={}",
            templateType, referenceKey);
        return repository.findByReferenceKeyAndTemplateWithDateRange(
            referenceKey, referenceKeyType, templateType, templateVersion,
            postedFromDate, postedToDate, currentDate);
    }

    /**
     * Update storage index
     */
    public Mono<StorageIndexEntity> update(StorageIndexEntity entity) {
        log.debug("Updating storage index: id={}", entity.getStorageIndexId());
        return repository.save(entity);
    }

    /**
     * Delete storage index (soft delete by setting archive_indicator)
     */
    public Mono<StorageIndexEntity> softDelete(StorageIndexEntity entity) {
        log.debug("Soft deleting storage index: id={}", entity.getStorageIndexId());
        entity.setArchiveIndicator(true);
        entity.setRecordStatus("ARCHIVED");
        entity.setArchiveTimestamp(java.time.LocalDateTime.now());
        entity.setUpdatedTimestamp(java.time.LocalDateTime.now());
        entity.setUpdatedBy("SYSTEM");
        return repository.save(entity);
    }

    /**
     * Update end_date for existing document.
     * Used when single_document_flag is true to close the active period of old documents.
     */
    public Mono<StorageIndexEntity> updateEndDate(StorageIndexEntity entity, Long newEndDate) {
        log.debug("Updating end_date for storage index: id={}, newEndDate={}",
            entity.getStorageIndexId(), newEndDate);
        entity.setEndDate(newEndDate);
        entity.setUpdatedTimestamp(java.time.LocalDateTime.now());
        entity.setUpdatedBy("SYSTEM");
        return repository.save(entity);
    }

    /**
     * Find active documents by reference key and template type.
     * Used for single_document_flag enforcement during upload.
     */
    public Flux<StorageIndexEntity> findActiveByReferenceKey(
            String referenceKey,
            String referenceKeyType,
            String templateType) {
        log.debug("Finding active documents by reference key: refKey={}, refKeyType={}, templateType={}",
            referenceKey, referenceKeyType, templateType);
        return repository.findByReferenceKeyAndTemplate(
            referenceKey, referenceKeyType, templateType, null, System.currentTimeMillis())
            .filter(doc -> Boolean.TRUE.equals(doc.getAccessibleFlag()));
    }

    /**
     * Update end_date of documents that overlap with the new document's start_date.
     * Only documents where (end_date is null OR end_date > newDocStartDate) are updated.
     * Returns the count of updated documents.
     */
    public Mono<Long> updateEndDateByReferenceKey(
            String referenceKey,
            String referenceKeyType,
            String templateType,
            Long newDocStartDate) {
        log.info("Updating end_date for overlapping docs: refKey={}, templateType={}, newDocStartDate={}",
            referenceKey, templateType, newDocStartDate);
        return findActiveByReferenceKey(referenceKey, referenceKeyType, templateType)
            .filter(doc -> isOverlapping(doc, newDocStartDate))
            .flatMap(doc -> updateEndDate(doc, newDocStartDate))
            .count()
            .doOnSuccess(count -> {
                if (count > 0) {
                    log.info("Updated end_date for {} overlapping documents for refKey={}", count, referenceKey);
                }
            });
    }

    /**
     * Check if existing document overlaps with the new document's start date.
     * Overlapping means: doc has no end_date OR doc's end_date is after new start_date.
     */
    private boolean isOverlapping(StorageIndexEntity doc, Long newDocStartDate) {
        if (newDocStartDate == null) {
            return true; // If no start date specified, consider all as overlapping
        }
        return doc.getEndDate() == null || doc.getEndDate() > newDocStartDate;
    }
}
