-- ============================================================
-- V4: Vendor Mapping Enhancements
-- Adds columns to template_vendor_mapping for:
-- - Vendor type categorization (GENERATION, PRINT, EMAIL, SMS, PUSH, STORAGE)
-- - Failover priority ordering
-- - Geographic region support
-- - Vendor status tracking
-- - Rate limiting and cost tracking
-- ============================================================

-- ============================================================
-- 1. Add vendor_type column
-- Categorizes vendors by their function (John's requirement:
-- distinguish Salesforce (email) from Smartcom (print))
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS vendor_type VARCHAR(20);

COMMENT ON COLUMN document_hub.template_vendor_mapping.vendor_type IS
'Type of vendor: GENERATION (document creation), PRINT (physical mail), EMAIL, SMS, PUSH, STORAGE';

-- Add check constraint for valid vendor types
ALTER TABLE document_hub.template_vendor_mapping
ADD CONSTRAINT chk_vendor_type
CHECK (vendor_type IS NULL OR vendor_type IN ('GENERATION', 'PRINT', 'EMAIL', 'SMS', 'PUSH', 'STORAGE'));

-- ============================================================
-- 2. Add priority_order column
-- Enables failover chain: 1 = first choice, 2 = fallback, etc.
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS priority_order INTEGER DEFAULT 1;

COMMENT ON COLUMN document_hub.template_vendor_mapping.priority_order IS
'Routing priority for failover: 1 = primary, 2 = first fallback, 3 = second fallback, etc.';

-- ============================================================
-- 3. Add supported_regions column
-- For geographic routing (e.g., different print vendors for US vs INTL)
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS supported_regions TEXT[];

COMMENT ON COLUMN document_hub.template_vendor_mapping.supported_regions IS
'Geographic regions this vendor supports: {US, CA, UK, INTL, etc.}';

-- ============================================================
-- 4. Add vendor status column
-- For health-based routing and monitoring
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS vendor_status VARCHAR(20) DEFAULT 'ACTIVE';

COMMENT ON COLUMN document_hub.template_vendor_mapping.vendor_status IS
'Current vendor status: ACTIVE, DEGRADED, DOWN, MAINTENANCE';

ALTER TABLE document_hub.template_vendor_mapping
ADD CONSTRAINT chk_vendor_status
CHECK (vendor_status IN ('ACTIVE', 'DEGRADED', 'DOWN', 'MAINTENANCE'));

-- ============================================================
-- 5. Add rate limiting columns
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS rate_limit_per_minute INTEGER;

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS rate_limit_per_day INTEGER;

COMMENT ON COLUMN document_hub.template_vendor_mapping.rate_limit_per_minute IS
'Maximum requests per minute to this vendor (null = unlimited)';

COMMENT ON COLUMN document_hub.template_vendor_mapping.rate_limit_per_day IS
'Maximum requests per day to this vendor (null = unlimited)';

-- ============================================================
-- 6. Add timeout and retry columns
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS timeout_ms INTEGER DEFAULT 30000;

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS max_retry_attempts INTEGER DEFAULT 3;

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS retry_backoff_ms INTEGER DEFAULT 1000;

COMMENT ON COLUMN document_hub.template_vendor_mapping.timeout_ms IS
'Request timeout in milliseconds (default: 30000)';

COMMENT ON COLUMN document_hub.template_vendor_mapping.max_retry_attempts IS
'Maximum retry attempts on failure (default: 3)';

COMMENT ON COLUMN document_hub.template_vendor_mapping.retry_backoff_ms IS
'Initial backoff delay between retries in milliseconds (default: 1000)';

-- ============================================================
-- 7. Add cost tracking columns
-- For vendor cost reporting (UC-RA-003)
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS cost_per_unit DECIMAL(10, 4);

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS cost_unit VARCHAR(20);

COMMENT ON COLUMN document_hub.template_vendor_mapping.cost_per_unit IS
'Cost per unit for this vendor (e.g., 0.55 per letter)';

COMMENT ON COLUMN document_hub.template_vendor_mapping.cost_unit IS
'Unit of cost measurement: PER_DOCUMENT, PER_PAGE, PER_MB, PER_MESSAGE';

ALTER TABLE document_hub.template_vendor_mapping
ADD CONSTRAINT chk_cost_unit
CHECK (cost_unit IS NULL OR cost_unit IN ('PER_DOCUMENT', 'PER_PAGE', 'PER_MB', 'PER_MESSAGE'));

-- ============================================================
-- 8. Add supported formats column
-- For capability-based routing
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS supported_formats TEXT[];

COMMENT ON COLUMN document_hub.template_vendor_mapping.supported_formats IS
'Document formats this vendor supports: {PDF, HTML, DOCX, POSTSCRIPT, etc.}';

-- ============================================================
-- 9. Add last health check columns
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS last_health_check TIMESTAMP;

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS last_health_status VARCHAR(20);

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN IF NOT EXISTS health_check_endpoint VARCHAR(500);

COMMENT ON COLUMN document_hub.template_vendor_mapping.last_health_check IS
'Timestamp of last health check';

COMMENT ON COLUMN document_hub.template_vendor_mapping.last_health_status IS
'Result of last health check: SUCCESS, FAILURE, TIMEOUT';

COMMENT ON COLUMN document_hub.template_vendor_mapping.health_check_endpoint IS
'URL endpoint for health checks (if different from main API)';

-- ============================================================
-- 10. Create indexes for efficient routing queries
-- ============================================================

-- Index for routing by vendor type and priority
CREATE INDEX IF NOT EXISTS idx_vendor_routing
ON document_hub.template_vendor_mapping(
    master_template_id,
    template_version,
    vendor_type,
    priority_order
) WHERE active_flag = true;

-- Index for finding active vendors by type
CREATE INDEX IF NOT EXISTS idx_vendor_type_status
ON document_hub.template_vendor_mapping(vendor_type, vendor_status)
WHERE active_flag = true;

-- Index for region-based routing (GIN index for array)
CREATE INDEX IF NOT EXISTS idx_vendor_regions
ON document_hub.template_vendor_mapping USING GIN (supported_regions)
WHERE active_flag = true;

-- ============================================================
-- 11. Backfill existing data with sensible defaults
-- ============================================================

-- Set vendor_type based on vendor name patterns (best guess)
UPDATE document_hub.template_vendor_mapping
SET vendor_type = CASE
    WHEN LOWER(vendor) LIKE '%print%' OR LOWER(vendor) LIKE '%smartcom%' OR LOWER(vendor) LIKE '%mail%' THEN 'PRINT'
    WHEN LOWER(vendor) LIKE '%email%' OR LOWER(vendor) LIKE '%sendgrid%' OR LOWER(vendor) LIKE '%salesforce%' THEN 'EMAIL'
    WHEN LOWER(vendor) LIKE '%sms%' OR LOWER(vendor) LIKE '%twilio%' THEN 'SMS'
    WHEN LOWER(vendor) LIKE '%push%' OR LOWER(vendor) LIKE '%firebase%' THEN 'PUSH'
    WHEN LOWER(vendor) LIKE '%s3%' OR LOWER(vendor) LIKE '%storage%' OR LOWER(vendor) LIKE '%ecms%' THEN 'STORAGE'
    ELSE 'GENERATION'
END
WHERE vendor_type IS NULL;

-- Set default supported regions for existing vendors
UPDATE document_hub.template_vendor_mapping
SET supported_regions = ARRAY['US']
WHERE supported_regions IS NULL;

-- Set default supported formats
UPDATE document_hub.template_vendor_mapping
SET supported_formats = CASE
    WHEN vendor_type = 'PRINT' THEN ARRAY['PDF', 'POSTSCRIPT']
    WHEN vendor_type = 'EMAIL' THEN ARRAY['HTML', 'TEXT']
    WHEN vendor_type = 'SMS' THEN ARRAY['TEXT']
    WHEN vendor_type = 'PUSH' THEN ARRAY['JSON']
    ELSE ARRAY['PDF', 'HTML']
END
WHERE supported_formats IS NULL;

-- ============================================================
-- 12. Add NOT NULL constraint after backfill
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ALTER COLUMN vendor_type SET NOT NULL;

ALTER TABLE document_hub.template_vendor_mapping
ALTER COLUMN vendor_status SET NOT NULL;

-- ============================================================
-- Verification query
-- ============================================================
-- SELECT
--     template_vendor_id,
--     vendor,
--     vendor_type,
--     priority_order,
--     vendor_status,
--     supported_regions,
--     supported_formats,
--     cost_per_unit,
--     cost_unit
-- FROM document_hub.template_vendor_mapping
-- ORDER BY master_template_id, vendor_type, priority_order;
