-- ====================================================================
-- Document Hub POC - Database Initialization Script
-- ====================================================================
-- This script creates the schema and sample data for testing the
-- document enquiry endpoint with various sharing scenarios
-- ====================================================================

-- Clean up existing data (for dev/testing)
TRUNCATE TABLE storage_index CASCADE;
TRUNCATE TABLE master_template_definition CASCADE;

-- ====================================================================
-- SAMPLE DATA: Master Template Definitions
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
    is_active,
    is_shared_document,
    sharing_scope,
    access_control,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'Privacy Policy 2024',
    '2024 Privacy Policy - Accessible to all customers',
    'Privacy',
    'ALL',
    'PrivacyPolicy',
    'EN_US',
    true,
    true,
    'ALL',
    '{"eligibilityCriteria": {"operator": "AND", "rules": []}}'::jsonb,
    '{"documentMatchingStrategy": {"matchByTemplate": true}}'::jsonb,
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-01')::bigint,
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
    is_active,
    is_shared_document,
    sharing_scope,
    access_control,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
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
    '{"eligibilityCriteria": {"operator": "AND", "rules": []}}'::jsonb,
    '{"documentMatchingStrategy": {"matchByAccount": true}}'::jsonb,
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-01')::bigint,
    'system',
    NOW()
);

-- Template 3: Credit Card Promotional Offer (Shared by Account Type)
INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    is_active,
    is_shared_document,
    sharing_scope,
    access_control,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '33333333-3333-3333-3333-333333333333'::uuid,
    1,
    'Credit Card Balance Transfer Offer',
    'Special balance transfer offer for credit card customers',
    'Promotional',
    'CREDIT_CARD',
    'BalanceTransferLetter',
    'EN_US',
    true,
    true,
    'ACCOUNT_TYPE',
    '{"eligibilityCriteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "IN", "value": ["credit_card"]}]}}'::jsonb,
    '{"documentMatchingStrategy": {"matchByTemplate": true}}'::jsonb,
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-01')::bigint,
    'system',
    NOW()
);

-- Template 4: VIP Customer Offer (Shared with Custom Rules)
INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    is_active,
    is_shared_document,
    sharing_scope,
    access_control,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '44444444-4444-4444-4444-444444444444'::uuid,
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
                {
                    "field": "customerSegment",
                    "operator": "EQUALS",
                    "value": "VIP"
                },
                {
                    "field": "region",
                    "operator": "EQUALS",
                    "value": "US_WEST"
                }
            ]
        }
    }'::jsonb,
    '{"documentMatchingStrategy": {"matchByTemplate": true}}'::jsonb,
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-01')::bigint,
    'system',
    NOW()
);

-- Template 5: Disclosure Document (Shared with Custom Rules - Disclosure Code Match)
INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_name,
    template_description,
    template_category,
    line_of_business,
    template_type,
    language_code,
    is_active,
    is_shared_document,
    sharing_scope,
    access_control,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '55555555-5555-5555-5555-555555555555'::uuid,
    1,
    'Regulatory Disclosure D164',
    'Regulatory disclosure for specific disclosure codes',
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
                {
                    "field": "$metadata.disclosure_code",
                    "operator": "EQUALS",
                    "value": "D164"
                }
            ]
        }
    }'::jsonb,
    '{"documentMatchingStrategy": {"matchByTemplate": true, "metadataMatch": {"disclosure_code": "$request.referenceKey"}}}'::jsonb,
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-01')::bigint,
    'system',
    NOW()
);

-- ====================================================================
-- SAMPLE DATA: Storage Index (Documents)
-- ====================================================================

-- Document 1: Privacy Policy (Shared with ALL)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    "template_version ",
    template_type,
    is_shared,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000001'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'PrivacyPolicy',
    true,
    'S3',
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'privacy_policy_2024.pdf',
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-15')::bigint,
    1,
    '{"document_type": "privacy_policy", "version": "2024.1"}'::jsonb,
    'system',
    NOW()
);

-- Document 2: Account-Specific Statement for Customer 1, Account 1
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    "template_version ",
    template_type,
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
) VALUES (
    'a0000000-0000-0000-0000-000000000002'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    1,
    'Statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001'::uuid,
    'cccc0000-0000-0000-0000-000000000001'::uuid,
    'S3',
    'b0000000-0000-0000-0000-000000000002'::uuid,
    'statement_202401.pdf',
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-31')::bigint,
    1,
    '{"statement_date": "2024-01-31", "account_id": "aaaa0000-0000-0000-0000-000000000001"}'::jsonb,
    'system',
    NOW()
);

-- Document 3: Account-Specific Statement for Customer 1, Account 2
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    "template_version ",
    template_type,
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
) VALUES (
    'a0000000-0000-0000-0000-000000000003'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    1,
    'Statement',
    false,
    'aaaa0000-0000-0000-0000-000000000002'::uuid,
    'cccc0000-0000-0000-0000-000000000001'::uuid,
    'S3',
    'b0000000-0000-0000-0000-000000000003'::uuid,
    'statement_202401_account2.pdf',
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-31')::bigint,
    1,
    '{"statement_date": "2024-01-31", "account_id": "aaaa0000-0000-0000-0000-000000000002"}'::jsonb,
    'system',
    NOW()
);

-- Document 4: Credit Card Promotional Offer (Shared by Account Type)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    "template_version ",
    template_type,
    is_shared,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000004'::uuid,
    '33333333-3333-3333-3333-333333333333'::uuid,
    1,
    'BalanceTransferLetter',
    true,
    'S3',
    'b0000000-0000-0000-0000-000000000004'::uuid,
    'balance_transfer_offer_q1_2024.pdf',
    EXTRACT(EPOCH FROM TIMESTAMP '2024-02-01')::bigint,
    1,
    '{"offer_code": "BT2024Q1", "account_type": "credit_card"}'::jsonb,
    'system',
    NOW()
);

-- Document 5: VIP Exclusive Offer (Shared with Custom Rules)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    "template_version ",
    template_type,
    is_shared,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000005'::uuid,
    '44444444-4444-4444-4444-444444444444'::uuid,
    1,
    'CustomerLetter',
    true,
    'S3',
    'b0000000-0000-0000-0000-000000000005'::uuid,
    'vip_exclusive_offer.pdf',
    EXTRACT(EPOCH FROM TIMESTAMP '2024-02-15')::bigint,
    1,
    '{"customer_segment": "VIP", "region": "US_WEST"}'::jsonb,
    'system',
    NOW()
);

-- Document 6: Disclosure Document D164 (Shared with Custom Rules)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    "template_version ",
    template_type,
    is_shared,
    storage_vendor,
    storage_document_key,
    file_name,
    reference_key,
    reference_key_type,
    doc_creation_date,
    is_accessible,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000006'::uuid,
    '55555555-5555-5555-5555-555555555555'::uuid,
    1,
    'ElectronicDisclosure',
    true,
    'S3',
    'b0000000-0000-0000-0000-000000000006'::uuid,
    'disclosure_d164.pdf',
    'D164',
    'DISCLOSURE_CODE',
    EXTRACT(EPOCH FROM TIMESTAMP '2024-01-10')::bigint,
    1,
    '{"disclosure_code": "D164", "regulatory_type": "ESIGN"}'::jsonb,
    'system',
    NOW()
);

-- ====================================================================
-- VERIFICATION QUERIES
-- ====================================================================

-- Verify templates
SELECT
    template_type,
    is_shared_document,
    sharing_scope,
    template_name
FROM master_template_definition
ORDER BY template_type;

-- Verify documents
SELECT
    template_type,
    is_shared,
    account_key,
    file_name,
    doc_metadata
FROM storage_index
ORDER BY template_type;
