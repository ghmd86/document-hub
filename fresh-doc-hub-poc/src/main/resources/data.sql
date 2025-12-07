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
    '{"version": "2024.1", "effective_date": "2024-01-01"}',
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
    '{"statement_date": "2024-01-31", "balance": 1234.56}',
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
    '{"statement_date": "2024-01-31", "balance": 5678.90}',
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
    '{"offer_code": "BT2024Q1", "apr_rate": 0.00, "valid_until": "2024-06-30"}',
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
    '{"segment": "VIP", "region": "US_WEST", "offer_type": "exclusive"}',
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
    '{"disclosure_code": "D164", "regulatory_type": "ESIGN", "version": "1.0"}',
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
    '{"disclosureCode": "D164", "version": "1.0", "effectiveDate": "2024-01-01", "documentType": "CARDHOLDER_AGREEMENT"}',
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
    '{"disclosureCode": "D165", "version": "1.0", "effectiveDate": "2024-01-01", "documentType": "CARDHOLDER_AGREEMENT"}',
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
    '{"disclosureCode": "D166", "version": "1.0", "effectiveDate": "2024-01-01", "documentType": "CARDHOLDER_AGREEMENT", "cardTier": "PREMIUM"}',
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
    '{"promoCode": "PROMO-BAYAREA-2024", "offerType": "EXCLUSIVE", "region": "SF_BAY_AREA", "validUntil": "2024-12-31", "discountPercent": 15}',
    'system',
    NOW()
);
