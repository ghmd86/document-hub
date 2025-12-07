-- ========================================
-- FINAL JSONPath Fix
-- Use filter to get first matching element, then access domainId
-- ==========================================================

-- The correct syntax: Get the first element from content array where domain=PRICING
UPDATE document_hub.master_template_definition
SET data_extraction_config = replace(
    data_extraction_config::text,
    ')].domainId[0]"',
    ' && @.status == \"ACTIVE\")][0].domainId"'
)::jsonb
WHERE template_type = 'CardholderAgreement'
  AND data_extraction_config::text LIKE '%].domainId[0]%';

-- Alternative: If above doesn't work, use this simpler path that assumes PRICING is always present
-- UPDATE document_hub.master_template_definition
-- SET data_extraction_config = jsonb_set(
--     data_extraction_config,
--     '{fieldSources,pricingId,extractionPath}',
--     '"$.content[0].domainId"'
-- )
-- WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
