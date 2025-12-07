-- ========================================
-- MOCK DATA FOR TESTING MULTI-STEP DATA EXTRACTION
-- ========================================

-- Clean up existing data
DELETE FROM storage_index;
DELETE FROM master_template_definition;

-- ========================================
-- TEST SCENARIO 1: Simple 3-Step Chain
-- Template: ACCOUNT_STATEMENT
-- Chain: Account → Product → Disclosure
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
    B'1'::bit(1),
    true,
    B'1'::bit(1),
    'CUSTOM_RULES',
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1',
    '{
      "fieldsToExtract": ["pricingId", "disclosureCode"],
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
            "url": "http://localhost:8080/api/v1/api/v1/mock-api/creditcard/accounts/${accountId}/arrangements",
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
            "url": "http://localhost:8080/api/v1/api/v1/mock-api/pricing-service/prices/${pricingId}",
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
-- TEST SCENARIO 2: Parallel + Sequential (Diamond Pattern)
-- Template: REGULATORY_DISCLOSURE
-- Chain: Account + Customer (parallel) → Product + Region (parallel) → Disclosure (merge)
-- ========================================

INSERT INTO master_template_definition (
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
    start_date,
    end_date,
    created_by,
    created_timestamp,
    record_status,
    data_extraction_config
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    1,
    'REGULATORY_DISCLOSURE',
    'Regulatory Disclosure Document',
    'Disclosure Document',
    'Regulatory required disclosure based on product and region',
    'REGULATORY',
    'COMPLIANCE',
    'en',
    B'1'::bit(1),
    true,
    B'1'::bit(1),
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1',
    '{
      "fieldsToExtract": ["productCode", "customerLocation", "productCategory", "regulatoryRegion", "finalDisclosures"],
      "fieldSources": {
        "productCode": {
          "sourceApi": "accountApi",
          "extractionPath": "$.account.productCode",
          "requiredInputs": ["accountId"],
          "fieldType": "string"
        },
        "customerLocation": {
          "sourceApi": "customerApi",
          "extractionPath": "$.customer.address.state",
          "requiredInputs": ["customerId"],
          "fieldType": "string"
        },
        "productCategory": {
          "sourceApi": "productApi",
          "extractionPath": "$.product.category",
          "requiredInputs": ["productCode"],
          "fieldType": "string"
        },
        "regulatoryRegion": {
          "sourceApi": "regionMappingApi",
          "extractionPath": "$.mapping.region",
          "requiredInputs": ["customerLocation"],
          "fieldType": "string"
        },
        "finalDisclosures": {
          "sourceApi": "disclosureApi",
          "extractionPath": "$.disclosures",
          "requiredInputs": ["productCategory", "regulatoryRegion"],
          "fieldType": "array"
        }
      },
      "dataSources": {
        "accountApi": {
          "description": "Level 1: Get account info",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/accounts/${accountId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["productCode"]
        },
        "customerApi": {
          "description": "Level 1: Get customer info (parallel with accountApi)",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/customers/${customerId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["customerLocation"]
        },
        "productApi": {
          "description": "Level 2: Get product category",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/products/${productCode}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["productCategory"]
        },
        "regionMappingApi": {
          "description": "Level 2: Map location to region",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/regions/map",
            "method": "POST",
            "timeout": 3000,
            "headers": {
              "Content-Type": "application/json"
            },
            "body": "{\"state\": \"${customerLocation}\"}"
          },
          "providesFields": ["regulatoryRegion"]
        },
        "disclosureApi": {
          "description": "Level 3: Get final disclosures (merge both paths)",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/disclosures?category=${productCategory}&region=${regulatoryRegion}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["finalDisclosures"]
        }
      },
      "executionStrategy": {
        "mode": "auto",
        "continueOnError": false,
        "timeout": 25000
      }
    }'
);

-- ========================================
-- TEST SCENARIO 3: 5-Step Deep Chain
-- Template: BRANCH_SPECIFIC_DOCUMENT
-- Chain: Account → Branch → Region → Compliance → Documents
-- ========================================

INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    display_name,
    template_description,
    active_flag,
    shared_document_flag,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    record_status,
    data_extraction_config
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    1,
    'BRANCH_SPECIFIC_DOCUMENT',
    'Branch Specific Document',
    'Branch Document',
    'Document specific to branch compliance rules',
    B'1'::bit(1),
    B'1'::bit(1),
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1',
    '{
      "fieldsToExtract": ["branchCode", "regionCode", "complianceRules", "documentList"],
      "fieldSources": {
        "branchCode": {
          "description": "Branch code from account",
          "sourceApi": "accountApi",
          "extractionPath": "$.account.branchCode",
          "requiredInputs": ["accountId"]
        },
        "regionCode": {
          "description": "Region from branch",
          "sourceApi": "branchApi",
          "extractionPath": "$.branch.regionCode",
          "requiredInputs": ["branchCode"]
        },
        "complianceRules": {
          "description": "Compliance rules for region",
          "sourceApi": "complianceApi",
          "extractionPath": "$.compliance.rules",
          "requiredInputs": ["regionCode"]
        },
        "documentList": {
          "description": "Applicable documents based on compliance",
          "sourceApi": "documentRulesApi",
          "extractionPath": "$.documents",
          "requiredInputs": ["complianceRules"]
        }
      },
      "dataSources": {
        "accountApi": {
          "description": "Step 1: Get account details",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/accounts/${accountId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["branchCode"]
        },
        "branchApi": {
          "description": "Step 2: Get branch details",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/branches/${branchCode}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["regionCode"]
        },
        "complianceApi": {
          "description": "Step 3: Get compliance rules",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/compliance/regions/${regionCode}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["complianceRules"]
        },
        "documentRulesApi": {
          "description": "Step 4: Get applicable documents",
          "endpoint": {
            "url": "http://localhost:8080/api/v1/mock-api/document-rules",
            "method": "POST",
            "timeout": 5000,
            "headers": {
              "Content-Type": "application/json"
            },
            "body": "{\"rules\": ${complianceRules}}"
          },
          "providesFields": ["documentList"]
        }
      },
      "executionStrategy": {
        "mode": "auto",
        "continueOnError": false,
        "timeout": 30000
      }
    }'
);

-- ========================================
-- STORAGE INDEX DATA (Documents for testing)
-- ========================================

-- Documents for ACCOUNT_STATEMENT template
INSERT INTO storage_index (
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
) VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    1,
    'ACCOUNT_STATEMENT',
    'STMT-2024-01',
    'STATEMENT_ID',
    false,
    '550e8400-e29b-41d4-a716-446655440000',
    '123e4567-e89b-12d3-a456-426614174000',
    'S3',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'Statement_January_2024.pdf',
    1704067200000,
    true,
    '{"productCode": "CC-PREMIUM-001", "productCategory": "CREDIT_CARD", "disclosureCode": "D164"}',
    'system',
    CURRENT_TIMESTAMP,
    '1'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-bbbbbbbbbbbb',
    '11111111-1111-1111-1111-111111111111',
    1,
    'ACCOUNT_STATEMENT',
    'STMT-2024-02',
    'STATEMENT_ID',
    false,
    '550e8400-e29b-41d4-a716-446655440000',
    '123e4567-e89b-12d3-a456-426614174000',
    'S3',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'Statement_February_2024.pdf',
    1706745600000,
    true,
    '{"productCode": "CC-PREMIUM-001", "productCategory": "CREDIT_CARD"}',
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Documents for REGULATORY_DISCLOSURE template (shared)
INSERT INTO storage_index (
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
) VALUES
(
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '22222222-2222-2222-2222-222222222222',
    1,
    'REGULATORY_DISCLOSURE',
    'D164',
    'DISCLOSURE_CODE',
    true,
    null,
    null,
    'S3',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    'Credit_Card_Disclosures_D164.pdf',
    1704067200000,
    true,
    '{"productCategory": "CREDIT_CARD", "regulatoryRegion": "US_WEST", "disclosures": ["TILA", "CARD_ACT", "CA_DISCLOSURE"]}',
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Documents for BRANCH_SPECIFIC_DOCUMENT template (shared)
INSERT INTO storage_index (
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
) VALUES
(
    'ffffffff-ffff-ffff-ffff-ffffffffffff',
    '33333333-3333-3333-3333-333333333333',
    1,
    'BRANCH_SPECIFIC_DOCUMENT',
    'BRANCH-DOC-001',
    'BRANCH_DOC_ID',
    true,
    null,
    null,
    'S3',
    '11111111-2222-3333-4444-555555555555',
    'West_Region_Compliance_Guide.pdf',
    1704067200000,
    true,
    '{"branchCode": "BR-555", "regionCode": "WEST", "complianceType": "BANKING"}',
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- ========================================
-- TEST ACCOUNT AND CUSTOMER IDS
-- ========================================
-- Use these IDs for testing:
-- Account ID: 550e8400-e29b-41d4-a716-446655440000
-- Customer ID: 123e4567-e89b-12d3-a456-426614174000
