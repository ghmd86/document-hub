-- ========================================
-- TEST SCENARIO 1: Simple 3-Step Chain
-- Template: ACCOUNT_STATEMENT_SHARED
-- Chain: accountArrangementsApi → pricingApi → statementApi
-- Document Matching: by statementCode
-- ========================================

-- Delete existing test data if exists
DELETE FROM document_hub.storage_index WHERE template_type = 'ACCOUNT_STATEMENT_SHARED';
DELETE FROM document_hub.master_template_definition WHERE template_type = 'ACCOUNT_STATEMENT_SHARED';

-- ========================================
-- TEMPLATE: 3-Step Chain with CUSTOM_RULES
-- ========================================

INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    legacy_template_id,
    template_type,
    template_name,
    display_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    owning_dept,
    notification_needed,
    active_flag,
    regulatory_flag,
    message_center_doc_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    record_status,
    data_extraction_config
) VALUES (
    'aaa11111-1111-1111-1111-111111111111'::uuid,
    1,
    'TMPL-3STEP-001',
    'ACCOUNT_STATEMENT_SHARED',
    'Shared Account Statement',
    'Account Statement',
    '3-Step Chain: accountArrangementsApi → pricingApi → statementApi',
    'STATEMENTS',
    'RETAIL_BANKING',
    'en',
    'OPERATIONS',
    false,
    true,
    false,
    true,
    true,
    'CUSTOM_RULES',
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1',
    '{
      "requiredFields": ["pricingId", "statementCode"],
      "fieldSources": {
        "pricingId": {
          "description": "Step 1: Extract pricingId from arrangements API",
          "sourceApi": "accountArrangementsApi",
          "extractionPath": "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId",
          "requiredInputs": ["accountId"],
          "fieldType": "string"
        },
        "statementCode": {
          "description": "Step 2: Extract statementCode from statement API using pricingId",
          "sourceApi": "statementApi",
          "extractionPath": "$.statementCode",
          "requiredInputs": ["pricingId"],
          "fieldType": "string"
        }
      },
      "dataSources": {
        "accountArrangementsApi": {
          "description": "Step 1: Get account arrangements to extract pricingId",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/creditcard/accounts/${accountId}/arrangements",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["pricingId"]
        },
        "statementApi": {
          "description": "Step 2: Get statement config using pricingId to get statementCode",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/statement-service/statements/${pricingId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["statementCode"]
        }
      },
      "documentMatching": {
        "matchBy": "reference_key",
        "referenceKeyField": "statementCode",
        "referenceKeyType": "STATEMENT_CODE"
      },
      "executionStrategy": {
        "mode": "auto",
        "continueOnError": false,
        "timeout": 15000
      }
    }'::jsonb
);

-- ========================================
-- DOCUMENTS: Matched by statementCode
-- ========================================

-- VIP Statement Document (matches STMT-VIP-2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    account_key,
    customer_key,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp,
    record_status
) VALUES (
    '55555555-5555-5555-5555-555555555555'::uuid,
    'aaa11111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT_SHARED',
    'STMT-VIP-2024',
    'STATEMENT_CODE',
    true,
    null,
    null,
    'S3',
    '66666666-6666-6666-6666-666666666666'::uuid,
    'VIP_Monthly_Statement_Template_2024.pdf',
    1704067200000,
    true,
    '{"statementType": "MONTHLY_DETAILED", "tier": "VIP", "features": ["transaction_details", "rewards_summary", "investment_snapshot"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Standard Statement Document (matches STMT-STD-2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    account_key,
    customer_key,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp,
    record_status
) VALUES (
    '77777777-7777-7777-7777-777777777777'::uuid,
    'aaa11111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT_SHARED',
    'STMT-STD-2024',
    'STATEMENT_CODE',
    true,
    null,
    null,
    'S3',
    '88888888-8888-8888-8888-888888888888'::uuid,
    'Standard_Monthly_Statement_2024.pdf',
    1704067200000,
    true,
    '{"statementType": "MONTHLY_SUMMARY", "tier": "STANDARD", "features": ["transaction_summary", "balance_overview"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Basic Statement Document (matches STMT-BASIC-2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    account_key,
    customer_key,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp,
    record_status
) VALUES (
    '99999999-9999-9999-9999-999999999999'::uuid,
    'aaa11111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT_SHARED',
    'STMT-BASIC-2024',
    'STATEMENT_CODE',
    true,
    null,
    null,
    'S3',
    'aaaaaaaa-1111-2222-3333-444444444444'::uuid,
    'Basic_Monthly_Statement_2024.pdf',
    1704067200000,
    true,
    '{"statementType": "MONTHLY_BASIC", "tier": "BASIC", "features": ["balance_only"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- ========================================
-- VERIFICATION
-- ========================================
SELECT 'Template created:' as info, template_type, template_name, sharing_scope
FROM document_hub.master_template_definition
WHERE template_type = 'ACCOUNT_STATEMENT_SHARED';

SELECT 'Documents created:' as info, reference_key, file_name
FROM document_hub.storage_index
WHERE template_type = 'ACCOUNT_STATEMENT_SHARED';
