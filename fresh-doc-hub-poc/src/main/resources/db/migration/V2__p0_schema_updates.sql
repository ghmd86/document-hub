-- ====================================================================
-- Document Hub - P0 Schema Updates Migration
-- ====================================================================
-- Version: V2
-- Date: December 2024
-- Description: Adds columns required for P0 critical items
-- Source: TODO_BACKLOG.md - P0-002, P0-006, P0-009
-- ====================================================================

-- ====================================================================
-- P0-002: Add communication_type column to master_template_definition
-- ====================================================================
-- Purpose: Filter templates by communication channel (LETTER, EMAIL, SMS, PUSH)
-- Default: LETTER (most common use case - 90% of calls)
-- John's Decision: "For letters you might have Smartcom. For emails you might
--                   have Salesforce. But you'll never have Salesforce for a letter."

ALTER TABLE document_hub.master_template_definition
ADD COLUMN IF NOT EXISTS communication_type VARCHAR(20) DEFAULT 'LETTER';

-- Add constraint for valid values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_communication_type'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD CONSTRAINT chk_communication_type
        CHECK (communication_type IN ('LETTER', 'EMAIL', 'SMS', 'PUSH'));
    END IF;
END $$;

COMMENT ON COLUMN document_hub.master_template_definition.communication_type IS
'Communication channel for document delivery: LETTER (physical mail), EMAIL, SMS, PUSH. Default: LETTER';


-- ====================================================================
-- P0-006: Add workflow column to master_template_definition
-- ====================================================================
-- Purpose: WCM workflow type for template approval process
-- Values:
--   - 2_EYES: Single person approval (maker only)
--   - 4_EYES: Dual person approval (maker-checker)
-- John's Decision: "We also talked about... for the WCM... 2 eyes, 4 eyes"

ALTER TABLE document_hub.master_template_definition
ADD COLUMN IF NOT EXISTS workflow VARCHAR(20);

-- Add constraint for valid values (NULL allowed for backwards compatibility)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_workflow'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD CONSTRAINT chk_workflow
        CHECK (workflow IN ('2_EYES', '4_EYES') OR workflow IS NULL);
    END IF;
END $$;

COMMENT ON COLUMN document_hub.master_template_definition.workflow IS
'WCM workflow type: 2_EYES (single approver), 4_EYES (dual approver/maker-checker)';


-- ====================================================================
-- P0-009: Add single_document_flag column to master_template_definition
-- ====================================================================
-- Purpose: Control whether a template returns single or multiple documents
-- John's Decision: "Current queries must return only ONE document"
-- Values:
--   - true (default): Return only the most recent document
--   - false: Return all matching documents (e.g., all statements)

ALTER TABLE document_hub.master_template_definition
ADD COLUMN IF NOT EXISTS single_document_flag BOOLEAN DEFAULT true;

COMMENT ON COLUMN document_hub.master_template_definition.single_document_flag IS
'If true, return only the most recent document. If false, return all matching documents (e.g., statements).';


-- ====================================================================
-- P0-007/P0-008: Verify doc_creation_date exists and add index
-- ====================================================================
-- Purpose: Support postedFromDate/postedToDate filtering
-- John's Decision: "Use doc_creation_date (not created_timestamp)"
-- Note: Column already exists, just adding index for performance

-- Ensure column exists (should already be there, but be safe)
ALTER TABLE document_hub.storage_index
ADD COLUMN IF NOT EXISTS doc_creation_date BIGINT;

-- Add index for date range filtering performance
CREATE INDEX IF NOT EXISTS idx_storage_index_doc_creation_date
ON document_hub.storage_index(doc_creation_date);

COMMENT ON COLUMN document_hub.storage_index.doc_creation_date IS
'Document creation date in epoch milliseconds. Used for postedFromDate/postedToDate filtering. Different from created_timestamp (DB insert time).';


-- ====================================================================
-- Additional Performance Indexes
-- ====================================================================

-- Composite index for common query pattern: LOB + communication_type
CREATE INDEX IF NOT EXISTS idx_template_lob_comm_type
ON document_hub.master_template_definition(line_of_business, communication_type);

-- Composite index for message center filtering
CREATE INDEX IF NOT EXISTS idx_template_lob_msg_center
ON document_hub.master_template_definition(line_of_business, message_center_doc_flag);

-- Composite index for full filter query (LOB + template_type + communication_type + message_center_doc_flag)
CREATE INDEX IF NOT EXISTS idx_template_full_filter
ON document_hub.master_template_definition(line_of_business, template_type, communication_type, message_center_doc_flag)
WHERE active_flag = true;


-- ====================================================================
-- Backfill Existing Data
-- ====================================================================

-- Set communication_type = 'LETTER' for all existing templates (if NULL)
UPDATE document_hub.master_template_definition
SET communication_type = 'LETTER'
WHERE communication_type IS NULL;

-- Set workflow based on regulatory_flag (sensible default)
-- Regulatory documents typically need more oversight (4_EYES)
UPDATE document_hub.master_template_definition
SET workflow = '4_EYES'
WHERE workflow IS NULL AND regulatory_flag = true;

UPDATE document_hub.master_template_definition
SET workflow = '2_EYES'
WHERE workflow IS NULL AND regulatory_flag = false;

-- Set single_document_flag based on template type
-- Statements can have multiple documents per account
UPDATE document_hub.master_template_definition
SET single_document_flag = false
WHERE template_type IN ('Statement', 'SavingsStatement', 'SavingsTaxStatement')
AND single_document_flag IS NULL;

-- All other templates default to single document
UPDATE document_hub.master_template_definition
SET single_document_flag = true
WHERE single_document_flag IS NULL;

-- Backfill doc_creation_date from created_timestamp where NULL
UPDATE document_hub.storage_index
SET doc_creation_date = EXTRACT(EPOCH FROM created_timestamp)::BIGINT * 1000
WHERE doc_creation_date IS NULL;


-- ====================================================================
-- Verification Queries (run manually to verify migration)
-- ====================================================================
-- SELECT column_name, data_type, column_default, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'document_hub'
-- AND table_name = 'master_template_definition'
-- AND column_name IN ('communication_type', 'workflow', 'single_document_flag');

-- SELECT column_name, data_type, column_default, is_nullable
-- FROM information_schema.columns
-- WHERE table_schema = 'document_hub'
-- AND table_name = 'storage_index'
-- AND column_name = 'doc_creation_date';

-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE schemaname = 'document_hub';


-- ====================================================================
-- Migration Complete
-- ====================================================================
-- New columns added:
--   - master_template_definition.communication_type (VARCHAR(20), DEFAULT 'LETTER')
--   - master_template_definition.workflow (VARCHAR(20), NULL allowed)
--   - master_template_definition.single_document_flag (BOOLEAN, DEFAULT true)
--
-- Indexes added:
--   - idx_storage_index_doc_creation_date
--   - idx_template_lob_comm_type
--   - idx_template_lob_msg_center
--   - idx_template_full_filter
-- ====================================================================
