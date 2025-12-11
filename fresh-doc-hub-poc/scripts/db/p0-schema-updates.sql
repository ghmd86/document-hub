-- ====================================================================
-- Document Hub - P0 Schema Updates (Standalone Script)
-- ====================================================================
-- This script can be run directly against the PostgreSQL database
-- without requiring Flyway migration framework.
--
-- Run with: psql -U postgres -d document_hub -f p0-schema-updates.sql
-- Or use your preferred PostgreSQL client
--
-- Date: December 2024
-- Source: TODO_BACKLOG.md - P0 Critical Items
-- ====================================================================

\echo '======================================================================'
\echo 'Starting P0 Schema Updates...'
\echo '======================================================================'

-- ====================================================================
-- STEP 1: Add communication_type column (P0-002)
-- ====================================================================
\echo 'Adding communication_type column...'

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'document_hub'
        AND table_name = 'master_template_definition'
        AND column_name = 'communication_type'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD COLUMN communication_type VARCHAR(20) DEFAULT 'LETTER';

        RAISE NOTICE 'Column communication_type added successfully';
    ELSE
        RAISE NOTICE 'Column communication_type already exists';
    END IF;
END $$;

-- Add constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_communication_type'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD CONSTRAINT chk_communication_type
        CHECK (communication_type IN ('LETTER', 'EMAIL', 'SMS', 'PUSH'));

        RAISE NOTICE 'Constraint chk_communication_type added successfully';
    ELSE
        RAISE NOTICE 'Constraint chk_communication_type already exists';
    END IF;
END $$;

COMMENT ON COLUMN document_hub.master_template_definition.communication_type IS
'Communication channel: LETTER (physical mail), EMAIL, SMS, PUSH. Default: LETTER';


-- ====================================================================
-- STEP 2: Add workflow column (P0-006)
-- ====================================================================
\echo 'Adding workflow column...'

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'document_hub'
        AND table_name = 'master_template_definition'
        AND column_name = 'workflow'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD COLUMN workflow VARCHAR(20);

        RAISE NOTICE 'Column workflow added successfully';
    ELSE
        RAISE NOTICE 'Column workflow already exists';
    END IF;
END $$;

-- Add constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_workflow'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD CONSTRAINT chk_workflow
        CHECK (workflow IN ('2_EYES', '4_EYES') OR workflow IS NULL);

        RAISE NOTICE 'Constraint chk_workflow added successfully';
    ELSE
        RAISE NOTICE 'Constraint chk_workflow already exists';
    END IF;
END $$;

COMMENT ON COLUMN document_hub.master_template_definition.workflow IS
'WCM workflow: 2_EYES (single approver), 4_EYES (dual approver/maker-checker)';


-- ====================================================================
-- STEP 3: Add single_document_flag column (P0-009)
-- ====================================================================
\echo 'Adding single_document_flag column...'

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'document_hub'
        AND table_name = 'master_template_definition'
        AND column_name = 'single_document_flag'
    ) THEN
        ALTER TABLE document_hub.master_template_definition
        ADD COLUMN single_document_flag BOOLEAN DEFAULT true;

        RAISE NOTICE 'Column single_document_flag added successfully';
    ELSE
        RAISE NOTICE 'Column single_document_flag already exists';
    END IF;
END $$;

COMMENT ON COLUMN document_hub.master_template_definition.single_document_flag IS
'If true, return only most recent document. If false, return all (e.g., statements).';


-- ====================================================================
-- STEP 4: Verify/Add doc_creation_date column (P0-007)
-- ====================================================================
\echo 'Verifying doc_creation_date column...'

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'document_hub'
        AND table_name = 'storage_index'
        AND column_name = 'doc_creation_date'
    ) THEN
        ALTER TABLE document_hub.storage_index
        ADD COLUMN doc_creation_date BIGINT;

        RAISE NOTICE 'Column doc_creation_date added successfully';
    ELSE
        RAISE NOTICE 'Column doc_creation_date already exists';
    END IF;
END $$;

COMMENT ON COLUMN document_hub.storage_index.doc_creation_date IS
'Document creation date (epoch ms). Used for posted date filtering.';


-- ====================================================================
-- STEP 5: Create Performance Indexes (P0-008)
-- ====================================================================
\echo 'Creating performance indexes...'

-- Index for doc_creation_date filtering
CREATE INDEX IF NOT EXISTS idx_storage_index_doc_creation_date
ON document_hub.storage_index(doc_creation_date);

-- Index for LOB + communication_type
CREATE INDEX IF NOT EXISTS idx_template_lob_comm_type
ON document_hub.master_template_definition(line_of_business, communication_type);

-- Index for message center filtering
CREATE INDEX IF NOT EXISTS idx_template_lob_msg_center
ON document_hub.master_template_definition(line_of_business, message_center_doc_flag);

-- Composite index for full filter query
CREATE INDEX IF NOT EXISTS idx_template_full_filter
ON document_hub.master_template_definition(
    line_of_business,
    template_type,
    communication_type,
    message_center_doc_flag
)
WHERE active_flag = true;

\echo 'Indexes created successfully'


-- ====================================================================
-- STEP 6: Backfill Existing Data
-- ====================================================================
\echo 'Backfilling existing data...'

-- Set communication_type = 'LETTER' for all existing templates
UPDATE document_hub.master_template_definition
SET communication_type = 'LETTER'
WHERE communication_type IS NULL;

-- Set workflow based on regulatory_flag
UPDATE document_hub.master_template_definition
SET workflow = '4_EYES'
WHERE workflow IS NULL AND regulatory_flag = true;

UPDATE document_hub.master_template_definition
SET workflow = '2_EYES'
WHERE workflow IS NULL AND regulatory_flag = false;

-- Set single_document_flag (statements allow multiple)
UPDATE document_hub.master_template_definition
SET single_document_flag = false
WHERE template_type IN ('Statement', 'SavingsStatement', 'SavingsTaxStatement')
AND (single_document_flag IS NULL OR single_document_flag = true);

UPDATE document_hub.master_template_definition
SET single_document_flag = true
WHERE single_document_flag IS NULL;

-- Backfill doc_creation_date
UPDATE document_hub.storage_index
SET doc_creation_date = EXTRACT(EPOCH FROM created_timestamp)::BIGINT * 1000
WHERE doc_creation_date IS NULL;

\echo 'Data backfill complete'


-- ====================================================================
-- STEP 7: Verification
-- ====================================================================
\echo ''
\echo '======================================================================'
\echo 'VERIFICATION RESULTS'
\echo '======================================================================'

\echo ''
\echo '--- New Columns in master_template_definition ---'
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_schema = 'document_hub'
AND table_name = 'master_template_definition'
AND column_name IN ('communication_type', 'workflow', 'single_document_flag')
ORDER BY column_name;

\echo ''
\echo '--- doc_creation_date in storage_index ---'
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_schema = 'document_hub'
AND table_name = 'storage_index'
AND column_name = 'doc_creation_date';

\echo ''
\echo '--- New Indexes ---'
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'document_hub'
AND indexname LIKE 'idx_template%' OR indexname = 'idx_storage_index_doc_creation_date'
ORDER BY indexname;

\echo ''
\echo '--- Communication Type Distribution ---'
SELECT communication_type, COUNT(*) as template_count
FROM document_hub.master_template_definition
GROUP BY communication_type
ORDER BY communication_type;

\echo ''
\echo '--- Workflow Distribution ---'
SELECT workflow, COUNT(*) as template_count
FROM document_hub.master_template_definition
GROUP BY workflow
ORDER BY workflow;

\echo ''
\echo '--- Message Center Flag Distribution ---'
SELECT message_center_doc_flag, COUNT(*) as template_count
FROM document_hub.master_template_definition
GROUP BY message_center_doc_flag
ORDER BY message_center_doc_flag;

\echo ''
\echo '--- Single Document Flag Distribution ---'
SELECT single_document_flag, COUNT(*) as template_count
FROM document_hub.master_template_definition
GROUP BY single_document_flag
ORDER BY single_document_flag;

\echo ''
\echo '======================================================================'
\echo 'P0 Schema Updates Complete!'
\echo '======================================================================'
