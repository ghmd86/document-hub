
-- ====================================================================
-- Document Hub POC - Sample Data
-- ====================================================================
-- This script inserts sample data for testing different sharing scenarios
-- ====================================================================

-- Clean up existing data (H2 compatible)
-- Truncate table document_hub.storage_index;
-- Truncate table document_hub.master_template_definition CASCADE;

-- ====================================================================
-- MASTER TEMPLATE DEFINITIONS
-- ====================================================================

-- Template 1: Privacy Policy (Shared with ALL)
-- No eligibility criteria needed - available to everyone
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    1,
    'Privacy Policy 2024',
    '2024 Privacy Policy - Shared with all customers',
    'Privacy',
    'ALL',
    'PrivacyPolicy',
    'EN_US',
    true,
    true,
    'ALL',
    1704067200000,
    'system',
    NOW()
);

-- Template 2: Credit Card Statement (Account-Specific)
-- No eligibility criteria - matched by account_key in document_hub.storage_index
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    1,
    'Credit Card Statement',
    'Monthly credit card statement',
    'Statement',
    'CREDIT_CARD',
    'Statement',
    'EN_US',
    true,
    false,
    NULL,
    1704067200000,
    'system',
    NOW()
);

-- Template 3: Balance Transfer Offer (Shared by Account Type - Credit Card)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    1,
    'Balance Transfer Offer',
    'Special balance transfer offer for credit card customers',
    'Promotional',
    'CREDIT_CARD',
    'BalanceTransferLetter',
    'EN_US',
    true,
    true,
    'ACCOUNT_TYPE',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "IN", "value": ["credit_card"]}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Template 4: VIP Customer Offer (Shared with Custom Rules - VIP + US_WEST)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '44444444-4444-4444-4444-444444444444',
    1,
    'VIP Exclusive Offer',
    'Exclusive offer for VIP customers in US West region',
    'Promotional',
    'ALL',
    'CustomerLetter',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "customerSegment", "operator": "EQUALS", "value": "VIP"}, {"field": "region", "operator": "EQUALS", "value": "US_WEST"}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Template 5: Regulatory Disclosure (Shared with Custom Rules - Reference Key Match)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '55555555-5555-5555-5555-555555555555',
    1,
    'Regulatory Disclosure D164',
    'Regulatory disclosure document',
    'Disclosure',
    'ALL',
    'ElectronicDisclosure',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "$metadata.disclosure_code", "operator": "EQUALS", "value": "D164"}]}}',
    1704067200000,
    'system',
    NOW()
);

-- ====================================================================
-- STORAGE INDEX (DOCUMENTS)
-- ====================================================================

-- Document 1: Privacy Policy (Shared with ALL)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000001',
    'privacy_policy_2024.pdf',
    1705276800000,
    true,
    '{"version": "2024.1", "valid_from": "2024-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 2: Statement for Account 1 (Account-Specific)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    '22222222-2222-2222-2222-222222222222',
    1,
    'Statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000002',
    'statement_jan_2024.pdf',
    1706659200000,
    true,
    '{"statement_date": "2024-01-31", "balance": 1234.56, "valid_from": "2024-01-31", "valid_until": "2027-01-31"}',
    'system',
    NOW()
);

-- Document 3: Statement for Account 2 (Account-Specific)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000003',
    '22222222-2222-2222-2222-222222222222',
    1,
    'Statement',
    false,
    'aaaa0000-0000-0000-0000-000000000002',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000003',
    'statement_jan_2024_acct2.pdf',
    1706659200000,
    true,
    '{"statement_date": "2024-01-31", "balance": 5678.90, "valid_from": "2024-01-31", "valid_until": "2027-01-31"}',
    'system',
    NOW()
);

-- Document 4: Balance Transfer Offer (Shared by Account Type)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000004',
    '33333333-3333-3333-3333-333333333333',
    1,
    'BalanceTransferLetter',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000004',
    'balance_transfer_offer_q1_2024.pdf',
    1706745600000,
    true,
    '{"offer_code": "BT2024Q1", "apr_rate": 0.00, "valid_from": "2024-01-01", "valid_until": "2024-06-30"}',
    'system',
    NOW()
);

-- Document 5: VIP Exclusive Offer (Shared with Custom Rules)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000005',
    '44444444-4444-4444-4444-444444444444',
    1,
    'CustomerLetter',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000005',
    'vip_exclusive_offer.pdf',
    1707955200000,
    true,
    '{"segment": "VIP", "region": "US_WEST", "offer_type": "exclusive", "valid_from": "2024-02-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 6: Regulatory Disclosure (Shared with Custom Rules)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    reference_key,
    reference_key_type,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000006',
    '55555555-5555-5555-5555-555555555555',
    1,
    'ElectronicDisclosure',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000006',
    'disclosure_d164.pdf',
    'D164',
    'DISCLOSURE_CODE',
    1704844800000,
    true,
    '{"disclosure_code": "D164", "regulatory_type": "ESIGN", "version": "1.0", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 6: CREDIT CARD TERMS & CONDITIONS (with Data Extraction)
-- ====================================================================
-- This template demonstrates a 2-step data extraction chain:
-- Step 1: Call arrangements API to get pricingId
-- Step 2: Use pricingId to call pricing API to get disclosureCode
-- Step 3: Match documents using disclosureCode as reference_key
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp,
    data_extraction_config
) VALUES (
    '66666666-6666-6666-6666-666666666666',
    1,
    'Credit Card Terms and Conditions',
    'Terms and conditions document matched by disclosure code',
    'Regulatory',
    'CREDIT_CARD',
    'CardholderAgreement',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{"eligibility_criteria": {"operator": "AND", "rules": []}}',
    1609459200000,
    'system',
    NOW(),
    '{
      "requiredFields": ["pricingId", "disclosureCode"],
      "fieldSources": {
        "pricingId": {
          "description": "Pricing ID from account arrangements (active PRICING domain)",
          "sourceApi": "accountArrangementsApi",
          "extractionPath": "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId",
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
    }'
);

-- ====================================================================
-- DOCUMENTS for Credit Card Terms & Conditions (Disclosure Code Matching)
-- ====================================================================

-- Document 7: Terms & Conditions for disclosure code D164
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000007',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    'D164',
    'DISCLOSURE_CODE',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000007',
    'Credit_Card_Terms_D164_v1.pdf',
    1704067200000,
    true,
    '{"disclosureCode": "D164", "version": "1.0", "documentType": "CARDHOLDER_AGREEMENT", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Document 8: Terms & Conditions for disclosure code D165
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000008',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    'D165',
    'DISCLOSURE_CODE',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000008',
    'Credit_Card_Terms_D165_v1.pdf',
    1704067200000,
    true,
    '{"disclosureCode": "D165", "version": "1.0", "documentType": "CARDHOLDER_AGREEMENT", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Document 9: Terms & Conditions for disclosure code D166 (Premium cards)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000009',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    'D166',
    'DISCLOSURE_CODE',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000009',
    'Premium_Credit_Card_Terms_D166_v1.pdf',
    1704067200000,
    true,
    '{"disclosureCode": "D166", "version": "1.0", "documentType": "CARDHOLDER_AGREEMENT", "cardTier": "PREMIUM", "valid_from": "2024-01-01", "valid_until": "2024-06-30"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 7: ZIPCODE-BASED PROMOTIONAL OFFER
-- ====================================================================
-- This template demonstrates:
-- 1. data_extraction_config: Fetch zipcode from Customer API
-- 2. template_config: Eligibility check - only customers in specific zipcodes
-- 3. Shared document visible only to eligible customers
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    access_control,
    template_config,
    start_date,
    created_by,
    created_timestamp,
    data_extraction_config
) VALUES (
    '77777777-7777-7777-7777-777777777777',
    1,
    'Bay Area Exclusive Credit Card Offer',
    'Special promotional offer for customers in San Francisco Bay Area zipcodes',
    'Promotional',
    'CREDIT_CARD',
    'PromoOffer',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    NULL,
    '{
      "eligibility_criteria": {
        "operator": "AND",
        "rules": [
          {
            "field": "zipcode",
            "operator": "IN",
            "value": ["94102", "94103", "94104", "94105", "94107", "94108", "94109", "94110", "94111", "94112", "94114", "94115", "94116", "94117", "94118", "94121", "94122", "94123", "94124", "94127", "94129", "94130", "94131", "94132", "94133", "94134"]
          },
          {
            "field": "accountType",
            "operator": "EQUALS",
            "value": "credit_card"
          }
        ]
      },
      "reprint_policy": {
        "cooldown_period_days": 30
      }
    }',
    1704067200000,
    'system',
    NOW(),
    '{
      "fieldsToExtract": ["zipcode"],
      "fieldSources": {
        "zipcode": {
          "description": "Customer zipcode from address",
          "sourceApi": "customerApi",
          "extractionPath": "$.customer.address.zipCode",
          "requiredInputs": ["customerId"],
          "fieldType": "string",
          "defaultValue": null
        }
      },
      "dataSources": {
        "customerApi": {
          "description": "Fetch customer address to get zipcode",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/customers/${customerId}",
            "method": "GET",
            "timeout": 5000,
            "headers": {
              "X-Request-ID": "${correlationId}"
            }
          },
          "cache": {
            "enabled": true,
            "ttlSeconds": 3600,
            "keyPattern": "customer:${customerId}"
          },
          "providesFields": ["zipcode"]
        }
      },
      "executionStrategy": {
        "mode": "sequential",
        "continueOnError": false,
        "timeout": 10000
      }
    }'
);

-- ====================================================================
-- DOCUMENT for Zipcode-Based Promotional Offer
-- ====================================================================

-- Document 10: Bay Area Exclusive Offer (shared, zipcode-eligible)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000010',
    '77777777-7777-7777-7777-777777777777',
    1,
    'PromoOffer',
    'PROMO-BAYAREA-2024',
    'PROMO_CODE',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000010',
    'Bay_Area_Exclusive_Offer_2024.pdf',
    1704067200000,
    true,
    '{"promoCode": "PROMO-BAYAREA-2024", "offerType": "EXCLUSIVE", "region": "SF_BAY_AREA", "discountPercent": 15, "valid_from": "2024-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- ADDITIONAL TEST SCENARIOS FOR VALIDITY PERIOD TESTING
-- ====================================================================

-- ====================================================================
-- Template 8: Seasonal Promotional Offers
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '88888888-8888-8888-8888-888888888888',
    1,
    'Seasonal Promotional Offers',
    'Holiday and seasonal promotional documents',
    'Promotional',
    'ALL',
    'SeasonalOffer',
    'EN_US',
    true,
    true,
    'ALL',
    1704067200000,
    'system',
    NOW()
);

-- Document 11: EXPIRED - Black Friday 2024 Offer (expired Nov 30, 2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000011',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000011',
    'black_friday_2024_offer.pdf',
    1732060800000,
    true,
    '{"campaign": "BLACK_FRIDAY_2024", "discount": "25%", "valid_from": "2024-11-25", "valid_until": "2024-11-30"}',
    'system',
    NOW()
);

-- Document 12: EXPIRED - Cyber Monday 2024 Offer (expired Dec 2, 2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000012',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000012',
    'cyber_monday_2024_offer.pdf',
    1733097600000,
    true,
    '{"campaign": "CYBER_MONDAY_2024", "discount": "30%", "valid_from": "2024-12-01", "valid_until": "2024-12-02"}',
    'system',
    NOW()
);

-- Document 13: FUTURE - Christmas 2025 Offer (starts Dec 20, 2025)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000013',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000013',
    'christmas_2025_offer.pdf',
    1734220800000,
    true,
    '{"campaign": "CHRISTMAS_2025", "discount": "20%", "valid_from": "2025-12-20", "valid_until": "2025-12-26"}',
    'system',
    NOW()
);

-- Document 14: FUTURE - New Year 2026 Offer (starts Jan 1, 2026)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000014',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000014',
    'new_year_2026_offer.pdf',
    1735689600000,
    true,
    '{"campaign": "NEW_YEAR_2026", "discount": "15%", "valid_from": "2026-01-01", "valid_until": "2026-01-15"}',
    'system',
    NOW()
);

-- Document 15: ACTIVE - Holiday Season 2025 (currently valid)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000015',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000015',
    'holiday_season_2025_offer.pdf',
    1733097600000,
    true,
    '{"campaign": "HOLIDAY_2025", "discount": "18%", "valid_from": "2025-12-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 9: Rate Change Notices
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '99999999-9999-9999-9999-999999999999',
    1,
    'Interest Rate Change Notice',
    'Notices about APR and interest rate changes',
    'Regulatory',
    'CREDIT_CARD',
    'RateChangeNotice',
    'EN_US',
    true,
    true,
    'ACCOUNT_TYPE',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "EQUALS", "value": "credit_card"}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Document 16: EXPIRED - Q1 2024 Rate Change (expired Mar 31, 2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000016',
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000016',
    'rate_change_notice_q1_2024.pdf',
    1704067200000,
    true,
    '{"notice_type": "APR_CHANGE", "old_apr": 19.99, "new_apr": 21.99, "valid_from": "2024-01-01", "valid_until": "2024-03-31"}',
    'system',
    NOW()
);

-- Document 17: ACTIVE - Q4 2025 Rate Change (currently valid)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000017',
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000017',
    'rate_change_notice_q4_2025.pdf',
    1727740800000,
    true,
    '{"notice_type": "APR_CHANGE", "old_apr": 21.99, "new_apr": 22.49, "valid_from": "2025-10-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 18: FUTURE - Q1 2026 Rate Change (starts Jan 1, 2026)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000018',
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000018',
    'rate_change_notice_q1_2026.pdf',
    1733097600000,
    true,
    '{"notice_type": "APR_CHANGE", "old_apr": 22.49, "new_apr": 23.99, "valid_from": "2026-01-01", "valid_until": "2026-03-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 10: Tax Documents (Annual)
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    1,
    'Annual Tax Documents',
    'Year-end tax statements and 1099 forms',
    'Tax',
    'ALL',
    'TaxDocument',
    'EN_US',
    true,
    false,
    NULL,
    1704067200000,
    'system',
    NOW()
);

-- Document 19: Tax Document 2023 - PERPETUAL (no expiry, only valid_from)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000019',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    1,
    'TaxDocument',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000019',
    '1099_INT_2023.pdf',
    1706745600000,
    true,
    '{"tax_year": 2023, "form_type": "1099-INT", "interest_earned": 245.67, "valid_from": "2024-01-31"}',
    'system',
    NOW()
);

-- Document 20: Tax Document 2024 - FUTURE (available from Jan 31, 2025)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000020',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    1,
    'TaxDocument',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000020',
    '1099_INT_2024.pdf',
    1735689600000,
    true,
    '{"tax_year": 2024, "form_type": "1099-INT", "interest_earned": 312.45, "valid_from": "2025-01-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 11: Savings Account Documents
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    1,
    'Savings Account Offers',
    'Promotional offers for savings account holders',
    'Promotional',
    'SAVINGS',
    'SavingsOffer',
    'EN_US',
    true,
    true,
    'ACCOUNT_TYPE',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "EQUALS", "value": "savings"}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Document 21: High-Yield Savings Promo - ACTIVE
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000021',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    1,
    'SavingsOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000021',
    'high_yield_savings_promo.pdf',
    1727740800000,
    true,
    '{"offer_type": "HIGH_YIELD", "bonus_apy": 5.25, "min_balance": 10000, "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 22: CD Ladder Promo - EXPIRED (ended Sep 30, 2024)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000022',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    1,
    'SavingsOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000022',
    'cd_ladder_promo_2024.pdf',
    1704067200000,
    true,
    '{"offer_type": "CD_LADDER", "bonus_apy": 4.75, "terms": [6, 12, 18, 24], "valid_from": "2024-01-01", "valid_until": "2024-09-30"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 12: Document Version Management Demo
-- Shows how multiple versions of same document type can coexist
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'Fee Schedule',
    'Account fee schedule with version history',
    'Regulatory',
    'ALL',
    'FeeSchedule',
    'EN_US',
    true,
    true,
    'ALL',
    1704067200000,
    'system',
    NOW()
);

-- Document 23: Fee Schedule v1 - EXPIRED (superseded)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    reference_key,
    reference_key_type,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000023',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000023',
    'fee_schedule_v1.pdf',
    'FEE-SCHEDULE-V1',
    'DOCUMENT_VERSION',
    1704067200000,
    true,
    '{"version": "1.0", "monthly_fee": 12.00, "atm_fee": 3.00, "valid_from": "2024-01-01", "valid_until": "2024-06-30"}',
    'system',
    NOW()
);

-- Document 24: Fee Schedule v2 - EXPIRED (superseded)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    reference_key,
    reference_key_type,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000024',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000024',
    'fee_schedule_v2.pdf',
    'FEE-SCHEDULE-V2',
    'DOCUMENT_VERSION',
    1719792000000,
    true,
    '{"version": "2.0", "monthly_fee": 10.00, "atm_fee": 2.50, "valid_from": "2024-07-01", "valid_until": "2024-12-31"}',
    'system',
    NOW()
);

-- Document 25: Fee Schedule v3 - ACTIVE (current)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    reference_key,
    reference_key_type,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000025',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000025',
    'fee_schedule_v3.pdf',
    'FEE-SCHEDULE-V3',
    'DOCUMENT_VERSION',
    1735689600000,
    true,
    '{"version": "3.0", "monthly_fee": 8.00, "atm_fee": 0.00, "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 26: Fee Schedule v4 - FUTURE (not yet effective)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    reference_key,
    reference_key_type,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000026',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000026',
    'fee_schedule_v4.pdf',
    'FEE-SCHEDULE-V4',
    'DOCUMENT_VERSION',
    1735689600000,
    true,
    '{"version": "4.0", "monthly_fee": 5.00, "atm_fee": 0.00, "note": "Reduced fees program", "valid_from": "2026-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 13: Welcome Kit Documents
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    1,
    'New Account Welcome Kit',
    'Welcome documents for new account holders',
    'Onboarding',
    'ALL',
    'WelcomeKit',
    'EN_US',
    true,
    true,
    'ALL',
    1704067200000,
    'system',
    NOW()
);

-- Document 27: Welcome Kit 2024 - EXPIRED
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000027',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    1,
    'WelcomeKit',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000027',
    'welcome_kit_2024.pdf',
    1704067200000,
    true,
    '{"kit_version": "2024", "includes": ["account_guide", "mobile_app_setup", "security_tips"], "valid_from": "2024-01-01", "valid_until": "2024-12-31"}',
    'system',
    NOW()
);

-- Document 28: Welcome Kit 2025 - ACTIVE
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000028',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    1,
    'WelcomeKit',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000028',
    'welcome_kit_2025.pdf',
    1735689600000,
    true,
    '{"kit_version": "2025", "includes": ["account_guide", "mobile_app_setup", "security_tips", "rewards_overview"], "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 14: Regional Offers (US_EAST)
-- ====================================================================
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    1,
    'East Coast Regional Offers',
    'Special offers for US East region customers',
    'Promotional',
    'ALL',
    'RegionalOffer',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "region", "operator": "EQUALS", "value": "US_EAST"}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Document 29: East Coast Winter Special - ACTIVE
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000029',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    1,
    'RegionalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000029',
    'east_coast_winter_special.pdf',
    1733097600000,
    true,
    '{"region": "US_EAST", "offer_name": "Winter Warmth Bonus", "bonus_points": 5000, "valid_from": "2025-12-01", "valid_until": "2026-02-28"}',
    'system',
    NOW()
);

-- ====================================================================
-- Document 30: EDGE CASE - No validity fields (always valid)
-- ====================================================================
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000030',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000030',
    'privacy_policy_evergreen.pdf',
    1704067200000,
    true,
    '{"version": "EVERGREEN", "note": "This document has no validity period - always valid"}',
    'system',
    NOW()
);

-- ====================================================================
-- Document 31: EDGE CASE - Only valid_until (no start date)
-- ====================================================================
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000031',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000031',
    'privacy_policy_legacy.pdf',
    1609459200000,
    true,
    '{"version": "LEGACY", "note": "Valid until replaced", "valid_until": "2025-06-30"}',
    'system',
    NOW()
);

-- ====================================================================
-- ADDITIONAL TEST SCENARIOS - TEMPLATE EDGE CASES
-- ====================================================================

-- Template 15: INACTIVE Template (active_flag = false)
-- Should NEVER be returned regardless of other conditions
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000001',
    1,
    'Inactive Promotional Offer',
    'This template is inactive and should never appear',
    'Promotional',
    'ALL',
    'InactivePromo',
    'EN_US',
    false,
    true,
    'ALL',
    1704067200000,
    'system',
    NOW()
);

-- Document 32: Document under INACTIVE template
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000032',
    'f0000000-0000-0000-0000-000000000001',
    1,
    'InactivePromo',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000032',
    'inactive_promo_should_not_appear.pdf',
    1704067200000,
    true,
    '{"note": "This document should NEVER appear - template is inactive", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Template 16: EXPIRED Template (end_date in the past)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    end_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000002',
    1,
    'Expired Template - Summer 2024 Campaign',
    'Template expired on Sep 30, 2024',
    'Promotional',
    'ALL',
    'ExpiredTemplate',
    'EN_US',
    true,
    true,
    'ALL',
    1717200000000,
    1727740800000,
    'system',
    NOW()
);

-- Document 33: Document under EXPIRED template
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000033',
    'f0000000-0000-0000-0000-000000000002',
    1,
    'ExpiredTemplate',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000033',
    'summer_2024_expired_template.pdf',
    1719792000000,
    true,
    '{"campaign": "SUMMER_2024", "note": "Template expired, should not appear"}',
    'system',
    NOW()
);

-- Template 17: FUTURE Template (start_date in the future)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000003',
    1,
    'Future Template - 2026 Campaign',
    'Template starts on Jan 1, 2026',
    'Promotional',
    'ALL',
    'FutureTemplate',
    'EN_US',
    true,
    true,
    'ALL',
    1767225600000,
    'system',
    NOW()
);

-- Document 34: Document under FUTURE template
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000034',
    'f0000000-0000-0000-0000-000000000003',
    1,
    'FutureTemplate',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000034',
    'future_2026_template.pdf',
    1735689600000,
    true,
    '{"campaign": "2026_LAUNCH", "note": "Template not yet active, should not appear"}',
    'system',
    NOW()
);

-- ====================================================================
-- DOCUMENT STATE EDGE CASES
-- ====================================================================

-- Document 35: INACCESSIBLE Document (accessible_flag = 0)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000035',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000035',
    'inaccessible_document.pdf',
    1704067200000,
    false,
    '{"note": "Document marked inaccessible - should not appear", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Document 36: NULL Metadata Edge Case
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000036',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000036',
    'null_metadata_document.pdf',
    1704067200000,
    true,
    NULL,
    'system',
    NOW()
);

-- Document 37: Different Storage Vendor (Azure)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000037',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'AZURE_BLOB',
    'b0000000-0000-0000-0000-000000000037',
    'azure_stored_document.pdf',
    1704067200000,
    true,
    '{"storage": "AZURE_BLOB", "container": "documents", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Document 38: Different Language (EN_UK)
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000038',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000038',
    'privacy_policy_uk.pdf',
    1704067200000,
    true,
    '{"language": "EN_UK", "region": "UK", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- OR LOGIC ELIGIBILITY TEMPLATE
-- ====================================================================

-- Template 18: OR Logic - Multi-Region Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000004',
    1,
    'Multi-Region Coastal Offer',
    'Offer for customers in US_EAST OR US_WEST regions',
    'Promotional',
    'ALL',
    'CoastalOffer',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{
      "eligibility_criteria": {
        "operator": "OR",
        "rules": [
          {"field": "region", "operator": "EQUALS", "value": "US_EAST"},
          {"field": "region", "operator": "EQUALS", "value": "US_WEST"}
        ]
      }
    }',
    1704067200000,
    'system',
    NOW()
);

-- Document 39: Coastal Offer Document
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000039',
    'f0000000-0000-0000-0000-000000000004',
    1,
    'CoastalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000039',
    'coastal_regions_offer.pdf',
    1704067200000,
    true,
    '{"offer": "COASTAL_2025", "eligible_regions": ["US_EAST", "US_WEST"], "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Template 19: Nested Rules (AND containing OR)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000005',
    1,
    'Premium Coastal Credit Card Offer',
    'VIP/Premium customers in coastal regions only',
    'Promotional',
    'CREDIT_CARD',
    'PremiumCoastalOffer',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{
      "eligibility_criteria": {
        "operator": "AND",
        "rules": [
          {"field": "accountType", "operator": "EQUALS", "value": "credit_card"},
          {
            "operator": "OR",
            "rules": [
              {"field": "customerSegment", "operator": "EQUALS", "value": "VIP"},
              {"field": "customerSegment", "operator": "EQUALS", "value": "PREMIUM"}
            ]
          },
          {
            "operator": "OR",
            "rules": [
              {"field": "region", "operator": "EQUALS", "value": "US_EAST"},
              {"field": "region", "operator": "EQUALS", "value": "US_WEST"}
            ]
          }
        ]
      }
    }',
    1704067200000,
    'system',
    NOW()
);

-- Document 40: Premium Coastal Offer
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000040',
    'f0000000-0000-0000-0000-000000000005',
    1,
    'PremiumCoastalOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000040',
    'premium_coastal_exclusive.pdf',
    1704067200000,
    true,
    '{"offer": "PREMIUM_COASTAL_2025", "segments": ["VIP", "PREMIUM"], "regions": ["US_EAST", "US_WEST"], "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- CUSTOMER/ACCOUNT SCENARIOS
-- ====================================================================

-- Additional Test Customers with Different Attributes
-- Customer 2: Standard segment, US_CENTRAL region
-- Customer 3: Premium segment, US_EAST region
-- Customer 4: VIP segment, US_WEST region (no documents)
-- Customer 5: Standard segment, no accounts

-- Template 20: Central Region Only
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000006',
    1,
    'Central Region Exclusive',
    'Offer for US_CENTRAL region only',
    'Promotional',
    'ALL',
    'CentralOffer',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "region", "operator": "EQUALS", "value": "US_CENTRAL"}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Document 41: Central Region Offer
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000041',
    'f0000000-0000-0000-0000-000000000006',
    1,
    'CentralOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000041',
    'central_region_exclusive.pdf',
    1704067200000,
    true,
    '{"region": "US_CENTRAL", "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- API SPEC DOCUMENT TYPES
-- ====================================================================

-- Template 21: Payment Letters
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000007',
    1,
    'Payment Confirmation Letters',
    'Payment confirmation notices',
    'PaymentConfirmationNotice',
    'ALL',
    'PaymentLetter',
    'EN_US',
    true,
    false,
    NULL,
    1704067200000,
    'system',
    NOW()
);

-- Document 42: Payment Confirmation Letter
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000042',
    'f0000000-0000-0000-0000-000000000007',
    1,
    'PaymentLetter',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000042',
    'payment_confirmation_dec_2025.pdf',
    1733097600000,
    true,
    '{"payment_date": "2025-12-01", "amount": 150.00, "confirmation_number": "PAY-2025-001", "valid_from": "2025-12-01", "valid_until": "2028-12-01"}',
    'system',
    NOW()
);

-- Template 22: Delinquency Notices
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000008',
    1,
    'Delinquency Notices',
    'Account delinquency notifications',
    'DelinquencyNotice',
    'ALL',
    'DelinquencyNotice',
    'EN_US',
    true,
    false,
    NULL,
    1704067200000,
    'system',
    NOW()
);

-- Document 43: Delinquency Notice
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000043',
    'f0000000-0000-0000-0000-000000000008',
    1,
    'DelinquencyNotice',
    false,
    'aaaa0000-0000-0000-0000-000000000002',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000043',
    'delinquency_notice_30day.pdf',
    1732060800000,
    true,
    '{"days_past_due": 30, "amount_due": 275.50, "notice_type": "30_DAY", "valid_from": "2025-11-20", "valid_until": "2028-11-20"}',
    'system',
    NOW()
);

-- Template 23: Fraud Letters
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000009',
    1,
    'Fraud Alert Letters',
    'Fraud investigation and resolution letters',
    'FraudLetter',
    'ALL',
    'FraudLetter',
    'EN_US',
    true,
    false,
    NULL,
    1704067200000,
    'system',
    NOW()
);

-- Document 44: Fraud Alert Letter
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000044',
    'f0000000-0000-0000-0000-000000000009',
    1,
    'FraudLetter',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000044',
    'fraud_investigation_resolved.pdf',
    1730419200000,
    true,
    '{"case_number": "FRD-2025-0042", "status": "RESOLVED", "resolution": "NO_FRAUD_CONFIRMED", "valid_from": "2025-11-01", "valid_until": "2032-11-01"}',
    'system',
    NOW()
);

-- Template 24: Adverse Action Notices
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000010',
    1,
    'Adverse Action Notices',
    'Credit decision adverse action notifications',
    'AdverseAction',
    'CREDIT_CARD',
    'AdverseActionNotice',
    'EN_US',
    true,
    false,
    NULL,
    1704067200000,
    'system',
    NOW()
);

-- Document 45: Adverse Action Notice
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
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
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000045',
    'f0000000-0000-0000-0000-000000000010',
    1,
    'AdverseActionNotice',
    false,
    'aaaa0000-0000-0000-0000-000000000003',
    'cccc0000-0000-0000-0000-000000000001',
    'ecms',
    'b0000000-0000-0000-0000-000000000045',
    'adverse_action_cli_denied.pdf',
    1727740800000,
    true,
    '{"action_type": "CLI_DENIED", "reason_codes": ["R001", "R003"], "valid_from": "2025-10-01", "valid_until": "2032-10-01"}',
    'system',
    NOW()
);

-- ====================================================================
-- PAGINATION TEST DOCUMENTS (Documents 46-60)
-- ====================================================================

-- Add more statement documents for pagination testing
INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000046', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000046', 'statement_feb_2024.pdf', 1709251200000, true, '{"statement_date": "2024-02-29", "balance": 1456.78, "valid_from": "2024-02-29", "valid_until": "2027-02-28"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000047', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000047', 'statement_mar_2024.pdf', 1711929600000, true, '{"statement_date": "2024-03-31", "balance": 1678.90, "valid_from": "2024-03-31", "valid_until": "2027-03-31"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000048', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000048', 'statement_apr_2024.pdf', 1714521600000, true, '{"statement_date": "2024-04-30", "balance": 2012.34, "valid_from": "2024-04-30", "valid_until": "2027-04-30"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000049', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000049', 'statement_may_2024.pdf', 1717200000000, true, '{"statement_date": "2024-05-31", "balance": 1890.12, "valid_from": "2024-05-31", "valid_until": "2027-05-31"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000050', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000050', 'statement_jun_2024.pdf', 1719792000000, true, '{"statement_date": "2024-06-30", "balance": 2234.56, "valid_from": "2024-06-30", "valid_until": "2027-06-30"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000051', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000051', 'statement_jul_2024.pdf', 1722470400000, true, '{"statement_date": "2024-07-31", "balance": 1567.89, "valid_from": "2024-07-31", "valid_until": "2027-07-31"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000052', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000052', 'statement_aug_2024.pdf', 1725148800000, true, '{"statement_date": "2024-08-31", "balance": 1789.01, "valid_from": "2024-08-31", "valid_until": "2027-08-31"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000053', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000053', 'statement_sep_2024.pdf', 1727740800000, true, '{"statement_date": "2024-09-30", "balance": 2345.67, "valid_from": "2024-09-30", "valid_until": "2027-09-30"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000054', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000054', 'statement_oct_2024.pdf', 1730419200000, true, '{"statement_date": "2024-10-31", "balance": 1987.65, "valid_from": "2024-10-31", "valid_until": "2027-10-31"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000055', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000055', 'statement_nov_2024.pdf', 1733011200000, true, '{"statement_date": "2024-11-30", "balance": 2678.90, "valid_from": "2024-11-30", "valid_until": "2027-11-30"}', 'system', NOW());

INSERT INTO document_hub.storage_index (storage_index_id, master_template_id, template_version, template_type, shared_flag, account_key, customer_key, storage_vendor, storage_document_key, file_name, doc_creation_date, accessible_flag, doc_metadata, created_by, created_timestamp)
VALUES ('a0000000-0000-0000-0000-000000000056', '22222222-2222-2222-2222-222222222222', 1, 'Statement', false, 'aaaa0000-0000-0000-0000-000000000001', 'cccc0000-0000-0000-0000-000000000001', 'ecms', 'b0000000-0000-0000-0000-000000000056', 'statement_dec_2024.pdf', 1735689600000, true, '{"statement_date": "2024-12-31", "balance": 3012.34, "valid_from": "2024-12-31", "valid_until": "2027-12-31"}', 'system', NOW());

-- ====================================================================
-- CREDIT SCORE BASED ELIGIBILITY
-- ====================================================================

-- Template 25: High Credit Score Offers (750+)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    template_config,
    start_date,
    created_by,
    created_timestamp,
    data_extraction_config
) VALUES (
    'f0000000-0000-0000-0000-000000000011',
    1,
    'Elite Credit Score Offers',
    'Premium offers for customers with credit score 750+',
    'Promotional',
    'CREDIT_CARD',
    'EliteCreditOffer',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    '{
      "eligibility_criteria": {
        "operator": "AND",
        "rules": [
          {"field": "creditScore", "operator": "GREATER_THAN_OR_EQUAL", "value": 750}
        ]
      }
    }',
    1704067200000,
    'system',
    NOW(),
    '{
      "fieldsToExtract": ["creditScore"],
      "fieldSources": {
        "creditScore": {
          "sourceApi": "creditBureauApi",
          "extractionPath": "$.creditScore",
          "requiredInputs": ["customerId"]
        }
      },
      "dataSources": {
        "creditBureauApi": {
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/credit-bureau/${customerId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["creditScore"]
        }
      }
    }'
);

-- Document 57: Elite Credit Offer
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000057',
    'f0000000-0000-0000-0000-000000000011',
    1,
    'EliteCreditOffer',
    true,
    'ecms',
    'b0000000-0000-0000-0000-000000000057',
    'elite_credit_platinum_rewards.pdf',
    1704067200000,
    true,
    '{"offer": "ELITE_PLATINUM_2025", "min_credit_score": 750, "apr": 12.99, "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- SUMMARY OF ALL TEST SCENARIOS
-- ====================================================================
--
-- TEMPLATE EDGE CASES:
--   Template 15: Inactive (active_flag = false) - Doc 32
--   Template 16: Expired (end_date in past) - Doc 33
--   Template 17: Future (start_date in future) - Doc 34
--
-- DOCUMENT STATE EDGE CASES:
--   Doc 35: Inaccessible (accessible_flag = 0)
--   Doc 36: NULL metadata
--   Doc 37: Different storage vendor (AZURE_BLOB)
--   Doc 38: Different language (EN_UK)
--
-- OR LOGIC ELIGIBILITY:
--   Template 18: OR logic (US_EAST OR US_WEST) - Doc 39
--   Template 19: Nested AND(OR) logic - Doc 40
--
-- CUSTOMER SEGMENTS:
--   Template 20: US_CENTRAL region only - Doc 41
--
-- API SPEC DOCUMENT TYPES:
--   Template 21: PaymentLetter - Doc 42
--   Template 22: DelinquencyNotice - Doc 43
--   Template 23: FraudLetter - Doc 44
--   Template 24: AdverseActionNotice - Doc 45
--
-- PAGINATION TEST DOCUMENTS:
--   Docs 46-56: Monthly statements for Account 1 (Feb-Dec 2024)
--
-- CREDIT SCORE ELIGIBILITY:
--   Template 25: Credit Score 750+ offers - Doc 57
--
-- TOTAL: 25 templates, 57 documents