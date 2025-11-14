-- ============================================================================
-- Document View Events Table
-- ============================================================================
-- Purpose: Separate table for tracking document view events to prevent
--          millions of entries in main ledger (as warned by John Drum)
-- Owner: Document Hub Platform
-- Retention: 24 months (monthly partitions)
-- Partitioning: By view_date (monthly)
-- ============================================================================

CREATE TABLE document_view_events (
  -- ============================================================================
  -- PRIMARY KEY
  -- ============================================================================
  view_event_id UUID NOT NULL DEFAULT gen_random_uuid(),

  -- ============================================================================
  -- CORE EVENT INFORMATION
  -- ============================================================================
  -- When the view occurred
  view_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- Separate date field for partition key
  view_date DATE NOT NULL DEFAULT CURRENT_DATE,

  -- Correlation ID for linking to document creation/print events
  correlation_id UUID,

  -- ============================================================================
  -- DOCUMENT IDENTIFICATION (Denormalized)
  -- ============================================================================
  -- Reference to storage index
  storage_index_id UUID NOT NULL,

  -- Master template reference
  master_template_id UUID,

  -- Denormalized for analytics
  template_type VARCHAR(100),

  -- Denormalized document type
  document_type VARCHAR(100),

  -- Denormalized template name for human-readable reporting
  template_name VARCHAR(255),

  -- Shared document flag (shared docs have different view patterns)
  is_shared_document BOOLEAN DEFAULT FALSE,

  -- ============================================================================
  -- VIEWER INFORMATION (Required - as John said: "what you really want to
  -- know is who viewed it")
  -- ============================================================================
  -- Customer ID for customer-level analytics
  customer_id UUID,

  -- Account that viewed the document
  account_id UUID NOT NULL,

  -- Specific viewer (may differ from account for shared documents)
  viewer_id UUID NOT NULL,

  -- Type of viewer (USER, SYSTEM, etc.)
  viewer_type VARCHAR(50) NOT NULL DEFAULT 'USER',

  -- ============================================================================
  -- VIEW CONTEXT
  -- ============================================================================
  -- Application/channel where view occurred
  source_app VARCHAR(100) NOT NULL,

  -- Device type
  device_type VARCHAR(100),

  -- Geographic location or IP address
  location VARCHAR(255),

  -- Session identifier for grouping related views
  session_id UUID,

  -- View duration in seconds (if tracked)
  view_duration_seconds INTEGER,

  -- ============================================================================
  -- ADDITIONAL METADATA
  -- ============================================================================
  -- Flexible metadata storage
  view_data JSONB NOT NULL DEFAULT '{}'::jsonb,

  -- ============================================================================
  -- CONSTRAINTS
  -- ============================================================================
  PRIMARY KEY (view_event_id, view_date)
) PARTITION BY RANGE (view_date);

-- ============================================================================
-- INDEXES FOR QUERY PERFORMANCE
-- ============================================================================

-- Time-based queries
CREATE INDEX idx_document_view_events_timestamp ON document_view_events (view_timestamp);

-- Account-specific view history (most common query)
CREATE INDEX idx_document_view_events_account_id ON document_view_events (account_id);

-- Viewer-specific history
CREATE INDEX idx_document_view_events_viewer_id ON document_view_events (viewer_id);

-- Document-specific view tracking
CREATE INDEX idx_document_view_events_storage_id ON document_view_events (storage_index_id);

-- Template analytics
CREATE INDEX idx_document_view_events_template_id ON document_view_events (master_template_id) WHERE master_template_id IS NOT NULL;

-- Correlation tracking
CREATE INDEX idx_document_view_events_correlation_id ON document_view_events (correlation_id) WHERE correlation_id IS NOT NULL;

-- Customer-level view analytics (across multiple accounts)
CREATE INDEX idx_document_view_events_customer_id ON document_view_events (customer_id) WHERE customer_id IS NOT NULL;

-- Shared document filtering
CREATE INDEX idx_document_view_events_is_shared ON document_view_events (is_shared_document);

-- Composite index for common query: which accounts viewed a specific document
CREATE INDEX idx_document_view_events_doc_account ON document_view_events (storage_index_id, account_id, view_date);

-- Composite index for customer-level view analytics over time
CREATE INDEX idx_document_view_events_customer_time ON document_view_events (customer_id, view_date) WHERE customer_id IS NOT NULL;

-- Composite index for analytics: account views over time
CREATE INDEX idx_document_view_events_account_time ON document_view_events (account_id, view_date);

-- Template popularity analytics
CREATE INDEX idx_document_view_events_template_time ON document_view_events (master_template_id, view_date) WHERE master_template_id IS NOT NULL;

-- ============================================================================
-- PARTITIONING STRATEGY
-- ============================================================================
-- Monthly partitions for 24-month retention
-- Partitions managed by maintenance job

-- Example partition creation (should be automated):
-- CREATE TABLE document_view_events_2025_01 PARTITION OF document_view_events
--     FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
-- CREATE TABLE document_view_events_2025_02 PARTITION OF document_view_events
--     FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
-- ... continue for 24 months

-- ============================================================================
-- MATERIALIZED VIEW FOR LAST VIEW TRACKING
-- ============================================================================
-- As John suggested, use update-in-place for "last viewed" instead of
-- creating millions of rows. This materialized view provides quick access
-- to the most recent view for each document.

CREATE MATERIALIZED VIEW document_last_viewed AS
SELECT DISTINCT ON (storage_index_id)
  storage_index_id,
  view_event_id,
  view_timestamp AS last_viewed_at,
  account_id AS last_viewed_by_account,
  viewer_id AS last_viewed_by_viewer,
  device_type AS last_viewed_device,
  source_app AS last_viewed_from
FROM document_view_events
ORDER BY storage_index_id, view_timestamp DESC;

-- Index on materialized view for fast lookups
CREATE UNIQUE INDEX idx_document_last_viewed_storage_id ON document_last_viewed (storage_index_id);

-- Refresh strategy (should be run periodically, e.g., every hour or daily depending on requirements)
-- REFRESH MATERIALIZED VIEW CONCURRENTLY document_last_viewed;

COMMENT ON MATERIALIZED VIEW document_last_viewed IS 'Materialized view tracking the most recent view event for each document. Refresh periodically based on business requirements. This avoids expensive queries for "last viewed" information.';

-- ============================================================================
-- AGGREGATE VIEW FOR VIEW COUNTS
-- ============================================================================
-- Common analytics query: How many times has a document been viewed?

CREATE MATERIALIZED VIEW document_view_counts AS
SELECT
  storage_index_id,
  master_template_id,
  template_type,
  document_type,
  COUNT(*) AS total_views,
  COUNT(DISTINCT account_id) AS unique_accounts,
  COUNT(DISTINCT viewer_id) AS unique_viewers,
  MIN(view_timestamp) AS first_viewed_at,
  MAX(view_timestamp) AS last_viewed_at
FROM document_view_events
GROUP BY storage_index_id, master_template_id, template_type, document_type;

-- Indexes on aggregate view
CREATE UNIQUE INDEX idx_document_view_counts_storage_id ON document_view_counts (storage_index_id);
CREATE INDEX idx_document_view_counts_template_id ON document_view_counts (master_template_id) WHERE master_template_id IS NOT NULL;

COMMENT ON MATERIALIZED VIEW document_view_counts IS 'Pre-aggregated view counts for fast analytics queries. Refresh based on reporting requirements (e.g., daily for dashboards).';

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================
COMMENT ON TABLE document_view_events IS 'Separate table for document view tracking. Isolated from main ledger to prevent volume issues with shared documents that may be viewed millions of times. Includes viewer information as emphasized by John Drum.';

COMMENT ON COLUMN document_view_events.view_event_id IS 'Unique identifier for this view event';
COMMENT ON COLUMN document_view_events.view_timestamp IS 'Precise timestamp when view occurred';
COMMENT ON COLUMN document_view_events.view_date IS 'Date extracted from view_timestamp, used for partitioning';
COMMENT ON COLUMN document_view_events.correlation_id IS 'Links to original document creation/print event';
COMMENT ON COLUMN document_view_events.storage_index_id IS 'Document that was viewed (required)';
COMMENT ON COLUMN document_view_events.master_template_id IS 'Template reference for the viewed document';
COMMENT ON COLUMN document_view_events.template_type IS 'Denormalized template type for analytics';
COMMENT ON COLUMN document_view_events.document_type IS 'Denormalized document type for analytics';
COMMENT ON COLUMN document_view_events.template_name IS 'Denormalized template name for human-readable reporting';
COMMENT ON COLUMN document_view_events.is_shared_document IS 'TRUE if document is shared. Shared documents have different view patterns (potentially millions of views).';
COMMENT ON COLUMN document_view_events.customer_id IS 'Customer identifier for customer-level view analytics across multiple accounts';
COMMENT ON COLUMN document_view_events.account_id IS 'Account context for the view (required for meaningful analytics)';
COMMENT ON COLUMN document_view_events.viewer_id IS 'Who actually viewed the document (required - may differ from account holder for shared docs)';
COMMENT ON COLUMN document_view_events.viewer_type IS 'Type of viewer (USER, SYSTEM, etc.)';
COMMENT ON COLUMN document_view_events.source_app IS 'Application/channel where view occurred (required)';
COMMENT ON COLUMN document_view_events.device_type IS 'Device used for viewing';
COMMENT ON COLUMN document_view_events.location IS 'Geographic location or IP address';
COMMENT ON COLUMN document_view_events.session_id IS 'Session identifier for grouping related views';
COMMENT ON COLUMN document_view_events.view_duration_seconds IS 'How long the document was viewed (if tracked)';
COMMENT ON COLUMN document_view_events.view_data IS 'Additional metadata in JSONB format';

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: How many times did a specific account view documents this month?
-- SELECT COUNT(*)
-- FROM document_view_events
-- WHERE account_id = 'xxx'
--   AND view_date >= '2025-11-01'
--   AND view_date < '2025-12-01';

-- Example 2: Which accounts viewed a specific disclosure this month?
-- SELECT DISTINCT account_id, viewer_id, COUNT(*) as view_count
-- FROM document_view_events
-- WHERE storage_index_id = 'xxx'
--   AND view_date >= '2025-11-01'
--   AND view_date < '2025-12-01'
-- GROUP BY account_id, viewer_id;

-- Example 3: Most viewed document types this quarter
-- SELECT document_type, COUNT(*) as views, COUNT(DISTINCT account_id) as unique_accounts
-- FROM document_view_events
-- WHERE view_date >= '2025-10-01' AND view_date < '2026-01-01'
-- GROUP BY document_type
-- ORDER BY views DESC
-- LIMIT 10;

-- Example 4: Get last viewed information for a document (using materialized view)
-- SELECT * FROM document_last_viewed WHERE storage_index_id = 'xxx';

-- Example 5: Get view statistics for a document (using materialized view)
-- SELECT * FROM document_view_counts WHERE storage_index_id = 'xxx';
