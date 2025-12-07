-- Fix API endpoint paths to include /api/v1 context path
-- Run this script to update the data extraction config URLs

UPDATE document_hub.master_template_definition
SET data_extraction_config = REPLACE(
    data_extraction_config::text,
    'http://localhost:8080/mock-api/',
    'http://localhost:8080/api/v1/mock-api/'
)::jsonb
WHERE template_type = 'CardholderAgreement';

-- Verify the update
SELECT
    template_type,
    template_name,
    data_extraction_config->'dataSources'->'accountArrangementsApi'->'endpoint'->>'url' as arrangements_url,
    data_extraction_config->'dataSources'->'pricingApi'->'endpoint'->>'url' as pricing_url
FROM document_hub.master_template_definition
WHERE template_type = 'CardholderAgreement';
