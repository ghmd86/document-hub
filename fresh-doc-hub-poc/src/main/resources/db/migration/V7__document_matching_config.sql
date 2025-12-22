-- ====================================================================
-- Document Hub - Add document_matching_config Column
-- ====================================================================
-- Version: V7
-- Date: December 2024
-- Description: Adds separate column for document matching configuration
--              Previously stored under data_extraction_config.documentMatching
-- ====================================================================

-- ====================================================================
-- Add document_matching_config column to master_template_definition
-- ====================================================================
-- Purpose: Separate document matching logic from data extraction config
-- for better separation of concerns and cleaner configuration management.
--
-- This column stores the configuration for how to match/query documents
-- based on extracted data. Supports two matching strategies:
--   1. reference_key: Match by a field value from extracted data
--   2. conditional: Evaluate conditions to determine the reference key
--
-- Example (reference_key matching):
-- {
--   "matchBy": "reference_key",
--   "referenceKeyField": "disclosureCode",
--   "referenceKeyType": "DISCLOSURE_CODE"
-- }
--
-- Example (conditional matching):
-- {
--   "matchBy": "conditional",
--   "referenceKeyType": "CREDIT_TIER",
--   "conditions": [
--     {"field": "creditScore", "operator": ">=", "value": 700, "referenceKey": "TIER_1"},
--     {"field": "creditScore", "operator": ">=", "value": 600, "referenceKey": "TIER_2"}
--   ]
-- }

ALTER TABLE document_hub.master_template_definition
ADD COLUMN IF NOT EXISTS document_matching_config JSONB;

COMMENT ON COLUMN document_hub.master_template_definition.document_matching_config IS
'Configuration for document matching/querying logic. Defines how to match documents based on extracted data. Supports reference_key and conditional matching strategies.';


-- ====================================================================
-- Migrate existing documentMatching from data_extraction_config
-- ====================================================================
-- Extracts the documentMatching key from data_extraction_config and
-- moves it to the new document_matching_config column.

UPDATE document_hub.master_template_definition
SET document_matching_config = data_extraction_config->'documentMatching'
WHERE data_extraction_config IS NOT NULL
  AND data_extraction_config ? 'documentMatching'
  AND document_matching_config IS NULL;


-- ====================================================================
-- Remove documentMatching from data_extraction_config
-- ====================================================================
-- After migration, remove the documentMatching key from data_extraction_config
-- to avoid duplication.

UPDATE document_hub.master_template_definition
SET data_extraction_config = data_extraction_config - 'documentMatching'
WHERE data_extraction_config IS NOT NULL
  AND data_extraction_config ? 'documentMatching';


-- ====================================================================
-- Verification Queries (run manually to verify migration)
-- ====================================================================
-- Check new column exists:
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'document_hub'
-- AND table_name = 'master_template_definition'
-- AND column_name = 'document_matching_config';

-- Check migration results:
-- SELECT master_template_id, template_type,
--        document_matching_config IS NOT NULL as has_matching_config,
--        data_extraction_config ? 'documentMatching' as still_has_old
-- FROM document_hub.master_template_definition
-- WHERE data_extraction_config IS NOT NULL;


-- ====================================================================
-- Migration Complete
-- ====================================================================
-- New column added:
--   - master_template_definition.document_matching_config (JSONB)
--
-- Data migrated from:
--   - data_extraction_config.documentMatching -> document_matching_config
-- ====================================================================
