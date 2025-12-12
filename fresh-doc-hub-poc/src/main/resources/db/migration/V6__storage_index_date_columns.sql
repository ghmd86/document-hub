-- ====================================================================
-- Document Hub - Storage Index Date Columns Migration
-- ====================================================================
-- Version: V6
-- Date: December 2024
-- Description: Adds start_date and end_date columns to storage_index
--              for better query performance on date range filters
-- Rationale: Separate columns enable B-tree indexing, faster range
--            queries, and better query planner statistics compared
--            to extracting dates from JSONB doc_metadata
-- ====================================================================

-- ====================================================================
-- Add start_date and end_date columns
-- ====================================================================
-- Using BIGINT (epoch milliseconds) for consistency with doc_creation_date
-- NULL allowed for backwards compatibility with existing documents

ALTER TABLE document_hub.storage_index
ADD COLUMN IF NOT EXISTS start_date BIGINT;

ALTER TABLE document_hub.storage_index
ADD COLUMN IF NOT EXISTS end_date BIGINT;

COMMENT ON COLUMN document_hub.storage_index.start_date IS
'Document validity start date in epoch milliseconds. For query performance over doc_metadata JSON extraction.';

COMMENT ON COLUMN document_hub.storage_index.end_date IS
'Document validity end date / expiration in epoch milliseconds. For query performance over doc_metadata JSON extraction.';


-- ====================================================================
-- Create indexes for date range queries
-- ====================================================================

-- Individual indexes for single-column filters
CREATE INDEX IF NOT EXISTS idx_storage_index_start_date
ON document_hub.storage_index(start_date);

CREATE INDEX IF NOT EXISTS idx_storage_index_end_date
ON document_hub.storage_index(end_date);

-- Composite index for range queries (WHERE start_date >= ? AND end_date <= ?)
CREATE INDEX IF NOT EXISTS idx_storage_index_date_range
ON document_hub.storage_index(start_date, end_date);

-- Composite index for common query pattern: template + date range
CREATE INDEX IF NOT EXISTS idx_storage_index_template_dates
ON document_hub.storage_index(master_template_id, start_date, end_date);


-- ====================================================================
-- Backfill existing data from doc_metadata
-- ====================================================================
-- Extract start_date and end_date from doc_metadata JSON
-- Convert date string (YYYY-MM-DD) to epoch milliseconds

UPDATE document_hub.storage_index
SET start_date = EXTRACT(EPOCH FROM (doc_metadata->>'start_date')::DATE)::BIGINT * 1000
WHERE start_date IS NULL
  AND doc_metadata IS NOT NULL
  AND doc_metadata->>'start_date' IS NOT NULL
  AND doc_metadata->>'start_date' != '';

UPDATE document_hub.storage_index
SET end_date = EXTRACT(EPOCH FROM (doc_metadata->>'end_date')::DATE)::BIGINT * 1000
WHERE end_date IS NULL
  AND doc_metadata IS NOT NULL
  AND doc_metadata->>'end_date' IS NOT NULL
  AND doc_metadata->>'end_date' != '';


-- ====================================================================
-- Verification Queries (run manually to verify migration)
-- ====================================================================
-- SELECT column_name, data_type, column_default, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'document_hub'
-- AND table_name = 'storage_index'
-- AND column_name IN ('start_date', 'end_date');

-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE schemaname = 'document_hub'
-- AND indexname LIKE '%date%';

-- SELECT COUNT(*),
--        COUNT(start_date) as has_start_date,
--        COUNT(end_date) as has_end_date
-- FROM document_hub.storage_index;


-- ====================================================================
-- Migration Complete
-- ====================================================================
-- New columns added:
--   - storage_index.start_date (BIGINT epoch ms, NULL allowed)
--   - storage_index.end_date (BIGINT epoch ms, NULL allowed)
--
-- Indexes added:
--   - idx_storage_index_start_date
--   - idx_storage_index_end_date
--   - idx_storage_index_date_range (composite)
--   - idx_storage_index_template_dates (composite)
--
-- Existing data backfilled from doc_metadata.start_date/end_date
-- ====================================================================
