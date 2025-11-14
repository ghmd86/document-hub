# Data Dictionary - Document Events Ledger System

**Version:** 1.0
**Last Updated:** 2025-11-13
**Owner:** Document Hub Platform Team
**Status:** Production Ready

---

## Table of Contents
- [Overview](#overview)
- [Table: document_events_ledger](#table-document_events_ledger)
- [Table: document_view_events](#table-document_view_events)
- [Materialized Views](#materialized-views)
- [Indexing Strategy](#indexing-strategy)
- [Partitioning Strategy](#partitioning-strategy)
- [Common Query Patterns](#common-query-patterns)
- [Data Retention Policy](#data-retention-policy)

---

## Overview

This data dictionary documents the **Document Events Ledger System**, an analytics-optimized database schema for tracking document lifecycle events across the Document Hub Platform.

### Architecture Decisions

Based on review feedback from John Drum, the following key architectural decisions were made:

1. **Denormalization for Performance**: Template type and document type are denormalized into the analytics tables to avoid expensive joins across multiple databases.

2. **Separate View Tracking**: Document view events are tracked in a separate table (`document_view_events`) to prevent millions of entries in the main ledger for shared documents.

3. **Partitioning**: Both tables use monthly partitioning on date fields for efficient data retention management (24-month retention).

4. **Entry Type Classification**: The ledger tracks entry type (INITIAL, STATUS_CHANGE, FINAL, ERROR) to distinguish between initial requests and subsequent lifecycle updates.

5. **Correlation Tracking**: Correlation IDs link related entries across different systems for complete request tracing.

### Table Relationships

```
document_events_ledger
    ├─ Links to storage_index (via storage_index_id)
    ├─ Links to master_template_definition (via master_template_id)
    └─ Related entries linked via correlation_id

document_view_events
    ├─ Links to storage_index (via storage_index_id)
    ├─ Links to master_template_definition (via master_template_id)
    └─ Links to document_events_ledger (via correlation_id)
```

---

## Table: document_events_ledger

**Purpose**: Analytics-optimized ledger for tracking document lifecycle events (creation, deletion, printing, etc.). This is the primary audit trail for document operations.

**Type**: Partitioned table (append-only ledger pattern)
**Retention**: 24 months
**Partitioning**: Monthly by `event_date`
**Estimated Volume**: 10-50M rows/year (varies by usage)

### Columns

| Column Name | Data Type | Nullable | Default | Description | Populated By | When Populated |
|-------------|-----------|----------|---------|-------------|--------------|----------------|
| **doc_event_id** | UUID | NOT NULL | gen_random_uuid() | Unique identifier for this ledger entry | Database | On INSERT |
| **entry_type** | VARCHAR(50) | NOT NULL | - | Classification of entry type:<br>• **INITIAL**: First entry with full payload<br>• **STATUS_CHANGE**: Lifecycle update<br>• **FINAL**: Completion/terminal status<br>• **ERROR**: Exception occurred | Application | On INSERT |
| **correlation_id** | UUID | NULL | - | Links related entries across different tables and systems. All entries for a single document request should share the same correlation_id. | Application | On INSERT (when available) |
| **event_timestamp** | TIMESTAMPTZ | NOT NULL | CURRENT_TIMESTAMP | Precise timestamp when event occurred (with timezone) | Database | On INSERT |
| **event_date** | DATE | NOT NULL | CURRENT_DATE | Date extracted from event_timestamp, used as partition key | Database | On INSERT |
| **event_type** | VARCHAR(50) | NOT NULL | - | High-level event category:<br>• **CREATED**: Document created<br>• **DELETED**: Document deleted<br>• **SIGNED**: Document signed<br>• **RESTORED**: Deleted document restored<br>• **SHARED**: Document shared<br>• **PRINTED**: Document printed/rendered<br>• **REPRINTED**: Existing document resent<br><br>**Note**: VIEWED events tracked in separate table | Application | On INSERT |
| **event_code** | VARCHAR(100) | NULL | - | Granular event code for detailed classification. Examples:<br>• PRINTED_TO_HOV<br>• PRINTED_TO_BROADRIDGE<br>• SHARED_VIA_EMAIL<br>• CREATED_FROM_API<br><br>Values vary by event_type and source_app | Application | On INSERT (optional) |
| **event_data** | JSONB | NOT NULL | '{}'::jsonb | Flexible metadata storage. Structure varies by entry_type and source_app.<br><br>**Common fields**:<br>• request_payload (for INITIAL entries)<br>• status_from, status_to (for STATUS_CHANGE)<br>• error_message, error_code (for ERROR)<br>• vendor_reference (for print events)<br>• delivery_address (for reprint with alternate address) | Application | On INSERT |
| **actor_id** | UUID | NULL | - | Identifier of who/what performed the action:<br>• User ID (for user actions)<br>• System ID (for automated processes)<br>• Service ID (for inter-service calls) | Application | On INSERT (when available) |
| **actor_type** | VARCHAR(50) | NULL | - | Type of actor:<br>• **USER**: End user action<br>• **SYSTEM**: Automated system action<br>• **SERVICE**: Inter-service call<br>• **API**: External API call<br>• **SCHEDULER**: Scheduled job | Application | On INSERT (when available) |
| **storage_index_id** | UUID | NULL | - | Reference to the storage_index table (where document is stored) | Application | On INSERT (when document exists) |
| **master_template_id** | UUID | NULL | - | Reference to master_template_definition table | Application | On INSERT (when available) |
| **template_type** | VARCHAR(100) | NULL | - | **Denormalized** template type to avoid joins in analytics queries. Examples:<br>• STATEMENT<br>• DISCLOSURE<br>• NOTICE<br>• LETTER<br>• FORM | Application | On INSERT (denormalized from master_template) |
| **document_type** | VARCHAR(100) | NULL | - | **Denormalized** document type for analytics. Examples:<br>• MONTHLY_STATEMENT<br>• ANNUAL_DISCLOSURE<br>• LETTER_FF083<br>• PRIVACY_NOTICE<br><br>**IMPORTANT**: This should contain semantic document types, **NOT** "paperless". "Paperless" is an enrollment status, not a document type. | Application | On INSERT (denormalized from master_template) |
| **account_id** | UUID | NOT NULL | - | Account identifier (required for analytics). For shared documents, this enables account-level tracking and filtering. | Application | On INSERT |
| **source_app** | VARCHAR(100) | NOT NULL | - | Application/service that created this ledger entry. Examples:<br>• PRINT_SERVICE<br>• DOCUMENT_HUB<br>• DELIVERY_HUB<br>• READ_AND_SEND<br>• REPRINT_SERVICE | Application | On INSERT |
| **device_type** | VARCHAR(100) | NULL | - | Device type if applicable:<br>• MOBILE<br>• DESKTOP<br>• TABLET<br>• API (for programmatic access) | Application | On INSERT (for user-initiated events) |
| **location** | VARCHAR(255) | NULL | - | Geographic location, IP address, or region if applicable. Examples:<br>• IP: 192.168.1.1<br>• Region: US-EAST<br>• City: New York, NY | Application | On INSERT (for user-initiated events) |

### Constraints

- **Primary Key**: (doc_event_id, event_date) - Composite key required for partitioning
- **Check Constraints**:
  - `entry_type` IN ('INITIAL', 'STATUS_CHANGE', 'FINAL', 'ERROR')
  - `event_type` IN ('CREATED', 'DELETED', 'SIGNED', 'RESTORED', 'SHARED', 'PRINTED', 'REPRINTED')

### Indexes

See [Indexing Strategy](#indexing-strategy) section below.

---

## Table: document_view_events

**Purpose**: Separate table for tracking document view events. Isolated from main ledger to prevent volume issues with shared documents that may be viewed millions of times.

**Type**: Partitioned table (append-only)
**Retention**: 24 months
**Partitioning**: Monthly by `view_date`
**Estimated Volume**: 100M-1B rows/year (high volume for shared documents)

### Columns

| Column Name | Data Type | Nullable | Default | Description | Populated By | When Populated |
|-------------|-----------|----------|---------|-------------|--------------|----------------|
| **view_event_id** | UUID | NOT NULL | gen_random_uuid() | Unique identifier for this view event | Database | On INSERT |
| **view_timestamp** | TIMESTAMPTZ | NOT NULL | CURRENT_TIMESTAMP | Precise timestamp when view occurred | Database | On INSERT |
| **view_date** | DATE | NOT NULL | CURRENT_DATE | Date extracted from view_timestamp, used as partition key | Database | On INSERT |
| **correlation_id** | UUID | NULL | - | Links to original document creation/print event in document_events_ledger | Application | On INSERT (when available) |
| **storage_index_id** | UUID | NOT NULL | - | Document that was viewed (required) | Application | On INSERT |
| **master_template_id** | UUID | NULL | - | Template reference for the viewed document | Application | On INSERT (when available) |
| **template_type** | VARCHAR(100) | NULL | - | **Denormalized** template type for analytics (avoids joins). Same values as document_events_ledger.template_type | Application | On INSERT |
| **document_type** | VARCHAR(100) | NULL | - | **Denormalized** document type for analytics. Same values as document_events_ledger.document_type | Application | On INSERT |
| **account_id** | UUID | NOT NULL | - | Account context for the view. **Required** for meaningful analytics (as emphasized by John Drum: "what you really want to know is who viewed it") | Application | On INSERT |
| **viewer_id** | UUID | NOT NULL | - | Who actually viewed the document. **Required**. May differ from account holder for shared documents. | Application | On INSERT |
| **viewer_type** | VARCHAR(50) | NOT NULL | 'USER' | Type of viewer:<br>• **USER**: End user<br>• **SYSTEM**: Automated system check<br>• **SUPPORT**: Customer support representative<br>• **ADMIN**: Administrator | Application | On INSERT |
| **source_app** | VARCHAR(100) | NOT NULL | - | Application/channel where view occurred:<br>• DOCUMENT_HUB_WEB<br>• DOCUMENT_HUB_MOBILE<br>• EMAIL_LINK<br>• CUSTOMER_PORTAL | Application | On INSERT |
| **device_type** | VARCHAR(100) | NULL | - | Device used for viewing:<br>• MOBILE<br>• DESKTOP<br>• TABLET | Application | On INSERT |
| **location** | VARCHAR(255) | NULL | - | Geographic location or IP address | Application | On INSERT (optional) |
| **session_id** | UUID | NULL | - | Session identifier for grouping related views within a single user session | Application | On INSERT (when available) |
| **view_duration_seconds** | INTEGER | NULL | - | How long the document was viewed, in seconds (if tracked by application) | Application | On INSERT or UPDATE |
| **view_data** | JSONB | NOT NULL | '{}'::jsonb | Additional metadata in JSONB format. May include:<br>• scroll_depth<br>• pages_viewed<br>• download_occurred<br>• print_occurred | Application | On INSERT |

### Constraints

- **Primary Key**: (view_event_id, view_date) - Composite key required for partitioning

### Indexes

See [Indexing Strategy](#indexing-strategy) section below.

---

## Materialized Views

### document_last_viewed

**Purpose**: Provides quick access to the most recent view event for each document without scanning millions of rows.

**Refresh Strategy**: Hourly or daily depending on requirements. Use `REFRESH MATERIALIZED VIEW CONCURRENTLY document_last_viewed;`

**Columns**:
- `storage_index_id` - Document identifier (UNIQUE)
- `view_event_id` - Most recent view event
- `last_viewed_at` - Timestamp of last view
- `last_viewed_by_account` - Account that last viewed
- `last_viewed_by_viewer` - Viewer that last viewed
- `last_viewed_device` - Device type used
- `last_viewed_from` - Source application

**Usage**:
```sql
-- Get last viewed info for a document
SELECT * FROM document_last_viewed WHERE storage_index_id = 'xxx';
```

---

### document_view_counts

**Purpose**: Pre-aggregated view statistics for fast analytics queries and dashboards.

**Refresh Strategy**: Daily or as needed for reporting. Use `REFRESH MATERIALIZED VIEW CONCURRENTLY document_view_counts;`

**Columns**:
- `storage_index_id` - Document identifier (UNIQUE)
- `master_template_id` - Template reference
- `template_type` - Template type
- `document_type` - Document type
- `total_views` - Total number of views
- `unique_accounts` - Count of distinct accounts that viewed
- `unique_viewers` - Count of distinct viewers
- `first_viewed_at` - First view timestamp
- `last_viewed_at` - Last view timestamp

**Usage**:
```sql
-- Get view statistics for a document
SELECT * FROM document_view_counts WHERE storage_index_id = 'xxx';
```

---

## Indexing Strategy

### document_events_ledger Indexes

| Index Name | Columns | Type | Purpose | Estimated Usage |
|------------|---------|------|---------|-----------------|
| PRIMARY KEY | (doc_event_id, event_date) | B-tree | Uniqueness + partitioning | All queries |
| idx_document_events_ledger_timestamp | (event_timestamp) | B-tree | Time-range queries | Very High |
| idx_document_events_ledger_account_id | (account_id) | B-tree | Account-specific analytics | Very High |
| idx_document_events_ledger_template_id | (master_template_id) | B-tree | Template analytics | High |
| idx_document_events_ledger_event_type | (event_type) | B-tree | Filter by event type | High |
| idx_document_events_ledger_entry_type | (entry_type) | B-tree | Filter INITIAL vs STATUS_CHANGE | Medium |
| idx_document_events_ledger_source_app | (source_app) | B-tree | Source application tracking | Medium |
| idx_document_events_ledger_correlation_id | (correlation_id) WHERE correlation_id IS NOT NULL | B-tree (partial) | Trace related events | Medium |
| idx_document_events_ledger_account_time_type | (account_id, event_date, event_type) | B-tree | Common analytics pattern | Very High |
| idx_document_events_ledger_template_time | (master_template_id, event_date) WHERE master_template_id IS NOT NULL | B-tree (partial) | Template trends | High |

### document_view_events Indexes

| Index Name | Columns | Type | Purpose | Estimated Usage |
|------------|---------|------|---------|-----------------|
| PRIMARY KEY | (view_event_id, view_date) | B-tree | Uniqueness + partitioning | All queries |
| idx_document_view_events_timestamp | (view_timestamp) | B-tree | Time-range queries | Very High |
| idx_document_view_events_account_id | (account_id) | B-tree | Account view history | Very High |
| idx_document_view_events_viewer_id | (viewer_id) | B-tree | Individual viewer history | High |
| idx_document_view_events_storage_id | (storage_index_id) | B-tree | Document-specific views | Very High |
| idx_document_view_events_template_id | (master_template_id) WHERE master_template_id IS NOT NULL | B-tree (partial) | Template popularity | High |
| idx_document_view_events_correlation_id | (correlation_id) WHERE correlation_id IS NOT NULL | B-tree (partial) | Link to creation events | Medium |
| idx_document_view_events_doc_account | (storage_index_id, account_id, view_date) | B-tree | Which accounts viewed doc | Very High |
| idx_document_view_events_account_time | (account_id, view_date) | B-tree | Account activity over time | High |
| idx_document_view_events_template_time | (master_template_id, view_date) WHERE master_template_id IS NOT NULL | B-tree (partial) | Template trends | High |

---

## Partitioning Strategy

### Overview

Both `document_events_ledger` and `document_view_events` use **RANGE partitioning** on their respective date columns (`event_date` and `view_date`).

### Partition Scheme

- **Partition Interval**: Monthly
- **Retention Period**: 24 months
- **Partition Naming**: `{table_name}_YYYY_MM`
- **Partition Management**: Automated via scheduled job

### Example Partitions

```sql
-- document_events_ledger partitions
CREATE TABLE document_events_ledger_2025_01 PARTITION OF document_events_ledger
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE document_events_ledger_2025_02 PARTITION OF document_events_ledger
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- ... continue for 24 months

-- document_view_events partitions (same pattern)
CREATE TABLE document_view_events_2025_01 PARTITION OF document_view_events
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

### Partition Maintenance

**Automated Job** (should run monthly):
1. Create new partition for upcoming month
2. Drop partitions older than 24 months
3. Update monitoring for partition health

**Benefits**:
- Fast data deletion (DROP partition vs DELETE rows)
- Query performance (partition pruning)
- Manageable partition sizes
- Clear retention enforcement

---

## Common Query Patterns

### Analytics Queries on document_events_ledger

#### How many documents were created this month?
```sql
SELECT COUNT(*)
FROM document_events_ledger
WHERE event_type = 'CREATED'
  AND entry_type = 'INITIAL'
  AND event_date >= '2025-11-01'
  AND event_date < '2025-12-01';
```

#### Which accounts had documents printed this week?
```sql
SELECT DISTINCT account_id, COUNT(*) as print_count
FROM document_events_ledger
WHERE event_type = 'PRINTED'
  AND event_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY account_id
ORDER BY print_count DESC;
```

#### Track complete lifecycle of a document using correlation_id
```sql
SELECT
  entry_type,
  event_type,
  event_timestamp,
  source_app,
  event_data
FROM document_events_ledger
WHERE correlation_id = 'xxx'
ORDER BY event_timestamp;
```

#### Most popular document types this quarter
```sql
SELECT
  document_type,
  event_type,
  COUNT(*) as event_count
FROM document_events_ledger
WHERE event_date >= '2025-10-01'
  AND event_date < '2026-01-01'
GROUP BY document_type, event_type
ORDER BY event_count DESC;
```

### Analytics Queries on document_view_events

#### How many times did a specific account view documents this month?
```sql
SELECT COUNT(*)
FROM document_view_events
WHERE account_id = 'xxx'
  AND view_date >= '2025-11-01'
  AND view_date < '2025-12-01';
```

#### Which accounts viewed a specific disclosure?
```sql
SELECT
  account_id,
  viewer_id,
  COUNT(*) as view_count,
  MAX(view_timestamp) as last_viewed
FROM document_view_events
WHERE storage_index_id = 'xxx'
  AND view_date >= '2025-11-01'
GROUP BY account_id, viewer_id
ORDER BY view_count DESC;
```

#### Most viewed document types this quarter
```sql
SELECT
  document_type,
  COUNT(*) as total_views,
  COUNT(DISTINCT account_id) as unique_accounts
FROM document_view_events
WHERE view_date >= '2025-10-01' AND view_date < '2026-01-01'
GROUP BY document_type
ORDER BY total_views DESC
LIMIT 10;
```

#### Average view duration by document type
```sql
SELECT
  document_type,
  AVG(view_duration_seconds) as avg_duration,
  COUNT(*) as views
FROM document_view_events
WHERE view_duration_seconds IS NOT NULL
  AND view_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY document_type
ORDER BY avg_duration DESC;
```

---

## Data Retention Policy

### Retention Period
- **Active Data**: 24 months in partitioned tables
- **Archive**: Data older than 24 months should be exported to cold storage (S3, data warehouse, etc.) before partition drop

### Retention Process

**Monthly Process**:
1. Export data from partition scheduled for deletion
2. Verify export completed successfully
3. Drop partition older than 24 months
4. Create partition for new month (3 months ahead)
5. Update monitoring dashboards

### Compliance Considerations

Depending on regulatory requirements, some document events may need longer retention:
- **Financial disclosures**: May require 7-year retention
- **Regulatory documents**: May require indefinite retention
- **User consent records**: May require specific retention per privacy laws

**Recommendation**: Before implementing automated deletion, consult with:
- Legal team for regulatory requirements
- Compliance team for industry standards
- Data governance team for company policies

---

## Field Value Reference

### entry_type Values

| Value | Description | When Used | Example Scenario |
|-------|-------------|-----------|------------------|
| INITIAL | First entry for a request with full payload | When request first received | Document creation request arrives |
| STATUS_CHANGE | Lifecycle status update | During processing | PDF rendered, saved to storage, sent to vendor |
| FINAL | Terminal completion status | When processing complete | Successfully delivered, permanently deleted |
| ERROR | Exception occurred | When error happens | Rendering failed, delivery failed |

### event_type Values

| Value | Description | Reprint Included? | Example Scenarios |
|-------|-------------|-------------------|-------------------|
| CREATED | Document created/generated | No | New document rendered from template |
| DELETED | Document deleted | No | User deleted document, system purge |
| SIGNED | Document digitally signed | No | User applied e-signature |
| RESTORED | Deleted document restored | No | User undeleted from trash |
| SHARED | Document shared | No | Shared via email, shared with another account |
| PRINTED | Document printed/rendered | Yes (initial) | PDF generated and printed |
| REPRINTED | Existing document resent | Yes (subsequent) | Resend existing PDF to new address |

### actor_type Values

| Value | Description | Example actor_id |
|-------|-------------|------------------|
| USER | End user action | User UUID |
| SYSTEM | Automated system action | System identifier |
| SERVICE | Inter-service call | Service name/ID |
| API | External API call | API client ID |
| SCHEDULER | Scheduled job | Job identifier |

### viewer_type Values (document_view_events)

| Value | Description | Use Case |
|-------|-------------|----------|
| USER | End user | Customer viewing their documents |
| SYSTEM | Automated system check | Health check, validation |
| SUPPORT | Customer support rep | Support helping customer |
| ADMIN | Administrator | Admin audit or troubleshooting |

---

## Important Design Notes

### Why Separate Tables for View Events?

As emphasized by John Drum during schema review:

> "So this is going to be a problem because if you use viewed and you have a shared document, you're going to get millions, literally millions of entries in here for viewing a shared document."

Shared documents (e.g., public disclosures, shared statements) can be viewed millions of times. Including these in the main ledger would:
- Create massive table bloat
- Slow down all analytics queries
- Make partition management difficult

By separating view events:
- Main ledger remains focused on document lifecycle
- View analytics can use different optimization strategies (materialized views)
- Different retention policies can be applied if needed

### Why Denormalize template_type and document_type?

As emphasized by John Drum:

> "If this is a analytic schema specifically for documents, you should denormalize it."

These analytics tables are specifically designed for fast querying. Common analytics questions include:
- "How many statements were viewed this month?"
- "Which account viewed their disclosure?"

Without denormalization, these queries would require:
```sql
-- WITHOUT denormalization (expensive)
SELECT COUNT(*)
FROM document_events_ledger del
JOIN storage_index si ON del.storage_index_id = si.id
JOIN master_template_definition mtd ON si.master_template_id = mtd.id
WHERE mtd.template_type = 'STATEMENT'
  AND del.event_date >= '2025-11-01';

-- WITH denormalization (fast)
SELECT COUNT(*)
FROM document_events_ledger
WHERE template_type = 'STATEMENT'
  AND event_date >= '2025-11-01';
```

The denormalized version:
- Uses only one table
- Leverages indexes efficiently
- Avoids cross-database joins
- Returns results in milliseconds vs. seconds

### Why correlation_id is Critical

Correlation ID allows complete request tracing across the entire system:

```sql
-- See complete lifecycle of a document request
SELECT
  entry_type,
  event_type,
  event_timestamp,
  source_app,
  CASE
    WHEN entry_type = 'INITIAL' THEN event_data->>'request_payload'
    WHEN entry_type = 'STATUS_CHANGE' THEN event_data->>'status_from' || ' → ' || event_data->>'status_to'
    WHEN entry_type = 'ERROR' THEN event_data->>'error_message'
  END as details
FROM document_events_ledger
WHERE correlation_id = 'xxx'
ORDER BY event_timestamp;
```

This single query shows:
1. Initial request received (with full payload)
2. PDF rendering started
3. PDF rendering completed
4. Saved to document hub
5. Sent to delivery vendor
6. Delivery confirmed
7. Any errors that occurred

---

## Schema Evolution Guidelines

### Adding New Columns

When adding columns to partitioned tables:
1. Add to parent table
2. Columns automatically propagate to existing partitions
3. Update this data dictionary
4. Update application code
5. Consider backfilling strategy for existing data

### Adding New event_type or entry_type Values

When adding new enum values to CHECK constraints:
1. Update CHECK constraint on parent table
2. Update this data dictionary
3. Update application code
4. Consider backward compatibility

### Modifying Indexes

Before modifying indexes:
1. Analyze current query patterns
2. Test on representative data volume
3. Schedule during maintenance window (index creation can be slow)
4. Monitor query performance after change

---

## Related Documentation

- [Verification Report](verification_report.md) - Analysis of requirements vs. implementation
- [ERD Diagram](erd_diagram.md) - Visual representation of schema
- [Partition Management Job](../scripts/partition_maintenance.sql) - Automated partition management
- [Migration Guide](../docs/migration_guide.md) - How to migrate from old schema

---

## Revision History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-11-13 | 1.0 | Document Hub Platform Team | Initial data dictionary based on John Drum's review feedback |

---

## Contact

For questions or clarification about this schema:
- **Technical Questions**: Document Hub Platform Team
- **Analytics Questions**: Data Analytics Team
- **Compliance Questions**: Data Governance Team

---

**End of Data Dictionary**
