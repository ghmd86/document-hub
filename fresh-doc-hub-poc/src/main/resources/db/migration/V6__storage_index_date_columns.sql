-- ====================================================================
-- Document Hub - Storage Index Date Columns Migration
-- ====================================================================
-- Version: V6
-- Date: December 2024
-- Description: Adds valid_from and valid_until columns to storage_index
--              for better query performance on date range filters
-- Rationale: Separate columns enable B-tree indexing, faster range
--            queries, and better query planner statistics compared
--            to extracting dates from JSONB doc_metadata
-- ====================================================================

-- ====================================================================
-- Add valid_from and valid_until columns
-- ====================================================================
-- Using BIGINT (epoch milliseconds) for consistency with doc_creation_date
-- NULL allowed for backwards compatibility with existing documents

ALTER TABLE document_hub.storage_index
ADD COLUMN IF NOT EXISTS valid_from BIGINT;

ALTER TABLE document_hub.storage_index
ADD COLUMN IF NOT EXISTS valid_until BIGINT;

COMMENT ON COLUMN document_hub.storage_index.valid_from IS
'Document validity start date in epoch milliseconds. For query performance over doc_metadata JSON extraction.';

COMMENT ON COLUMN document_hub.storage_index.valid_until IS
'Document validity end date / expiration in epoch milliseconds. For query performance over doc_metadata JSON extraction.';


-- ====================================================================
-- Create indexes for date range queries
-- ====================================================================

-- Individual indexes for single-column filters
CREATE INDEX IF NOT EXISTS idx_storage_index_valid_from
ON document_hub.storage_index(valid_from);

CREATE INDEX IF NOT EXISTS idx_storage_index_valid_until
ON document_hub.storage_index(valid_until);

-- Composite index for range queries (WHERE valid_from >= ? AND valid_until <= ?)
CREATE INDEX IF NOT EXISTS idx_storage_index_validity_range
ON document_hub.storage_index(valid_from, valid_until);

-- Composite index for common query pattern: template + validity range
CREATE INDEX IF NOT EXISTS idx_storage_index_template_validity
ON document_hub.storage_index(master_template_id, valid_from, valid_until);


-- ====================================================================
-- Backfill existing data from doc_metadata
-- ====================================================================
-- Extract valid_from and valid_until from doc_metadata JSON
-- Convert date string (YYYY-MM-DD) to epoch milliseconds

UPDATE document_hub.storage_index
SET valid_from = EXTRACT(EPOCH FROM (doc_metadata->>'valid_from')::DATE)::BIGINT * 1000
WHERE valid_from IS NULL
  AND doc_metadata IS NOT NULL
  AND doc_metadata->>'valid_from' IS NOT NULL
  AND doc_metadata->>'valid_from' != '';

UPDATE document_hub.storage_index
SET valid_until = EXTRACT(EPOCH FROM (doc_metadata->>'valid_until')::DATE)::BIGINT * 1000
WHERE valid_until IS NULL
  AND doc_metadata IS NOT NULL
  AND doc_metadata->>'valid_until' IS NOT NULL
  AND doc_metadata->>'valid_until' != '';


-- ====================================================================
-- Verification Queries (run manually to verify migration)
-- ====================================================================
-- SELECT column_name, data_type, column_default, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'document_hub'
-- AND table_name = 'storage_index'
-- AND column_name IN ('valid_from', 'valid_until');

-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE schemaname = 'document_hub'
-- AND indexname LIKE '%valid%';

-- SELECT COUNT(*),
--        COUNT(valid_from) as has_valid_from,
--        COUNT(valid_until) as has_valid_until
-- FROM document_hub.storage_index;


-- ====================================================================
-- Migration Complete
-- ====================================================================
-- New columns added:
--   - storage_index.valid_from (BIGINT epoch ms, NULL allowed)
--   - storage_index.valid_until (BIGINT epoch ms, NULL allowed)
--
-- Indexes added:
--   - idx_storage_index_valid_from
--   - idx_storage_index_valid_until
--   - idx_storage_index_validity_range (composite)
--   - idx_storage_index_template_validity (composite)
--
-- Existing data backfilled from doc_metadata.valid_from/valid_until
-- ====================================================================
