-- Database Schema for Configuration-Driven Rule Engine
-- Uses JSONB columns for flexible configuration storage

-- ============================================================================
-- Data Sources Table (External API Configurations)
-- ============================================================================

CREATE TABLE IF NOT EXISTS data_sources (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- REST_API, DATABASE, etc.
    configuration JSONB NOT NULL,  -- ⭐ Complete API configuration as JSON
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_data_sources_enabled ON data_sources(enabled);

COMMENT ON TABLE data_sources IS 'External API data source configurations';
COMMENT ON COLUMN data_sources.configuration IS 'JSONB: {method, baseUrl, endpoint, timeoutMs, retryCount, dependsOn, responseMapping}';

-- ============================================================================
-- Document Eligibility Rules Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS document_eligibility_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) UNIQUE NOT NULL,
    document_id VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    description TEXT,
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT true,
    conditions JSONB NOT NULL,  -- ⭐ Rule conditions as JSON
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_rules_enabled_priority ON document_eligibility_rules(enabled, priority DESC);
CREATE INDEX idx_rules_document_id ON document_eligibility_rules(document_id);
CREATE INDEX idx_rules_rule_id ON document_eligibility_rules(rule_id);

-- GIN index for JSONB queries (if you need to query inside JSONB)
CREATE INDEX idx_rules_conditions_gin ON document_eligibility_rules USING GIN (conditions);

COMMENT ON TABLE document_eligibility_rules IS 'Document eligibility rules with conditions stored as JSONB';
COMMENT ON COLUMN document_eligibility_rules.conditions IS 'JSONB: {type: ALL/ANY, expressions: [...]}';

-- ============================================================================
-- Rule Change History (Audit Trail)
-- ============================================================================

CREATE TABLE IF NOT EXISTS rule_change_history (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    change_type VARCHAR(50),  -- CREATE, UPDATE, DELETE, ENABLE, DISABLE
    old_value JSONB,
    new_value JSONB,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);

CREATE INDEX idx_rule_history_rule_id ON rule_change_history(rule_id);
CREATE INDEX idx_rule_history_changed_at ON rule_change_history(changed_at DESC);

COMMENT ON TABLE rule_change_history IS 'Audit trail for rule changes';

-- ============================================================================
-- Sample Data: External API Configurations
-- ============================================================================

-- Arrangements API (Step 1: Get pricingId)
INSERT INTO data_sources (id, name, type, configuration, enabled, created_by) VALUES (
    'arrangements_api',
    'Arrangements API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8081",
        "endpoint": "/api/v1/arrangements/{arrangementId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "responseMapping": [
            {
                "fieldName": "pricingId",
                "jsonPath": "$.pricingId",
                "dataType": "STRING"
            },
            {
                "fieldName": "productCode",
                "jsonPath": "$.productCode",
                "dataType": "STRING"
            },
            {
                "fieldName": "arrangementStatus",
                "jsonPath": "$.status",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- Cardholder Agreements API (Step 2: Use pricingId from arrangements_api - CHAINED)
INSERT INTO data_sources (id, name, type, configuration, enabled, created_by) VALUES (
    'cardholder_agreements_api',
    'Cardholder Agreements API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8082",
        "endpoint": "/api/v1/cardholder-agreements/{pricingId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "dependsOn": [
            {
                "sourceId": "arrangements_api",
                "field": "pricingId"
            }
        ],
        "responseMapping": [
            {
                "fieldName": "cardholderAgreementsTNCCode",
                "jsonPath": "$.cardholderAgreementsTNCCode",
                "dataType": "STRING"
            },
            {
                "fieldName": "tncEffectiveDate",
                "jsonPath": "$.effectiveDate",
                "dataType": "DATE"
            },
            {
                "fieldName": "tncExpiryDate",
                "jsonPath": "$.expiryDate",
                "dataType": "DATE"
            }
        ]
    }'::jsonb,
    true,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- Account Service API (Parallel)
INSERT INTO data_sources (id, name, type, configuration, enabled, created_by) VALUES (
    'account_service_api',
    'Account Service API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8083",
        "endpoint": "/api/v1/accounts/{accountId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "responseMapping": [
            {
                "fieldName": "accountBalance",
                "jsonPath": "$.balance",
                "dataType": "DECIMAL"
            },
            {
                "fieldName": "accountStatus",
                "jsonPath": "$.status",
                "dataType": "STRING"
            },
            {
                "fieldName": "accountType",
                "jsonPath": "$.accountType",
                "dataType": "STRING"
            },
            {
                "fieldName": "creditLimit",
                "jsonPath": "$.creditLimit",
                "dataType": "DECIMAL"
            },
            {
                "fieldName": "state",
                "jsonPath": "$.state",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- Customer Service API (Parallel)
INSERT INTO data_sources (id, name, type, configuration, enabled, created_by) VALUES (
    'customer_service_api',
    'Customer Service API',
    'REST_API',
    '{
        "method": "GET",
        "baseUrl": "http://localhost:8084",
        "endpoint": "/api/v1/customers/{customerId}",
        "timeoutMs": 5000,
        "retryCount": 2,
        "responseMapping": [
            {
                "fieldName": "customerTier",
                "jsonPath": "$.tier",
                "dataType": "STRING"
            },
            {
                "fieldName": "creditScore",
                "jsonPath": "$.creditScore",
                "dataType": "INTEGER"
            },
            {
                "fieldName": "customerStatus",
                "jsonPath": "$.status",
                "dataType": "STRING"
            },
            {
                "fieldName": "enrollmentDate",
                "jsonPath": "$.enrollmentDate",
                "dataType": "DATE"
            }
        ]
    }'::jsonb,
    true,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Sample Data: Document Eligibility Rules
-- ============================================================================

-- Rule 1: Gold TNC Specific Document
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-001',
    'DOC-TNC-GOLD-2024-BENEFITS',
    'Gold TNC Specific Document',
    'Customers with Gold TNC are eligible for gold benefits document',
    100,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "cardholder_agreements_api",
                "field": "cardholderAgreementsTNCCode",
                "operator": "EQUALS",
                "value": "TNC_GOLD_2024"
            }
        ]
    }'::jsonb,
    'system'
) ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2: Platinum TNC Specific Document
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-002',
    'DOC-TNC-PLATINUM-2024-BENEFITS',
    'Platinum TNC Specific Document',
    'Customers with Platinum TNC are eligible for platinum benefits',
    95,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "cardholder_agreements_api",
                "field": "cardholderAgreementsTNCCode",
                "operator": "EQUALS",
                "value": "TNC_PLATINUM_2024"
            }
        ]
    }'::jsonb,
    'system'
) ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3: Premium Pricing Package
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-003',
    'DOC-PREMIUM-PRICING-BENEFITS',
    'Premium Pricing Package',
    'Accounts with premium pricing get premium benefits documentation',
    90,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "arrangements_api",
                "field": "pricingId",
                "operator": "MATCHES",
                "value": "PRICING_PREMIUM_.*"
            }
        ]
    }'::jsonb,
    'system'
) ON CONFLICT (rule_id) DO NOTHING;

-- Rule 4: High Balance Gold TNC Exclusive (Multiple conditions from different APIs)
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-004',
    'DOC-HIGH-BALANCE-GOLD-EXCLUSIVE',
    'High Balance Gold TNC Exclusive',
    'High balance customers with Gold TNC get exclusive document',
    85,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "cardholder_agreements_api",
                "field": "cardholderAgreementsTNCCode",
                "operator": "EQUALS",
                "value": "TNC_GOLD_2024"
            },
            {
                "source": "account_service_api",
                "field": "accountBalance",
                "operator": "GREATER_THAN",
                "value": 50000
            }
        ]
    }'::jsonb,
    'system'
) ON CONFLICT (rule_id) DO NOTHING;

-- Rule 5: VIP Customer with Platinum TNC (Multi-source)
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-005',
    'DOC-VIP-PLATINUM-TNC',
    'VIP Customer with Platinum TNC',
    'VIP tier customers with Platinum TNC get VIP documentation',
    80,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "cardholder_agreements_api",
                "field": "cardholderAgreementsTNCCode",
                "operator": "EQUALS",
                "value": "TNC_PLATINUM_2024"
            },
            {
                "source": "customer_service_api",
                "field": "customerTier",
                "operator": "IN",
                "value": ["PLATINUM", "BLACK"]
            }
        ]
    }'::jsonb,
    'system'
) ON CONFLICT (rule_id) DO NOTHING;

-- Rule 6: Complex Nested Logic (Demonstrates AND + OR)
INSERT INTO document_eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-010',
    'DOC-COMPLEX-ELIGIBILITY',
    'Complex Nested Eligibility',
    'Demonstrates nested AND/OR logic',
    30,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "type": "ANY",
                "expressions": [
                    {
                        "source": "cardholder_agreements_api",
                        "field": "cardholderAgreementsTNCCode",
                        "operator": "EQUALS",
                        "value": "TNC_GOLD_2024"
                    },
                    {
                        "source": "cardholder_agreements_api",
                        "field": "cardholderAgreementsTNCCode",
                        "operator": "EQUALS",
                        "value": "TNC_PLATINUM_2024"
                    }
                ]
            },
            {
                "type": "ANY",
                "expressions": [
                    {
                        "source": "account_service_api",
                        "field": "accountBalance",
                        "operator": "GREATER_THAN",
                        "value": 75000
                    },
                    {
                        "source": "customer_service_api",
                        "field": "creditScore",
                        "operator": "GREATER_THAN_OR_EQUAL",
                        "value": 750
                    }
                ]
            }
        ]
    }'::jsonb,
    'system'
) ON CONFLICT (rule_id) DO NOTHING;

-- ============================================================================
-- Useful Queries
-- ============================================================================

-- View all enabled data sources
-- SELECT id, name, type, enabled FROM data_sources WHERE enabled = true;

-- View all enabled rules
-- SELECT rule_id, document_id, name, priority, enabled FROM document_eligibility_rules WHERE enabled = true ORDER BY priority DESC;

-- View specific data source configuration
-- SELECT id, name, jsonb_pretty(configuration) FROM data_sources WHERE id = 'arrangements_api';

-- View specific rule conditions
-- SELECT rule_id, name, jsonb_pretty(conditions) FROM document_eligibility_rules WHERE rule_id = 'RULE-004';

-- Find rules using a specific data source
-- SELECT rule_id, name, conditions
-- FROM document_eligibility_rules
-- WHERE conditions::text LIKE '%cardholder_agreements_api%';

-- Count rules by priority range
-- SELECT
--     CASE
--         WHEN priority >= 90 THEN 'High (90+)'
--         WHEN priority >= 50 THEN 'Medium (50-89)'
--         ELSE 'Low (<50)'
--     END as priority_range,
--     COUNT(*) as rule_count
-- FROM document_eligibility_rules
-- WHERE enabled = true
-- GROUP BY priority_range
-- ORDER BY MIN(priority) DESC;
