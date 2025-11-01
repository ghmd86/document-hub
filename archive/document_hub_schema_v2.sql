-- ============================================================================
-- Document Hub - PostgreSQL Schema Design v2 (Shared Documents Support)
-- ============================================================================
-- Purpose: Support documents shared across customers/accounts + single-owner docs
-- Design: Separated document metadata from access control (many-to-many)
-- Key Change: document_access junction table replaces denormalized customer_id
-- ============================================================================

-- ============================================================================
-- 1. TEMPLATES TABLE (Unchanged from v1)
-- ============================================================================

CREATE TABLE templates (
    -- Primary Key
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Template Identification
    template_code VARCHAR(100) NOT NULL,
    version_number INTEGER NOT NULL,

    -- Template Metadata
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    document_type VARCHAR(100) NOT NULL,
    document_category VARCHAR(100) NOT NULL,

    -- Status Management
    status VARCHAR(20) NOT NULL CHECK (status IN ('active', 'deprecated', 'draft')),

    -- Business Metadata
    retention_period_days INTEGER,
    requires_signature BOOLEAN DEFAULT false,
    requires_approval BOOLEAN DEFAULT false,

    -- Template Configuration (JSONB for flexible fields)
    configuration JSONB,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),

    -- Unique Constraint: One template_code can have only one version number
    CONSTRAINT unique_template_version UNIQUE (template_code, version_number)
);

-- Indexes for Templates
CREATE INDEX idx_templates_code ON templates(template_code);
CREATE INDEX idx_templates_status ON templates(status);
CREATE INDEX idx_templates_type_category ON templates(document_type, document_category);
CREATE INDEX idx_templates_code_status ON templates(template_code, status);
CREATE INDEX idx_templates_active ON templates(template_code, version_number) WHERE status = 'active';

COMMENT ON TABLE templates IS 'Stores template definitions with denormalized versioning';


-- ============================================================================
-- 2. TEMPLATE_RULES TABLE (Unchanged from v1)
-- ============================================================================

CREATE TABLE template_rules (
    -- Primary Key
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Key to Template
    template_id UUID NOT NULL REFERENCES templates(template_id) ON DELETE CASCADE,

    -- Rule Definition
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('validation', 'transformation', 'business_logic', 'compliance')),

    -- Rule Expression/Condition
    rule_expression TEXT NOT NULL,
    rule_description TEXT,

    -- Rule Scope
    scope VARCHAR(50) CHECK (scope IN ('upload', 'retrieval', 'both')),

    -- Rule Execution Order
    execution_order INTEGER DEFAULT 0,

    -- Rule Status
    is_active BOOLEAN DEFAULT true,

    -- Rule Configuration (JSONB for flexible parameters)
    parameters JSONB,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255)
);

-- Indexes for Template Rules
CREATE INDEX idx_template_rules_template_id ON template_rules(template_id);
CREATE INDEX idx_template_rules_type ON template_rules(rule_type);
CREATE INDEX idx_template_rules_active ON template_rules(template_id, is_active) WHERE is_active = true;

COMMENT ON TABLE template_rules IS 'Normalized table storing reusable business rules for templates';


-- ============================================================================
-- 3. DOCUMENTS TABLE (Redesigned - No Customer Denormalization)
-- ============================================================================
-- Changed: Removed customer_id, customer_name, account_id from main table
-- Access control moved to document_access junction table
-- Partitioned by uploaded_at for time-based queries and archival

CREATE TABLE documents (
    -- Primary Key
    document_id UUID NOT NULL DEFAULT gen_random_uuid(),

    -- ECMS Integration
    ecms_document_id VARCHAR(255) NOT NULL UNIQUE,

    -- Document Classification
    document_type VARCHAR(100) NOT NULL,
    document_category VARCHAR(100) NOT NULL,
    document_sub_category VARCHAR(100),

    -- Template Reference (specific version)
    template_id UUID REFERENCES templates(template_id),
    template_code VARCHAR(100),
    template_version INTEGER,

    -- Document Metadata
    document_name VARCHAR(500) NOT NULL,
    document_description TEXT,
    file_extension VARCHAR(10),
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),

    -- Document Status
    status VARCHAR(50) NOT NULL CHECK (status IN ('uploaded', 'processing', 'active', 'archived', 'deleted')),

    -- Document Sharing Scope
    sharing_scope VARCHAR(20) NOT NULL DEFAULT 'private' CHECK (sharing_scope IN ('private', 'shared', 'public')),
    -- 'private': Single owner (1 customer or account)
    -- 'shared': Multiple customers/accounts
    -- 'public': No specific owner, available to all

    -- Date Fields for Document Lifecycle
    document_date DATE,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMP WITH TIME ZONE,
    expiry_date DATE,

    -- Business Fields
    is_confidential BOOLEAN DEFAULT false,
    requires_signature BOOLEAN DEFAULT false,
    signature_status VARCHAR(50),
    tags TEXT[], -- Array for flexible tagging

    -- Additional Metadata (JSONB for flexible fields)
    metadata JSONB,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),

    -- Primary Key
    PRIMARY KEY (document_id, uploaded_at)
) PARTITION BY RANGE (uploaded_at);

COMMENT ON TABLE documents IS 'Document metadata without denormalized access control (see document_access table)';
COMMENT ON COLUMN documents.sharing_scope IS 'private=single owner, shared=multiple entities, public=global access';


-- ============================================================================
-- PARTITIONING STRATEGY: Range Partitioning by uploaded_at
-- ============================================================================
-- Changed from HASH by customer_id to RANGE by date
-- Reason: Supports shared documents + easy archival of old partitions

-- Create quarterly partitions for 2024-2025 (expand as needed)
CREATE TABLE documents_2024_q1 PARTITION OF documents
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE documents_2024_q2 PARTITION OF documents
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE documents_2024_q3 PARTITION OF documents
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE documents_2024_q4 PARTITION OF documents
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

CREATE TABLE documents_2025_q1 PARTITION OF documents
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');

CREATE TABLE documents_2025_q2 PARTITION OF documents
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');

CREATE TABLE documents_2025_q3 PARTITION OF documents
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');

CREATE TABLE documents_2025_q4 PARTITION OF documents
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');

-- Default partition for future dates
CREATE TABLE documents_default PARTITION OF documents DEFAULT;

COMMENT ON TABLE documents_2024_q1 IS 'Partition for Q1 2024 documents (Jan-Mar)';


-- ============================================================================
-- 4. DOCUMENT_ACCESS TABLE (New - Junction Table)
-- ============================================================================
-- Many-to-Many relationship between documents and customers/accounts
-- Supports: shared documents, single-owner documents, public documents

CREATE TABLE document_access (
    -- Primary Key
    access_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Key to Document
    document_id UUID NOT NULL,

    -- Entity Type and ID (polymorphic relationship)
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('customer', 'account', 'user', 'public')),
    entity_id VARCHAR(100), -- NULL for entity_type = 'public'

    -- Denormalized Entity Name (for query performance)
    entity_name VARCHAR(255),

    -- Access Control
    access_level VARCHAR(20) NOT NULL DEFAULT 'view' CHECK (access_level IN ('view', 'edit', 'owner', 'admin')),
    -- 'owner': Original uploader/creator
    -- 'view': Can view only
    -- 'edit': Can modify document metadata
    -- 'admin': Full control

    -- Access Grant Details
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    granted_by VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Unique Constraint: One entity can't have duplicate access to same document
    CONSTRAINT unique_document_entity_access UNIQUE (document_id, entity_type, entity_id)
);

-- Critical Indexes for document_access
CREATE INDEX idx_document_access_document_id ON document_access(document_id);
CREATE INDEX idx_document_access_entity ON document_access(entity_type, entity_id);
CREATE INDEX idx_document_access_customer_id ON document_access(entity_id) WHERE entity_type = 'customer';
CREATE INDEX idx_document_access_account_id ON document_access(entity_id) WHERE entity_type = 'account';

-- Composite index for common query pattern
CREATE INDEX idx_document_access_entity_document ON document_access(entity_type, entity_id, document_id);

-- Index for access level queries
CREATE INDEX idx_document_access_level ON document_access(entity_type, entity_id, access_level);

COMMENT ON TABLE document_access IS 'Junction table managing document access control (many-to-many)';
COMMENT ON COLUMN document_access.entity_type IS 'Type of entity: customer, account, user, or public';
COMMENT ON COLUMN document_access.entity_id IS 'ID of the entity (NULL for public documents)';
COMMENT ON COLUMN document_access.access_level IS 'Level of access: owner, view, edit, admin';


-- ============================================================================
-- INDEXES FOR DOCUMENTS TABLE (Updated for v2)
-- ============================================================================

-- Query by document type/category (without customer filter)
CREATE INDEX idx_documents_type ON documents(document_type);
CREATE INDEX idx_documents_category ON documents(document_category);
CREATE INDEX idx_documents_type_category ON documents(document_type, document_category);

-- Query by template
CREATE INDEX idx_documents_template_id ON documents(template_id);
CREATE INDEX idx_documents_template_code ON documents(template_code, template_version);

-- Query by ECMS document ID (for lookups from ECMS)
CREATE INDEX idx_documents_ecms_id ON documents(ecms_document_id);

-- Query by status
CREATE INDEX idx_documents_status ON documents(status);

-- Query by sharing scope
CREATE INDEX idx_documents_sharing_scope ON documents(sharing_scope);

-- Query by date ranges (common for reporting)
CREATE INDEX idx_documents_date_range ON documents(document_date);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);

-- Full-text search on document name
CREATE INDEX idx_documents_name_gin ON documents USING gin(to_tsvector('english', document_name));

-- GIN index for JSONB metadata queries
CREATE INDEX idx_documents_metadata_gin ON documents USING gin(metadata);

-- GIN index for tags array
CREATE INDEX idx_documents_tags_gin ON documents USING gin(tags);

-- Partial indexes for frequently filtered subsets
CREATE INDEX idx_documents_active ON documents(document_id) WHERE status = 'active';
CREATE INDEX idx_documents_shared ON documents(document_id) WHERE sharing_scope = 'shared';
CREATE INDEX idx_documents_public ON documents(document_id) WHERE sharing_scope = 'public';


-- ============================================================================
-- TRIGGERS: Auto-update updated_at timestamp
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_templates_updated_at
    BEFORE UPDATE ON templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_template_rules_updated_at
    BEFORE UPDATE ON template_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_document_access_updated_at
    BEFORE UPDATE ON document_access
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


-- ============================================================================
-- SAMPLE SQL QUERIES (Updated for v2)
-- ============================================================================

-- Query 1: Fetch documents for a customer (with access control)
-- Use Case: "Show me all documents accessible to customer C123"
-- Performance: Uses idx_document_access_customer_id + idx_documents_type
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.document_date,
    d.status,
    d.sharing_scope,
    da.access_level,
    da.granted_at
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'customer'
  AND da.entity_id = 'C123'
  AND d.status = 'active'
  AND (da.expires_at IS NULL OR da.expires_at > NOW())
ORDER BY d.document_date DESC
LIMIT 100;
*/

-- Query 2: Fetch documents for a customer by type (common use case)
-- Use Case: "Get all loan applications for customer C123"
-- Performance: Uses composite index idx_document_access_entity_document
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_date,
    d.ecms_document_id,
    d.template_code,
    da.access_level
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'customer'
  AND da.entity_id = 'C123'
  AND d.document_type = 'LOAN_APPLICATION'
  AND d.status = 'active'
ORDER BY d.document_date DESC;
*/

-- Query 3: Fetch documents for an account
-- Use Case: "Get all statements for account A456"
-- Performance: Uses idx_document_access_account_id
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_category,
    d.document_date,
    d.file_size_bytes,
    da.access_level
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'account'
  AND da.entity_id = 'A456'
  AND d.document_category = 'STATEMENTS'
  AND d.status = 'active'
ORDER BY d.document_date DESC;
*/

-- Query 4: Find shared documents (multiple customers/accounts)
-- Use Case: "List all documents shared with more than one entity"
-- Performance: Uses idx_document_access_document_id
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.sharing_scope,
    COUNT(da.access_id) AS access_count,
    array_agg(da.entity_type || ':' || da.entity_id) AS shared_with
FROM documents d
JOIN document_access da ON d.document_id = da.document_id
WHERE d.sharing_scope = 'shared'
GROUP BY d.document_id, d.document_name, d.document_type, d.sharing_scope
HAVING COUNT(da.access_id) > 1
ORDER BY COUNT(da.access_id) DESC;
*/

-- Query 5: Find public documents
-- Use Case: "Get all publicly accessible documents"
-- Performance: Uses idx_documents_public
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.uploaded_at
FROM documents d
WHERE d.sharing_scope = 'public'
  AND d.status = 'active'
ORDER BY d.uploaded_at DESC;
*/

-- Query 6: Check if customer has access to specific document
-- Use Case: "Can customer C123 access document DOC-456?"
-- Performance: Uses unique_document_entity_access constraint index
/*
SELECT
    da.access_level,
    da.granted_at,
    da.expires_at,
    CASE
        WHEN da.expires_at IS NULL THEN true
        WHEN da.expires_at > NOW() THEN true
        ELSE false
    END AS is_valid
FROM document_access da
WHERE da.document_id = 'DOC-456'
  AND da.entity_type = 'customer'
  AND da.entity_id = 'C123';
*/

-- Query 7: Get all customers/accounts with access to a document
-- Use Case: "Who has access to this document?"
-- Performance: Uses idx_document_access_document_id
/*
SELECT
    da.entity_type,
    da.entity_id,
    da.entity_name,
    da.access_level,
    da.granted_at,
    da.granted_by,
    da.expires_at
FROM document_access da
WHERE da.document_id = 'DOC-789'
  AND (da.expires_at IS NULL OR da.expires_at > NOW())
ORDER BY da.access_level DESC, da.granted_at ASC;
*/

-- Query 8: Find documents owned by a customer (not just accessible)
-- Use Case: "Show documents uploaded/owned by customer C123"
-- Performance: Uses idx_document_access_level
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.uploaded_at,
    d.sharing_scope
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'customer'
  AND da.entity_id = 'C123'
  AND da.access_level = 'owner'
  AND d.status = 'active'
ORDER BY d.uploaded_at DESC;
*/

-- Query 9: Documents accessible to multiple entities (customer + account)
-- Use Case: "Show documents accessible to customer C123 OR their account A456"
-- Performance: Uses idx_document_access_entity
/*
SELECT DISTINCT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_date
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE (
    (da.entity_type = 'customer' AND da.entity_id = 'C123')
    OR (da.entity_type = 'account' AND da.entity_id = 'A456')
)
  AND d.status = 'active'
  AND (da.expires_at IS NULL OR da.expires_at > NOW())
ORDER BY d.document_date DESC;
*/

-- Query 10: Documents by template for a customer
-- Use Case: "Get all documents created with LOAN_APPLICATION template for customer C123"
-- Performance: Uses multiple indexes
/*
SELECT
    d.document_id,
    d.document_name,
    d.template_code,
    d.template_version,
    d.document_date,
    da.access_level
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'customer'
  AND da.entity_id = 'C123'
  AND d.template_code = 'LOAN_APPLICATION'
  AND d.status = 'active'
ORDER BY d.document_date DESC;
*/


-- ============================================================================
-- MATERIALIZED VIEW: Customer Document Summary (Updated for v2)
-- ============================================================================

CREATE MATERIALIZED VIEW mv_customer_document_summary AS
SELECT
    da.entity_id AS customer_id,
    da.entity_name AS customer_name,
    COUNT(DISTINCT d.document_id) AS total_documents,
    COUNT(DISTINCT d.document_type) AS unique_document_types,
    COUNT(DISTINCT d.document_id) FILTER (WHERE d.is_confidential = true) AS confidential_count,
    COUNT(DISTINCT d.document_id) FILTER (WHERE d.status = 'active') AS active_documents,
    COUNT(DISTINCT d.document_id) FILTER (WHERE d.status = 'archived') AS archived_documents,
    COUNT(DISTINCT d.document_id) FILTER (WHERE da.access_level = 'owner') AS owned_documents,
    COUNT(DISTINCT d.document_id) FILTER (WHERE d.sharing_scope = 'shared') AS shared_documents,
    MAX(d.uploaded_at) AS last_upload_date,
    SUM(d.file_size_bytes) AS total_storage_bytes
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'customer'
  AND d.status IN ('active', 'archived')
GROUP BY da.entity_id, da.entity_name;

CREATE UNIQUE INDEX idx_mv_customer_summary ON mv_customer_document_summary(customer_id);

COMMENT ON MATERIALIZED VIEW mv_customer_document_summary IS 'Pre-aggregated customer document statistics (refresh hourly)';

-- Refresh command: REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_document_summary;


-- ============================================================================
-- UTILITY VIEWS AND FUNCTIONS
-- ============================================================================

-- View: Documents with Access Summary
CREATE VIEW v_documents_with_access AS
SELECT
    d.document_id,
    d.ecms_document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.sharing_scope,
    d.status,
    d.uploaded_at,
    COUNT(da.access_id) AS access_count,
    array_agg(da.entity_type || ':' || da.entity_id ORDER BY da.entity_type, da.entity_id)
        FILTER (WHERE da.entity_type != 'public') AS accessible_to
FROM documents d
LEFT JOIN document_access da ON d.document_id = da.document_id
GROUP BY d.document_id, d.ecms_document_id, d.document_name, d.document_type,
         d.document_category, d.sharing_scope, d.status, d.uploaded_at;

COMMENT ON VIEW v_documents_with_access IS 'Shows documents with access control summary';


-- Function: Grant document access to entity
CREATE OR REPLACE FUNCTION grant_document_access(
    p_document_id UUID,
    p_entity_type VARCHAR,
    p_entity_id VARCHAR,
    p_entity_name VARCHAR,
    p_access_level VARCHAR DEFAULT 'view',
    p_granted_by VARCHAR DEFAULT 'system',
    p_expires_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_access_id UUID;
BEGIN
    -- Insert or update access record
    INSERT INTO document_access (
        document_id,
        entity_type,
        entity_id,
        entity_name,
        access_level,
        granted_by,
        expires_at
    ) VALUES (
        p_document_id,
        p_entity_type,
        p_entity_id,
        p_entity_name,
        p_access_level,
        p_granted_by,
        p_expires_at
    )
    ON CONFLICT (document_id, entity_type, entity_id)
    DO UPDATE SET
        access_level = EXCLUDED.access_level,
        granted_by = EXCLUDED.granted_by,
        expires_at = EXCLUDED.expires_at,
        updated_at = NOW()
    RETURNING access_id INTO v_access_id;

    RETURN v_access_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION grant_document_access IS 'Grants or updates access to a document for an entity';


-- Function: Revoke document access
CREATE OR REPLACE FUNCTION revoke_document_access(
    p_document_id UUID,
    p_entity_type VARCHAR,
    p_entity_id VARCHAR
)
RETURNS BOOLEAN AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM document_access
    WHERE document_id = p_document_id
      AND entity_type = p_entity_type
      AND entity_id = p_entity_id;

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count > 0;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION revoke_document_access IS 'Revokes access to a document for an entity';


-- Function: Check if entity has access to document
CREATE OR REPLACE FUNCTION has_document_access(
    p_document_id UUID,
    p_entity_type VARCHAR,
    p_entity_id VARCHAR
)
RETURNS BOOLEAN AS $$
DECLARE
    v_has_access BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM document_access da
        WHERE da.document_id = p_document_id
          AND da.entity_type = p_entity_type
          AND da.entity_id = p_entity_id
          AND (da.expires_at IS NULL OR da.expires_at > NOW())
    ) INTO v_has_access;

    RETURN v_has_access;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION has_document_access IS 'Checks if an entity has access to a document';


-- ============================================================================
-- ANALYSIS & MAINTENANCE
-- ============================================================================

-- Analyze tables for query planner statistics
ANALYZE templates;
ANALYZE template_rules;
ANALYZE documents;
ANALYZE document_access;
