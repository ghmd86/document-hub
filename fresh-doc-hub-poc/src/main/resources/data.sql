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
    is_active,
    is_shared_document,
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
    is_active,
    is_shared_document,
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
    is_active,
    is_shared_document,
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
    is_active,
    is_shared_document,
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
    is_active,
    is_shared_document,
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
