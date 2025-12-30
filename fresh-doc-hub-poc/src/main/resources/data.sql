-- =============================================================================
-- DOCUMENT HUB SEED DATA
-- =============================================================================
-- Auto-generated from YAML files
-- Generated: 2025-12-30T12:33:53.523727
--
-- Source files:
--   - templates.yaml
--   - documents.yaml (if exists)
--   - accounts.yaml (if exists)
--
-- To regenerate: python generate_sql.py > ../data.sql
-- =============================================================================


-- ====================================================================
-- MASTER TEMPLATE DEFINITIONS
-- ====================================================================

-- Template: Privacy Policy 2024
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    'Privacy Policy 2024',
    '2024 Privacy Policy - Shared with all customers',
    'Privacy',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "policyVersion",
  "referenceKeyType": "DOCUMENT_VERSION"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Credit Card Statement
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    'Credit Card Statement',
    'Monthly credit card statement',
    'Statement',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    false,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Balance Transfer Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    1,
    'BalanceTransferLetter',
    'Balance Transfer Offer',
    'Special balance transfer offer for credit card customers',
    'Promotional',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: VIP Exclusive Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '44444444-4444-4444-4444-444444444444',
    1,
    'CustomerLetter',
    'VIP Exclusive Offer',
    'Exclusive offer for VIP customers in US West region',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "customerSegment",
      "operator": "EQUALS",
      "value": "VIP"
    },
    {
      "field": "region",
      "operator": "EQUALS",
      "value": "US_WEST"
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Regulatory Disclosure
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '55555555-5555-5555-5555-555555555555',
    1,
    'ElectronicDisclosure',
    'Regulatory Disclosure',
    'Regulatory disclosure document matched by disclosure code',
    'Disclosure',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "disclosureCode",
  "referenceKeyType": "DISCLOSURE_CODE"
}',
    '{
  "fieldsToExtract": [
    "disclosureCode"
  ],
  "fieldSources": {
    "disclosureCode": {
      "description": "Disclosure code from account or product configuration",
      "sourceApi": "accountDisclosureApi",
      "extractionPath": "$.disclosureCode",
      "requiredInputs": [
        "accountId"
      ],
      "fieldType": "string"
    }
  },
  "dataSources": {
    "accountDisclosureApi": {
      "description": "Get disclosure code applicable to the account",
      "endpoint": {
        "url": "http://localhost:8080/api/v1/mock-api/accounts/${accountId}/disclosures",
        "method": "GET",
        "timeout": 5000
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 1800,
        "keyPattern": "disclosures:${accountId}"
      },
      "providesFields": [
        "disclosureCode"
      ]
    }
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Credit Card Terms and Conditions
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    'Credit Card Terms and Conditions',
    'Terms and conditions document matched by disclosure code',
    'Regulatory',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": []
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "disclosureCode",
  "referenceKeyType": "DISCLOSURE_CODE"
}',
    '{
  "fieldsToExtract": [
    "pricingId",
    "disclosureCode"
  ],
  "fieldSources": {
    "pricingId": {
      "description": "Pricing ID from account arrangements (active PRICING domain)",
      "sourceApi": "accountArrangementsApi",
      "extractionPath": "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId",
      "requiredInputs": [
        "accountId"
      ],
      "fieldType": "string",
      "defaultValue": null
    },
    "disclosureCode": {
      "description": "Disclosure code from pricing service (cardholder agreements TnC code)",
      "sourceApi": "pricingApi",
      "extractionPath": "$.cardholderAgreementsTncCode",
      "requiredInputs": [
        "pricingId"
      ],
      "fieldType": "string",
      "defaultValue": null
    }
  },
  "dataSources": {
    "accountArrangementsApi": {
      "description": "Step 1: Get account arrangements to extract pricingId",
      "endpoint": {
        "url": "http://localhost:8080/api/v1/mock-api/creditcard/accounts/${accountId}/arrangements",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}",
          "X-Request-ID": "${correlationId}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 1800,
        "keyPattern": "arrangements:${accountId}"
      },
      "retry": {
        "maxAttempts": 3,
        "delayMs": 100
      },
      "providesFields": [
        "pricingId"
      ]
    },
    "pricingApi": {
      "description": "Step 2: Get pricing data to extract disclosure code",
      "endpoint": {
        "url": "http://localhost:8080/api/v1/mock-api/pricing-service/prices/${pricingId}",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}",
          "X-Request-ID": "${correlationId}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 3600,
        "keyPattern": "pricing:${pricingId}"
      },
      "retry": {
        "maxAttempts": 2,
        "delayMs": 100
      },
      "providesFields": [
        "disclosureCode"
      ]
    }
  },
  "executionStrategy": {
    "mode": "auto",
    "continueOnError": false,
    "timeout": 15000
  }
}',
    1609459200000,
    'system',
    NOW()
);

-- Template: Bay Area Exclusive Credit Card Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '77777777-7777-7777-7777-777777777777',
    1,
    'PromoOffer',
    'Bay Area Exclusive Credit Card Offer',
    'Special promotional offer for customers in San Francisco Bay Area zipcodes',
    'Promotional',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{
  "reprint_policy": {
    "cooldown_period_days": 30
  }
}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "zipcode",
      "operator": "IN",
      "value": [
        "94102",
        "94103",
        "94104",
        "94105",
        "94107",
        "94108",
        "94109",
        "94110",
        "94111",
        "94112",
        "94114",
        "94115",
        "94116",
        "94117",
        "94118",
        "94121",
        "94122",
        "94123",
        "94124",
        "94127",
        "94129",
        "94130",
        "94131",
        "94132",
        "94133",
        "94134"
      ]
    },
    {
      "field": "accountType",
      "operator": "EQUALS",
      "value": "credit_card"
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "matchMode": "auto_discover",
  "referenceKeyType": "PROMO_CODE"
}',
    '{
  "fieldsToExtract": [
    "zipcode"
  ],
  "fieldSources": {
    "zipcode": {
      "description": "Customer zipcode from address",
      "sourceApi": "customerApi",
      "extractionPath": "$.customer.address.zipCode",
      "requiredInputs": [
        "customerId"
      ],
      "fieldType": "string",
      "defaultValue": null
    }
  },
  "dataSources": {
    "customerApi": {
      "description": "Fetch customer address to get zipcode",
      "endpoint": {
        "url": "http://localhost:8080/api/v1/mock-api/customers/${customerId}",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "X-Request-ID": "${correlationId}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 3600,
        "keyPattern": "customer:${customerId}"
      },
      "providesFields": [
        "zipcode"
      ]
    }
  },
  "executionStrategy": {
    "mode": "sequential",
    "continueOnError": false,
    "timeout": 10000
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Seasonal Promotional Offers
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    'Seasonal Promotional Offers',
    'Holiday and seasonal promotional documents',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "campaignCode",
  "referenceKeyType": "CAMPAIGN_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Interest Rate Change Notice
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    'Interest Rate Change Notice',
    'Notices about APR and interest rate changes',
    'Regulatory',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "noticeId",
  "referenceKeyType": "NOTICE_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Annual Tax Documents
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    1,
    'TaxDocument',
    'Annual Tax Documents',
    'Year-end tax statements and 1099 forms',
    'Tax',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Savings Account Offers
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    1,
    'SavingsOffer',
    'Savings Account Offers',
    'Promotional offers for savings account holders',
    'Promotional',
    'digital_banking',
    'EN_US',
    true,
    true,
    'ACCOUNT_TYPE',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "accountType",
      "operator": "EQUALS",
      "value": "savings"
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Fee Schedule
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    'Fee Schedule',
    'Account fee schedule with version history',
    'Regulatory',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "feeScheduleVersion",
  "referenceKeyType": "DOCUMENT_VERSION"
}',
    '{
  "fieldsToExtract": [
    "feeScheduleVersion"
  ],
  "fieldSources": {
    "feeScheduleVersion": {
      "description": "Fee schedule version applicable to the account",
      "sourceApi": "accountConfigApi",
      "extractionPath": "$.feeScheduleVersion",
      "requiredInputs": [
        "accountId"
      ],
      "fieldType": "string",
      "defaultValue": "FEE-SCHEDULE-V3"
    }
  },
  "dataSources": {
    "accountConfigApi": {
      "description": "Get account configuration to determine fee schedule version",
      "endpoint": {
        "url": "http://localhost:8080/api/v1/mock-api/accounts/${accountId}/config",
        "method": "GET",
        "timeout": 5000
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 3600,
        "keyPattern": "account-config:${accountId}"
      },
      "providesFields": [
        "feeScheduleVersion"
      ]
    }
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: New Account Welcome Kit
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    1,
    'WelcomeKit',
    'New Account Welcome Kit',
    'Welcome documents for new account holders',
    'Onboarding',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "kitVersion",
  "referenceKeyType": "KIT_VERSION"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: East Coast Regional Offers
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    1,
    'RegionalOffer',
    'East Coast Regional Offers',
    'Special offers for US East region customers',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "region",
      "operator": "EQUALS",
      "value": "US_EAST"
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Inactive Promotional Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000001',
    1,
    'InactivePromo',
    'Inactive Promotional Offer',
    'This template is inactive and should never appear',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    false,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "promoCode",
  "referenceKeyType": "PROMO_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Expired Template - Summer 2024 Campaign
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000002',
    1,
    'ExpiredTemplate',
    'Expired Template - Summer 2024 Campaign',
    'Template expired on Sep 30, 2024',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "campaignCode",
  "referenceKeyType": "CAMPAIGN_CODE"
}',
    1717200000000,
    'system',
    NOW()
);

-- Template: Future Template - 2026 Campaign
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000003',
    1,
    'FutureTemplate',
    'Future Template - 2026 Campaign',
    'Template starts on Jan 1, 2026',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "campaignCode",
  "referenceKeyType": "CAMPAIGN_CODE"
}',
    1767225600000,
    'system',
    NOW()
);

-- Template: Multi-Region Coastal Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000004',
    1,
    'CoastalOffer',
    'Multi-Region Coastal Offer',
    'Offer for customers in US_EAST OR US_WEST regions',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "OR",
  "rules": [
    {
      "field": "region",
      "operator": "EQUALS",
      "value": "US_EAST"
    },
    {
      "field": "region",
      "operator": "EQUALS",
      "value": "US_WEST"
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Premium Coastal Credit Card Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000005',
    1,
    'PremiumCoastalOffer',
    'Premium Coastal Credit Card Offer',
    'VIP/Premium customers in coastal regions only',
    'Promotional',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "accountType",
      "operator": "EQUALS",
      "value": "credit_card"
    },
    {
      "operator": "OR",
      "rules": [
        {
          "field": "customerSegment",
          "operator": "EQUALS",
          "value": "VIP"
        },
        {
          "field": "customerSegment",
          "operator": "EQUALS",
          "value": "PREMIUM"
        }
      ]
    },
    {
      "operator": "OR",
      "rules": [
        {
          "field": "region",
          "operator": "EQUALS",
          "value": "US_EAST"
        },
        {
          "field": "region",
          "operator": "EQUALS",
          "value": "US_WEST"
        }
      ]
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Central Region Exclusive
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000006',
    1,
    'CentralOffer',
    'Central Region Exclusive',
    'Offer for US_CENTRAL region only',
    'Promotional',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "region",
      "operator": "EQUALS",
      "value": "US_CENTRAL"
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Payment Confirmation Letters
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000007',
    1,
    'PaymentLetter',
    'Payment Confirmation Letters',
    'Payment confirmation notices',
    'PaymentConfirmationNotice',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Delinquency Notices
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000008',
    1,
    'DelinquencyNotice',
    'Delinquency Notices',
    'Account delinquency notifications',
    'DelinquencyNotice',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Fraud Alert Letters
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000009',
    1,
    'FraudLetter',
    'Fraud Alert Letters',
    'Fraud investigation and resolution letters',
    'FraudLetter',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Adverse Action Notices
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000010',
    1,
    'AdverseActionNotice',
    'Adverse Action Notices',
    'Credit decision adverse action notifications',
    'AdverseAction',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Elite Credit Score Offers
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    eligibility_criteria,
    document_matching_config,
    data_extraction_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f0000000-0000-0000-0000-000000000011',
    1,
    'EliteCreditOffer',
    'Elite Credit Score Offers',
    'Premium offers for customers with credit score 750+',
    'Promotional',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    'CUSTOM_RULES',
    true,
    true,
    'LETTER',
    '{}',
    '{
  "operator": "AND",
  "rules": [
    {
      "field": "creditScore",
      "operator": "GREATER_THAN_OR_EQUAL",
      "value": 750
    }
  ]
}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "offerCode",
  "referenceKeyType": "OFFER_CODE"
}',
    '{
  "fieldsToExtract": [
    "creditScore"
  ],
  "fieldSources": {
    "creditScore": {
      "sourceApi": "creditBureauApi",
      "extractionPath": "$.creditScore",
      "requiredInputs": [
        "customerId"
      ]
    }
  },
  "dataSources": {
    "creditBureauApi": {
      "endpoint": {
        "url": "http://localhost:8080/api/v1/mock-api/credit-bureau/${customerId}",
        "method": "GET",
        "timeout": 5000
      },
      "providesFields": [
        "creditScore"
      ]
    }
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Digital Bank Savings Statement
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f1000000-0000-0000-0000-000000000001',
    1,
    'SavingsStatement',
    'Digital Bank Savings Statement',
    'Monthly savings account statement for digital banking customers',
    'Statement',
    'DIGITAL_BANK',
    'EN_US',
    true,
    false,
    false,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Digital Bank Welcome Kit
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f1000000-0000-0000-0000-000000000002',
    1,
    'WelcomeKit',
    'Digital Bank Welcome Kit',
    'Welcome package for new digital banking customers',
    'Onboarding',
    'DIGITAL_BANK',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'EMAIL',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "kitVersion",
  "referenceKeyType": "KIT_VERSION"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Digital Bank Security Alert
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f1000000-0000-0000-0000-000000000003',
    1,
    'SecurityAlert',
    'Digital Bank Security Alert',
    'Security alert notification for digital banking',
    'Alert',
    'DIGITAL_BANK',
    'EN_US',
    true,
    false,
    true,
    true,
    'PUSH',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Internal Audit Report
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f2000000-0000-0000-0000-000000000001',
    1,
    'InternalAuditReport',
    'Internal Audit Report',
    'Internal document - not visible in customer message center',
    'Internal',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    false,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Collection Notice - Internal
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f2000000-0000-0000-0000-000000000002',
    1,
    'CollectionNotice',
    'Collection Notice - Internal',
    'Collection action documentation - agent view only',
    'Collections',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    false,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Risk Assessment Report
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f2000000-0000-0000-0000-000000000003',
    1,
    'RiskAssessment',
    'Risk Assessment Report',
    'Credit risk assessment - internal use only',
    'Risk',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    false,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Credit Card E-Statement
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f3000000-0000-0000-0000-000000000001',
    1,
    'EStatement',
    'Credit Card E-Statement',
    'Electronic statement delivered via email',
    'Statement',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    false,
    true,
    'EMAIL',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Payment Reminder SMS
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f3000000-0000-0000-0000-000000000002',
    1,
    'PaymentReminderSMS',
    'Payment Reminder SMS',
    'Payment due reminder sent via SMS',
    'Reminder',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    true,
    'SMS',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Transaction Alert Push
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f3000000-0000-0000-0000-000000000003',
    1,
    'TransactionAlertPush',
    'Transaction Alert Push',
    'Real-time transaction alert via push notification',
    'Alert',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    true,
    'PUSH',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Holiday Rewards Campaign Email
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    sharing_scope,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f3000000-0000-0000-0000-000000000004',
    1,
    'MarketingEmail',
    'Holiday Rewards Campaign Email',
    'Holiday season rewards promotion email',
    'Marketing',
    'ENTERPRISE',
    'EN_US',
    true,
    true,
    'ALL',
    true,
    true,
    'EMAIL',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "campaignCode",
  "referenceKeyType": "CAMPAIGN_CODE"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Customer Agreement - View Only
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f4000000-0000-0000-0000-000000000001',
    1,
    'CustomerAgreement',
    'Customer Agreement - View Only',
    'Agreement document - customers can only view and download',
    'Agreement',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    '{
  "roles": {
    "CUSTOMER": {
      "actions": [
        "VIEW",
        "DOWNLOAD"
      ]
    },
    "AGENT": {
      "actions": [
        "VIEW",
        "DOWNLOAD",
        "UPDATE"
      ]
    },
    "SYSTEM": {
      "actions": [
        "VIEW",
        "DOWNLOAD",
        "UPDATE",
        "DELETE"
      ]
    }
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Case Notes - Agent Managed
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f4000000-0000-0000-0000-000000000002',
    1,
    'CaseNotes',
    'Case Notes - Agent Managed',
    'Case notes that agents can manage (including delete)',
    'CaseManagement',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    false,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    '{
  "roles": {
    "CUSTOMER": {
      "actions": []
    },
    "AGENT": {
      "actions": [
        "VIEW",
        "DOWNLOAD",
        "UPDATE",
        "DELETE"
      ]
    },
    "SYSTEM": {
      "actions": [
        "VIEW",
        "DOWNLOAD",
        "UPDATE",
        "DELETE"
      ]
    }
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: System Integration Log
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f4000000-0000-0000-0000-000000000003',
    1,
    'SystemLog',
    'System Integration Log',
    'System integration document - no customer or agent access',
    'System',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    false,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "accountId",
  "referenceKeyType": "ACCOUNT_ID"
}',
    '{
  "roles": {
    "CUSTOMER": {
      "actions": []
    },
    "AGENT": {
      "actions": [
        "VIEW"
      ]
    },
    "SYSTEM": {
      "actions": [
        "VIEW",
        "DOWNLOAD",
        "UPDATE",
        "DELETE"
      ]
    }
  }
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Credit Card Application Disclosure
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f5000000-0000-0000-0000-000000000001',
    1,
    'ApplicationDisclosure',
    'Credit Card Application Disclosure',
    'Required disclosures provided during credit card application',
    'Disclosure',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "applicantId",
  "referenceKeyType": "APPLICANT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Application Status Letter
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f5000000-0000-0000-0000-000000000002',
    1,
    'ApplicationStatus',
    'Application Status Letter',
    'Letter communicating application approval, denial, or pending status',
    'Application',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    false,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "applicantId",
  "referenceKeyType": "APPLICANT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Pre-Approval Credit Card Offer
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f5000000-0000-0000-0000-000000000003',
    1,
    'PreApprovalOffer',
    'Pre-Approval Credit Card Offer',
    'Pre-approved credit card offer for prospective applicants',
    'Promotional',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "applicantId",
  "referenceKeyType": "APPLICANT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- Template: Identity Verification Request
INSERT INTO document_hub.master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    active_flag,
    shared_document_flag,
    single_document_flag,
    message_center_doc_flag,
    communication_type,
    template_config,
    document_matching_config,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'f5000000-0000-0000-0000-000000000004',
    1,
    'IdentityVerification',
    'Identity Verification Request',
    'Request for additional identity verification documents',
    'Verification',
    'ENTERPRISE',
    'EN_US',
    true,
    false,
    true,
    true,
    'LETTER',
    '{}',
    '{
  "matchBy": "reference_key",
  "referenceKeyField": "applicantId",
  "referenceKeyType": "APPLICANT_ID"
}',
    1704067200000,
    'system',
    NOW()
);

-- ====================================================================
-- STORAGE INDEX (DOCUMENTS)
-- ====================================================================

-- Document: privacy_policy_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-2024.1',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000001',
    'privacy_policy_2024.pdf',
    1705276800000,
    true,
    '{
  "version": "2024.1",
  "valid_from": "2024-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: statement_jan_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000002',
    'statement_jan_2024.pdf',
    1706659200000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000002",
  "statement_date": "2024-01-31",
  "balance": 1234.56,
  "valid_from": "2024-01-31",
  "valid_until": "2027-01-31"
}',
    'system',
    NOW()
);

-- Document: statement_jan_2024_acct2.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000003',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000002',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000002',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000003',
    'statement_jan_2024_acct2.pdf',
    1706659200000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000003",
  "statement_date": "2024-01-31",
  "balance": 5678.9,
  "valid_from": "2024-01-31",
  "valid_until": "2027-01-31"
}',
    'system',
    NOW()
);

-- Document: balance_transfer_offer_q1_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000004',
    '33333333-3333-3333-3333-333333333333',
    1,
    'BalanceTransferLetter',
    true,
    'BT-000000000004',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000004',
    'balance_transfer_offer_q1_2024.pdf',
    1706745600000,
    true,
    '{
  "offer_code": "BT2024Q1",
  "apr_rate": 0.0,
  "valid_from": "2024-01-01",
  "valid_until": "2024-06-30"
}',
    'system',
    NOW()
);

-- Document: vip_exclusive_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000005',
    '44444444-4444-4444-4444-444444444444',
    1,
    'CustomerLetter',
    true,
    'VIP-000000000005',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000005',
    'vip_exclusive_offer.pdf',
    1707955200000,
    true,
    '{
  "segment": "VIP",
  "region": "US_WEST",
  "offer_type": "exclusive",
  "valid_from": "2024-02-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: disclosure_d164.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000006',
    '55555555-5555-5555-5555-555555555555',
    1,
    'ElectronicDisclosure',
    true,
    'D164',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000006',
    'disclosure_d164.pdf',
    1704844800000,
    true,
    '{
  "disclosure_code": "D164",
  "regulatory_type": "ESIGN",
  "version": "1.0",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D164_v1.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000007',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D164',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000007',
    'Credit_Card_Terms_D164_v1.pdf',
    1704067200000,
    true,
    '{
  "disclosureCode": "D164",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D164_v0.9_EXPIRED.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000064',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D164',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000060',
    'Credit_Card_Terms_D164_v0.9_EXPIRED.pdf',
    1672531200000,
    true,
    '{
  "disclosureCode": "D164",
  "version": "0.9",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "EXPIRED",
  "supersededBy": "v1.0"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D164_v2.0_FUTURE.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000061',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D164',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000061',
    'Credit_Card_Terms_D164_v2.0_FUTURE.pdf',
    1734220800000,
    true,
    '{
  "disclosureCode": "D164",
  "version": "2.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "FUTURE",
  "supersedes": "v1.0",
  "changes": [
    "Updated APR disclosures",
    "New late fee structure"
  ]
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D165_v1.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000008',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D165',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000008',
    'Credit_Card_Terms_D165_v1.pdf',
    1704067200000,
    true,
    '{
  "disclosureCode": "D165",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: Premium_Credit_Card_Terms_D166_v1.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000009',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D166',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000009',
    'Premium_Credit_Card_Terms_D166_v1.pdf',
    1704067200000,
    true,
    '{
  "disclosureCode": "D166",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "cardTier": "PREMIUM",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D167_v1_EXPIRED.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000058',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D167',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000058',
    'Credit_Card_Terms_D167_v1_EXPIRED.pdf',
    1672531200000,
    true,
    '{
  "disclosureCode": "D167",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "EXPIRED"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D168_v1_FUTURE.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000059',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D168',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000059',
    'Credit_Card_Terms_D168_v1_FUTURE.pdf',
    1734220800000,
    true,
    '{
  "disclosureCode": "D168",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "FUTURE"
}',
    'system',
    NOW()
);

-- Document: Bay_Area_Exclusive_Offer_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    start_date,
    end_date,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000010',
    '77777777-7777-7777-7777-777777777777',
    1,
    'PromoOffer',
    true,
    'PROMO-BAYAREA-2024',
    'PROMO_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000010',
    'Bay_Area_Exclusive_Offer_2024.pdf',
    1704067200000,
    true,
    1704067200000,
    1735689599000,
    '{
  "promoCode": "PROMO-BAYAREA-2024",
  "offerType": "EXCLUSIVE",
  "region": "SF_BAY_AREA",
  "discountPercent": 15,
  "valid_from": "2024-01-01",
  "valid_until": "2024-12-31"
}',
    'system',
    NOW()
);

-- Document: Bay_Area_Exclusive_Offer_2025.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    start_date,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000010b',
    '77777777-7777-7777-7777-777777777777',
    1,
    'PromoOffer',
    true,
    'PROMO-BAYAREA-2025',
    'PROMO_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000010b',
    'Bay_Area_Exclusive_Offer_2025.pdf',
    1735689600000,
    true,
    1735689600000,
    '{
  "promoCode": "PROMO-BAYAREA-2025",
  "offerType": "EXCLUSIVE",
  "region": "SF_BAY_AREA",
  "discountPercent": 20,
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: black_friday_2024_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000011',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'SEASON-000000000011',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000011',
    'black_friday_2024_offer.pdf',
    1732060800000,
    true,
    '{
  "campaign": "BLACK_FRIDAY_2024",
  "discount": "25%",
  "valid_from": "2024-11-25",
  "valid_until": "2024-11-30"
}',
    'system',
    NOW()
);

-- Document: cyber_monday_2024_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000012',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'SEASON-000000000012',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000012',
    'cyber_monday_2024_offer.pdf',
    1733097600000,
    true,
    '{
  "campaign": "CYBER_MONDAY_2024",
  "discount": "30%",
  "valid_from": "2024-12-01",
  "valid_until": "2024-12-02"
}',
    'system',
    NOW()
);

-- Document: christmas_2025_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000013',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'SEASON-000000000013',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000013',
    'christmas_2025_offer.pdf',
    1734220800000,
    true,
    '{
  "campaign": "CHRISTMAS_2025",
  "discount": "20%",
  "valid_from": "2025-12-20",
  "valid_until": "2025-12-26"
}',
    'system',
    NOW()
);

-- Document: new_year_2026_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000014',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'SEASON-000000000014',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000014',
    'new_year_2026_offer.pdf',
    1735689600000,
    true,
    '{
  "campaign": "NEW_YEAR_2026",
  "discount": "15%",
  "valid_from": "2026-01-01",
  "valid_until": "2026-01-15"
}',
    'system',
    NOW()
);

-- Document: holiday_season_2025_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000015',
    '88888888-8888-8888-8888-888888888888',
    1,
    'SeasonalOffer',
    true,
    'SEASON-000000000015',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000015',
    'holiday_season_2025_offer.pdf',
    1733097600000,
    true,
    '{
  "campaign": "HOLIDAY_2025",
  "discount": "18%",
  "valid_from": "2025-12-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: rate_change_notice_q1_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000016',
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    true,
    'RATE-000000000016',
    'NOTICE_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000016',
    'rate_change_notice_q1_2024.pdf',
    1704067200000,
    true,
    '{
  "notice_type": "APR_CHANGE",
  "old_apr": 19.99,
  "new_apr": 21.99,
  "valid_from": "2024-01-01",
  "valid_until": "2024-03-31"
}',
    'system',
    NOW()
);

-- Document: rate_change_notice_q4_2025.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000017',
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    true,
    'RATE-000000000017',
    'NOTICE_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000017',
    'rate_change_notice_q4_2025.pdf',
    1727740800000,
    true,
    '{
  "notice_type": "APR_CHANGE",
  "old_apr": 21.99,
  "new_apr": 22.49,
  "valid_from": "2025-10-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: rate_change_notice_q1_2026.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000018',
    '99999999-9999-9999-9999-999999999999',
    1,
    'RateChangeNotice',
    true,
    'RATE-000000000018',
    'NOTICE_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000018',
    'rate_change_notice_q1_2026.pdf',
    1733097600000,
    true,
    '{
  "notice_type": "APR_CHANGE",
  "old_apr": 22.49,
  "new_apr": 23.99,
  "valid_from": "2026-01-01",
  "valid_until": "2026-03-31"
}',
    'system',
    NOW()
);

-- Document: 1099_INT_2023.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000019',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    1,
    'TaxDocument',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000019',
    '1099_INT_2023.pdf',
    1706745600000,
    true,
    '{
  "tax_year": 2023,
  "form_type": "1099-INT",
  "interest_earned": 245.67,
  "valid_from": "2024-01-31"
}',
    'system',
    NOW()
);

-- Document: 1099_INT_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000020',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    1,
    'TaxDocument',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000020',
    '1099_INT_2024.pdf',
    1735689600000,
    true,
    '{
  "tax_year": 2024,
  "form_type": "1099-INT",
  "interest_earned": 312.45,
  "valid_from": "2025-01-31"
}',
    'system',
    NOW()
);

-- Document: high_yield_savings_promo.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000021',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    1,
    'SavingsOffer',
    true,
    'SAV-000000000021',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000021',
    'high_yield_savings_promo.pdf',
    1727740800000,
    true,
    '{
  "offer_type": "HIGH_YIELD",
  "bonus_apy": 5.25,
  "min_balance": 10000,
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: cd_ladder_promo_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000022',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    1,
    'SavingsOffer',
    true,
    'SAV-000000000022',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000022',
    'cd_ladder_promo_2024.pdf',
    1704067200000,
    true,
    '{
  "offer_type": "CD_LADDER",
  "bonus_apy": 4.75,
  "terms": [
    6,
    12,
    18,
    24
  ],
  "valid_from": "2024-01-01",
  "valid_until": "2024-09-30"
}',
    'system',
    NOW()
);

-- Document: fee_schedule_v1.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000023',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'FEE-SCHEDULE-V1',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000023',
    'fee_schedule_v1.pdf',
    1704067200000,
    true,
    '{
  "version": "1.0",
  "monthly_fee": 12.0,
  "atm_fee": 3.0,
  "valid_from": "2024-01-01",
  "valid_until": "2024-06-30"
}',
    'system',
    NOW()
);

-- Document: fee_schedule_v2.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000024',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'FEE-SCHEDULE-V2',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000024',
    'fee_schedule_v2.pdf',
    1719792000000,
    true,
    '{
  "version": "2.0",
  "monthly_fee": 10.0,
  "atm_fee": 2.5,
  "valid_from": "2024-07-01",
  "valid_until": "2024-12-31"
}',
    'system',
    NOW()
);

-- Document: fee_schedule_v3.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000025',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'FEE-SCHEDULE-V3',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000025',
    'fee_schedule_v3.pdf',
    1735689600000,
    true,
    '{
  "version": "3.0",
  "monthly_fee": 8.0,
  "atm_fee": 0.0,
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: fee_schedule_v4.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000026',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    1,
    'FeeSchedule',
    true,
    'FEE-SCHEDULE-V4',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000026',
    'fee_schedule_v4.pdf',
    1735689600000,
    true,
    '{
  "version": "4.0",
  "monthly_fee": 5.0,
  "atm_fee": 0.0,
  "note": "Reduced fees program",
  "valid_from": "2026-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: welcome_kit_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000027',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    1,
    'WelcomeKit',
    true,
    'WK-2024',
    'KIT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000027',
    'welcome_kit_2024.pdf',
    1704067200000,
    true,
    '{
  "kit_version": "2024",
  "includes": [
    "account_guide",
    "mobile_app_setup",
    "security_tips"
  ],
  "valid_from": "2024-01-01",
  "valid_until": "2024-12-31"
}',
    'system',
    NOW()
);

-- Document: welcome_kit_2025.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000028',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    1,
    'WelcomeKit',
    true,
    'WK-2025',
    'KIT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000028',
    'welcome_kit_2025.pdf',
    1735689600000,
    true,
    '{
  "kit_version": "2025",
  "includes": [
    "account_guide",
    "mobile_app_setup",
    "security_tips",
    "rewards_overview"
  ],
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: east_coast_winter_special.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000029',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    1,
    'RegionalOffer',
    true,
    'REG-000000000029',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000029',
    'east_coast_winter_special.pdf',
    1733097600000,
    true,
    '{
  "region": "US_EAST",
  "offer_name": "Winter Warmth Bonus",
  "bonus_points": 5000,
  "valid_from": "2025-12-01",
  "valid_until": "2026-02-28"
}',
    'system',
    NOW()
);

-- Document: privacy_policy_evergreen.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000030',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-EVERGREEN',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000030',
    'privacy_policy_evergreen.pdf',
    1704067200000,
    true,
    '{
  "version": "EVERGREEN",
  "note": "This document has no validity period - always valid"
}',
    'system',
    NOW()
);

-- Document: privacy_policy_legacy.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000031',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-LEGACY',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000031',
    'privacy_policy_legacy.pdf',
    1609459200000,
    true,
    '{
  "version": "LEGACY",
  "note": "Valid until replaced",
  "valid_until": "2025-06-30"
}',
    'system',
    NOW()
);

-- Document: inactive_promo_should_not_appear.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000032',
    'f0000000-0000-0000-0000-000000000001',
    1,
    'InactivePromo',
    true,
    'INACTIVE-000000000032',
    'PROMO_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000032',
    'inactive_promo_should_not_appear.pdf',
    1704067200000,
    true,
    '{
  "note": "This document should NEVER appear - template is inactive",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: summer_2024_expired_template.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000033',
    'f0000000-0000-0000-0000-000000000002',
    1,
    'ExpiredTemplate',
    true,
    'EXPIRED-000000000033',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000033',
    'summer_2024_expired_template.pdf',
    1719792000000,
    true,
    '{
  "campaign": "SUMMER_2024",
  "note": "Template expired, should not appear"
}',
    'system',
    NOW()
);

-- Document: future_2026_template.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000034',
    'f0000000-0000-0000-0000-000000000003',
    1,
    'FutureTemplate',
    true,
    'FUTURE-000000000034',
    'CAMPAIGN_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000034',
    'future_2026_template.pdf',
    1735689600000,
    true,
    '{
  "campaign": "2026_LAUNCH",
  "note": "Template not yet active, should not appear"
}',
    'system',
    NOW()
);

-- Document: inaccessible_document.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000035',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-000000000035',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000035',
    'inaccessible_document.pdf',
    1704067200000,
    false,
    '{
  "note": "Document marked inaccessible - should not appear",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: null_metadata_document.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000036',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-000000000036',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000036',
    'null_metadata_document.pdf',
    1704067200000,
    true,
    'system',
    NOW()
);

-- Document: azure_stored_document.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000037',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-000000000037',
    'DOCUMENT_VERSION',
    'AZURE_BLOB',
    'b0000000-0000-0000-0000-000000000037',
    'azure_stored_document.pdf',
    1704067200000,
    true,
    '{
  "storage": "AZURE_BLOB",
  "container": "documents",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: privacy_policy_uk.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000038',
    '11111111-1111-1111-1111-111111111111',
    1,
    'PrivacyPolicy',
    true,
    'PP-000000000038',
    'DOCUMENT_VERSION',
    'ecms',
    'b0000000-0000-0000-0000-000000000038',
    'privacy_policy_uk.pdf',
    1704067200000,
    true,
    '{
  "language": "EN_UK",
  "region": "UK",
  "valid_from": "2024-01-01",
  "valid_until": "2026-12-31"
}',
    'system',
    NOW()
);

-- Document: coastal_regions_offer.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000039',
    'f0000000-0000-0000-0000-000000000004',
    1,
    'CoastalOffer',
    true,
    'COAST-000000000039',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000039',
    'coastal_regions_offer.pdf',
    1704067200000,
    true,
    '{
  "offer": "COASTAL_2025",
  "eligible_regions": [
    "US_EAST",
    "US_WEST"
  ],
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: premium_coastal_exclusive.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000040',
    'f0000000-0000-0000-0000-000000000005',
    1,
    'PremiumCoastalOffer',
    true,
    'PREM-COAST-000000000040',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000040',
    'premium_coastal_exclusive.pdf',
    1704067200000,
    true,
    '{
  "offer": "PREMIUM_COASTAL_2025",
  "segments": [
    "VIP",
    "PREMIUM"
  ],
  "regions": [
    "US_EAST",
    "US_WEST"
  ],
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: central_region_exclusive.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000041',
    'f0000000-0000-0000-0000-000000000006',
    1,
    'CentralOffer',
    true,
    'CENTRAL-000000000041',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000041',
    'central_region_exclusive.pdf',
    1704067200000,
    true,
    '{
  "region": "US_CENTRAL",
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: payment_confirmation_dec_2025.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000042',
    'f0000000-0000-0000-0000-000000000007',
    1,
    'PaymentLetter',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000042',
    'payment_confirmation_dec_2025.pdf',
    1733097600000,
    true,
    '{
  "payment_date": "2025-12-01",
  "amount": 150.0,
  "confirmation_number": "PAY-2025-001",
  "valid_from": "2025-12-01",
  "valid_until": "2028-12-01"
}',
    'system',
    NOW()
);

-- Document: delinquency_notice_30day.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000043',
    'f0000000-0000-0000-0000-000000000008',
    1,
    'DelinquencyNotice',
    false,
    'aaaa0000-0000-0000-0000-000000000002',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000002',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000043',
    'delinquency_notice_30day.pdf',
    1732060800000,
    true,
    '{
  "days_past_due": 30,
  "amount_due": 275.5,
  "notice_type": "30_DAY",
  "valid_from": "2025-11-20",
  "valid_until": "2028-11-20"
}',
    'system',
    NOW()
);

-- Document: fraud_investigation_resolved.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000044',
    'f0000000-0000-0000-0000-000000000009',
    1,
    'FraudLetter',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000044',
    'fraud_investigation_resolved.pdf',
    1730419200000,
    true,
    '{
  "case_number": "FRD-2025-0042",
  "status": "RESOLVED",
  "resolution": "NO_FRAUD_CONFIRMED",
  "valid_from": "2025-11-01",
  "valid_until": "2032-11-01"
}',
    'system',
    NOW()
);

-- Document: adverse_action_cli_denied.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000045',
    'f0000000-0000-0000-0000-000000000010',
    1,
    'AdverseActionNotice',
    false,
    'aaaa0000-0000-0000-0000-000000000003',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000003',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000045',
    'adverse_action_cli_denied.pdf',
    1727740800000,
    true,
    '{
  "action_type": "CLI_DENIED",
  "reason_codes": [
    "R001",
    "R003"
  ],
  "valid_from": "2025-10-01",
  "valid_until": "2032-10-01"
}',
    'system',
    NOW()
);

-- Document: statement_feb_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000046',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000046',
    'statement_feb_2024.pdf',
    1709251200000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000046",
  "statement_date": "2024-02-29",
  "balance": 1456.78,
  "valid_from": "2024-02-29",
  "valid_until": "2027-02-28"
}',
    'system',
    NOW()
);

-- Document: statement_mar_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000047',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000047',
    'statement_mar_2024.pdf',
    1711929600000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000047",
  "statement_date": "2024-03-31",
  "balance": 1678.9,
  "valid_from": "2024-03-31",
  "valid_until": "2027-03-31"
}',
    'system',
    NOW()
);

-- Document: statement_apr_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000048',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000048',
    'statement_apr_2024.pdf',
    1714521600000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000048",
  "statement_date": "2024-04-30",
  "balance": 2012.34,
  "valid_from": "2024-04-30",
  "valid_until": "2027-04-30"
}',
    'system',
    NOW()
);

-- Document: statement_may_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000049',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000049',
    'statement_may_2024.pdf',
    1717200000000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000049",
  "statement_date": "2024-05-31",
  "balance": 1890.12,
  "valid_from": "2024-05-31",
  "valid_until": "2027-05-31"
}',
    'system',
    NOW()
);

-- Document: statement_jun_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000050',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000050',
    'statement_jun_2024.pdf',
    1719792000000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000050",
  "statement_date": "2024-06-30",
  "balance": 2234.56,
  "valid_from": "2024-06-30",
  "valid_until": "2027-06-30"
}',
    'system',
    NOW()
);

-- Document: statement_jul_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000051',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000051',
    'statement_jul_2024.pdf',
    1722470400000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000051",
  "statement_date": "2024-07-31",
  "balance": 1567.89,
  "valid_from": "2024-07-31",
  "valid_until": "2027-07-31"
}',
    'system',
    NOW()
);

-- Document: statement_aug_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000052',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000052',
    'statement_aug_2024.pdf',
    1725148800000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000052",
  "statement_date": "2024-08-31",
  "balance": 1789.01,
  "valid_from": "2024-08-31",
  "valid_until": "2027-08-31"
}',
    'system',
    NOW()
);

-- Document: statement_sep_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000053',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000053',
    'statement_sep_2024.pdf',
    1727740800000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000053",
  "statement_date": "2024-09-30",
  "balance": 2345.67,
  "valid_from": "2024-09-30",
  "valid_until": "2027-09-30"
}',
    'system',
    NOW()
);

-- Document: statement_oct_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000054',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000054',
    'statement_oct_2024.pdf',
    1730419200000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000054",
  "statement_date": "2024-10-31",
  "balance": 1987.65,
  "valid_from": "2024-10-31",
  "valid_until": "2027-10-31"
}',
    'system',
    NOW()
);

-- Document: statement_nov_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000055',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000055',
    'statement_nov_2024.pdf',
    1733011200000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000055",
  "statement_date": "2024-11-30",
  "balance": 2678.9,
  "valid_from": "2024-11-30",
  "valid_until": "2027-11-30"
}',
    'system',
    NOW()
);

-- Document: statement_dec_2024.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    account_key,
    customer_key,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000056',
    '22222222-2222-2222-2222-222222222222',
    1,
    'monthly_statement',
    false,
    'aaaa0000-0000-0000-0000-000000000001',
    'cccc0000-0000-0000-0000-000000000001',
    'aaaa0000-0000-0000-0000-000000000001',
    'ACCOUNT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000056',
    'statement_dec_2024.pdf',
    1735689600000,
    true,
    '{
  "statement_id": "ssss0000-0000-0000-0000-000000000056",
  "statement_date": "2024-12-31",
  "balance": 3012.34,
  "valid_from": "2024-12-31",
  "valid_until": "2027-12-31"
}',
    'system',
    NOW()
);

-- Document: elite_credit_platinum_rewards.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000057',
    'f0000000-0000-0000-0000-000000000011',
    1,
    'EliteCreditOffer',
    true,
    'ELITE-000000000057',
    'OFFER_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000057',
    'elite_credit_platinum_rewards.pdf',
    1704067200000,
    true,
    '{
  "offer": "ELITE_PLATINUM_2025",
  "min_credit_score": 750,
  "apr": 12.99,
  "valid_from": "2025-01-01",
  "valid_until": "2025-12-31"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D167_v1_EXPIRED.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    start_date,
    end_date,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000062',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D167',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000058',
    'Credit_Card_Terms_D167_v1_EXPIRED.pdf',
    1672531200000,
    true,
    1672531200000,
    1703980800000,
    '{
  "disclosureCode": "D167",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "EXPIRED"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D168_v1_FUTURE.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    start_date,
    end_date,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000063',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D168',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000059',
    'Credit_Card_Terms_D168_v1_FUTURE.pdf',
    1767225600000,
    true,
    1767225600000,
    1798761600000,
    '{
  "disclosureCode": "D168",
  "version": "1.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "FUTURE"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D164_v0.9_EXPIRED.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    start_date,
    end_date,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000060',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D164',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000060',
    'Credit_Card_Terms_D164_v0.9_EXPIRED.pdf',
    1672531200000,
    true,
    1672531200000,
    1703980800000,
    '{
  "disclosureCode": "D164",
  "version": "0.9",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "EXPIRED",
  "supersededBy": "v1.0"
}',
    'system',
    NOW()
);

-- Document: Credit_Card_Terms_D164_v2.0_FUTURE.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    start_date,
    end_date,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000066',
    '66666666-6666-6666-6666-666666666666',
    1,
    'CardholderAgreement',
    true,
    'D164',
    'DISCLOSURE_CODE',
    'ecms',
    'b0000000-0000-0000-0000-000000000061',
    'Credit_Card_Terms_D164_v2.0_FUTURE.pdf',
    1798761600000,
    true,
    1798761600000,
    1830297600000,
    '{
  "disclosureCode": "D164",
  "version": "2.0",
  "documentType": "CARDHOLDER_AGREEMENT",
  "status": "FUTURE"
}',
    'system',
    NOW()
);

-- Document: application_disclosure_john_smith.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000070',
    'f5000000-0000-0000-0000-000000000001',
    1,
    'ApplicationDisclosure',
    false,
    'appl-0000-0000-0000-000000000001',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000070',
    'application_disclosure_john_smith.pdf',
    1704067200000,
    true,
    '{
  "applicant_name": "John Smith",
  "application_date": "2024-01-01",
  "product_type": "CREDIT_CARD",
  "valid_from": "2024-01-01",
  "valid_until": "2031-01-01"
}',
    'system',
    NOW()
);

-- Document: application_approved_john_smith.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000071',
    'f5000000-0000-0000-0000-000000000002',
    1,
    'ApplicationStatus',
    false,
    'appl-0000-0000-0000-000000000001',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000071',
    'application_approved_john_smith.pdf',
    1704153600000,
    true,
    '{
  "applicant_name": "John Smith",
  "status": "APPROVED",
  "decision_date": "2024-01-02",
  "credit_limit": 5000,
  "valid_from": "2024-01-02",
  "valid_until": "2031-01-02"
}',
    'system',
    NOW()
);

-- Document: preapproval_offer_john_smith.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000072',
    'f5000000-0000-0000-0000-000000000003',
    1,
    'PreApprovalOffer',
    false,
    'appl-0000-0000-0000-000000000001',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000072',
    'preapproval_offer_john_smith.pdf',
    1703980800000,
    true,
    '{
  "applicant_name": "John Smith",
  "offer_type": "PRE_APPROVED",
  "credit_limit_range": "3000-7500",
  "apr_range": "15.99-22.99",
  "offer_expiry": "2024-03-31",
  "valid_from": "2023-12-31",
  "valid_until": "2024-03-31"
}',
    'system',
    NOW()
);

-- Document: application_disclosure_jane_doe.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000073',
    'f5000000-0000-0000-0000-000000000001',
    1,
    'ApplicationDisclosure',
    false,
    'appl-0000-0000-0000-000000000002',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000073',
    'application_disclosure_jane_doe.pdf',
    1733097600000,
    true,
    '{
  "applicant_name": "Jane Doe",
  "application_date": "2024-12-01",
  "product_type": "CREDIT_CARD",
  "valid_from": "2024-12-01",
  "valid_until": "2031-12-01"
}',
    'system',
    NOW()
);

-- Document: application_pending_jane_doe.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000074',
    'f5000000-0000-0000-0000-000000000002',
    1,
    'ApplicationStatus',
    false,
    'appl-0000-0000-0000-000000000002',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000074',
    'application_pending_jane_doe.pdf',
    1733184000000,
    true,
    '{
  "applicant_name": "Jane Doe",
  "status": "PENDING_VERIFICATION",
  "decision_date": "2024-12-02",
  "pending_reason": "ADDITIONAL_DOCS_REQUIRED",
  "valid_from": "2024-12-02",
  "valid_until": "2025-01-02"
}',
    'system',
    NOW()
);

-- Document: identity_verification_request_jane_doe.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000075',
    'f5000000-0000-0000-0000-000000000004',
    1,
    'IdentityVerification',
    false,
    'appl-0000-0000-0000-000000000002',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000075',
    'identity_verification_request_jane_doe.pdf',
    1733184000000,
    true,
    '{
  "applicant_name": "Jane Doe",
  "verification_type": "ID_DOCUMENT",
  "documents_required": [
    "DRIVERS_LICENSE",
    "UTILITY_BILL"
  ],
  "due_date": "2024-12-15",
  "valid_from": "2024-12-02",
  "valid_until": "2024-12-15"
}',
    'system',
    NOW()
);

-- Document: application_disclosure_bob_wilson.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000076',
    'f5000000-0000-0000-0000-000000000001',
    1,
    'ApplicationDisclosure',
    false,
    'appl-0000-0000-0000-000000000003',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000076',
    'application_disclosure_bob_wilson.pdf',
    1730419200000,
    true,
    '{
  "applicant_name": "Bob Wilson",
  "application_date": "2024-11-01",
  "product_type": "CREDIT_CARD",
  "valid_from": "2024-11-01",
  "valid_until": "2031-11-01"
}',
    'system',
    NOW()
);

-- Document: application_denied_bob_wilson.pdf
INSERT INTO document_hub.storage_index (
    storage_index_id,
    master_template_id,
    template_version,
    template_type,
    shared_flag,
    reference_key,
    reference_key_type,
    storage_vendor,
    storage_document_key,
    file_name,
    doc_creation_date,
    accessible_flag,
    doc_metadata,
    created_by,
    created_timestamp
) VALUES (
    'a0000000-0000-0000-0000-000000000077',
    'f5000000-0000-0000-0000-000000000002',
    1,
    'ApplicationStatus',
    false,
    'appl-0000-0000-0000-000000000003',
    'APPLICANT_ID',
    'ecms',
    'b0000000-0000-0000-0000-000000000077',
    'application_denied_bob_wilson.pdf',
    1730505600000,
    true,
    '{
  "applicant_name": "Bob Wilson",
  "status": "DENIED",
  "decision_date": "2024-11-02",
  "denial_reasons": [
    "INSUFFICIENT_CREDIT_HISTORY",
    "HIGH_DEBT_TO_INCOME"
  ],
  "valid_from": "2024-11-02",
  "valid_until": "2031-11-02"
}',
    'system',
    NOW()
);

