# Document Hub Service - Denormalized Database Schema (Performance Optimized)

## Overview
This schema design prioritizes **query performance** by avoiding joins through denormalization while still fixing critical design issues:
1. Proper composite primary key for template versioning
2. Consistent data types (TIMESTAMPTZ for all dates)
3. Removed redundant columns
4. Optimized JSONB structure with proper indexing
5. Strategic denormalization to avoid joins in common queries

**Philosophy**: "Store data redundantly where it's queried frequently, normalize where it changes frequently"

---

## Core Tables (Denormalized Design)

### 1. Master Template Definition

**Purpose:** Stores template metadata with all related data embedded.

```sql
CREATE TABLE master_template_definition (
    -- Primary Keys (Composite for versioning)
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,

    -- Template Identification
    legacy_template_id VARCHAR(100),
    template_name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Business Classification (Denormalized - no category lookup table)
    line_of_business VARCHAR(50) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(100) NOT NULL,
    language_code VARCHAR(10) NOT NULL DEFAULT 'en_us',
    owning_dept VARCHAR(100),

    -- Flags and Settings
    notification_needed BOOLEAN DEFAULT FALSE,
    is_regulatory BOOLEAN DEFAULT FALSE,
    is_message_center_doc BOOLEAN DEFAULT FALSE,
    is_shared_document BOOLEAN DEFAULT FALSE,
    sharing_scope VARCHAR(100),

    -- Status and Lifecycle
    template_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_date TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    retention_days INTEGER DEFAULT 365,

    -- ========================================
    -- JSONB Fields (Optimized Structure)
    -- ========================================

    -- Access Control Rules
    access_control JSONB NOT NULL DEFAULT '[]'::jsonb,
    /*
    Structure:
    [
        {"role": "admin", "actions": ["View", "Update", "Delete"]},
        {"role": "customer", "actions": ["View", "Download"]},
        {"role": "agent", "actions": ["View"]}
    ]
    */

    -- Required Fields and Validation
    required_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    /*
    Structure:
    [
        {"field": "pricingCode", "type": "string", "required": true, "validation": "^[A-Z0-9]+$"},
        {"field": "disclosureVersion", "type": "integer", "required": true, "min": 1},
        {"field": "effectiveDate", "type": "date", "required": true}
    ]
    */

    -- Delivery Channels
    channels JSONB NOT NULL DEFAULT '[]'::jsonb,
    /*
    Structure:
    [
        {"type": "EMAIL", "enabled": true, "config": {"priority": "high"}},
        {"type": "PRINT", "enabled": true, "config": {"duplex": true}},
        {"type": "WEB", "enabled": true}
    ]
    */

    -- Template Variables and Data Sources
    template_variables JSONB NOT NULL DEFAULT '{}'::jsonb,
    /*
    Structure:
    {
        "customerName": {
            "type": "string",
            "source": "customer-service",
            "endpoint": "/api/v1/customers/{customerId}",
            "method": "GET",
            "jsonPath": "$.data.name",
            "required": true,
            "defaultValue": null,
            "validation": {
                "maxLength": 100,
                "pattern": "^[A-Za-z\\s]+$"
            }
        },
        "accountBalance": {
            "type": "decimal",
            "source": "account-service",
            "endpoint": "/api/v1/accounts/{accountId}/balance",
            "method": "GET",
            "jsonPath": "$.currentBalance",
            "required": true,
            "transformation": "formatCurrency",
            "cacheableFor": 300,
            "fallback": {
                "source": "account-cache-service",
                "endpoint": "/cache/accounts/{accountId}"
            }
        },
        "transactionHistory": {
            "type": "array",
            "source": "transaction-service",
            "endpoint": "/api/v1/accounts/{accountId}/transactions",
            "method": "GET",
            "queryParams": {
                "limit": 10,
                "startDate": "{statementStartDate}",
                "endDate": "{statementEndDate}"
            },
            "jsonPath": "$.transactions",
            "required": true,
            "itemSchema": {
                "type": "object",
                "properties": {
                    "date": "string",
                    "amount": "decimal",
                    "merchant": "string"
                }
            }
        },
        "disclosureContent": {
            "type": "string",
            "source": "disclosure-service",
            "endpoint": "/api/v1/disclosures/{disclosureCode}/content",
            "method": "GET",
            "jsonPath": "$.content",
            "required": false,
            "condition": {
                "field": "isRegulatory",
                "operator": "equals",
                "value": true
            }
        }
    }
    */

    -- Data Extraction Configuration
    data_extraction_config JSONB,
    /*
    Structure:
    {
        "parser": "NLP",
        "rules": [
            {"field": "totalAmount", "pattern": "Total: \\$([0-9,]+\\.[0-9]{2})", "type": "currency"},
            {"field": "dueDate", "pattern": "Due Date: (\\d{2}/\\d{2}/\\d{4})", "type": "date"}
        ],
        "confidence_threshold": 0.85
    }
    */

    -- Audit Fields (DBA Required)
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    archive_indicator BOOLEAN DEFAULT FALSE,
    version_number BIGINT DEFAULT 1,

    -- Constraints
    PRIMARY KEY (template_id, version),

    CONSTRAINT chk_template_status
        CHECK (template_status IN ('DRAFT', 'APPROVED', 'REJECTED', 'PENDING', 'ARCHIVED')),

    CONSTRAINT chk_valid_date_range
        CHECK (valid_until IS NULL OR valid_until > effective_date),

    CONSTRAINT chk_line_of_business
        CHECK (line_of_business IN ('credit_card', 'enterprise', 'digital_banking')),

    CONSTRAINT chk_sharing_scope
        CHECK (sharing_scope IN ('all', 'credit_card_accounts_only',
                'digital_bank_customer_only', 'enterprise_customer_only', 'custom_rule'))
);

-- ========================================
-- Standard B-tree Indexes
-- ========================================
CREATE INDEX idx_template_name ON master_template_definition(template_name);
CREATE INDEX idx_template_effective_date ON master_template_definition(effective_date);
CREATE INDEX idx_template_status ON master_template_definition(template_status);
CREATE INDEX idx_template_doc_type ON master_template_definition(doc_type);
CREATE INDEX idx_template_lob ON master_template_definition(line_of_business);
CREATE INDEX idx_template_category ON master_template_definition(category_code);
CREATE UNIQUE INDEX idx_unique_template_name ON master_template_definition(template_name, version);

-- Active templates (most common query)
CREATE INDEX idx_template_active ON master_template_definition(template_id, version)
    WHERE archive_indicator = FALSE
      AND template_status = 'APPROVED'
      AND (valid_until IS NULL OR valid_until > CURRENT_TIMESTAMP);

-- ========================================
-- JSONB GIN Indexes (for fast JSONB queries)
-- ========================================

-- Index for access control queries (e.g., "which templates can 'customer' role access?")
CREATE INDEX idx_template_access_control ON master_template_definition USING GIN (access_control);

-- Index for channel queries (e.g., "which templates support EMAIL?")
CREATE INDEX idx_template_channels ON master_template_definition USING GIN (channels);

-- Index for required fields queries
CREATE INDEX idx_template_required_fields ON master_template_definition USING GIN (required_fields);

-- Index for template variables queries
CREATE INDEX idx_template_variables ON master_template_definition USING GIN (template_variables);

-- ========================================
-- JSONB Expression Indexes (for specific queries)
-- ========================================

-- Fast lookup: Does this template have EMAIL channel enabled?
CREATE INDEX idx_template_has_email ON master_template_definition ((channels @> '[{"type":"EMAIL","enabled":true}]'))
    WHERE channels @> '[{"type":"EMAIL","enabled":true}]';

-- Fast lookup: Does this template allow 'customer' role to 'View'?
CREATE INDEX idx_template_customer_view ON master_template_definition
    ((access_control @> '[{"role":"customer","actions":["View"]}]'))
    WHERE access_control @> '[{"role":"customer","actions":["View"]}]';

COMMENT ON TABLE master_template_definition IS 'Master table storing document template definitions with versioning support (denormalized for performance)';
COMMENT ON COLUMN master_template_definition.access_control IS 'JSONB array of role-based access control rules';
COMMENT ON COLUMN master_template_definition.required_fields IS 'JSONB array of required fields with validation rules';
COMMENT ON COLUMN master_template_definition.channels IS 'JSONB array of delivery channels with configuration';
COMMENT ON COLUMN master_template_definition.template_variables IS 'JSONB object mapping variable names to their data sources';
```

---

### 2. Storage Index (Fully Denormalized)

**Purpose:** Index for stored documents with denormalized template data for zero-join queries.

```sql
CREATE TABLE storage_index (
    -- Primary Key
    storage_index_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Template Reference (with version)
    template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,

    -- ========================================
    -- Denormalized Template Data (for zero-join queries)
    -- ========================================
    template_name VARCHAR(255) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(100) NOT NULL,
    line_of_business VARCHAR(50) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    is_regulatory BOOLEAN DEFAULT FALSE,

    -- Storage Information
    storage_vendor VARCHAR(50) NOT NULL DEFAULT 'ECMS',
    storage_document_key UUID NOT NULL UNIQUE,
    file_name VARCHAR(500),
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    file_hash VARCHAR(64), -- SHA-256 hash for integrity

    -- Reference Keys (flexible key system)
    reference_key VARCHAR(255),
    reference_key_type VARCHAR(50),

    -- Customer/Account Information
    account_id UUID,
    customer_id UUID,

    -- Document Lifecycle
    doc_creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_accessible BOOLEAN DEFAULT TRUE,
    last_accessed_at TIMESTAMPTZ,
    access_count INTEGER DEFAULT 0,

    -- ========================================
    -- JSONB Fields (Document-Specific Data)
    -- ========================================

    -- Document Metadata (flexible for different doc types)
    doc_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    /*
    Structure (varies by doc_type):

    For MONTHLY_STATEMENT:
    {
        "statement_id": "STMT-2024-01-12345",
        "cycle_date": "2024-01-31",
        "statement_period": {"start": "2024-01-01", "end": "2024-01-31"},
        "total_amount": 1234.56,
        "due_date": "2024-02-15",
        "minimum_payment": 25.00
    }

    For DISCLOSURE:
    {
        "disclosure_code": "DISC-001",
        "disclosure_version": 3,
        "effective_date": "2024-01-01",
        "pricing_code": "STANDARD",
        "apr": 18.99
    }

    For TRANSACTION_CONFIRMATION:
    {
        "transaction_id": "TXN-20240115-001",
        "transaction_date": "2024-01-15",
        "amount": 99.99,
        "merchant": "Amazon"
    }
    */

    -- Access Control (denormalized from template, can be overridden per document)
    access_control JSONB,
    /*
    Structure (same as template):
    [
        {"role": "customer", "actions": ["View", "Download"]},
        {"role": "agent", "actions": ["View"]}
    ]
    */

    -- Retention and Compliance
    retention_until TIMESTAMPTZ,
    compliance_tags JSONB,
    /*
    {
        "data_classification": "CONFIDENTIAL",
        "contains_pii": true,
        "gdpr_applicable": true,
        "retention_policy": "7_years",
        "encryption_required": true
    }
    */

    -- Audit Fields (DBA Required)
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    archive_indicator BOOLEAN DEFAULT FALSE,
    version_number BIGINT DEFAULT 1,

    -- Constraints
    CONSTRAINT fk_template_reference
        FOREIGN KEY (template_id, template_version)
        REFERENCES master_template_definition(template_id, version)
        ON DELETE RESTRICT,

    CONSTRAINT chk_storage_vendor
        CHECK (storage_vendor IN ('ECMS', 'S3', 'AZURE_BLOB', 'GCP_STORAGE')),

    CONSTRAINT chk_reference_key_type
        CHECK (reference_key_type IN ('account_id', 'disclosure_code', 'thread_id',
                'correlation_id', 'statement_id', 'transaction_id', 'customer_id'))
);

-- ========================================
-- Standard B-tree Indexes
-- ========================================
CREATE INDEX idx_storage_template ON storage_index(template_id, template_version);
CREATE INDEX idx_storage_template_name ON storage_index(template_name);
CREATE INDEX idx_storage_account ON storage_index(account_id) WHERE account_id IS NOT NULL;
CREATE INDEX idx_storage_customer ON storage_index(customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_storage_reference ON storage_index(reference_key_type, reference_key);
CREATE INDEX idx_storage_creation_date ON storage_index(doc_creation_date DESC);
CREATE INDEX idx_storage_doc_type ON storage_index(doc_type);
CREATE INDEX idx_storage_category ON storage_index(category_code);
CREATE INDEX idx_storage_lob ON storage_index(line_of_business);

-- Composite index for most common query: account + date range
CREATE INDEX idx_storage_account_date ON storage_index(account_id, doc_creation_date DESC)
    WHERE is_accessible = TRUE AND archive_indicator = FALSE;

-- Composite index for customer documents
CREATE INDEX idx_storage_customer_date ON storage_index(customer_id, doc_creation_date DESC)
    WHERE is_accessible = TRUE AND archive_indicator = FALSE;

-- Accessible documents only (hot data)
CREATE INDEX idx_storage_accessible ON storage_index(storage_index_id)
    WHERE is_accessible = TRUE AND archive_indicator = FALSE;

-- Regulatory documents requiring special handling
CREATE INDEX idx_storage_regulatory ON storage_index(doc_creation_date DESC)
    WHERE is_regulatory = TRUE AND archive_indicator = FALSE;

-- ========================================
-- JSONB GIN Indexes
-- ========================================
CREATE INDEX idx_storage_metadata ON storage_index USING GIN (doc_metadata);
CREATE INDEX idx_storage_compliance ON storage_index USING GIN (compliance_tags);
CREATE INDEX idx_storage_access ON storage_index USING GIN (access_control);

-- ========================================
-- JSONB Expression Indexes (for common queries)
-- ========================================

-- Fast lookup by statement_id in metadata
CREATE INDEX idx_storage_statement_id ON storage_index
    ((doc_metadata->>'statement_id'))
    WHERE doc_type = 'monthly_statement';

-- Fast lookup by disclosure_code in metadata
CREATE INDEX idx_storage_disclosure_code ON storage_index
    ((doc_metadata->>'disclosure_code'))
    WHERE doc_type = 'disclosure';

-- Fast lookup by transaction_id in metadata
CREATE INDEX idx_storage_transaction_id ON storage_index
    ((doc_metadata->>'transaction_id'))
    WHERE doc_type LIKE '%transaction%';

-- Fast lookup for PII documents
CREATE INDEX idx_storage_pii ON storage_index
    ((compliance_tags->>'contains_pii'))
    WHERE (compliance_tags->>'contains_pii')::boolean = true;

COMMENT ON TABLE storage_index IS 'Index for stored documents with denormalized template data for zero-join queries';
COMMENT ON COLUMN storage_index.doc_metadata IS 'Document-specific metadata (structure varies by doc_type)';
COMMENT ON COLUMN storage_index.compliance_tags IS 'Compliance and data classification metadata';
```

---

### 3. Template Vendor Mapping (Denormalized)

**Purpose:** Maps templates to vendor implementations with denormalized template data.

```sql
CREATE TABLE template_vendor_mapping (
    -- Primary Key
    template_vendor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Template Reference (with version)
    template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,

    -- ========================================
    -- Denormalized Template Data
    -- ========================================
    template_name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,

    -- Vendor Information
    vendor VARCHAR(50) NOT NULL,
    vendor_template_key VARCHAR(255) NOT NULL,
    vendor_template_name VARCHAR(255),
    api_endpoint VARCHAR(1000),

    -- Template Content Reference (NOT blob storage)
    template_content_uri VARCHAR(1000), -- URI/path to ECMS or external storage
    template_content_hash VARCHAR(64), -- SHA-256 hash for integrity

    -- Version Management
    vendor_version VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    is_primary BOOLEAN DEFAULT FALSE,

    -- Lifecycle
    effective_date TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,

    -- ========================================
    -- JSONB Fields
    -- ========================================

    -- Vendor Schema Definition
    schema_info JSONB NOT NULL DEFAULT '{}'::jsonb,
    /*
    Structure:
    {
        "input_schema": {
            "customerData": {"type": "object", "required": ["name", "address"]},
            "accountData": {"type": "object", "required": ["accountNumber", "balance"]}
        },
        "output_format": "PDF",
        "supported_languages": ["en_us", "es_us"],
        "max_page_count": 50
    }
    */

    -- Template Fields (Required for Document Generation)
    template_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    /*
    Structure - Array of field definitions with their data sources:
    [
        {
            "fieldName": "customerName",
            "fieldType": "string",
            "vendorFieldName": "Customer.Name",
            "required": true,
            "dataSource": {
                "serviceName": "customer-service",
                "serviceUrl": "http://customer-service:8080",
                "endpoint": "/api/v1/customers/{customerId}",
                "method": "GET",
                "headers": {
                    "Authorization": "Bearer {token}",
                    "X-Request-ID": "{requestId}"
                },
                "jsonPath": "$.data.fullName",
                "timeout": 5000,
                "retryPolicy": {
                    "maxAttempts": 3,
                    "backoffMs": 1000
                }
            },
            "transformation": {
                "type": "uppercase",
                "maxLength": 100
            },
            "defaultValue": "VALUED CUSTOMER",
            "validationRules": {
                "minLength": 1,
                "maxLength": 100,
                "pattern": "^[A-Za-z\\s]+$"
            }
        },
        {
            "fieldName": "accountNumber",
            "fieldType": "string",
            "vendorFieldName": "Account.Number",
            "required": true,
            "dataSource": {
                "serviceName": "account-service",
                "serviceUrl": "http://account-service:8080",
                "endpoint": "/api/v1/accounts/{accountId}",
                "method": "GET",
                "jsonPath": "$.accountNumber",
                "timeout": 5000,
                "cacheConfig": {
                    "enabled": true,
                    "ttlSeconds": 300,
                    "cacheKey": "account:{accountId}"
                }
            },
            "transformation": {
                "type": "mask",
                "maskPattern": "XXXX-XXXX-XXXX-{last4}"
            }
        },
        {
            "fieldName": "currentBalance",
            "fieldType": "decimal",
            "vendorFieldName": "Account.Balance.Current",
            "required": true,
            "dataSource": {
                "serviceName": "account-service",
                "serviceUrl": "http://account-service:8080",
                "endpoint": "/api/v1/accounts/{accountId}/balance",
                "method": "GET",
                "jsonPath": "$.currentBalance",
                "timeout": 5000,
                "fallbackChain": [
                    {
                        "serviceName": "account-cache-service",
                        "endpoint": "/cache/accounts/{accountId}/balance"
                    },
                    {
                        "serviceName": "core-banking-service",
                        "endpoint": "/legacy/balance/{accountId}"
                    }
                ]
            },
            "transformation": {
                "type": "formatCurrency",
                "locale": "en_US",
                "currencyCode": "USD"
            },
            "validationRules": {
                "min": -999999.99,
                "max": 999999.99
            }
        },
        {
            "fieldName": "transactions",
            "fieldType": "array",
            "vendorFieldName": "Account.Transactions",
            "required": true,
            "dataSource": {
                "serviceName": "transaction-service",
                "serviceUrl": "http://transaction-service:8080",
                "endpoint": "/api/v1/accounts/{accountId}/transactions",
                "method": "GET",
                "queryParams": {
                    "startDate": "{statementStartDate}",
                    "endDate": "{statementEndDate}",
                    "limit": "100",
                    "includeDeclined": "false"
                },
                "jsonPath": "$.transactions",
                "timeout": 10000,
                "pagination": {
                    "enabled": true,
                    "pageSize": 100,
                    "maxPages": 10
                }
            },
            "transformation": {
                "type": "mapArray",
                "mapping": {
                    "date": "transactionDate",
                    "amount": "transactionAmount",
                    "description": "merchantName"
                },
                "sortBy": "date",
                "sortOrder": "desc"
            },
            "itemSchema": {
                "type": "object",
                "properties": {
                    "date": {"type": "string", "format": "date"},
                    "amount": {"type": "decimal"},
                    "description": {"type": "string"}
                }
            }
        },
        {
            "fieldName": "disclosureText",
            "fieldType": "string",
            "vendorFieldName": "Disclosure.Content",
            "required": false,
            "conditionalInclusion": {
                "condition": "isRegulatory == true",
                "requiredWhen": true
            },
            "dataSource": {
                "serviceName": "disclosure-service",
                "serviceUrl": "http://disclosure-service:8080",
                "endpoint": "/api/v1/disclosures/{disclosureCode}/content",
                "method": "POST",
                "headers": {
                    "Content-Type": "application/json"
                },
                "requestBody": {
                    "disclosureCode": "{disclosureCode}",
                    "pricingCode": "{pricingCode}",
                    "effectiveDate": "{statementDate}",
                    "language": "{languageCode}"
                },
                "jsonPath": "$.disclosureContent.text",
                "timeout": 8000
            }
        },
        {
            "fieldName": "customerAddress",
            "fieldType": "object",
            "vendorFieldName": "Customer.MailingAddress",
            "required": true,
            "dataSource": {
                "serviceName": "customer-service",
                "serviceUrl": "http://customer-service:8080",
                "endpoint": "/api/v1/customers/{customerId}/addresses",
                "method": "GET",
                "queryParams": {
                    "type": "MAILING"
                },
                "jsonPath": "$.addresses[?(@.type=='MAILING')]",
                "timeout": 5000
            },
            "transformation": {
                "type": "formatAddress",
                "format": "US_POSTAL"
            },
            "objectSchema": {
                "street1": "string",
                "street2": "string",
                "city": "string",
                "state": "string",
                "zipCode": "string"
            }
        },
        {
            "fieldName": "apr",
            "fieldType": "decimal",
            "vendorFieldName": "Account.APR",
            "required": false,
            "conditionalInclusion": {
                "condition": "docType == 'monthly_statement' || docType == 'disclosure'"
            },
            "dataSource": {
                "serviceName": "pricing-service",
                "serviceUrl": "http://pricing-service:8080",
                "endpoint": "/api/v1/pricing/{pricingCode}/apr",
                "method": "GET",
                "queryParams": {
                    "effectiveDate": "{statementDate}"
                },
                "jsonPath": "$.purchaseAPR",
                "timeout": 5000
            },
            "transformation": {
                "type": "formatPercentage",
                "decimalPlaces": 2
            }
        },
        {
            "fieldName": "staticContent",
            "fieldType": "string",
            "vendorFieldName": "Template.StaticContent",
            "required": false,
            "dataSource": {
                "serviceName": "content-management-service",
                "serviceUrl": "http://cms-service:8080",
                "endpoint": "/api/v1/content/{contentId}",
                "method": "GET",
                "jsonPath": "$.content.html",
                "timeout": 3000,
                "cacheConfig": {
                    "enabled": true,
                    "ttlSeconds": 3600,
                    "cacheKey": "content:{contentId}"
                }
            }
        }
    ]
    */

    -- Vendor-Specific Configuration
    vendor_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    /*
    Structure (varies by vendor):

    For SMARTCOMM:
    {
        "project_id": "PROJECT-123",
        "template_type": "interactive",
        "batch_enabled": true,
        "output_options": {
            "pdf_version": "1.7",
            "compression": true
        }
    }

    For ASSENTIS:
    {
        "workflow_id": "WF-456",
        "approval_required": true,
        "branding": {
            "logo_url": "https://...",
            "color_scheme": "#003366"
        }
    }
    */

    -- API Configuration
    api_config JSONB,
    /*
    {
        "base_url": "https://vendor-api.com/v1",
        "auth_type": "oauth2",
        "timeout_seconds": 30,
        "retry_policy": {
            "max_attempts": 3,
            "backoff": "exponential"
        },
        "headers": {
            "X-Client-ID": "client-123"
        }
    }
    */

    -- Consumer Tracking (for Kafka consumers)
    consumer_id UUID,
    last_sync_at TIMESTAMPTZ,
    sync_status JSONB,
    /*
    {
        "last_successful_sync": "2024-01-15T10:30:00Z",
        "last_error": null,
        "sync_count": 150,
        "last_sync_duration_ms": 1234
    }
    */

    -- Audit Fields (DBA Required)
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    archive_indicator BOOLEAN DEFAULT FALSE,
    version_number BIGINT DEFAULT 1,

    -- Constraints
    CONSTRAINT fk_vendor_template
        FOREIGN KEY (template_id, template_version)
        REFERENCES master_template_definition(template_id, version)
        ON DELETE RESTRICT,

    CONSTRAINT chk_vendor
        CHECK (vendor IN ('SMARTCOMM', 'ASSENTIS', 'HANDLEBAR', 'CUSTOM')),

    CONSTRAINT chk_vendor_dates
        CHECK (valid_until IS NULL OR valid_until > effective_date),

    UNIQUE (template_id, template_version, vendor)
);

-- ========================================
-- Standard B-tree Indexes
-- ========================================
CREATE INDEX idx_vendor_template ON template_vendor_mapping(template_id, template_version);
CREATE INDEX idx_vendor_template_name ON template_vendor_mapping(template_name);
CREATE INDEX idx_vendor_type ON template_vendor_mapping(vendor);
CREATE INDEX idx_vendor_doc_type ON template_vendor_mapping(doc_type);
CREATE INDEX idx_vendor_key ON template_vendor_mapping(vendor_template_key);

-- Active vendors only
CREATE INDEX idx_vendor_active ON template_vendor_mapping(template_id, template_version)
    WHERE is_active = TRUE AND archive_indicator = FALSE;

-- Primary vendor lookup (most common)
CREATE INDEX idx_vendor_primary ON template_vendor_mapping(template_id, template_version)
    WHERE is_primary = TRUE AND is_active = TRUE;

-- Ensure only one primary vendor per template version
CREATE UNIQUE INDEX idx_unique_primary_vendor
    ON template_vendor_mapping(template_id, template_version)
    WHERE is_primary = TRUE;

-- ========================================
-- JSONB GIN Indexes
-- ========================================
CREATE INDEX idx_vendor_schema ON template_vendor_mapping USING GIN (schema_info);
CREATE INDEX idx_vendor_config ON template_vendor_mapping USING GIN (vendor_config);
CREATE INDEX idx_vendor_api_config ON template_vendor_mapping USING GIN (api_config);
CREATE INDEX idx_vendor_template_fields ON template_vendor_mapping USING GIN (template_fields);

COMMENT ON TABLE template_vendor_mapping IS 'Maps templates to vendor implementations with denormalized data for zero-join queries';
COMMENT ON COLUMN template_vendor_mapping.template_content_uri IS 'URI/path to template content in ECMS (NOT stored as BLOB)';
COMMENT ON COLUMN template_vendor_mapping.template_fields IS 'Array of field definitions with data sources from multiple services';
```

---

## Helper Functions

### Function: Get Active Template Version (No Joins)

```sql
CREATE OR REPLACE FUNCTION get_active_template_version(
    p_template_id UUID,
    p_reference_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
)
RETURNS TABLE (
    template_id UUID,
    version INTEGER,
    template_name VARCHAR,
    category_code VARCHAR,
    category_name VARCHAR,
    doc_type VARCHAR,
    access_control JSONB,
    channels JSONB,
    template_variables JSONB
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        mtd.template_id,
        mtd.version,
        mtd.template_name,
        mtd.category_code,
        mtd.category_name,
        mtd.doc_type,
        mtd.access_control,
        mtd.channels,
        mtd.template_variables
    FROM master_template_definition mtd
    WHERE mtd.template_id = p_template_id
      AND mtd.effective_date <= p_reference_date
      AND (mtd.valid_until IS NULL OR mtd.valid_until > p_reference_date)
      AND mtd.template_status = 'APPROVED'
      AND mtd.archive_indicator = FALSE
    ORDER BY mtd.version DESC
    LIMIT 1;
END;
$$;

COMMENT ON FUNCTION get_active_template_version IS 'Returns active template version with all data (no joins required)';
```

### Function: Update Timestamp Trigger

```sql
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version_number = OLD.version_number + 1;
    RETURN NEW;
END;
$$;

-- Apply to all tables
CREATE TRIGGER trg_update_timestamp_master_template
    BEFORE UPDATE ON master_template_definition
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_update_timestamp_storage_index
    BEFORE UPDATE ON storage_index
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_update_timestamp_vendor_mapping
    BEFORE UPDATE ON template_vendor_mapping
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();
```

### Function: Sync Denormalized Data (When Template Changes)

```sql
CREATE OR REPLACE FUNCTION sync_denormalized_template_data()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- When template metadata changes, update storage_index denormalized fields
    -- Only update if specific fields changed
    IF (TG_OP = 'UPDATE' AND (
        OLD.template_name IS DISTINCT FROM NEW.template_name OR
        OLD.category_code IS DISTINCT FROM NEW.category_code OR
        OLD.category_name IS DISTINCT FROM NEW.category_name OR
        OLD.doc_type IS DISTINCT FROM NEW.doc_type OR
        OLD.line_of_business IS DISTINCT FROM NEW.line_of_business OR
        OLD.is_regulatory IS DISTINCT FROM NEW.is_regulatory
    )) THEN
        UPDATE storage_index
        SET
            template_name = NEW.template_name,
            category_code = NEW.category_code,
            category_name = NEW.category_name,
            doc_type = NEW.doc_type,
            line_of_business = NEW.line_of_business,
            is_regulatory = NEW.is_regulatory,
            updated_by = NEW.updated_by,
            updated_at = CURRENT_TIMESTAMP
        WHERE template_id = NEW.template_id
          AND template_version = NEW.version;

        -- Also update vendor mapping
        UPDATE template_vendor_mapping
        SET
            template_name = NEW.template_name,
            doc_type = NEW.doc_type,
            category_code = NEW.category_code,
            updated_by = NEW.updated_by,
            updated_at = CURRENT_TIMESTAMP
        WHERE template_id = NEW.template_id
          AND template_version = NEW.version;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sync_denormalized_data
    AFTER INSERT OR UPDATE ON master_template_definition
    FOR EACH ROW
    EXECUTE FUNCTION sync_denormalized_template_data();

COMMENT ON FUNCTION sync_denormalized_template_data IS 'Keeps denormalized template data in sync across tables';
```

### Function: Validate Primary Vendor

```sql
CREATE OR REPLACE FUNCTION validate_primary_vendor()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    primary_count INTEGER;
BEGIN
    IF NEW.is_primary = TRUE THEN
        SELECT COUNT(*) INTO primary_count
        FROM template_vendor_mapping
        WHERE template_id = NEW.template_id
          AND template_version = NEW.template_version
          AND is_primary = TRUE
          AND template_vendor_id != COALESCE(NEW.template_vendor_id, '00000000-0000-0000-0000-000000000000'::uuid);

        IF primary_count > 0 THEN
            RAISE EXCEPTION 'Only one primary vendor allowed per template version';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_primary_vendor
    BEFORE INSERT OR UPDATE ON template_vendor_mapping
    FOR EACH ROW
    EXECUTE FUNCTION validate_primary_vendor();
```

---

## Helper Functions for Template Fields

### Function: Get Required Fields with Data Sources

```sql
CREATE OR REPLACE FUNCTION get_template_required_fields(
    p_template_id UUID,
    p_template_version INTEGER
)
RETURNS TABLE (
    field_name TEXT,
    field_type TEXT,
    vendor_field_name TEXT,
    service_name TEXT,
    endpoint TEXT,
    method TEXT,
    is_required BOOLEAN,
    has_fallback BOOLEAN
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        field->>'fieldName',
        field->>'fieldType',
        field->>'vendorFieldName',
        field->'dataSource'->>'serviceName',
        field->'dataSource'->>'endpoint',
        field->'dataSource'->>'method',
        (field->>'required')::boolean,
        (field->'dataSource'->'fallbackChain' IS NOT NULL) as has_fallback
    FROM template_vendor_mapping,
         jsonb_array_elements(template_fields) as field
    WHERE template_id = p_template_id
      AND template_version = p_template_version
      AND is_primary = TRUE
      AND is_active = TRUE;
END;
$$;

COMMENT ON FUNCTION get_template_required_fields IS 'Returns all required fields and their data sources for a template';
```

### Function: Get Fields by Service

```sql
CREATE OR REPLACE FUNCTION get_fields_by_service(
    p_template_id UUID,
    p_template_version INTEGER,
    p_service_name TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    result JSONB;
BEGIN
    SELECT jsonb_agg(field)
    INTO result
    FROM template_vendor_mapping,
         jsonb_array_elements(template_fields) as field
    WHERE template_id = p_template_id
      AND template_version = p_template_version
      AND is_primary = TRUE
      AND field->'dataSource'->>'serviceName' = p_service_name;

    RETURN COALESCE(result, '[]'::jsonb);
END;
$$;

COMMENT ON FUNCTION get_fields_by_service IS 'Returns all fields that use a specific service as data source';
```

### Function: Validate Required Fields Available

```sql
CREATE OR REPLACE FUNCTION validate_template_data_sources(
    p_template_id UUID,
    p_template_version INTEGER
)
RETURNS TABLE (
    field_name TEXT,
    service_name TEXT,
    endpoint TEXT,
    has_fallback BOOLEAN,
    validation_status TEXT
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        field->>'fieldName',
        field->'dataSource'->>'serviceName',
        field->'dataSource'->>'endpoint',
        (field->'dataSource'->'fallbackChain' IS NOT NULL),
        CASE
            WHEN field->>'required' = 'true'
                AND field->'dataSource'->>'serviceName' IS NULL
            THEN 'MISSING_DATA_SOURCE'
            WHEN field->>'required' = 'true'
                AND field->'dataSource'->'fallbackChain' IS NULL
            THEN 'NO_FALLBACK'
            WHEN field->'dataSource'->>'timeout' IS NULL
            THEN 'NO_TIMEOUT_CONFIGURED'
            ELSE 'OK'
        END as validation_status
    FROM template_vendor_mapping,
         jsonb_array_elements(template_fields) as field
    WHERE template_id = p_template_id
      AND template_version = p_template_version
      AND is_primary = TRUE;
END;
$$;

COMMENT ON FUNCTION validate_template_data_sources IS 'Validates that all required fields have proper data source configuration';
```

---

## Common Queries (Zero Joins!)

### 1. Get All Documents for an Account

```sql
-- Zero joins! All data in one table
SELECT
    storage_index_id,
    template_name,
    category_name,
    doc_type,
    file_name,
    doc_creation_date,
    storage_document_key,
    doc_metadata,
    access_control
FROM storage_index
WHERE account_id = 'account-uuid-here'
  AND is_accessible = TRUE
  AND archive_indicator = FALSE
ORDER BY doc_creation_date DESC
LIMIT 50;
```

### 2. Get Statements for Account in Date Range

```sql
-- Zero joins! Query with JSONB filtering
SELECT
    storage_index_id,
    template_name,
    file_name,
    doc_creation_date,
    doc_metadata->>'statement_id' as statement_id,
    doc_metadata->>'cycle_date' as cycle_date,
    (doc_metadata->>'total_amount')::numeric as total_amount,
    storage_document_key
FROM storage_index
WHERE account_id = 'account-uuid-here'
  AND doc_type = 'monthly_statement'
  AND doc_creation_date BETWEEN '2024-01-01' AND '2024-12-31'
  AND is_accessible = TRUE
ORDER BY doc_creation_date DESC;
```

### 3. Get Active Template with All Details

```sql
-- Zero joins! All data in one row
SELECT
    template_id,
    version,
    template_name,
    category_code,
    category_name,
    doc_type,
    line_of_business,
    access_control,
    channels,
    template_variables,
    required_fields,
    data_extraction_config,
    effective_date,
    valid_until
FROM master_template_definition
WHERE template_id = 'template-uuid-here'
  AND effective_date <= CURRENT_TIMESTAMP
  AND (valid_until IS NULL OR valid_until > CURRENT_TIMESTAMP)
  AND template_status = 'APPROVED'
  AND archive_indicator = FALSE
ORDER BY version DESC
LIMIT 1;
```

### 4. Get Primary Vendor for Template

```sql
-- Zero joins!
SELECT
    template_vendor_id,
    vendor,
    vendor_template_key,
    api_endpoint,
    template_content_uri,
    schema_info,
    vendor_config,
    api_config
FROM template_vendor_mapping
WHERE template_id = 'template-uuid-here'
  AND template_version = 1
  AND is_primary = TRUE
  AND is_active = TRUE;
```

### 5. Check User Access to Document

```sql
-- Zero joins! Check access control in JSONB
SELECT
    storage_index_id,
    template_name,
    file_name,
    storage_document_key
FROM storage_index
WHERE storage_index_id = 'document-uuid-here'
  AND is_accessible = TRUE
  AND access_control @> '[{"role": "customer", "actions": ["View"]}]'::jsonb;
```

### 6. Get All Email-Enabled Templates

```sql
-- Zero joins! Query JSONB directly
SELECT
    template_id,
    version,
    template_name,
    doc_type,
    channels
FROM master_template_definition
WHERE channels @> '[{"type": "EMAIL", "enabled": true}]'::jsonb
  AND template_status = 'APPROVED'
  AND archive_indicator = FALSE;
```

### 7. Get Regulatory Documents for Customer

```sql
-- Zero joins!
SELECT
    storage_index_id,
    template_name,
    category_name,
    file_name,
    doc_creation_date,
    doc_metadata->>'disclosure_code' as disclosure_code,
    storage_document_key
FROM storage_index
WHERE customer_id = 'customer-uuid-here'
  AND is_regulatory = TRUE
  AND is_accessible = TRUE
  AND archive_indicator = FALSE
ORDER BY doc_creation_date DESC;
```

### 8. Get Documents with PII for GDPR Compliance

```sql
-- Zero joins! Query compliance tags
SELECT
    storage_index_id,
    template_name,
    file_name,
    account_id,
    customer_id,
    doc_creation_date,
    compliance_tags->>'data_classification' as classification,
    retention_until
FROM storage_index
WHERE compliance_tags->>'contains_pii' = 'true'
  AND compliance_tags->>'gdpr_applicable' = 'true'
  AND archive_indicator = FALSE
ORDER BY retention_until ASC;
```

---

## JSONB Query Examples

### Query Access Control

```sql
-- Find all templates where 'admin' role can 'Delete'
SELECT template_id, version, template_name
FROM master_template_definition
WHERE access_control @> '[{"role": "admin", "actions": ["Delete"]}]'::jsonb;

-- Find templates accessible by customer role
SELECT template_id, version, template_name
FROM master_template_definition
WHERE access_control @@ '$.* ? (@.role == "customer")';
```

### Query Channels

```sql
-- Find templates with PRINT channel enabled
SELECT template_id, version, template_name
FROM master_template_definition
WHERE channels @> '[{"type": "PRINT", "enabled": true}]'::jsonb;

-- Get channel configuration for specific template
SELECT
    template_name,
    jsonb_array_elements(channels) as channel
FROM master_template_definition
WHERE template_id = 'uuid-here'
  AND version = 1;
```

### Query Template Variables

```sql
-- Find templates that use customer-service API
SELECT template_id, version, template_name
FROM master_template_definition
WHERE template_variables @@ '$.* ? (@.source == "customer-service")';

-- Get all required variables for a template
SELECT
    template_name,
    key as variable_name,
    value->>'type' as variable_type,
    value->>'required' as is_required,
    value->>'endpoint' as api_endpoint
FROM master_template_definition,
     jsonb_each(template_variables)
WHERE template_id = 'uuid-here'
  AND version = 1
  AND (value->>'required')::boolean = true;
```

### Query Document Metadata

```sql
-- Find statements with total amount > $1000
SELECT
    storage_index_id,
    template_name,
    file_name,
    doc_metadata->>'statement_id' as statement_id,
    (doc_metadata->>'total_amount')::numeric as total_amount
FROM storage_index
WHERE doc_type = 'monthly_statement'
  AND (doc_metadata->>'total_amount')::numeric > 1000
ORDER BY (doc_metadata->>'total_amount')::numeric DESC;

-- Find disclosures for specific pricing code
SELECT
    storage_index_id,
    template_name,
    doc_metadata->>'disclosure_code' as disclosure_code,
    doc_metadata->>'pricing_code' as pricing_code,
    doc_metadata->>'apr' as apr
FROM storage_index
WHERE doc_type = 'disclosure'
  AND doc_metadata->>'pricing_code' = 'PREMIUM';
```

### Query Template Fields

```sql
-- Get all required fields for a template
SELECT
    template_name,
    vendor,
    field->>'fieldName' as field_name,
    field->>'fieldType' as field_type,
    field->>'vendorFieldName' as vendor_field_name,
    field->'dataSource'->>'serviceName' as service_name,
    field->'dataSource'->>'endpoint' as endpoint
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE template_id = 'your-template-uuid'
  AND template_version = 1
  AND is_primary = TRUE
  AND (field->>'required')::boolean = true;

-- Get all fields that use customer-service
SELECT
    template_name,
    vendor,
    field->>'fieldName' as field_name,
    field->'dataSource'->>'endpoint' as endpoint,
    field->'dataSource'->>'jsonPath' as json_path
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE is_active = TRUE
  AND field->'dataSource'->>'serviceName' = 'customer-service';

-- Find templates that require transaction data
SELECT
    template_id,
    template_version,
    template_name,
    vendor,
    field->>'fieldName' as field_name,
    field->'dataSource'->>'endpoint' as endpoint
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE field->'dataSource'->>'serviceName' = 'transaction-service'
  AND is_active = TRUE;

-- Get fields with fallback configuration
SELECT
    template_name,
    vendor,
    field->>'fieldName' as field_name,
    field->'dataSource'->>'serviceName' as primary_service,
    jsonb_array_length(field->'dataSource'->'fallbackChain') as fallback_count,
    field->'dataSource'->'fallbackChain' as fallback_chain
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE template_id = 'your-template-uuid'
  AND template_version = 1
  AND field->'dataSource'->'fallbackChain' IS NOT NULL;

-- Find fields with caching enabled
SELECT
    template_name,
    field->>'fieldName' as field_name,
    field->'dataSource'->>'serviceName' as service_name,
    (field->'dataSource'->'cacheConfig'->>'ttlSeconds')::int as cache_ttl_seconds,
    field->'dataSource'->'cacheConfig'->>'cacheKey' as cache_key
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE (field->'dataSource'->'cacheConfig'->>'enabled')::boolean = true
  AND is_active = TRUE;

-- Get all services used by a template
SELECT DISTINCT
    field->'dataSource'->>'serviceName' as service_name,
    field->'dataSource'->>'serviceUrl' as service_url,
    COUNT(*) as field_count
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE template_id = 'your-template-uuid'
  AND template_version = 1
  AND is_primary = TRUE
GROUP BY field->'dataSource'->>'serviceName', field->'dataSource'->>'serviceUrl';

-- Get conditional fields (only included based on conditions)
SELECT
    template_name,
    field->>'fieldName' as field_name,
    field->'conditionalInclusion'->>'condition' as inclusion_condition,
    field->'dataSource'->>'serviceName' as service_name
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE template_id = 'your-template-uuid'
  AND template_version = 1
  AND field->'conditionalInclusion' IS NOT NULL;

-- Find fields that require specific transformations
SELECT
    template_name,
    field->>'fieldName' as field_name,
    field->'transformation'->>'type' as transformation_type,
    field->'transformation' as transformation_config
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE field->'transformation' IS NOT NULL
  AND is_active = TRUE
ORDER BY template_name, field->>'fieldName';

-- Get all POST endpoints (that send data to services)
SELECT
    template_name,
    field->>'fieldName' as field_name,
    field->'dataSource'->>'serviceName' as service_name,
    field->'dataSource'->>'endpoint' as endpoint,
    field->'dataSource'->'requestBody' as request_body
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE field->'dataSource'->>'method' = 'POST'
  AND is_active = TRUE;
```

---

## Performance Optimization Tips

### 1. JSONB Indexing Strategy

```sql
-- Use GIN indexes for containment queries (@>, @?, @@)
CREATE INDEX idx_name ON table_name USING GIN (jsonb_column);

-- Use expression indexes for specific field lookups
CREATE INDEX idx_name ON table_name ((jsonb_column->>'field_name'));

-- Use partial indexes for filtered queries
CREATE INDEX idx_name ON table_name ((jsonb_column->>'field'))
WHERE (jsonb_column->>'field') IS NOT NULL;
```

### 2. Query Performance

```sql
-- GOOD: Use containment operators with GIN index
WHERE channels @> '[{"type": "EMAIL"}]'::jsonb

-- BAD: Extracting JSON and comparing (can't use index)
WHERE channels::text LIKE '%EMAIL%'

-- GOOD: Use expression index
WHERE doc_metadata->>'statement_id' = 'STMT-123'

-- BAD: Converting entire JSONB to text
WHERE doc_metadata::text LIKE '%STMT-123%'
```

### 3. Partitioning for Large Tables

```sql
-- Partition storage_index by doc_creation_date (monthly)
CREATE TABLE storage_index (
    -- ... columns ...
) PARTITION BY RANGE (doc_creation_date);

-- Create partitions
CREATE TABLE storage_index_2024_01 PARTITION OF storage_index
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE storage_index_2024_02 PARTITION OF storage_index
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- Partitions improve query performance by scanning less data
```

### 4. Materialized Views for Common Queries

```sql
-- Create materialized view for frequently accessed account documents
CREATE MATERIALIZED VIEW mv_active_account_documents AS
SELECT
    account_id,
    storage_index_id,
    template_name,
    category_name,
    doc_type,
    file_name,
    doc_creation_date,
    storage_document_key,
    doc_metadata
FROM storage_index
WHERE is_accessible = TRUE
  AND archive_indicator = FALSE
  AND account_id IS NOT NULL;

-- Create indexes on materialized view
CREATE INDEX idx_mv_account ON mv_active_account_documents(account_id);
CREATE INDEX idx_mv_date ON mv_active_account_documents(doc_creation_date DESC);

-- Refresh strategy (scheduled job)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_active_account_documents;
```

---

## Data Consistency Strategy

### When to Sync Denormalized Data

The trigger `sync_denormalized_template_data()` automatically syncs when template metadata changes. However, you should also:

1. **Batch Sync Job**: Run nightly to catch any missed updates
```sql
-- Sync script
UPDATE storage_index si
SET
    template_name = mtd.template_name,
    category_code = mtd.category_code,
    category_name = mtd.category_name,
    doc_type = mtd.doc_type,
    line_of_business = mtd.line_of_business,
    is_regulatory = mtd.is_regulatory
FROM master_template_definition mtd
WHERE si.template_id = mtd.template_id
  AND si.template_version = mtd.version
  AND (
      si.template_name != mtd.template_name OR
      si.category_code != mtd.category_code OR
      si.category_name != mtd.category_name OR
      si.doc_type != mtd.doc_type OR
      si.line_of_business != mtd.line_of_business OR
      si.is_regulatory != mtd.is_regulatory
  );
```

2. **Version Control**: Never update template metadata for approved versions - always create new version

3. **Audit Trail**: Track all changes to detect inconsistencies

---

## Migration from Original Schema

### Step 1: Add New Columns

```sql
-- Add denormalized columns to storage_index
ALTER TABLE storage_index
ADD COLUMN template_version INTEGER,
ADD COLUMN template_name VARCHAR(255),
ADD COLUMN category_code VARCHAR(50),
ADD COLUMN category_name VARCHAR(255),
ADD COLUMN line_of_business VARCHAR(50),
ADD COLUMN is_regulatory BOOLEAN DEFAULT FALSE;

-- Add JSONB columns to master_template_definition
ALTER TABLE master_template_definition
ADD COLUMN access_control JSONB DEFAULT '[]'::jsonb,
ADD COLUMN required_fields JSONB DEFAULT '[]'::jsonb,
ADD COLUMN channels JSONB DEFAULT '[]'::jsonb,
ADD COLUMN template_variables JSONB DEFAULT '{}'::jsonb;
```

### Step 2: Migrate Data

```sql
-- Migrate JSONB data from old doc_supporting_data to new columns
UPDATE master_template_definition
SET
    access_control = doc_supporting_data->'access_control',
    required_fields = doc_supporting_data->'required_fields',
    channels = document_channel;

-- Set default version for existing templates
UPDATE master_template_definition
SET version = 1
WHERE version IS NULL;

-- Populate denormalized fields in storage_index
UPDATE storage_index si
SET
    template_version = 1,
    template_name = mtd.template_name,
    category_code = mtd.category_code,
    category_name = mtd.category_name,
    line_of_business = mtd.line_of_business,
    is_regulatory = mtd.is_regulatory
FROM master_template_definition mtd
WHERE si.template_id = mtd.template_id;
```

### Step 3: Update Primary Key

```sql
-- Drop old PK and create composite PK
ALTER TABLE master_template_definition
DROP CONSTRAINT master_template_definition_pkey,
ADD PRIMARY KEY (template_id, version);
```

### Step 4: Update Foreign Keys

```sql
-- Update FK in storage_index
ALTER TABLE storage_index
DROP CONSTRAINT IF EXISTS fk_template_reference,
ADD CONSTRAINT fk_template_reference
    FOREIGN KEY (template_id, template_version)
    REFERENCES master_template_definition(template_id, version);
```

---

## Practical Usage Example: Document Generation Flow

### Complete Example: Generating a Monthly Statement

This example shows how to use the template fields to generate a document:

#### Step 1: Get Template and Field Configuration

```sql
-- Get the active template version and all required fields
SELECT
    tvm.template_id,
    tvm.template_version,
    tvm.template_name,
    tvm.vendor,
    tvm.vendor_template_key,
    tvm.api_endpoint,
    tvm.template_fields
FROM template_vendor_mapping tvm
WHERE tvm.template_name = 'Monthly Credit Card Statement'
  AND tvm.is_primary = TRUE
  AND tvm.is_active = TRUE
  AND tvm.effective_date <= CURRENT_TIMESTAMP
  AND (tvm.valid_until IS NULL OR tvm.valid_until > CURRENT_TIMESTAMP)
LIMIT 1;
```

**Result:**
```json
{
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "template_version": 2,
  "template_name": "Monthly Credit Card Statement",
  "vendor": "SMARTCOMM",
  "vendor_template_key": "STMT_CC_V2",
  "api_endpoint": "https://smartcomm-api.com/v1/generate",
  "template_fields": [/* array of field definitions */]
}
```

#### Step 2: Extract Required Services and Endpoints

```sql
-- Get unique services needed for this template
SELECT DISTINCT
    field->'dataSource'->>'serviceName' as service_name,
    field->'dataSource'->>'serviceUrl' as service_url,
    jsonb_agg(
        jsonb_build_object(
            'fieldName', field->>'fieldName',
            'endpoint', field->'dataSource'->>'endpoint',
            'method', field->'dataSource'->>'method',
            'required', field->>'required'
        )
    ) as fields_from_this_service
FROM template_vendor_mapping,
     jsonb_array_elements(template_fields) as field
WHERE template_id = '550e8400-e29b-41d4-a716-446655440000'
  AND template_version = 2
  AND is_primary = TRUE
GROUP BY field->'dataSource'->>'serviceName', field->'dataSource'->>'serviceUrl';
```

**Result:**
```
service_name          | service_url                        | fields_from_this_service
----------------------|------------------------------------|--------------------------
customer-service      | http://customer-service:8080       | [{"fieldName":"customerName",...}, {"fieldName":"customerAddress",...}]
account-service       | http://account-service:8080        | [{"fieldName":"accountNumber",...}, {"fieldName":"currentBalance",...}]
transaction-service   | http://transaction-service:8080    | [{"fieldName":"transactions",...}]
pricing-service       | http://pricing-service:8080        | [{"fieldName":"apr",...}]
disclosure-service    | http://disclosure-service:8080     | [{"fieldName":"disclosureText",...}]
```

#### Step 3: Application Code (Pseudo-code)

```java
// 1. Get template configuration from database
TemplateConfig config = getTemplateConfig("550e8400-e29b-41d4-a716-446655440000", 2);

// 2. Build data collection plan
Map<String, List<FieldDefinition>> serviceFieldMap = groupFieldsByService(config.getTemplateFields());

// 3. Collect data from all services (in parallel)
CompletableFuture<Map<String, Object>> dataCollection = CompletableFuture.supplyAsync(() -> {
    Map<String, Object> allData = new ConcurrentHashMap<>();

    serviceFieldMap.entrySet().parallelStream().forEach(entry -> {
        String serviceName = entry.getKey();
        List<FieldDefinition> fields = entry.getValue();

        for (FieldDefinition field : fields) {
            try {
                // Make API call to service
                Object data = callService(
                    field.getDataSource().getServiceUrl(),
                    field.getDataSource().getEndpoint(),
                    field.getDataSource().getMethod(),
                    field.getDataSource().getHeaders(),
                    accountId,
                    customerId
                );

                // Extract value using JSONPath
                Object value = extractJsonPath(data, field.getDataSource().getJsonPath());

                // Apply transformation if defined
                if (field.getTransformation() != null) {
                    value = applyTransformation(value, field.getTransformation());
                }

                // Store with vendor field name
                allData.put(field.getVendorFieldName(), value);

            } catch (Exception e) {
                // Try fallback if available
                if (field.getDataSource().getFallbackChain() != null) {
                    value = tryFallbackChain(field);
                    allData.put(field.getVendorFieldName(), value);
                } else if (field.getDefaultValue() != null) {
                    allData.put(field.getVendorFieldName(), field.getDefaultValue());
                } else if (field.isRequired()) {
                    throw new DataCollectionException("Failed to get required field: " + field.getFieldName());
                }
            }
        }
    });

    return allData;
});

// 4. Call vendor API to generate document
Map<String, Object> documentData = dataCollection.get();
byte[] generatedPdf = callVendorApi(
    config.getApiEndpoint(),
    config.getVendorTemplateKey(),
    documentData
);

// 5. Store document in ECMS and create storage index
UUID documentKey = storeInEcms(generatedPdf, "statement.pdf");

// 6. Insert into storage_index
insertStorageIndex(
    config.getTemplateId(),
    config.getTemplateVersion(),
    config.getTemplateName(),
    config.getCategoryCode(),
    config.getCategoryName(),
    accountId,
    customerId,
    documentKey,
    documentData  // Store as doc_metadata
);
```

#### Step 4: Insert Document Record

```sql
-- Insert storage index record with all denormalized data
INSERT INTO storage_index (
    storage_index_id,
    template_id,
    template_version,
    -- Denormalized template data
    template_name,
    category_code,
    category_name,
    doc_type,
    line_of_business,
    language_code,
    is_regulatory,
    -- Storage info
    storage_vendor,
    storage_document_key,
    file_name,
    file_size_bytes,
    mime_type,
    -- Customer/Account
    account_id,
    customer_id,
    -- Document metadata
    doc_metadata,
    -- Audit
    created_by
) VALUES (
    gen_random_uuid(),
    '550e8400-e29b-41d4-a716-446655440000',
    2,
    'Monthly Credit Card Statement',
    'STATEMENTS',
    'Statements',
    'monthly_statement',
    'credit_card',
    'en_us',
    false,
    'ECMS',
    '7c9e6679-7425-40de-944b-e07fc1f90ae7',  -- from ECMS
    'statement_2024_01_account_12345.pdf',
    245678,
    'application/pdf',
    '123e4567-e89b-12d3-a456-426614174000',  -- account_id
    '987fcdeb-51a2-43f1-b89c-012345678901',  -- customer_id
    '{
        "statement_id": "STMT-2024-01-12345",
        "cycle_date": "2024-01-31",
        "statement_period": {
            "start": "2024-01-01",
            "end": "2024-01-31"
        },
        "current_balance": 1234.56,
        "minimum_payment": 35.00,
        "due_date": "2024-02-25",
        "apr": 18.99,
        "transaction_count": 23
    }'::jsonb,
    'statement-generation-service'
);
```

#### Step 5: Retrieve Document (Zero Joins!)

```sql
-- Customer retrieves their statement - NO JOINS NEEDED!
SELECT
    storage_index_id,
    template_name,
    category_name,
    file_name,
    doc_creation_date,
    storage_document_key,
    doc_metadata->>'statement_id' as statement_id,
    doc_metadata->>'cycle_date' as cycle_date,
    (doc_metadata->>'current_balance')::numeric as current_balance,
    doc_metadata->>'due_date' as due_date
FROM storage_index
WHERE account_id = '123e4567-e89b-12d3-a456-426614174000'
  AND doc_type = 'monthly_statement'
  AND doc_metadata->>'cycle_date' = '2024-01-31'
  AND is_accessible = TRUE;
```

### Example: Add New Field to Existing Template

```sql
-- Add a new field (e.g., "rewards_earned") to template
UPDATE template_vendor_mapping
SET template_fields = template_fields || '[
    {
        "fieldName": "rewardsEarned",
        "fieldType": "decimal",
        "vendorFieldName": "Rewards.Earned",
        "required": false,
        "dataSource": {
            "serviceName": "rewards-service",
            "serviceUrl": "http://rewards-service:8080",
            "endpoint": "/api/v1/accounts/{accountId}/rewards/earned",
            "method": "GET",
            "queryParams": {
                "startDate": "{statementStartDate}",
                "endDate": "{statementEndDate}"
            },
            "jsonPath": "$.totalRewardsEarned",
            "timeout": 5000,
            "cacheConfig": {
                "enabled": true,
                "ttlSeconds": 600,
                "cacheKey": "rewards:{accountId}:{statementDate}"
            }
        },
        "transformation": {
            "type": "formatDecimal",
            "decimalPlaces": 2
        },
        "defaultValue": 0.00
    }
]'::jsonb,
updated_by = 'admin',
updated_at = CURRENT_TIMESTAMP
WHERE template_id = '550e8400-e29b-41d4-a716-446655440000'
  AND template_version = 2
  AND is_primary = TRUE;
```

---

## Monitoring and Maintenance

### 1. Monitor JSONB Column Sizes

```sql
SELECT
    template_id,
    version,
    template_name,
    pg_column_size(access_control) as access_control_size,
    pg_column_size(channels) as channels_size,
    pg_column_size(template_variables) as variables_size,
    pg_column_size(required_fields) as fields_size
FROM master_template_definition
ORDER BY pg_column_size(template_variables) DESC
LIMIT 20;
```

### 2. Monitor Index Usage

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan ASC;
```

### 3. Vacuum and Analyze

```sql
-- Regular maintenance
VACUUM ANALYZE master_template_definition;
VACUUM ANALYZE storage_index;
VACUUM ANALYZE template_vendor_mapping;
```

---

## Summary: Benefits of This Design

✅ **Zero Joins**: Most common queries require no joins
✅ **Fast Queries**: Denormalized data = faster reads
✅ **Flexible JSONB**: Easy to add new fields without schema changes
✅ **Proper Versioning**: Composite PK handles versions correctly
✅ **Consistent Types**: TIMESTAMPTZ for all dates
✅ **Good Indexing**: GIN indexes for JSONB, B-tree for scalars
✅ **Data Sync**: Triggers maintain consistency
✅ **Performance**: Optimized for read-heavy workloads

⚠️ **Trade-offs**:
- More storage (denormalized data)
- Sync complexity (triggers needed)
- Update cost (multiple tables need updates)

**Perfect for**: Read-heavy document management systems where query performance is critical.

---

**Version**: 2.0 (Denormalized)
**Date**: 2025-11-06
**Status**: Performance-Optimized Design
