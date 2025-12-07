-- ====================================================================
-- Document Hub POC - Sample Data
-- ====================================================================
-- This script inserts sample data for testing different sharing scenarios
-- ====================================================================

-- Clean up existing data (H2 compatible)
DELETE FROM storage_index;
DELETE FROM master_template_definition;

-- ====================================================================
-- MASTER TEMPLATE DEFINITIONS
-- ====================================================================

-- Template 1: Privacy Policy (Shared with ALL)
INSERT INTO master_template_definition (
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
    '{"eligibilityCriteria": {"operator": "AND", "rules": []}}',
    1704067200000,
    'system',
    NOW()
);

-- Template 2: Credit Card Statement (Account-Specific)
INSERT INTO master_template_definition (
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
    '{"eligibilityCriteria": {"operator": "AND", "rules": []}}',
    1704067200000,
    'system',
    NOW()
);

-- Template 3: Balance Transfer Offer (Shared by Account Type - Credit Card)
INSERT INTO master_template_definition (
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
    '{"eligibilityCriteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "IN", "value": ["credit_card"]}]}}',
    1704067200000,
    'system',
    NOW()
);

-- Template 4: VIP Customer Offer (Shared with Custom Rules - VIP + US_WEST)
INSERT INTO master_template_definition (
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
    '{
        "eligibilityCriteria": {
            "operator": "AND",
            "rules": [
                {"field": "customerSegment", "operator": "EQUALS", "value": "VIP"},
                {"field": "region", "operator": "EQUALS", "value": "US_WEST"}
            ]
        }
    }',
    1704067200000,
    'system',
    NOW()
);

-- Template 5: Regulatory Disclosure (Shared with Custom Rules - Reference Key Match)
INSERT INTO master_template_definition (
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
    '{
        "eligibilityCriteria": {
            "operator": "AND",
            "rules": [
                {"field": "$metadata.disclosure_code", "operator": "EQUALS", "value": "D164"}
            ]
        }
    }',
    1704067200000,
    'system',
    NOW()
);

-- ====================================================================
-- STORAGE INDEX (DOCUMENTS)
-- ====================================================================

-- Document 1: Privacy Policy (Shared with ALL)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000001',
    'privacy_policy_2024.pdf',
    1705276800000,
    1,
    '{"version": "2024.1", "valid_from": "2024-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 2: Statement for Account 1 (Account-Specific)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000002',
    'statement_jan_2024.pdf',
    1706659200000,
    1,
    '{"statement_date": "2024-01-31", "balance": 1234.56, "valid_from": "2024-01-31", "valid_until": "2027-01-31"}',
    'system',
    NOW()
);

-- Document 3: Statement for Account 2 (Account-Specific)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000003',
    'statement_jan_2024_acct2.pdf',
    1706659200000,
    1,
    '{"statement_date": "2024-01-31", "balance": 5678.90, "valid_from": "2024-01-31", "valid_until": "2027-01-31"}',
    'system',
    NOW()
);

-- Document 4: Balance Transfer Offer (Shared by Account Type)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000004',
    'balance_transfer_offer_q1_2024.pdf',
    1706745600000,
    1,
    '{"offer_code": "BT2024Q1", "apr_rate": 0.00, "valid_from": "2024-01-01", "valid_until": "2024-06-30"}',
    'system',
    NOW()
);

-- Document 5: VIP Exclusive Offer (Shared with Custom Rules)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000005',
    'vip_exclusive_offer.pdf',
    1707955200000,
    1,
    '{"segment": "VIP", "region": "US_WEST", "offer_type": "exclusive", "valid_from": "2024-02-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 6: Regulatory Disclosure (Shared with Custom Rules)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000006',
    'disclosure_d164.pdf',
    'D164',
    'DISCLOSURE_CODE',
    1704844800000,
    1,
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
INSERT INTO master_template_definition (
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
    '{"eligibilityCriteria": {"operator": "AND", "rules": []}}',
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
            "url": "http://localhost:8080/mock-api/creditcard/accounts/${accountId}/arrangements",
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
            "url": "http://localhost:8080/mock-api/pricing-service/prices/${pricingId}",
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000007',
    'Credit_Card_Terms_D164_v1.pdf',
    1704067200000,
    1,
    '{"disclosureCode": "D164", "version": "1.0", "documentType": "CARDHOLDER_AGREEMENT", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Document 8: Terms & Conditions for disclosure code D165
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000008',
    'Credit_Card_Terms_D165_v1.pdf',
    1704067200000,
    1,
    '{"disclosureCode": "D165", "version": "1.0", "documentType": "CARDHOLDER_AGREEMENT", "valid_from": "2024-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- Document 9: Terms & Conditions for disclosure code D166 (Premium cards)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000009',
    'Premium_Credit_Card_Terms_D166_v1.pdf',
    1704067200000,
    1,
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
INSERT INTO master_template_definition (
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
            "url": "http://localhost:8080/mock-api/customers/${customerId}",
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000010',
    'Bay_Area_Exclusive_Offer_2024.pdf',
    1704067200000,
    1,
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
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000011',
    'black_friday_2024_offer.pdf',
    1732060800000,
    1,
    '{"campaign": "BLACK_FRIDAY_2024", "discount": "25%", "valid_from": "2024-11-25", "valid_until": "2024-11-30"}',
    'system',
    NOW()
);

-- Document 12: EXPIRED - Cyber Monday 2024 Offer (expired Dec 2, 2024)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000012',
    'cyber_monday_2024_offer.pdf',
    1733097600000,
    1,
    '{"campaign": "CYBER_MONDAY_2024", "discount": "30%", "valid_from": "2024-12-01", "valid_until": "2024-12-02"}',
    'system',
    NOW()
);

-- Document 13: FUTURE - Christmas 2025 Offer (starts Dec 20, 2025)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000013',
    'christmas_2025_offer.pdf',
    1734220800000,
    1,
    '{"campaign": "CHRISTMAS_2025", "discount": "20%", "valid_from": "2025-12-20", "valid_until": "2025-12-26"}',
    'system',
    NOW()
);

-- Document 14: FUTURE - New Year 2026 Offer (starts Jan 1, 2026)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000014',
    'new_year_2026_offer.pdf',
    1735689600000,
    1,
    '{"campaign": "NEW_YEAR_2026", "discount": "15%", "valid_from": "2026-01-01", "valid_until": "2026-01-15"}',
    'system',
    NOW()
);

-- Document 15: ACTIVE - Holiday Season 2025 (currently valid)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000015',
    'holiday_season_2025_offer.pdf',
    1733097600000,
    1,
    '{"campaign": "HOLIDAY_2025", "discount": "18%", "valid_from": "2025-12-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 9: Rate Change Notices
-- ====================================================================
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000016',
    'rate_change_notice_q1_2024.pdf',
    1704067200000,
    1,
    '{"notice_type": "APR_CHANGE", "old_apr": 19.99, "new_apr": 21.99, "valid_from": "2024-01-01", "valid_until": "2024-03-31"}',
    'system',
    NOW()
);

-- Document 17: ACTIVE - Q4 2025 Rate Change (currently valid)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000017',
    'rate_change_notice_q4_2025.pdf',
    1727740800000,
    1,
    '{"notice_type": "APR_CHANGE", "old_apr": 21.99, "new_apr": 22.49, "valid_from": "2025-10-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 18: FUTURE - Q1 2026 Rate Change (starts Jan 1, 2026)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000018',
    'rate_change_notice_q1_2026.pdf',
    1733097600000,
    1,
    '{"notice_type": "APR_CHANGE", "old_apr": 22.49, "new_apr": 23.99, "valid_from": "2026-01-01", "valid_until": "2026-03-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 10: Tax Documents (Annual)
-- ====================================================================
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000019',
    '1099_INT_2023.pdf',
    1706745600000,
    1,
    '{"tax_year": 2023, "form_type": "1099-INT", "interest_earned": 245.67, "valid_from": "2024-01-31"}',
    'system',
    NOW()
);

-- Document 20: Tax Document 2024 - FUTURE (available from Jan 31, 2025)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000020',
    '1099_INT_2024.pdf',
    1735689600000,
    1,
    '{"tax_year": 2024, "form_type": "1099-INT", "interest_earned": 312.45, "valid_from": "2025-01-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 11: Savings Account Documents
-- ====================================================================
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000021',
    'high_yield_savings_promo.pdf',
    1727740800000,
    1,
    '{"offer_type": "HIGH_YIELD", "bonus_apy": 5.25, "min_balance": 10000, "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 22: CD Ladder Promo - EXPIRED (ended Sep 30, 2024)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000022',
    'cd_ladder_promo_2024.pdf',
    1704067200000,
    1,
    '{"offer_type": "CD_LADDER", "bonus_apy": 4.75, "terms": [6, 12, 18, 24], "valid_from": "2024-01-01", "valid_until": "2024-09-30"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 12: Document Version Management Demo
-- Shows how multiple versions of same document type can coexist
-- ====================================================================
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000023',
    'fee_schedule_v1.pdf',
    'FEE-SCHEDULE-V1',
    'DOCUMENT_VERSION',
    1704067200000,
    1,
    '{"version": "1.0", "monthly_fee": 12.00, "atm_fee": 3.00, "valid_from": "2024-01-01", "valid_until": "2024-06-30"}',
    'system',
    NOW()
);

-- Document 24: Fee Schedule v2 - EXPIRED (superseded)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000024',
    'fee_schedule_v2.pdf',
    'FEE-SCHEDULE-V2',
    'DOCUMENT_VERSION',
    1719792000000,
    1,
    '{"version": "2.0", "monthly_fee": 10.00, "atm_fee": 2.50, "valid_from": "2024-07-01", "valid_until": "2024-12-31"}',
    'system',
    NOW()
);

-- Document 25: Fee Schedule v3 - ACTIVE (current)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000025',
    'fee_schedule_v3.pdf',
    'FEE-SCHEDULE-V3',
    'DOCUMENT_VERSION',
    1735689600000,
    1,
    '{"version": "3.0", "monthly_fee": 8.00, "atm_fee": 0.00, "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- Document 26: Fee Schedule v4 - FUTURE (not yet effective)
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000026',
    'fee_schedule_v4.pdf',
    'FEE-SCHEDULE-V4',
    'DOCUMENT_VERSION',
    1735689600000,
    1,
    '{"version": "4.0", "monthly_fee": 5.00, "atm_fee": 0.00, "note": "Reduced fees program", "valid_from": "2026-01-01", "valid_until": "2026-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 13: Welcome Kit Documents
-- ====================================================================
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000027',
    'welcome_kit_2024.pdf',
    1704067200000,
    1,
    '{"kit_version": "2024", "includes": ["account_guide", "mobile_app_setup", "security_tips"], "valid_from": "2024-01-01", "valid_until": "2024-12-31"}',
    'system',
    NOW()
);

-- Document 28: Welcome Kit 2025 - ACTIVE
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000028',
    'welcome_kit_2025.pdf',
    1735689600000,
    1,
    '{"kit_version": "2025", "includes": ["account_guide", "mobile_app_setup", "security_tips", "rewards_overview"], "valid_from": "2025-01-01", "valid_until": "2025-12-31"}',
    'system',
    NOW()
);

-- ====================================================================
-- Template 14: Regional Offers (US_EAST)
-- ====================================================================
INSERT INTO master_template_definition (
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
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000029',
    'east_coast_winter_special.pdf',
    1733097600000,
    1,
    '{"region": "US_EAST", "offer_name": "Winter Warmth Bonus", "bonus_points": 5000, "valid_from": "2025-12-01", "valid_until": "2026-02-28"}',
    'system',
    NOW()
);

-- ====================================================================
-- Document 30: EDGE CASE - No validity fields (always valid)
-- ====================================================================
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000030',
    'privacy_policy_evergreen.pdf',
    1704067200000,
    1,
    '{"version": "EVERGREEN", "note": "This document has no validity period - always valid"}',
    'system',
    NOW()
);

-- ====================================================================
-- Document 31: EDGE CASE - Only valid_until (no start date)
-- ====================================================================
INSERT INTO storage_index (
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
    'S3',
    'b0000000-0000-0000-0000-000000000031',
    'privacy_policy_legacy.pdf',
    1609459200000,
    1,
    '{"version": "LEGACY", "note": "Valid until replaced", "valid_until": "2025-06-30"}',
    'system',
    NOW()
);

-- ====================================================================
-- SUMMARY OF TEST SCENARIOS
-- ====================================================================
-- EXPIRED Documents (should NOT be returned):
--   Doc 4:  Balance Transfer Q1 2024 (valid_until: 2024-06-30)
--   Doc 9:  Premium Card Terms D166 (valid_until: 2024-06-30)
--   Doc 11: Black Friday 2024 (valid_until: 2024-11-30)
--   Doc 12: Cyber Monday 2024 (valid_until: 2024-12-02)
--   Doc 16: Rate Change Q1 2024 (valid_until: 2024-03-31)
--   Doc 22: CD Ladder Promo 2024 (valid_until: 2024-09-30)
--   Doc 23: Fee Schedule v1 (valid_until: 2024-06-30)
--   Doc 24: Fee Schedule v2 (valid_until: 2024-12-31)
--   Doc 27: Welcome Kit 2024 (valid_until: 2024-12-31)
--
-- FUTURE Documents (should NOT be returned yet):
--   Doc 13: Christmas 2025 (valid_from: 2025-12-20)
--   Doc 14: New Year 2026 (valid_from: 2026-01-01)
--   Doc 18: Rate Change Q1 2026 (valid_from: 2026-01-01)
--   Doc 20: Tax Doc 2024 (valid_from: 2025-01-31)
--   Doc 26: Fee Schedule v4 (valid_from: 2026-01-01)
--
-- ACTIVE Documents (should be returned):
--   Doc 1, 2, 3, 5, 6, 7, 8, 10, 15, 17, 19, 21, 25, 28, 29, 30, 31
--
-- EDGE CASES:
--   Doc 19: Only valid_from (perpetual - no expiry)
--   Doc 30: No validity fields (always valid)
--   Doc 31: Only valid_until (no start date constraint)
