-- Insert Sample Data for Document Hub Service
-- This script provides realistic test data for development and testing

-- ============================================================================
-- Sample Master Template Definitions
-- ============================================================================

-- 1. Shared Document: Privacy Policy (available to all)
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    valid_until, is_shared_document, sharing_scope, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440001',
    1,
    'Privacy Policy 2024',
    'Annual Privacy Policy Document - Available to all customers',
    'credit_card',
    'PrivacyPolicy',
    'PrivacyNotice',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '30 days'))::BIGINT,
    EXTRACT(EPOCH FROM (NOW() + INTERVAL '365 days'))::BIGINT,
    TRUE,
    'all',
    'system'
);

-- 2. Shared Document: Cardholder Agreement (credit card only)
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, sharing_scope, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440002',
    1,
    'Cardholder Agreement',
    'Credit Card Cardholder Agreement - Credit Card Accounts Only',
    'credit_card',
    'CardholderAgreement',
    'CardholderAgreement',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '60 days'))::BIGINT,
    TRUE,
    'credit_card_account_only',
    'system'
);

-- 3. Shared Document: Digital Banking Guide (digital banking only)
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, sharing_scope, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440003',
    1,
    'Digital Banking User Guide',
    'Digital Banking User Guide - Digital Banking Customers Only',
    'digital_banking',
    'GeneralCommunications',
    'UserGuide',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '90 days'))::BIGINT,
    TRUE,
    'digital_bank_customer_only',
    'system'
);

-- 4. Shared Document with Custom Rule: Low Balance Alert (balance < $5000)
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, sharing_scope, data_extraction_schema, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440004',
    1,
    'Low Balance Alert Notice',
    'Special offer for customers with balance less than $5000',
    'credit_card',
    'GeneralCommunications',
    'BalanceAlert',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    'custom_rule',
    '{
      "ruleType": "balance_based",
      "description": "Show to customers with balance less than $5000",
      "extractionStrategy": [{
        "id": "getBalance",
        "endpoint": {
          "url": "/accounts-service/accounts/${$input.accountId}/balance",
          "method": "GET",
          "timeout": 3000
        },
        "responseMapping": {
          "extract": {"currentBalance": "$.currentBalance"}
        }
      }],
      "eligibilityCriteria": {
        "currentBalance": {
          "operator": "lessThan",
          "value": 5000,
          "dataType": "number"
        }
      },
      "errorHandling": {
        "onExtractionFailure": "exclude",
        "onTimeout": "exclude"
      }
    }',
    'system'
);

-- 5. Shared Document with Custom Rule: Loyal Customer Offer (tenure > 5 years)
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, sharing_scope, data_extraction_schema, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440005',
    1,
    'Loyal Customer Rewards',
    'Special rewards for customers with more than 5 years tenure',
    'credit_card',
    'GeneralCommunications',
    'RewardsOffer',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    'custom_rule',
    '{
      "ruleType": "tenure_based",
      "description": "Show to customers with tenure > 5 years",
      "extractionStrategy": [{
        "id": "getTenure",
        "endpoint": {
          "url": "/customer-service/customers/${$input.customerId}/profile",
          "method": "GET",
          "timeout": 3000
        },
        "responseMapping": {
          "extract": {"customerSinceDate": "$.customerSince"}
        }
      }],
      "eligibilityCriteria": {
        "tenureYears": {
          "operator": "greaterThan",
          "value": 5,
          "dataType": "duration",
          "unit": "years",
          "compareField": "customerSinceDate"
        }
      }
    }',
    'system'
);

-- 6. Regular Template: Monthly Statement
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440006',
    1,
    'Monthly Statement Template',
    'Standard monthly credit card statement',
    'credit_card',
    'Statement',
    'monthly_statement',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '180 days'))::BIGINT,
    FALSE,
    'system'
);

-- 7. Regular Template: Payment Confirmation
INSERT INTO master_template_definition (
    template_id, version, template_name, description, line_of_business,
    category, doc_type, language_code, template_status, effective_date,
    is_shared_document, created_by
) VALUES (
    '550e8400-e29b-41d4-a716-446655440007',
    1,
    'Payment Confirmation Letter',
    'Payment confirmation notice',
    'credit_card',
    'PaymentConfirmationNotice',
    'PaymentLetter',
    'EN_US',
    'Approved',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '90 days'))::BIGINT,
    FALSE,
    'system'
);

-- ============================================================================
-- Sample Storage Index (Account-Specific Documents)
-- ============================================================================

-- Sample Customer IDs and Account IDs for testing
-- Customer 1: 880e8400-e29b-41d4-a716-446655440001
-- Account 1: 770e8400-e29b-41d4-a716-446655440001
-- Account 2: 770e8400-e29b-41d4-a716-446655440002

-- Customer 2: 880e8400-e29b-41d4-a716-446655440002
-- Account 3: 770e8400-e29b-41d4-a716-446655440003

-- Document 1: Monthly Statement for Account 1 (January 2024)
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440001',
    '550e8400-e29b-41d4-a716-446655440006',
    'monthly_statement',
    '770e8400-e29b-41d4-a716-446655440001',
    '880e8400-e29b-41d4-a716-446655440001',
    '990e8400-e29b-41d4-a716-446655440001',
    'statement_2024_01.pdf',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '60 days'))::BIGINT,
    TRUE,
    '{"cycle_date": "2024-01-31", "statement_id": "STMT-2024-01-001", "balance": "4500.00"}',
    'system'
);

-- Document 2: Monthly Statement for Account 1 (February 2024)
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440002',
    '550e8400-e29b-41d4-a716-446655440006',
    'monthly_statement',
    '770e8400-e29b-41d4-a716-446655440001',
    '880e8400-e29b-41d4-a716-446655440001',
    '990e8400-e29b-41d4-a716-446655440002',
    'statement_2024_02.pdf',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '30 days'))::BIGINT,
    TRUE,
    '{"cycle_date": "2024-02-29", "statement_id": "STMT-2024-02-001", "balance": "4750.25"}',
    'system'
);

-- Document 3: Monthly Statement for Account 1 (March 2024)
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440003',
    '550e8400-e29b-41d4-a716-446655440006',
    'monthly_statement',
    '770e8400-e29b-41d4-a716-446655440001',
    '880e8400-e29b-41d4-a716-446655440001',
    '990e8400-e29b-41d4-a716-446655440003',
    'statement_2024_03.pdf',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    '{"cycle_date": "2024-03-31", "statement_id": "STMT-2024-03-001", "balance": "4925.50"}',
    'system'
);

-- Document 4: Payment Confirmation for Account 1
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440004',
    '550e8400-e29b-41d4-a716-446655440007',
    'PaymentLetter',
    '770e8400-e29b-41d4-a716-446655440001',
    '880e8400-e29b-41d4-a716-446655440001',
    '990e8400-e29b-41d4-a716-446655440004',
    'payment_confirmation_2024_02_15.pdf',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '15 days'))::BIGINT,
    TRUE,
    '{"payment_date": "2024-02-15", "amount": "500.00", "confirmation_id": "PAY-2024-02-001"}',
    'system'
);

-- Document 5: Monthly Statement for Account 2 (Customer 1, different account)
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440005',
    '550e8400-e29b-41d4-a716-446655440006',
    'monthly_statement',
    '770e8400-e29b-41d4-a716-446655440002',
    '880e8400-e29b-41d4-a716-446655440001',
    '990e8400-e29b-41d4-a716-446655440005',
    'statement_2024_03_acc2.pdf',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    '{"cycle_date": "2024-03-31", "statement_id": "STMT-2024-03-002", "balance": "12500.00"}',
    'system'
);

-- Document 6: Monthly Statement for Account 3 (Customer 2)
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440006',
    '550e8400-e29b-41d4-a716-446655440006',
    'monthly_statement',
    '770e8400-e29b-41d4-a716-446655440003',
    '880e8400-e29b-41d4-a716-446655440002',
    '990e8400-e29b-41d4-a716-446655440006',
    'statement_2024_03_cust2.pdf',
    EXTRACT(EPOCH FROM NOW())::BIGINT,
    TRUE,
    '{"cycle_date": "2024-03-31", "statement_id": "STMT-2024-03-003", "balance": "8200.75"}',
    'system'
);

-- Document 7: Payment Confirmation for Account 3
INSERT INTO storage_index (
    storage_index_id, template_id, doc_type, account_key, customer_key,
    storage_document_key, file_name, doc_creation_date, is_accessible,
    doc_info, created_by
) VALUES (
    '660e8400-e29b-41d4-a716-446655440007',
    '550e8400-e29b-41d4-a716-446655440007',
    'PaymentLetter',
    '770e8400-e29b-41d4-a716-446655440003',
    '880e8400-e29b-41d4-a716-446655440002',
    '990e8400-e29b-41d4-a716-446655440007',
    'payment_confirmation_2024_03_10.pdf',
    EXTRACT(EPOCH FROM (NOW() - INTERVAL '5 days'))::BIGINT,
    TRUE,
    '{"payment_date": "2024-03-10", "amount": "1000.00", "confirmation_id": "PAY-2024-03-002"}',
    'system'
);

-- ============================================================================
-- Summary of Sample Data
-- ============================================================================

-- Templates Created:
--   - 5 Shared document templates (various scopes and rules)
--   - 2 Regular templates (monthly statement, payment confirmation)
--
-- Documents Created:
--   - 7 account-specific documents across 3 accounts and 2 customers
--
-- Test Scenarios Supported:
--   1. Customer with single account (Customer 1, Account 1): 4 documents
--   2. Customer with multiple accounts (Customer 1, Accounts 1+2): 5 documents total
--   3. Different customer (Customer 2, Account 3): 2 documents
--   4. Shared documents (5 templates with different eligibility rules)
--   5. Date range filtering (documents from last 60 days to present)
--   6. Document type filtering (statements vs payment confirmations)

COMMENT ON TABLE master_template_definition IS 'Sample data includes 7 templates: 5 shared documents with various eligibility rules and 2 regular templates';
COMMENT ON TABLE storage_index IS 'Sample data includes 7 documents across 2 customers and 3 accounts for comprehensive testing';
