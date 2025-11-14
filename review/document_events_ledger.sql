-- ============================================================================
-- Document Events Ledger Table
-- ============================================================================
-- Purpose: Analytics-optimized ledger for tracking document lifecycle events
-- Owner: Document Hub Platform
-- Retention: 24 months (monthly partitions)
-- Partitioning: By event_date (monthly)
-- ============================================================================

CREATE TABLE document_events_ledger (
  -- ============================================================================
  -- PRIMARY KEY
  -- ============================================================================
  doc_event_id UUID NOT NULL DEFAULT gen_random_uuid(),

  -- ============================================================================
  -- CORE EVENT CLASSIFICATION
  -- ============================================================================
  -- Entry type classifies the nature of this ledger entry
  entry_type VARCHAR(50) NOT NULL CHECK (entry_type IN ('INITIAL', 'STATUS_CHANGE', 'FINAL', 'ERROR')),

  -- Correlation ID for tracking related entries across the system
  correlation_id UUID,

  -- Event timestamp with timezone support
  event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- Separate date field for partition key (derived from event_timestamp)
  event_date DATE NOT NULL DEFAULT CURRENT_DATE,

  -- Event type for high-level categorization (VIEWED moved to separate table)
  event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('CREATED', 'DELETED', 'SIGNED', 'RESTORED', 'SHARED', 'PRINTED', 'REPRINTED')),

  -- Granular event code for detailed classification
  event_code VARCHAR(100),

  -- Flexible metadata storage (structure varies by entry_type and source_app)
  event_data JSONB NOT NULL DEFAULT '{}'::jsonb,

  -- ============================================================================
  -- ACTOR INFORMATION
  -- ============================================================================
  -- Who performed this action (user ID, system ID, etc.)
  actor_id UUID,

  -- Type of actor (USER, SYSTEM, SERVICE, API, etc.)
  actor_type VARCHAR(50),

  -- ============================================================================
  -- DOCUMENT CONTEXT (Denormalized for Analytics Performance)
  -- ============================================================================
  -- Reference to storage index
  storage_index_id UUID,

  -- Master template reference
  master_template_id UUID,

  -- Denormalized template type to avoid joins in analytics queries
  template_type VARCHAR(100),

  -- Denormalized document type to avoid joins in analytics queries
  document_type VARCHAR(100),

  -- Denormalized template name for human-readable reporting
  template_name VARCHAR(255),

  -- Shared document flag (shared docs have different analytics patterns)
  is_shared_document BOOLEAN DEFAULT FALSE,

  -- ============================================================================
  -- CUSTOMER & ACCOUNT CONTEXT
  -- ============================================================================
  -- Customer ID - enables customer-level analytics across multiple accounts
  -- (As John noted: "that customer's got five accounts")
  customer_id UUID,

  -- Account ID - required for analytics queries
  account_id UUID NOT NULL,

  -- ============================================================================
  -- SOURCE/ORIGIN TRACKING
  -- ============================================================================
  -- Which application/service created this ledger entry
  source_app VARCHAR(100) NOT NULL,

  -- ============================================================================
  -- ADDITIONAL CONTEXT
  -- ============================================================================
  -- Device type (MOBILE, DESKTOP, TABLET, etc.)
  device_type VARCHAR(100),

  -- Geographic location or IP address
  location VARCHAR(255),

  -- Delivery channel for print/reprint events (EMAIL, POSTAL_MAIL, ONLINE_PORTAL, VENDOR_HOV, etc.)
  delivery_channel VARCHAR(50),

  -- ============================================================================
  -- CONSTRAINTS
  -- ============================================================================
  PRIMARY KEY (doc_event_id, event_date)
) PARTITION BY RANGE (event_date);

-- ============================================================================
-- INDEXES FOR QUERY PERFORMANCE
-- ============================================================================
-- Note: Indexes are created on the parent table and inherited by partitions

-- Primary timestamp-based queries
CREATE INDEX idx_document_events_ledger_timestamp ON document_events_ledger (event_timestamp);

-- Account-specific queries (most common analytics pattern)
CREATE INDEX idx_document_events_ledger_account_id ON document_events_ledger (account_id);

-- Template-based analytics
CREATE INDEX idx_document_events_ledger_template_id ON document_events_ledger (master_template_id);

-- Event type filtering (for analytics aggregations)
CREATE INDEX idx_document_events_ledger_event_type ON document_events_ledger (event_type);

-- Entry type filtering (to query only INITIAL entries or STATUS_CHANGE entries)
CREATE INDEX idx_document_events_ledger_entry_type ON document_events_ledger (entry_type);

-- Source application tracking
CREATE INDEX idx_document_events_ledger_source_app ON document_events_ledger (source_app);

-- Correlation ID for tracing related events
CREATE INDEX idx_document_events_ledger_correlation_id ON document_events_ledger (correlation_id) WHERE correlation_id IS NOT NULL;

-- Customer-level analytics (across multiple accounts per customer)
CREATE INDEX idx_document_events_ledger_customer_id ON document_events_ledger (customer_id) WHERE customer_id IS NOT NULL;

-- Delivery channel analytics (for print/reprint events)
CREATE INDEX idx_document_events_ledger_delivery_channel ON document_events_ledger (delivery_channel) WHERE delivery_channel IS NOT NULL;

-- Shared document filtering (shared vs. personal documents)
CREATE INDEX idx_document_events_ledger_is_shared ON document_events_ledger (is_shared_document);

-- Composite index for common query pattern: account + time range + event type
CREATE INDEX idx_document_events_ledger_account_time_type ON document_events_ledger (account_id, event_date, event_type);

-- Composite index for customer-level analytics over time
CREATE INDEX idx_document_events_ledger_customer_time ON document_events_ledger (customer_id, event_date) WHERE customer_id IS NOT NULL;

-- Composite index for template analytics: template + time range
CREATE INDEX idx_document_events_ledger_template_time ON document_events_ledger (master_template_id, event_date) WHERE master_template_id IS NOT NULL;

-- ============================================================================
-- PARTITIONING STRATEGY
-- ============================================================================
-- Monthly partitions for 24-month retention
-- Partitions should be created in advance and managed by a maintenance job
-- Old partitions can be dropped to enforce retention policy

-- Example partition creation (should be automated):
-- CREATE TABLE document_events_ledger_2025_01 PARTITION OF document_events_ledger
--     FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
-- CREATE TABLE document_events_ledger_2025_02 PARTITION OF document_events_ledger
--     FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
-- ... continue for 24 months

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================
COMMENT ON TABLE document_events_ledger IS 'Analytics-optimized ledger for document lifecycle events. Denormalized for query performance. Partitioned monthly for 24-month retention. VIEWED events tracked separately in document_view_events table.';

COMMENT ON COLUMN document_events_ledger.doc_event_id IS 'Unique identifier for this ledger entry';
COMMENT ON COLUMN document_events_ledger.entry_type IS 'Classification of entry: INITIAL (first entry with full payload), STATUS_CHANGE (lifecycle update), FINAL (completion), ERROR (exception)';
COMMENT ON COLUMN document_events_ledger.correlation_id IS 'Links related entries across different tables and systems for request tracing';
COMMENT ON COLUMN document_events_ledger.event_timestamp IS 'Precise timestamp when event occurred (with timezone)';
COMMENT ON COLUMN document_events_ledger.event_date IS 'Date extracted from event_timestamp, used for partitioning';
COMMENT ON COLUMN document_events_ledger.event_type IS 'High-level event category. VIEWED events tracked separately to prevent volume issues.';
COMMENT ON COLUMN document_events_ledger.event_code IS 'Granular event code for detailed classification (e.g., VIEWED_FROM_MOBILE)';
COMMENT ON COLUMN document_events_ledger.event_data IS 'Flexible JSONB metadata. Structure varies by entry_type and source_app.';
COMMENT ON COLUMN document_events_ledger.actor_id IS 'Identifier of who/what performed the action (user, system, service)';
COMMENT ON COLUMN document_events_ledger.actor_type IS 'Type of actor (USER, SYSTEM, SERVICE, API)';
COMMENT ON COLUMN document_events_ledger.storage_index_id IS 'Reference to storage_index table';
COMMENT ON COLUMN document_events_ledger.master_template_id IS 'Reference to master template';
COMMENT ON COLUMN document_events_ledger.template_type IS 'Denormalized template type to avoid joins (e.g., STATEMENT, DISCLOSURE, NOTICE)';
COMMENT ON COLUMN document_events_ledger.document_type IS 'Denormalized document type (e.g., MONTHLY_STATEMENT, ANNUAL_DISCLOSURE). NOT "paperless" - that is an enrollment status.';
COMMENT ON COLUMN document_events_ledger.template_name IS 'Denormalized template name for human-readable reporting (e.g., "Monthly Credit Card Statement", "Annual Privacy Notice")';
COMMENT ON COLUMN document_events_ledger.is_shared_document IS 'TRUE if document is shared across multiple accounts. Shared documents typically have much higher view counts and different analytics patterns.';
COMMENT ON COLUMN document_events_ledger.customer_id IS 'Customer identifier (denormalized from account table). Enables customer-level analytics across multiple accounts. As John noted: "that customer''s got five accounts" - this avoids joins to analyze customer behavior.';
COMMENT ON COLUMN document_events_ledger.account_id IS 'Account identifier (required for analytics). For shared documents, this enables account-level tracking.';
COMMENT ON COLUMN document_events_ledger.source_app IS 'Application/service that created this entry (e.g., PRINT_SERVICE, DOCUMENT_HUB, DELIVERY_HUB)';
COMMENT ON COLUMN document_events_ledger.device_type IS 'Device type if applicable (MOBILE, DESKTOP, TABLET)';
COMMENT ON COLUMN document_events_ledger.location IS 'Geographic location, IP address, or region if applicable';
COMMENT ON COLUMN document_events_ledger.delivery_channel IS 'How document was delivered (e.g., EMAIL, POSTAL_MAIL, ONLINE_PORTAL, VENDOR_HOV, VENDOR_BROADRIDGE). Used for channel effectiveness analytics.';

