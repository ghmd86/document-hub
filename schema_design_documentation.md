# Document Hub - Schema Design Documentation

## Table of Contents
1. [Overview](#overview)
2. [Design Principles](#design-principles)
3. [Table Architecture](#table-architecture)
4. [Denormalized Versioning Logic](#denormalized-versioning-logic)
5. [Indexing Strategy](#indexing-strategy)
6. [Partitioning Strategy](#partitioning-strategy)
7. [Caching Recommendations](#caching-recommendations)
8. [Performance Optimization](#performance-optimization)
9. [Scalability Considerations](#scalability-considerations)

---

## Overview

The Document Hub is a metadata indexing and retrieval layer built on PostgreSQL that sits on top of an existing Enterprise Content Management System (ECMS). The system is designed to handle millions of document records with high-performance query capabilities.

### Key Objectives
- **Fast Metadata Retrieval**: Sub-100ms query response times for customer/account lookups
- **Template Management**: Flexible template versioning with business rules
- **Shared Document Support**: Store documents once and associate them with multiple customers (e.g., privacy policies, notices)
- **Scalability**: Support for millions of documents across thousands of customers
- **Audit Trail**: Complete tracking of all changes

---

## Design Principles

### 1. Hybrid Normalization Approach

The schema uses a strategic mix of normalization and denormalization:

| Entity | Approach | Rationale |
|--------|----------|-----------|
| **Documents** | Denormalized | Maximize query performance for filtering by customer, account, type, and category |
| **Shared Documents** | Hybrid (Single document + mapping table) | Store one copy of shared documents, use mapping table for customer associations |
| **Templates** | Denormalized Versioning | Each version is a new row (not a separate table), enabling simple joins and queries |
| **Template Rules** | Normalized | Reusable rules can be shared and managed independently |

### 2. Write-Optimized vs Read-Optimized

- **Documents Table**: Read-optimized (90% reads, 10% writes)
  - Denormalized fields reduce joins
  - Composite indexes for common query patterns
  - Partitioning for horizontal scalability

- **Templates Table**: Balanced (60% reads, 40% writes)
  - Versioning as rows (not separate table) simplifies queries
  - Active templates cached aggressively

- **Template Rules Table**: Write-moderate (70% reads, 30% writes)
  - Normalized structure allows rule reuse
  - Foreign key to template_id enables efficient joins

---

## Table Architecture

### 1. Templates Table

```sql
templates (
    template_id UUID PRIMARY KEY,
    template_code VARCHAR(100),      -- Logical identifier (e.g., 'LOAN_APPLICATION')
    version_number INTEGER,           -- Sequential version (1, 2, 3, ...)
    template_name VARCHAR(255),
    document_type VARCHAR(100),
    document_category VARCHAR(100),
    status VARCHAR(20),               -- 'active', 'deprecated', 'draft'
    is_shared_document BOOLEAN DEFAULT false,  -- Indicates if documents from this template are shared
    sharing_scope VARCHAR(50),        -- 'all_customers', 'customer_segment', 'account_type', 'specific_customers', null
    ...
    UNIQUE (template_code, version_number)
)
```

**Key Features:**
- Each row represents a complete template version
- `template_code` groups logical templates
- `version_number` increments with each new version
- Only one version can be `active` per template_code (enforced at application level)
- **Shared Document Support**: `is_shared_document` flag dictates whether documents created from this template are shared across customers
- **Sharing Scope**: Defines the sharing pattern (all customers, specific segment, account type, etc.)

### 2. Template Rules Table

```sql
template_rules (
    rule_id UUID PRIMARY KEY,
    template_id UUID REFERENCES templates(template_id),
    rule_name VARCHAR(255),
    rule_type VARCHAR(50),            -- 'validation', 'transformation', 'business_logic'
    rule_expression TEXT,             -- SQL, regex, or custom DSL
    execution_order INTEGER,
    is_active BOOLEAN,
    ...
)
```

**Key Features:**
- Normalized: Rules belong to specific template versions
- When a template is versioned, rules can be copied or inherited
- Allows rule reuse across versions via application logic

### 3. Documents Table

```sql
documents (
    document_id UUID,
    ecms_document_id VARCHAR(255) UNIQUE,
    is_shared BOOLEAN DEFAULT false,  -- Indicates if this is a shared document
    sharing_scope VARCHAR(50),        -- 'all_customers', 'customer_segment', 'account_type', 'specific_customers', null
    customer_id VARCHAR(100),         -- Partition key (NULL for shared documents with scope 'all_customers')
    customer_name VARCHAR(255),       -- Denormalized (NULL for shared documents)
    account_id VARCHAR(100),          -- Denormalized (NULL for shared documents)
    account_type VARCHAR(100),        -- Denormalized
    document_type VARCHAR(100),       -- Denormalized
    document_category VARCHAR(100),   -- Denormalized
    template_id UUID REFERENCES templates(template_id),
    template_code VARCHAR(100),       -- Denormalized for faster queries
    template_version INTEGER,         -- Denormalized
    effective_from TIMESTAMP,         -- When this document becomes applicable
    effective_to TIMESTAMP,           -- When this document expires (for shared docs with timeline)
    ...
    PRIMARY KEY (document_id, customer_id)
) PARTITION BY HASH (customer_id);
```

**Key Features:**
- Heavily denormalized for query performance
- **Shared Document Support**: `is_shared` flag indicates shared documents; shared docs stored once with NULL or special customer_id
- **Timeline Support**: `effective_from` and `effective_to` enable querying documents applicable at a specific time
- Partitioned by `customer_id` for horizontal scalability (shared documents use special partition)
- Direct link to specific template version via `template_id`
- Denormalized `template_code` and `template_version` avoid joins

### 4. Document Customer Mapping Table (for Shared Documents)

```sql
document_customer_mapping (
    mapping_id UUID PRIMARY KEY,
    document_id UUID NOT NULL,        -- References documents(document_id)
    customer_id VARCHAR(100) NOT NULL,
    account_id VARCHAR(100),          -- Optional: specific account association
    assigned_at TIMESTAMP DEFAULT NOW(),
    assigned_by VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100),
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE,
    UNIQUE (document_id, customer_id, account_id)
)
PARTITION BY HASH (customer_id);

-- Index for fast customer lookups
CREATE INDEX idx_doc_customer_map_customer ON document_customer_mapping(customer_id, is_active)
WHERE is_active = true;

CREATE INDEX idx_doc_customer_map_document ON document_customer_mapping(document_id, is_active)
WHERE is_active = true;
```

**Key Features:**
- **Many-to-Many Relationship**: Maps shared documents to applicable customers
- Used only when `sharing_scope = 'specific_customers'`
- For `sharing_scope = 'all_customers'`, no mapping needed (query logic handles it)
- Partitioned by `customer_id` for parallel query execution
- `is_active` flag allows removing document associations without deletion

---

## Shared Document Architecture

### Overview

The system supports **shared documents** - documents that are stored once but apply to multiple customers. Examples include:
- Privacy policies
- Terms of service updates
- Customer notices
- Regulatory disclosures
- Bank-wide announcements

### Design Rationale

**Storage Efficiency**: Instead of creating duplicate document records for each customer, shared documents:
1. Are stored once in the `documents` table with `is_shared = true`
2. Use `customer_id = '00000000-SHARED'` (special sentinel value) for partitioning
3. Are associated with customers via `document_customer_mapping` table (when scope is specific customers) or query logic (when scope is all customers)

### Sharing Scopes

| Scope | Description | Implementation | Example Use Case |
|-------|-------------|----------------|------------------|
| `all_customers` | Document applies to all customers | No mapping table needed; query joins all shared docs | Privacy policy update |
| `customer_segment` | Document applies to customer segment | Filter by `account_type` or customer attributes | Premium customer notice |
| `account_type` | Document applies to specific account types | Filter by `account_type` in documents table | Mortgage-specific disclosure |
| `specific_customers` | Document applies to explicit customer list | Uses `document_customer_mapping` table | Targeted regulatory notice |

### Query Pattern: Fetch All Documents for a Customer

To retrieve all documents for a customer (both customer-specific and shared):

```sql
-- Unified query to get customer-specific + shared documents
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.is_shared,
    d.effective_from,
    d.effective_to,
    d.uploaded_at
FROM documents d
WHERE
    -- Customer-specific documents
    (d.customer_id = 'C123' AND d.is_shared = false)

    OR

    -- Shared documents for all customers (within effective timeline)
    (d.is_shared = true
     AND d.sharing_scope = 'all_customers'
     AND d.effective_from <= NOW()
     AND (d.effective_to IS NULL OR d.effective_to >= NOW()))

    OR

    -- Shared documents by account type
    (d.is_shared = true
     AND d.sharing_scope = 'account_type'
     AND d.account_type = (SELECT account_type FROM customers WHERE customer_id = 'C123')
     AND d.effective_from <= NOW()
     AND (d.effective_to IS NULL OR d.effective_to >= NOW()))

    OR

    -- Shared documents explicitly mapped to this customer
    (d.is_shared = true
     AND d.sharing_scope = 'specific_customers'
     AND EXISTS (
         SELECT 1 FROM document_customer_mapping dcm
         WHERE dcm.document_id = d.document_id
           AND dcm.customer_id = 'C123'
           AND dcm.is_active = true
     )
     AND d.effective_from <= NOW()
     AND (d.effective_to IS NULL OR d.effective_to >= NOW()))

ORDER BY d.uploaded_at DESC;
```

### Optimized Query with UNION

For better performance, use UNION to leverage partition pruning:

```sql
-- Customer-specific documents (uses partition pruning)
SELECT document_id, document_name, document_type, is_shared, uploaded_at
FROM documents
WHERE customer_id = 'C123' AND is_shared = false

UNION ALL

-- Shared documents for all customers
SELECT document_id, document_name, document_type, is_shared, uploaded_at
FROM documents
WHERE customer_id = '00000000-SHARED'
  AND is_shared = true
  AND sharing_scope = 'all_customers'
  AND effective_from <= NOW()
  AND (effective_to IS NULL OR effective_to >= NOW())

UNION ALL

-- Shared documents mapped to this customer
SELECT d.document_id, d.document_name, d.document_type, d.is_shared, d.uploaded_at
FROM documents d
INNER JOIN document_customer_mapping dcm ON d.document_id = dcm.document_id
WHERE d.customer_id = '00000000-SHARED'
  AND d.is_shared = true
  AND d.sharing_scope = 'specific_customers'
  AND dcm.customer_id = 'C123'
  AND dcm.is_active = true
  AND d.effective_from <= NOW()
  AND (d.effective_to IS NULL OR d.effective_to >= NOW())

ORDER BY uploaded_at DESC;
```

### Workflow: Creating a Shared Document

```sql
-- Step 1: Create shared document (e.g., privacy policy)
INSERT INTO documents (
    document_id,
    ecms_document_id,
    is_shared,
    sharing_scope,
    customer_id,           -- Use special sentinel value
    document_type,
    document_category,
    template_id,
    template_code,
    template_version,
    effective_from,
    effective_to,
    document_name,
    created_at,
    created_by
) VALUES (
    gen_random_uuid(),
    'ECMS-12345',
    true,                  -- Shared document
    'all_customers',       -- Applies to all customers
    '00000000-SHARED',     -- Special partition key for shared docs
    'PRIVACY_POLICY',
    'REGULATORY',
    'template-uuid',
    'PRIVACY_POLICY_TEMPLATE',
    1,
    '2024-01-01 00:00:00', -- Effective from
    NULL,                  -- No expiration
    'Privacy Policy Update 2024',
    NOW(),
    'admin@bank.com'
);

-- Step 2: If sharing_scope = 'specific_customers', create mappings
-- (Skip this step for 'all_customers' scope)
INSERT INTO document_customer_mapping (
    mapping_id,
    document_id,
    customer_id,
    assigned_at,
    assigned_by,
    is_active
) VALUES
    (gen_random_uuid(), 'doc-uuid', 'C123', NOW(), 'admin@bank.com', true),
    (gen_random_uuid(), 'doc-uuid', 'C456', NOW(), 'admin@bank.com', true);
```

### Benefits

1. **Storage Efficiency**: One privacy policy stored once, not duplicated across 1M customers
2. **Consistency**: Update shared document once, all customers see the update
3. **Flexible Targeting**: Support all customers, segments, account types, or specific lists
4. **Timeline Support**: `effective_from` and `effective_to` enable time-based queries
5. **Audit Trail**: Track which customers were assigned which shared documents

---

## Denormalized Versioning Logic

### Why Denormalized Versioning?

Traditional normalized versioning would separate templates into two tables:

```sql
-- Traditional Approach (NOT USED)
templates (
    template_id UUID PRIMARY KEY,
    template_code VARCHAR(100) UNIQUE,
    current_version_id UUID
)

template_versions (
    version_id UUID PRIMARY KEY,
    template_id UUID REFERENCES templates(template_id),
    version_number INTEGER,
    ...
)
```

### Problems with Normalized Versioning

1. **Extra Join Required**: Every query needs to join templates → template_versions
2. **Complex Foreign Keys**: Documents must reference version_id, making template_code queries harder
3. **Cascading Updates**: Changing active version requires updating parent table
4. **Query Complexity**: Getting template history requires multiple joins

### Denormalized Versioning Benefits

Our approach stores each version as a row in the same table:

```sql
templates (
    template_id UUID PRIMARY KEY,
    template_code VARCHAR(100),
    version_number INTEGER,
    status VARCHAR(20),  -- 'active', 'deprecated', 'draft'
    ...
    UNIQUE (template_code, version_number)
)
```

**Advantages:**

1. **Simple Queries**
   ```sql
   -- Get active template version (no joins)
   SELECT * FROM templates
   WHERE template_code = 'LOAN_APPLICATION' AND status = 'active';

   -- Get template history (single table scan)
   SELECT * FROM templates
   WHERE template_code = 'LOAN_APPLICATION'
   ORDER BY version_number DESC;
   ```

2. **Efficient Document Joins**
   ```sql
   -- Documents join directly to templates (one join)
   SELECT d.*, t.template_name, t.version_number
   FROM documents d
   JOIN templates t ON d.template_id = t.template_id
   WHERE d.customer_id = 'C123';
   ```

3. **Immutable Versions**: Each version is a complete, immutable record
   - No risk of accidentally modifying historical versions
   - Audit trail is implicit (all versions retained)

4. **Index Efficiency**
   - Partial index on active templates: `WHERE status = 'active'`
   - Composite index on (template_code, version_number) handles most queries

5. **Storage Trade-off is Minimal**
   - Templates are small (< 1KB per row)
   - Even 100 versions of 1000 templates = only 100,000 rows
   - Negligible compared to millions of documents

### Version Management Workflow

```
1. Create new template → status = 'draft'
2. Publish template → status = 'active'
   - Application ensures only one 'active' per template_code
   - Previous active version → status = 'deprecated'
3. Documents created → reference active template_id
4. Even if template deprecated, documents retain original template_id
```

### Version Querying Patterns

```sql
-- Get latest active version
SELECT * FROM templates
WHERE template_code = 'LOAN_APPLICATION' AND status = 'active';

-- Get specific version
SELECT * FROM templates
WHERE template_code = 'LOAN_APPLICATION' AND version_number = 3;

-- Get version history
SELECT version_number, template_name, status, created_at
FROM templates
WHERE template_code = 'LOAN_APPLICATION'
ORDER BY version_number DESC;

-- Find all documents using a deprecated template version
SELECT d.*
FROM documents d
JOIN templates t ON d.template_id = t.template_id
WHERE t.template_code = 'LOAN_APPLICATION'
  AND t.status = 'deprecated'
  AND t.version_number = 2;
```

---

## Indexing Strategy

### Composite Indexes for Common Queries

The indexing strategy is based on expected query patterns:

#### Documents Table Indexes

| Index | Use Case | Query Pattern |
|-------|----------|---------------|
| `idx_documents_customer_type` | Primary query pattern | `WHERE customer_id = ? AND document_type = ?` |
| `idx_documents_customer_category` | Category filtering | `WHERE customer_id = ? AND document_category = ?` |
| `idx_documents_account_type` | Account-level queries | `WHERE account_id = ? AND document_type = ?` |
| `idx_documents_template_code` | Template-based lookups | `WHERE template_code = ? AND template_version = ?` |
| `idx_documents_date_range` | Date range queries | `WHERE customer_id = ? AND document_date BETWEEN ? AND ?` |
| `idx_documents_shared_scope` | Shared document lookups | `WHERE is_shared = true AND sharing_scope = ?` |
| `idx_documents_effective_timeline` | Timeline-based queries | `WHERE effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?)` |
| `idx_documents_shared_account_type` | Account type shared docs | `WHERE is_shared = true AND sharing_scope = 'account_type' AND account_type = ?` |

```sql
-- Core indexes for customer-specific documents
CREATE INDEX idx_documents_customer_type ON documents(customer_id, document_type)
WHERE is_shared = false;

CREATE INDEX idx_documents_customer_category ON documents(customer_id, document_category)
WHERE is_shared = false;

-- Indexes for shared documents
CREATE INDEX idx_documents_shared_all ON documents(is_shared, sharing_scope, effective_from, effective_to)
WHERE is_shared = true AND sharing_scope = 'all_customers';

CREATE INDEX idx_documents_shared_account_type ON documents(is_shared, sharing_scope, account_type, effective_from)
WHERE is_shared = true AND sharing_scope = 'account_type';

CREATE INDEX idx_documents_shared_specific ON documents(is_shared, sharing_scope, effective_from)
WHERE is_shared = true AND sharing_scope = 'specific_customers';

-- Timeline query optimization
CREATE INDEX idx_documents_timeline ON documents(effective_from, effective_to)
WHERE is_shared = true;
```

#### Partial Indexes

Partial indexes reduce index size and improve performance for filtered queries:

```sql
-- Only index active documents (most queries filter by status = 'active')
CREATE INDEX idx_documents_active ON documents(customer_id)
WHERE status = 'active';

-- Only index confidential documents (frequently filtered subset)
CREATE INDEX idx_documents_confidential ON documents(customer_id)
WHERE is_confidential = true;
```

**Benefit**: Partial indexes are smaller and faster to scan than full indexes.

#### GIN Indexes for Advanced Queries

```sql
-- Full-text search on document names
CREATE INDEX idx_documents_name_gin ON documents
USING gin(to_tsvector('english', document_name));

-- JSONB metadata queries
CREATE INDEX idx_documents_metadata_gin ON documents
USING gin(metadata);

-- Array tag searches
CREATE INDEX idx_documents_tags_gin ON documents
USING gin(tags);
```

### Index Maintenance

- **ANALYZE**: Run weekly to update query planner statistics
- **REINDEX CONCURRENTLY**: Monthly on high-write tables
- **Monitor Index Bloat**: Use pg_stat_user_indexes to identify unused indexes

---

## Partitioning Strategy

### Hash Partitioning by customer_id

The `documents` table is partitioned using hash partitioning on `customer_id`:

```sql
CREATE TABLE documents (
    ...
    PRIMARY KEY (document_id, customer_id)
) PARTITION BY HASH (customer_id);

-- 16 partitions (adjust based on data volume)
CREATE TABLE documents_p0 PARTITION OF documents
FOR VALUES WITH (MODULUS 16, REMAINDER 0);
...
CREATE TABLE documents_p15 PARTITION OF documents
FOR VALUES WITH (MODULUS 16, REMAINDER 15);
```

### Why Hash Partitioning?

| Partitioning Type | When to Use | Pros | Cons |
|-------------------|-------------|------|------|
| **Range** | Time-series data | Easy to archive old partitions | Uneven distribution if data skewed |
| **List** | Known discrete values | Explicit control | Requires manual partition creation |
| **Hash** | Even distribution needed | Automatic balancing | Cannot easily drop partitions |

**Chosen: Hash Partitioning** because:
1. Customer data is evenly distributed
2. No natural time-based archival pattern
3. Automatic load balancing across partitions
4. Scales to millions of customers

### Partition Count Selection

**Current: 16 partitions**

- **Too Few**: Limited parallelism, larger partition sizes
- **Too Many**: Overhead from partition management, slower query planning

**When to Increase**:
- If individual partitions exceed 50M rows
- If query performance degrades
- Easy to add partitions: `CREATE TABLE documents_p16 PARTITION OF documents FOR VALUES WITH (MODULUS 32, REMAINDER 16);`

### Partition Pruning

PostgreSQL automatically prunes (skips) irrelevant partitions:

```sql
-- This query only scans one partition (customer_id hashes to specific partition)
SELECT * FROM documents WHERE customer_id = 'C123';

-- This query scans all partitions (no partition key in WHERE clause)
SELECT * FROM documents WHERE document_type = 'LOAN_APPLICATION';
```

**Best Practice**: Always include `customer_id` in queries when possible.

### Alternative: Range Partitioning by uploaded_at

If archival is important, consider range partitioning by date:

```sql
CREATE TABLE documents (...)
PARTITION BY RANGE (uploaded_at);

CREATE TABLE documents_2024_q1 PARTITION OF documents
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE documents_2024_q2 PARTITION OF documents
FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
```

**Pros**: Easy to archive old quarters (detach partition, move to cold storage)
**Cons**: Recent partitions will be hot-spotted (all writes go to latest partition)

---

## Caching Recommendations

### Three-Tier Caching Strategy

```
[ Application Layer ]
        ↓
  [ Redis Cache ]
        ↓
[ Materialized Views ]
        ↓
  [ PostgreSQL ]
```

### 1. Application-Level Cache (Redis)

#### Cache Active Templates

Templates change infrequently and are queried constantly:

```redis
# Key Pattern: template:active:{template_code}
# TTL: 1 hour (3600 seconds)

Key: "template:active:LOAN_APPLICATION"
Value: {
  "template_id": "uuid",
  "template_code": "LOAN_APPLICATION",
  "version_number": 3,
  "template_name": "Loan Application Form",
  "status": "active",
  "rules": [...]
}
```

**Cache Invalidation**: Publish event when template is activated/deprecated

#### Cache Document Metadata

Recent documents are frequently accessed:

```redis
# Key Pattern: document:{ecms_document_id}
# TTL: 5 minutes (300 seconds)

Key: "document:12345-ECMS-ID"
Value: {
  "document_id": "uuid",
  "customer_id": "C123",
  "document_name": "Application Form.pdf",
  "document_type": "LOAN_APPLICATION",
  "template_code": "LOAN_APPLICATION",
  "uploaded_at": "2024-01-15T10:30:00Z"
}
```

#### Cache Customer Document Lists

Frequently accessed customer document lists:

```redis
# Key Pattern: customer:documents:{customer_id}:{document_type}
# TTL: 2 minutes (120 seconds)

Key: "customer:documents:C123:LOAN_APPLICATION"
Value: [
  {"document_id": "uuid1", "document_name": "App1.pdf", ...},
  {"document_id": "uuid2", "document_name": "App2.pdf", ...}
]
```

**Cache Invalidation**: Expire on new document upload or status change

#### Cache Template Rules

Rules are queried during document upload validation:

```redis
# Key Pattern: template:rules:{template_id}
# TTL: 1 hour (3600 seconds)

Key: "template:rules:550e8400-e29b-41d4-a716-446655440000"
Value: [
  {
    "rule_id": "uuid",
    "rule_name": "Validate SSN Format",
    "rule_type": "validation",
    "rule_expression": "regex: ^\\d{3}-\\d{2}-\\d{4}$",
    "execution_order": 1
  },
  ...
]
```

### 2. Database-Level Materialized Views

Pre-aggregate expensive queries for dashboards and reporting:

#### Customer Document Summary

```sql
CREATE MATERIALIZED VIEW mv_customer_document_summary AS
SELECT
    customer_id,
    customer_name,
    COUNT(*) AS total_documents,
    COUNT(DISTINCT document_type) AS unique_document_types,
    COUNT(*) FILTER (WHERE is_confidential = true) AS confidential_count,
    COUNT(*) FILTER (WHERE status = 'active') AS active_documents,
    MAX(uploaded_at) AS last_upload_date,
    SUM(file_size_bytes) AS total_storage_bytes
FROM documents
WHERE status IN ('active', 'archived')
GROUP BY customer_id, customer_name;

CREATE UNIQUE INDEX idx_mv_customer_summary ON mv_customer_document_summary(customer_id);
```

**Refresh Strategy**:
- **Concurrent Refresh**: `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_document_summary;`
- **Schedule**: Hourly via cron or pg_cron extension
- **Impact**: Queries run against stale data (up to 1 hour old), but instant response times

#### Document Type Distribution

```sql
CREATE MATERIALIZED VIEW mv_document_type_distribution AS
SELECT
    document_type,
    document_category,
    COUNT(*) AS document_count,
    COUNT(DISTINCT customer_id) AS customer_count,
    AVG(file_size_bytes) AS avg_file_size,
    MAX(uploaded_at) AS latest_upload
FROM documents
WHERE status = 'active'
GROUP BY document_type, document_category;
```

**Use Case**: Dashboard showing document type breakdown

### 3. Query Result Caching (Redis)

Cache expensive query results:

```redis
# Key Pattern: query:hash:{query_hash}
# TTL: 5 minutes

Key: "query:hash:abc123def456"
Value: [query result JSON]
```

**Implementation**: Hash the SQL query + parameters → cache result

### Cache Eviction Strategies

| Cache Type | Eviction Strategy | Rationale |
|------------|-------------------|-----------|
| Active Templates | Event-driven (publish/subscribe) | Templates change rarely; immediate invalidation needed |
| Document Metadata | TTL (5 min) + event-driven | Balance freshness with cache hit rate |
| Customer Lists | TTL (2 min) | Frequent changes; short TTL acceptable |
| Materialized Views | Scheduled refresh (hourly) | Reporting/analytics can tolerate staleness |

### Cache Warming Strategy

Pre-warm cache on application startup:

```python
# Pseudocode
def warm_cache():
    # Load all active templates into Redis
    active_templates = db.query("SELECT * FROM templates WHERE status = 'active'")
    for template in active_templates:
        redis.setex(f"template:active:{template.code}", 3600, json.dumps(template))

    # Load top 1000 most-accessed customers
    top_customers = db.query("SELECT customer_id FROM mv_customer_document_summary ORDER BY total_documents DESC LIMIT 1000")
    for customer in top_customers:
        documents = db.query("SELECT * FROM documents WHERE customer_id = ? AND status = 'active' LIMIT 100", customer.id)
        redis.setex(f"customer:documents:{customer.id}", 120, json.dumps(documents))
```

---

## Performance Optimization

### 1. Connection Pooling

Use PgBouncer or application-level connection pooling:

```ini
# PgBouncer configuration
[databases]
document_hub = host=localhost port=5432 dbname=document_hub

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 25
```

### 2. Prepared Statements

Use prepared statements for frequently executed queries:

```sql
PREPARE customer_docs_query (VARCHAR, VARCHAR) AS
SELECT document_id, document_name, document_date
FROM documents
WHERE customer_id = $1 AND document_type = $2 AND status = 'active'
ORDER BY document_date DESC LIMIT 100;

EXECUTE customer_docs_query('C123', 'LOAN_APPLICATION');
```

### 3. EXPLAIN ANALYZE

Regularly analyze slow queries:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM documents
WHERE customer_id = 'C123' AND document_type = 'LOAN_APPLICATION';
```

Look for:
- Seq Scan → missing index
- High buffer reads → inefficient query
- Large sort operations → need more work_mem

### 4. PostgreSQL Tuning

Key parameters for high-performance workloads:

```ini
# postgresql.conf

# Memory
shared_buffers = 8GB              # 25% of RAM
effective_cache_size = 24GB       # 75% of RAM
work_mem = 64MB                   # Per-operation memory
maintenance_work_mem = 2GB        # For VACUUM, CREATE INDEX

# Query Planning
random_page_cost = 1.1            # SSD optimization (default 4.0 is for HDD)
effective_io_concurrency = 200    # SSD parallelism

# Parallelism
max_parallel_workers_per_gather = 4
max_parallel_workers = 8

# Connections
max_connections = 200

# WAL
wal_buffers = 16MB
checkpoint_completion_target = 0.9
```

### 5. Partition-Aware Joins

Enable partition-wise joins for multi-table queries:

```ini
enable_partitionwise_join = on
enable_partitionwise_aggregate = on
```

---

## Scalability Considerations

### Horizontal Scaling

1. **Read Replicas**: Use PostgreSQL streaming replication
   - Primary: Write operations
   - Replicas: Read-only queries (customer lookups, reporting)

2. **Partition Distribution**: Move partitions to separate tablespaces/servers

3. **Sharding** (if needed at extreme scale):
   - Shard by customer_id ranges
   - Use Citus extension for distributed PostgreSQL

### Vertical Scaling

1. **Increase Partition Count**: From 16 to 32 or 64 partitions
2. **Add Indexes**: Monitor slow query log
3. **Increase Hardware**: More RAM for larger shared_buffers

### Archival Strategy

Move old/inactive documents to archive tables:

```sql
-- Monthly job: Move documents older than 7 years to archive
CREATE TABLE documents_archive (LIKE documents INCLUDING ALL);

INSERT INTO documents_archive
SELECT * FROM documents
WHERE uploaded_at < NOW() - INTERVAL '7 years';

DELETE FROM documents
WHERE uploaded_at < NOW() - INTERVAL '7 years';

-- Or detach partition and move to cold storage
ALTER TABLE documents DETACH PARTITION documents_p0;
-- Move documents_p0 to S3 or Glacier
```

### Monitoring

Key metrics to track:

1. **Query Performance**
   - p95/p99 latency for customer/account queries
   - Slow query log (queries > 100ms)

2. **Table Statistics**
   - Table sizes: `SELECT pg_size_pretty(pg_total_relation_size('documents'));`
   - Index usage: `pg_stat_user_indexes`

3. **Cache Hit Ratio**
   - Redis cache hit rate: Target > 90%
   - PostgreSQL buffer cache: Target > 95%

4. **Partition Distribution**
   - Row count per partition: Should be balanced
   - Query distribution: Check if certain partitions are hot-spotted

---

## Sample SQL Queries

This section provides comprehensive SQL query examples for common retrieval patterns, including both customer-specific and shared documents.

### 1. Fetch Documents by Customer ID + Document Type

Retrieve all documents (customer-specific + shared) for a specific customer and document type:

```sql
-- Using UNION for optimal partition pruning
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.is_shared,
    d.uploaded_at,
    d.effective_from,
    d.effective_to,
    t.template_name,
    t.version_number
FROM documents d
LEFT JOIN templates t ON d.template_id = t.template_id
WHERE d.customer_id = 'C123'
  AND d.document_type = 'LOAN_APPLICATION'
  AND d.is_shared = false

UNION ALL

-- Shared documents for all customers
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.is_shared,
    d.uploaded_at,
    d.effective_from,
    d.effective_to,
    t.template_name,
    t.version_number
FROM documents d
LEFT JOIN templates t ON d.template_id = t.template_id
WHERE d.customer_id = '00000000-SHARED'
  AND d.document_type = 'LOAN_APPLICATION'
  AND d.is_shared = true
  AND d.sharing_scope = 'all_customers'
  AND d.effective_from <= NOW()
  AND (d.effective_to IS NULL OR d.effective_to >= NOW())

UNION ALL

-- Shared documents mapped to this specific customer
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.is_shared,
    d.uploaded_at,
    d.effective_from,
    d.effective_to,
    t.template_name,
    t.version_number
FROM documents d
INNER JOIN document_customer_mapping dcm ON d.document_id = dcm.document_id
LEFT JOIN templates t ON d.template_id = t.template_id
WHERE d.customer_id = '00000000-SHARED'
  AND d.document_type = 'LOAN_APPLICATION'
  AND d.is_shared = true
  AND d.sharing_scope = 'specific_customers'
  AND dcm.customer_id = 'C123'
  AND dcm.is_active = true
  AND d.effective_from <= NOW()
  AND (d.effective_to IS NULL OR d.effective_to >= NOW())

ORDER BY uploaded_at DESC;
```

### 2. Fetch Documents by Account ID + Document Category

Retrieve all documents for a specific account and category:

```sql
-- Customer-specific documents for this account
SELECT
    d.document_id,
    d.document_name,
    d.document_category,
    d.document_type,
    d.is_shared,
    d.uploaded_at,
    d.account_id,
    d.account_type
FROM documents d
WHERE d.account_id = 'ACC-456'
  AND d.document_category = 'REGULATORY'
  AND d.is_shared = false

UNION ALL

-- Shared documents for all customers (regardless of account)
SELECT
    d.document_id,
    d.document_name,
    d.document_category,
    d.document_type,
    d.is_shared,
    d.uploaded_at,
    d.account_id,
    d.account_type
FROM documents d
WHERE d.customer_id = '00000000-SHARED'
  AND d.document_category = 'REGULATORY'
  AND d.is_shared = true
  AND d.sharing_scope = 'all_customers'
  AND d.effective_from <= NOW()
  AND (d.effective_to IS NULL OR d.effective_to >= NOW())

UNION ALL

-- Shared documents for specific account type
SELECT
    d.document_id,
    d.document_name,
    d.document_category,
    d.document_type,
    d.is_shared,
    d.uploaded_at,
    d.account_id,
    d.account_type
FROM documents d
WHERE d.customer_id = '00000000-SHARED'
  AND d.document_category = 'REGULATORY'
  AND d.is_shared = true
  AND d.sharing_scope = 'account_type'
  AND d.account_type = (SELECT account_type FROM accounts WHERE account_id = 'ACC-456')
  AND d.effective_from <= NOW()
  AND (d.effective_to IS NULL OR d.effective_to >= NOW())

ORDER BY uploaded_at DESC;
```

### 3. Fetch All Documents for a Specific Template Code

Retrieve all documents created from a specific template (any version):

```sql
SELECT
    d.document_id,
    d.document_name,
    d.customer_id,
    d.account_id,
    d.document_type,
    d.is_shared,
    d.template_code,
    d.template_version,
    t.template_name,
    t.status AS template_status,
    d.uploaded_at
FROM documents d
LEFT JOIN templates t ON d.template_id = t.template_id
WHERE d.template_code = 'LOAN_APPLICATION'
ORDER BY d.uploaded_at DESC;
```

### 4. Fetch All Documents for a Specific Template Version

Retrieve documents created from a specific version of a template:

```sql
SELECT
    d.document_id,
    d.document_name,
    d.customer_id,
    d.account_id,
    d.is_shared,
    d.template_code,
    d.template_version,
    t.template_name,
    t.status AS template_status,
    d.uploaded_at
FROM documents d
INNER JOIN templates t ON d.template_id = t.template_id
WHERE t.template_code = 'LOAN_APPLICATION'
  AND t.version_number = 3
ORDER BY d.uploaded_at DESC;
```

### 5. Fetch All Active Shared Documents

Retrieve all currently active shared documents across all scopes:

```sql
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.document_category,
    d.sharing_scope,
    d.effective_from,
    d.effective_to,
    d.uploaded_at,
    CASE
        WHEN d.sharing_scope = 'all_customers' THEN 'All Customers'
        WHEN d.sharing_scope = 'account_type' THEN 'Account Type: ' || d.account_type
        WHEN d.sharing_scope = 'specific_customers' THEN 'Specific Customers'
        ELSE 'Unknown'
    END AS scope_description
FROM documents d
WHERE d.is_shared = true
  AND d.effective_from <= NOW()
  AND (d.effective_to IS NULL OR d.effective_to >= NOW())
ORDER BY d.effective_from DESC;
```

### 6. Fetch Documents by Customer with Timeline Filter

Retrieve all documents (customer-specific + shared) applicable to a customer at a specific point in time:

```sql
-- Set the query timestamp (e.g., '2024-06-15 12:00:00')
WITH query_time AS (
    SELECT TIMESTAMP '2024-06-15 12:00:00' AS ts
)
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.is_shared,
    d.effective_from,
    d.effective_to,
    d.uploaded_at
FROM documents d, query_time qt
WHERE (
    -- Customer-specific documents
    (d.customer_id = 'C123' AND d.is_shared = false)
    OR
    -- Shared documents for all customers within timeline
    (d.is_shared = true
     AND d.sharing_scope = 'all_customers'
     AND d.effective_from <= qt.ts
     AND (d.effective_to IS NULL OR d.effective_to >= qt.ts))
    OR
    -- Shared documents mapped to this customer within timeline
    (d.is_shared = true
     AND d.sharing_scope = 'specific_customers'
     AND EXISTS (
         SELECT 1 FROM document_customer_mapping dcm
         WHERE dcm.document_id = d.document_id
           AND dcm.customer_id = 'C123'
           AND dcm.is_active = true
     )
     AND d.effective_from <= qt.ts
     AND (d.effective_to IS NULL OR d.effective_to >= qt.ts))
)
ORDER BY d.uploaded_at DESC;
```

### 7. Find Customers Associated with a Shared Document

Retrieve all customers who have access to a specific shared document:

```sql
-- For 'all_customers' scope
SELECT
    'All Customers' AS customer_info,
    d.document_name,
    d.sharing_scope
FROM documents d
WHERE d.document_id = 'doc-uuid-123'
  AND d.sharing_scope = 'all_customers'

UNION ALL

-- For 'specific_customers' scope
SELECT
    dcm.customer_id AS customer_info,
    d.document_name,
    d.sharing_scope
FROM documents d
INNER JOIN document_customer_mapping dcm ON d.document_id = dcm.document_id
WHERE d.document_id = 'doc-uuid-123'
  AND d.sharing_scope = 'specific_customers'
  AND dcm.is_active = true

UNION ALL

-- For 'account_type' scope
SELECT
    'Account Type: ' || d.account_type AS customer_info,
    d.document_name,
    d.sharing_scope
FROM documents d
WHERE d.document_id = 'doc-uuid-123'
  AND d.sharing_scope = 'account_type';
```

### 8. Count Documents by Type (Customer-Specific vs Shared)

Aggregate document counts broken down by type and sharing status:

```sql
SELECT
    document_type,
    document_category,
    COUNT(*) FILTER (WHERE is_shared = false) AS customer_specific_count,
    COUNT(*) FILTER (WHERE is_shared = true AND sharing_scope = 'all_customers') AS shared_all_count,
    COUNT(*) FILTER (WHERE is_shared = true AND sharing_scope = 'specific_customers') AS shared_specific_count,
    COUNT(*) FILTER (WHERE is_shared = true AND sharing_scope = 'account_type') AS shared_account_type_count,
    COUNT(*) AS total_count
FROM documents
GROUP BY document_type, document_category
ORDER BY total_count DESC;
```

### 9. Find Documents Using Deprecated Template Versions

Identify documents that were created using deprecated template versions:

```sql
SELECT
    d.document_id,
    d.document_name,
    d.customer_id,
    d.is_shared,
    t.template_code,
    t.version_number AS deprecated_version,
    t.status AS template_status,
    d.uploaded_at
FROM documents d
INNER JOIN templates t ON d.template_id = t.template_id
WHERE t.status = 'deprecated'
ORDER BY t.template_code, t.version_number DESC, d.uploaded_at DESC;
```

### 10. Fetch All Documents for Customer (Complete Pattern)

This is the production-ready query to fetch all documents for a customer, combining customer-specific and all types of shared documents:

```sql
CREATE OR REPLACE FUNCTION get_customer_documents(
    p_customer_id VARCHAR(100),
    p_document_type VARCHAR(100) DEFAULT NULL,
    p_as_of_date TIMESTAMP DEFAULT NOW()
)
RETURNS TABLE (
    document_id UUID,
    document_name VARCHAR,
    document_type VARCHAR,
    document_category VARCHAR,
    is_shared BOOLEAN,
    uploaded_at TIMESTAMP,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    template_name VARCHAR,
    template_version INTEGER
) AS $$
BEGIN
    RETURN QUERY
    -- Customer-specific documents
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.is_shared,
        d.uploaded_at,
        d.effective_from,
        d.effective_to,
        t.template_name,
        t.version_number
    FROM documents d
    LEFT JOIN templates t ON d.template_id = t.template_id
    WHERE d.customer_id = p_customer_id
      AND d.is_shared = false
      AND (p_document_type IS NULL OR d.document_type = p_document_type)

    UNION ALL

    -- Shared documents for all customers
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.is_shared,
        d.uploaded_at,
        d.effective_from,
        d.effective_to,
        t.template_name,
        t.version_number
    FROM documents d
    LEFT JOIN templates t ON d.template_id = t.template_id
    WHERE d.customer_id = '00000000-SHARED'
      AND d.is_shared = true
      AND d.sharing_scope = 'all_customers'
      AND d.effective_from <= p_as_of_date
      AND (d.effective_to IS NULL OR d.effective_to >= p_as_of_date)
      AND (p_document_type IS NULL OR d.document_type = p_document_type)

    UNION ALL

    -- Shared documents for specific customers
    SELECT
        d.document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.is_shared,
        d.uploaded_at,
        d.effective_from,
        d.effective_to,
        t.template_name,
        t.version_number
    FROM documents d
    INNER JOIN document_customer_mapping dcm ON d.document_id = dcm.document_id
    LEFT JOIN templates t ON d.template_id = t.template_id
    WHERE d.customer_id = '00000000-SHARED'
      AND d.is_shared = true
      AND d.sharing_scope = 'specific_customers'
      AND dcm.customer_id = p_customer_id
      AND dcm.is_active = true
      AND d.effective_from <= p_as_of_date
      AND (d.effective_to IS NULL OR d.effective_to >= p_as_of_date)
      AND (p_document_type IS NULL OR d.document_type = p_document_type)

    ORDER BY uploaded_at DESC;
END;
$$ LANGUAGE plpgsql;

-- Usage examples:
-- SELECT * FROM get_customer_documents('C123');
-- SELECT * FROM get_customer_documents('C123', 'LOAN_APPLICATION');
-- SELECT * FROM get_customer_documents('C123', NULL, '2024-01-15 00:00:00');
```

---

## Summary

This schema design provides:

- **High Performance**: Composite indexes + denormalization + partitioning
- **Scalability**: Hash partitioning + caching + read replicas
- **Shared Document Support**: Efficient storage and retrieval of documents applicable to multiple customers with flexible scoping
- **Timeline Queries**: Temporal data support with `effective_from` and `effective_to` fields
- **Flexibility**: Template versioning + template-driven sharing scope + JSONB metadata + array tags
- **Maintainability**: Clear audit trail + materialized views + normalized rules
- **Storage Efficiency**: Store shared documents once instead of duplicating across customers

The denormalized versioning approach strikes the optimal balance between query simplicity, storage efficiency, and operational flexibility. The hybrid approach to shared documents (single document record + mapping table) provides both storage efficiency and query performance, allowing the system to handle millions of customer-specific documents alongside shared documents that apply to all or specific customer segments.
