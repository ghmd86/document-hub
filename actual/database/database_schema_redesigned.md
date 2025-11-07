# Document Hub Service - Redesigned Database Schema

## Overview
This redesigned schema addresses the following key issues from the original design:
1. Proper composite primary key for template versioning
2. Normalized JSONB fields into proper relational tables
3. Consistent data types (TIMESTAMPTZ for all dates)
4. Removed redundant columns
5. Added proper constraints and indexes
6. Separated concerns into normalized tables

---

## Core Tables

### 1. Master Template Definition (Core)

**Purpose:** Stores template metadata with proper versioning support.

```sql
CREATE TABLE master_template_definition (
    -- Primary Keys
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,

    -- Template Identification
    legacy_template_id VARCHAR(100),
    template_name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Business Classification
    line_of_business VARCHAR(50) NOT NULL,
    category_id UUID NOT NULL,
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

    -- Data Extraction Configuration
    data_extraction_schema_id UUID,

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

-- Indexes for performance
CREATE INDEX idx_template_name ON master_template_definition(template_name);
CREATE INDEX idx_template_effective_date ON master_template_definition(effective_date);
CREATE INDEX idx_template_status ON master_template_definition(template_status);
CREATE INDEX idx_template_doc_type ON master_template_definition(doc_type);
CREATE INDEX idx_template_lob ON master_template_definition(line_of_business);
CREATE INDEX idx_template_active ON master_template_definition(template_id, version)
    WHERE valid_until IS NULL OR valid_until > CURRENT_TIMESTAMP;

-- Unique constraint: template name should be unique per active version
CREATE UNIQUE INDEX idx_unique_template_name
    ON master_template_definition(template_name, version);

COMMENT ON TABLE master_template_definition IS 'Master table storing document template definitions with versioning support';
COMMENT ON COLUMN master_template_definition.template_id IS 'Unique identifier for the template (constant across versions)';
COMMENT ON COLUMN master_template_definition.version IS 'Version number of the template (increments with each new version)';
COMMENT ON COLUMN master_template_definition.retention_days IS 'Number of days to retain documents generated from this template';
```

---

### 2. Document Categories (Lookup Table)

**Purpose:** Normalized category definitions.

```sql
CREATE TABLE document_categories (
    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(255) NOT NULL UNIQUE,
    category_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    display_order INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Reference data
INSERT INTO document_categories (category_code, category_name, display_order) VALUES
    ('STATEMENTS', 'Statements', 1),
    ('GENERAL_COMM', 'General Communications', 2),
    ('PAYMENT_CONF', 'Payments Confirmation Notice', 3),
    ('CREDIT_INCREASE', 'Credit Line Increase', 4),
    ('TERMS_NOTICE', 'Change In Terms Notice', 5),
    ('ANNUAL_FEE', 'Annual Fee Notice', 6),
    ('PRIVACY', 'Privacy Policy', 7),
    ('ELECTRONIC_DISC', 'Electronic Disclosure and Communications Consent Agreement', 8),
    ('CARDHOLDER_AGREE', 'Cardholder Agreement', 9),
    ('CREDIT_PROTECT', 'Credit Protection', 10),
    ('BALANCE_TRANSFER', 'Balance Transfers', 11),
    ('DISPUTED_TRANS', 'Disputed Transactions', 12);

-- Add FK to master template
ALTER TABLE master_template_definition
    ADD CONSTRAINT fk_category
    FOREIGN KEY (category_id) REFERENCES document_categories(category_id);

COMMENT ON TABLE document_categories IS 'Lookup table for document categories';
```

---

### 3. Template Access Control

**Purpose:** Normalized access control rules (replacing JSONB).

```sql
CREATE TABLE template_access_control (
    access_control_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_template_access
        FOREIGN KEY (template_id, version)
        REFERENCES master_template_definition(template_id, version)
        ON DELETE CASCADE,

    CONSTRAINT chk_role_name
        CHECK (role_name IN ('admin', 'backOffice', 'customer', 'agent', 'system')),

    CONSTRAINT chk_action
        CHECK (action IN ('View', 'Update', 'Delete', 'Download', 'Print', 'Share')),

    UNIQUE (template_id, version, role_name, action)
);

CREATE INDEX idx_access_template ON template_access_control(template_id, version);
CREATE INDEX idx_access_role ON template_access_control(role_name);

COMMENT ON TABLE template_access_control IS 'Defines role-based access control for templates';
```

---

### 4. Template Required Fields

**Purpose:** Validation rules for template fields (replacing JSONB).

```sql
CREATE TABLE template_required_fields (
    field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    is_required BOOLEAN DEFAULT TRUE,
    validation_rule TEXT,
    default_value TEXT,
    display_order INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_template_fields
        FOREIGN KEY (template_id, version)
        REFERENCES master_template_definition(template_id, version)
        ON DELETE CASCADE,

    CONSTRAINT chk_field_type
        CHECK (field_type IN ('string', 'integer', 'decimal', 'boolean', 'date', 'datetime', 'uuid', 'email', 'url')),

    UNIQUE (template_id, version, field_name)
);

CREATE INDEX idx_fields_template ON template_required_fields(template_id, version);

COMMENT ON TABLE template_required_fields IS 'Defines required fields and validation rules for templates';
```

---

### 5. Template Channels

**Purpose:** Document delivery channels (replacing JSONB).

```sql
CREATE TABLE template_channels (
    channel_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    channel_config JSONB, -- Channel-specific configuration if needed
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_template_channels
        FOREIGN KEY (template_id, version)
        REFERENCES master_template_definition(template_id, version)
        ON DELETE CASCADE,

    CONSTRAINT chk_channel_type
        CHECK (channel_type IN ('PRINT', 'EMAIL', 'WEB', 'MOBILE', 'SMS', 'PUSH')),

    UNIQUE (template_id, version, channel_type)
);

CREATE INDEX idx_channels_template ON template_channels(template_id, version);
CREATE INDEX idx_channels_type ON template_channels(channel_type) WHERE is_enabled = TRUE;

COMMENT ON TABLE template_channels IS 'Defines available delivery channels for templates';
```

---

### 6. Template Variables

**Purpose:** Define variables and their API sources for template generation.

```sql
CREATE TABLE template_variables (
    variable_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    variable_type VARCHAR(50) NOT NULL,
    description TEXT,
    api_source VARCHAR(255),
    api_endpoint VARCHAR(500),
    json_path VARCHAR(255),
    is_required BOOLEAN DEFAULT TRUE,
    default_value TEXT,
    transformation_rule TEXT,
    display_order INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_template_variables
        FOREIGN KEY (template_id, version)
        REFERENCES master_template_definition(template_id, version)
        ON DELETE CASCADE,

    UNIQUE (template_id, version, variable_name)
);

CREATE INDEX idx_variables_template ON template_variables(template_id, version);

COMMENT ON TABLE template_variables IS 'Defines variables and their data sources for template generation';
```

---

### 7. Storage Index (Redesigned)

**Purpose:** Index for stored documents with proper template version tracking.

```sql
CREATE TABLE storage_index (
    -- Primary Key
    storage_index_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Template Reference (with version)
    template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,
    doc_type VARCHAR(100) NOT NULL,

    -- Storage Information
    storage_vendor VARCHAR(50) NOT NULL DEFAULT 'ECMS',
    storage_document_key UUID NOT NULL,
    file_name VARCHAR(500),
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),

    -- Reference Keys
    reference_key VARCHAR(255),
    reference_key_type VARCHAR(50),

    -- Customer/Account Information
    account_key UUID,
    customer_key UUID,

    -- Document Lifecycle
    doc_creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_accessible BOOLEAN DEFAULT TRUE,
    last_accessed_at TIMESTAMPTZ,
    access_count INTEGER DEFAULT 0,

    -- Document Metadata (flexible for statement dates, disclosure info, etc.)
    doc_metadata JSONB,

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

-- Indexes for performance
CREATE INDEX idx_storage_template ON storage_index(template_id, template_version);
CREATE INDEX idx_storage_account ON storage_index(account_key) WHERE account_key IS NOT NULL;
CREATE INDEX idx_storage_customer ON storage_index(customer_key) WHERE customer_key IS NOT NULL;
CREATE INDEX idx_storage_reference ON storage_index(reference_key_type, reference_key);
CREATE INDEX idx_storage_doc_key ON storage_index(storage_document_key);
CREATE INDEX idx_storage_creation_date ON storage_index(doc_creation_date DESC);
CREATE INDEX idx_storage_accessible ON storage_index(is_accessible) WHERE is_accessible = TRUE;
CREATE INDEX idx_storage_doc_type ON storage_index(doc_type);

-- GIN index for JSONB querying
CREATE INDEX idx_storage_metadata ON storage_index USING GIN (doc_metadata);

COMMENT ON TABLE storage_index IS 'Index for stored documents with references to storage locations';
COMMENT ON COLUMN storage_index.doc_metadata IS 'Flexible JSONB field for document-specific metadata (cycle_date, statement_id, disclosure_start_date, etc.)';
COMMENT ON COLUMN storage_index.access_count IS 'Counter tracking how many times document has been accessed';
```

---

### 8. Storage Index by Account (Optimized View)

**Purpose:** Materialized view for fast account-based lookups.

```sql
CREATE MATERIALIZED VIEW storage_index_by_account AS
SELECT
    si.account_key,
    si.storage_index_id,
    si.template_id,
    si.template_version,
    mtd.template_name,
    mtd.category_id,
    dc.category_name,
    si.doc_type,
    si.file_name,
    si.doc_creation_date,
    si.is_accessible,
    si.storage_document_key,
    si.doc_metadata
FROM storage_index si
JOIN master_template_definition mtd
    ON si.template_id = mtd.template_id
    AND si.template_version = mtd.version
JOIN document_categories dc
    ON mtd.category_id = dc.category_id
WHERE si.account_key IS NOT NULL
  AND si.is_accessible = TRUE
  AND si.archive_indicator = FALSE;

CREATE UNIQUE INDEX idx_mv_account_storage ON storage_index_by_account(storage_index_id);
CREATE INDEX idx_mv_account_key ON storage_index_by_account(account_key);
CREATE INDEX idx_mv_doc_creation ON storage_index_by_account(doc_creation_date DESC);

COMMENT ON MATERIALIZED VIEW storage_index_by_account IS 'Optimized view for account-based document lookups';
```

---

### 9. Template Vendor Mapping (Redesigned)

**Purpose:** Maps templates to vendor-specific implementations.

```sql
CREATE TABLE template_vendor_mapping (
    -- Primary Key
    template_vendor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Template Reference (with version)
    template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,

    -- Vendor Information
    vendor VARCHAR(50) NOT NULL,
    vendor_template_key VARCHAR(255) NOT NULL,
    api_endpoint VARCHAR(1000),

    -- Template Content Reference (NOT blob storage)
    template_content_uri VARCHAR(1000), -- URI/path to ECMS or external storage
    template_content_hash VARCHAR(64), -- SHA-256 hash for integrity verification

    -- Version Management
    vendor_version VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    is_primary BOOLEAN DEFAULT FALSE, -- Only one primary vendor per template version

    -- Lifecycle
    effective_date TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,

    -- Schema and Configuration
    schema_info JSONB,
    vendor_config JSONB, -- Vendor-specific configuration

    -- Consumer tracking (for Kafka consumers)
    consumer_id UUID,
    last_sync_at TIMESTAMPTZ,

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

-- Indexes
CREATE INDEX idx_vendor_template ON template_vendor_mapping(template_id, template_version);
CREATE INDEX idx_vendor_type ON template_vendor_mapping(vendor);
CREATE INDEX idx_vendor_active ON template_vendor_mapping(template_id, template_version, is_active)
    WHERE is_active = TRUE;
CREATE INDEX idx_vendor_primary ON template_vendor_mapping(template_id, template_version, is_primary)
    WHERE is_primary = TRUE;

-- GIN indexes for JSONB
CREATE INDEX idx_vendor_schema ON template_vendor_mapping USING GIN (schema_info);
CREATE INDEX idx_vendor_config ON template_vendor_mapping USING GIN (vendor_config);

-- Ensure only one primary vendor per template version
CREATE UNIQUE INDEX idx_unique_primary_vendor
    ON template_vendor_mapping(template_id, template_version)
    WHERE is_primary = TRUE;

COMMENT ON TABLE template_vendor_mapping IS 'Maps templates to vendor-specific implementations';
COMMENT ON COLUMN template_vendor_mapping.template_content_uri IS 'URI/path to template content in ECMS or external storage (NOT stored as BLOB)';
COMMENT ON COLUMN template_vendor_mapping.is_primary IS 'Indicates the primary vendor for this template version (only one allowed)';
```

---

## Supporting Tables

### 10. Audit Trail

**Purpose:** Comprehensive audit logging for all template changes.

```sql
CREATE TABLE template_audit_trail (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    template_version INTEGER,
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_values JSONB,
    new_values JSONB,
    change_reason TEXT,
    ip_address INET,
    user_agent TEXT,

    CONSTRAINT chk_operation
        CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'ARCHIVE'))
);

CREATE INDEX idx_audit_template ON template_audit_trail(template_id, template_version);
CREATE INDEX idx_audit_changed_at ON template_audit_trail(changed_at DESC);
CREATE INDEX idx_audit_changed_by ON template_audit_trail(changed_by);
CREATE INDEX idx_audit_operation ON template_audit_trail(operation);

COMMENT ON TABLE template_audit_trail IS 'Comprehensive audit trail for all template-related changes';
```

---

### 11. Data Extraction Schema

**Purpose:** Define how data should be extracted from documents.

```sql
CREATE TABLE data_extraction_schema (
    schema_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    extraction_rules JSONB NOT NULL,
    parser_type VARCHAR(50) NOT NULL,
    version INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_parser_type
        CHECK (parser_type IN ('REGEX', 'NLP', 'OCR', 'STRUCTURED_JSON', 'XML_XPATH', 'CUSTOM'))
);

CREATE INDEX idx_extraction_active ON data_extraction_schema(schema_name) WHERE is_active = TRUE;

-- Add FK to master template
ALTER TABLE master_template_definition
    ADD CONSTRAINT fk_extraction_schema
    FOREIGN KEY (data_extraction_schema_id)
    REFERENCES data_extraction_schema(schema_id);

COMMENT ON TABLE data_extraction_schema IS 'Defines data extraction rules for document processing';
```

---

## Helper Functions and Triggers

### Function: Get Active Template Version

```sql
CREATE OR REPLACE FUNCTION get_active_template_version(p_template_id UUID, p_reference_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
RETURNS INTEGER
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_version INTEGER;
BEGIN
    SELECT version INTO v_version
    FROM master_template_definition
    WHERE template_id = p_template_id
      AND effective_date <= p_reference_date
      AND (valid_until IS NULL OR valid_until > p_reference_date)
      AND template_status = 'APPROVED'
    ORDER BY version DESC
    LIMIT 1;

    RETURN v_version;
END;
$$;

COMMENT ON FUNCTION get_active_template_version IS 'Returns the active version number for a template at a given date';
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

### Function: Audit Trail Trigger

```sql
CREATE OR REPLACE FUNCTION audit_template_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO template_audit_trail (
            template_id, template_version, table_name, operation,
            changed_by, old_values
        ) VALUES (
            OLD.template_id, OLD.version, TG_TABLE_NAME, TG_OP,
            OLD.updated_by, row_to_json(OLD)
        );
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO template_audit_trail (
            template_id, template_version, table_name, operation,
            changed_by, old_values, new_values
        ) VALUES (
            NEW.template_id, NEW.version, TG_TABLE_NAME, TG_OP,
            NEW.updated_by, row_to_json(OLD), row_to_json(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO template_audit_trail (
            template_id, template_version, table_name, operation,
            changed_by, new_values
        ) VALUES (
            NEW.template_id, NEW.version, TG_TABLE_NAME, TG_OP,
            NEW.created_by, row_to_json(NEW)
        );
        RETURN NEW;
    END IF;
END;
$$;

CREATE TRIGGER trg_audit_master_template
    AFTER INSERT OR UPDATE OR DELETE ON master_template_definition
    FOR EACH ROW
    EXECUTE FUNCTION audit_template_changes();
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
          AND template_vendor_id != NEW.template_vendor_id;

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

## Common Queries

### Get Active Template with All Details

```sql
SELECT
    mtd.*,
    dc.category_name,
    des.schema_name as extraction_schema_name,
    array_agg(DISTINCT tc.channel_type) as channels,
    array_agg(DISTINCT tac.role_name || ':' || tac.action) as access_controls
FROM master_template_definition mtd
LEFT JOIN document_categories dc ON mtd.category_id = dc.category_id
LEFT JOIN data_extraction_schema des ON mtd.data_extraction_schema_id = des.schema_id
LEFT JOIN template_channels tc ON mtd.template_id = tc.template_id AND mtd.version = tc.version
LEFT JOIN template_access_control tac ON mtd.template_id = tac.template_id AND mtd.version = tac.version
WHERE mtd.template_id = 'your-template-id'
  AND mtd.effective_date <= CURRENT_TIMESTAMP
  AND (mtd.valid_until IS NULL OR mtd.valid_until > CURRENT_TIMESTAMP)
  AND mtd.template_status = 'APPROVED'
GROUP BY mtd.template_id, mtd.version, dc.category_name, des.schema_name
ORDER BY mtd.version DESC
LIMIT 1;
```

### Get Documents for an Account

```sql
SELECT
    si.storage_index_id,
    si.file_name,
    si.doc_creation_date,
    mtd.template_name,
    dc.category_name,
    si.doc_metadata,
    si.storage_document_key
FROM storage_index si
JOIN master_template_definition mtd
    ON si.template_id = mtd.template_id
    AND si.template_version = mtd.version
JOIN document_categories dc
    ON mtd.category_id = dc.category_id
WHERE si.account_key = 'account-uuid'
  AND si.is_accessible = TRUE
  AND si.archive_indicator = FALSE
ORDER BY si.doc_creation_date DESC;
```

### Get Primary Vendor for Template

```sql
SELECT
    tvm.*
FROM template_vendor_mapping tvm
WHERE tvm.template_id = 'your-template-id'
  AND tvm.template_version = get_active_template_version('your-template-id')
  AND tvm.is_primary = TRUE
  AND tvm.is_active = TRUE;
```

---

## Migration Notes

### Key Changes from Original Schema:

1. **Composite Primary Key**: `(template_id, version)` instead of just `template_id`
2. **Normalized Tables**: Separated JSONB data into proper tables
3. **Consistent Types**: All timestamps use TIMESTAMPTZ
4. **Foreign Keys**: Composite FKs in storage_index and template_vendor_mapping
5. **Removed Redundancies**: Eliminated duplicate status/version columns
6. **Added Constraints**: CHECK constraints for data validation
7. **Better Indexing**: Targeted indexes for common queries
8. **Audit Trail**: Proper audit table with triggers

### Migration Steps:

1. Create all lookup tables first (document_categories, data_extraction_schema)
2. Create master_template_definition with composite PK
3. Migrate existing template data, creating version 1 for all
4. Create normalized tables (access_control, channels, etc.)
5. Migrate JSONB data into normalized tables
6. Update storage_index with template_version column
7. Update template_vendor_mapping with template_version column
8. Create indexes and triggers
9. Validate data integrity
10. Update application code to handle composite keys

---

## Performance Considerations

1. **Partitioning**: Consider partitioning storage_index by doc_creation_date for large volumes
2. **Materialized Views**: Refresh storage_index_by_account regularly
3. **Index Maintenance**: Regular VACUUM and ANALYZE on high-traffic tables
4. **JSONB Indexing**: Use GIN indexes sparingly, only on frequently queried JSONB fields
5. **Connection Pooling**: Use pgBouncer or similar for connection management

---

## Security Recommendations

1. **Row-Level Security**: Implement RLS policies for multi-tenant access
2. **Encryption**: Enable encryption at rest for sensitive columns
3. **Audit Retention**: Archive audit_trail data after 7 years
4. **Access Control**: Use PostgreSQL roles matching template_access_control roles
5. **PII Handling**: Add data classification columns if storing PII in doc_metadata

---

**Version**: 2.0
**Date**: 2025-11-06
**Status**: Redesigned Schema
