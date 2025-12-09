# Schema Analysis: Template Versioning & Vendor Mapping

This document analyzes the current database schema for managing template versions and vendor mappings, identifies gaps, and provides recommendations for improvement.

---

## Table of Contents

1. [Current Schema Overview](#1-current-schema-overview)
2. [What's Working Well](#2-whats-working-well)
3. [Issues & Gaps Identified](#3-issues--gaps-identified)
4. [Recommended Schema Improvements](#4-recommended-schema-improvements)
5. [Migration Strategy](#5-migration-strategy)

---

## 1. Current Schema Overview

### Table Relationships

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CURRENT SCHEMA RELATIONSHIPS                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  master_template_definition                                                  │
│  ┌─────────────────────────────┐                                            │
│  │ PK: (master_template_id,    │                                            │
│  │     template_version)       │◄──────────────────────┐                    │
│  │                             │                        │                    │
│  │ template_name               │                        │                    │
│  │ template_type               │                        │                    │
│  │ active_flag                 │                        │                    │
│  │ start_date / end_date       │                        │                    │
│  │ data_extraction_config      │                        │                    │
│  │ template_config             │                        │                    │
│  │ ...                         │                        │                    │
│  └─────────────────────────────┘                        │                    │
│           │                                              │                    │
│           │ FK                                           │ FK                 │
│           ▼                                              │                    │
│  ┌─────────────────────────────┐      ┌─────────────────┴───────────────┐   │
│  │ storage_index               │      │ template_vendor_mapping          │   │
│  │                             │      │                                  │   │
│  │ PK: storage_index_id        │      │ PK: template_vendor_id           │   │
│  │ FK: (master_template_id,    │      │ FK: (master_template_id,         │   │
│  │     template_version)       │      │     template_version)            │   │
│  │                             │      │                                  │   │
│  │ account_key                 │      │ vendor                           │   │
│  │ customer_key                │      │ vendor_template_key              │   │
│  │ storage_document_key        │      │ vendor_mapping_version           │   │
│  │ generation_vendor_id ───────┼──?──▶│ primary_flag                     │   │
│  │ reference_key               │      │ vendor_config (JSON)             │   │
│  │ doc_metadata                │      │ api_config (JSON)                │   │
│  └─────────────────────────────┘      │ template_content                 │   │
│                                        └──────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Columns by Table

#### master_template_definition

| Column | Type | Purpose |
|--------|------|---------|
| `master_template_id` | UUID | Template identifier |
| `template_version` | INTEGER | Version number (1, 2, 3...) |
| `template_name` | VARCHAR | Human-readable name |
| `template_type` | VARCHAR | Document type classification |
| `active_flag` | BOOLEAN | Is this version active? |
| `start_date` | BIGINT | When this version becomes valid |
| `end_date` | BIGINT | When this version expires |
| `version_number` | BIGINT | Optimistic locking counter |
| `archive_indicator` | BOOLEAN | Soft delete flag |

#### template_vendor_mapping

| Column | Type | Purpose |
|--------|------|---------|
| `template_vendor_id` | UUID | Mapping identifier |
| `master_template_id` | UUID | FK to template |
| `template_version` | INTEGER | FK to template version |
| `vendor` | VARCHAR | Vendor name/code |
| `vendor_template_key` | VARCHAR | Vendor's internal template ID |
| `vendor_mapping_version` | INTEGER | Mapping version |
| `primary_flag` | BOOLEAN | Is this the primary vendor? |
| `vendor_config` | JSONB | Vendor-specific settings |
| `api_config` | JSONB | API connection settings |
| `template_content` | BYTEA | Actual template content |

#### storage_index

| Column | Type | Purpose |
|--------|------|---------|
| `storage_index_id` | UUID | Document identifier |
| `master_template_id` | UUID | FK to template |
| `template_version` | INTEGER | FK to template version |
| `generation_vendor_id` | UUID | Which vendor generated this? |
| `storage_vendor` | VARCHAR | Where document is stored |
| `storage_document_key` | UUID | Storage location reference |

---

## 2. What's Working Well

| Aspect | Implementation | Assessment |
|--------|----------------|------------|
| **Template Versioning** | Composite PK `(master_template_id, template_version)` | ✅ Good - allows multiple versions of same template |
| **Explicit Version Numbers** | `template_version` integer (1, 2, 3...) | ✅ Good - clear, sequential versioning |
| **Time-Bounded Validity** | `start_date`, `end_date` columns | ✅ Good - versions can be scheduled |
| **Quick Toggle** | `active_flag` boolean | ✅ Good - enable/disable without deletion |
| **Version-Specific Vendor Mapping** | FK to `(master_template_id, template_version)` | ✅ Good - vendor config tied to specific version |
| **Multi-Vendor Support** | One template version → many vendor mappings | ✅ Good - supports multiple vendors |
| **Primary Vendor Indicator** | `primary_flag` boolean | ✅ Good - identifies default vendor |
| **Flexible Vendor Config** | `vendor_config` JSONB | ✅ Good - vendor-specific settings |
| **Soft Delete** | `archive_indicator` boolean | ✅ Good - audit trail preserved |

### Example: How Versioning Works Today

```sql
-- Template "Privacy Policy" with 3 versions
master_template_id = '11111111-1111-1111-1111-111111111111'

-- Version 1: Original (archived)
template_version = 1, active_flag = false, archive_indicator = true

-- Version 2: Minor update (inactive)
template_version = 2, active_flag = false, archive_indicator = false

-- Version 3: Current (active)
template_version = 3, active_flag = true, archive_indicator = false
```

---

## 3. Issues & Gaps Identified

### Issue 1: No Template Workflow Status

**Problem:** No column to track template lifecycle state for approval workflow.

```sql
-- Current: Only boolean active_flag
active_flag boolean  -- true or false only

-- What's needed: Workflow state tracking
-- DRAFT → IN_REVIEW → APPROVED → ACTIVE → DEPRECATED → ARCHIVED
```

**Impact:**
- Cannot implement approval workflow
- Cannot distinguish between "inactive because not approved yet" vs "inactive because deprecated"
- No visibility into templates pending review

**Current Workaround:** None - workflow not supported.

---

### Issue 2: Confusing Version Columns

**Problem:** Multiple columns with "version" in the name serving different purposes.

| Column | Table | Actual Purpose | Confusion Level |
|--------|-------|----------------|-----------------|
| `template_version` | master_template_definition | Business version (user-facing) | ✅ Clear |
| `version_number` | master_template_definition | Optimistic locking for concurrency | ❓ Misleading name |
| `template_version` | template_vendor_mapping | FK reference to template | ✅ Clear |
| `vendor_mapping_version` | template_vendor_mapping | Version of the mapping itself | ❓ When would this differ? |
| `version_number` | template_vendor_mapping | Optimistic locking | ❓ Misleading name |
| `version_number` | storage_index | Optimistic locking | ❓ Misleading name |

**Recommendation:**
- Rename `version_number` → `optimistic_lock_version` or `row_version`
- Clarify or remove `vendor_mapping_version` if redundant

---

### Issue 3: No "Latest Version" Indicator

**Problem:** Finding the latest version requires a subquery or window function.

```sql
-- Current approach: Subquery needed
SELECT * FROM master_template_definition t1
WHERE template_version = (
    SELECT MAX(template_version)
    FROM master_template_definition t2
    WHERE t2.master_template_id = t1.master_template_id
);

-- Better approach: Direct flag
SELECT * FROM master_template_definition
WHERE is_latest_version = true;
```

**Impact:**
- More complex queries
- Potential performance issues at scale
- Easy to accidentally show old versions

---

### Issue 4: Vendor Type Not Distinguished

**Problem:** The `vendor` column is just a name - no categorization by vendor type.

```sql
-- Current: Just a string
vendor = 'VendorA'      -- Is this print? email? generation?
vendor = 'SendGrid'     -- Probably email, but not explicit
vendor = 'ECMS'         -- Storage? Generation?
```

**Impact:**
- Cannot query "all email vendors for this template"
- Cannot implement channel-specific routing
- Vendor selection logic must hardcode vendor names

**Needed:**
```sql
vendor_type VARCHAR  -- GENERATION, PRINT, EMAIL, SMS, PUSH, STORAGE
```

---

### Issue 5: No Vendor Priority/Failover Order

**Problem:** Only `primary_flag` exists - binary choice, no fallback chain.

```sql
-- Current: Only one primary
primary_flag = true   -- This is THE vendor
primary_flag = false  -- These are... backups? alternatives?

-- What's needed: Priority order
priority_order = 1  -- First choice
priority_order = 2  -- First fallback
priority_order = 3  -- Second fallback
```

**Impact:**
- Cannot implement automatic failover
- Cannot define routing preferences
- Manual intervention needed when primary vendor fails

---

### Issue 6: generation_vendor_id Not Constrained

**Problem:** `storage_index.generation_vendor_id` has no FK constraint.

```sql
-- Current: Just a UUID, no enforcement
generation_vendor_id uuid  -- Could be any value, even invalid

-- Should be: Constrained reference
FOREIGN KEY (generation_vendor_id)
REFERENCES template_vendor_mapping(template_vendor_id)
```

**Impact:**
- Data integrity not enforced
- Cannot reliably trace which vendor generated a document
- Orphaned references possible

---

### Issue 7: No Centralized Vendor Definition

**Problem:** Vendor details are scattered across `template_vendor_mapping` records.

```sql
-- Current: Each mapping has its own vendor config
-- Mapping 1: vendor='SendGrid', api_config={...sendgrid settings...}
-- Mapping 2: vendor='SendGrid', api_config={...same settings duplicated...}
-- Mapping 3: vendor='SendGrid', api_config={...possibly different!...}
```

**Impact:**
- Vendor credentials duplicated across mappings
- Inconsistent configuration possible
- No single source of truth for vendor health/status
- Difficult to update vendor settings globally

---

### Issue 8: No Vendor Health Tracking

**Problem:** No way to know if a vendor is available or degraded.

```sql
-- Missing entirely:
vendor_status VARCHAR        -- ACTIVE, DEGRADED, DOWN, MAINTENANCE
last_health_check TIMESTAMP
success_rate DECIMAL
avg_response_time_ms INTEGER
circuit_breaker_state VARCHAR -- CLOSED, OPEN, HALF_OPEN
```

**Impact:**
- Cannot implement intelligent routing based on health
- No visibility into vendor performance
- Manual monitoring required

---

## 4. Recommended Schema Improvements

### Option A: Minimal Changes (Quick Wins)

Add columns to existing tables without structural changes.

```sql
-- ============================================================
-- 1. Add template workflow status
-- ============================================================
ALTER TABLE document_hub.master_template_definition
ADD COLUMN template_status VARCHAR(20) DEFAULT 'DRAFT';

COMMENT ON COLUMN document_hub.master_template_definition.template_status IS
'Workflow status: DRAFT, IN_REVIEW, APPROVED, ACTIVE, DEPRECATED, ARCHIVED';

-- Add check constraint for valid values
ALTER TABLE document_hub.master_template_definition
ADD CONSTRAINT chk_template_status
CHECK (template_status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'ACTIVE', 'DEPRECATED', 'ARCHIVED'));

-- ============================================================
-- 2. Add latest version flag
-- ============================================================
ALTER TABLE document_hub.master_template_definition
ADD COLUMN is_latest_version BOOLEAN DEFAULT true;

COMMENT ON COLUMN document_hub.master_template_definition.is_latest_version IS
'True if this is the most recent version of the template';

-- Index for fast latest version queries
CREATE INDEX idx_template_latest
ON document_hub.master_template_definition(master_template_id, is_latest_version)
WHERE is_latest_version = true;

-- ============================================================
-- 3. Add vendor type to mapping
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN vendor_type VARCHAR(20) NOT NULL DEFAULT 'GENERATION';

COMMENT ON COLUMN document_hub.template_vendor_mapping.vendor_type IS
'Type of vendor: GENERATION, PRINT, EMAIL, SMS, PUSH, STORAGE';

ALTER TABLE document_hub.template_vendor_mapping
ADD CONSTRAINT chk_vendor_type
CHECK (vendor_type IN ('GENERATION', 'PRINT', 'EMAIL', 'SMS', 'PUSH', 'STORAGE'));

-- ============================================================
-- 4. Add priority order for failover
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN priority_order INTEGER DEFAULT 1;

COMMENT ON COLUMN document_hub.template_vendor_mapping.priority_order IS
'Routing priority: 1 = first choice, 2 = first fallback, etc.';

-- ============================================================
-- 5. Add FK constraint for generation_vendor_id
-- ============================================================
ALTER TABLE document_hub.storage_index
ADD CONSTRAINT fk_storage_generation_vendor
FOREIGN KEY (generation_vendor_id)
REFERENCES document_hub.template_vendor_mapping(template_vendor_id);

-- ============================================================
-- 6. Add composite index for vendor routing queries
-- ============================================================
CREATE INDEX idx_vendor_mapping_routing
ON document_hub.template_vendor_mapping(
    master_template_id,
    template_version,
    vendor_type,
    priority_order
) WHERE active_flag = true;

-- ============================================================
-- 7. Rename confusing version_number columns (optional)
-- ============================================================
ALTER TABLE document_hub.master_template_definition
RENAME COLUMN version_number TO optimistic_lock_version;

ALTER TABLE document_hub.template_vendor_mapping
RENAME COLUMN version_number TO optimistic_lock_version;

ALTER TABLE document_hub.storage_index
RENAME COLUMN version_number TO optimistic_lock_version;
```

---

### Option B: Add Vendor Definition Table (Recommended)

Create a centralized vendor registry for better vendor management.

```sql
-- ============================================================
-- New Table: vendor_definition
-- ============================================================
CREATE TABLE document_hub.vendor_definition (
    vendor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identification
    vendor_code VARCHAR(50) NOT NULL UNIQUE,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_type VARCHAR(20) NOT NULL,

    -- Connection Configuration
    base_url VARCHAR(500),
    auth_type VARCHAR(20),              -- API_KEY, OAUTH2, BASIC, NONE
    credentials_vault_path VARCHAR(255), -- Path in secrets manager
    default_timeout_ms INTEGER DEFAULT 30000,

    -- Health & Status
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_health_check TIMESTAMP,
    health_check_endpoint VARCHAR(255),
    circuit_breaker_state VARCHAR(20) DEFAULT 'CLOSED',

    -- Capabilities (JSONB for flexibility)
    capabilities JSONB,
    /* Example:
    {
        "supportedFormats": ["PDF", "HTML", "DOCX"],
        "supportedRegions": ["US", "CA", "UK"],
        "maxFileSizeMb": 25,
        "supportsAsync": true,
        "supportsBatch": true,
        "batchSizeLimit": 1000
    }
    */

    -- Rate Limiting
    rate_limit_per_minute INTEGER,
    rate_limit_per_day INTEGER,

    -- Cost Tracking
    cost_per_unit DECIMAL(10, 4),
    cost_unit VARCHAR(20),              -- PER_DOCUMENT, PER_PAGE, PER_MB

    -- Audit
    active_flag BOOLEAN DEFAULT true,
    created_by VARCHAR(100) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT now(),
    updated_by VARCHAR(100),
    updated_timestamp TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_vendor_type
        CHECK (vendor_type IN ('GENERATION', 'PRINT', 'EMAIL', 'SMS', 'PUSH', 'STORAGE')),
    CONSTRAINT chk_vendor_status
        CHECK (status IN ('ACTIVE', 'DEGRADED', 'DOWN', 'MAINTENANCE')),
    CONSTRAINT chk_auth_type
        CHECK (auth_type IN ('API_KEY', 'OAUTH2', 'BASIC', 'NONE', 'CUSTOM'))
);

-- Indexes
CREATE INDEX idx_vendor_type ON document_hub.vendor_definition(vendor_type);
CREATE INDEX idx_vendor_status ON document_hub.vendor_definition(status);
CREATE INDEX idx_vendor_active ON document_hub.vendor_definition(active_flag) WHERE active_flag = true;

-- ============================================================
-- Update template_vendor_mapping to reference vendor_definition
-- ============================================================
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN vendor_id UUID;

ALTER TABLE document_hub.template_vendor_mapping
ADD CONSTRAINT fk_vendor_definition
FOREIGN KEY (vendor_id)
REFERENCES document_hub.vendor_definition(vendor_id);

-- Index for vendor lookups
CREATE INDEX idx_mapping_vendor_id
ON document_hub.template_vendor_mapping(vendor_id);
```

---

### Option C: Full Schema (Complete Solution)

Includes approval workflow tables and vendor health tracking.

```sql
-- ============================================================
-- Approval Workflow Tables
-- ============================================================

-- Workflow definition (types of approval processes)
CREATE TABLE document_hub.approval_workflow_definition (
    workflow_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_name VARCHAR(100) NOT NULL,
    workflow_type VARCHAR(50) NOT NULL,     -- STANDARD, EXPRESS, COMPLIANCE
    description VARCHAR(500),
    steps JSONB NOT NULL,
    /* Example steps:
    [
        {"order": 1, "role": "REVIEWER", "action": "REVIEW", "required": true},
        {"order": 2, "role": "APPROVER", "action": "APPROVE", "required": true}
    ]
    */
    active_flag BOOLEAN DEFAULT true,
    created_by VARCHAR(100) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT now()
);

-- Approval request (workflow instance)
CREATE TABLE document_hub.approval_request (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- What's being approved
    master_template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,

    -- Workflow reference
    workflow_id UUID NOT NULL REFERENCES document_hub.approval_workflow_definition(workflow_id),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, APPROVED, REJECTED, CANCELLED
    current_step INTEGER DEFAULT 1,

    -- Submitter
    submitted_by VARCHAR(100) NOT NULL,
    submitted_timestamp TIMESTAMP DEFAULT now(),
    submission_notes TEXT,

    -- Completion
    completed_timestamp TIMESTAMP,
    final_decision VARCHAR(20),             -- APPROVED, REJECTED
    final_decision_by VARCHAR(100),

    -- Foreign key to template
    CONSTRAINT fk_approval_template
        FOREIGN KEY (master_template_id, template_version)
        REFERENCES document_hub.master_template_definition(master_template_id, template_version)
);

-- Approval step history
CREATE TABLE document_hub.approval_step_history (
    step_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES document_hub.approval_request(request_id),

    step_number INTEGER NOT NULL,
    action_taken VARCHAR(20) NOT NULL,      -- APPROVED, REJECTED, RETURNED
    action_by VARCHAR(100) NOT NULL,
    action_timestamp TIMESTAMP DEFAULT now(),
    comments TEXT
);

-- Approval comments/feedback
CREATE TABLE document_hub.approval_comment (
    comment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES document_hub.approval_request(request_id),

    comment_type VARCHAR(20) NOT NULL,      -- FEEDBACK, QUESTION, RESPONSE
    comment_text TEXT NOT NULL,
    section_reference VARCHAR(100),         -- Which part of template

    created_by VARCHAR(100) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT now(),
    resolved_flag BOOLEAN DEFAULT false,
    resolved_by VARCHAR(100),
    resolved_timestamp TIMESTAMP
);

-- ============================================================
-- Vendor Health Tracking
-- ============================================================

CREATE TABLE document_hub.vendor_health_log (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL REFERENCES document_hub.vendor_definition(vendor_id),

    check_timestamp TIMESTAMP DEFAULT now(),
    status VARCHAR(20) NOT NULL,            -- SUCCESS, FAILURE, TIMEOUT, DEGRADED
    response_time_ms INTEGER,
    error_message TEXT,

    -- Metrics
    success_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    avg_response_time_ms INTEGER
);

-- Keep only recent health logs (partition or cleanup job)
CREATE INDEX idx_vendor_health_timestamp
ON document_hub.vendor_health_log(vendor_id, check_timestamp DESC);
```

---

## 5. Migration Strategy

### Phase 1: Non-Breaking Additions

These changes add new columns with defaults - no application changes required.

```sql
-- Safe to run immediately
ALTER TABLE document_hub.master_template_definition
ADD COLUMN template_status VARCHAR(20) DEFAULT 'ACTIVE';

ALTER TABLE document_hub.master_template_definition
ADD COLUMN is_latest_version BOOLEAN DEFAULT true;

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN vendor_type VARCHAR(20) DEFAULT 'GENERATION';

ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN priority_order INTEGER DEFAULT 1;
```

### Phase 2: Data Migration

Update existing records with appropriate values.

```sql
-- Set all current active templates to ACTIVE status
UPDATE document_hub.master_template_definition
SET template_status = 'ACTIVE'
WHERE active_flag = true AND archive_indicator = false;

-- Set archived templates to ARCHIVED status
UPDATE document_hub.master_template_definition
SET template_status = 'ARCHIVED'
WHERE archive_indicator = true;

-- Mark latest versions
WITH latest_versions AS (
    SELECT master_template_id, MAX(template_version) as max_version
    FROM document_hub.master_template_definition
    GROUP BY master_template_id
)
UPDATE document_hub.master_template_definition t
SET is_latest_version = true
FROM latest_versions lv
WHERE t.master_template_id = lv.master_template_id
AND t.template_version = lv.max_version;

UPDATE document_hub.master_template_definition
SET is_latest_version = false
WHERE is_latest_version IS NULL;
```

### Phase 3: New Tables

Create new tables after application is ready to use them.

```sql
-- Create vendor_definition table
-- Create approval workflow tables
-- Migrate vendor data from template_vendor_mapping.vendor_config
```

### Phase 4: Constraints & Cleanup

Add constraints and remove deprecated columns after migration verified.

```sql
-- Add FK constraint for generation_vendor_id
-- Add check constraints
-- Consider removing redundant columns
```

---

## Summary

### Current State Assessment

| Aspect | Status | Notes |
|--------|--------|-------|
| Template Versioning | ✅ Good | Composite PK works well |
| Version Validity | ✅ Good | start_date/end_date support |
| Workflow Status | ❌ Missing | Blocks approval workflow |
| Latest Version Query | 🟡 Inefficient | Needs flag or view |
| Vendor Mapping | ✅ Good | FK to specific version |
| Vendor Types | ❌ Missing | Cannot categorize vendors |
| Vendor Failover | 🟡 Limited | Only primary_flag |
| Vendor Registry | ❌ Missing | No centralized vendor config |
| Vendor Health | ❌ Missing | No health tracking |

### Recommended Priority

| Priority | Change | Effort | Impact |
|----------|--------|--------|--------|
| **P1** | Add `template_status` | Low | Enables approval workflow |
| **P1** | Add `vendor_type` | Low | Enables multi-channel routing |
| **P2** | Add `priority_order` | Low | Enables vendor failover |
| **P2** | Add `is_latest_version` | Low | Improves query performance |
| **P2** | Create `vendor_definition` | Medium | Centralizes vendor config |
| **P3** | Add FK constraint | Low | Improves data integrity |
| **P3** | Create approval tables | Medium | Full workflow support |
| **P4** | Add vendor health tracking | Medium | Enables intelligent routing |

---

## Related Documentation

- [Gap Analysis](./gap-analysis.md) - Overall feature gaps
- [Business Use Cases](./business-use-cases-comprehensive.md) - Full use case list
- [Document Enquiry Flow](./document-enquiry-flow.md) - Current implementation flow
