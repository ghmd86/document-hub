package com.documenthub.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing the document_events table.
 * Stores all document interaction events (views, downloads, prints, shares, etc.).
 * This table is partitioned by event_date (monthly).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("document_events")
public class DocumentEvent {

    @Id
    @Column("event_id")
    private UUID eventId;

    @Column("event_type")
    private String eventType; // VIEW, DOWNLOAD, PRINT, SHARE, EXPORT, DELETE, FAILED_ACCESS

    @Column("event_timestamp")
    private OffsetDateTime eventTimestamp;

    // Document Reference
    @Column("storage_index_id")
    private UUID storageIndexId;

    @Column("template_id")
    private UUID templateId;

    // Denormalized for zero-join
    @Column("doc_type")
    private String docType;

    @Column("template_name")
    private String templateName;

    @Column("is_regulatory")
    private Boolean isRegulatory;

    // User Information
    @Column("actor_id")
    private UUID actorId;

    @Column("actor_type")
    private String actorType; // CUSTOMER, AGENT, SYSTEM

    @Column("customer_id")
    private UUID customerId;

    @Column("account_id")
    private UUID accountId;

    // Session and Request Context
    @Column("session_id")
    private UUID sessionId;

    @Column("correlation_id")
    private UUID correlationId;

    @Column("access_channel")
    private String accessChannel; // WEB, MOBILE, API, EMAIL

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    // Flexible Event Data (JSONB)
    @Column("event_data")
    private JsonNode eventData;
    /*
     * For PRINT: {"print_type": "INITIAL|REPRINT", "copies": 2, "print_cost_cents": 50}
     * For VIEW: {"duration_seconds": 45, "pages_viewed": [1,2,3]}
     * For SHARE: {"share_method": "email", "recipients": ["user@example.com"], "recipient_count": 1}
     */

    // Event Date (for partitioning)
    @Column("event_date")
    private LocalDate eventDate;
}
