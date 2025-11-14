# Data Dictionary - Core Document Hub Tables

**Version:** 1.0
**Last Updated:** 2025-11-13
**Status:** Production Ready

---

## Table of Contents

1. [master_template_definition](#1-master_template_definition)
2. [storage_index](#2-storage_index)
3. [template_vendor_mapping](#3-template_vendor_mapping)
4. [Cross-Table Relationships](#4-cross-table-relationships)
5. [Index Strategy Summary](#5-index-strategy-summary)

---

## 1. master_template_definition

**Purpose:** Stores master metadata and configuration details of document templates with versioning support.

**Design Philosophy:** Denormalized for performance - embeds category and business data to avoid joins.

### Table Properties

| Property | Value |
|----------|-------|
| Primary Key | `(template_id, version)` - Composite key |
| Partitioning | None |
| Retention | Indefinite (archived via flag) |
| Estimated Row Count | 1,000 - 10,000 templates |

### Column Definitions

#### Primary Key Columns

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_id` | UUID | NOT NULL | - | Unique identifier for the template across all versions |
| `version` | INTEGER | NOT NULL | 1 | Version number of the template (1, 2, 3, etc.) |

**Composite Primary Key:** `PRIMARY KEY (template_id, version)`

#### Template Identification

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `legacy_template_id` | VARCHAR(100) | NULL | - | Legacy system template ID for migration tracking |
| `template_name` | VARCHAR(255) | NOT NULL | - | Human-readable template name (unique per version) |
| `description` | TEXT | NULL | - | Detailed description of template purpose and usage |

#### Business Classification

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `line_of_business` | VARCHAR(50) | NOT NULL | - | Business line: `credit_card`, `enterprise`, `digital_banking` |
| `category_code` | VARCHAR(50) | NOT NULL | - | Category code (e.g., `STMT`, `DISC`, `NOTICE`) |
| `category_name` | VARCHAR(255) | NOT NULL | - | Category display name (denormalized for zero-join queries) |
| `doc_type` | VARCHAR(100) | NOT NULL | - | Document type (e.g., `monthly_statement`, `disclosure`, `confirmation`) |
| `language_code` | VARCHAR(10) | NOT NULL | `en_us` | Language code (ISO format with underscore) |
| `owning_dept` | VARCHAR(100) | NULL | - | Department that owns this template |

**Constraints:**
- `CHECK (line_of_business IN ('credit_card', 'enterprise', 'digital_banking'))`

#### Flags and Settings

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `notification_needed` | BOOLEAN | NULL | FALSE | Whether notifications should be sent when documents are created |
| `is_regulatory` | BOOLEAN | NULL | FALSE | Flag indicating regulatory/compliance document |
| `is_message_center_doc` | BOOLEAN | NULL | FALSE | Whether document appears in message center |
| `is_shared_document` | BOOLEAN | NULL | FALSE | Whether documents of this template can be shared |
| `sharing_scope` | VARCHAR(100) | NULL | - | Sharing scope: `all`, `credit_card_accounts_only`, etc. |

**Constraints:**
- `CHECK (sharing_scope IN ('all', 'credit_card_accounts_only', 'digital_bank_customer_only', 'enterprise_customer_only', 'custom_rule'))`

#### Status and Lifecycle

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_status` | VARCHAR(20) | NOT NULL | `DRAFT` | Status: `DRAFT`, `APPROVED`, `REJECTED`, `PENDING`, `ARCHIVED` |
| `effective_date` | TIMESTAMPTZ | NOT NULL | - | Date when template becomes effective |
| `valid_until` | TIMESTAMPTZ | NULL | - | Date when template expires (NULL = no expiration) |
| `retention_days` | INTEGER | NULL | 365 | Number of days to retain documents created from this template |

**Constraints:**
- `CHECK (template_status IN ('DRAFT', 'APPROVED', 'REJECTED', 'PENDING', 'ARCHIVED'))`
- `CHECK (valid_until IS NULL OR valid_until > effective_date)`

#### JSONB Fields

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `access_control` | JSONB | NOT NULL | `'[]'::jsonb` | Array of role-based access control rules |
| `required_fields` | JSONB | NOT NULL | `'[]'::jsonb` | Array of required fields with validation rules |
| `channels` | JSONB | NOT NULL | `'[]'::jsonb` | Array of delivery channels (EMAIL, PRINT, WEB) with config |
| `template_variables` | JSONB | NOT NULL | `'{}'::jsonb` | Object mapping variable names to their data sources |
| `data_extraction_config` | JSONB | NULL | - | Configuration for data extraction from generated documents |

**JSONB Structure Examples:**

**access_control:**
```json
[
    {"role": "admin", "actions": ["View", "Update", "Delete"]},
    {"role": "customer", "actions": ["View", "Download"]},
    {"role": "agent", "actions": ["View"]}
]
```

**required_fields:**
```json
[
    {"field": "pricingCode", "type": "string", "required": true, "validation": "^[A-Z0-9]+$"},
    {"field": "disclosureVersion", "type": "integer", "required": true, "min": 1},
    {"field": "effectiveDate", "type": "date", "required": true}
]
```

**channels:**
```json
[
    {"type": "EMAIL", "enabled": true, "config": {"priority": "high"}},
    {"type": "PRINT", "enabled": true, "config": {"duplex": true}},
    {"type": "WEB", "enabled": true}
]
```

**template_variables:**
```json
{
    "customerName": {
        "type": "string",
        "source": "customer-service",
        "endpoint": "/api/v1/customers/{customerId}",
        "method": "GET",
        "jsonPath": "$.data.name",
        "required": true,
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
        "cacheableFor": 300
    }
}
```

**data_extraction_config:**
```json
{
    "parser": "NLP",
    "rules": [
        {"field": "totalAmount", "pattern": "Total: \\$([0-9,]+\\.[0-9]{2})", "type": "currency"},
        {"field": "dueDate", "pattern": "Due Date: (\\d{2}/\\d{2}/\\d{4})", "type": "date"}
    ],
    "confidence_threshold": 0.85
}
```

#### Audit Columns (DBA Required)

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `created_by` | VARCHAR(100) | NOT NULL | - | User/system that created this template version |
| `created_at` | TIMESTAMPTZ | NOT NULL | `CURRENT_TIMESTAMP` | Timestamp when record was created |
| `updated_by` | VARCHAR(100) | NULL | - | User/system that last updated this record |
| `updated_at` | TIMESTAMPTZ | NULL | `CURRENT_TIMESTAMP` | Timestamp of last update |
| `archived_at` | TIMESTAMPTZ | NULL | - | Timestamp when record was archived |
| `archive_indicator` | BOOLEAN | NULL | FALSE | Soft delete flag |
| `version_number` | BIGINT | NULL | 1 | Internal version number for optimistic locking |

### Indexes

#### B-tree Indexes

| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_template_name` | `template_name` | B-tree | Lookup by template name |
| `idx_template_effective_date` | `effective_date` | B-tree | Filter by effective date |
| `idx_template_status` | `template_status` | B-tree | Filter by status |
| `idx_template_doc_type` | `doc_type` | B-tree | Filter by document type |
| `idx_template_lob` | `line_of_business` | B-tree | Filter by line of business |
| `idx_template_category` | `category_code` | B-tree | Filter by category |
| `idx_unique_template_name` | `(template_name, version)` | Unique | Ensure unique template name per version |

#### Partial Indexes (Filtered)

| Index Name | Expression | Filter | Purpose |
|------------|------------|--------|---------|
| `idx_template_active` | `(template_id, version)` | `archive_indicator = FALSE AND template_status = 'APPROVED' AND (valid_until IS NULL OR valid_until > CURRENT_TIMESTAMP)` | Fast lookup of active templates |

#### GIN Indexes (JSONB)

| Index Name | Column | Type | Purpose |
|------------|--------|------|---------|
| `idx_template_access_control` | `access_control` | GIN | Query access control rules |
| `idx_template_channels` | `channels` | GIN | Query delivery channels |
| `idx_template_required_fields` | `required_fields` | GIN | Query required fields |
| `idx_template_variables` | `template_variables` | GIN | Query template variables |

#### Expression Indexes

| Index Name | Expression | Purpose |
|------------|------------|---------|
| `idx_template_has_email` | `(channels @> '[{"type":"EMAIL","enabled":true}]')` | Fast check if EMAIL channel enabled |
| `idx_template_customer_view` | `(access_control @> '[{"role":"customer","actions":["View"]}]')` | Fast check if customers can view |

### Sample Data

```sql
INSERT INTO master_template_definition VALUES (
    '550e8400-e29b-41d4-a716-446655440000', -- template_id
    1, -- version
    'LEG-STMT-001', -- legacy_template_id
    'Monthly Credit Card Statement', -- template_name
    'Standard monthly statement for credit card accounts', -- description
    'credit_card', -- line_of_business
    'STMT', -- category_code
    'Statements', -- category_name
    'monthly_statement', -- doc_type
    'en_us', -- language_code
    'Credit Card Operations', -- owning_dept
    TRUE, -- notification_needed
    FALSE, -- is_regulatory
    TRUE, -- is_message_center_doc
    FALSE, -- is_shared_document
    NULL, -- sharing_scope
    'APPROVED', -- template_status
    '2024-01-01 00:00:00-05', -- effective_date
    NULL, -- valid_until
    2555, -- retention_days (7 years)
    '[{"role":"customer","actions":["View","Download"]}]'::jsonb, -- access_control
    '[{"field":"pricingCode","type":"string","required":true}]'::jsonb, -- required_fields
    '[{"type":"EMAIL","enabled":true},{"type":"PRINT","enabled":true}]'::jsonb, -- channels
    '{"customerName":{"type":"string","source":"customer-service"}}'::jsonb, -- template_variables
    NULL, -- data_extraction_config
    'system', -- created_by
    CURRENT_TIMESTAMP, -- created_at
    NULL, NULL, NULL, FALSE, 1 -- updated_by, updated_at, archived_at, archive_indicator, version_number
);
```

---

## 2. storage_index

**Purpose:** Index for stored documents with denormalized template data for zero-join queries.

**Design Philosophy:** Fully denormalized - contains all template metadata needed for document queries without joins.

### Table Properties

| Property | Value |
|----------|-------|
| Primary Key | `storage_index_id` (UUID) |
| Partitioning | Optional (by doc_creation_date for high volume) |
| Retention | Based on template retention policy + regulatory requirements |
| Estimated Row Count | 100M - 1B documents |

### Column Definitions

#### Primary Key

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `storage_index_id` | UUID | NOT NULL | `gen_random_uuid()` | Unique identifier for this document |

#### Template Reference

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_id` | UUID | NOT NULL | - | Reference to master_template_definition |
| `template_version` | INTEGER | NOT NULL | - | Version of template used to create this document |

**Foreign Key:** `FOREIGN KEY (template_id, template_version) REFERENCES master_template_definition(template_id, version)`

#### Denormalized Template Data

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_name` | VARCHAR(255) | NOT NULL | - | Template name (denormalized from master_template) |
| `category_code` | VARCHAR(50) | NOT NULL | - | Category code (denormalized) |
| `category_name` | VARCHAR(255) | NOT NULL | - | Category name (denormalized) |
| `doc_type` | VARCHAR(100) | NOT NULL | - | Document type (denormalized) |
| `line_of_business` | VARCHAR(50) | NOT NULL | - | Line of business (denormalized) |
| `language_code` | VARCHAR(10) | NOT NULL | - | Language code (denormalized) |
| `is_regulatory` | BOOLEAN | NULL | FALSE | Regulatory flag (denormalized) |

**Note:** These fields are copied from master_template_definition at document creation time to enable zero-join queries.

#### Storage Information

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `storage_vendor` | VARCHAR(50) | NOT NULL | `ECMS` | Storage system: `ECMS`, `S3`, `AZURE_BLOB`, `GCP_STORAGE` |
| `storage_document_key` | UUID | NOT NULL | - | Unique key in storage system (globally unique) |
| `file_name` | VARCHAR(500) | NULL | - | Original or generated filename |
| `file_size_bytes` | BIGINT | NULL | - | File size in bytes |
| `mime_type` | VARCHAR(100) | NULL | - | MIME type (e.g., `application/pdf`) |
| `file_hash` | VARCHAR(64) | NULL | - | SHA-256 hash for file integrity verification |

**Constraints:**
- `UNIQUE (storage_document_key)`
- `CHECK (storage_vendor IN ('ECMS', 'S3', 'AZURE_BLOB', 'GCP_STORAGE'))`

#### Reference Keys

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `reference_key` | VARCHAR(255) | NULL | - | Flexible reference key (value depends on reference_key_type) |
| `reference_key_type` | VARCHAR(50) | NULL | - | Type of reference: `account_id`, `disclosure_code`, `thread_id`, etc. |

**Constraints:**
- `CHECK (reference_key_type IN ('account_id', 'disclosure_code', 'thread_id', 'correlation_id', 'statement_id', 'transaction_id', 'customer_id'))`

#### Customer/Account Information

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `account_id` | UUID | NULL | - | Account identifier (if document is account-specific) |
| `customer_id` | UUID | NULL | - | Customer identifier (if document is customer-specific) |

#### Document Lifecycle

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `doc_creation_date` | TIMESTAMPTZ | NOT NULL | `CURRENT_TIMESTAMP` | When document was created/stored |
| `is_accessible` | BOOLEAN | NULL | TRUE | Whether document is currently accessible (soft delete) |
| `last_accessed_at` | TIMESTAMPTZ | NULL | - | Last time document was accessed/viewed |
| `access_count` | INTEGER | NULL | 0 | Number of times document has been accessed |

#### JSONB Fields

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `doc_metadata` | JSONB | NOT NULL | `'{}'::jsonb` | Document-specific metadata (structure varies by doc_type) |
| `access_control` | JSONB | NULL | - | Document-level access control (can override template default) |
| `compliance_tags` | JSONB | NULL | - | Compliance and data classification metadata |

**JSONB Structure Examples:**

**doc_metadata for monthly_statement:**
```json
{
    "statement_id": "STMT-2024-01-12345",
    "cycle_date": "2024-01-31",
    "statement_period": {"start": "2024-01-01", "end": "2024-01-31"},
    "total_amount": 1234.56,
    "due_date": "2024-02-15",
    "minimum_payment": 25.00
}
```

**doc_metadata for disclosure:**
```json
{
    "disclosure_code": "DISC-001",
    "disclosure_version": 3,
    "effective_date": "2024-01-01",
    "pricing_code": "STANDARD",
    "apr": 18.99
}
```

**compliance_tags:**
```json
{
    "data_classification": "CONFIDENTIAL",
    "contains_pii": true,
    "gdpr_applicable": true,
    "retention_policy": "7_years",
    "encryption_required": true
}
```

#### Retention and Compliance

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `retention_until` | TIMESTAMPTZ | NULL | - | Calculated retention expiration date |

#### Audit Columns (DBA Required)

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `created_by` | VARCHAR(100) | NOT NULL | - | User/system that created this document |
| `created_at` | TIMESTAMPTZ | NOT NULL | `CURRENT_TIMESTAMP` | Timestamp when record was created |
| `updated_by` | VARCHAR(100) | NULL | - | User/system that last updated this record |
| `updated_at` | TIMESTAMPTZ | NULL | `CURRENT_TIMESTAMP` | Timestamp of last update |
| `archived_at` | TIMESTAMPTZ | NULL | - | Timestamp when record was archived |
| `archive_indicator` | BOOLEAN | NULL | FALSE | Soft delete flag |
| `version_number` | BIGINT | NULL | 1 | Internal version number for optimistic locking |

### Indexes

#### B-tree Indexes

| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_storage_template` | `(template_id, template_version)` | B-tree | Lookup documents by template |
| `idx_storage_template_name` | `template_name` | B-tree | Search by template name |
| `idx_storage_creation_date` | `doc_creation_date DESC` | B-tree | Sort by creation date |
| `idx_storage_doc_type` | `doc_type` | B-tree | Filter by document type |
| `idx_storage_category` | `category_code` | B-tree | Filter by category |
| `idx_storage_lob` | `line_of_business` | B-tree | Filter by line of business |
| `idx_storage_reference` | `(reference_key_type, reference_key)` | B-tree | Lookup by reference key |

#### Partial Indexes (Filtered)

| Index Name | Expression | Filter | Purpose |
|------------|------------|--------|---------|
| `idx_storage_account` | `account_id` | `account_id IS NOT NULL` | Account documents only |
| `idx_storage_customer` | `customer_id` | `customer_id IS NOT NULL` | Customer documents only |
| `idx_storage_account_date` | `(account_id, doc_creation_date DESC)` | `is_accessible = TRUE AND archive_indicator = FALSE` | Active account documents |
| `idx_storage_customer_date` | `(customer_id, doc_creation_date DESC)` | `is_accessible = TRUE AND archive_indicator = FALSE` | Active customer documents |
| `idx_storage_accessible` | `storage_index_id` | `is_accessible = TRUE AND archive_indicator = FALSE` | Hot data only |
| `idx_storage_regulatory` | `doc_creation_date DESC` | `is_regulatory = TRUE AND archive_indicator = FALSE` | Active regulatory documents |

#### GIN Indexes (JSONB)

| Index Name | Column | Type | Purpose |
|------------|--------|------|---------|
| `idx_storage_metadata` | `doc_metadata` | GIN | Query document metadata |
| `idx_storage_compliance` | `compliance_tags` | GIN | Query compliance tags |
| `idx_storage_access` | `access_control` | GIN | Query access control |

#### Expression Indexes

| Index Name | Expression | Filter | Purpose |
|------------|------------|--------|---------|
| `idx_storage_statement_id` | `(doc_metadata->>'statement_id')` | `doc_type = 'monthly_statement'` | Fast statement lookup |
| `idx_storage_disclosure_code` | `(doc_metadata->>'disclosure_code')` | `doc_type = 'disclosure'` | Fast disclosure lookup |
| `idx_storage_transaction_id` | `(doc_metadata->>'transaction_id')` | `doc_type LIKE '%transaction%'` | Fast transaction lookup |
| `idx_storage_pii` | `(compliance_tags->>'contains_pii')` | `(compliance_tags->>'contains_pii')::boolean = true` | PII documents |

### Sample Data

```sql
INSERT INTO storage_index VALUES (
    '660e8400-e29b-41d4-a716-446655440001', -- storage_index_id
    '550e8400-e29b-41d4-a716-446655440000', -- template_id
    1, -- template_version
    'Monthly Credit Card Statement', -- template_name (denormalized)
    'STMT', -- category_code (denormalized)
    'Statements', -- category_name (denormalized)
    'monthly_statement', -- doc_type (denormalized)
    'credit_card', -- line_of_business (denormalized)
    'en_us', -- language_code (denormalized)
    FALSE, -- is_regulatory (denormalized)
    'ECMS', -- storage_vendor
    '770e8400-e29b-41d4-a716-446655440002', -- storage_document_key
    'statement_2024_01_account_12345.pdf', -- file_name
    1048576, -- file_size_bytes (1MB)
    'application/pdf', -- mime_type
    'abc123def456...', -- file_hash
    'ACCT-12345', -- reference_key
    'account_id', -- reference_key_type
    '880e8400-e29b-41d4-a716-446655440003', -- account_id
    '990e8400-e29b-41d4-a716-446655440004', -- customer_id
    '2024-01-31 23:59:00-05', -- doc_creation_date
    TRUE, -- is_accessible
    '2024-02-15 10:30:00-05', -- last_accessed_at
    5, -- access_count
    '{"statement_id":"STMT-2024-01-12345","cycle_date":"2024-01-31","total_amount":1234.56}'::jsonb, -- doc_metadata
    NULL, -- access_control
    '{"data_classification":"CONFIDENTIAL","contains_pii":true}'::jsonb, -- compliance_tags
    '2031-01-31 23:59:59-05', -- retention_until (7 years)
    'system', -- created_by
    CURRENT_TIMESTAMP, -- created_at
    NULL, NULL, NULL, FALSE, 1 -- updated_by, updated_at, archived_at, archive_indicator, version_number
);
```

---

## 3. template_vendor_mapping

**Purpose:** Maps templates to vendor implementations with denormalized template data and field definitions for document generation.

**Design Philosophy:** Contains all information needed to call vendor APIs without joins - includes data source mappings for all template fields.

### Table Properties

| Property | Value |
|----------|-------|
| Primary Key | `template_vendor_id` (UUID) |
| Unique Constraint | `(template_id, template_version, vendor)` |
| Partitioning | None |
| Retention | Indefinite (archived via flag) |
| Estimated Row Count | 10,000 - 50,000 mappings |

### Column Definitions

#### Primary Key

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_vendor_id` | UUID | NOT NULL | `gen_random_uuid()` | Unique identifier for this vendor mapping |

#### Template Reference

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_id` | UUID | NOT NULL | - | Reference to master_template_definition |
| `template_version` | INTEGER | NOT NULL | - | Template version this mapping applies to |

**Foreign Key:** `FOREIGN KEY (template_id, template_version) REFERENCES master_template_definition(template_id, version)`

**Unique Constraint:** `UNIQUE (template_id, template_version, vendor)` - One mapping per vendor per template version

#### Denormalized Template Data

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_name` | VARCHAR(255) | NOT NULL | - | Template name (denormalized for zero-join queries) |
| `doc_type` | VARCHAR(100) | NOT NULL | - | Document type (denormalized) |
| `category_code` | VARCHAR(50) | NOT NULL | - | Category code (denormalized) |

#### Vendor Information

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `vendor` | VARCHAR(50) | NOT NULL | - | Vendor name: `SMARTCOMM`, `ASSENTIS`, `HANDLEBAR`, `CUSTOM` |
| `vendor_template_key` | VARCHAR(255) | NOT NULL | - | Vendor's internal template identifier/key |
| `vendor_template_name` | VARCHAR(255) | NULL | - | Vendor's template name (may differ from our template_name) |
| `api_endpoint` | VARCHAR(1000) | NULL | - | Vendor API endpoint for document generation |

**Constraints:**
- `CHECK (vendor IN ('SMARTCOMM', 'ASSENTIS', 'HANDLEBAR', 'CUSTOM'))`

#### Template Content Reference

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `template_content_uri` | VARCHAR(1000) | NULL | - | URI/path to template content in ECMS or external storage (NOT blob) |
| `template_content_hash` | VARCHAR(64) | NULL | - | SHA-256 hash for template content integrity verification |

**Note:** Template content is NOT stored as blob in database - only URI reference.

#### Version Management

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `vendor_version` | VARCHAR(50) | NULL | - | Vendor's internal version identifier |
| `is_active` | BOOLEAN | NULL | TRUE | Whether this mapping is currently active |
| `is_primary` | BOOLEAN | NULL | FALSE | Whether this is the primary vendor for this template |

**Unique Constraint:** Only one primary vendor per template version
- `CREATE UNIQUE INDEX idx_unique_primary_vendor ON template_vendor_mapping(template_id, template_version) WHERE is_primary = TRUE`

#### Lifecycle

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `effective_date` | TIMESTAMPTZ | NOT NULL | - | When this vendor mapping becomes effective |
| `valid_until` | TIMESTAMPTZ | NULL | - | When this mapping expires (NULL = no expiration) |

**Constraints:**
- `CHECK (valid_until IS NULL OR valid_until > effective_date)`

#### JSONB Fields

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `schema_info` | JSONB | NOT NULL | `'{}'::jsonb` | Vendor schema definition (input schema, output format, etc.) |
| `template_fields` | JSONB | NOT NULL | `'[]'::jsonb` | Array of field definitions with data sources from multiple services |
| `vendor_config` | JSONB | NOT NULL | `'{}'::jsonb` | Vendor-specific configuration (varies by vendor) |
| `api_config` | JSONB | NULL | - | API authentication and connection configuration |

**JSONB Structure Examples:**

**schema_info:**
```json
{
    "input_schema": {
        "customerData": {"type": "object", "required": ["name", "address"]},
        "accountData": {"type": "object", "required": ["accountNumber", "balance"]}
    },
    "output_format": "PDF",
    "supported_languages": ["en_us", "es_us"],
    "max_page_count": 50
}
```

**template_fields:** (Array of field definitions)
```json
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
        "fieldName": "accountBalance",
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
                "limit": "100"
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
        }
    }
]
```

**vendor_config for SMARTCOMM:**
```json
{
    "project_id": "PROJECT-123",
    "template_type": "interactive",
    "batch_enabled": true,
    "output_options": {
        "pdf_version": "1.7",
        "compression": true
    }
}
```

**vendor_config for ASSENTIS:**
```json
{
    "workflow_id": "WF-456",
    "approval_required": true,
    "branding": {
        "logo_url": "https://cdn.example.com/logo.png",
        "color_scheme": "#003366"
    }
}
```

**api_config:**
```json
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
```

#### Consumer Tracking

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `consumer_id` | UUID | NULL | - | Kafka consumer ID that manages this mapping |
| `last_sync_at` | TIMESTAMPTZ | NULL | - | Last time mapping was synced from source |
| `sync_status` | JSONB | NULL | - | Sync status and error tracking |

**sync_status structure:**
```json
{
    "last_successful_sync": "2024-01-15T10:30:00Z",
    "last_error": null,
    "sync_count": 150,
    "last_sync_duration_ms": 1234
}
```

#### Audit Columns (DBA Required)

| Column Name | Data Type | Nullable | Default | Description |
|-------------|-----------|----------|---------|-------------|
| `created_by` | VARCHAR(100) | NOT NULL | - | User/system that created this mapping |
| `created_at` | TIMESTAMPTZ | NOT NULL | `CURRENT_TIMESTAMP` | Timestamp when record was created |
| `updated_by` | VARCHAR(100) | NULL | - | User/system that last updated this record |
| `updated_at` | TIMESTAMPTZ | NULL | `CURRENT_TIMESTAMP` | Timestamp of last update |
| `archived_at` | TIMESTAMPTZ | NULL | - | Timestamp when record was archived |
| `archive_indicator` | BOOLEAN | NULL | FALSE | Soft delete flag |
| `version_number` | BIGINT | NULL | 1 | Internal version number for optimistic locking |

### Indexes

#### B-tree Indexes

| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_vendor_template` | `(template_id, template_version)` | B-tree | Lookup mappings by template |
| `idx_vendor_template_name` | `template_name` | B-tree | Search by template name |
| `idx_vendor_type` | `vendor` | B-tree | Filter by vendor |
| `idx_vendor_doc_type` | `doc_type` | B-tree | Filter by document type |
| `idx_vendor_key` | `vendor_template_key` | B-tree | Lookup by vendor's template key |

#### Partial Indexes (Filtered)

| Index Name | Expression | Filter | Purpose |
|------------|------------|--------|---------|
| `idx_vendor_active` | `(template_id, template_version)` | `is_active = TRUE AND archive_indicator = FALSE` | Active mappings only |
| `idx_vendor_primary` | `(template_id, template_version)` | `is_primary = TRUE AND is_active = TRUE` | Primary vendor lookup |
| `idx_unique_primary_vendor` | `(template_id, template_version)` | `is_primary = TRUE` | Ensure only one primary vendor |

#### GIN Indexes (JSONB)

| Index Name | Column | Type | Purpose |
|------------|--------|------|---------|
| `idx_vendor_schema` | `schema_info` | GIN | Query vendor schemas |
| `idx_vendor_config` | `vendor_config` | GIN | Query vendor configs |
| `idx_vendor_api_config` | `api_config` | GIN | Query API configs |
| `idx_vendor_template_fields` | `template_fields` | GIN | Query template fields |

### Sample Data

```sql
INSERT INTO template_vendor_mapping VALUES (
    'aa0e8400-e29b-41d4-a716-446655440005', -- template_vendor_id
    '550e8400-e29b-41d4-a716-446655440000', -- template_id
    1, -- template_version
    'Monthly Credit Card Statement', -- template_name (denormalized)
    'monthly_statement', -- doc_type (denormalized)
    'STMT', -- category_code (denormalized)
    'SMARTCOMM', -- vendor
    'SMARTCOMM-STMT-CC-001', -- vendor_template_key
    'Credit Card Monthly Statement Template', -- vendor_template_name
    'https://smartcomm-api.example.com/v1/generate', -- api_endpoint
    'ecms://templates/smartcomm/stmt-cc-001.xml', -- template_content_uri
    'def789abc123...', -- template_content_hash
    'v2.5.1', -- vendor_version
    TRUE, -- is_active
    TRUE, -- is_primary
    '2024-01-01 00:00:00-05', -- effective_date
    NULL, -- valid_until
    '{"input_schema":{"customerData":{"type":"object"}},"output_format":"PDF"}'::jsonb, -- schema_info
    '[{"fieldName":"customerName","fieldType":"string","vendorFieldName":"Customer.Name"}]'::jsonb, -- template_fields
    '{"project_id":"PROJECT-123","template_type":"interactive"}'::jsonb, -- vendor_config
    '{"base_url":"https://smartcomm-api.example.com/v1","auth_type":"oauth2"}'::jsonb, -- api_config
    NULL, NULL, NULL, -- consumer_id, last_sync_at, sync_status
    'system', -- created_by
    CURRENT_TIMESTAMP, -- created_at
    NULL, NULL, NULL, FALSE, 1 -- updated_by, updated_at, archived_at, archive_indicator, version_number
);
```

---

## 4. Cross-Table Relationships

### Relationship Diagram

```
master_template_definition (template_id, version)
    |
    |---- FK: storage_index (template_id, template_version)
    |         Purpose: Links stored documents to their templates
    |         Cardinality: 1 template → many documents
    |         Constraint: ON DELETE RESTRICT
    |
    |---- FK: template_vendor_mapping (template_id, template_version)
              Purpose: Links templates to vendor implementations
              Cardinality: 1 template → many vendor mappings (one per vendor)
              Constraint: ON DELETE RESTRICT
```

### Denormalization Strategy

**Why Denormalize?**
- Query performance: Avoid expensive joins in high-volume queries
- Analytics optimization: Common queries need template metadata + document data
- Read-heavy workload: Templates change infrequently, documents queried frequently

**What is Denormalized?**

| Source Table | Denormalized To | Fields Copied |
|--------------|-----------------|---------------|
| `master_template_definition` | `storage_index` | `template_name`, `category_code`, `category_name`, `doc_type`, `line_of_business`, `language_code`, `is_regulatory` |
| `master_template_definition` | `template_vendor_mapping` | `template_name`, `doc_type`, `category_code` |

**Trade-offs:**
- **Pro:** 10-100x faster queries (no joins needed)
- **Pro:** Simpler queries, easier to maintain
- **Con:** Data duplication (~200 bytes per document)
- **Con:** Must update denormalized data if template changes (rare event)

**Update Strategy:**
When template metadata changes:
1. Create new template version
2. New documents use new version automatically
3. Existing documents retain historical data (correct for audit purposes)

---

## 5. Index Strategy Summary

### Indexing Philosophy

1. **B-tree indexes**: For exact matches, range queries, sorting
2. **Partial indexes**: For common filtered queries (active records only)
3. **GIN indexes**: For JSONB containment queries (@>, ?, ?&, ?|)
4. **Expression indexes**: For specific JSONB field lookups
5. **Unique indexes**: For business constraints and data integrity

### Index Maintenance

**Statistics Updates:**
```sql
-- After bulk loads or major changes
ANALYZE master_template_definition;
ANALYZE storage_index;
ANALYZE template_vendor_mapping;
```

**Index Bloat Monitoring:**
```sql
-- Check index sizes
SELECT schemaname, tablename, indexname, pg_size_pretty(pg_relation_size(indexrelid))
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Rebuild bloated indexes
REINDEX INDEX CONCURRENTLY idx_storage_account_date;
```

### Common Query Patterns

#### Pattern 1: Get Documents for Account (Zero-Join Query)

```sql
-- Without denormalization: 2-table join
SELECT s.*, t.template_name, t.doc_type, t.category_name
FROM storage_index s
JOIN master_template_definition t ON (s.template_id = t.template_id AND s.template_version = t.version)
WHERE s.account_id = ?
  AND s.is_accessible = TRUE
ORDER BY s.doc_creation_date DESC
LIMIT 50;

-- With denormalization: Single table, uses idx_storage_account_date
SELECT storage_index_id, template_name, doc_type, category_name,
       doc_creation_date, file_name
FROM storage_index
WHERE account_id = ?
  AND is_accessible = TRUE
ORDER BY doc_creation_date DESC
LIMIT 50;
```

**Performance:** <10ms (vs. 100-500ms with join)

#### Pattern 2: Get Vendor Mapping for Template

```sql
-- Get primary vendor for template
-- Uses idx_vendor_primary (partial index)
SELECT vendor, vendor_template_key, api_endpoint, template_fields
FROM template_vendor_mapping
WHERE template_id = ?
  AND template_version = ?
  AND is_primary = TRUE
  AND is_active = TRUE;
```

**Performance:** <1ms (index-only scan)

#### Pattern 3: Search Documents by Statement ID

```sql
-- Uses idx_storage_statement_id (expression index)
SELECT storage_index_id, template_name, account_id, doc_creation_date
FROM storage_index
WHERE doc_type = 'monthly_statement'
  AND doc_metadata->>'statement_id' = ?;
```

**Performance:** <5ms (index scan on expression index)

#### Pattern 4: Find Templates with Email Channel

```sql
-- Uses idx_template_has_email (expression index)
SELECT template_id, version, template_name, doc_type
FROM master_template_definition
WHERE channels @> '[{"type":"EMAIL","enabled":true}]'
  AND template_status = 'APPROVED'
  AND archive_indicator = FALSE;
```

**Performance:** <10ms (bitmap index scan)

---

## Appendix A: Data Type Standards

### UUID Standards
- Use `gen_random_uuid()` for new IDs (requires `pgcrypto` extension)
- Store as UUID type (not VARCHAR)
- Display as lowercase with dashes: `550e8400-e29b-41d4-a716-446655440000`

### Timestamp Standards
- Always use `TIMESTAMPTZ` (timestamp with time zone)
- Store in UTC internally
- Default: `CURRENT_TIMESTAMP`
- Display format: ISO 8601 with timezone

### JSONB Standards
- Always use `JSONB` (not `JSON`) for indexing and performance
- Validate structure at application layer
- Use consistent key naming: snake_case
- Default to empty object `'{}'::jsonb` or empty array `'[]'::jsonb`

### Boolean Standards
- Use `BOOLEAN` type (not INT or CHAR)
- Default values: `TRUE`, `FALSE`, or `NULL`
- Avoid "is_not_" naming (use positive flags)

### VARCHAR Standards
- Set reasonable limits based on data
- Use TEXT for unbounded strings
- Common sizes:
  - Names: VARCHAR(255)
  - Codes: VARCHAR(50-100)
  - URLs: VARCHAR(1000)
  - Descriptions: TEXT

---

## Appendix B: Audit Column Standards

All tables MUST include these audit columns:

```sql
-- Audit Fields (DBA Required)
created_by VARCHAR(100) NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_by VARCHAR(100),
updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
archived_at TIMESTAMPTZ,
archive_indicator BOOLEAN DEFAULT FALSE,
version_number BIGINT DEFAULT 1
```

**Rules:**
1. `created_by` is REQUIRED (use 'system' for automated processes)
2. `created_at` automatically populated
3. `updated_by` and `updated_at` populated on updates
4. Never DELETE rows - use `archive_indicator = TRUE` and set `archived_at`
5. `version_number` incremented on each update (optimistic locking)

---

## Appendix C: Naming Conventions

### Table Names
- Lowercase with underscores: `master_template_definition`
- Singular form: `template` not `templates`
- Descriptive: avoid abbreviations unless domain standard

### Column Names
- Lowercase with underscores: `template_name`
- Suffix conventions:
  - `_id`: UUID identifiers
  - `_at`: Timestamps
  - `_by`: User/system identifiers
  - `_date`: Date-only fields
  - `_indicator`: Boolean flags

### Index Names
- Prefix by type:
  - `idx_`: Standard B-tree index
  - `uidx_`: Unique index
  - `pk_`: Primary key (auto-generated)
  - `fk_`: Foreign key (if explicitly named)
- Include table and column: `idx_storage_account_date`

### Constraint Names
- Prefix by type:
  - `pk_`: Primary key
  - `fk_`: Foreign key
  - `chk_`: Check constraint
  - `unq_`: Unique constraint
- Descriptive: `chk_valid_date_range`

---

## Document Revision History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-11-13 | 1.0 | Claude Code | Initial comprehensive data dictionary for core tables |

---

**End of Data Dictionary**
