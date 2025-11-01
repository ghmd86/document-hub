-- ============================================================================
-- Document Hub - PostgreSQL Schema Design v3 (HYBRID APPROACH)
-- ============================================================================
-- Purpose: Best of both worlds - denormalized + shared documents
-- Design: Primary owner denormalized, additional access via junction table
-- Performance: Fast path for private docs (no join), shared path for multi-access
-- ============================================================================

-- Strategy:
-- - 90% of documents: Single owner → Use denormalized fields (FAST)
-- - 10% of documents: Shared → Use document_access table (FLEXIBLE)
-- - Query optimizer chooses path based on sharing_scope field

-- ============================================================================
-- 1. TEMPLATES TABLE (Unchanged)
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

    -- Unique Constraint
    CONSTRAINT unique_template_version UNIQUE (template_code, version_number)
);

-- Indexes for Templates
CREATE INDEX idx_templates_code ON templates(template_code);
CREATE INDEX idx_templates_status ON templates(status);
CREATE INDEX idx_templates_type_category ON templates(document_type, document_category);
CREATE INDEX idx_templates_active ON templates(template_code, version_number) WHERE status = 'active';

COMMENT ON TABLE templates IS 'Template definitions with denormalized versioning';


-- ============================================================================
-- 2. TEMPLATE_RULES TABLE (Unchanged)
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

    -- Rule Configuration
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

COMMENT ON TABLE template_rules IS 'Normalized business rules for templates';


-- ============================================================================
-- 3. DOCUMENTS TABLE (HYBRID: Denormalized + Sharing Support)
-- ============================================================================
-- Key Design: Denormalized primary owner + sharing_scope indicator
-- Fast path: sharing_scope = 'private' → Use primary_customer_id (no join)
-- Shared path: sharing_scope = 'shared' → Join document_access table

CREATE TABLE documents (
    -- Primary Key
    document_id UUID NOT NULL DEFAULT gen_random_uuid(),

    -- ECMS Integration
    ecms_document_id VARCHAR(255) NOT NULL UNIQUE,

    -- PRIMARY OWNER (Denormalized for Fast Queries)
    primary_customer_id VARCHAR(100) NOT NULL,      -- ✅ Denormalized (required)
    primary_customer_name VARCHAR(255),             -- ✅ Denormalized
    primary_account_id VARCHAR(100),                -- ✅ Denormalized
    primary_account_type VARCHAR(100),              -- ✅ Denormalized

    -- Document Classification
    document_type VARCHAR(100) NOT NULL,
    document_category VARCHAR(100) NOT NULL,
    document_sub_category VARCHAR(100),

    -- Template Reference
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

    -- SHARING SCOPE (Key Field for Hybrid Approach)
    sharing_scope VARCHAR(20) NOT NULL DEFAULT 'private'
        CHECK (sharing_scope IN ('private', 'shared', 'public')),
    -- 'private': Single owner (use primary_customer_id - FAST PATH)
    -- 'shared': Multiple owners (use document_access - SHARED PATH)
    -- 'public': Everyone (use document_access - PUBLIC PATH)

    -- Date Fields
    document_date DATE,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMP WITH TIME ZONE,
    expiry_date DATE,

    -- Business Fields
    is_confidential BOOLEAN DEFAULT false,
    requires_signature BOOLEAN DEFAULT false,
    signature_status VARCHAR(50),
    tags TEXT[],

    -- Additional Metadata
    metadata JSONB,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),

    -- Primary Key (includes partition key)
    PRIMARY KEY (document_id, primary_customer_id)
) PARTITION BY HASH (primary_customer_id);

COMMENT ON TABLE documents IS 'Hybrid: Denormalized primary owner + sharing_scope for optimal query performance';
COMMENT ON COLUMN documents.primary_customer_id IS 'Primary owner (denormalized) - used for fast queries and partitioning';
COMMENT ON COLUMN documents.sharing_scope IS 'private=fast path (no join), shared=use document_access, public=global';


-- ============================================================================
-- PARTITIONING: Hash by primary_customer_id (Optimal for Most Queries)
-- ============================================================================
-- Benefit: Fast private document queries leverage partition pruning

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
-- 4. DOCUMENT_ACCESS TABLE (For Shared/Public Documents Only)
-- ============================================================================
-- Only populated when sharing_scope = 'shared' or 'public'
-- Private documents (90% of cases) don't need entries here

CREATE TABLE document_access (
    -- Primary Key
    access_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Key to Document
    document_id UUID NOT NULL,

    -- Entity Access
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('customer', 'account', 'user', 'public')),
    entity_id VARCHAR(100),  -- NULL for entity_type = 'public'
    entity_name VARCHAR(255),

    -- Access Control
    access_level VARCHAR(20) NOT NULL DEFAULT 'view'
        CHECK (access_level IN ('owner', 'view', 'edit', 'admin')),

    -- Access Grant Details
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    granted_by VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Unique Constraint
    CONSTRAINT unique_document_entity_access UNIQUE (document_id, entity_type, entity_id)
);

-- Indexes for document_access
CREATE INDEX idx_document_access_document_id ON document_access(document_id);
CREATE INDEX idx_document_access_entity ON document_access(entity_type, entity_id);
CREATE INDEX idx_document_access_customer_id ON document_access(entity_id) WHERE entity_type = 'customer';
CREATE INDEX idx_document_access_account_id ON document_access(entity_id) WHERE entity_type = 'account';
CREATE INDEX idx_document_access_entity_document ON document_access(entity_type, entity_id, document_id);

COMMENT ON TABLE document_access IS 'Additional access control for shared/public documents only';


-- ============================================================================
-- INDEXES FOR DOCUMENTS TABLE (Optimized for Hybrid Approach)
-- ============================================================================

-- FAST PATH: Primary owner queries (most common - 90% of queries)
CREATE INDEX idx_documents_primary_customer_type ON documents(primary_customer_id, document_type);
CREATE INDEX idx_documents_primary_customer_category ON documents(primary_customer_id, document_category);
CREATE INDEX idx_documents_primary_account_type ON documents(primary_account_id, document_type);
CREATE INDEX idx_documents_primary_account_category ON documents(primary_account_id, document_category);

-- Sharing scope for query path selection
CREATE INDEX idx_documents_sharing_scope ON documents(sharing_scope);

-- Partial indexes for fast path (private documents only)
CREATE INDEX idx_documents_private_customer ON documents(primary_customer_id, document_type)
    WHERE sharing_scope = 'private';

-- Query by template
CREATE INDEX idx_documents_template_id ON documents(template_id);
CREATE INDEX idx_documents_template_code ON documents(template_code, template_version);

-- Query by ECMS ID
CREATE INDEX idx_documents_ecms_id ON documents(ecms_document_id);

-- Query by status
CREATE INDEX idx_documents_status ON documents(status);

-- Query by date ranges
CREATE INDEX idx_documents_date_range ON documents(primary_customer_id, document_date);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);

-- Full-text search
CREATE INDEX idx_documents_name_gin ON documents USING gin(to_tsvector('english', document_name));

-- JSONB and array indexes
CREATE INDEX idx_documents_metadata_gin ON documents USING gin(metadata);
CREATE INDEX idx_documents_tags_gin ON documents USING gin(tags);

-- Partial indexes for subsets
CREATE INDEX idx_documents_active ON documents(primary_customer_id) WHERE status = 'active';
CREATE INDEX idx_documents_shared ON documents(document_id) WHERE sharing_scope = 'shared';
CREATE INDEX idx_documents_confidential ON documents(primary_customer_id) WHERE is_confidential = true;


-- ============================================================================
-- TRIGGERS
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
-- SAMPLE QUERIES (Optimized for Hybrid Approach)
-- ============================================================================

-- Query 1: FAST PATH - Get private documents for customer (NO JOIN)
-- Use Case: "Show me all my loan applications"
-- Performance: 10-15ms (same as v1 - optimal)
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_date,
    status,
    uploaded_at
FROM documents
WHERE primary_customer_id = 'C123'
  AND document_type = 'LOAN_APPLICATION'
  AND sharing_scope = 'private'
  AND status = 'active'
ORDER BY document_date DESC
LIMIT 100;
*/

-- Query 2: COMPLETE VIEW - Get ALL documents for customer (private + shared)
-- Use Case: "Show me everything I have access to"
-- Performance: Private docs fast (10-15ms), shared docs join (15-20ms)
/*
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_date,
    d.sharing_scope,
    CASE
        WHEN d.sharing_scope = 'private' THEN 'owner'
        ELSE da.access_level
    END AS access_level
FROM documents d
LEFT JOIN document_access da ON d.document_id = da.document_id
    AND da.entity_type = 'customer'
    AND da.entity_id = 'C123'
WHERE (
    -- Fast path: Primary owner
    (d.primary_customer_id = 'C123' AND d.sharing_scope = 'private')
    OR
    -- Shared path: Additional access
    (da.document_id IS NOT NULL AND d.sharing_scope IN ('shared', 'public'))
)
  AND d.status = 'active'
ORDER BY d.document_date DESC
LIMIT 100;
*/

-- Query 3: FAST PATH - Account documents (private only)
-- Use Case: "Show me statements for my checking account"
-- Performance: 10-15ms (optimal)
/*
SELECT
    document_id,
    document_name,
    document_category,
    document_date,
    file_size_bytes
FROM documents
WHERE primary_account_id = 'A456'
  AND document_category = 'STATEMENTS'
  AND sharing_scope = 'private'
  AND status = 'active'
ORDER BY document_date DESC;
*/

-- Query 4: Find shared documents
-- Use Case: "Show me documents I've shared with others"
-- Performance: 15-20ms (uses document_access)
/*
SELECT
    d.document_id,
    d.document_name,
    d.primary_customer_name AS owner_name,
    d.sharing_scope,
    COUNT(da.access_id) AS shared_with_count,
    array_agg(da.entity_type || ':' || da.entity_id) AS shared_entities
FROM documents d
JOIN document_access da ON d.document_id = da.document_id
WHERE d.primary_customer_id = 'C123'
  AND d.sharing_scope = 'shared'
  AND da.entity_id != 'C123'  -- Exclude self
GROUP BY d.document_id, d.document_name, d.primary_customer_name, d.sharing_scope
ORDER BY shared_with_count DESC;
*/

-- Query 5: Check if customer can access document (with primary owner optimization)
-- Use Case: "Can customer C456 access this document?"
-- Performance: 5ms (fast check)
/*
SELECT
    CASE
        WHEN d.primary_customer_id = 'C456' AND d.sharing_scope = 'private' THEN 'owner'
        WHEN d.sharing_scope = 'public' THEN 'view'
        WHEN da.access_level IS NOT NULL THEN da.access_level
        ELSE 'none'
    END AS access_level
FROM documents d
LEFT JOIN document_access da ON d.document_id = da.document_id
    AND da.entity_type = 'customer'
    AND da.entity_id = 'C456'
WHERE d.document_id = 'doc-uuid-123';
*/

-- Query 6: Get documents by template (mixed private/shared)
-- Use Case: "All documents using LOAN_APPLICATION template"
-- Performance: 10-15ms (uses template index)
/*
SELECT
    document_id,
    document_name,
    primary_customer_id,
    primary_customer_name,
    sharing_scope,
    document_date
FROM documents
WHERE template_code = 'LOAN_APPLICATION'
  AND status = 'active'
ORDER BY document_date DESC;
*/

-- Query 7: Search with full-text (private documents only - fast)
-- Use Case: "Find documents containing 'mortgage'"
-- Performance: 20-50ms (GIN index)
/*
SELECT
    document_id,
    document_name,
    primary_customer_id,
    document_type,
    ts_rank(to_tsvector('english', document_name), query) AS rank
FROM documents,
     to_tsquery('english', 'mortgage') query
WHERE to_tsvector('english', document_name) @@ query
  AND primary_customer_id = 'C123'
  AND sharing_scope = 'private'
  AND status = 'active'
ORDER BY rank DESC
LIMIT 50;
*/

-- Query 8: Get public documents
-- Use Case: "Show all terms and conditions documents"
-- Performance: 10-15ms
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_category,
    uploaded_at
FROM documents
WHERE sharing_scope = 'public'
  AND status = 'active'
ORDER BY uploaded_at DESC;
*/

-- Query 9: Analytics - Document count by sharing type
-- Use Case: "How many documents are private vs shared?"
/*
SELECT
    sharing_scope,
    COUNT(*) AS document_count,
    COUNT(DISTINCT primary_customer_id) AS unique_customers,
    AVG(file_size_bytes) AS avg_size_bytes
FROM documents
WHERE status = 'active'
GROUP BY sharing_scope
ORDER BY document_count DESC;
*/

-- Query 10: Find documents with expiring shared access
-- Use Case: "Which shared access grants are expiring soon?"
/*
SELECT
    d.document_name,
    d.primary_customer_name AS owner,
    da.entity_type,
    da.entity_id,
    da.entity_name,
    da.expires_at,
    EXTRACT(DAY FROM (da.expires_at - NOW())) AS days_remaining
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.expires_at IS NOT NULL
  AND da.expires_at > NOW()
  AND da.expires_at < NOW() + INTERVAL '7 days'
ORDER BY da.expires_at ASC;
*/


-- ============================================================================
-- MATERIALIZED VIEW: Customer Document Summary (Hybrid)
-- ============================================================================

CREATE MATERIALIZED VIEW mv_customer_document_summary AS
SELECT
    d.primary_customer_id AS customer_id,
    d.primary_customer_name AS customer_name,
    COUNT(*) AS total_owned_documents,
    COUNT(*) FILTER (WHERE d.sharing_scope = 'private') AS private_documents,
    COUNT(*) FILTER (WHERE d.sharing_scope = 'shared') AS shared_documents,
    COUNT(DISTINCT d.document_type) AS unique_document_types,
    COUNT(*) FILTER (WHERE d.is_confidential = true) AS confidential_count,
    COUNT(*) FILTER (WHERE d.status = 'active') AS active_documents,
    MAX(d.uploaded_at) AS last_upload_date,
    SUM(d.file_size_bytes) AS total_storage_bytes
FROM documents d
WHERE d.status IN ('active', 'archived')
GROUP BY d.primary_customer_id, d.primary_customer_name;

CREATE UNIQUE INDEX idx_mv_customer_summary ON mv_customer_document_summary(customer_id);

-- Additional view: Documents accessible to customer (including shared access)
CREATE MATERIALIZED VIEW mv_customer_accessible_documents AS
SELECT
    'C' || generate_series AS customer_id,  -- Placeholder, regenerate per customer
    COUNT(DISTINCT d.document_id) AS total_accessible,
    COUNT(DISTINCT d.document_id) FILTER (WHERE d.primary_customer_id = 'C' || generate_series) AS owned,
    COUNT(DISTINCT d.document_id) FILTER (WHERE da.access_id IS NOT NULL) AS shared_with_me
FROM generate_series(1, 1000) -- Adjust range based on customer IDs
LEFT JOIN documents d ON d.primary_customer_id = 'C' || generate_series
LEFT JOIN document_access da ON da.entity_id = 'C' || generate_series AND da.entity_type = 'customer'
GROUP BY 'C' || generate_series;


-- ============================================================================
-- UTILITY FUNCTIONS
-- ============================================================================

-- Function: Create document with primary owner
CREATE OR REPLACE FUNCTION create_document_hybrid(
    p_ecms_document_id VARCHAR,
    p_primary_customer_id VARCHAR,
    p_primary_customer_name VARCHAR,
    p_primary_account_id VARCHAR,
    p_primary_account_type VARCHAR,
    p_document_type VARCHAR,
    p_document_category VARCHAR,
    p_template_code VARCHAR,
    p_document_name VARCHAR,
    p_file_extension VARCHAR,
    p_file_size_bytes BIGINT,
    p_mime_type VARCHAR,
    p_sharing_scope VARCHAR DEFAULT 'private',
    p_metadata JSONB DEFAULT '{}',
    p_tags TEXT[] DEFAULT '{}',
    p_created_by VARCHAR DEFAULT 'system'
)
RETURNS UUID AS $$
DECLARE
    v_document_id UUID;
    v_template_id UUID;
    v_template_version INTEGER;
BEGIN
    -- Get active template
    SELECT template_id, version_number
    INTO v_template_id, v_template_version
    FROM templates
    WHERE template_code = p_template_code
      AND status = 'active'
    LIMIT 1;

    -- Insert document
    INSERT INTO documents (
        ecms_document_id,
        primary_customer_id,
        primary_customer_name,
        primary_account_id,
        primary_account_type,
        document_type,
        document_category,
        template_id,
        template_code,
        template_version,
        document_name,
        file_extension,
        file_size_bytes,
        mime_type,
        status,
        sharing_scope,
        document_date,
        metadata,
        tags,
        created_by
    ) VALUES (
        p_ecms_document_id,
        p_primary_customer_id,
        p_primary_customer_name,
        p_primary_account_id,
        p_primary_account_type,
        p_document_type,
        p_document_category,
        v_template_id,
        p_template_code,
        v_template_version,
        p_document_name,
        p_file_extension,
        p_file_size_bytes,
        p_mime_type,
        'active',
        p_sharing_scope,
        CURRENT_DATE,
        p_metadata,
        p_tags,
        p_created_by
    )
    RETURNING document_id INTO v_document_id;

    -- If shared/public, also create entry in document_access for primary owner
    IF p_sharing_scope != 'private' THEN
        INSERT INTO document_access (
            document_id,
            entity_type,
            entity_id,
            entity_name,
            access_level,
            granted_by
        ) VALUES (
            v_document_id,
            'customer',
            p_primary_customer_id,
            p_primary_customer_name,
            'owner',
            p_created_by
        );
    END IF;

    RETURN v_document_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_document_hybrid IS 'Create document with primary owner (hybrid approach)';


-- Function: Share document (converts private to shared)
CREATE OR REPLACE FUNCTION share_document_hybrid(
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
    v_current_scope VARCHAR;
    v_primary_customer_id VARCHAR;
    v_primary_customer_name VARCHAR;
BEGIN
    -- Get current document info
    SELECT sharing_scope, primary_customer_id, primary_customer_name
    INTO v_current_scope, v_primary_customer_id, v_primary_customer_name
    FROM documents
    WHERE document_id = p_document_id;

    -- If currently private, convert to shared
    IF v_current_scope = 'private' THEN
        UPDATE documents
        SET sharing_scope = 'shared',
            updated_at = NOW()
        WHERE document_id = p_document_id;

        -- Create access entry for primary owner
        INSERT INTO document_access (
            document_id,
            entity_type,
            entity_id,
            entity_name,
            access_level,
            granted_by
        ) VALUES (
            p_document_id,
            'customer',
            v_primary_customer_id,
            v_primary_customer_name,
            'owner',
            p_granted_by
        );
    END IF;

    -- Grant access to new entity
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

COMMENT ON FUNCTION share_document_hybrid IS 'Share document (converts private to shared if needed)';


-- Function: Check access (optimized for hybrid)
CREATE OR REPLACE FUNCTION has_access_hybrid(
    p_document_id UUID,
    p_entity_type VARCHAR,
    p_entity_id VARCHAR
)
RETURNS BOOLEAN AS $$
DECLARE
    v_has_access BOOLEAN;
BEGIN
    -- Check if entity has access
    SELECT EXISTS (
        SELECT 1
        FROM documents d
        WHERE d.document_id = p_document_id
          AND (
              -- Private: Check primary owner
              (d.sharing_scope = 'private' AND d.primary_customer_id = p_entity_id AND p_entity_type = 'customer')
              OR
              -- Shared/Public: Check document_access
              (d.sharing_scope IN ('shared', 'public') AND EXISTS (
                  SELECT 1
                  FROM document_access da
                  WHERE da.document_id = d.document_id
                    AND da.entity_type = p_entity_type
                    AND (da.entity_id = p_entity_id OR da.entity_type = 'public')
                    AND (da.expires_at IS NULL OR da.expires_at > NOW())
              ))
          )
    ) INTO v_has_access;

    RETURN v_has_access;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION has_access_hybrid IS 'Check access using hybrid approach (fast for private docs)';


-- ============================================================================
-- ANALYSIS & MAINTENANCE
-- ============================================================================

ANALYZE templates;
ANALYZE template_rules;
ANALYZE documents;
ANALYZE document_access;

-- Vacuum (run periodically)
-- VACUUM ANALYZE documents;
-- VACUUM ANALYZE document_access;
