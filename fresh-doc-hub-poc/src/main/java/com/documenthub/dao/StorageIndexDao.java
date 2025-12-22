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
        return repository.save(entity);
    }
}
