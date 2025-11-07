# Document Analytics Schema - Simplified Design

## Overview

Minimal analytics schema with just 2 tables to track all document interactions including views, downloads, prints, reprints, shares, and user behavior.

---

## Schema Design

### 1. Document Events (Single Event Log Table)

**Purpose:** Captures ALL document interaction events in one table.

```sql
CREATE TABLE document_events (
    -- Primary Key
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Event Info
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',

    -- Document Reference
    storage_index_id UUID NOT NULL,
    template_id UUID,
    template_version INTEGER,

    -- Denormalized Document Info (for zero-join queries)
    doc_type VARCHAR(100),
    template_name VARCHAR(255),
    category_code VARCHAR(50),
    is_regulatory BOOLEAN DEFAULT FALSE,

    -- User/Actor
    actor_id UUID NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    customer_id UUID,
    account_id UUID,

    -- Session & Context
    session_id UUID,
    correlation_id UUID,
    access_channel VARCHAR(20),
    device_type VARCHAR(50),
    ip_address INET,

    -- Event-Specific Data (flexible JSONB for all event types)
    event_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    /*
    Universal structure that handles all event types:
    {
        // For VIEW events
        "duration_seconds": 45,
        "pages_viewed": [1,2,3],

        // For DOWNLOAD events
        "file_format": "pdf",
        "file_size_bytes": 245678,

        // For PRINT events
        "print_type": "INITIAL|REPRINT",
        "copies": 2,
        "is_color": true,
        "print_cost_cents": 50,

        // For SHARE events
        "share_method": "email|link",
        "recipients": ["user@example.com"],
        "expiry_date": "2024-02-01",

        // Common fields
        "user_agent": "Mozilla/5.0...",
        "geo_location": {"country": "US", "city": "NYC"}
    }
    */

    -- Partitioning helper
    event_date DATE NOT NULL GENERATED ALWAYS AS (DATE(event_timestamp)) STORED,

    CONSTRAINT chk_event_type CHECK (event_type IN (
        'VIEW', 'DOWNLOAD', 'PRINT', 'SHARE', 'EXPORT',
        'DELETE', 'FAILED_ACCESS'
    )),

    CONSTRAINT chk_actor_type CHECK (actor_type IN (
        'CUSTOMER', 'AGENT', 'SYSTEM', 'ADMIN'
    )),

    CONSTRAINT chk_access_channel CHECK (access_channel IN (
        'WEB', 'MOBILE', 'API', 'EMAIL', 'PRINT'
    )),

    CONSTRAINT chk_event_status CHECK (event_status IN (
        'SUCCESS', 'FAILED', 'PARTIAL'
    )),

    CONSTRAINT fk_event_document
        FOREIGN KEY (storage_index_id)
        REFERENCES storage_index(storage_index_id)
        ON DELETE CASCADE

) PARTITION BY RANGE (event_date);

-- Create monthly partitions
CREATE TABLE document_events_2024_01 PARTITION OF document_events
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE document_events_2024_02 PARTITION OF document_events
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
-- ... create more partitions as needed

-- ========================================
-- Indexes
-- ========================================

-- Core access patterns
CREATE INDEX idx_events_document_time
    ON document_events(storage_index_id, event_timestamp DESC);

CREATE INDEX idx_events_actor_time
    ON document_events(actor_id, event_timestamp DESC);

CREATE INDEX idx_events_customer_time
    ON document_events(customer_id, event_timestamp DESC)
    WHERE customer_id IS NOT NULL;

CREATE INDEX idx_events_account_time
    ON document_events(account_id, event_timestamp DESC)
    WHERE account_id IS NOT NULL;

-- Event type queries
CREATE INDEX idx_events_type_time
    ON document_events(event_type, event_timestamp DESC);

-- Template analytics
CREATE INDEX idx_events_template
    ON document_events(template_id, template_version, event_timestamp DESC);

CREATE INDEX idx_events_doctype
    ON document_events(doc_type, event_timestamp DESC);

-- Print tracking (for reprints)
CREATE INDEX idx_events_prints
    ON document_events(storage_index_id, event_timestamp DESC)
    WHERE event_type = 'PRINT';

-- Regulatory compliance
CREATE INDEX idx_events_regulatory
    ON document_events(storage_index_id, event_timestamp DESC)
    WHERE is_regulatory = TRUE;

-- Failed access tracking
CREATE INDEX idx_events_failed
    ON document_events(event_timestamp DESC)
    WHERE event_status = 'FAILED';

-- Session tracking
CREATE INDEX idx_events_session
    ON document_events(session_id, event_timestamp)
    WHERE session_id IS NOT NULL;

-- JSONB queries
CREATE INDEX idx_events_data
    ON document_events USING GIN (event_data);

COMMENT ON TABLE document_events IS 'Universal event log for all document interactions';
COMMENT ON COLUMN document_events.event_data IS 'Flexible JSONB storing event-specific data for all event types';
```

---

### 2. Document Analytics Summary (Pre-Aggregated Stats)

**Purpose:** Daily aggregated statistics per document for fast reporting.

```sql
CREATE TABLE document_analytics (
    -- Composite Primary Key
    storage_index_id UUID NOT NULL,
    summary_date DATE NOT NULL,

    -- Denormalized Document Info (for zero-join reporting)
    template_id UUID,
    template_version INTEGER,
    template_name VARCHAR(255),
    doc_type VARCHAR(100),
    category_code VARCHAR(50),
    line_of_business VARCHAR(50),

    -- View Metrics
    view_count INTEGER DEFAULT 0,
    unique_viewers INTEGER DEFAULT 0,
    total_view_duration_seconds BIGINT DEFAULT 0,

    -- Download Metrics
    download_count INTEGER DEFAULT 0,
    unique_downloaders INTEGER DEFAULT 0,

    -- Print Metrics
    print_count INTEGER DEFAULT 0,
    reprint_count INTEGER DEFAULT 0,
    total_copies_printed INTEGER DEFAULT 0,
    total_print_cost_cents BIGINT DEFAULT 0,

    -- Share Metrics
    share_count INTEGER DEFAULT 0,
    total_share_recipients INTEGER DEFAULT 0,

    -- Export Metrics
    export_count INTEGER DEFAULT 0,

    -- User Type Breakdown
    customer_access_count INTEGER DEFAULT 0,
    agent_access_count INTEGER DEFAULT 0,
    system_access_count INTEGER DEFAULT 0,

    -- Channel Breakdown
    web_access_count INTEGER DEFAULT 0,
    mobile_access_count INTEGER DEFAULT 0,
    api_access_count INTEGER DEFAULT 0,

    -- Error Tracking
    failed_access_count INTEGER DEFAULT 0,

    -- First & Last Access
    first_access_at TIMESTAMPTZ,
    last_access_at TIMESTAMPTZ,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (storage_index_id, summary_date),

    CONSTRAINT fk_analytics_document
        FOREIGN KEY (storage_index_id)
        REFERENCES storage_index(storage_index_id)
        ON DELETE CASCADE
);

-- ========================================
-- Indexes
-- ========================================

CREATE INDEX idx_analytics_date
    ON document_analytics(summary_date DESC);

CREATE INDEX idx_analytics_template
    ON document_analytics(template_id, template_version, summary_date DESC);

CREATE INDEX idx_analytics_doctype
    ON document_analytics(doc_type, summary_date DESC);

CREATE INDEX idx_analytics_category
    ON document_analytics(category_code, summary_date DESC);

CREATE INDEX idx_analytics_views
    ON document_analytics(view_count DESC, summary_date DESC)
    WHERE view_count > 0;

CREATE INDEX idx_analytics_prints
    ON document_analytics(print_count DESC, summary_date DESC)
    WHERE print_count > 0;

CREATE INDEX idx_analytics_updated
    ON document_analytics(updated_at DESC);

COMMENT ON TABLE document_analytics IS 'Daily aggregated document analytics for fast reporting (updated via triggers or batch job)';
```

---

## Helper Functions

### Function: Log Document Event (Single Entry Point)

```sql
CREATE OR REPLACE FUNCTION log_document_event(
    p_event_type VARCHAR,
    p_storage_index_id UUID,
    p_actor_id UUID,
    p_actor_type VARCHAR,
    p_event_data JSONB DEFAULT '{}'::jsonb,
    p_session_id UUID DEFAULT NULL,
    p_access_channel VARCHAR DEFAULT 'WEB',
    p_correlation_id UUID DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
    v_event_id UUID;
    v_doc_info RECORD;
BEGIN
    -- Get document info (denormalized for event log)
    SELECT
        template_id,
        template_version,
        template_name,
        doc_type,
        category_code,
        line_of_business,
        is_regulatory,
        customer_id,
        account_id
    INTO v_doc_info
    FROM storage_index
    WHERE storage_index_id = p_storage_index_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Document not found: %', p_storage_index_id;
    END IF;

    -- Insert event
    INSERT INTO document_events (
        event_type,
        storage_index_id,
        template_id,
        template_version,
        doc_type,
        template_name,
        category_code,
        is_regulatory,
        actor_id,
        actor_type,
        customer_id,
        account_id,
        session_id,
        correlation_id,
        access_channel,
        event_data
    ) VALUES (
        p_event_type,
        p_storage_index_id,
        v_doc_info.template_id,
        v_doc_info.template_version,
        v_doc_info.doc_type,
        v_doc_info.template_name,
        v_doc_info.category_code,
        v_doc_info.is_regulatory,
        p_actor_id,
        p_actor_type,
        v_doc_info.customer_id,
        v_doc_info.account_id,
        p_session_id,
        p_correlation_id,
        p_access_channel,
        p_event_data
    )
    RETURNING event_id INTO v_event_id;

    -- Update storage_index counters (real-time)
    UPDATE storage_index
    SET
        last_accessed_at = CURRENT_TIMESTAMP,
        access_count = access_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE storage_index_id = p_storage_index_id;

    RETURN v_event_id;
END;
$$;

COMMENT ON FUNCTION log_document_event IS 'Single entry point for logging all document events';
```

### Function: Aggregate Daily Analytics (Batch Job)

```sql
CREATE OR REPLACE FUNCTION aggregate_daily_analytics(p_date DATE)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO document_analytics (
        storage_index_id,
        summary_date,
        template_id,
        template_version,
        template_name,
        doc_type,
        category_code,
        line_of_business,
        view_count,
        unique_viewers,
        total_view_duration_seconds,
        download_count,
        unique_downloaders,
        print_count,
        reprint_count,
        total_copies_printed,
        total_print_cost_cents,
        share_count,
        total_share_recipients,
        export_count,
        customer_access_count,
        agent_access_count,
        system_access_count,
        web_access_count,
        mobile_access_count,
        api_access_count,
        failed_access_count,
        first_access_at,
        last_access_at
    )
    SELECT
        storage_index_id,
        p_date,
        MAX(template_id),
        MAX(template_version),
        MAX(template_name),
        MAX(doc_type),
        MAX(category_code),
        MAX(line_of_business),

        -- View metrics
        COUNT(*) FILTER (WHERE event_type = 'VIEW'),
        COUNT(DISTINCT actor_id) FILTER (WHERE event_type = 'VIEW'),
        COALESCE(SUM((event_data->>'duration_seconds')::integer) FILTER (WHERE event_type = 'VIEW'), 0),

        -- Download metrics
        COUNT(*) FILTER (WHERE event_type = 'DOWNLOAD'),
        COUNT(DISTINCT actor_id) FILTER (WHERE event_type = 'DOWNLOAD'),

        -- Print metrics
        COUNT(*) FILTER (WHERE event_type = 'PRINT' AND (event_data->>'print_type' IS NULL OR event_data->>'print_type' = 'INITIAL')),
        COUNT(*) FILTER (WHERE event_type = 'PRINT' AND event_data->>'print_type' = 'REPRINT'),
        COALESCE(SUM((event_data->>'copies')::integer) FILTER (WHERE event_type = 'PRINT'), 0),
        COALESCE(SUM((event_data->>'print_cost_cents')::bigint) FILTER (WHERE event_type = 'PRINT'), 0),

        -- Share metrics
        COUNT(*) FILTER (WHERE event_type = 'SHARE'),
        COALESCE(SUM(jsonb_array_length(event_data->'recipients')) FILTER (WHERE event_type = 'SHARE'), 0),

        -- Export metrics
        COUNT(*) FILTER (WHERE event_type = 'EXPORT'),

        -- User type breakdown
        COUNT(*) FILTER (WHERE actor_type = 'CUSTOMER'),
        COUNT(*) FILTER (WHERE actor_type = 'AGENT'),
        COUNT(*) FILTER (WHERE actor_type = 'SYSTEM'),

        -- Channel breakdown
        COUNT(*) FILTER (WHERE access_channel = 'WEB'),
        COUNT(*) FILTER (WHERE access_channel = 'MOBILE'),
        COUNT(*) FILTER (WHERE access_channel = 'API'),

        -- Error tracking
        COUNT(*) FILTER (WHERE event_status = 'FAILED'),

        -- First & last access
        MIN(event_timestamp),
        MAX(event_timestamp)

    FROM document_events
    WHERE event_date = p_date
    GROUP BY storage_index_id

    ON CONFLICT (storage_index_id, summary_date)
    DO UPDATE SET
        view_count = EXCLUDED.view_count,
        unique_viewers = EXCLUDED.unique_viewers,
        total_view_duration_seconds = EXCLUDED.total_view_duration_seconds,
        download_count = EXCLUDED.download_count,
        unique_downloaders = EXCLUDED.unique_downloaders,
        print_count = EXCLUDED.print_count,
        reprint_count = EXCLUDED.reprint_count,
        total_copies_printed = EXCLUDED.total_copies_printed,
        total_print_cost_cents = EXCLUDED.total_print_cost_cents,
        share_count = EXCLUDED.share_count,
        total_share_recipients = EXCLUDED.total_share_recipients,
        export_count = EXCLUDED.export_count,
        customer_access_count = EXCLUDED.customer_access_count,
        agent_access_count = EXCLUDED.agent_access_count,
        system_access_count = EXCLUDED.system_access_count,
        web_access_count = EXCLUDED.web_access_count,
        mobile_access_count = EXCLUDED.mobile_access_count,
        api_access_count = EXCLUDED.api_access_count,
        failed_access_count = EXCLUDED.failed_access_count,
        last_access_at = EXCLUDED.last_access_at,
        updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Daily analytics aggregated for %', p_date;
END;
$$;

COMMENT ON FUNCTION aggregate_daily_analytics IS 'Batch job to aggregate daily analytics from events into summary table';
```

---

## Common Query Examples

### 1. Track Who Viewed a Document and When

```sql
-- Get all views for a document
SELECT
    event_id,
    event_timestamp,
    actor_id,
    actor_type,
    customer_id,
    access_channel,
    event_data->>'duration_seconds' as duration_seconds,
    event_data->>'pages_viewed' as pages_viewed,
    ip_address
FROM document_events
WHERE storage_index_id = 'document-uuid'
  AND event_type = 'VIEW'
  AND event_status = 'SUCCESS'
ORDER BY event_timestamp DESC;
```

### 2. Count Prints and Reprints

```sql
-- Get print/reprint counts for a document
SELECT
    storage_index_id,
    COUNT(*) FILTER (WHERE event_data->>'print_type' IS NULL OR event_data->>'print_type' = 'INITIAL') as initial_prints,
    COUNT(*) FILTER (WHERE event_data->>'print_type' = 'REPRINT') as reprints,
    SUM((event_data->>'copies')::integer) as total_copies,
    SUM((event_data->>'print_cost_cents')::bigint) / 100.0 as total_cost_dollars
FROM document_events
WHERE storage_index_id = 'document-uuid'
  AND event_type = 'PRINT'
GROUP BY storage_index_id;
```

### 3. Most Viewed Documents (Last 30 Days)

```sql
-- Fast query using aggregated table
SELECT
    da.storage_index_id,
    da.template_name,
    da.doc_type,
    da.category_code,
    SUM(da.view_count) as total_views,
    SUM(da.download_count) as total_downloads,
    SUM(da.print_count) as total_prints
FROM document_analytics da
WHERE da.summary_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY da.storage_index_id, da.template_name, da.doc_type, da.category_code
ORDER BY total_views DESC
LIMIT 50;
```

### 4. User Document Activity

```sql
-- All documents accessed by a user
SELECT
    de.storage_index_id,
    de.template_name,
    de.doc_type,
    COUNT(*) FILTER (WHERE de.event_type = 'VIEW') as views,
    COUNT(*) FILTER (WHERE de.event_type = 'DOWNLOAD') as downloads,
    COUNT(*) FILTER (WHERE de.event_type = 'PRINT') as prints,
    MIN(de.event_timestamp) as first_access,
    MAX(de.event_timestamp) as last_access
FROM document_events de
WHERE de.actor_id = 'user-uuid'
  AND de.event_timestamp >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY de.storage_index_id, de.template_name, de.doc_type
ORDER BY last_access DESC;
```

### 5. Regulatory Document Access Audit

```sql
-- Compliance audit trail
SELECT
    event_id,
    event_timestamp,
    storage_index_id,
    template_name,
    event_type,
    actor_id,
    actor_type,
    customer_id,
    account_id,
    access_channel,
    ip_address,
    event_data
FROM document_events
WHERE is_regulatory = TRUE
  AND event_timestamp >= CURRENT_DATE - INTERVAL '7 years'
ORDER BY event_timestamp DESC;
```

### 6. Document Sharing Analysis

```sql
-- All share events with recipients
SELECT
    event_timestamp,
    storage_index_id,
    template_name,
    actor_id,
    event_data->>'share_method' as share_method,
    event_data->'recipients' as recipients,
    jsonb_array_length(event_data->'recipients') as recipient_count,
    event_data->>'expiry_date' as expiry_date
FROM document_events
WHERE event_type = 'SHARE'
  AND event_timestamp >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY event_timestamp DESC;
```

### 7. Failed Access Attempts

```sql
-- Security monitoring
SELECT
    event_timestamp,
    storage_index_id,
    template_name,
    actor_id,
    actor_type,
    event_type,
    event_data->>'failure_reason' as failure_reason,
    ip_address,
    access_channel
FROM document_events
WHERE event_status = 'FAILED'
  AND event_timestamp >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY event_timestamp DESC;
```

### 8. Print Cost Analysis

```sql
-- Total print costs by document type
SELECT
    doc_type,
    category_code,
    SUM(total_print_cost_cents) / 100.0 as total_cost_dollars,
    SUM(print_count + reprint_count) as total_print_jobs,
    SUM(total_copies_printed) as total_copies
FROM document_analytics
WHERE summary_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY doc_type, category_code
ORDER BY total_cost_dollars DESC;
```

---

## Usage Examples

### Example 1: Log a View Event

```sql
SELECT log_document_event(
    'VIEW',
    'doc-uuid-here',
    'user-uuid-here',
    'CUSTOMER',
    '{"duration_seconds": 45, "pages_viewed": [1,2,3], "scroll_percentage": 75}'::jsonb,
    'session-uuid',
    'WEB'
);
```

### Example 2: Log a Print Event

```sql
SELECT log_document_event(
    'PRINT',
    'doc-uuid-here',
    'user-uuid-here',
    'CUSTOMER',
    '{"print_type": "INITIAL", "copies": 2, "is_color": true, "print_cost_cents": 50}'::jsonb,
    'session-uuid',
    'PRINT'
);
```

### Example 3: Log a Reprint Event

```sql
SELECT log_document_event(
    'PRINT',
    'doc-uuid-here',
    'user-uuid-here',
    'CUSTOMER',
    '{"print_type": "REPRINT", "copies": 1, "is_color": false, "print_cost_cents": 25}'::jsonb,
    'session-uuid',
    'PRINT'
);
```

### Example 4: Log a Share Event

```sql
SELECT log_document_event(
    'SHARE',
    'doc-uuid-here',
    'user-uuid-here',
    'CUSTOMER',
    '{
        "share_method": "email",
        "recipients": ["friend@example.com", "colleague@example.com"],
        "expiry_date": "2024-02-01T00:00:00Z",
        "message": "Check out this document"
    }'::jsonb,
    'session-uuid',
    'WEB'
);
```

---

## Data Retention

```sql
-- Drop old partitions (older than 7 years for non-regulatory)
DROP TABLE IF EXISTS document_events_2017_01;

-- Or delete specific data
DELETE FROM document_events
WHERE event_date < CURRENT_DATE - INTERVAL '7 years'
  AND is_regulatory = FALSE;

-- Archive old aggregated data
DELETE FROM document_analytics
WHERE summary_date < CURRENT_DATE - INTERVAL '3 years';
```

---

## Scheduled Jobs

### Daily Aggregation Job (Run at 2 AM)

```sql
-- Aggregate previous day's analytics
SELECT aggregate_daily_analytics(CURRENT_DATE - INTERVAL '1 day');
```

### Partition Maintenance (Run Monthly)

```sql
-- Create next month's partition
DO $$
DECLARE
    v_next_month DATE := DATE_TRUNC('month', CURRENT_DATE + INTERVAL '2 months');
    v_month_after DATE := v_next_month + INTERVAL '1 month';
    v_table_name TEXT := 'document_events_' || TO_CHAR(v_next_month, 'YYYY_MM');
BEGIN
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF document_events FOR VALUES FROM (%L) TO (%L)',
        v_table_name,
        v_next_month,
        v_month_after
    );
    RAISE NOTICE 'Created partition: %', v_table_name;
END $$;
```

---

## Performance Considerations

1. **Partitioning**: document_events partitioned by month for query performance
2. **Indexes**: Targeted indexes for common query patterns
3. **Denormalization**: Template info denormalized in events table (zero joins)
4. **Aggregation**: Pre-aggregate daily stats for reporting
5. **JSONB**: Flexible event_data field with GIN index
6. **Batch Processing**: Daily aggregation runs during low-traffic hours

---

## Summary

✅ **Just 2 Tables**: document_events + document_analytics
✅ **Captures Everything**: Views, downloads, prints, reprints, shares, all user actions
✅ **Zero-Join Queries**: Denormalized data for fast queries
✅ **Flexible JSONB**: Event-specific data in single field
✅ **Partitioned**: Monthly partitions for performance
✅ **Pre-Aggregated**: Daily summaries for reports
✅ **Simple API**: Single function to log all events

**Version**: 1.0 (Simplified)
**Date**: 2025-11-06
**Tables**: 2 (document_events, document_analytics)
