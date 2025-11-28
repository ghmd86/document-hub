-- Sample Template Configurations for Document Hub POC
-- Load order: Run this file FIRST before 02-documents.sql
-- Database: PostgreSQL with JSONB support

-- Clean up existing test data (optional)
-- DELETE FROM master_template_definition WHERE template_type IN ('MONTHLY_STATEMENT', 'PRIVACY_POLICY', 'CARDHOLDER_AGREEMENT', 'VIP_LETTER');

---------------------------------------------------------------------------------------------------
-- TEMPLATE 1: Monthly Statement (Simple - No External API Calls)
---------------------------------------------------------------------------------------------------
-- This template demonstrates:
-- - Direct account-based matching
-- - No data extraction from external services
-- - Minimal configuration
---------------------------------------------------------------------------------------------------

INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    is_active,
    is_regulatory,
    is_shared_document,
    data_extraction_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'a1111111-1111-1111-1111-111111111111'::uuid,
    1,
    'MONTHLY_STATEMENT',
    'Monthly Account Statement',
    'Monthly statement showing account activity and balance',
    'STATEMENTS',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    false,
    '{
        "executionRules": {
            "executionMode": "sequential",
            "maxParallelCalls": 1,
            "timeout": 5000
        },
        "extractionStrategy": [],
        "documentMatchingStrategy": {
            "strategy": "account-key",
            "conditions": []
        }
    }'::jsonb,
    '{
        "eligibilityType": "criteria-based",
        "eligibilityCriteria": {},
        "logic": "AND"
    }'::jsonb,
    extract(epoch from timestamp '2024-01-01') * 1000,
    'system',
    CURRENT_TIMESTAMP
);

---------------------------------------------------------------------------------------------------
-- TEMPLATE 2: Privacy Policy (Single API Call - Location-Based)
---------------------------------------------------------------------------------------------------
-- This template demonstrates:
-- - Single external API call to get customer location
-- - Metadata-based document matching
-- - Simple data extraction with JSONPath
---------------------------------------------------------------------------------------------------

INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    is_active,
    is_regulatory,
    is_shared_document,
    data_extraction_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'a2222222-2222-2222-2222-222222222222'::uuid,
    1,
    'PRIVACY_POLICY',
    'Privacy Policy Notice',
    'State-specific privacy policy document',
    'REGULATORY',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    true,
    '{
        "executionRules": {
            "executionMode": "sequential",
            "maxParallelCalls": 1,
            "timeout": 10000
        },
        "extractionStrategy": [
            {
                "id": "get_customer_location",
                "description": "Fetch customer location from customer service",
                "endpoint": {
                    "url": "http://localhost:8081/api/customers/${input.customerId}/location",
                    "method": "GET",
                    "timeout": 3000
                },
                "cache": {
                    "enabled": true,
                    "ttl": 3600,
                    "keyPattern": "customer:${input.customerId}:location"
                },
                "responseMapping": {
                    "extract": {
                        "$location.state": "$.state",
                        "$location.country": "$.country"
                    },
                    "validate": {
                        "$location.state": {
                            "required": true,
                            "pattern": "^[A-Z]{2}$"
                        }
                    }
                }
            }
        ],
        "documentMatchingStrategy": {
            "strategy": "metadata",
            "conditions": [
                {
                    "type": "metadata_field",
                    "metadataKey": "state",
                    "valueSource": "$location.state",
                    "operator": "EQUALS"
                }
            ]
        }
    }'::jsonb,
    '{
        "eligibilityType": "criteria-based",
        "eligibilityCriteria": {
            "has_location": {
                "field": "$location.state",
                "operator": "notNull",
                "value": null
            }
        },
        "logic": "AND"
    }'::jsonb,
    extract(epoch from timestamp '2024-01-01') * 1000,
    'system',
    CURRENT_TIMESTAMP
);

---------------------------------------------------------------------------------------------------
-- TEMPLATE 3: Cardholder Agreement (2-Step Chain - Pricing → Disclosure)
---------------------------------------------------------------------------------------------------
-- This template demonstrates:
-- - Multi-step API call chaining
-- - Conditional execution (nextCalls)
-- - Variable dependency between steps
-- - Reference key matching
---------------------------------------------------------------------------------------------------

INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    is_active,
    is_regulatory,
    is_shared_document,
    data_extraction_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'a3333333-3333-3333-3333-333333333333'::uuid,
    1,
    'CARDHOLDER_AGREEMENT',
    'Cardholder Agreement',
    'Account-specific cardholder agreement based on pricing tier',
    'AGREEMENTS',
    'CREDIT_CARD',
    'EN_US',
    true,
    true,
    false,
    '{
        "executionRules": {
            "executionMode": "sequential",
            "maxParallelCalls": 1,
            "timeout": 15000
        },
        "extractionStrategy": [
            {
                "id": "get_pricing_id",
                "description": "Get pricing ID from account service",
                "endpoint": {
                    "url": "http://localhost:8081/api/accounts/${input.accountId}/pricing",
                    "method": "GET",
                    "timeout": 5000
                },
                "cache": {
                    "enabled": true,
                    "ttl": 1800,
                    "keyPattern": "account:${input.accountId}:pricing"
                },
                "responseMapping": {
                    "extract": {
                        "$pricing.pricingId": "$.pricingId",
                        "$pricing.tier": "$.tier"
                    },
                    "validate": {
                        "$pricing.pricingId": {
                            "required": true
                        }
                    }
                },
                "nextCalls": [
                    {
                        "condition": {
                            "field": "$pricing.pricingId",
                            "operator": "notNull",
                            "value": null
                        },
                        "targetDataSource": "get_disclosure_code"
                    }
                ]
            },
            {
                "id": "get_disclosure_code",
                "description": "Get disclosure code using pricing ID",
                "dependencies": ["$pricing.pricingId"],
                "endpoint": {
                    "url": "http://localhost:8081/api/pricing/${pricing.pricingId}/disclosure",
                    "method": "GET",
                    "timeout": 5000
                },
                "cache": {
                    "enabled": true,
                    "ttl": 3600,
                    "keyPattern": "pricing:${pricing.pricingId}:disclosure"
                },
                "responseMapping": {
                    "extract": {
                        "$disclosure.code": "$.disclosureCode",
                        "$disclosure.version": "$.version"
                    },
                    "validate": {
                        "$disclosure.code": {
                            "required": true,
                            "pattern": "^D[0-9]{3,4}$"
                        }
                    }
                }
            }
        ],
        "documentMatchingStrategy": {
            "strategy": "reference_key",
            "conditions": [
                {
                    "type": "reference_key",
                    "metadataKey": "referenceKey",
                    "valueSource": "$disclosure.code",
                    "operator": "EQUALS"
                },
                {
                    "type": "reference_key",
                    "metadataKey": "referenceKeyType",
                    "valueSource": "DISCLOSURE_CODE",
                    "operator": "EQUALS"
                }
            ]
        }
    }'::jsonb,
    '{
        "eligibilityType": "criteria-based",
        "eligibilityCriteria": {
            "has_pricing_id": {
                "field": "$pricing.pricingId",
                "operator": "notNull",
                "value": null
            },
            "has_disclosure_code": {
                "field": "$disclosure.code",
                "operator": "notNull",
                "value": null
            }
        },
        "logic": "AND"
    }'::jsonb,
    extract(epoch from timestamp '2024-01-01') * 1000,
    'system',
    CURRENT_TIMESTAMP
);

---------------------------------------------------------------------------------------------------
-- TEMPLATE 4: VIP Customer Letter (3-Step Chain with Complex Rules)
---------------------------------------------------------------------------------------------------
-- This template demonstrates:
-- - Three-step API call chain
-- - Complex eligibility criteria
-- - Multiple conditions with AND logic
-- - Composite document matching
---------------------------------------------------------------------------------------------------

INSERT INTO master_template_definition (
    master_template_id,
    template_version,
    template_type,
    template_name,
    template_description,
    template_category,
    line_of_business,
    language_code,
    is_active,
    is_regulatory,
    is_shared_document,
    data_extraction_config,
    access_control,
    start_date,
    created_by,
    created_timestamp
) VALUES (
    'a4444444-4444-4444-4444-444444444444'::uuid,
    1,
    'VIP_LETTER',
    'VIP Customer Letter',
    'Personalized letter for VIP tier customers',
    'MARKETING',
    'CREDIT_CARD',
    'EN_US',
    true,
    false,
    false,
    '{
        "executionRules": {
            "executionMode": "sequential",
            "maxParallelCalls": 1,
            "timeout": 20000
        },
        "extractionStrategy": [
            {
                "id": "get_customer_tier",
                "description": "Get customer tier information",
                "endpoint": {
                    "url": "http://localhost:8081/api/customers/${input.customerId}/tier",
                    "method": "GET",
                    "timeout": 5000
                },
                "cache": {
                    "enabled": true,
                    "ttl": 7200,
                    "keyPattern": "customer:${input.customerId}:tier"
                },
                "responseMapping": {
                    "extract": {
                        "$customer.tier": "$.tier",
                        "$customer.status": "$.status",
                        "$customer.joinDate": "$.vipJoinDate"
                    }
                },
                "nextCalls": [
                    {
                        "condition": {
                            "field": "$customer.tier",
                            "operator": "equals",
                            "value": "VIP"
                        },
                        "targetDataSource": "get_vip_benefits"
                    }
                ]
            },
            {
                "id": "get_vip_benefits",
                "description": "Get VIP benefits and offers",
                "dependencies": ["$customer.tier"],
                "endpoint": {
                    "url": "http://localhost:8081/api/vip/benefits?tier=${customer.tier}",
                    "method": "GET",
                    "timeout": 5000
                },
                "responseMapping": {
                    "extract": {
                        "$vip.offerCode": "$.currentOfferCode",
                        "$vip.expiryDate": "$.offerExpiryDate"
                    }
                },
                "nextCalls": [
                    {
                        "condition": {
                            "field": "$vip.offerCode",
                            "operator": "notNull",
                            "value": null
                        },
                        "targetDataSource": "get_personalized_content"
                    }
                ]
            },
            {
                "id": "get_personalized_content",
                "description": "Get personalized content for VIP letter",
                "dependencies": ["$vip.offerCode"],
                "endpoint": {
                    "url": "http://localhost:8081/api/content/personalized?offer=${vip.offerCode}&customer=${input.customerId}",
                    "method": "GET",
                    "timeout": 5000
                },
                "responseMapping": {
                    "extract": {
                        "$content.templateId": "$.letterTemplateId"
                    }
                }
            }
        ],
        "documentMatchingStrategy": {
            "strategy": "composite",
            "conditions": [
                {
                    "type": "metadata_field",
                    "metadataKey": "tier",
                    "valueSource": "$customer.tier",
                    "operator": "EQUALS"
                },
                {
                    "type": "metadata_field",
                    "metadataKey": "offerCode",
                    "valueSource": "$vip.offerCode",
                    "operator": "EQUALS"
                }
            ]
        }
    }'::jsonb,
    '{
        "eligibilityType": "criteria-based",
        "eligibilityCriteria": {
            "is_vip": {
                "field": "$customer.tier",
                "operator": "equals",
                "value": "VIP"
            },
            "is_active": {
                "field": "$customer.status",
                "operator": "equals",
                "value": "ACTIVE"
            },
            "has_offer": {
                "field": "$vip.offerCode",
                "operator": "notNull",
                "value": null
            }
        },
        "logic": "AND"
    }'::jsonb,
    extract(epoch from timestamp '2024-01-01') * 1000,
    'system',
    CURRENT_TIMESTAMP
);

-- Verify templates were inserted
SELECT template_type, template_version, template_name, is_active
FROM master_template_definition
WHERE template_type IN ('MONTHLY_STATEMENT', 'PRIVACY_POLICY', 'CARDHOLDER_AGREEMENT', 'VIP_LETTER')
ORDER BY template_type;
