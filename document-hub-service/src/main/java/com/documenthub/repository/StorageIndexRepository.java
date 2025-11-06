package com.documenthub.repository;

import com.documenthub.model.entity.StorageIndex;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * Reactive repository for StorageIndex entity.
 * Provides methods to query document storage index.
 */
@Repository
public interface StorageIndexRepository extends R2dbcRepository<StorageIndex, UUID> {

    /**
     * Find all documents by account ID.
     */
    Flux<StorageIndex> findByAccountKey(UUID accountKey);

    /**
     * Find all documents by customer ID.
     */
    Flux<StorageIndex> findByCustomerKey(UUID customerKey);

    /**
     * Find documents by account ID and accessible status.
     */
    Flux<StorageIndex> findByAccountKeyAndIsAccessible(UUID accountKey, Boolean isAccessible);

    /**
     * Find documents by customer ID and accessible status.
     */
    Flux<StorageIndex> findByCustomerKeyAndIsAccessible(UUID customerKey, Boolean isAccessible);

    /**
     * Find documents by multiple account IDs.
     */
    @Query("SELECT * FROM storage_index WHERE account_key = ANY(:accountKeys) AND is_accessible = :isAccessible")
    Flux<StorageIndex> findByAccountKeysAndIsAccessible(
            @Param("accountKeys") UUID[] accountKeys,
            @Param("isAccessible") Boolean isAccessible
    );

    /**
     * Find documents by customer ID and account IDs with date range.
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE customer_key = :customerId " +
           "AND account_key = ANY(:accountIds) " +
           "AND is_accessible = true " +
           "AND doc_creation_date >= :fromDate " +
           "AND doc_creation_date <= :toDate")
    Flux<StorageIndex> findByCustomerAndAccountsWithDateRange(
            @Param("customerId") UUID customerId,
            @Param("accountIds") UUID[] accountIds,
            @Param("fromDate") Long fromDate,
            @Param("toDate") Long toDate
    );

    /**
     * Find documents by customer ID with date range.
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE customer_key = :customerId " +
           "AND is_accessible = true " +
           "AND doc_creation_date >= :fromDate " +
           "AND doc_creation_date <= :toDate")
    Flux<StorageIndex> findByCustomerWithDateRange(
            @Param("customerId") UUID customerId,
            @Param("fromDate") Long fromDate,
            @Param("toDate") Long toDate
    );

    /**
     * Find documents by template ID and account ID.
     */
    Flux<StorageIndex> findByTemplateIdAndAccountKey(UUID templateId, UUID accountKey);

    /**
     * Find documents with category and doc type filtering.
     */
    @Query("SELECT si.* FROM storage_index si " +
           "JOIN master_template_definition mtd ON si.template_id = mtd.template_id " +
           "WHERE si.account_key = :accountKey " +
           "AND si.is_accessible = true " +
           "AND mtd.category = :category " +
           "AND mtd.doc_type = :docType")
    Flux<StorageIndex> findByAccountAndCategoryAndDocType(
            @Param("accountKey") UUID accountKey,
            @Param("category") String category,
            @Param("docType") String docType
    );

    /**
     * Count documents by account ID.
     */
    @Query("SELECT COUNT(*) FROM storage_index WHERE account_key = :accountKey AND is_accessible = true")
    reactor.core.publisher.Mono<Long> countByAccountKey(@Param("accountKey") UUID accountKey);
}
