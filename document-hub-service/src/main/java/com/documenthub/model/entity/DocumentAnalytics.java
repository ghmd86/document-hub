package com.documenthub.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing the document_analytics table.
 * Stores pre-aggregated daily statistics for document interactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("document_analytics")
public class DocumentAnalytics {

    @Id
    @Column("analytics_id")
    private UUID analyticsId;

    @Column("storage_index_id")
    private UUID storageIndexId;

    @Column("template_id")
    private UUID templateId;

    @Column("doc_type")
    private String docType;

    @Column("template_name")
    private String templateName;

    @Column("category_code")
    private String categoryCode;

    @Column("account_id")
    private UUID accountId;

    @Column("customer_id")
    private UUID customerId;

    @Column("event_date")
    private LocalDate eventDate;

    // Aggregated Counts
    @Column("view_count")
    private Integer viewCount;

    @Column("download_count")
    private Integer downloadCount;

    @Column("print_count")
    private Integer printCount;

    @Column("reprint_count")
    private Integer reprintCount;

    @Column("share_count")
    private Integer shareCount;

    @Column("export_count")
    private Integer exportCount;

    @Column("failed_access_count")
    private Integer failedAccessCount;

    // Unique User Counts
    @Column("unique_viewers")
    private Integer uniqueViewers;

    @Column("unique_downloaders")
    private Integer uniqueDownloaders;

    // Additional Metrics
    @Column("total_print_copies")
    private Integer totalPrintCopies;

    @Column("total_share_recipients")
    private Integer totalShareRecipients;

    @Column("avg_view_duration_seconds")
    private Integer avgViewDurationSeconds;

    // Last Activity Tracking
    @Column("last_viewed_at")
    private java.time.OffsetDateTime lastViewedAt;

    @Column("last_downloaded_at")
    private java.time.OffsetDateTime lastDownloadedAt;

    @Column("last_printed_at")
    private java.time.OffsetDateTime lastPrintedAt;

    @Column("last_shared_at")
    private java.time.OffsetDateTime lastSharedAt;

    // Audit
    @Column("created_at")
    private java.time.OffsetDateTime createdAt;

    @Column("updated_at")
    private java.time.OffsetDateTime updatedAt;
}
