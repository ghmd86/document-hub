package com.documenthub.repository;

import com.documenthub.model.entity.StorageIndex;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reactive repository for StorageIndex entity.
 * Provides methods to query document storage index with denormalized data (zero-join queries).
 */
@Repository
public interface StorageIndexRepository extends R2dbcRepository<StorageIndex, UUID> {

    /**
     * Find all documents by account ID.
     */
    Flux<StorageIndex> findByAccountId(UUID accountId);

    /**
     * Find all documents by customer ID.
     */
    Flux<StorageIndex> findByCustomerId(UUID customerId);

    /**
     * Find documents by account ID and accessible status.
     */
    Flux<StorageIndex> findByAccountIdAndIsAccessible(UUID accountId, Boolean isAccessible);

    /**
     * Find documents by customer ID and accessible status.
     */
    Flux<StorageIndex> findByCustomerIdAndIsAccessible(UUID customerId, Boolean isAccessible);

    /**
     * Find documents by multiple account IDs (zero-join).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE account_id = ANY(:accountIds) " +
           "AND is_accessible = :isAccessible " +
           "AND archive_indicator = FALSE " +
           "ORDER BY doc_creation_date DESC")
    Flux<StorageIndex> findByAccountIdsAndIsAccessible(
            @Param("accountIds") UUID[] accountIds,
            @Param("isAccessible") Boolean isAccessible
    );

    /**
     * Find documents by customer ID and account IDs with date range (zero-join).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE customer_id = :customerId " +
           "AND account_id = ANY(:accountIds) " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE " +
           "AND doc_creation_date >= :fromDate " +
           "AND doc_creation_date <= :toDate " +
           "ORDER BY doc_creation_date DESC")
    Flux<StorageIndex> findByCustomerAndAccountsWithDateRange(
            @Param("customerId") UUID customerId,
            @Param("accountIds") UUID[] accountIds,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate
    );

    /**
     * Find documents by customer ID with date range (zero-join).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE customer_id = :customerId " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE " +
           "AND doc_creation_date >= :fromDate " +
           "AND doc_creation_date <= :toDate " +
           "ORDER BY doc_creation_date DESC")
    Flux<StorageIndex> findByCustomerWithDateRange(
            @Param("customerId") UUID customerId,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate
    );

    /**
     * Find documents by template ID and version and account ID.
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE template_id = :templateId " +
           "AND template_version = :templateVersion " +
           "AND account_id = :accountId " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE")
    Flux<StorageIndex> findByTemplateAndAccountId(
            @Param("templateId") UUID templateId,
            @Param("templateVersion") Integer templateVersion,
            @Param("accountId") UUID accountId
    );

    /**
     * Find documents with category and doc type filtering (zero-join, uses denormalized data).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE account_id = :accountId " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE " +
           "AND category_code = :categoryCode " +
           "AND doc_type = :docType " +
           "ORDER BY doc_creation_date DESC")
    Flux<StorageIndex> findByAccountAndCategoryAndDocType(
            @Param("accountId") UUID accountId,
            @Param("categoryCode") String categoryCode,
            @Param("docType") String docType
    );

    /**
     * Find documents by doc type (zero-join).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE doc_type = :docType " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE " +
           "ORDER BY doc_creation_date DESC")
    Flux<StorageIndex> findByDocType(@Param("docType") String docType);

    /**
     * Find regulatory documents for customer (zero-join).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE customer_id = :customerId " +
           "AND is_regulatory = TRUE " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE " +
           "ORDER BY doc_creation_date DESC")
    Flux<StorageIndex> findRegulatoryDocumentsByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find documents by storage document key.
     */
    Mono<StorageIndex> findByStorageDocumentKey(UUID storageDocumentKey);

    /**
     * Count documents by account ID.
     */
    @Query("SELECT COUNT(*) FROM storage_index " +
           "WHERE account_id = :accountId " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE")
    Mono<Long> countByAccountId(@Param("accountId") UUID accountId);

    /**
     * Count documents by customer ID.
     */
    @Query("SELECT COUNT(*) FROM storage_index " +
           "WHERE customer_id = :customerId " +
           "AND is_accessible = TRUE " +
           "AND archive_indicator = FALSE")
    Mono<Long> countByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find documents with PII for compliance (JSONB query).
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE compliance_tags->>'contains_pii' = 'true' " +
           "AND compliance_tags->>'gdpr_applicable' = 'true' " +
           "AND archive_indicator = FALSE " +
           "ORDER BY retention_until ASC")
    Flux<StorageIndex> findDocumentsWithPII();
}
