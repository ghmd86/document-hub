-- ========================================
-- CORRECTED JSONPath Fix
-- Move [0] to AFTER .domainId instead of before it
-- ========================================

-- The issue: $.content[?(@.domain == "PRICING")][0].domainId returns []
-- The fix: $.content[?(@.domain == "PRICING")].domainId[0] returns "PRC-12345"

UPDATE document_hub.master_template_definition
SET data_extraction_config = replace(
    data_extraction_config::text,
    ')][0].domainId"',
    ')].domainId[0]"'
)::jsonb
WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
