-- ============================================================
-- V5: Sample Vendor Mapping Data
-- Demonstrates different vendor types and configurations
-- ============================================================

-- ============================================================
-- 1. Print Vendors
-- ============================================================

-- Smartcom - Primary US Print Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    health_check_endpoint,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'a1111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',  -- Privacy Policy template
    1,
    'Smartcom',
    'SMT-PP-001',
    'Privacy Policy Letter',
    'PRINT',
    1,  -- Primary
    ARRAY['US', 'CA'],
    'ACTIVE',
    true,
    true,
    60000,  -- 60 second timeout for print jobs
    3,
    2000,
    0.55,
    'PER_DOCUMENT',
    ARRAY['PDF', 'POSTSCRIPT'],
    'https://api.smartcom.example/health',
    '{"mailClass": "FIRST_CLASS", "returnAddress": "PO Box 123, City, ST 12345", "trackingEnabled": true}',
    '{"baseUrl": "https://api.smartcom.example/v1", "authType": "API_KEY", "credentialsPath": "vault:///secrets/smartcom"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- Sentista - Backup US Print Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    health_check_endpoint,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'a2222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',  -- Privacy Policy template
    1,
    'Sentista',
    'SEN-PP-001',
    'Privacy Policy Letter Backup',
    'PRINT',
    2,  -- First fallback
    ARRAY['US'],
    'ACTIVE',
    false,
    true,
    45000,
    3,
    1000,
    0.65,  -- Slightly more expensive backup
    'PER_DOCUMENT',
    ARRAY['PDF'],
    'https://api.sentista.example/health',
    '{"mailClass": "FIRST_CLASS", "trackingEnabled": true}',
    '{"baseUrl": "https://api.sentista.example/v2", "authType": "OAUTH2", "credentialsPath": "vault:///secrets/sentista"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- International Print Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'a3333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    1,
    'GlobalPrint',
    'GP-PP-001',
    'Privacy Policy International',
    'PRINT',
    1,  -- Primary for international
    ARRAY['UK', 'EU', 'INTL'],
    'ACTIVE',
    false,  -- Not primary overall, but primary for INTL
    true,
    90000,  -- Longer timeout for international
    3,
    3000,
    1.25,
    'PER_DOCUMENT',
    ARRAY['PDF', 'POSTSCRIPT'],
    '{"mailClass": "INTERNATIONAL_STANDARD", "trackingEnabled": true}',
    '{"baseUrl": "https://api.globalprint.example/v1", "authType": "API_KEY", "credentialsPath": "vault:///secrets/globalprint"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- ============================================================
-- 2. Email Vendors
-- ============================================================

-- SendGrid - Primary Email Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    rate_limit_per_minute,
    rate_limit_per_day,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    health_check_endpoint,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'b1111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    1,
    'SendGrid',
    'SG-PP-001',
    'Privacy Policy Email',
    'EMAIL',
    1,
    ARRAY['US', 'CA', 'UK', 'EU', 'INTL'],
    'ACTIVE',
    true,
    true,
    1000,  -- 1000 emails per minute
    100000,  -- 100k per day
    30000,
    3,
    1000,
    0.001,
    'PER_MESSAGE',
    ARRAY['HTML', 'TEXT'],
    'https://api.sendgrid.com/v3/status',
    '{"fromAddress": "noreply@company.com", "replyTo": "support@company.com", "trackingEnabled": true, "unsubscribeGroup": 12345}',
    '{"baseUrl": "https://api.sendgrid.com/v3", "authType": "API_KEY", "credentialsPath": "vault:///secrets/sendgrid"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- Salesforce Marketing Cloud - Backup Email Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    rate_limit_per_minute,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'b2222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    1,
    'Salesforce',
    'SFMC-PP-001',
    'Privacy Policy Email SFMC',
    'EMAIL',
    2,  -- Fallback
    ARRAY['US', 'CA', 'UK', 'EU', 'INTL'],
    'ACTIVE',
    false,
    true,
    500,
    45000,
    3,
    2000,
    0.002,
    'PER_MESSAGE',
    ARRAY['HTML', 'TEXT', 'AMP'],
    '{"fromAddress": "noreply@company.com", "businessUnit": "BU123", "trackingEnabled": true}',
    '{"baseUrl": "https://mc.exacttarget.com/v1", "authType": "OAUTH2", "credentialsPath": "vault:///secrets/salesforce"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- ============================================================
-- 3. SMS Vendors
-- ============================================================

-- Twilio - Primary SMS Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    rate_limit_per_minute,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'c1111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    1,
    'Twilio',
    'TWL-PP-001',
    'Privacy Policy SMS',
    'SMS',
    1,
    ARRAY['US', 'CA'],
    'ACTIVE',
    true,
    true,
    100,  -- 100 SMS per minute
    15000,
    3,
    500,
    0.0075,
    'PER_MESSAGE',
    ARRAY['TEXT'],
    '{"fromNumber": "+18001234567", "messagingServiceSid": "MG123", "statusCallback": "https://api.company.com/webhooks/sms"}',
    '{"baseUrl": "https://api.twilio.com/2010-04-01", "authType": "BASIC", "credentialsPath": "vault:///secrets/twilio"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- ============================================================
-- 4. Push Notification Vendors
-- ============================================================

-- Firebase Cloud Messaging - Primary Push Vendor
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    rate_limit_per_minute,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'd1111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    1,
    'Firebase',
    'FCM-PP-001',
    'Privacy Policy Push',
    'PUSH',
    1,
    ARRAY['US', 'CA', 'UK', 'EU', 'INTL'],
    'ACTIVE',
    true,
    true,
    500,
    10000,
    3,
    500,
    0.00,  -- Free tier
    'PER_MESSAGE',
    ARRAY['JSON'],
    '{"projectId": "company-app-prod", "defaultIcon": "notification_icon", "defaultSound": "default"}',
    '{"baseUrl": "https://fcm.googleapis.com/v1", "authType": "OAUTH2", "credentialsPath": "vault:///secrets/firebase"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- ============================================================
-- 5. Document Generation Vendors
-- ============================================================

-- Internal Generation Service
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    supported_formats,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'e1111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',  -- Statement template
    1,
    'InternalDocGen',
    'IDG-STMT-001',
    'Statement Generator',
    'GENERATION',
    1,
    ARRAY['US', 'CA', 'UK', 'EU', 'INTL'],
    'ACTIVE',
    true,
    true,
    120000,  -- 2 minute timeout for complex documents
    3,
    5000,
    ARRAY['PDF', 'HTML', 'DOCX'],
    '{"outputFormat": "PDF", "compression": "HIGH", "watermark": false}',
    '{"baseUrl": "https://docgen.internal.company.com/v1", "authType": "API_KEY", "credentialsPath": "vault:///secrets/docgen"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- ============================================================
-- 6. Storage Vendors
-- ============================================================

-- S3 Storage
INSERT INTO document_hub.template_vendor_mapping (
    template_vendor_id,
    master_template_id,
    template_version,
    vendor,
    vendor_template_key,
    vendor_template_name,
    vendor_type,
    priority_order,
    supported_regions,
    vendor_status,
    primary_flag,
    active_flag,
    timeout_ms,
    max_retry_attempts,
    retry_backoff_ms,
    cost_per_unit,
    cost_unit,
    supported_formats,
    vendor_config,
    api_config,
    created_by,
    created_timestamp
) VALUES (
    'f1111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    1,
    'AWS-S3',
    'S3-STMT-001',
    'Statement Storage',
    'STORAGE',
    1,
    ARRAY['US', 'CA', 'UK', 'EU'],
    'ACTIVE',
    true,
    true,
    30000,
    3,
    1000,
    0.023,
    'PER_MB',
    ARRAY['PDF', 'HTML', 'DOCX', 'PNG', 'JPG'],
    '{"bucket": "company-documents-prod", "prefix": "statements/", "storageClass": "STANDARD", "encryption": "AES256"}',
    '{"region": "us-east-1", "authType": "IAM_ROLE", "credentialsPath": "vault:///secrets/aws-s3"}',
    'system',
    NOW()
) ON CONFLICT (template_vendor_id) DO NOTHING;

-- ============================================================
-- Summary query
-- ============================================================
-- SELECT
--     vendor,
--     vendor_type,
--     priority_order,
--     vendor_status,
--     supported_regions,
--     cost_per_unit,
--     cost_unit
-- FROM document_hub.template_vendor_mapping
-- WHERE active_flag = true
-- ORDER BY vendor_type, priority_order;
