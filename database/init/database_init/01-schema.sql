-- ============================================================================
-- Document Hub - PostgreSQL Schema (Final Design with Shared Documents)
-- ============================================================================
-- Purpose: Metadata indexing and retrieval layer over ECMS
-- Features:
--   - Customer-specific documents (account statements, applications)
--   - Shared documents (privacy policies, notices) - stored once, apply to many
--   - Template versioning (denormalized)
--   - Timeline support (effective_from/effective_to)
--   - High-performance partitioning and indexing
-- ============================================================================

-- ============================================================================
-- 1. TEMPLATES TABLE
-- ============================================================================
-- Each row represents a complete template version
-- Denormalized versioning: versions stored as rows, not separate table

CREATE TABLE templates (
    -- Primary Key
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Template Identification
    template_code VARCHAR(100) NOT NULL,        -- e.g., 'LOAN_APPLICATION'
    version_number INTEGER NOT NULL,            -- 1, 2, 3, ...

    -- Template Metadata
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    document_type VARCHAR(100) NOT NULL,
    document_category VARCHAR(100) NOT NULL,

    -- Status Management
    status VARCHAR(20) NOT NULL CHECK (status IN ('active', 'deprecated', 'draft')),

    -- Shared Document Configuration
    is_shared_document BOOLEAN DEFAULT false,   -- Documents from this template are shared
    sharing_scope VARCHAR(50),                  -- 'all_customers', 'customer_segment', 'account_type', 'specific_customers'

    -- Business Metadata
    retention_period_days INTEGER,
    requires_signature BOOLEAN DEFAULT false,
    requires_approval BOOLEAN DEFAULT false,

    -- Template Configuration (flexible JSON for custom fields)
    configuration JSONB,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),

    -- Constraints
    UNIQUE (template_code, version_number)
);

-- Indexes for Templates
CREATE INDEX idx_templates_code_version ON templates(template_code, version_number);
CREATE INDEX idx_templates_status ON templates(status) WHERE status = 'active';
CREATE INDEX idx_templates_type_category ON templates(document_type, document_category);
CREATE INDEX idx_templates_shared ON templates(is_shared_document, sharing_scope) WHERE is_shared_document = true;

-- ============================================================================
-- 2. TEMPLATE RULES TABLE
-- ============================================================================
-- Normalized rules associated with template versions

CREATE TABLE template_rules (
    -- Primary Key
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Key
    template_id UUID NOT NULL REFERENCES templates(template_id) ON DELETE CASCADE,

    -- Rule Definition
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,             -- 'validation', 'transformation', 'business_logic'
    rule_expression TEXT NOT NULL,              -- SQL, regex, or custom DSL
    rule_scope VARCHAR(100),                    -- 'upload', 'retrieval', 'both'

    -- Execution
    execution_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT true,

    -- Error Handling
    error_message TEXT,
    severity VARCHAR(20) CHECK (severity IN ('error', 'warning', 'info')),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255)
);

-- Indexes for Template Rules
CREATE INDEX idx_template_rules_template_id ON template_rules(template_id);
CREATE INDEX idx_template_rules_active ON template_rules(template_id, execution_order) WHERE is_active = true;
CREATE INDEX idx_template_rules_type ON template_rules(rule_type);

-- ============================================================================
-- 3. DOCUMENTS TABLE
-- ============================================================================
-- Heavily denormalized for query performance
-- Supports both customer-specific and shared documents
-- Partitioned by customer_id for horizontal scalability

CREATE TABLE documents (
    -- Primary Key
    document_id UUID NOT NULL DEFAULT gen_random_uuid(),

    -- ECMS Integration
    ecms_document_id VARCHAR(255) NOT NULL,
    ecms_version VARCHAR(50),

    -- Shared Document Support
    is_shared BOOLEAN DEFAULT false,
    sharing_scope VARCHAR(50),                  -- 'all_customers', 'customer_segment', 'account_type', 'specific_customers'

    -- Timeline Support (for shared documents)
    effective_from TIMESTAMP WITH TIME ZONE,
    effective_to TIMESTAMP WITH TIME ZONE,

    -- Customer/Account Information (denormalized)
    -- For shared documents with 'all_customers' scope, use customer_id = '00000000-SHARED'
    customer_id VARCHAR(100) NOT NULL,          -- Partition key
    customer_name VARCHAR(255),
    account_id VARCHAR(100),
    account_type VARCHAR(100),

    -- Document Metadata (denormalized)
    document_name VARCHAR(500) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_category VARCHAR(100) NOT NULL,

    -- Template Reference
    template_id UUID REFERENCES templates(template_id),
    template_code VARCHAR(100),                 -- Denormalized for faster queries
    template_version INTEGER,                   -- Denormalized

    -- File Metadata
    file_extension VARCHAR(20),
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),

    -- Document Status
    status VARCHAR(50) NOT NULL DEFAULT 'active',

    -- Security
    is_confidential BOOLEAN DEFAULT false,
    access_level VARCHAR(50),

    -- Dates
    document_date DATE,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE,

    -- Flexible Metadata
    metadata JSONB,                             -- Custom fields
    tags TEXT[],                                -- Array of tags

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),

    -- Composite Primary Key (required for partitioning)
    PRIMARY KEY (document_id, customer_id)
) PARTITION BY HASH (customer_id);

-- Create 16 partitions (adjust based on scale)
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

-- Add unique constraint for ECMS document ID (must include partition key)
ALTER TABLE documents ADD CONSTRAINT documents_ecms_document_id_key UNIQUE (ecms_document_id, customer_id);

-- Indexes for Documents Table

-- Core indexes for customer-specific documents
CREATE INDEX idx_documents_customer_type ON documents(customer_id, document_type)
WHERE is_shared = false;

CREATE INDEX idx_documents_customer_category ON documents(customer_id, document_category)
WHERE is_shared = false;

CREATE INDEX idx_documents_account_type ON documents(account_id, document_type)
WHERE is_shared = false;

-- Indexes for shared documents
CREATE INDEX idx_documents_shared_all ON documents(is_shared, sharing_scope, effective_from, effective_to)
WHERE is_shared = true AND sharing_scope = 'all_customers';

CREATE INDEX idx_documents_shared_account_type ON documents(is_shared, sharing_scope, account_type, effective_from)
WHERE is_shared = true AND sharing_scope = 'account_type';

CREATE INDEX idx_documents_shared_specific ON documents(is_shared, sharing_scope, effective_from)
WHERE is_shared = true AND sharing_scope = 'specific_customers';

-- Timeline query optimization
CREATE INDEX idx_documents_timeline ON documents(effective_from, effective_to)
WHERE is_shared = true;

-- Template lookups
CREATE INDEX idx_documents_template_code ON documents(template_code, template_version);

-- Status queries
CREATE INDEX idx_documents_status ON documents(customer_id, status);

-- Date range queries
CREATE INDEX idx_documents_date_range ON documents(customer_id, document_date);

-- Full-text search on document names
CREATE INDEX idx_documents_name_gin ON documents USING gin(to_tsvector('english', document_name));

-- JSONB metadata queries
CREATE INDEX idx_documents_metadata_gin ON documents USING gin(metadata);

-- Array tag searches
CREATE INDEX idx_documents_tags_gin ON documents USING gin(tags);

-- ============================================================================
-- 4. DOCUMENT CUSTOMER MAPPING TABLE
-- ============================================================================
-- Many-to-many mapping for shared documents with 'specific_customers' scope
-- Only used when sharing_scope = 'specific_customers'

CREATE TABLE document_customer_mapping (
    -- Primary Key
    mapping_id UUID NOT NULL DEFAULT gen_random_uuid(),

    -- Foreign Keys
    document_id UUID NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    account_id VARCHAR(100),                    -- Optional: specific account association

    -- Assignment Metadata
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by VARCHAR(255),
    is_active BOOLEAN DEFAULT true,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),

    -- Constraints
    PRIMARY KEY (mapping_id, customer_id),
    UNIQUE (document_id, customer_id, account_id)
) PARTITION BY HASH (customer_id);

-- Create partitions for mapping table
CREATE TABLE document_customer_mapping_p0 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 0);
CREATE TABLE document_customer_mapping_p1 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 1);
CREATE TABLE document_customer_mapping_p2 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 2);
CREATE TABLE document_customer_mapping_p3 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 3);
CREATE TABLE document_customer_mapping_p4 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 4);
CREATE TABLE document_customer_mapping_p5 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 5);
CREATE TABLE document_customer_mapping_p6 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 6);
CREATE TABLE document_customer_mapping_p7 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 7);
CREATE TABLE document_customer_mapping_p8 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 8);
CREATE TABLE document_customer_mapping_p9 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 9);
CREATE TABLE document_customer_mapping_p10 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 10);
CREATE TABLE document_customer_mapping_p11 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 11);
CREATE TABLE document_customer_mapping_p12 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 12);
CREATE TABLE document_customer_mapping_p13 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 13);
CREATE TABLE document_customer_mapping_p14 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 14);
CREATE TABLE document_customer_mapping_p15 PARTITION OF document_customer_mapping FOR VALUES WITH (MODULUS 16, REMAINDER 15);

-- Indexes for Document Customer Mapping
CREATE INDEX idx_doc_customer_map_customer ON document_customer_mapping(customer_id, is_active)
WHERE is_active = true;

CREATE INDEX idx_doc_customer_map_document ON document_customer_mapping(document_id, is_active)
WHERE is_active = true;

-- ============================================================================
-- 5. HELPER FUNCTION: Get All Documents for Customer
-- ============================================================================
-- Retrieves customer-specific + shared documents applicable to a customer

CREATE OR REPLACE FUNCTION get_customer_documents(
    p_customer_id VARCHAR(100),
    p_document_type VARCHAR(100) DEFAULT NULL,
    p_as_of_date TIMESTAMP DEFAULT NOW()
)
RETURNS TABLE (
    document_id UUID,
    document_name VARCHAR,
    document_type VARCHAR,
    document_category VARCHAR,
    is_shared BOOLEAN,
    uploaded_at TIMESTAMP WITH TIME ZONE,
    effective_from TIMESTAMP WITH TIME ZONE,
    effective_to TIMESTAMP WITH TIME ZONE,
    template_name VARCHAR,
    template_version INTEGER
) AS $$
BEGIN
    RETURN QUERY
    -- Customer-specific documents
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.is_shared,
        d.uploaded_at,
        d.effective_from,
        d.effective_to,
        t.template_name,
        t.version_number
    FROM documents d
    LEFT JOIN templates t ON d.template_id = t.template_id
    WHERE d.customer_id = p_customer_id
      AND d.is_shared = false
      AND (p_document_type IS NULL OR d.document_type = p_document_type)

    UNION ALL

    -- Shared documents for all customers
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.is_shared,
        d.uploaded_at,
        d.effective_from,
        d.effective_to,
        t.template_name,
        t.version_number
    FROM documents d
    LEFT JOIN templates t ON d.template_id = t.template_id
    WHERE d.customer_id = '00000000-SHARED'
      AND d.is_shared = true
      AND d.sharing_scope = 'all_customers'
      AND d.effective_from <= p_as_of_date
      AND (d.effective_to IS NULL OR d.effective_to >= p_as_of_date)
      AND (p_document_type IS NULL OR d.document_type = p_document_type)

    UNION ALL

    -- Shared documents for specific customers
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.is_shared,
        d.uploaded_at,
        d.effective_from,
        d.effective_to,
        t.template_name,
        t.version_number
    FROM documents d
    INNER JOIN document_customer_mapping dcm ON d.document_id = dcm.document_id
    LEFT JOIN templates t ON d.template_id = t.template_id
    WHERE d.customer_id = '00000000-SHARED'
      AND d.is_shared = true
      AND d.sharing_scope = 'specific_customers'
      AND dcm.customer_id = p_customer_id
      AND dcm.is_active = true
      AND d.effective_from <= p_as_of_date
      AND (d.effective_to IS NULL OR d.effective_to >= p_as_of_date)
      AND (p_document_type IS NULL OR d.document_type = p_document_type)

    ORDER BY uploaded_at DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 6. MATERIALIZED VIEW: Customer Document Summary
-- ============================================================================

CREATE MATERIALIZED VIEW mv_customer_document_summary AS
SELECT
    customer_id,
    customer_name,
    COUNT(*) AS total_documents,
    COUNT(DISTINCT document_type) AS unique_document_types,
    COUNT(*) FILTER (WHERE is_confidential = true) AS confidential_count,
    COUNT(*) FILTER (WHERE status = 'active') AS active_documents,
    MAX(uploaded_at) AS last_upload_date,
    SUM(file_size_bytes) AS total_storage_bytes
FROM documents
WHERE status IN ('active', 'archived')
  AND is_shared = false
GROUP BY customer_id, customer_name;

CREATE UNIQUE INDEX idx_mv_customer_summary ON mv_customer_document_summary(customer_id);

-- ============================================================================
-- 7. UPDATE TRIGGER FOR updated_at
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_templates_updated_at BEFORE UPDATE ON templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_template_rules_updated_at BEFORE UPDATE ON template_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
