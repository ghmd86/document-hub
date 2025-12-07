-- Fix JSONPath to extract first element instead of array
-- The issue: JSONPath $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId returns ["PRC-12345"]
-- We need: $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId[0] to return "PRC-12345"

UPDATE document_hub.master_template_definition
SET data_extraction_config = jsonb_set(
    data_extraction_config,
    '{fieldsToExtract,pricingId,extractionPath}',
    '"$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")][0].domainId"'
)
WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    data_extraction_config->'fieldsToExtract'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
