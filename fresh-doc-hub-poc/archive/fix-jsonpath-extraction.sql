-- Fix JSONPath extraction path for CardholderAgreement template
-- Use filter expression WITHOUT [0] at the end
-- The code already handles array unwrapping for single-element arrays

-- Fix pricingId extraction path
-- FROM: $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId[0]  (returns [])
-- TO:   $.content[?(@.domain == "PRICING" && @.status == "ACTIVE")].domainId     (returns ["PRC-12345"])

UPDATE document_hub.master_template_definition
SET data_extraction_config = jsonb_set(
    data_extraction_config,
    '{fieldSources,pricingId,extractionPath}',
    '"$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId"'::jsonb
)
WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricing_path,
    data_extraction_config->'fieldSources'->'disclosureCode'->>'extractionPath' as disclosure_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
