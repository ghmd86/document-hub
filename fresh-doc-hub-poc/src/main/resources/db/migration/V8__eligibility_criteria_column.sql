-- ====================================================================
-- Document Hub - Add eligibility_criteria Column
-- ====================================================================
-- Version: V8
-- Date: December 2024
-- Description: Adds separate column for eligibility criteria
--              Previously stored under template_config.eligibility_criteria
-- ====================================================================

-- ====================================================================
-- Add eligibility_criteria column to master_template_definition
-- ====================================================================
-- Purpose: Separate eligibility rules from template configuration
-- for better separation of concerns:
--   - template_config: print/reprint policies (HOW documents are handled)
--   - eligibility_criteria: access rules (WHO can see documents)
--   - document_matching_config: query logic (WHICH documents to return)
--   - data_extraction_config: data extraction (WHAT data to fetch)
--
-- Example:
-- {
--   "operator": "AND",
--   "rules": [
--     {"field": "customerSegment", "operator": "EQUALS", "value": "VIP"},
--     {"field": "region", "operator": "IN", "value": ["US_WEST", "US_EAST"]}
--   ]
-- }
--
-- Supported logical operators: AND, OR
-- Supported rule operators: EQUALS, NOT_EQUALS, IN, NOT_IN, GREATER_THAN,
--   LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, CONTAINS,
--   STARTS_WITH, ENDS_WITH

ALTER TABLE document_hub.master_template_definition
ADD COLUMN IF NOT EXISTS eligibility_criteria JSONB;

COMMENT ON COLUMN document_hub.master_template_definition.eligibility_criteria IS
'Eligibility criteria for document access. Defines rules that determine if a user/account can access documents of this template. Supports AND/OR logical operators with various rule operators.';


-- ====================================================================
-- Migrate existing eligibility_criteria from template_config
-- ====================================================================
-- Extracts the eligibility_criteria key from template_config and
-- moves it to the new eligibility_criteria column.

UPDATE document_hub.master_template_definition
SET eligibility_criteria = template_config->'eligibility_criteria'
WHERE template_config IS NOT NULL
  AND template_config ? 'eligibility_criteria'
  AND eligibility_criteria IS NULL;


-- ====================================================================
-- Remove eligibility_criteria from template_config
-- ====================================================================
-- After migration, remove the eligibility_criteria key from template_config
-- to avoid duplication.

UPDATE document_hub.master_template_definition
SET template_config = template_config - 'eligibility_criteria'
WHERE template_config IS NOT NULL
  AND template_config ? 'eligibility_criteria';


-- ====================================================================
-- Verification Queries (run manually to verify migration)
-- ====================================================================
-- Check new column exists:
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'document_hub'
-- AND table_name = 'master_template_definition'
-- AND column_name = 'eligibility_criteria';

-- Check migration results:
-- SELECT master_template_id, template_type,
--        eligibility_criteria IS NOT NULL as has_eligibility,
--        template_config ? 'eligibility_criteria' as still_has_old
-- FROM document_hub.master_template_definition
-- WHERE template_config IS NOT NULL;


-- ====================================================================
-- Migration Complete
-- ====================================================================
-- New column added:
--   - master_template_definition.eligibility_criteria (JSONB)
--
-- Data migrated from:
--   - template_config.eligibility_criteria -> eligibility_criteria
-- ====================================================================
