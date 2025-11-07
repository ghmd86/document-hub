package com.documenthub.repository;

import com.documenthub.model.entity.DocumentEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reactive repository for DocumentEvent entity.
 * Provides methods to query document interaction events.
 */
@Repository
public interface DocumentEventRepository extends R2dbcRepository<DocumentEvent, UUID> {

    /**
     * Find events by storage index ID.
     */
    Flux<DocumentEvent> findByStorageIndexId(UUID storageIndexId);

    /**
     * Find events by storage index ID and event type.
     */
    @Query("SELECT * FROM document_events " +
           "WHERE storage_index_id = :storageIndexId " +
           "AND event_type = :eventType " +
           "ORDER BY event_timestamp DESC")
    Flux<DocumentEvent> findByStorageIndexIdAndEventType(
            @Param("storageIndexId") UUID storageIndexId,
            @Param("eventType") String eventType
    );

    /**
     * Find events by customer ID within date range.
     */
    @Query("SELECT * FROM document_events " +
           "WHERE customer_id = :customerId " +
           "AND event_timestamp >= :fromDate " +
           "AND event_timestamp <= :toDate " +
           "ORDER BY event_timestamp DESC")
    Flux<DocumentEvent> findByCustomerIdAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate
    );

    /**
     * Find events by account ID.
     */
    @Query("SELECT * FROM document_events " +
           "WHERE account_id = :accountId " +
           "ORDER BY event_timestamp DESC " +
           "LIMIT :limit")
    Flux<DocumentEvent> findByAccountId(
            @Param("accountId") UUID accountId,
            @Param("limit") Integer limit
    );

    /**
     * Find events by actor ID (who performed the action).
     */
    @Query("SELECT * FROM document_events " +
           "WHERE actor_id = :actorId " +
           "ORDER BY event_timestamp DESC " +
           "LIMIT :limit")
    Flux<DocumentEvent> findByActorId(
            @Param("actorId") UUID actorId,
            @Param("limit") Integer limit
    );

    /**
     * Count events by storage index ID and event type.
     */
    @Query("SELECT COUNT(*) FROM document_events " +
           "WHERE storage_index_id = :storageIndexId " +
           "AND event_type = :eventType")
    Mono<Long> countByStorageIndexIdAndEventType(
            @Param("storageIndexId") UUID storageIndexId,
            @Param("eventType") String eventType
    );

    /**
     * Find events by event date for aggregation processing.
     */
    @Query("SELECT * FROM document_events " +
           "WHERE event_date = :eventDate " +
           "ORDER BY event_timestamp ASC")
    Flux<DocumentEvent> findByEventDate(@Param("eventDate") LocalDate eventDate);
}
