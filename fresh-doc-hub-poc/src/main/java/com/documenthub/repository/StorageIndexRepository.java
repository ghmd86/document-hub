package com.documenthub.repository;

import com.documenthub.entity.StorageIndexEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Repository for Storage Index (Documents)
 */
@Repository
public interface StorageIndexRepository extends R2dbcRepository<StorageIndexEntity, UUID> {

    /**
     * Find account-specific documents (non-shared)
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE account_key = :accountKey " +
           "AND shared_flag = false " +
           "AND template_type = :templateType " +
           "AND \"template_version \" = :templateVersion")
    Flux<StorageIndexEntity> findAccountSpecificDocuments(
        @Param("accountKey") UUID accountKey,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion
    );

    /**
     * Find shared documents by template
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE shared_flag = true " +
           "AND template_type = :templateType " +
           "AND \"template_version \" = :templateVersion")
    Flux<StorageIndexEntity> findSharedDocuments(
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion
    );

    /**
     * Find documents by customer key
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE customer_key = :customerKey " +
           "AND template_type = :templateType " +
           "AND \"template_version \" = :templateVersion")
    Flux<StorageIndexEntity> findByCustomerKey(
        @Param("customerKey") UUID customerKey,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion
    );

    /**
     * Find documents by reference key
     */
    @Query("SELECT * FROM storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType")
    Flux<StorageIndexEntity> findByReferenceKey(
        @Param("referenceKey") String referenceKey,
        @Param("referenceKeyType") String referenceKeyType
    );
}
