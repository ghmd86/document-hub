-- ============================================
-- Drools Reactive Eligibility System
-- Database Schema
-- ============================================

-- Drop existing tables (if any)
DROP TABLE IF EXISTS eligibility_rules CASCADE;
DROP TABLE IF EXISTS data_sources CASCADE;
DROP TABLE IF EXISTS rule_change_history CASCADE;

-- ============================================
-- Table: data_sources
-- Purpose: Store configuration for external APIs
-- ============================================
CREATE TABLE data_sources (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Add comment
COMMENT ON TABLE data_sources IS 'External API data sources with JSONB configuration';
COMMENT ON COLUMN data_sources.id IS 'Unique identifier (e.g., "arrangements_api")';
COMMENT ON COLUMN data_sources.type IS 'Data source type (e.g., "REST_API")';
COMMENT ON COLUMN data_sources.configuration IS 'JSONB configuration: URL, endpoint, mapping, dependencies';

-- Create index on enabled column
CREATE INDEX idx_data_sources_enabled ON data_sources(enabled);

-- ============================================
-- Table: eligibility_rules
-- Purpose: Store business rules for document eligibility
-- ============================================
CREATE TABLE eligibility_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) UNIQUE NOT NULL,
    document_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT true,
    conditions JSONB NOT NULL,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Add comments
COMMENT ON TABLE eligibility_rules IS 'Document eligibility rules with JSONB conditions';
COMMENT ON COLUMN eligibility_rules.rule_id IS 'Unique rule identifier (e.g., "RULE-001")';
COMMENT ON COLUMN eligibility_rules.document_id IS 'Document to add if rule matches';
COMMENT ON COLUMN eligibility_rules.priority IS 'Evaluation priority (higher = evaluated first)';
COMMENT ON COLUMN eligibility_rules.conditions IS 'JSONB conditions: field comparisons, operators, values';

-- Create indexes
CREATE INDEX idx_eligibility_rules_enabled ON eligibility_rules(enabled);
CREATE INDEX idx_eligibility_rules_priority ON eligibility_rules(priority DESC);
CREATE INDEX idx_eligibility_rules_document_id ON eligibility_rules(document_id);

-- ============================================
-- Table: rule_change_history
-- Purpose: Audit trail for rule changes
-- ============================================
CREATE TABLE rule_change_history (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);

-- Add comments
COMMENT ON TABLE rule_change_history IS 'Audit trail for rule changes';
COMMENT ON COLUMN rule_change_history.change_type IS 'Type of change: CREATE, UPDATE, DELETE, ENABLE, DISABLE';

-- Create index
CREATE INDEX idx_rule_change_history_rule_id ON rule_change_history(rule_id);
CREATE INDEX idx_rule_change_history_changed_at ON rule_change_history(changed_at DESC);

-- ============================================
-- Trigger: Update updated_at timestamp
-- ============================================

-- Function to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for data_sources
CREATE TRIGGER update_data_sources_updated_at
    BEFORE UPDATE ON data_sources
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for eligibility_rules
CREATE TRIGGER update_eligibility_rules_updated_at
    BEFORE UPDATE ON eligibility_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Verification Queries
-- ============================================

-- Check tables
SELECT
    tablename,
    schemaname
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN ('data_sources', 'eligibility_rules', 'rule_change_history');

-- Show table structure
\d data_sources
\d eligibility_rules
\d rule_change_history
