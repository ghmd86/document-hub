-- Create Document Hub Database Schema
-- This script creates all necessary tables for the Document Hub Service

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Table: master_template_definition
-- Purpose: Stores master metadata and configuration details of document templates
-- ============================================================================
CREATE TABLE IF NOT EXISTS master_template_definition (
    template_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version INT NOT NULL DEFAULT 1,
    legacy_template_id VARCHAR(255),
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    line_of_business VARCHAR(255),
    category VARCHAR(255),
    doc_type VARCHAR(255) NOT NULL,
    language_code VARCHAR(50) DEFAULT 'EN_US',
    owning_dept VARCHAR(255),
    notification_needed BOOLEAN DEFAULT FALSE,
    doc_supporting_data JSONB,
    is_regulatory BOOLEAN DEFAULT FALSE,
    is_message_center_doc BOOLEAN DEFAULT FALSE,
    document_channel JSONB,
    template_variables JSONB,
    template_status VARCHAR(50) DEFAULT 'Draft',
    effective_date BIGINT,
    valid_until BIGINT,
    -- Sharing configuration
    is_shared_document BOOLEAN DEFAULT FALSE,
    sharing_scope VARCHAR(100),
    data_extraction_schema TEXT,
    -- DBA Required columns
    created_by VARCHAR(255) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(255),
    updated_timestamp TIMESTAMP,
    archive_indicator BOOLEAN DEFAULT FALSE,
    archive_timestamp TIMESTAMP,
    version_number BIGINT DEFAULT 1,
    record_status BIGINT DEFAULT 1,
    -- Constraints
    CONSTRAINT chk_template_status CHECK (template_status IN ('Draft', 'Approved', 'Rejected', 'Pending')),
    CONSTRAINT chk_sharing_scope CHECK (sharing_scope IS NULL OR sharing_scope IN
        ('all', 'credit_card_account_only', 'digital_bank_customer_only', 'enterprise_customer_only', 'custom_rule'))
);

-- ============================================================================
-- Table: storage_index
-- Purpose: Stores actual document-related data and indexing information
-- ============================================================================
CREATE TABLE IF NOT EXISTS storage_index (
    storage_index_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    doc_type VARCHAR(255),
    storage_vendor VARCHAR(255) DEFAULT 'ECMS',
    reference_key VARCHAR(255),
    reference_key_type VARCHAR(255),
    account_key UUID,
    customer_key UUID,
    storage_document_key UUID,
    file_name VARCHAR(500),
    doc_creation_date BIGINT,
    is_accessible BOOLEAN DEFAULT TRUE,
    last_referenced BIGINT,
    time_referenced INT DEFAULT 0,
    doc_info JSONB,
    -- DBA Required columns
    created_by VARCHAR(255) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(255),
    updated_timestamp TIMESTAMP,
    archive_indicator BOOLEAN DEFAULT FALSE,
    archive_timestamp TIMESTAMP,
    version_number BIGINT DEFAULT 1,
    record_status BIGINT DEFAULT 1,
    -- Foreign key
    CONSTRAINT fk_storage_template FOREIGN KEY (template_id)
        REFERENCES master_template_definition(template_id) ON DELETE RESTRICT
);

-- ============================================================================
-- Indexes for performance optimization
-- ============================================================================

-- Indexes on master_template_definition
CREATE INDEX IF NOT EXISTS idx_template_shared ON master_template_definition(is_shared_document);
CREATE INDEX IF NOT EXISTS idx_template_scope ON master_template_definition(sharing_scope);
CREATE INDEX IF NOT EXISTS idx_template_status ON master_template_definition(template_status);
CREATE INDEX IF NOT EXISTS idx_template_lob ON master_template_definition(line_of_business);
CREATE INDEX IF NOT EXISTS idx_template_category ON master_template_definition(category);
CREATE INDEX IF NOT EXISTS idx_template_doctype ON master_template_definition(doc_type);
CREATE INDEX IF NOT EXISTS idx_template_effective ON master_template_definition(effective_date);

-- Indexes on storage_index
CREATE INDEX IF NOT EXISTS idx_storage_account ON storage_index(account_key);
CREATE INDEX IF NOT EXISTS idx_storage_customer ON storage_index(customer_key);
CREATE INDEX IF NOT EXISTS idx_storage_template ON storage_index(template_id);
CREATE INDEX IF NOT EXISTS idx_storage_creation_date ON storage_index(doc_creation_date);
CREATE INDEX IF NOT EXISTS idx_storage_accessible ON storage_index(is_accessible);
CREATE INDEX IF NOT EXISTS idx_storage_doctype ON storage_index(doc_type);
CREATE INDEX IF NOT EXISTS idx_storage_composite ON storage_index(account_key, is_accessible, doc_creation_date);

-- ============================================================================
-- Comments for documentation
-- ============================================================================
COMMENT ON TABLE master_template_definition IS 'Stores master metadata and configuration details of each document template';
COMMENT ON TABLE storage_index IS 'Stores actual document-related data and indexing information';

COMMENT ON COLUMN master_template_definition.is_shared_document IS 'Flag to indicate if documents of this kind are shared';
COMMENT ON COLUMN master_template_definition.sharing_scope IS 'Scope of sharing: all, credit_card_accounts_only, digital_bank_customer_only, enterprise_customer_only, custom_rule';
COMMENT ON COLUMN master_template_definition.data_extraction_schema IS 'JSON configuration for custom rule data extraction';

COMMENT ON COLUMN storage_index.is_accessible IS 'Stores if document is still active or archived';
COMMENT ON COLUMN storage_index.doc_info IS 'Extra information related to document (cycle date, statement_id, etc.)';
