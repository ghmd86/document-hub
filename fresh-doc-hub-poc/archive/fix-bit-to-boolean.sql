-- ========================================
-- Fix BIT columns to BOOLEAN
-- Resolves: operator does not exist: boolean = bit
-- ========================================

-- Convert active_flag from bit(1) to boolean
ALTER TABLE document_hub.master_template_definition
    ALTER COLUMN active_flag TYPE boolean
    USING (active_flag::int::boolean);

-- Convert regulatory_flag from bit(1) to boolean
ALTER TABLE document_hub.master_template_definition
    ALTER COLUMN regulatory_flag TYPE boolean
    USING (regulatory_flag::int::boolean);

-- Convert shared_document_flag from bit(1) to boolean
ALTER TABLE document_hub.master_template_definition
    ALTER COLUMN shared_document_flag TYPE boolean
    USING (shared_document_flag::int::boolean);

-- Convert accessible_flag in storage_index from bit(1) to boolean
ALTER TABLE document_hub.storage_index
    ALTER COLUMN accessible_flag TYPE boolean
    USING (accessible_flag::int::boolean);

-- Convert shared_flag in storage_index from bit(1) to boolean
ALTER TABLE document_hub.storage_index
    ALTER COLUMN shared_flag TYPE boolean
    USING (shared_flag::int::boolean);

-- Verify the changes
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'document_hub'
  AND table_name IN ('master_template_definition', 'storage_index')
  AND column_name LIKE '%flag%'
ORDER BY table_name, column_name;
