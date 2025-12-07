-- ========================================
-- Comprehensive Fix Script
-- Fixes both schema issues and JSONPath configuration
-- ========================================

-- PART 1: Fix Schema - Convert BIT columns to BOOLEAN
-- ========================================

-- Fix master_template_definition table
ALTER TABLE document_hub.master_template_definition
  ALTER COLUMN active_flag TYPE BOOLEAN USING (active_flag::int::boolean);

ALTER TABLE document_hub.master_template_definition
  ALTER COLUMN shared_document_flag TYPE BOOLEAN USING (shared_document_flag::int::boolean);

-- Fix storage_index table
ALTER TABLE document_hub.storage_index
  ALTER COLUMN shared_document_flag TYPE BOOLEAN USING (shared_document_flag::int::boolean);

-- PART 2: Fix JSONPath - Extract first element instead of array
-- ========================================

UPDATE document_hub.master_template_definition
SET data_extraction_config = replace(
    data_extraction_config::text,
    '").domainId"',
    '")[0].domainId"'
)::jsonb
WHERE template_type = 'CardholderAgreement';

-- PART 3: Verification Queries
-- ========================================

-- Verify schema changes
SELECT
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'document_hub'
  AND table_name = 'master_template_definition'
  AND column_name IN ('active_flag', 'shared_document_flag');

-- Verify JSONPath fix
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';

-- Verify template can be queried
SELECT
    template_type,
    template_name,
    active_flag,
    shared_document_flag
FROM document_hub.master_template_definition
WHERE active_flag = true
  AND (start_date IS NULL OR start_date <= EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::bigint * 1000);

-- Count templates
SELECT COUNT(*) as total_templates FROM document_hub.master_template_definition;
