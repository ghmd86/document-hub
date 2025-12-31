package com.documenthub.dao;

import com.documenthub.dto.StorageIndexDto;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.repository.StorageIndexRepository;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Data Access Object for StorageIndex operations.
 * Provides a layer of abstraction over the repository for data access.
 * Returns DTOs instead of entities to maintain layer separation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageIndexDao {

    private final StorageIndexRepository repository;

    /**
     * Save a storage index entry (accepts DTO, returns DTO)
     */
    public Mono<StorageIndexDto> save(StorageIndexDto dto) {
        log.debug("Saving storage index: id={}, templateType={}",
            dto.getStorageIndexId(), dto.getTemplateType());
        return repository.save(toEntity(dto)).map(this::toDto);
    }

    /**
     * Find storage index by ID
     */
    public Mono<StorageIndexDto> findById(UUID storageIndexId) {
        log.debug("Finding storage index by id: {}", storageIndexId);
        return repository.findById(storageIndexId).map(this::toDto);
    }

    /**
     * Find documents by account and template with date range
     */
    public Flux<StorageIndexDto> findAccountDocuments(
            UUID accountId,
            String templateType,
            Integer templateVersion,
            Long fromDate,
            Long toDate,
            Long currentDate) {
        log.debug("Finding account documents: accountId={}, templateType={}, version={}",
            accountId, templateType, templateVersion);
        return repository.findAccountSpecificDocumentsWithDateRange(
            accountId, templateType, templateVersion, fromDate, toDate, currentDate)
            .map(this::toDto);
    }

    /**
     * Find shared documents by template with date range
     */
    public Flux<StorageIndexDto> findSharedDocuments(
            String templateType,
            Integer templateVersion,
            Long fromDate,
            Long toDate,
            Long currentDate) {
        log.debug("Finding shared documents: templateType={}, version={}", templateType, templateVersion);
        return repository.findSharedDocumentsWithDateRange(
            templateType, templateVersion, fromDate, toDate, currentDate)
            .map(this::toDto);
    }

    /**
     * Find documents by customer key
     */
    public Flux<StorageIndexDto> findByCustomerKey(
            UUID customerKey,
            String templateType,
            Integer templateVersion,
            Long currentDate) {
        log.debug("Finding documents by customer: customerId={}, templateType={}",
            customerKey, templateType);
        return repository.findByCustomerKey(customerKey, templateType, templateVersion, currentDate)
            .map(this::toDto);
    }

    /**
     * Find documents by reference key and template
     */
    public Flux<StorageIndexDto> findByReferenceKeyAndTemplate(
            String referenceKey,
            String referenceKeyType,
            String templateType,
            Integer templateVersion,
            Long currentDate) {
        log.debug("Finding documents by reference key: templateType={}, referenceKey={}",
            templateType, referenceKey);
        return repository.findByReferenceKeyAndTemplate(
            referenceKey, referenceKeyType, templateType, templateVersion, currentDate)
            .map(this::toDto);
    }

    /**
     * Find documents by reference key and template with date range
     */
    public Flux<StorageIndexDto> findByReferenceKeyAndTemplateWithDateRange(
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
            postedFromDate, postedToDate, currentDate)
            .map(this::toDto);
    }

    /**
     * Update storage index (accepts DTO, returns DTO)
     */
    public Mono<StorageIndexDto> update(StorageIndexDto dto) {
        log.debug("Updating storage index: id={}", dto.getStorageIndexId());
        return repository.save(toEntity(dto)).map(this::toDto);
    }

    /**
     * Delete storage index (soft delete by setting archive_indicator)
     */
    public Mono<StorageIndexDto> softDelete(UUID storageIndexId) {
        log.debug("Soft deleting storage index: id={}", storageIndexId);
        return repository.findById(storageIndexId)
            .flatMap(entity -> {
                entity.setArchiveIndicator(true);
                entity.setRecordStatus("ARCHIVED");
                entity.setArchiveTimestamp(java.time.LocalDateTime.now());
                entity.setUpdatedTimestamp(java.time.LocalDateTime.now());
                entity.setUpdatedBy("SYSTEM");
                return repository.save(entity);
            })
            .map(this::toDto);
    }

    /**
     * Update end_date for existing document by ID.
     * Used when single_document_flag is true to close the active period of old documents.
     */
    public Mono<StorageIndexDto> updateEndDate(UUID storageIndexId, Long newEndDate) {
        log.debug("Updating end_date for storage index: id={}, newEndDate={}",
            storageIndexId, newEndDate);
        return repository.findById(storageIndexId)
            .flatMap(entity -> {
                entity.setEndDate(newEndDate);
                entity.setUpdatedTimestamp(java.time.LocalDateTime.now());
                entity.setUpdatedBy("SYSTEM");
                return repository.save(entity);
            })
            .map(this::toDto);
    }

    /**
     * Find active documents by reference key and template type.
     * Used for single_document_flag enforcement during upload.
     */
    public Flux<StorageIndexDto> findActiveByReferenceKey(
            String referenceKey,
            String referenceKeyType,
            String templateType) {
        log.debug("Finding active documents by reference key: refKey={}, refKeyType={}, templateType={}",
            referenceKey, referenceKeyType, templateType);
        return repository.findByReferenceKeyAndTemplate(
            referenceKey, referenceKeyType, templateType, null, System.currentTimeMillis())
            .filter(doc -> Boolean.TRUE.equals(doc.getAccessibleFlag()))
            .map(this::toDto);
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
        return repository.findByReferenceKeyAndTemplate(
                referenceKey, referenceKeyType, templateType, null, System.currentTimeMillis())
            .filter(doc -> Boolean.TRUE.equals(doc.getAccessibleFlag()))
            .filter(doc -> isOverlapping(doc, newDocStartDate))
            .flatMap(entity -> {
                entity.setEndDate(newDocStartDate);
                entity.setUpdatedTimestamp(java.time.LocalDateTime.now());
                entity.setUpdatedBy("SYSTEM");
                return repository.save(entity);
            })
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

    /**
     * Find documents by reference key type and template (for auto-discover mode).
     * Does not require specific reference key - finds all docs matching type.
     */
    public Flux<StorageIndexDto> findByReferenceKeyTypeAndTemplateWithDateRange(
            String referenceKeyType,
            String templateType,
            Integer templateVersion,
            Long postedFromDate,
            Long postedToDate,
            Long currentDate) {
        log.debug("Finding documents by reference key type: templateType={}, refKeyType={}",
            templateType, referenceKeyType);
        return repository.findByReferenceKeyTypeAndTemplateWithDateRange(
            referenceKeyType, templateType, templateVersion,
            postedFromDate, postedToDate, currentDate)
            .map(this::toDto);
    }

    /**
     * Find account-specific documents with date range
     */
    public Flux<StorageIndexDto> findAccountSpecificDocumentsWithDateRange(
            UUID accountId,
            String templateType,
            Integer templateVersion,
            Long postedFromDate,
            Long postedToDate,
            Long currentDate) {
        log.debug("Finding account-specific documents with date range: accountId={}, templateType={}",
            accountId, templateType);
        return repository.findAccountSpecificDocumentsWithDateRange(
            accountId, templateType, templateVersion, postedFromDate, postedToDate, currentDate)
            .map(this::toDto);
    }

    /**
     * Find shared documents with date range
     */
    public Flux<StorageIndexDto> findSharedDocumentsWithDateRange(
            String templateType,
            Integer templateVersion,
            Long postedFromDate,
            Long postedToDate,
            Long currentDate) {
        log.debug("Finding shared documents with date range: templateType={}", templateType);
        return repository.findSharedDocumentsWithDateRange(
            templateType, templateVersion, postedFromDate, postedToDate, currentDate)
            .map(this::toDto);
    }

    // ========================================================================
    // Entity <-> DTO Converters
    // ========================================================================

    /**
     * Convert entity to DTO
     */
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

    /**
     * Convert DTO to entity
     */
    private StorageIndexEntity toEntity(StorageIndexDto dto) {
        StorageIndexEntity entity = new StorageIndexEntity();
        entity.setStorageIndexId(dto.getStorageIndexId());
        entity.setMasterTemplateId(dto.getMasterTemplateId());
        entity.setTemplateVersion(dto.getTemplateVersion());
        entity.setTemplateType(dto.getTemplateType());
        entity.setStorageVendor(dto.getStorageVendor());
        entity.setReferenceKey(dto.getReferenceKey());
        entity.setReferenceKeyType(dto.getReferenceKeyType());
        entity.setAccountKey(dto.getAccountKey());
        entity.setCustomerKey(dto.getCustomerKey());
        entity.setStorageDocumentKey(dto.getStorageDocumentKey());
        entity.setFileName(dto.getFileName());
        entity.setDocCreationDate(dto.getDocCreationDate());
        entity.setAccessibleFlag(dto.getAccessibleFlag());
        entity.setDocMetadata(stringToJson(dto.getDocMetadata()));
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setSharedFlag(dto.getSharedFlag());
        entity.setGenerationVendorId(dto.getGenerationVendorId());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedTimestamp(dto.getCreatedTimestamp());
        entity.setUpdatedBy(dto.getUpdatedBy());
        entity.setUpdatedTimestamp(dto.getUpdatedTimestamp());
        entity.setArchiveIndicator(dto.getArchiveIndicator());
        entity.setArchiveTimestamp(dto.getArchiveTimestamp());
        entity.setVersionNumber(dto.getVersionNumber());
        entity.setRecordStatus(dto.getRecordStatus());
        return entity;
    }

    /**
     * Convert Json to String, handling null
     */
    private String jsonToString(Json json) {
        return json != null ? json.asString() : null;
    }

    /**
     * Convert String to Json, handling null
     */
    private Json stringToJson(String str) {
        return str != null ? Json.of(str) : null;
    }
}
