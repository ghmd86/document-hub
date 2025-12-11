-- ====================================================================
-- Document Hub - P0 Schema Rollback Script
-- ====================================================================
-- This script reverts all P0 schema changes.
-- USE WITH CAUTION - This will drop columns and data!
--
-- Run with: psql -U postgres -d document_hub -f p0-schema-rollback.sql
--
-- Date: December 2024
-- ====================================================================

\echo '======================================================================'
\echo 'WARNING: This will DROP columns and DATA!'
\echo 'Press Ctrl+C within 5 seconds to cancel...'
\echo '======================================================================'

SELECT pg_sleep(5);

\echo 'Starting rollback...'

-- ====================================================================
-- STEP 1: Drop Indexes
-- ====================================================================
\echo 'Dropping indexes...'

DROP INDEX IF EXISTS document_hub.idx_storage_index_doc_creation_date;
DROP INDEX IF EXISTS document_hub.idx_template_lob_comm_type;
DROP INDEX IF EXISTS document_hub.idx_template_lob_msg_center;
DROP INDEX IF EXISTS document_hub.idx_template_full_filter;

\echo 'Indexes dropped'


-- ====================================================================
-- STEP 2: Drop Constraints
-- ====================================================================
\echo 'Dropping constraints...'

ALTER TABLE document_hub.master_template_definition
DROP CONSTRAINT IF EXISTS chk_communication_type;

ALTER TABLE document_hub.master_template_definition
DROP CONSTRAINT IF EXISTS chk_workflow;

\echo 'Constraints dropped'


-- ====================================================================
-- STEP 3: Drop Columns from master_template_definition
-- ====================================================================
\echo 'Dropping columns from master_template_definition...'

ALTER TABLE document_hub.master_template_definition
DROP COLUMN IF EXISTS communication_type;

ALTER TABLE document_hub.master_template_definition
DROP COLUMN IF EXISTS workflow;

ALTER TABLE document_hub.master_template_definition
DROP COLUMN IF EXISTS single_document_flag;

\echo 'Columns dropped from master_template_definition'


-- ====================================================================
-- STEP 4: Note about doc_creation_date
-- ====================================================================
\echo ''
\echo 'NOTE: doc_creation_date column in storage_index was NOT dropped.'
\echo 'This column existed before P0 changes and may contain production data.'
\echo 'To drop it manually, run:'
\echo '  ALTER TABLE document_hub.storage_index DROP COLUMN IF EXISTS doc_creation_date;'
\echo ''


-- ====================================================================
-- STEP 5: Delete New Sample Templates (if added)
-- ====================================================================
\echo 'Removing new sample templates...'

DELETE FROM document_hub.storage_index
WHERE master_template_id IN (
    'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1',  -- Email Welcome Letter
    's1s1s1s1-s1s1-s1s1-s1s1-s1s1s1s1s1s1',  -- SMS Payment Reminder
    'p1p1p1p1-p1p1-p1p1-p1p1-p1p1p1p1p1p1',  -- Push Transaction Alert
    'i1i1i1i1-i1i1-i1i1-i1i1-i1i1i1i1i1i1'   -- Internal Audit Report
);

DELETE FROM document_hub.master_template_definition
WHERE master_template_id IN (
    'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1',
    's1s1s1s1-s1s1-s1s1-s1s1-s1s1s1s1s1s1',
    'p1p1p1p1-p1p1-p1p1-p1p1-p1p1p1p1p1p1',
    'i1i1i1i1-i1i1-i1i1-i1i1-i1i1i1i1i1i1'
);

\echo 'New sample templates removed'


-- ====================================================================
-- STEP 6: Verification
-- ====================================================================
\echo ''
\echo '======================================================================'
\echo 'ROLLBACK VERIFICATION'
\echo '======================================================================'

\echo ''
\echo '--- Columns in master_template_definition ---'
SELECT column_name
FROM information_schema.columns
WHERE table_schema = 'document_hub'
AND table_name = 'master_template_definition'
AND column_name IN ('communication_type', 'workflow', 'single_document_flag');

\echo ''
\echo '--- Remaining Indexes ---'
SELECT indexname
FROM pg_indexes
WHERE schemaname = 'document_hub'
AND (indexname LIKE 'idx_template%' OR indexname = 'idx_storage_index_doc_creation_date');

\echo ''
\echo '======================================================================'
\echo 'Rollback Complete!'
\echo '======================================================================'
