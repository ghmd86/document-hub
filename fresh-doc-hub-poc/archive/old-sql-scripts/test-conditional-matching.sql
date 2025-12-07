-- ========================================
-- TEST SCENARIO: Conditional Document Matching
-- Template: TIERED_CARD_AGREEMENT
-- Condition: Based on creditLimit
--   >= 50000 → Platinum_Card_Agreement.pdf
--   >= 25000 → Gold_Card_Agreement.pdf
--   < 25000  → Standard_Card_Agreement.pdf
-- ========================================

-- Delete existing test data if exists
DELETE FROM document_hub.storage_index WHERE template_type = 'TIERED_CARD_AGREEMENT';
DELETE FROM document_hub.master_template_definition WHERE template_type = 'TIERED_CARD_AGREEMENT';

-- ========================================
-- TEMPLATE: Conditional Matching with CUSTOM_RULES
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
    'bbb22222-2222-2222-2222-222222222222'::uuid,
    1,
    'TMPL-COND-001',
    'TIERED_CARD_AGREEMENT',
    'Tiered Card Agreement',
    'Card Agreement',
    'Card agreement document based on credit limit tier (Platinum >= $50k, Gold >= $25k, Standard < $25k)',
    'AGREEMENTS',
    'CREDIT_CARDS',
    'en',
    'LEGAL',
    false,
    true,
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
      "fieldsToExtract": ["creditLimit"],
      "fieldSources": {
        "creditLimit": {
          "description": "Extract credit limit from account credit info API",
          "sourceApi": "creditInfoApi",
          "extractionPath": "$.creditLimit",
          "requiredInputs": ["accountId"],
          "fieldType": "number"
        }
      },
      "dataSources": {
        "creditInfoApi": {
          "description": "Get account credit information including credit limit",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/accounts/${accountId}/credit-info",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["creditLimit"]
        }
      },
      "documentMatching": {
        "matchBy": "conditional",
        "referenceKeyType": "CREDIT_TIER",
        "conditions": [
          {
            "field": "creditLimit",
            "operator": ">=",
            "value": 50000,
            "referenceKey": "TIER-PLATINUM"
          },
          {
            "field": "creditLimit",
            "operator": ">=",
            "value": 25000,
            "referenceKey": "TIER-GOLD"
          },
          {
            "field": "creditLimit",
            "operator": "<",
            "value": 25000,
            "referenceKey": "TIER-STANDARD"
          }
        ]
      },
      "executionStrategy": {
        "mode": "auto",
        "continueOnError": false,
        "timeout": 10000
      }
    }'::jsonb
);

-- ========================================
-- DOCUMENTS: One for each tier
-- ========================================

-- Platinum Tier Document (creditLimit >= 50000)
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
    'ccc33333-3333-3333-3333-333333333333'::uuid,
    'bbb22222-2222-2222-2222-222222222222'::uuid,
    1,
    'TIERED_CARD_AGREEMENT',
    'TIER-PLATINUM',
    'CREDIT_TIER',
    true,
    null,
    null,
    'S3',
    'ddd44444-4444-4444-4444-444444444444'::uuid,
    'Platinum_Card_Agreement.pdf',
    1704067200000,
    true,
    '{"tier": "PLATINUM", "minCreditLimit": 50000, "features": ["unlimited_rewards", "concierge_service", "airport_lounge", "travel_insurance"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Gold Tier Document (creditLimit >= 25000)
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
    'eee55555-5555-5555-5555-555555555555'::uuid,
    'bbb22222-2222-2222-2222-222222222222'::uuid,
    1,
    'TIERED_CARD_AGREEMENT',
    'TIER-GOLD',
    'CREDIT_TIER',
    true,
    null,
    null,
    'S3',
    'fff66666-6666-6666-6666-666666666666'::uuid,
    'Gold_Card_Agreement.pdf',
    1704067200000,
    true,
    '{"tier": "GOLD", "minCreditLimit": 25000, "features": ["rewards_program", "purchase_protection", "extended_warranty"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Standard Tier Document (creditLimit < 25000)
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
    'aaa77777-7777-7777-7777-777777777777'::uuid,
    'bbb22222-2222-2222-2222-222222222222'::uuid,
    1,
    'TIERED_CARD_AGREEMENT',
    'TIER-STANDARD',
    'CREDIT_TIER',
    true,
    null,
    null,
    'S3',
    'bbb88888-8888-8888-8888-888888888888'::uuid,
    'Standard_Card_Agreement.pdf',
    1704067200000,
    true,
    '{"tier": "STANDARD", "minCreditLimit": 0, "features": ["basic_rewards", "fraud_protection"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- ========================================
-- VERIFICATION
-- ========================================
SELECT 'Template created:' as info, template_type, template_name, sharing_scope
FROM document_hub.master_template_definition
WHERE template_type = 'TIERED_CARD_AGREEMENT';

SELECT 'Documents created:' as info, reference_key, file_name
FROM document_hub.storage_index
WHERE template_type = 'TIERED_CARD_AGREEMENT'
ORDER BY reference_key;

-- ========================================
-- TEST ACCOUNTS REFERENCE
-- ========================================
-- Account 1: aaaa0000-0000-0000-0000-000000000001
--   Credit Limit: $75,000 → PLATINUM → Platinum_Card_Agreement.pdf
--
-- Account 2: aaaa0000-0000-0000-0000-000000000002
--   Credit Limit: $35,000 → GOLD → Gold_Card_Agreement.pdf
--
-- Account 3: aaaa0000-0000-0000-0000-000000000003
--   Credit Limit: $15,000 → STANDARD → Standard_Card_Agreement.pdf
