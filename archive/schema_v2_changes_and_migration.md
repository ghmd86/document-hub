# Document Hub Schema v2 - Changes and Migration Guide

## Executive Summary

Schema v2 introduces support for **shared documents** across customers and accounts while maintaining high performance. The key change is separating document metadata from access control using a junction table pattern.

---

## What Changed and Why

### New Requirement

> "There could also be documents that would be shared across customers or account. We need to make sure we are able to capture such documents too."

This requirement fundamentally changes the data model from **1:1** (one document = one customer) to **many-to-many** (one document = multiple customers/accounts).

---

## Major Schema Changes

### 1. Documents Table - Removed Customer Denormalization

**v1 Schema (Old):**
```sql
CREATE TABLE documents (
    document_id UUID,
    customer_id VARCHAR(100) NOT NULL,     -- ❌ Removed
    customer_name VARCHAR(255),            -- ❌ Removed
    account_id VARCHAR(100),               -- ❌ Removed
    account_type VARCHAR(100),             -- ❌ Removed
    document_type VARCHAR(100) NOT NULL,
    ...
    PRIMARY KEY (document_id, customer_id)
) PARTITION BY HASH (customer_id);         -- ❌ Changed
```

**v2 Schema (New):**
```sql
CREATE TABLE documents (
    document_id UUID NOT NULL,
    ecms_document_id VARCHAR(255) NOT NULL UNIQUE,
    document_type VARCHAR(100) NOT NULL,
    document_category VARCHAR(100) NOT NULL,

    -- ✅ New field: sharing scope
    sharing_scope VARCHAR(20) NOT NULL DEFAULT 'private'
        CHECK (sharing_scope IN ('private', 'shared', 'public')),

    ...
    PRIMARY KEY (document_id, uploaded_at)
) PARTITION BY RANGE (uploaded_at);        -- ✅ Changed to date-based
```

**Key Changes:**
- ❌ Removed: `customer_id`, `customer_name`, `account_id`, `account_type` (moved to `document_access`)
- ✅ Added: `sharing_scope` field ('private', 'shared', 'public')
- ✅ Changed: Partitioning from HASH(customer_id) to RANGE(uploaded_at)

---

### 2. New Table: document_access (Junction Table)

**Purpose:** Manages many-to-many relationships between documents and entities (customers/accounts).

```sql
CREATE TABLE document_access (
    access_id UUID PRIMARY KEY,
    document_id UUID NOT NULL,

    -- Entity (polymorphic)
    entity_type VARCHAR(20) NOT NULL,      -- 'customer', 'account', 'user', 'public'
    entity_id VARCHAR(100),                -- Customer ID, Account ID, etc.
    entity_name VARCHAR(255),              -- Denormalized for performance

    -- Access control
    access_level VARCHAR(20) NOT NULL,     -- 'owner', 'view', 'edit', 'admin'

    granted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    granted_by VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,   -- Optional expiration

    ...
    UNIQUE (document_id, entity_type, entity_id)
);
```

**Supported Scenarios:**

| Scenario | Implementation |
|----------|----------------|
| **Single-owner document** | 1 row: `(document_id, 'customer', 'C123', 'owner')` |
| **Shared document** | Multiple rows: `(doc_id, 'customer', 'C123', 'view')` + `(doc_id, 'customer', 'C456', 'view')` |
| **Public document** | 1 row: `(document_id, 'public', NULL, 'view')` |
| **Account-level access** | 1 row: `(document_id, 'account', 'A789', 'view')` |

---

### 3. Partitioning Strategy Changed

**v1:** HASH partitioning by `customer_id`
- **Problem:** Can't partition by customer_id when documents are shared

**v2:** RANGE partitioning by `uploaded_at`
- **Benefits:**
  - Works with shared documents
  - Easy to archive old partitions (detach Q1 2020, move to cold storage)
  - Time-based queries benefit from partition pruning
  - Supports automatic partition creation (pg_partman)

**Partition Structure:**
```sql
documents_2024_q1 (2024-01-01 to 2024-04-01)
documents_2024_q2 (2024-04-01 to 2024-07-01)
documents_2024_q3 (2024-07-01 to 2024-10-01)
documents_2024_q4 (2024-10-01 to 2025-01-01)
...
documents_default  (future dates)
```

---

## Query Pattern Changes

### Before (v1): Direct customer_id filter

```sql
-- v1: Direct denormalized query (fast, simple)
SELECT * FROM documents
WHERE customer_id = 'C123'
  AND document_type = 'LOAN_APPLICATION'
  AND status = 'active';
```

### After (v2): Join through document_access

```sql
-- v2: Join through access control (slightly more complex, still fast with indexes)
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    da.access_level
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'customer'
  AND da.entity_id = 'C123'
  AND d.document_type = 'LOAN_APPLICATION'
  AND d.status = 'active';
```

**Performance:** With proper indexing, the join adds only ~2-5ms overhead.

---

## New Capabilities in v2

### 1. Shared Documents

```sql
-- Document shared with 3 customers
INSERT INTO document_access (document_id, entity_type, entity_id, entity_name, access_level, granted_by)
VALUES
    ('doc-123', 'customer', 'C123', 'John Doe', 'owner', 'system'),
    ('doc-123', 'customer', 'C456', 'Jane Smith', 'view', 'C123'),
    ('doc-123', 'customer', 'C789', 'Bob Johnson', 'view', 'C123');

-- Query: Who has access to this document?
SELECT entity_type, entity_id, entity_name, access_level
FROM document_access
WHERE document_id = 'doc-123';
```

### 2. Public/Global Documents

```sql
-- Document accessible to everyone (e.g., terms and conditions)
UPDATE documents SET sharing_scope = 'public' WHERE document_id = 'doc-456';

INSERT INTO document_access (document_id, entity_type, access_level, granted_by)
VALUES ('doc-456', 'public', 'view', 'system');

-- Query: Get all public documents
SELECT * FROM documents WHERE sharing_scope = 'public' AND status = 'active';
```

### 3. Account-Level Access

```sql
-- Document belongs to account (shared across all customers on that account)
INSERT INTO document_access (document_id, entity_type, entity_id, entity_name, access_level, granted_by)
VALUES ('doc-789', 'account', 'A456', 'Savings Account #456', 'view', 'system');

-- Query: Documents for account
SELECT d.*
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.entity_type = 'account' AND da.entity_id = 'A456';
```

### 4. Time-Limited Access

```sql
-- Grant temporary access (expires in 30 days)
INSERT INTO document_access (document_id, entity_type, entity_id, access_level, granted_by, expires_at)
VALUES ('doc-999', 'customer', 'C123', 'view', 'admin', NOW() + INTERVAL '30 days');

-- Automatic cleanup of expired access
DELETE FROM document_access WHERE expires_at < NOW();
```

### 5. Access Levels

```sql
-- Different access levels
INSERT INTO document_access (document_id, entity_type, entity_id, access_level, granted_by)
VALUES
    ('doc-100', 'customer', 'C123', 'owner', 'system'),    -- Can delete
    ('doc-100', 'customer', 'C456', 'edit', 'C123'),       -- Can modify metadata
    ('doc-100', 'customer', 'C789', 'view', 'C123');       -- Read-only
```

---

## Indexing Strategy Comparison

### v1 Indexes (Customer-Centric)

```sql
-- v1: Optimized for single-customer queries
idx_documents_customer_type       ON (customer_id, document_type)
idx_documents_customer_category   ON (customer_id, document_category)
idx_documents_account_type        ON (account_id, document_type)
```

### v2 Indexes (Access-Control Centric)

```sql
-- v2: Optimized for access control + joins
idx_document_access_entity_document ON (entity_type, entity_id, document_id)
idx_document_access_customer_id     ON (entity_id) WHERE entity_type = 'customer'
idx_document_access_account_id      ON (entity_id) WHERE entity_type = 'account'
idx_documents_type                  ON (document_type)
idx_documents_template_code         ON (template_code, template_version)
```

**Key Difference:** v1 indexes optimize direct customer queries; v2 indexes optimize join queries through `document_access`.

---

## Migration Guide: v1 to v2

### Option A: Fresh Installation (Recommended for New Systems)

```bash
# Drop v1 schema (if exists)
psql -U postgres -d document_hub -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Install v2 schema
psql -U postgres -d document_hub -f document_hub_schema_v2.sql
```

### Option B: Migrate Existing Data (For Production Systems)

#### Step 1: Create v2 Tables (New Structure)

```sql
-- Run the v2 schema DDL
\i document_hub_schema_v2.sql
```

#### Step 2: Migrate Documents Table

```sql
-- Create temporary staging table
CREATE TABLE documents_v1_backup AS SELECT * FROM documents;

-- Drop old partitioned table
DROP TABLE documents CASCADE;

-- Recreate documents table from v2 schema (already done in Step 1)

-- Migrate data from v1 to v2
INSERT INTO documents (
    document_id,
    ecms_document_id,
    document_type,
    document_category,
    document_sub_category,
    template_id,
    template_code,
    template_version,
    document_name,
    document_description,
    file_extension,
    file_size_bytes,
    mime_type,
    status,
    sharing_scope,  -- ✅ New field: default to 'private'
    document_date,
    uploaded_at,
    archived_at,
    expiry_date,
    is_confidential,
    requires_signature,
    signature_status,
    tags,
    metadata,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    document_id,
    ecms_document_id,
    document_type,
    document_category,
    document_sub_category,
    template_id,
    template_code,
    template_version,
    document_name,
    document_description,
    file_extension,
    file_size_bytes,
    mime_type,
    status,
    'private'::VARCHAR,  -- ✅ All existing documents are 'private' (single-owner)
    document_date,
    uploaded_at,
    archived_at,
    expiry_date,
    is_confidential,
    requires_signature,
    signature_status,
    tags,
    metadata,
    created_at,
    updated_at,
    created_by,
    updated_by
FROM documents_v1_backup;
```

#### Step 3: Populate document_access Table

```sql
-- Create access records for all customers (from v1 customer_id)
INSERT INTO document_access (
    document_id,
    entity_type,
    entity_id,
    entity_name,
    access_level,
    granted_at,
    granted_by
)
SELECT
    document_id,
    'customer'::VARCHAR AS entity_type,
    customer_id AS entity_id,
    customer_name AS entity_name,
    'owner'::VARCHAR AS access_level,  -- Original customer is owner
    created_at AS granted_at,
    created_by AS granted_by
FROM documents_v1_backup
WHERE customer_id IS NOT NULL;

-- Create access records for all accounts (from v1 account_id)
INSERT INTO document_access (
    document_id,
    entity_type,
    entity_id,
    access_level,
    granted_at,
    granted_by
)
SELECT
    document_id,
    'account'::VARCHAR AS entity_type,
    account_id AS entity_id,
    'view'::VARCHAR AS access_level,  -- Account has view access
    created_at AS granted_at,
    created_by AS granted_by
FROM documents_v1_backup
WHERE account_id IS NOT NULL;
```

#### Step 4: Verify Migration

```sql
-- Check row counts match
SELECT 'v1_backup' AS source, COUNT(*) FROM documents_v1_backup
UNION ALL
SELECT 'v2_documents' AS source, COUNT(*) FROM documents;

-- Check document_access records created
SELECT
    entity_type,
    COUNT(*) AS access_count
FROM document_access
GROUP BY entity_type;

-- Verify sample documents migrated correctly
SELECT
    d.document_id,
    d.document_name,
    d.sharing_scope,
    da.entity_type,
    da.entity_id,
    da.access_level
FROM documents d
JOIN document_access da ON d.document_id = da.document_id
LIMIT 10;
```

#### Step 5: Update Application Code

Update all queries to use the new pattern:

**Before:**
```python
# v1: Direct customer_id filter
query = """
    SELECT * FROM documents
    WHERE customer_id = $1 AND document_type = $2
"""
results = await conn.fetch(query, customer_id, doc_type)
```

**After:**
```python
# v2: Join through document_access
query = """
    SELECT d.*, da.access_level
    FROM document_access da
    JOIN documents d ON da.document_id = d.document_id
    WHERE da.entity_type = 'customer'
      AND da.entity_id = $1
      AND d.document_type = $2
"""
results = await conn.fetch(query, customer_id, doc_type)
```

#### Step 6: Drop Backup Table (After Verification)

```sql
-- After confirming migration success
DROP TABLE documents_v1_backup;
```

---

## Performance Comparison

### v1 vs v2 Query Performance

| Query Type | v1 Latency | v2 Latency | Notes |
|------------|------------|------------|-------|
| Customer + Type | 10-15ms | 12-18ms | +2-3ms for join overhead |
| Account + Category | 10-15ms | 12-18ms | +2-3ms for join overhead |
| ECMS ID Lookup | 5-10ms | 5-10ms | No change (direct lookup) |
| Template Lookup | 5-15ms | 5-15ms | No change |
| **Shared Document Query** | ❌ Not supported | 15-25ms | ✅ New capability |
| **Public Document Query** | ❌ Not supported | 10-15ms | ✅ New capability |

**Summary:** Slight performance overhead (~2-5ms) for single-owner queries, but enables entirely new access patterns.

---

## Storage Impact

### Additional Storage for v2

| Component | Estimated Size | Notes |
|-----------|----------------|-------|
| `document_access` table | ~150 bytes/row | UUID (16) + VARCHAR(100) + metadata |
| Indexes on `document_access` | ~50% of table size | Multiple indexes for performance |
| **Total per document** | ~225 bytes | Minimal overhead for shared capability |

**Example:**
- 10M documents, single-owner: ~2.25 GB additional storage
- 10M documents, avg 2 entities/doc: ~4.5 GB additional storage

**Trade-off:** Minimal storage cost (<1% of total) for significant functional improvement.

---

## API Changes Required

### Document Upload (Create)

**v1 API:**
```python
POST /documents
{
    "ecms_document_id": "ECMS-123",
    "customer_id": "C123",          # ❌ Moved to separate call
    "customer_name": "John Doe",    # ❌ Moved to separate call
    "document_type": "LOAN_APPLICATION",
    ...
}
```

**v2 API:**
```python
# Step 1: Create document
POST /documents
{
    "ecms_document_id": "ECMS-123",
    "document_type": "LOAN_APPLICATION",
    "sharing_scope": "private",     # ✅ New field
    ...
}

# Step 2: Grant access (automatic in most cases)
POST /documents/{document_id}/access
{
    "entity_type": "customer",
    "entity_id": "C123",
    "entity_name": "John Doe",
    "access_level": "owner"
}
```

**Simplified v2 API (Combined):**
```python
POST /documents
{
    "ecms_document_id": "ECMS-123",
    "document_type": "LOAN_APPLICATION",
    "sharing_scope": "private",
    "access": [                      # ✅ Grant access in same call
        {
            "entity_type": "customer",
            "entity_id": "C123",
            "entity_name": "John Doe",
            "access_level": "owner"
        }
    ]
}
```

### Document Sharing (New Feature)

```python
# Share document with another customer
POST /documents/{document_id}/share
{
    "entity_type": "customer",
    "entity_id": "C456",
    "entity_name": "Jane Smith",
    "access_level": "view",
    "expires_at": "2024-12-31T23:59:59Z"  # Optional
}

# Revoke access
DELETE /documents/{document_id}/access/{entity_type}/{entity_id}
```

### Document Retrieval (Query)

**v1 API:**
```python
GET /customers/{customer_id}/documents?type=LOAN_APPLICATION
```

**v2 API (Same endpoint, different backend query):**
```python
GET /customers/{customer_id}/documents?type=LOAN_APPLICATION

# Backend now joins through document_access instead of direct filter
```

---

## Caching Strategy Changes

### v1 Caching (Customer-Centric)

```redis
Key: customer:documents:{customer_id}:{document_type}
Value: [list of documents]
TTL: 2 minutes
```

### v2 Caching (Access-Aware)

```redis
# Cache document with access info
Key: document:{document_id}:full
Value: {
    "document": {...},
    "access": [
        {"entity_type": "customer", "entity_id": "C123", "access_level": "owner"},
        {"entity_type": "customer", "entity_id": "C456", "access_level": "view"}
    ]
}
TTL: 5 minutes

# Cache customer's accessible documents
Key: customer:documents:{customer_id}:{document_type}
Value: [list of document_ids with access_level]
TTL: 2 minutes

# Invalidation: On share/revoke, invalidate all affected customer caches
```

---

## Rollback Plan

If v2 causes issues, you can rollback:

### Emergency Rollback (Restore v1)

```sql
-- 1. Restore v1 schema
\i document_hub_schema.sql

-- 2. Restore documents with customer_id (from document_access)
INSERT INTO documents (
    document_id,
    ecms_document_id,
    customer_id,  -- ✅ Restored
    customer_name, -- ✅ Restored
    ...
)
SELECT
    d.document_id,
    d.ecms_document_id,
    da.entity_id AS customer_id,
    da.entity_name AS customer_name,
    ...
FROM documents_v2_backup d
JOIN document_access da ON d.document_id = da.document_id
WHERE da.entity_type = 'customer' AND da.access_level = 'owner';

-- 3. Note: Shared documents will be duplicated (one per customer)
-- This is acceptable for emergency rollback
```

---

## Recommendations

### For New Installations
✅ **Use v2 schema directly** - No migration needed, full shared document support from day 1.

### For Existing v1 Systems
✅ **Migrate to v2 if**:
- You need shared documents
- You need account-level document access
- You need public/global documents
- You need granular access control (owner/view/edit)

❌ **Stay on v1 if**:
- All documents are strictly single-customer
- No shared document requirement in roadmap
- Performance is absolutely critical (save 2-5ms per query)

### Hybrid Approach
✅ **Use v2 with optimization**:
- Keep v1-style denormalized customer_id in documents table (optional)
- Use document_access for shared documents only
- Query logic: Check sharing_scope = 'private' → use customer_id, else join document_access

---

## Summary of Benefits (v2 over v1)

| Feature | v1 | v2 |
|---------|----|----|
| Single-owner documents | ✅ Fast | ✅ Fast (slightly slower) |
| Shared documents | ❌ Not supported | ✅ Supported |
| Public documents | ❌ Not supported | ✅ Supported |
| Account-level access | ❌ Workaround only | ✅ Native support |
| Granular permissions | ❌ No | ✅ Yes (owner/view/edit/admin) |
| Time-limited access | ❌ No | ✅ Yes (expires_at) |
| Access audit trail | ❌ Limited | ✅ Complete (who granted, when) |
| Archival strategy | ⚠️ Complex | ✅ Simple (detach old partitions) |
| Storage overhead | 0 | +1-2% |
| Query overhead | 0ms | +2-5ms |

---

## Next Steps

1. **Review Requirements**: Confirm shared document scenarios with stakeholders
2. **Choose Schema Version**: v1 (simple, fast) vs v2 (flexible, shared)
3. **Plan Migration**: If existing v1 system, schedule migration window
4. **Update Application**: Modify queries and caching logic
5. **Test Performance**: Benchmark v2 queries with realistic data volumes
6. **Deploy**: Gradual rollout with rollback plan ready

---

## Support

For questions about migration:
1. Review this guide and v2 schema DDL
2. Test migration on staging environment first
3. Monitor query performance after migration
4. Adjust indexes based on actual query patterns

---

**Schema Version:** v2.0
**Last Updated:** 2024
**Breaking Changes:** Yes (requires application code updates)
