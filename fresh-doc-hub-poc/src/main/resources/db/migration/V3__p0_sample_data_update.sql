-- ====================================================================
-- Document Hub - P0 Sample Data Update
-- ====================================================================
-- Version: V3
-- Date: December 2024
-- Description: Updates existing sample data with new P0 column values
-- Depends on: V2__p0_schema_updates.sql
-- ====================================================================

-- ====================================================================
-- Update Master Template Definitions with P0 fields
-- ====================================================================

-- Template 1: Privacy Policy (UUID: 11111111-1111-1111-1111-111111111111)
-- Enterprise shared document - LETTER communication, 4_EYES (regulatory)
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '4_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '11111111-1111-1111-1111-111111111111';

-- Template 2: Credit Card Statement (UUID: 22222222-2222-2222-2222-222222222222)
-- Account-specific - LETTER communication, 2_EYES, MULTIPLE documents allowed
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '2_EYES',
    single_document_flag = false,  -- Statements can have multiple per account
    message_center_doc_flag = true
WHERE master_template_id = '22222222-2222-2222-2222-222222222222';

-- Template 3: Balance Transfer Offer (UUID: 33333333-3333-3333-3333-333333333333)
-- Promotional - LETTER communication, 2_EYES, single document
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '33333333-3333-3333-3333-333333333333';

-- Template 4: VIP Exclusive Offer (UUID: 44444444-4444-4444-4444-444444444444)
-- Custom rules - LETTER communication, 2_EYES, single document
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '44444444-4444-4444-4444-444444444444';

-- Template 5: Regional Disclosure (UUID: 55555555-5555-5555-5555-555555555555)
-- Disclosure with data extraction - LETTER, 4_EYES (regulatory), single
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '4_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '55555555-5555-5555-5555-555555555555';

-- Template 6: Digital Bank Statement (UUID: 66666666-6666-6666-6666-666666666666)
-- Digital bank specific - LETTER, 2_EYES, MULTIPLE documents
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '2_EYES',
    single_document_flag = false,  -- Statements have multiple
    message_center_doc_flag = true
WHERE master_template_id = '66666666-6666-6666-6666-666666666666';

-- Template 7: Account Terms and Conditions (UUID: 77777777-7777-7777-7777-777777777777)
-- T&C document - LETTER, 4_EYES (regulatory), single
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '4_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '77777777-7777-7777-7777-777777777777';

-- Template 8: Fee Schedule (UUID: 88888888-8888-8888-8888-888888888888)
-- Fee schedule - LETTER, 4_EYES (regulatory), single
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '4_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '88888888-8888-8888-8888-888888888888';

-- Template 9: Credit Line Increase (UUID: 99999999-9999-9999-9999-999999999999)
-- CLI letter - LETTER, 2_EYES, single
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = '99999999-9999-9999-9999-999999999999';

-- Template 10: Payment Confirmation (UUID: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa)
-- Payment letter - LETTER, 2_EYES, single
UPDATE document_hub.master_template_definition
SET
    communication_type = 'LETTER',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = true
WHERE master_template_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';


-- ====================================================================
-- Add New Templates for Different Communication Types
-- ====================================================================

-- Template: Email Welcome Letter (EMAIL communication type)
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
    communication_type,
    workflow,
    single_document_flag,
    message_center_doc_flag,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1',
    1,
    'Email Welcome Letter',
    'Welcome email sent to new customers',
    'Welcome',
    'CREDIT_CARD',
    'CustomerLetter',
    'EN_US',
    true,
    false,
    NULL,
    'EMAIL',
    '2_EYES',
    true,
    false,  -- Email welcome is NOT shown in message center
    1704067200000,
    'system',
    NOW()
) ON CONFLICT (master_template_id, template_version) DO UPDATE
SET
    communication_type = 'EMAIL',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = false;


-- Template: SMS Payment Reminder (SMS communication type)
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
    communication_type,
    workflow,
    single_document_flag,
    message_center_doc_flag,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    's1s1s1s1-s1s1-s1s1-s1s1-s1s1s1s1s1s1',
    1,
    'SMS Payment Reminder',
    'SMS reminder for upcoming payment due',
    'Reminder',
    'CREDIT_CARD',
    'PaymentLetter',
    'EN_US',
    true,
    false,
    NULL,
    'SMS',
    '2_EYES',
    true,
    false,  -- SMS is NOT shown in message center
    1704067200000,
    'system',
    NOW()
) ON CONFLICT (master_template_id, template_version) DO UPDATE
SET
    communication_type = 'SMS',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = false;


-- Template: Push Notification Alert (PUSH communication type)
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
    communication_type,
    workflow,
    single_document_flag,
    message_center_doc_flag,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'p1p1p1p1-p1p1-p1p1-p1p1-p1p1p1p1p1p1',
    1,
    'Push Transaction Alert',
    'Push notification for transaction alerts',
    'Alert',
    'CREDIT_CARD',
    'FraudLetter',
    'EN_US',
    true,
    false,
    NULL,
    'PUSH',
    '2_EYES',
    true,
    false,  -- Push notifications are NOT shown in message center
    1704067200000,
    'system',
    NOW()
) ON CONFLICT (master_template_id, template_version) DO UPDATE
SET
    communication_type = 'PUSH',
    workflow = '2_EYES',
    single_document_flag = true,
    message_center_doc_flag = false;


-- Template: Internal System Document (NOT message center)
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
    communication_type,
    workflow,
    single_document_flag,
    message_center_doc_flag,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'i1i1i1i1-i1i1-i1i1-i1i1-i1i1i1i1i1i1',
    1,
    'Internal Audit Report',
    'Internal document for audit purposes - not visible in message center',
    'Internal',
    'ENTERPRISE',
    'MiscellaneousLetter',
    'EN_US',
    true,
    false,
    NULL,
    'LETTER',
    '4_EYES',
    true,
    false,  -- Internal doc NOT shown in message center
    1704067200000,
    'system',
    NOW()
) ON CONFLICT (master_template_id, template_version) DO UPDATE
SET
    communication_type = 'LETTER',
    workflow = '4_EYES',
    single_document_flag = true,
    message_center_doc_flag = false;


-- ====================================================================
-- Update Storage Index with doc_creation_date
-- ====================================================================

-- Ensure all storage_index records have doc_creation_date populated
-- Using different dates to test date range filtering

-- Statement documents - different months
UPDATE document_hub.storage_index
SET doc_creation_date = 1704067200000  -- Jan 1, 2024
WHERE storage_index_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd';

UPDATE document_hub.storage_index
SET doc_creation_date = 1706745600000  -- Feb 1, 2024
WHERE storage_index_id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';

-- Privacy Policy - effective from Jan 2024
UPDATE document_hub.storage_index
SET doc_creation_date = 1704067200000  -- Jan 1, 2024
WHERE template_type = 'PrivacyPolicy' AND doc_creation_date IS NULL;

-- Default: Set to created_timestamp for any remaining NULL values
UPDATE document_hub.storage_index
SET doc_creation_date = EXTRACT(EPOCH FROM created_timestamp)::BIGINT * 1000
WHERE doc_creation_date IS NULL;


-- ====================================================================
-- Add Sample Documents for New Templates
-- ====================================================================

-- Email Welcome Letter document
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    storage_vendor,
    reference_key,
    reference_key_type,
    account_key,
    customer_key,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    shared_flag,
    created_by,
    created_timestamp
) VALUES (
    'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
    'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1',
    1,
    'CustomerLetter',
    'S3',
    'ACC-001',
    'ACCOUNT_ID',
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f2',
    'welcome_email_001.html',
    1704067200000,
    true,
    false,
    'system',
    NOW()
) ON CONFLICT (storage_index_id) DO NOTHING;


-- ====================================================================
-- Verification Queries
-- ====================================================================
-- Run these manually to verify the data update:

-- Check template communication types
-- SELECT master_template_id, template_name, communication_type, workflow,
--        single_document_flag, message_center_doc_flag
-- FROM document_hub.master_template_definition
-- ORDER BY communication_type, template_name;

-- Check message center flag distribution
-- SELECT message_center_doc_flag, COUNT(*) as count
-- FROM document_hub.master_template_definition
-- GROUP BY message_center_doc_flag;

-- Check communication type distribution
-- SELECT communication_type, COUNT(*) as count
-- FROM document_hub.master_template_definition
-- GROUP BY communication_type;

-- Check doc_creation_date population
-- SELECT storage_index_id, template_type, doc_creation_date, created_timestamp
-- FROM document_hub.storage_index
-- LIMIT 10;


-- ====================================================================
-- Migration Complete
-- ====================================================================
-- Updated:
--   - 10 existing templates with new P0 fields
--   - Added 4 new templates for different communication types
--   - Added sample documents for new templates
--   - Backfilled doc_creation_date in storage_index
-- ====================================================================
