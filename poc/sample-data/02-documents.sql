-- Sample Document Data for Document Hub POC
-- Load order: Run this file AFTER 01-templates.sql
-- Database: PostgreSQL with JSONB support

-- This file creates sample documents that work with the 4 templates:
-- 1. MONTHLY_STATEMENT - Account-specific documents
-- 2. PRIVACY_POLICY - State-specific shared documents
-- 3. CARDHOLDER_AGREEMENT - Disclosure-code based documents
-- 4. VIP_LETTER - VIP customer-specific documents

---------------------------------------------------------------------------------------------------
-- SETUP: Create sample customers and accounts
---------------------------------------------------------------------------------------------------
-- Note: In a real system, these would come from customer/account services
-- For POC, we'll reference these IDs in the documents below

-- Sample Customer IDs:
-- C001 - Regular customer in California
-- C002 - Regular customer in New York
-- C003 - VIP customer in Texas
-- C004 - VIP customer in California

-- Sample Account IDs:
-- A001 - Basic account for C001
-- A002 - Premium account for C002
-- A003 - VIP account for C003
-- A004 - VIP account for C004

---------------------------------------------------------------------------------------------------
-- DOCUMENTS FOR TEMPLATE 1: Monthly Statements (Account-Specific)
---------------------------------------------------------------------------------------------------
-- These documents are tied to specific accounts and are retrieved using account-key matching
---------------------------------------------------------------------------------------------------

-- Monthly Statement for Account A001 (January 2024)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    account_key,
    customer_key,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a1111111-1111-1111-1111-111111111111'::uuid,
    1,
    'MONTHLY_STATEMENT',
    'A001',
    'C001',
    'STATEMENTS',
    'January 2024 Statement',
    'Monthly statement for account A001 - January 2024',
    's3://document-hub/statements/2024/01/A001-202401.pdf',
    'A001-202401.pdf',
    'application/pdf',
    1024567,
    false,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "statementDate": "2024-01-31",
        "statementPeriod": "2024-01",
        "accountNumber": "****1234",
        "balance": 1234.56,
        "minimumDue": 50.00
    }'::jsonb
);

-- Monthly Statement for Account A001 (February 2024)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    account_key,
    customer_key,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a1111111-1111-1111-1111-111111111111'::uuid,
    1,
    'MONTHLY_STATEMENT',
    'A001',
    'C001',
    'STATEMENTS',
    'February 2024 Statement',
    'Monthly statement for account A001 - February 2024',
    's3://document-hub/statements/2024/02/A001-202402.pdf',
    'A001-202402.pdf',
    'application/pdf',
    985432,
    false,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "statementDate": "2024-02-29",
        "statementPeriod": "2024-02",
        "accountNumber": "****1234",
        "balance": 2345.67,
        "minimumDue": 75.00
    }'::jsonb
);

-- Monthly Statement for Account A002
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    account_key,
    customer_key,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a1111111-1111-1111-1111-111111111111'::uuid,
    1,
    'MONTHLY_STATEMENT',
    'A002',
    'C002',
    'STATEMENTS',
    'February 2024 Statement',
    'Monthly statement for account A002 - February 2024',
    's3://document-hub/statements/2024/02/A002-202402.pdf',
    'A002-202402.pdf',
    'application/pdf',
    1156789,
    false,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "statementDate": "2024-02-29",
        "statementPeriod": "2024-02",
        "accountNumber": "****5678",
        "balance": 5432.10,
        "minimumDue": 100.00
    }'::jsonb
);

---------------------------------------------------------------------------------------------------
-- DOCUMENTS FOR TEMPLATE 2: Privacy Policies (State-Specific Shared Documents)
---------------------------------------------------------------------------------------------------
-- These are shared documents matched by state in metadata
-- is_shared = true means one document applies to many customers
---------------------------------------------------------------------------------------------------

-- California Privacy Policy
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a2222222-2222-2222-2222-222222222222'::uuid,
    1,
    'PRIVACY_POLICY',
    'REGULATORY',
    'California Privacy Policy',
    'CCPA-compliant privacy policy for California residents',
    's3://document-hub/regulatory/privacy/CA-Privacy-Policy-2024.pdf',
    'CA-Privacy-Policy-2024.pdf',
    'application/pdf',
    2567890,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "state": "CA",
        "country": "US",
        "regulationType": "CCPA",
        "effectiveDate": "2024-01-01",
        "version": "2024.1"
    }'::jsonb
);

-- New York Privacy Policy
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a2222222-2222-2222-2222-222222222222'::uuid,
    1,
    'PRIVACY_POLICY',
    'REGULATORY',
    'New York Privacy Policy',
    'NY SHIELD Act compliant privacy policy for New York residents',
    's3://document-hub/regulatory/privacy/NY-Privacy-Policy-2024.pdf',
    'NY-Privacy-Policy-2024.pdf',
    'application/pdf',
    2123456,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "state": "NY",
        "country": "US",
        "regulationType": "NY_SHIELD",
        "effectiveDate": "2024-01-01",
        "version": "2024.1"
    }'::jsonb
);

-- Texas Privacy Policy
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a2222222-2222-2222-2222-222222222222'::uuid,
    1,
    'PRIVACY_POLICY',
    'REGULATORY',
    'Texas Privacy Policy',
    'Standard privacy policy for Texas residents',
    's3://document-hub/regulatory/privacy/TX-Privacy-Policy-2024.pdf',
    'TX-Privacy-Policy-2024.pdf',
    'application/pdf',
    1987654,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "state": "TX",
        "country": "US",
        "regulationType": "STANDARD",
        "effectiveDate": "2024-01-01",
        "version": "2024.1"
    }'::jsonb
);

---------------------------------------------------------------------------------------------------
-- DOCUMENTS FOR TEMPLATE 3: Cardholder Agreements (Disclosure Code-Based)
---------------------------------------------------------------------------------------------------
-- These documents are matched by reference_key (disclosure code)
-- The template uses a 2-step chain: Account → Pricing → Disclosure Code
---------------------------------------------------------------------------------------------------

-- Standard Cardholder Agreement (Disclosure Code D001)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a3333333-3333-3333-3333-333333333333'::uuid,
    1,
    'CARDHOLDER_AGREEMENT',
    'D001',
    'DISCLOSURE_CODE',
    'AGREEMENTS',
    'Standard Cardholder Agreement',
    'Cardholder agreement for standard pricing tier',
    's3://document-hub/agreements/cardholder/D001-Standard-Agreement.pdf',
    'D001-Standard-Agreement.pdf',
    'application/pdf',
    3456789,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "disclosureCode": "D001",
        "pricingTier": "STANDARD",
        "apr": 19.99,
        "annualFee": 0,
        "version": "2024.1"
    }'::jsonb
);

-- Premium Cardholder Agreement (Disclosure Code D002)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a3333333-3333-3333-3333-333333333333'::uuid,
    1,
    'CARDHOLDER_AGREEMENT',
    'D002',
    'DISCLOSURE_CODE',
    'AGREEMENTS',
    'Premium Cardholder Agreement',
    'Cardholder agreement for premium pricing tier',
    's3://document-hub/agreements/cardholder/D002-Premium-Agreement.pdf',
    'D002-Premium-Agreement.pdf',
    'application/pdf',
    3567890,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "disclosureCode": "D002",
        "pricingTier": "PREMIUM",
        "apr": 15.99,
        "annualFee": 95,
        "version": "2024.1"
    }'::jsonb
);

-- VIP Cardholder Agreement (Disclosure Code D003)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    reference_key,
    reference_key_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a3333333-3333-3333-3333-333333333333'::uuid,
    1,
    'CARDHOLDER_AGREEMENT',
    'D003',
    'DISCLOSURE_CODE',
    'AGREEMENTS',
    'VIP Cardholder Agreement',
    'Cardholder agreement for VIP pricing tier',
    's3://document-hub/agreements/cardholder/D003-VIP-Agreement.pdf',
    'D003-VIP-Agreement.pdf',
    'application/pdf',
    3678901,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "disclosureCode": "D003",
        "pricingTier": "VIP",
        "apr": 11.99,
        "annualFee": 495,
        "version": "2024.1"
    }'::jsonb
);

---------------------------------------------------------------------------------------------------
-- DOCUMENTS FOR TEMPLATE 4: VIP Letters (Composite Matching - Tier + Offer)
---------------------------------------------------------------------------------------------------
-- These documents are matched by multiple metadata fields (tier and offerCode)
-- The template uses a 3-step chain: Customer → VIP Benefits → Personalized Content
---------------------------------------------------------------------------------------------------

-- VIP Welcome Letter (Summer 2024 Offer)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    customer_key,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a4444444-4444-4444-4444-444444444444'::uuid,
    1,
    'VIP_LETTER',
    'C003',
    'MARKETING',
    'VIP Summer Welcome Letter',
    'Personalized VIP welcome letter with summer 2024 offer',
    's3://document-hub/marketing/vip/VIP-Summer-2024-C003.pdf',
    'VIP-Summer-2024-C003.pdf',
    'application/pdf',
    987654,
    false,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "tier": "VIP",
        "offerCode": "SUMMER2024",
        "offerDescription": "Triple points on travel",
        "expiryDate": "2024-09-30",
        "personalized": true
    }'::jsonb
);

-- VIP Travel Rewards Letter (Fall 2024 Offer)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    customer_key,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a4444444-4444-4444-4444-444444444444'::uuid,
    1,
    'VIP_LETTER',
    'C004',
    'MARKETING',
    'VIP Fall Travel Letter',
    'Personalized VIP letter with fall 2024 travel rewards',
    's3://document-hub/marketing/vip/VIP-Fall-2024-C004.pdf',
    'VIP-Fall-2024-C004.pdf',
    'application/pdf',
    1023456,
    false,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "tier": "VIP",
        "offerCode": "FALL2024",
        "offerDescription": "Bonus points for Q4 spending",
        "expiryDate": "2024-12-31",
        "personalized": true
    }'::jsonb
);

-- VIP Exclusive Benefits Letter (General)
INSERT INTO storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    document_category,
    document_name,
    document_description,
    storage_location,
    file_name,
    file_type,
    file_size_bytes,
    is_shared,
    created_by,
    created_timestamp,
    doc_metadata
) VALUES (
    gen_random_uuid(),
    'a4444444-4444-4444-4444-444444444444'::uuid,
    1,
    'VIP_LETTER',
    'MARKETING',
    'VIP Exclusive Benefits Guide',
    'General VIP benefits and services guide',
    's3://document-hub/marketing/vip/VIP-Benefits-Guide-2024.pdf',
    'VIP-Benefits-Guide-2024.pdf',
    'application/pdf',
    2456789,
    true,
    'system',
    CURRENT_TIMESTAMP,
    '{
        "tier": "VIP",
        "offerCode": "GENERAL2024",
        "offerDescription": "Year-round VIP benefits",
        "expiryDate": "2024-12-31",
        "personalized": false
    }'::jsonb
);

---------------------------------------------------------------------------------------------------
-- VERIFICATION QUERIES
---------------------------------------------------------------------------------------------------

-- Count documents by template type
SELECT
    template_type,
    COUNT(*) as document_count,
    SUM(CASE WHEN is_shared THEN 1 ELSE 0 END) as shared_count,
    SUM(CASE WHEN is_shared THEN 0 ELSE 1 END) as customer_specific_count
FROM storage_index
WHERE template_type IN ('MONTHLY_STATEMENT', 'PRIVACY_POLICY', 'CARDHOLDER_AGREEMENT', 'VIP_LETTER')
GROUP BY template_type
ORDER BY template_type;

-- Verify documents by category
SELECT
    document_category,
    COUNT(*) as document_count
FROM storage_index
GROUP BY document_category
ORDER BY document_category;

-- Show sample of each document type
SELECT
    template_type,
    document_name,
    is_shared,
    CASE
        WHEN account_key IS NOT NULL THEN 'Account: ' || account_key
        WHEN customer_key IS NOT NULL THEN 'Customer: ' || customer_key
        WHEN reference_key IS NOT NULL THEN 'Reference: ' || reference_key
        ELSE 'Shared'
    END as association
FROM storage_index
WHERE template_type IN ('MONTHLY_STATEMENT', 'PRIVACY_POLICY', 'CARDHOLDER_AGREEMENT', 'VIP_LETTER')
ORDER BY template_type, document_name;
