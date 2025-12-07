-- Quick fix for credit-info API URL
UPDATE document_hub.master_template_definition
SET data_extraction_config = jsonb_set(
    data_extraction_config,
    '{dataSources,creditInfoApi,endpoint,url}',
    '"http://localhost:8080/api/v1/mock-api/accounts/${accountId}/credit-info"'::jsonb
)
WHERE template_type = 'TIERED_CARD_AGREEMENT';

-- Verify the update
SELECT data_extraction_config->'dataSources'->'creditInfoApi'->'endpoint'->>'url' as url
FROM document_hub.master_template_definition
WHERE template_type = 'TIERED_CARD_AGREEMENT';
