-- ============================================
-- Sample Data for Drools Reactive Eligibility System
-- ============================================

-- ============================================
-- 1. DATA SOURCES (External APIs)
-- ============================================

-- Clear existing data
TRUNCATE TABLE data_sources CASCADE;
TRUNCATE TABLE eligibility_rules CASCADE;

-- Data Source 1: Arrangements API
-- Fetches arrangement details (pricingId, productCode)
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
            }
        ]
    }'::jsonb,
    true,
    'system'
);

-- Data Source 2: Cardholder Agreements API (CHAINED)
-- Depends on arrangements_api.pricingId
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
                "fieldName": "agreementStatus",
                "jsonPath": "$.status",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true,
    'system'
);

-- Data Source 3: Account Service API
-- Fetches account balance and status
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
            }
        ]
    }'::jsonb,
    true,
    'system'
);

-- Data Source 4: Customer Service API
-- Fetches customer profile
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
                "fieldName": "customerStatus",
                "jsonPath": "$.status",
                "dataType": "STRING"
            }
        ]
    }'::jsonb,
    true,
    'system'
);

-- ============================================
-- 2. ELIGIBILITY RULES
-- ============================================

-- Rule 1: Gold TNC Benefits
-- Customers with Gold TNC code get gold benefits document
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-001',
    'DOC-TNC-GOLD-2024-BENEFITS',
    'Gold TNC Specific Document',
    'Customers with Gold TNC (TNC_GOLD_2024) are eligible for gold benefits document',
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
    'admin'
);

-- Rule 2: Platinum TNC Benefits
-- Customers with Platinum TNC code get platinum benefits document
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-002',
    'DOC-TNC-PLATINUM-2024-BENEFITS',
    'Platinum TNC Specific Document',
    'Customers with Platinum TNC (TNC_PLATINUM_2024) are eligible for platinum benefits document',
    100,
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
    'admin'
);

-- Rule 3: High Balance Benefits (ANY account type)
-- Customers with balance > 100,000 get high balance benefits
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-003',
    'DOC-HIGH-BALANCE-BENEFITS',
    'High Balance Benefits',
    'Customers with account balance over 100,000 get high balance benefits',
    90,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "account_service_api",
                "field": "accountBalance",
                "operator": "GREATER_THAN",
                "value": 100000
            }
        ]
    }'::jsonb,
    'admin'
);

-- Rule 4: High Balance Gold Exclusive (Multiple conditions)
-- Customers with Gold TNC AND balance > 50,000 get exclusive document
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-004',
    'DOC-HIGH-BALANCE-GOLD-EXCLUSIVE',
    'High Balance Gold TNC Exclusive',
    'Customers with Gold TNC and account balance over 50,000 get exclusive benefits',
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
    'admin'
);

-- Rule 5: Premium Customer Tier
-- Premium tier customers get premium document
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-005',
    'DOC-PREMIUM-CUSTOMER-BENEFITS',
    'Premium Customer Tier Benefits',
    'Customers with PREMIUM tier get premium benefits document',
    80,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "customer_service_api",
                "field": "customerTier",
                "operator": "EQUALS",
                "value": "PREMIUM"
            }
        ]
    }'::jsonb,
    'admin'
);

-- Rule 6: Active Account Status
-- Only active accounts get general benefits
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-006',
    'DOC-GENERAL-ACCOUNT-BENEFITS',
    'General Account Benefits',
    'All active accounts get general benefits document',
    50,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "account_service_api",
                "field": "accountStatus",
                "operator": "EQUALS",
                "value": "ACTIVE"
            }
        ]
    }'::jsonb,
    'admin'
);

-- Rule 7: Credit Card Product (Product-specific)
-- Credit card products get credit card benefits
INSERT INTO eligibility_rules (
    rule_id, document_id, name, description, priority, enabled, conditions, created_by
) VALUES (
    'RULE-007',
    'DOC-CREDIT-CARD-BENEFITS',
    'Credit Card Product Benefits',
    'Credit card product holders get credit card benefits document',
    70,
    true,
    '{
        "type": "ALL",
        "expressions": [
            {
                "source": "arrangements_api",
                "field": "productCode",
                "operator": "EQUALS",
                "value": "CREDIT_CARD"
            }
        ]
    }'::jsonb,
    'admin'
);

-- ============================================
-- 3. VERIFICATION QUERIES
-- ============================================

-- Show all data sources
SELECT id, name, enabled FROM data_sources ORDER BY id;

-- Show all rules
SELECT rule_id, name, priority, enabled FROM eligibility_rules ORDER BY priority DESC;

-- Show rule conditions (pretty print)
SELECT
    rule_id,
    name,
    jsonb_pretty(conditions) as conditions
FROM eligibility_rules
WHERE enabled = true
ORDER BY priority DESC;

-- Show data source configurations (pretty print)
SELECT
    id,
    name,
    jsonb_pretty(configuration) as configuration
FROM data_sources
WHERE enabled = true
ORDER BY id;
