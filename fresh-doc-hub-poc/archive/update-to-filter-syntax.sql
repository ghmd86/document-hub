-- ========================================
-- Update JSONPath to Use Filter Syntax
-- Now that application code unwraps arrays, we can use the filter syntax
-- ========================================

UPDATE document_hub.master_template_definition
SET data_extraction_config = jsonb_set(
    data_extraction_config,
    '{fieldSources,pricingId,extractionPath}',
    '"$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId"'
)
WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
