-- ========================================
-- DISCLOSURE CODE EXTRACTION EXAMPLE
-- Based on document-hub-service Example 1
-- ========================================
-- This example demonstrates:
-- 1. Call arrangements API to get pricingId
-- 2. Use pricingId to call pricing API to get disclosureCode
-- 3. Match documents using disclosureCode as reference_key

-- ========================================
-- TEST SCENARIO 4: Disclosure Code Extraction (2-Step Chain)
-- Template: CardholderAgreement
-- Chain: Arrangements API → Pricing API → Match by disclosure_code
-- ========================================

INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    display_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    regulatory_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    record_status,
    data_extraction_config
) VALUES (
    '44444444-4444-4444-4444-444444444444'::uuid,
    1,
    'CardholderAgreement',
    'Card holder Agreement',
    'Credit Card T&C',
    'Terms and conditions document matched by disclosure code',
    'REGULATORY',
    'CREDIT_CARDS',
    'en',
    true,
    true,
    true,
    'CUSTOM_RULES',
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1',
    '{
      "requiredFields": ["pricingId", "disclosureCode"],
      "fieldSources": {
        "pricingId": {
          "description": "Pricing ID from account arrangements (active PRICING domain)",
          "sourceApi": "accountArrangementsApi",
          "extractionPath": "$.content[0].domainId",
          "requiredInputs": ["accountId"],
          "fieldType": "string",
          "defaultValue": null
        },
        "disclosureCode": {
          "description": "Disclosure code from pricing service (cardholder agreements TnC code)",
          "sourceApi": "pricingApi",
          "extractionPath": "$.cardholderAgreementsTncCode",
          "requiredInputs": ["pricingId"],
          "fieldType": "string",
          "defaultValue": null
        }
      },
      "dataSources": {
        "accountArrangementsApi": {
          "description": "Step 1: Get account arrangements to extract pricingId",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/creditcard/accounts/${accountId}/arrangements",
            "method": "GET",
            "timeout": 5000,
            "headers": {
              "Authorization": "Bearer ${auth.token}",
              "X-Request-ID": "${correlationId}"
            }
          },
          "cache": {
            "enabled": true,
            "ttlSeconds": 1800,
            "keyPattern": "arrangements:${accountId}"
          },
          "retry": {
            "maxAttempts": 3,
            "delayMs": 100
          },
          "providesFields": ["pricingId"]
        },
        "pricingApi": {
          "description": "Step 2: Get pricing data to extract disclosure code",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/pricing-service/prices/${pricingId}",
            "method": "GET",
            "timeout": 5000,
            "headers": {
              "Authorization": "Bearer ${auth.token}",
              "X-Request-ID": "${correlationId}"
            }
          },
          "cache": {
            "enabled": true,
            "ttlSeconds": 3600,
            "keyPattern": "pricing:${pricingId}"
          },
          "retry": {
            "maxAttempts": 2,
            "delayMs": 100
          },
          "providesFields": ["disclosureCode"]
        }
      },
      "executionStrategy": {
        "mode": "auto",
        "continueOnError": false,
        "timeout": 15000
      },
      "documentMatching": {
        "matchBy": "reference_key",
        "referenceKeyField": "disclosureCode",
        "referenceKeyType": "DISCLOSURE_CODE"
      }
    }'::jsonb
);

-- ========================================
-- DOCUMENTS for Disclosure Code Matching
-- ========================================

-- Document 1: Terms & Conditions for disclosure code D164
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
    '44444444-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    '44444444-4444-4444-4444-444444444444'::uuid,
    1,
    'CardholderAgreement',
    'D164',  -- This is the disclosure code
    'DISCLOSURE_CODE',
    true,
    null,
    null,
    'S3',
    '44444444-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid,
    'Credit_Card_Terms_D164_v1.pdf',
    1704067200000,
    true,
    '{"disclosureCode": "D164", "version": "1.0", "effectiveDate": "2024-01-01", "documentType": "CARDHOLDER_AGREEMENT"}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Document 2: Terms & Conditions for disclosure code D165
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
    '44444444-aaaa-aaaa-aaaa-bbbbbbbbbbbb'::uuid,
    '44444444-4444-4444-4444-444444444444'::uuid,
    1,
    'CardholderAgreement',
    'D165',  -- Different disclosure code
    'DISCLOSURE_CODE',
    true,
    null,
    null,
    'S3',
    '44444444-cccc-cccc-cccc-cccccccccccc'::uuid,
    'Credit_Card_Terms_D165_v1.pdf',
    1704067200000,
    true,
    '{"disclosureCode": "D165", "version": "1.0", "effectiveDate": "2024-01-01", "documentType": "CARDHOLDER_AGREEMENT"}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Document 3: Terms & Conditions for disclosure code D166 (Premium cards)
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
    '44444444-aaaa-aaaa-aaaa-cccccccccccc'::uuid,
    '44444444-4444-4444-4444-444444444444'::uuid,
    1,
    'CardholderAgreement',
    'D166',  -- Premium card disclosure code
    'DISCLOSURE_CODE',
    true,
    null,
    null,
    'S3',
    '44444444-dddd-dddd-dddd-dddddddddddd'::uuid,
    'Premium_Credit_Card_Terms_D166_v1.pdf',
    1704067200000,
    true,
    '{"disclosureCode": "D166", "version": "1.0", "effectiveDate": "2024-01-01", "documentType": "CARDHOLDER_AGREEMENT", "cardTier": "PREMIUM"}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- ========================================
-- VERIFICATION QUERIES
-- ========================================

-- Verify template loaded
SELECT
    template_type,
    template_name,
    shared_document_flag,
    sharing_scope,
    jsonb_array_length(data_extraction_config->'requiredFields') as required_fields_count
FROM master_template_definition
WHERE template_type = 'CardholderAgreement';

-- Verify documents loaded with disclosure codes
SELECT
    template_type,
    reference_key as disclosure_code,
    reference_key_type,
    file_name,
    shared_flag,
    doc_metadata->>'cardTier' as card_tier
FROM storage_index
WHERE template_type = 'CardholderAgreement'
ORDER BY reference_key;

-- View extraction config for disclosure code template
SELECT
    template_type,
    jsonb_pretty(data_extraction_config)
FROM master_template_definition
WHERE template_type = 'CardholderAgreement';

-- ========================================
-- TEST ACCOUNT IDS FOR DISCLOSURE CODE SCENARIO
-- ========================================
-- Use these IDs for testing:
-- Account ID: 550e8400-e29b-41d4-a716-446655440000
--   → arrangements returns pricingId: "PRC-12345"
--   → pricing returns disclosureCode: "D164"
--   → should match: Credit_Card_Terms_D164_v1.pdf
--
-- Account ID: 770e8400-e29b-41d4-a716-446655440002
--   → arrangements returns pricingId: "PRC-67890"
--   → pricing returns disclosureCode: "D166"
--   → should match: Premium_Credit_Card_Terms_D166_v1.pdf
