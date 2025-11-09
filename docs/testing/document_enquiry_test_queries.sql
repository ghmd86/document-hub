-- Document Enquiry API Test Queries
-- Simulates POST /documents-enquiry logic

-- Test Parameters
-- accountId: 770e8400-e29b-41d4-a716-446655440001
-- customerId: 880e8400-e29b-41d4-a716-446655440001

-- ========================================
-- TEST 1: Account-Specific Documents
-- ========================================
SELECT
    '=== TEST 1: Account-Specific Documents ===' AS test_header;

SELECT
    si.storage_index_id,
    si.doc_type,
    si.file_name,
    mt.template_name,
    mt.is_shared_document,
    'Account-Specific' AS document_source
FROM storage_index si
JOIN master_template_definition mt ON si.template_id = mt.template_id
WHERE si.account_key = '770e8400-e29b-41d4-a716-446655440001'
  AND si.customer_key = '880e8400-e29b-41d4-a716-446655440001'
  AND si.archive_indicator = false
  AND si.is_accessible = true
ORDER BY si.doc_creation_date DESC;

-- ========================================
-- TEST 2: Shared Documents - Scope: "all"
-- ========================================
SELECT
    '=== TEST 2: Shared Documents - Scope: all ===' AS test_header;

SELECT
    template_id,
    template_name,
    sharing_scope,
    'Should be included for ALL users' AS evaluation
FROM master_template_definition
WHERE is_shared_document = true
  AND sharing_scope = 'all'
  AND archive_indicator = false;

-- ========================================
-- TEST 3: Shared Documents - Scope: "credit_card_account_only"
-- ========================================
SELECT
    '=== TEST 3: Shared Documents - Scope: credit_card_account_only ===' AS test_header;

SELECT
    template_id,
    template_name,
    sharing_scope,
    'Requires credit card account check' AS evaluation
FROM master_template_definition
WHERE is_shared_document = true
  AND sharing_scope = 'credit_card_account_only'
  AND archive_indicator = false;

-- ========================================
-- TEST 4: Shared Documents - Scope: "custom_rule"
-- ========================================
SELECT
    '=== TEST 4: Shared Documents - Scope: custom_rule ===' AS test_header;

SELECT
    template_id,
    template_name,
    sharing_scope,
    data_extraction_schema,
    'Requires extraction logic evaluation' AS evaluation
FROM master_template_definition
WHERE is_shared_document = true
  AND sharing_scope = 'custom_rule'
  AND archive_indicator = false;

-- ========================================
-- TEST 5: Complete Document List (Merged)
-- ========================================
SELECT
    '=== TEST 5: Complete Document List (Account + Shared) ===' AS test_header;

-- This simulates what the API should return
WITH account_docs AS (
    SELECT
        si.storage_index_id AS document_id,
        mt.template_name AS display_name,
        mt.category,
        si.doc_type,
        si.doc_creation_date AS date_posted,
        false AS is_shared,
        NULL::varchar AS sharing_scope,
        1 AS priority
    FROM storage_index si
    JOIN master_template_definition mt ON si.template_id = mt.template_id
    WHERE si.account_key = '770e8400-e29b-41d4-a716-446655440001'
      AND si.customer_key = '880e8400-e29b-41d4-a716-446655440001'
      AND si.archive_indicator = false
      AND si.is_accessible = true
),
shared_docs AS (
    SELECT
        mt.template_id::text AS document_id,
        mt.template_name AS display_name,
        mt.category,
        mt.doc_type,
        EXTRACT(EPOCH FROM mt.effective_date)::bigint AS date_posted,
        true AS is_shared,
        mt.sharing_scope,
        CASE
            WHEN mt.sharing_scope = 'all' THEN 2
            WHEN mt.sharing_scope = 'credit_card_account_only' THEN 3
            WHEN mt.sharing_scope = 'custom_rule' THEN 4
            ELSE 5
        END AS priority
    FROM master_template_definition mt
    WHERE mt.is_shared_document = true
      AND mt.archive_indicator = false
      AND (
          mt.sharing_scope = 'all'
          OR (mt.sharing_scope = 'credit_card_account_only' AND true) -- Assume credit card account
          -- OR (mt.sharing_scope = 'custom_rule' AND <extraction_logic_passes>)
      )
)
SELECT
    document_id,
    display_name,
    category,
    doc_type,
    date_posted,
    is_shared,
    sharing_scope
FROM (
    SELECT * FROM account_docs
    UNION ALL
    SELECT * FROM shared_docs
) combined
ORDER BY priority, date_posted DESC;

-- ========================================
-- TEST 6: Summary Statistics
-- ========================================
SELECT
    '=== TEST 6: Summary Statistics ===' AS test_header;

SELECT
    COUNT(*) FILTER (WHERE is_shared = false) AS account_specific_count,
    COUNT(*) FILTER (WHERE is_shared = true AND sharing_scope = 'all') AS shared_all_count,
    COUNT(*) FILTER (WHERE is_shared = true AND sharing_scope = 'credit_card_account_only') AS shared_cc_count,
    COUNT(*) FILTER (WHERE is_shared = true AND sharing_scope = 'custom_rule') AS shared_custom_rule_count,
    COUNT(*) AS total_documents
FROM (
    SELECT false AS is_shared, NULL::varchar AS sharing_scope
    FROM storage_index
    WHERE account_key = '770e8400-e29b-41d4-a716-446655440001'
      AND archive_indicator = false

    UNION ALL

    SELECT true AS is_shared, sharing_scope
    FROM master_template_definition
    WHERE is_shared_document = true
      AND archive_indicator = false
      AND sharing_scope IN ('all', 'credit_card_account_only')
) stats;
