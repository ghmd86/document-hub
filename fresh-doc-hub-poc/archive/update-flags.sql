-- ========================================
-- Update Script for active_flag and shared_document_flag
-- Run this script to update the database with correct boolean values
-- ========================================

-- First, ensure the database schema is set up correctly
-- The schema should already have these columns as boolean type

-- Clear existing data (optional - comment out if you want to keep existing data)
TRUNCATE TABLE document_hub.storage_index CASCADE;
TRUNCATE TABLE document_hub.master_template_definition CASCADE;

-- ========================================
-- Insert Master Template Definitions with boolean flags
-- ========================================

-- Template 1: ACCOUNT_STATEMENT (not shared, active)
INSERT INTO document_hub.master_template_definition (
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
    record_status
) VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT',
    'Monthly Account Statement',
    'Account Statement',
    'Monthly statement showing account activity',
    true,  -- active_flag as boolean
    false, -- shared_document_flag as boolean
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Template 2: REGULATORY_DISCLOSURE (shared, active)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    display_name,
    template_description,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    record_status
) VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    1,
    'REGULATORY_DISCLOSURE',
    'Regulatory Disclosure Document',
    'Disclosure Document',
    'Regulatory required disclosure',
    true,  -- active_flag as boolean
    true,  -- shared_document_flag as boolean
    'ALL',  -- Shared with all
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Template 3: BRANCH_SPECIFIC_DOCUMENT (shared, active)
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    display_name,
    template_description,
    active_flag,
    shared_document_flag,
    sharing_scope,
    start_date,
    end_date,
    created_by,
    created_timestamp,
    record_status
) VALUES (
    '33333333-3333-3333-3333-333333333333'::uuid,
    1,
    'BRANCH_SPECIFIC_DOCUMENT',
    'Branch Specific Document',
    'Branch Document',
    'Document specific to branch',
    true,  -- active_flag as boolean
    true,  -- shared_document_flag as boolean
    'ALL',  -- Shared with all
    1609459200000,
    2051222400000,
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- ========================================
-- Insert Storage Index (Documents)
-- ========================================

-- Account-specific document
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
    created_by,
    created_timestamp,
    record_status
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    1,
    'ACCOUNT_STATEMENT',
    'STMT-2024-01',
    'STATEMENT_ID',
    false,  -- shared_flag as boolean (not shared)
    '550e8400-e29b-41d4-a716-446655440000'::uuid,
    '123e4567-e89b-12d3-a456-426614174000'::uuid,
    'S3',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid,
    'Statement_January_2024.pdf',
    1704067200000,
    true,  -- accessible_flag as boolean
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Shared document 1
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
    created_by,
    created_timestamp,
    record_status
) VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    1,
    'REGULATORY_DISCLOSURE',
    'REG-DISC-2024',
    'DISCLOSURE_ID',
    true,  -- shared_flag as boolean (shared)
    'S3',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'::uuid,
    'Credit_Card_Disclosures_2024.pdf',
    1704067200000,
    true,  -- accessible_flag as boolean
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- Shared document 2
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
    created_by,
    created_timestamp,
    record_status
) VALUES (
    'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid,
    '33333333-3333-3333-3333-333333333333'::uuid,
    1,
    'BRANCH_SPECIFIC_DOCUMENT',
    'BRANCH-DOC-001',
    'BRANCH_DOC_ID',
    true,  -- shared_flag as boolean (shared)
    'S3',
    '11111111-2222-3333-4444-555555555555'::uuid,
    'West_Region_Compliance_Guide.pdf',
    1704067200000,
    true,  -- accessible_flag as boolean
    'system',
    CURRENT_TIMESTAMP,
    '1'
);

-- ========================================
-- Verification Queries
-- ========================================

-- Check templates with boolean flags
SELECT
    template_type,
    template_name,
    active_flag,
    shared_document_flag,
    sharing_scope
FROM document_hub.master_template_definition
ORDER BY template_type;

-- Check documents
SELECT
    template_type,
    file_name,
    shared_flag,
    account_key
FROM document_hub.storage_index
ORDER BY template_type, shared_flag;
