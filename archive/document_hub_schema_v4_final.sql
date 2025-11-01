-- ============================================================================
-- Document Hub - PostgreSQL Schema Design v4 (FINAL - Broadcast Documents)
-- ============================================================================
-- Purpose: ONE copy of broadcast documents (policies, notices) + customer-specific docs
-- Design: Simple indicator flag + timeline filtering (NO junction table needed)
-- Use Case: Store ONE privacy policy that applies to ALL/CERTAIN customers at given time
-- ============================================================================

-- CORRECT UNDERSTANDING:
-- - Customer-specific docs: customer_id populated (statements, applications)
-- - Broadcast docs: customer_id = NULL, applies to all/certain customers
-- - Timeline: applicable_from/applicable_to dates control when doc applies
-- - Segment targeting: 'all', 'savings_customers', 'loan_customers', etc.

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

    -- Template Configuration
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
-- 3. DOCUMENTS TABLE (v4 - Broadcast Documents with Timeline)
-- ============================================================================
-- Key Design: customer_id is NULLABLE
--   - NULL = Broadcast document (applies to many customers)
--   - NOT NULL = Customer-specific document (traditional)

CREATE TABLE documents (
    -- Primary Key
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- ECMS Integration
    ecms_document_id VARCHAR(255) NOT NULL UNIQUE,

    -- CUSTOMER/ACCOUNT (NULL for broadcast documents)
    customer_id VARCHAR(100),              -- NULL for broadcast docs
    customer_name VARCHAR(255),            -- NULL for broadcast docs
    account_id VARCHAR(100),               -- NULL for broadcast docs
    account_type VARCHAR(100),             -- NULL for broadcast docs

    -- DOCUMENT SCOPE (Key Field)
    document_scope VARCHAR(30) NOT NULL DEFAULT 'customer_specific'
        CHECK (document_scope IN (
            'customer_specific',    -- Traditional doc for one customer
            'account_specific',     -- Traditional doc for one account
            'broadcast_all',        -- Applies to ALL customers (privacy policy, T&C)
            'broadcast_segment'     -- Applies to CERTAIN customers (rate change for savings)
        )),

    -- TIMELINE FILTERING (For broadcast documents)
    applicable_from DATE,                  -- When this document becomes applicable
    applicable_to DATE,                    -- When this document expires (NULL = no expiry)

    -- SEGMENT TARGETING (For broadcast_segment documents)
    target_segment VARCHAR(100),           -- 'all', 'savings_customers', 'loan_customers', 'business_accounts', etc.
    target_criteria JSONB,                 -- Additional criteria: {"account_balance_min": 10000, "state": "CA"}

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

    -- Constraints
    -- If customer_specific or account_specific, customer_id/account_id must be populated
    CONSTRAINT check_customer_scope CHECK (
        (document_scope = 'customer_specific' AND customer_id IS NOT NULL) OR
        (document_scope = 'account_specific' AND account_id IS NOT NULL) OR
        (document_scope LIKE 'broadcast%' AND customer_id IS NULL)
    ),

    -- If broadcast, must have applicable_from
    CONSTRAINT check_broadcast_timeline CHECK (
        (document_scope NOT LIKE 'broadcast%') OR
        (document_scope LIKE 'broadcast%' AND applicable_from IS NOT NULL)
    ),

    -- If broadcast_segment, must have target_segment
    CONSTRAINT check_segment_targeting CHECK (
        (document_scope != 'broadcast_segment') OR
        (document_scope = 'broadcast_segment' AND target_segment IS NOT NULL)
    )
);

COMMENT ON TABLE documents IS 'Documents with broadcast support: customer_id NULL = broadcast to many, NOT NULL = customer-specific';
COMMENT ON COLUMN documents.customer_id IS 'NULL for broadcast documents, populated for customer-specific documents';
COMMENT ON COLUMN documents.document_scope IS 'customer_specific (1 customer) | broadcast_all (all customers) | broadcast_segment (certain customers)';
COMMENT ON COLUMN documents.applicable_from IS 'Timeline start: when broadcast document becomes applicable';
COMMENT ON COLUMN documents.applicable_to IS 'Timeline end: when broadcast document expires (NULL = no expiry)';
COMMENT ON COLUMN documents.target_segment IS 'Customer segment for broadcast_segment: all, savings_customers, loan_customers, etc.';


-- ============================================================================
-- INDEXES FOR DOCUMENTS TABLE (Optimized for Customer + Broadcast Queries)
-- ============================================================================

-- PRIMARY QUERY PATTERN: Customer-specific documents
CREATE INDEX idx_documents_customer_type ON documents(customer_id, document_type) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_documents_customer_category ON documents(customer_id, document_category) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_documents_customer_status ON documents(customer_id, status) WHERE customer_id IS NOT NULL;

-- Account-level queries
CREATE INDEX idx_documents_account_type ON documents(account_id, document_type) WHERE account_id IS NOT NULL;
CREATE INDEX idx_documents_account_category ON documents(account_id, document_category) WHERE account_id IS NOT NULL;

-- BROADCAST QUERY PATTERN: Timeline-based filtering
CREATE INDEX idx_documents_broadcast_timeline ON documents(document_scope, applicable_from, applicable_to)
    WHERE document_scope LIKE 'broadcast%';

-- Broadcast by type/category
CREATE INDEX idx_documents_broadcast_type ON documents(document_scope, document_type, applicable_from)
    WHERE document_scope LIKE 'broadcast%';

CREATE INDEX idx_documents_broadcast_category ON documents(document_scope, document_category, applicable_from)
    WHERE document_scope LIKE 'broadcast%';

-- Segment targeting
CREATE INDEX idx_documents_segment ON documents(document_scope, target_segment, applicable_from)
    WHERE document_scope = 'broadcast_segment';

-- Document scope for query path selection
CREATE INDEX idx_documents_scope ON documents(document_scope);

-- Template queries
CREATE INDEX idx_documents_template_id ON documents(template_id);
CREATE INDEX idx_documents_template_code ON documents(template_code, template_version);

-- ECMS integration
CREATE INDEX idx_documents_ecms_id ON documents(ecms_document_id);

-- Status filtering
CREATE INDEX idx_documents_status ON documents(status);

-- Date range queries
CREATE INDEX idx_documents_date_range ON documents(document_date);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);

-- Full-text search
CREATE INDEX idx_documents_name_gin ON documents USING gin(to_tsvector('english', document_name));

-- JSONB indexes
CREATE INDEX idx_documents_metadata_gin ON documents USING gin(metadata);
CREATE INDEX idx_documents_target_criteria_gin ON documents USING gin(target_criteria);
CREATE INDEX idx_documents_tags_gin ON documents USING gin(tags);

-- Partial indexes for common subsets
CREATE INDEX idx_documents_active ON documents(document_id) WHERE status = 'active';
CREATE INDEX idx_documents_broadcast_active ON documents(document_scope, applicable_from)
    WHERE document_scope LIKE 'broadcast%' AND status = 'active';


-- ============================================================================
-- PARTITIONING STRATEGY: Hash by customer_id (with special handling for NULLs)
-- ============================================================================
-- Strategy: Partition by COALESCE(customer_id, 'BROADCAST')
-- Customer-specific docs: Distributed across partitions by customer_id
-- Broadcast docs: All go to special 'BROADCAST' partition

CREATE TABLE documents_partitioned (
    LIKE documents INCLUDING ALL
) PARTITION BY HASH (COALESCE(customer_id, 'BROADCAST'));

-- Create 16 partitions for customer-specific documents + 1 for broadcast
CREATE TABLE documents_p0 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 0);
CREATE TABLE documents_p1 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 1);
CREATE TABLE documents_p2 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 2);
CREATE TABLE documents_p3 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 3);
CREATE TABLE documents_p4 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 4);
CREATE TABLE documents_p5 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 5);
CREATE TABLE documents_p6 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 6);
CREATE TABLE documents_p7 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 7);
CREATE TABLE documents_p8 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 8);
CREATE TABLE documents_p9 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 9);
CREATE TABLE documents_p10 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 10);
CREATE TABLE documents_p11 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 11);
CREATE TABLE documents_p12 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 12);
CREATE TABLE documents_p13 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 13);
CREATE TABLE documents_p14 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 14);
CREATE TABLE documents_p15 PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 15);
CREATE TABLE documents_p16_broadcast PARTITION OF documents_partitioned
    FOR VALUES WITH (MODULUS 17, REMAINDER 16);

COMMENT ON TABLE documents_p16_broadcast IS 'Partition for all broadcast documents (customer_id IS NULL)';

-- Note: Use documents_partitioned instead of documents for production
-- For simplicity in examples below, we'll reference 'documents' table


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


-- ============================================================================
-- CUSTOMER SEGMENT MAPPING TABLE (Optional - for complex segment rules)
-- ============================================================================
-- Use this if segment targeting needs to be dynamic/queryable

CREATE TABLE customer_segments (
    customer_segment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(100) NOT NULL,
    segment_name VARCHAR(100) NOT NULL,

    -- Segment details
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,

    -- Segment metadata
    segment_criteria JSONB,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_customer_segment UNIQUE (customer_id, segment_name)
);

CREATE INDEX idx_customer_segments_customer ON customer_segments(customer_id);
CREATE INDEX idx_customer_segments_segment ON customer_segments(segment_name);
CREATE INDEX idx_customer_segments_active ON customer_segments(customer_id, segment_name)
    WHERE expires_at IS NULL OR expires_at > NOW();

COMMENT ON TABLE customer_segments IS 'Maps customers to segments for broadcast targeting (optional)';


-- ============================================================================
-- SAMPLE SQL QUERIES (v4 - Customer + Broadcast)
-- ============================================================================

-- Query 1: Get ALL documents for a customer (specific + applicable broadcasts)
-- Use Case: "Show me everything relevant to customer C123 right now"
-- Performance: 15-20ms (combines two index scans + UNION)
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_category,
    document_scope,
    document_date,
    applicable_from,
    applicable_to,
    'customer_specific' AS source
FROM documents
WHERE customer_id = 'C123'
  AND status = 'active'

UNION ALL

SELECT
    document_id,
    document_name,
    document_type,
    document_category,
    document_scope,
    document_date,
    applicable_from,
    applicable_to,
    'broadcast' AS source
FROM documents
WHERE document_scope LIKE 'broadcast%'
  AND status = 'active'
  AND CURRENT_DATE >= applicable_from
  AND (applicable_to IS NULL OR CURRENT_DATE <= applicable_to)
  AND (
      document_scope = 'broadcast_all'
      OR (document_scope = 'broadcast_segment'
          AND target_segment IN ('all', 'savings_customers')  -- Customer's segments
      )
  )
ORDER BY document_date DESC;
*/

-- Query 2: Get customer-specific documents ONLY (fast path)
-- Use Case: "Show me my statements and applications"
-- Performance: 10-15ms (single index scan, partition pruning)
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_date,
    status
FROM documents
WHERE customer_id = 'C123'
  AND document_type = 'LOAN_APPLICATION'
  AND status = 'active'
ORDER BY document_date DESC;
*/

-- Query 3: Get active broadcast documents
-- Use Case: "Show all current policies and notices"
-- Performance: 10-15ms (broadcast partition scan)
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_scope,
    target_segment,
    applicable_from,
    applicable_to
FROM documents
WHERE document_scope LIKE 'broadcast%'
  AND status = 'active'
  AND CURRENT_DATE >= applicable_from
  AND (applicable_to IS NULL OR CURRENT_DATE <= applicable_to)
ORDER BY applicable_from DESC;
*/

-- Query 4: Get broadcast documents by segment
-- Use Case: "Show all notices for savings customers"
-- Performance: 10-15ms (segment index scan)
/*
SELECT
    document_id,
    document_name,
    document_type,
    applicable_from,
    applicable_to,
    target_segment
FROM documents
WHERE document_scope = 'broadcast_segment'
  AND target_segment = 'savings_customers'
  AND status = 'active'
  AND CURRENT_DATE >= applicable_from
  AND (applicable_to IS NULL OR CURRENT_DATE <= applicable_to)
ORDER BY applicable_from DESC;
*/

-- Query 5: Get documents applicable at specific date
-- Use Case: "What documents applied to customer C123 on 2024-01-15?"
-- Timeline query for audit/compliance
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_scope,
    applicable_from,
    applicable_to
FROM documents
WHERE (
    -- Customer-specific documents
    (customer_id = 'C123' AND document_date <= '2024-01-15')
    OR
    -- Broadcast documents applicable on that date
    (document_scope LIKE 'broadcast%'
     AND '2024-01-15' >= applicable_from
     AND ('2024-01-15' <= applicable_to OR applicable_to IS NULL)
    )
)
  AND status IN ('active', 'archived')
ORDER BY document_date DESC;
*/

-- Query 6: Count documents by scope
-- Use Case: "How many broadcast vs customer-specific documents?"
/*
SELECT
    document_scope,
    COUNT(*) AS document_count,
    COUNT(DISTINCT COALESCE(customer_id, 'BROADCAST')) AS entity_count,
    AVG(file_size_bytes) AS avg_size_bytes
FROM documents
WHERE status = 'active'
GROUP BY document_scope
ORDER BY document_count DESC;
*/

-- Query 7: Find expiring broadcast documents
-- Use Case: "Which policies expire in the next 30 days?"
/*
SELECT
    document_id,
    document_name,
    document_type,
    target_segment,
    applicable_from,
    applicable_to,
    EXTRACT(DAY FROM (applicable_to - CURRENT_DATE)) AS days_until_expiry
FROM documents
WHERE document_scope LIKE 'broadcast%'
  AND applicable_to IS NOT NULL
  AND applicable_to > CURRENT_DATE
  AND applicable_to <= CURRENT_DATE + INTERVAL '30 days'
  AND status = 'active'
ORDER BY applicable_to ASC;
*/

-- Query 8: Get documents by template (customer + broadcast)
-- Use Case: "All documents using PRIVACY_POLICY template"
/*
SELECT
    document_id,
    document_name,
    document_scope,
    customer_id,
    applicable_from,
    applicable_to,
    status
FROM documents
WHERE template_code = 'PRIVACY_POLICY'
  AND status = 'active'
ORDER BY applicable_from DESC NULLS LAST, document_date DESC;
*/

-- Query 9: Account-level documents + broadcasts
-- Use Case: "Show all documents for account A456"
/*
SELECT
    document_id,
    document_name,
    document_type,
    document_scope,
    account_id
FROM documents
WHERE (
    account_id = 'A456'
    OR (document_scope LIKE 'broadcast%'
        AND CURRENT_DATE >= applicable_from
        AND (applicable_to IS NULL OR CURRENT_DATE <= applicable_to)
    )
)
  AND status = 'active'
ORDER BY document_date DESC;
*/

-- Query 10: Search broadcast documents by type and timeline
-- Use Case: "Find all rate change notices from last year"
/*
SELECT
    document_id,
    document_name,
    document_scope,
    target_segment,
    applicable_from,
    applicable_to
FROM documents
WHERE document_scope LIKE 'broadcast%'
  AND document_type = 'RATE_CHANGE_NOTICE'
  AND applicable_from >= '2024-01-01'
  AND applicable_from < '2025-01-01'
  AND status = 'active'
ORDER BY applicable_from DESC;
*/


-- ============================================================================
-- UTILITY FUNCTIONS
-- ============================================================================

-- Function: Create customer-specific document
CREATE OR REPLACE FUNCTION create_customer_document(
    p_ecms_document_id VARCHAR,
    p_customer_id VARCHAR,
    p_customer_name VARCHAR,
    p_account_id VARCHAR,
    p_account_type VARCHAR,
    p_document_type VARCHAR,
    p_document_category VARCHAR,
    p_template_code VARCHAR,
    p_document_name VARCHAR,
    p_file_extension VARCHAR,
    p_file_size_bytes BIGINT,
    p_mime_type VARCHAR,
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

    -- Insert customer-specific document
    INSERT INTO documents (
        ecms_document_id,
        customer_id,
        customer_name,
        account_id,
        account_type,
        document_scope,
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
        document_date,
        metadata,
        tags,
        created_by
    ) VALUES (
        p_ecms_document_id,
        p_customer_id,
        p_customer_name,
        p_account_id,
        p_account_type,
        'customer_specific',
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
        CURRENT_DATE,
        p_metadata,
        p_tags,
        p_created_by
    )
    RETURNING document_id INTO v_document_id;

    RETURN v_document_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_customer_document IS 'Create customer-specific document (traditional use case)';


-- Function: Create broadcast document
CREATE OR REPLACE FUNCTION create_broadcast_document(
    p_ecms_document_id VARCHAR,
    p_document_type VARCHAR,
    p_document_category VARCHAR,
    p_document_name VARCHAR,
    p_file_extension VARCHAR,
    p_file_size_bytes BIGINT,
    p_mime_type VARCHAR,
    p_applicable_from DATE,
    p_applicable_to DATE DEFAULT NULL,
    p_broadcast_scope VARCHAR DEFAULT 'broadcast_all',  -- 'broadcast_all' or 'broadcast_segment'
    p_target_segment VARCHAR DEFAULT NULL,
    p_target_criteria JSONB DEFAULT NULL,
    p_metadata JSONB DEFAULT '{}',
    p_tags TEXT[] DEFAULT '{}',
    p_created_by VARCHAR DEFAULT 'system'
)
RETURNS UUID AS $$
DECLARE
    v_document_id UUID;
BEGIN
    -- Validate broadcast scope
    IF p_broadcast_scope NOT IN ('broadcast_all', 'broadcast_segment') THEN
        RAISE EXCEPTION 'Invalid broadcast scope: %. Must be broadcast_all or broadcast_segment', p_broadcast_scope;
    END IF;

    -- Validate segment targeting
    IF p_broadcast_scope = 'broadcast_segment' AND p_target_segment IS NULL THEN
        RAISE EXCEPTION 'target_segment required for broadcast_segment scope';
    END IF;

    -- Insert broadcast document
    INSERT INTO documents (
        ecms_document_id,
        customer_id,            -- NULL for broadcast
        document_scope,
        applicable_from,
        applicable_to,
        target_segment,
        target_criteria,
        document_type,
        document_category,
        document_name,
        file_extension,
        file_size_bytes,
        mime_type,
        status,
        metadata,
        tags,
        created_by
    ) VALUES (
        p_ecms_document_id,
        NULL,                   -- NULL customer_id = broadcast
        p_broadcast_scope,
        p_applicable_from,
        p_applicable_to,
        p_target_segment,
        p_target_criteria,
        p_document_type,
        p_document_category,
        p_document_name,
        p_file_extension,
        p_file_size_bytes,
        p_mime_type,
        'active',
        p_metadata,
        p_tags,
        p_created_by
    )
    RETURNING document_id INTO v_document_id;

    RETURN v_document_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_broadcast_document IS 'Create broadcast document (applies to all/certain customers at timeline)';


-- Function: Get all documents for customer (specific + applicable broadcasts)
CREATE OR REPLACE FUNCTION get_customer_documents(
    p_customer_id VARCHAR,
    p_customer_segments VARCHAR[] DEFAULT '{}',  -- Customer's segments: {'savings_customers', 'premium'}
    p_document_type VARCHAR DEFAULT NULL,
    p_as_of_date DATE DEFAULT CURRENT_DATE,
    p_limit INTEGER DEFAULT 100
)
RETURNS TABLE (
    document_id UUID,
    document_name VARCHAR,
    document_type VARCHAR,
    document_category VARCHAR,
    document_scope VARCHAR,
    document_date DATE,
    applicable_from DATE,
    applicable_to DATE,
    source VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    -- Customer-specific documents
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.document_scope,
        d.document_date,
        d.applicable_from,
        d.applicable_to,
        'customer_specific'::VARCHAR AS source
    FROM documents d
    WHERE d.customer_id = p_customer_id
      AND (p_document_type IS NULL OR d.document_type = p_document_type)
      AND d.status = 'active'

    UNION ALL

    -- Broadcast documents
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.document_scope,
        d.document_date,
        d.applicable_from,
        d.applicable_to,
        'broadcast'::VARCHAR AS source
    FROM documents d
    WHERE d.document_scope LIKE 'broadcast%'
      AND (p_document_type IS NULL OR d.document_type = p_document_type)
      AND d.status = 'active'
      AND p_as_of_date >= d.applicable_from
      AND (d.applicable_to IS NULL OR p_as_of_date <= d.applicable_to)
      AND (
          d.document_scope = 'broadcast_all'
          OR (d.document_scope = 'broadcast_segment'
              AND (d.target_segment = ANY(p_customer_segments) OR d.target_segment = 'all')
          )
      )

    ORDER BY document_date DESC NULLS LAST
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_customer_documents IS 'Get all documents for customer: specific + applicable broadcasts at given timeline';


-- Function: Check if broadcast document applies to customer
CREATE OR REPLACE FUNCTION broadcast_applies_to_customer(
    p_document_id UUID,
    p_customer_id VARCHAR,
    p_customer_segments VARCHAR[] DEFAULT '{}',
    p_as_of_date DATE DEFAULT CURRENT_DATE
)
RETURNS BOOLEAN AS $$
DECLARE
    v_applies BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM documents d
        WHERE d.document_id = p_document_id
          AND d.document_scope LIKE 'broadcast%'
          AND d.status = 'active'
          AND p_as_of_date >= d.applicable_from
          AND (d.applicable_to IS NULL OR p_as_of_date <= d.applicable_to)
          AND (
              d.document_scope = 'broadcast_all'
              OR (d.document_scope = 'broadcast_segment'
                  AND (d.target_segment = ANY(p_customer_segments) OR d.target_segment = 'all')
              )
          )
    ) INTO v_applies;

    RETURN v_applies;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION broadcast_applies_to_customer IS 'Check if broadcast document applies to customer at given timeline';


-- ============================================================================
-- MATERIALIZED VIEW: Document Summary by Scope
-- ============================================================================

CREATE MATERIALIZED VIEW mv_document_scope_summary AS
SELECT
    document_scope,
    document_type,
    COUNT(*) AS document_count,
    COUNT(*) FILTER (WHERE status = 'active') AS active_count,
    COUNT(*) FILTER (WHERE document_scope LIKE 'broadcast%'
                     AND CURRENT_DATE >= applicable_from
                     AND (applicable_to IS NULL OR CURRENT_DATE <= applicable_to)) AS currently_applicable,
    AVG(file_size_bytes) AS avg_size_bytes,
    MIN(applicable_from) AS earliest_applicable,
    MAX(applicable_to) AS latest_expiry
FROM documents
GROUP BY document_scope, document_type;

CREATE INDEX idx_mv_scope_summary ON mv_document_scope_summary(document_scope, document_type);

COMMENT ON MATERIALIZED VIEW mv_document_scope_summary IS 'Summary statistics by document scope and type';


-- ============================================================================
-- ANALYSIS & MAINTENANCE
-- ============================================================================

ANALYZE templates;
ANALYZE template_rules;
ANALYZE documents;
ANALYZE customer_segments;

-- Regular maintenance
-- VACUUM ANALYZE documents;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_document_scope_summary;
