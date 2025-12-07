-- Auto-generated SQL from CSV onboarding files
-- Generated: 2025-12-06T23:25:49.776233
-- ================================================

-- Master Template Definitions
-- Generated: 2025-12-06T23:25:49.776233

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '2d96e1db-8bf0-4c74-b13d-397d14f7e16d',
    'CREDIT_CARD_STATEMENT',
    1,
    'Statement',
    'Credit Card Statement',
    'Monthly credit card statement',
    'CREDIT_CARD',
    false,
    NULL,
    '{"fields": [{"name": "statement_date", "path": "$.header.statementDate", "type": "DATE", "required": true, "label": "Statement Date"}, {"name": "balance", "path": "$.summary.balance", "type": "NUMBER", "required": true, "label": "Current Balance"}, {"name": "due_date", "path": "$.summary.dueDate", "type": "DATE", "required": true, "label": "Payment Due Date"}, {"name": "minimum_payment", "path": "$.summary.minimumPayment", "type": "NUMBER", "required": true, "label": "Minimum Payment"}]}',
    '{}',
    true
);

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '5b4b4b72-601d-4f5e-b0c0-3f6076913d2f',
    'CREDIT_CARD_TERMS',
    1,
    'Legal',
    'Credit Card Terms',
    'Credit card terms and conditions',
    'CREDIT_CARD',
    false,
    NULL,
    '{"fields": [{"name": "effective_date", "path": "$.metadata.effectiveDate", "type": "DATE", "required": true, "label": "Effective Date"}, {"name": "apr", "path": "$.terms.apr", "type": "NUMBER", "required": true, "label": "Annual Percentage Rate"}]}',
    '{"eligibility_criteria": [{"field": "disclosure_code", "source": "REQUEST_CONTEXT", "operator": "EQUALS", "values": ["D164"]}]}',
    true
);

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '9048c74d-7d31-427b-b29e-392c5479ab15',
    'PLATINUM_CARD_AGREEMENT',
    1,
    'Legal',
    'Platinum Card Agreement',
    'Special terms for platinum cardholders',
    'CREDIT_CARD',
    false,
    'http://localhost:8080/api/v1/mock-api/credit-info',
    '{"fields": [{"name": "membership_tier", "path": "$.eligibility.tier", "type": "STRING", "required": true, "label": "Membership Tier"}, {"name": "effective_date", "path": "$.metadata.effectiveDate", "type": "DATE", "required": true, "label": "Effective Date"}]}',
    '{"eligibility_criteria": [{"field": "membership_tier", "source": "API_CALL", "operator": "IN", "values": ["PLATINUM", "GOLD"], "api_endpoint": "/mock-api/credit-info"}]}',
    true
);

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    'bd37b924-1ada-4808-8013-781cf025be54',
    'PRIVACY_POLICY',
    1,
    'Regulatory',
    'Privacy Policy',
    'Privacy policy document',
    'SHARED',
    true,
    NULL,
    '{"fields": []}',
    '{}',
    true
);

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '728d0c67-e5ec-40e7-b36f-db0af6509b26',
    'TERMS_OF_SERVICE',
    1,
    'Regulatory',
    'Terms of Service',
    'General terms of service',
    'SHARED',
    true,
    NULL,
    '{"fields": []}',
    '{}',
    true
);

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '28b70aa9-6590-4834-98af-fceb5d915f22',
    'TAX_1099_INT',
    1,
    'Tax',
    '1099-INT Form',
    'Year-end tax interest statement',
    'TAX',
    false,
    NULL,
    '{"fields": [{"name": "tax_year", "path": "$.header.taxYear", "type": "STRING", "required": true, "label": "Tax Year"}, {"name": "interest_income", "path": "$.amounts.interestIncome", "type": "NUMBER", "required": true, "label": "Interest Income"}, {"name": "federal_tax_withheld", "path": "$.amounts.federalTaxWithheld", "type": "NUMBER", "required": false, "label": "Federal Tax Withheld"}]}',
    '{"eligibility_criteria": [{"field": "interest_earned", "source": "API_CALL", "operator": "GREATER_THAN", "values": ["10"], "api_endpoint": "/mock-api/tax-info"}]}',
    true
);

INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '98a6b88d-9378-4974-a5b4-b074ae90c907',
    'MORTGAGE_STATEMENT',
    1,
    'Statement',
    'Mortgage Statement',
    'Monthly mortgage statement',
    'MORTGAGE',
    false,
    NULL,
    '{"fields": [{"name": "statement_date", "path": "$.header.statementDate", "type": "DATE", "required": true, "label": "Statement Date"}, {"name": "principal_balance", "path": "$.summary.principalBalance", "type": "NUMBER", "required": true, "label": "Principal Balance"}, {"name": "payment_amount", "path": "$.summary.paymentAmount", "type": "NUMBER", "required": true, "label": "Monthly Payment"}]}',
    '{}',
    true
);


-- Storage Index (Documents)
-- Generated: 2025-12-06T23:25:49.776233

INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, extracted_data
) VALUES (
    '8c76ea36-a5c9-473a-a4ae-c4a0985f7cc2',
    'CREDIT_CARD_STATEMENT',
    1,
    'statement_jan_2024.pdf',
    '/documents/statements/',
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    NULL,
    NULL,
    NOW(),
    '2024-01-31',
    '2027-01-31',
    false,
    true,
    '{"statement_date": "2024-01-31", "balance": "1234.56", "due_date": "2024-02-15"}'
);

INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, extracted_data
) VALUES (
    'd1d8e828-bee5-4216-9547-a7506b1fe05d',
    'CREDIT_CARD_STATEMENT',
    1,
    'statement_feb_2024.pdf',
    '/documents/statements/',
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    NULL,
    NULL,
    NOW(),
    '2024-02-29',
    '2027-02-28',
    false,
    true,
    '{"statement_date": "2024-02-29", "balance": "1456.78", "due_date": "2024-03-15"}'
);

INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, extracted_data
) VALUES (
    'e2763c50-0828-4215-a764-317469837250',
    'CREDIT_CARD_TERMS',
    1,
    'Credit_Card_Terms_D164.pdf',
    '/documents/terms/',
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'D164',
    'Disclosure_Code',
    NOW(),
    '2024-01-01',
    '2027-12-31',
    false,
    true,
    '{}'
);

INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, extracted_data
) VALUES (
    '3055a2e9-8831-4471-a713-b12c11be9b49',
    'PLATINUM_CARD_AGREEMENT',
    1,
    'Platinum_Agreement.pdf',
    '/documents/agreements/',
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    NULL,
    NULL,
    NOW(),
    '2024-01-01',
    '2027-12-31',
    false,
    true,
    '{}'
);

INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, extracted_data
) VALUES (
    '06915f48-43fd-4150-a93f-5bd274f4a909',
    'PRIVACY_POLICY',
    1,
    'Privacy_Policy_2024.pdf',
    '/documents/regulatory/',
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    '2024-01-01',
    '2027-12-31',
    true,
    true,
    '{}'
);

INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, extracted_data
) VALUES (
    'cb8c8a3a-4cac-46c8-aa00-712d1a7f5144',
    'TAX_1099_INT',
    1,
    '1099_INT_2024.pdf',
    '/documents/tax/',
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    NULL,
    NULL,
    NOW(),
    '2024-01-15',
    '2025-04-15',
    false,
    true,
    '{"statement_date": "2024", "balance": "523.45"}'
);
