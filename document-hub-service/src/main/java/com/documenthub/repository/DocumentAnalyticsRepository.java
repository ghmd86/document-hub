package com.documenthub.repository;

import com.documenthub.model.entity.DocumentAnalytics;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive repository for DocumentAnalytics entity.
 * Provides methods to query pre-aggregated document analytics.
 */
@Repository
public interface DocumentAnalyticsRepository extends R2dbcRepository<DocumentAnalytics, UUID> {

    /**
     * Find analytics by storage index ID.
     */
    Flux<DocumentAnalytics> findByStorageIndexId(UUID storageIndexId);

    /**
     * Find analytics by storage index ID and date.
     */
    @Query("SELECT * FROM document_analytics " +
           "WHERE storage_index_id = :storageIndexId " +
           "AND event_date = :eventDate " +
           "LIMIT 1")
    Mono<DocumentAnalytics> findByStorageIndexIdAndEventDate(
            @Param("storageIndexId") UUID storageIndexId,
            @Param("eventDate") LocalDate eventDate
    );

    /**
     * Find analytics by customer ID within date range.
     */
    @Query("SELECT * FROM document_analytics " +
           "WHERE customer_id = :customerId " +
           "AND event_date >= :fromDate " +
           "AND event_date <= :toDate " +
           "ORDER BY event_date DESC")
    Flux<DocumentAnalytics> findByCustomerIdAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Find analytics by account ID within date range.
     */
    @Query("SELECT * FROM document_analytics " +
           "WHERE account_id = :accountId " +
           "AND event_date >= :fromDate " +
           "AND event_date <= :toDate " +
           "ORDER BY event_date DESC")
    Flux<DocumentAnalytics> findByAccountIdAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Find analytics by doc type within date range.
     */
    @Query("SELECT * FROM document_analytics " +
           "WHERE doc_type = :docType " +
           "AND event_date >= :fromDate " +
           "AND event_date <= :toDate " +
           "ORDER BY event_date DESC")
    Flux<DocumentAnalytics> findByDocTypeAndDateRange(
            @Param("docType") String docType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Get total views for a document.
     */
    @Query("SELECT COALESCE(SUM(view_count), 0) " +
           "FROM document_analytics " +
           "WHERE storage_index_id = :storageIndexId")
    Mono<Long> getTotalViewsByStorageIndexId(@Param("storageIndexId") UUID storageIndexId);

    /**
     * Get total downloads for a document.
     */
    @Query("SELECT COALESCE(SUM(download_count), 0) " +
           "FROM document_analytics " +
           "WHERE storage_index_id = :storageIndexId")
    Mono<Long> getTotalDownloadsByStorageIndexId(@Param("storageIndexId") UUID storageIndexId);

    /**
     * Get total prints for a document.
     */
    @Query("SELECT COALESCE(SUM(print_count), 0) " +
           "FROM document_analytics " +
           "WHERE storage_index_id = :storageIndexId")
    Mono<Long> getTotalPrintsByStorageIndexId(@Param("storageIndexId") UUID storageIndexId);
}
