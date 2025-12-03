-- ========================================
-- MOCK DATA FOR TESTING MULTI-STEP DATA EXTRACTION
-- PostgreSQL Version
-- ========================================

-- Clean up existing data
DELETE FROM storage_index;
DELETE FROM master_template_definition;

-- ========================================
-- TEST SCENARIO 1: Simple 3-Step Chain
-- Template: ACCOUNT_STATEMENT
-- Chain: Account → Product → Disclosure
-- ========================================

INSERT INTO master_template_definition (
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
    is_active,
    is_regulatory,
    is_message_center_doc,
    is_shared_document,
    sharing_scope,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    data_extraction_config
) VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'TMPL-001',
    'ACCOUNT_STATEMENT',
    'Monthly Account Statement',
    'Account Statement',
    'Monthly statement showing account activity',
    'STATEMENTS',
    'RETAIL_BANKING',
    'en',
    'OPERATIONS',
    false,
    true,
    false,
    true,
    false,
    null,
    1609459200000, -- 2021-01-01
    2051222400000, -- 2035-01-01
    'system',
    CURRENT_TIMESTAMP,
    '{
      "requiredFields": ["productCode", "productCategory", "disclosureRequirements"],
      "fieldSources": {
        "productCode": {
          "description": "Product code from account",
          "sourceApi": "accountDetailsApi",
          "extractionPath": "$.account.productCode",
          "requiredInputs": ["accountId"],
          "fieldType": "string",
          "defaultValue": "STANDARD"
        },
        "productCategory": {
          "description": "Product category from product catalog",
          "sourceApi": "productCatalogApi",
          "extractionPath": "$.product.category",
          "requiredInputs": ["productCode"],
          "fieldType": "string",
          "defaultValue": "GENERAL"
        },
        "disclosureRequirements": {
          "description": "Required disclosures for product category",
          "sourceApi": "regulatoryApi",
          "extractionPath": "$.rules.disclosures",
          "requiredInputs": ["productCategory"],
          "fieldType": "array",
          "defaultValue": []
        }
      },
      "dataSources": {
        "accountDetailsApi": {
          "description": "Get account details including product code",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/accounts/${accountId}/details",
            "method": "GET",
            "timeout": 5000,
            "headers": {
              "Authorization": "Bearer ${auth.token}"
            }
          },
          "cache": {
            "enabled": true,
            "ttlSeconds": 3600
          },
          "retry": {
            "maxAttempts": 2,
            "delayMs": 1000
          },
          "providesFields": ["productCode"]
        },
        "productCatalogApi": {
          "description": "Get product details from product code",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/products/${productCode}",
            "method": "GET",
            "timeout": 5000,
            "headers": {
              "Authorization": "Bearer ${auth.token}"
            }
          },
          "cache": {
            "enabled": true,
            "ttlSeconds": 86400
          },
          "retry": {
            "maxAttempts": 2,
            "delayMs": 1000
          },
          "providesFields": ["productCategory"]
        },
        "regulatoryApi": {
          "description": "Get regulatory requirements for product category",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/regulatory/requirements?category=${productCategory}",
            "method": "GET",
            "timeout": 5000,
            "headers": {
              "Authorization": "Bearer ${auth.token}"
            }
          },
          "cache": {
            "enabled": true,
            "ttlSeconds": 7200
          },
          "retry": {
            "maxAttempts": 2,
            "delayMs": 1000
          },
          "providesFields": ["disclosureRequirements"]
        }
      },
      "executionStrategy": {
        "mode": "auto",
        "continueOnError": false,
        "timeout": 20000
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
    is_active,
    is_regulatory,
    is_shared_document,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    data_extraction_config
) VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    1,
    'REGULATORY_DISCLOSURE',
    'Regulatory Disclosure Document',
    'Disclosure Document',
    'Regulatory required disclosure based on product and region',
    'REGULATORY',
    'COMPLIANCE',
    'en',
    true,
    true,
    true,
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '{
      "requiredFields": ["productCode", "customerLocation", "productCategory", "regulatoryRegion", "finalDisclosures"],
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
            "url": "http://localhost:8080/mock-api/accounts/${accountId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["productCode"]
        },
        "customerApi": {
          "description": "Level 1: Get customer info (parallel with accountApi)",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/customers/${customerId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["customerLocation"]
        },
        "productApi": {
          "description": "Level 2: Get product category",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/products/${productCode}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["productCategory"]
        },
        "regionMappingApi": {
          "description": "Level 2: Map location to region",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/regions/map",
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
            "url": "http://localhost:8080/mock-api/disclosures?category=${productCategory}&region=${regulatoryRegion}",
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
    }'::jsonb
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
    is_active,
    is_shared_document,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    data_extraction_config
) VALUES (
    '33333333-3333-3333-3333-333333333333'::uuid,
    1,
    'BRANCH_SPECIFIC_DOCUMENT',
    'Branch Specific Document',
    'Branch Document',
    'Document specific to branch compliance rules',
    true,
    true,
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '{
      "requiredFields": ["branchCode", "regionCode", "complianceRules", "documentList"],
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
            "url": "http://localhost:8080/mock-api/accounts/${accountId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["branchCode"]
        },
        "branchApi": {
          "description": "Step 2: Get branch details",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/branches/${branchCode}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["regionCode"]
        },
        "complianceApi": {
          "description": "Step 3: Get compliance rules",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/compliance/regions/${regionCode}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["complianceRules"]
        },
        "documentRulesApi": {
          "description": "Step 4: Get applicable documents",
          "endpoint": {
            "url": "http://localhost:8080/mock-api/document-rules",
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
    }'::jsonb
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
    is_shared,
    account_key,
    customer_key,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT',
    'STMT-2024-01',
    'STATEMENT_ID',
    false,
    '550e8400-e29b-41d4-a716-446655440000'::uuid,
    '123e4567-e89b-12d3-a456-426614174000'::uuid,
    'S3',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid,
    'Statement_January_2024.pdf',
    1704067200000,
    1,
    '{"productCode": "CC-PREMIUM-001", "productCategory": "CREDIT_CARD", "disclosureCode": "D164"}'::jsonb,
    'system',
    CURRENT_TIMESTAMP
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-bbbbbbbbbbbb'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT',
    'STMT-2024-02',
    'STATEMENT_ID',
    false,
    '550e8400-e29b-41d4-a716-446655440000'::uuid,
    '123e4567-e89b-12d3-a456-426614174000'::uuid,
    'S3',
    'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid,
    'Statement_February_2024.pdf',
    1706745600000,
    1,
    '{"productCode": "CC-PREMIUM-001", "productCategory": "CREDIT_CARD"}'::jsonb,
    'system',
    CURRENT_TIMESTAMP
);

-- Documents for REGULATORY_DISCLOSURE template (shared)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    is_shared,
    account_key,
    customer_key,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES
(
    'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    1,
    'REGULATORY_DISCLOSURE',
    'REG-DISC-2024',
    'DISCLOSURE_ID',
    true,
    null,
    null,
    'S3',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'::uuid,
    'Credit_Card_Disclosures_2024.pdf',
    1704067200000,
    1,
    '{"productCategory": "CREDIT_CARD", "regulatoryRegion": "US_WEST", "disclosures": ["TILA", "CARD_ACT", "CA_DISCLOSURE"]}'::jsonb,
    'system',
    CURRENT_TIMESTAMP
);

-- Documents for BRANCH_SPECIFIC_DOCUMENT template (shared)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    is_shared,
    account_key,
    customer_key,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES
(
    'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid,
    '33333333-3333-3333-3333-333333333333'::uuid,
    1,
    'BRANCH_SPECIFIC_DOCUMENT',
    'BRANCH-DOC-001',
    'BRANCH_DOC_ID',
    true,
    null,
    null,
    'S3',
    '11111111-2222-3333-4444-555555555555'::uuid,
    'West_Region_Compliance_Guide.pdf',
    1704067200000,
    1,
    '{"branchCode": "BR-555", "regionCode": "WEST", "complianceType": "BANKING"}'::jsonb,
    'system',
    CURRENT_TIMESTAMP
);

-- ========================================
-- VERIFICATION QUERIES
-- ========================================

-- Verify templates loaded
SELECT
    template_type,
    template_name,
    is_active,
    jsonb_array_length(data_extraction_config->'requiredFields') as required_fields_count
FROM master_template_definition
ORDER BY template_type;

-- Verify documents loaded
SELECT
    template_type,
    file_name,
    is_shared,
    account_key
FROM storage_index
ORDER BY template_type, is_shared;

-- ========================================
-- TEST ACCOUNT AND CUSTOMER IDS
-- ========================================
-- Use these IDs for testing:
-- Account ID: 550e8400-e29b-41d4-a716-446655440000
-- Customer ID: 123e4567-e89b-12d3-a456-426614174000

-- Test query to verify data_extraction_config
SELECT
    template_type,
    jsonb_pretty(data_extraction_config) as extraction_config
FROM master_template_definition
WHERE template_type = 'ACCOUNT_STATEMENT';
