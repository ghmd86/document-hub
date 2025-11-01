-- ============================================================================
-- Document Hub - PostgreSQL Schema Design
-- ============================================================================
-- Purpose: Hybrid denormalized schema for document metadata indexing
-- Design: Documents denormalized, Templates versioned denormalized, Rules normalized
-- Scalability: Partitioning + Composite Indexing for millions of records
-- ============================================================================

-- ============================================================================
-- 1. TEMPLATES TABLE (Denormalized Versioning)
-- ============================================================================
-- Each version of a template is stored as a separate row
-- Same template_code with incremented version_number

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

-- Partial index for active templates (most frequently queried)
CREATE INDEX idx_templates_active ON templates(template_code, version_number) WHERE status = 'active';

COMMENT ON TABLE templates IS 'Stores template definitions with denormalized versioning (each version is a new row)';
COMMENT ON COLUMN templates.template_code IS 'Logical identifier for the template (e.g., LOAN_APPLICATION, ACCOUNT_STATEMENT)';
COMMENT ON COLUMN templates.version_number IS 'Sequential version number for the template_code';
COMMENT ON COLUMN templates.status IS 'Template status: active (in use), deprecated (no longer used), draft (not yet published)';


-- ============================================================================
-- 2. TEMPLATE_RULES TABLE (Normalized)
-- ============================================================================
-- Defines reusable business rules that can be attached to templates

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
CREATE INDEX idx_template_rules_execution_order ON template_rules(template_id, execution_order);

COMMENT ON TABLE template_rules IS 'Normalized table storing reusable business rules for templates';
COMMENT ON COLUMN template_rules.rule_type IS 'Type of rule: validation, transformation, business_logic, compliance';
COMMENT ON COLUMN template_rules.rule_expression IS 'Rule logic (could be SQL, regex, or custom DSL)';


-- ============================================================================
-- 3. DOCUMENTS TABLE (Denormalized for Query Performance)
-- ============================================================================
-- Partitioned by customer_id for horizontal scalability

CREATE TABLE documents (
    -- Primary Key
    document_id UUID NOT NULL DEFAULT gen_random_uuid(),

    -- ECMS Integration
    ecms_document_id VARCHAR(255) NOT NULL UNIQUE,

    -- Customer & Account Denormalized Fields (for fast filtering)
    customer_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255),
    account_id VARCHAR(100),
    account_type VARCHAR(100),

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
    PRIMARY KEY (document_id, customer_id)
) PARTITION BY HASH (customer_id);

COMMENT ON TABLE documents IS 'Denormalized document metadata with partitioning by customer_id for scalability';
COMMENT ON COLUMN documents.ecms_document_id IS 'Unique identifier from the ECMS system';
COMMENT ON COLUMN documents.customer_id IS 'Denormalized customer ID for fast filtering (partition key)';
COMMENT ON COLUMN documents.template_id IS 'Reference to specific template version used for this document';


-- ============================================================================
-- PARTITIONING STRATEGY: Hash Partitioning by customer_id
-- ============================================================================
-- Create 16 partitions (adjust based on expected data distribution)

CREATE TABLE documents_p0 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 0);
CREATE TABLE documents_p1 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 1);
CREATE TABLE documents_p2 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 2);
CREATE TABLE documents_p3 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 3);
CREATE TABLE documents_p4 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 4);
CREATE TABLE documents_p5 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 5);
CREATE TABLE documents_p6 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 6);
CREATE TABLE documents_p7 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 7);
CREATE TABLE documents_p8 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 8);
CREATE TABLE documents_p9 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 9);
CREATE TABLE documents_p10 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 10);
CREATE TABLE documents_p11 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 11);
CREATE TABLE documents_p12 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 12);
CREATE TABLE documents_p13 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 13);
CREATE TABLE documents_p14 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 14);
CREATE TABLE documents_p15 PARTITION OF documents FOR VALUES WITH (MODULUS 16, REMAINDER 15);


-- ============================================================================
-- INDEXES FOR DOCUMENTS TABLE
-- ============================================================================
-- Composite indexes for common query patterns

-- Most frequent: Query by customer + document type
CREATE INDEX idx_documents_customer_type ON documents(customer_id, document_type);

-- Query by customer + document category
CREATE INDEX idx_documents_customer_category ON documents(customer_id, document_category);

-- Query by account + document type
CREATE INDEX idx_documents_account_type ON documents(account_id, document_type);

-- Query by account + document category
CREATE INDEX idx_documents_account_category ON documents(account_id, document_category);

-- Query by template
CREATE INDEX idx_documents_template_id ON documents(template_id);
CREATE INDEX idx_documents_template_code ON documents(template_code, template_version);

-- Query by ECMS document ID (for lookups from ECMS)
CREATE INDEX idx_documents_ecms_id ON documents(ecms_document_id);

-- Query by status (for filtering active/archived documents)
CREATE INDEX idx_documents_status ON documents(status);

-- Query by date ranges (common for reporting)
CREATE INDEX idx_documents_date_range ON documents(customer_id, document_date);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);

-- Full-text search on document name (if needed)
CREATE INDEX idx_documents_name_gin ON documents USING gin(to_tsvector('english', document_name));

-- GIN index for JSONB metadata queries
CREATE INDEX idx_documents_metadata_gin ON documents USING gin(metadata);

-- GIN index for tags array
CREATE INDEX idx_documents_tags_gin ON documents USING gin(tags);

-- Partial indexes for frequently filtered subsets
CREATE INDEX idx_documents_active ON documents(customer_id) WHERE status = 'active';
CREATE INDEX idx_documents_confidential ON documents(customer_id) WHERE is_confidential = true;


-- ============================================================================
-- TRIGGER: Auto-update updated_at timestamp
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


-- ============================================================================
-- SAMPLE SQL QUERIES
-- ============================================================================

-- Query 1: Fetch documents by customerId + documentType
-- Use Case: "Show me all loan applications for customer C123"
-- Performance: Uses idx_documents_customer_type composite index
/*
SELECT
    document_id,
    document_name,
    document_date,
    status,
    template_code,
    uploaded_at
FROM documents
WHERE customer_id = 'C123'
  AND document_type = 'LOAN_APPLICATION'
  AND status = 'active'
ORDER BY document_date DESC
LIMIT 100;
*/

-- Query 2: Fetch documents by accountId + documentCategory
-- Use Case: "Get all statements for account A456"
-- Performance: Uses idx_documents_account_category composite index
/*
SELECT
    document_id,
    document_name,
    document_category,
    document_date,
    ecms_document_id,
    file_size_bytes
FROM documents
WHERE account_id = 'A456'
  AND document_category = 'STATEMENTS'
  AND status IN ('active', 'archived')
ORDER BY document_date DESC
LIMIT 50;
*/

-- Query 3: Fetch all documents linked to a template_code
-- Use Case: "Find all documents created using ACCOUNT_STATEMENT template"
-- Performance: Uses idx_documents_template_code composite index
/*
SELECT
    d.document_id,
    d.document_name,
    d.customer_id,
    d.account_id,
    d.template_version,
    t.template_name,
    t.status AS template_status
FROM documents d
JOIN templates t ON d.template_id = t.template_id
WHERE d.template_code = 'ACCOUNT_STATEMENT'
  AND d.status = 'active'
ORDER BY d.created_at DESC
LIMIT 100;
*/

-- Query 4: Fetch documents for a specific template version
-- Use Case: "Get all documents created with version 2 of LOAN_APPLICATION"
-- Performance: Uses idx_documents_template_code composite index
/*
SELECT
    document_id,
    document_name,
    customer_id,
    document_date,
    created_at
FROM documents
WHERE template_code = 'LOAN_APPLICATION'
  AND template_version = 2
ORDER BY created_at DESC;
*/

-- Query 5: Get active template with its rules
-- Use Case: "Fetch the current active template and all its validation rules"
-- Performance: Uses idx_templates_active and idx_template_rules_active indexes
/*
SELECT
    t.template_id,
    t.template_code,
    t.version_number,
    t.template_name,
    tr.rule_name,
    tr.rule_type,
    tr.rule_expression,
    tr.execution_order
FROM templates t
LEFT JOIN template_rules tr ON t.template_id = tr.template_id AND tr.is_active = true
WHERE t.template_code = 'LOAN_APPLICATION'
  AND t.status = 'active'
ORDER BY tr.execution_order;
*/

-- Query 6: Search documents by multiple criteria
-- Use Case: "Find confidential documents uploaded in the last 30 days for a customer"
-- Performance: Uses idx_documents_customer_type and idx_documents_confidential
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_category,
    uploaded_at,
    is_confidential
FROM documents
WHERE customer_id = 'C789'
  AND is_confidential = true
  AND uploaded_at >= NOW() - INTERVAL '30 days'
  AND status = 'active'
ORDER BY uploaded_at DESC;
*/

-- Query 7: Full-text search on document names
-- Use Case: "Search for documents containing 'mortgage' in the name"
-- Performance: Uses idx_documents_name_gin GIN index
/*
SELECT
    document_id,
    document_name,
    customer_id,
    document_type,
    ts_rank(to_tsvector('english', document_name), query) AS rank
FROM documents,
     to_tsquery('english', 'mortgage') query
WHERE to_tsvector('english', document_name) @@ query
  AND status = 'active'
ORDER BY rank DESC
LIMIT 50;
*/

-- Query 8: Get document count by template
-- Use Case: "How many documents were created using each template?"
-- Performance: Uses idx_documents_template_code index
/*
SELECT
    template_code,
    template_version,
    COUNT(*) AS document_count,
    COUNT(DISTINCT customer_id) AS unique_customers
FROM documents
WHERE status = 'active'
GROUP BY template_code, template_version
ORDER BY document_count DESC;
*/

-- Query 9: Find documents expiring soon
-- Use Case: "Get all documents expiring in the next 30 days for compliance"
-- Performance: Uses idx_documents_date_range or table scan with filter
/*
SELECT
    document_id,
    document_name,
    customer_id,
    account_id,
    expiry_date,
    document_type
FROM documents
WHERE expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
  AND status = 'active'
ORDER BY expiry_date ASC;
*/

-- Query 10: Query JSONB metadata
-- Use Case: "Find documents with specific metadata attributes"
-- Performance: Uses idx_documents_metadata_gin GIN index
/*
SELECT
    document_id,
    document_name,
    customer_id,
    metadata
FROM documents
WHERE metadata @> '{"loan_type": "mortgage"}'::jsonb
  AND customer_id = 'C123'
  AND status = 'active';
*/


-- ============================================================================
-- MATERIALIZED VIEW: Document Summary by Customer
-- ============================================================================
-- Pre-aggregated view for dashboard/reporting queries

CREATE MATERIALIZED VIEW mv_customer_document_summary AS
SELECT
    customer_id,
    customer_name,
    COUNT(*) AS total_documents,
    COUNT(DISTINCT document_type) AS unique_document_types,
    COUNT(*) FILTER (WHERE is_confidential = true) AS confidential_count,
    COUNT(*) FILTER (WHERE status = 'active') AS active_documents,
    COUNT(*) FILTER (WHERE status = 'archived') AS archived_documents,
    MAX(uploaded_at) AS last_upload_date,
    SUM(file_size_bytes) AS total_storage_bytes
FROM documents
WHERE status IN ('active', 'archived')
GROUP BY customer_id, customer_name;

CREATE UNIQUE INDEX idx_mv_customer_summary ON mv_customer_document_summary(customer_id);

-- Refresh materialized view (run periodically or via cron job)
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_document_summary;


-- ============================================================================
-- ADDITIONAL UTILITIES
-- ============================================================================

-- View: Get Latest Active Template Version
CREATE VIEW v_latest_active_templates AS
SELECT DISTINCT ON (template_code)
    template_id,
    template_code,
    version_number,
    template_name,
    document_type,
    document_category,
    created_at
FROM templates
WHERE status = 'active'
ORDER BY template_code, version_number DESC;

COMMENT ON VIEW v_latest_active_templates IS 'Shows the latest active version of each template';


-- Function: Get Template History
CREATE OR REPLACE FUNCTION get_template_history(p_template_code VARCHAR)
RETURNS TABLE (
    template_id UUID,
    version_number INTEGER,
    template_name VARCHAR,
    status VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.template_id,
        t.version_number,
        t.template_name,
        t.status,
        t.created_at,
        t.created_by
    FROM templates t
    WHERE t.template_code = p_template_code
    ORDER BY t.version_number DESC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_template_history IS 'Returns all versions of a template ordered by version number';


-- ============================================================================
-- ANALYSIS & MAINTENANCE QUERIES
-- ============================================================================

-- Analyze tables for query planner statistics
ANALYZE templates;
ANALYZE template_rules;
ANALYZE documents;

-- Vacuum tables to reclaim space and update statistics
-- VACUUM ANALYZE templates;
-- VACUUM ANALYZE template_rules;
-- VACUUM ANALYZE documents;
