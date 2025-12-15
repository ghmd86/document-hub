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
 *
 * All queries filter by document validity period (start_date/end_date) to ensure
 * only currently valid documents are returned.
 */
@Repository
public interface StorageIndexRepository extends R2dbcRepository<StorageIndexEntity, UUID> {

    /**
     * Find account-specific documents (non-shared)
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE account_key = :accountKey " +
           "AND shared_flag = false " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findAccountSpecificDocuments(
        @Param("accountKey") UUID accountKey,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find shared documents by template
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE shared_flag = true " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findSharedDocuments(
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find documents by customer key
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE customer_key = :customerKey " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findByCustomerKey(
        @Param("customerKey") UUID customerKey,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find documents by reference key
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType " +
           "AND accessible_flag = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findByReferenceKey(
        @Param("referenceKey") String referenceKey,
        @Param("referenceKeyType") String referenceKeyType,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find documents by reference key with template filtering
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findByReferenceKeyAndTemplate(
        @Param("referenceKey") String referenceKey,
        @Param("referenceKeyType") String referenceKeyType,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find account-specific documents with date range filtering
     * Uses doc_creation_date for posted date filtering
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE account_key = :accountKey " +
           "AND shared_flag = false " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (:postedFromDate IS NULL OR doc_creation_date >= :postedFromDate) " +
           "AND (:postedToDate IS NULL OR doc_creation_date <= :postedToDate) " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findAccountSpecificDocumentsWithDateRange(
        @Param("accountKey") UUID accountKey,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("postedFromDate") Long postedFromDate,
        @Param("postedToDate") Long postedToDate,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find shared documents with date range filtering
     * Uses doc_creation_date for posted date filtering
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE shared_flag = true " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (:postedFromDate IS NULL OR doc_creation_date >= :postedFromDate) " +
           "AND (:postedToDate IS NULL OR doc_creation_date <= :postedToDate) " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findSharedDocumentsWithDateRange(
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("postedFromDate") Long postedFromDate,
        @Param("postedToDate") Long postedToDate,
        @Param("currentDate") Long currentDate
    );

    /**
     * Find documents by reference key with template filtering and date range
     */
    @Query("SELECT * FROM document_hub.storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType " +
           "AND accessible_flag = true " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND (:postedFromDate IS NULL OR doc_creation_date >= :postedFromDate) " +
           "AND (:postedToDate IS NULL OR doc_creation_date <= :postedToDate) " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<StorageIndexEntity> findByReferenceKeyAndTemplateWithDateRange(
        @Param("referenceKey") String referenceKey,
        @Param("referenceKeyType") String referenceKeyType,
        @Param("templateType") String templateType,
        @Param("templateVersion") Integer templateVersion,
        @Param("postedFromDate") Long postedFromDate,
        @Param("postedToDate") Long postedToDate,
        @Param("currentDate") Long currentDate
    );
}
