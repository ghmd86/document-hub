-- ============================================================================
-- Document Selection Rules - Database Schema
-- ============================================================================
-- This schema supports storing, versioning, and managing document selection
-- rules externally, allowing updates without code deployment
-- ============================================================================

-- Rule Sets (versions of rule collections)
CREATE TABLE rule_sets (
    id VARCHAR(50) PRIMARY KEY,
    version VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    is_active BOOLEAN DEFAULT false,
    activated_at TIMESTAMP,
    environment VARCHAR(20) DEFAULT 'production', -- production, staging, test

    -- Default output when no rules match
    default_document_id VARCHAR(100),
    default_disclosure_code VARCHAR(50),
    default_document_version VARCHAR(20),
    default_metadata JSONB,

    -- Metadata
    tags JSONB, -- For categorization
    change_log TEXT,

    UNIQUE(version, environment)
);

-- Individual Rules
CREATE TABLE rules (
    id VARCHAR(50) PRIMARY KEY,
    rule_set_id VARCHAR(50) NOT NULL REFERENCES rule_sets(id) ON DELETE CASCADE,
    priority INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN DEFAULT true,

    -- Temporal validity
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,

    -- Conditions (stored as JSON)
    conditions JSONB NOT NULL,
    -- Example structure:
    -- {
    --   "all": [
    --     {"field": "accountType", "operator": "equals", "value": "CREDIT_CARD"}
    --   ],
    --   "any": []
    -- }

    -- Output
    output_document_id VARCHAR(100) NOT NULL,
    output_disclosure_code VARCHAR(50) NOT NULL,
    output_document_version VARCHAR(20) NOT NULL,
    output_metadata JSONB,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Performance tracking
    times_evaluated INTEGER DEFAULT 0,
    times_matched INTEGER DEFAULT 0,
    avg_evaluation_time_ms DECIMAL(10,2),

    UNIQUE(rule_set_id, priority)
);

-- Rule Execution History (for auditing and analytics)
CREATE TABLE rule_execution_logs (
    id BIGSERIAL PRIMARY KEY,
    rule_set_id VARCHAR(50) NOT NULL,
    rule_id VARCHAR(50),
    customer_id VARCHAR(100),
    account_id VARCHAR(100),

    -- Input context
    input_context JSONB NOT NULL,

    -- Execution result
    matched BOOLEAN NOT NULL,
    output JSONB,

    -- Performance metrics
    execution_time_ms INTEGER,
    rules_evaluated INTEGER,
    cache_hit BOOLEAN,

    -- Audit
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trace_id VARCHAR(100),

    -- Indexes for common queries
    INDEX idx_customer_id (customer_id),
    INDEX idx_account_id (account_id),
    INDEX idx_rule_id (rule_id),
    INDEX idx_executed_at (executed_at),
    INDEX idx_trace_id (trace_id)
);

-- Documents catalog
CREATE TABLE documents (
    document_id VARCHAR(100) PRIMARY KEY,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50), -- agreement, disclosure, notice
    current_version VARCHAR(20),
    file_path VARCHAR(500),

    -- Categorization
    product_type VARCHAR(50), -- CREDIT_CARD, CHECKING, SAVINGS
    jurisdiction VARCHAR(50),  -- CA, NY, Federal

    -- Status
    status VARCHAR(20) DEFAULT 'active', -- active, deprecated, archived
    effective_date DATE,
    expiration_date DATE,

    -- Metadata
    description TEXT,
    tags JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Document versions
CREATE TABLE document_versions (
    id SERIAL PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL REFERENCES documents(document_id),
    version VARCHAR(20) NOT NULL,
    disclosure_code VARCHAR(50),

    file_path VARCHAR(500) NOT NULL,
    file_hash VARCHAR(64), -- SHA-256 hash for integrity

    -- Change tracking
    change_description TEXT,
    changed_sections JSONB,

    -- Status
    status VARCHAR(20) DEFAULT 'draft', -- draft, review, approved, published
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    published_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    UNIQUE(document_id, version)
);

-- API Data Source Configuration
CREATE TABLE api_data_sources (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Endpoint configuration
    base_url VARCHAR(500) NOT NULL,
    endpoint_path VARCHAR(500),
    http_method VARCHAR(10) DEFAULT 'GET',
    headers JSONB,
    query_params JSONB,

    -- Authentication
    auth_type VARCHAR(50), -- bearer, api_key, oauth2
    auth_config JSONB,

    -- Caching
    cache_enabled BOOLEAN DEFAULT true,
    cache_ttl_seconds INTEGER DEFAULT 3600,
    cache_key_pattern VARCHAR(255),

    -- Retry policy
    retry_enabled BOOLEAN DEFAULT true,
    max_retry_attempts INTEGER DEFAULT 3,
    retry_backoff_strategy VARCHAR(20) DEFAULT 'exponential',

    -- Performance
    timeout_ms INTEGER DEFAULT 5000,

    -- Response mapping
    response_mapping JSONB,
    -- Example:
    -- {
    --   "extract": {
    --     "accountType": "$.data.account.type",
    --     "state": "$.data.address.state"
    --   }
    -- }

    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Views for Easy Querying
-- ============================================================================

-- Active rules with their complete configuration
CREATE VIEW v_active_rules AS
SELECT
    rs.id as rule_set_id,
    rs.version as rule_set_version,
    rs.name as rule_set_name,
    r.id as rule_id,
    r.priority,
    r.name as rule_name,
    r.conditions,
    r.output_document_id,
    r.output_disclosure_code,
    r.output_document_version,
    r.output_metadata,
    r.valid_from,
    r.valid_until,
    r.enabled,
    d.document_name,
    d.document_type,
    d.file_path
FROM rule_sets rs
JOIN rules r ON rs.id = r.rule_set_id
LEFT JOIN documents d ON r.output_document_id = d.document_id
WHERE rs.is_active = true
  AND r.enabled = true
  AND (r.valid_from IS NULL OR r.valid_from <= CURRENT_TIMESTAMP)
  AND (r.valid_until IS NULL OR r.valid_until > CURRENT_TIMESTAMP)
ORDER BY r.priority;

-- Rule performance metrics
CREATE VIEW v_rule_performance AS
SELECT
    r.id,
    r.name,
    r.times_evaluated,
    r.times_matched,
    CASE
        WHEN r.times_evaluated > 0
        THEN ROUND((r.times_matched::DECIMAL / r.times_evaluated) * 100, 2)
        ELSE 0
    END as match_rate_percent,
    r.avg_evaluation_time_ms,
    rs.version as rule_set_version
FROM rules r
JOIN rule_sets rs ON r.rule_set_id = rs.id
WHERE rs.is_active = true
ORDER BY r.times_evaluated DESC;

-- Document usage statistics
CREATE VIEW v_document_usage AS
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    COUNT(DISTINCT rel.rule_id) as rules_using,
    COUNT(rel.id) as times_selected,
    MAX(rel.executed_at) as last_used
FROM documents d
LEFT JOIN rule_execution_logs rel ON
    rel.matched = true AND
    rel.output->>'documentId' = d.document_id
GROUP BY d.document_id, d.document_name, d.document_type;

-- ============================================================================
-- Functions
-- ============================================================================

-- Function to get active rule set
CREATE OR REPLACE FUNCTION get_active_rule_set(env VARCHAR DEFAULT 'production')
RETURNS TABLE (
    rule_set_id VARCHAR,
    version VARCHAR,
    rules JSONB,
    default_output JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        rs.id,
        rs.version,
        jsonb_agg(
            jsonb_build_object(
                'id', r.id,
                'priority', r.priority,
                'name', r.name,
                'conditions', r.conditions,
                'output', jsonb_build_object(
                    'documentId', r.output_document_id,
                    'disclosureCode', r.output_disclosure_code,
                    'documentVersion', r.output_document_version,
                    'metadata', r.output_metadata
                ),
                'enabled', r.enabled,
                'validFrom', r.valid_from,
                'validUntil', r.valid_until
            ) ORDER BY r.priority
        ) as rules,
        jsonb_build_object(
            'documentId', rs.default_document_id,
            'disclosureCode', rs.default_disclosure_code,
            'documentVersion', rs.default_document_version,
            'metadata', rs.default_metadata
        ) as default_output
    FROM rule_sets rs
    JOIN rules r ON rs.id = r.rule_set_id
    WHERE rs.is_active = true
      AND rs.environment = env
      AND r.enabled = true
    GROUP BY rs.id, rs.version, rs.default_document_id,
             rs.default_disclosure_code, rs.default_document_version,
             rs.default_metadata;
END;
$$ LANGUAGE plpgsql;

-- Function to log rule execution
CREATE OR REPLACE FUNCTION log_rule_execution(
    p_rule_set_id VARCHAR,
    p_rule_id VARCHAR,
    p_customer_id VARCHAR,
    p_account_id VARCHAR,
    p_input_context JSONB,
    p_matched BOOLEAN,
    p_output JSONB,
    p_execution_time_ms INTEGER,
    p_rules_evaluated INTEGER,
    p_cache_hit BOOLEAN,
    p_trace_id VARCHAR
) RETURNS VOID AS $$
BEGIN
    -- Insert execution log
    INSERT INTO rule_execution_logs (
        rule_set_id, rule_id, customer_id, account_id,
        input_context, matched, output,
        execution_time_ms, rules_evaluated, cache_hit, trace_id
    ) VALUES (
        p_rule_set_id, p_rule_id, p_customer_id, p_account_id,
        p_input_context, p_matched, p_output,
        p_execution_time_ms, p_rules_evaluated, p_cache_hit, p_trace_id
    );

    -- Update rule statistics if matched
    IF p_matched AND p_rule_id IS NOT NULL THEN
        UPDATE rules
        SET times_evaluated = times_evaluated + p_rules_evaluated,
            times_matched = times_matched + 1,
            avg_evaluation_time_ms =
                CASE
                    WHEN times_evaluated = 0 THEN p_execution_time_ms
                    ELSE ((avg_evaluation_time_ms * times_evaluated) + p_execution_time_ms) / (times_evaluated + 1)
                END
        WHERE id = p_rule_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Sample Data
-- ============================================================================

-- Insert sample rule set
INSERT INTO rule_sets (id, version, name, environment, is_active,
                       default_document_id, default_disclosure_code, default_document_version)
VALUES ('rs-doc-sel-v2', '2.0.0', 'Document Selection Rules V2', 'production', true,
        'STANDARD_AGREEMENT_v1.0', 'DISC_DEFAULT', '1.0');

-- Insert sample documents
INSERT INTO documents (document_id, document_name, document_type, current_version,
                       product_type, status, effective_date)
VALUES
    ('CC_AGREEMENT_CA_v2.1', 'Credit Card Agreement - California', 'agreement', '2.1',
     'CREDIT_CARD', 'active', '2025-01-01'),
    ('CC_AGREEMENT_STANDARD_v2.0', 'Credit Card Agreement - Standard', 'agreement', '2.0',
     'CREDIT_CARD', 'active', '2025-01-01'),
    ('CHECKING_PREMIUM_AGREEMENT_v1.5', 'Premium Checking Agreement', 'agreement', '1.5',
     'CHECKING', 'active', '2024-06-01');

-- Insert sample rules
INSERT INTO rules (id, rule_set_id, priority, name, conditions,
                   output_document_id, output_disclosure_code, output_document_version)
VALUES
    ('rule-cc-ca', 'rs-doc-sel-v2', 1, 'Credit Card - California',
     '{"all": [{"field": "accountType", "operator": "equals", "value": "CREDIT_CARD"},
               {"field": "state", "operator": "equals", "value": "CA"}]}',
     'CC_AGREEMENT_CA_v2.1', 'DISC_CC_CA_001', '2.1'),

    ('rule-cc-std', 'rs-doc-sel-v2', 2, 'Credit Card - Standard',
     '{"all": [{"field": "accountType", "operator": "equals", "value": "CREDIT_CARD"}]}',
     'CC_AGREEMENT_STANDARD_v2.0', 'DISC_CC_STD_001', '2.0'),

    ('rule-chk-prem', 'rs-doc-sel-v2', 3, 'Checking - Premium',
     '{"all": [{"field": "accountType", "operator": "equals", "value": "CHECKING"},
               {"field": "balance", "operator": "greaterThan", "value": 10000}]}',
     'CHECKING_PREMIUM_AGREEMENT_v1.5', 'DISC_CHK_PREM_001', '1.5');

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

CREATE INDEX idx_rules_rule_set_priority ON rules(rule_set_id, priority);
CREATE INDEX idx_rules_enabled ON rules(enabled) WHERE enabled = true;
CREATE INDEX idx_rule_sets_active ON rule_sets(is_active, environment) WHERE is_active = true;
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_rule_execution_logs_executed_at ON rule_execution_logs(executed_at DESC);

-- GIN index for JSONB queries
CREATE INDEX idx_rules_conditions ON rules USING GIN (conditions);
CREATE INDEX idx_rule_execution_logs_context ON rule_execution_logs USING GIN (input_context);
CREATE INDEX idx_rule_execution_logs_output ON rule_execution_logs USING GIN (output);

-- ============================================================================
-- Usage Examples
-- ============================================================================

-- Get active rule set for production
SELECT * FROM get_active_rule_set('production');

-- Log a rule execution
SELECT log_rule_execution(
    'rs-doc-sel-v2',
    'rule-cc-ca',
    'CUST-12345',
    'ACC-67890',
    '{"accountType": "CREDIT_CARD", "state": "CA", "balance": 5000}'::jsonb,
    true,
    '{"documentId": "CC_AGREEMENT_CA_v2.1", "disclosureCode": "DISC_CC_CA_001"}'::jsonb,
    15,
    1,
    false,
    'trace-abc-123'
);

-- Query rule performance
SELECT * FROM v_rule_performance;

-- Query document usage
SELECT * FROM v_document_usage ORDER BY times_selected DESC;

-- Find all rules for a specific document
SELECT r.*
FROM rules r
WHERE r.output_document_id = 'CC_AGREEMENT_CA_v2.1'
  AND r.enabled = true;

-- Audit: Who changed what and when
SELECT
    r.id,
    r.name,
    r.updated_at,
    r.updated_by,
    r.enabled
FROM rules r
WHERE r.updated_at > CURRENT_TIMESTAMP - INTERVAL '7 days'
ORDER BY r.updated_at DESC;
