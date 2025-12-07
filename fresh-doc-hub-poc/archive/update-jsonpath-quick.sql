-- Quick fix: Update just the pricingId extractionPath field
-- This extracts the first element [0] instead of returning an array

UPDATE document_hub.master_template_definition
SET data_extraction_config =
    data_extraction_config::jsonb
    || jsonb_build_object(
        'fieldSources',
        (data_extraction_config->'fieldSources')::jsonb
        || jsonb_build_object(
            'pricingId',
            (data_extraction_config->'fieldSources'->'pricingId')::jsonb
            || jsonb_build_object(
                'extractionPath',
                '$.content[?(@.domain == "PRICING" && @.status == "ACTIVE")][0].domainId'
            )
        )
    )
WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    data_extraction_config->'fieldSources'->'pricingId'->>'extractionPath' as pricingId_path
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
