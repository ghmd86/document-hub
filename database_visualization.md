# Document Hub - Database Storage & Retrieval Visualization

This document provides visual representations of how data is stored and retrieved in the Document Hub system.

## Table of Contents
1. [Entity Relationship Diagram](#entity-relationship-diagram)
2. [Data Storage Patterns](#data-storage-patterns)
3. [Partition Structure](#partition-structure)
4. [Data Flow: Upload Document](#data-flow-upload-document)
5. [Data Flow: Retrieve Documents](#data-flow-retrieve-documents)
6. [Shared Document Storage](#shared-document-storage)
7. [Query Execution Paths](#query-execution-paths)

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TEMPLATES TABLE                              │
├─────────────────────────────────────────────────────────────────────┤
│ PK: template_id (UUID)                                               │
│ UK: (template_code, version_number)                                  │
├─────────────────────────────────────────────────────────────────────┤
│ • template_code (e.g., 'LOAN_APPLICATION')                          │
│ • version_number (1, 2, 3, ...)                                     │
│ • template_name                                                      │
│ • document_type, document_category                                   │
│ • status ('active', 'deprecated', 'draft')                          │
│ • is_shared_document (BOOLEAN)                                      │
│ • sharing_scope ('all_customers', 'specific_customers', ...)        │
│ • configuration (JSONB)                                              │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ 1:N
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      TEMPLATE_RULES TABLE                            │
├─────────────────────────────────────────────────────────────────────┤
│ PK: rule_id (UUID)                                                   │
│ FK: template_id → templates.template_id                             │
├─────────────────────────────────────────────────────────────────────┤
│ • rule_name                                                          │
│ • rule_type ('validation', 'transformation', 'business_logic')      │
│ • rule_expression (SQL, regex, DSL)                                 │
│ • execution_order                                                    │
│ • is_active                                                          │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                        DOCUMENTS TABLE                               │
│                  (PARTITIONED BY HASH(customer_id))                  │
├─────────────────────────────────────────────────────────────────────┤
│ PK: (document_id, customer_id)                                       │
│ UK: ecms_document_id                                                 │
│ FK: template_id → templates.template_id                             │
├─────────────────────────────────────────────────────────────────────┤
│ • ecms_document_id (link to S3)                                     │
│ • is_shared (BOOLEAN)                                               │
│ • sharing_scope                                                      │
│ • effective_from, effective_to (timeline)                           │
│ • customer_id (partition key, '00000000-SHARED' for shared docs)   │
│ • customer_name, account_id, account_type (denormalized)            │
│ • document_name, document_type, document_category                    │
│ • template_code, template_version (denormalized)                    │
│ • file_extension, file_size_bytes, mime_type                        │
│ • status, is_confidential                                           │
│ • metadata (JSONB), tags (TEXT[])                                   │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ N:M (for shared docs with specific customers)
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│               DOCUMENT_CUSTOMER_MAPPING TABLE                        │
│                  (PARTITIONED BY HASH(customer_id))                  │
├─────────────────────────────────────────────────────────────────────┤
│ PK: mapping_id (UUID)                                                │
│ UK: (document_id, customer_id, account_id)                          │
├─────────────────────────────────────────────────────────────────────┤
│ • document_id (references shared document)                           │
│ • customer_id                                                        │
│ • account_id (optional)                                              │
│ • assigned_at, assigned_by                                           │
│ • is_active                                                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Data Storage Patterns

### Pattern 1: Customer-Specific Documents

```
┌──────────────────────────────────────────────────────────────────┐
│  Customer C123's Documents                                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  documents Table (customer_id = 'C123')                          │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ doc-1: Statement_Jan.pdf     is_shared=false               │ │
│  │ doc-2: Statement_Feb.pdf     is_shared=false               │ │
│  │ doc-3: Loan_Application.pdf  is_shared=false               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Stored in partition based on hash(C123)                         │
└──────────────────────────────────────────────────────────────────┘
```

### Pattern 2: Shared Documents (All Customers)

```
┌──────────────────────────────────────────────────────────────────┐
│  Shared Document for ALL Customers                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  documents Table (customer_id = '00000000-SHARED')               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ doc-shared-1: Privacy_Policy_2024.pdf                      │ │
│  │   is_shared = true                                         │ │
│  │   sharing_scope = 'all_customers'                          │ │
│  │   effective_from = 2024-01-01                              │ │
│  │   effective_to = NULL (no expiration)                      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Applies to: C123, C456, C789, ... ALL customers                 │
│  No mapping table needed - query logic includes it               │
└──────────────────────────────────────────────────────────────────┘
```

### Pattern 3: Shared Documents (Specific Customers)

```
┌──────────────────────────────────────────────────────────────────┐
│  Shared Document for SPECIFIC Customers                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  documents Table (customer_id = '00000000-SHARED')               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ doc-shared-2: Regulatory_Notice_CA.pdf                     │ │
│  │   is_shared = true                                         │ │
│  │   sharing_scope = 'specific_customers'                     │ │
│  │   effective_from = 2024-06-01                              │ │
│  │   effective_to = 2024-12-31                                │ │
│  └────────────────────────────────────────────────────────────┘ │
│                         │                                         │
│                         │ mapped to                               │
│                         ▼                                         │
│  document_customer_mapping Table                                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ doc-shared-2 → C123  (is_active=true)                      │ │
│  │ doc-shared-2 → C456  (is_active=true)                      │ │
│  │ doc-shared-2 → C789  (is_active=true)                      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Only applies to: C123, C456, C789 (explicitly mapped)           │
└──────────────────────────────────────────────────────────────────┘
```

---

## Partition Structure

### Hash Partitioning by customer_id (16 partitions)

```
                        documents TABLE (Logical)
                                 │
                ┌────────────────┴────────────────┐
                │    HASH(customer_id)            │
                │    MODULUS 16                   │
                └────────────────┬────────────────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │            │               │               │            │
    ▼            ▼               ▼               ▼            ▼
┌────────┐  ┌────────┐      ┌────────┐      ┌────────┐  ┌────────┐
│  p0    │  │  p1    │ ...  │  p7    │ ...  │  p14   │  │  p15   │
│ (R=0)  │  │ (R=1)  │      │ (R=7)  │      │ (R=14) │  │ (R=15) │
└────────┘  └────────┘      └────────┘      └────────┘  └────────┘

Example Customer Distribution:
┌────────────────────────────────────────────────────────────────┐
│ hash('C123') % 16 = 7   → documents_p7                         │
│ hash('C456') % 16 = 2   → documents_p2                         │
│ hash('C789') % 16 = 14  → documents_p14                        │
│ hash('00000000-SHARED') % 16 = 5  → documents_p5 (all shared) │
└────────────────────────────────────────────────────────────────┘

Benefits:
✓ Even distribution across partitions
✓ Parallel query execution
✓ Partition pruning (query scans only 1 partition when customer_id provided)
✓ Scalable to millions of documents
```

---

## Data Flow: Upload Document

### Scenario: Upload Customer-Specific Document

```
┌──────────────┐
│   Client     │
│ Application  │
└──────┬───────┘
       │ 1. Upload file + metadata
       │    (customer_id, document_type, template_code)
       ▼
┌──────────────────────────────────────────────────────────────┐
│                    Document Hub API                           │
└──────┬───────────────────────────────────────────────────────┘
       │ 2. Validate metadata
       │ 3. Fetch active template
       ▼
┌─────────────────────────────────────────────────────────────┐
│  Query: SELECT * FROM templates                              │
│         WHERE template_code = 'LOAN_APPLICATION'             │
│         AND status = 'active';                               │
└──────┬──────────────────────────────────────────────────────┘
       │ 4. Template found (template_id, rules)
       ▼
┌─────────────────────────────────────────────────────────────┐
│  Execute Template Rules (validation, transformation)         │
│  - Validate file extension                                   │
│  - Check file size limits                                    │
│  - Apply business rules                                      │
└──────┬──────────────────────────────────────────────────────┘
       │ 5. Rules passed
       ▼
┌─────────────────────────────────────────────────────────────┐
│                    ECMS API                                  │
│  - Upload file to S3                                         │
│  - Returns: ecms_document_id, S3 URL                         │
└──────┬──────────────────────────────────────────────────────┘
       │ 6. ecms_document_id = 'ECMS-12345'
       ▼
┌─────────────────────────────────────────────────────────────┐
│  INSERT INTO documents (                                     │
│    document_id = gen_random_uuid(),                          │
│    ecms_document_id = 'ECMS-12345',                          │
│    customer_id = 'C123',          ← Partition key            │
│    is_shared = false,                                        │
│    document_name = 'Loan_App.pdf',                           │
│    document_type = 'LOAN_APPLICATION',                       │
│    template_id = 'template-uuid',                            │
│    template_code = 'LOAN_APPLICATION',  ← Denormalized       │
│    template_version = 1,                ← Denormalized       │
│    ...                                                        │
│  );                                                           │
└──────┬──────────────────────────────────────────────────────┘
       │ 7. Insert into partition based on hash(C123)
       │    → documents_p7
       ▼
┌─────────────────────────────────────────────────────────────┐
│              documents_p7 (Physical Partition)               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ New row: doc-uuid, ECMS-12345, C123, Loan_App.pdf     │ │
│  └────────────────────────────────────────────────────────┘ │
└──────┬──────────────────────────────────────────────────────┘
       │ 8. Return success
       ▼
┌──────────────┐
│   Client     │
│  (Success)   │
└──────────────┘
```

### Scenario: Upload Shared Document (Privacy Policy)

```
┌──────────────┐
│   Admin      │
│   Portal     │
└──────┬───────┘
       │ 1. Upload shared document
       │    (document_type='PRIVACY_POLICY', sharing_scope='all_customers')
       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Document Hub API                          │
└──────┬──────────────────────────────────────────────────────┘
       │ 2. Upload to ECMS → ecms_document_id = 'ECMS-99999'
       ▼
┌─────────────────────────────────────────────────────────────┐
│  INSERT INTO documents (                                     │
│    document_id = gen_random_uuid(),                          │
│    ecms_document_id = 'ECMS-99999',                          │
│    customer_id = '00000000-SHARED',  ← Special partition key │
│    is_shared = true,                                         │
│    sharing_scope = 'all_customers',                          │
│    effective_from = NOW(),                                   │
│    effective_to = NULL,                                      │
│    document_name = 'Privacy_Policy_2024.pdf',                │
│    document_type = 'PRIVACY_POLICY',                         │
│    ...                                                        │
│  );                                                           │
└──────┬──────────────────────────────────────────────────────┘
       │ 3. Insert into partition based on hash('00000000-SHARED')
       │    → documents_p5
       ▼
┌─────────────────────────────────────────────────────────────┐
│              documents_p5 (Shared Docs Partition)            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ New shared doc: Privacy_Policy_2024.pdf               │ │
│  │ Applies to: ALL customers (via query logic)           │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘

NOTE: No entries in document_customer_mapping needed
      because sharing_scope = 'all_customers'
```

---

## Data Flow: Retrieve Documents

### Scenario: Fetch All Documents for Customer C123

```
┌──────────────┐
│   Client     │
│ Application  │
└──────┬───────┘
       │ Query: Get all documents for customer C123
       ▼
┌─────────────────────────────────────────────────────────────┐
│  SELECT * FROM get_customer_documents('C123');               │
└──────┬──────────────────────────────────────────────────────┘
       │
       │ Function executes 3 UNION ALL queries:
       │
       ├─── Query 1: Customer-Specific Documents ─────────────┐
       │                                                        │
       │    SELECT * FROM documents                            │
       │    WHERE customer_id = 'C123'                         │
       │      AND is_shared = false;                           │
       │                                                        │
       │    ┌─────────────────────────────────────────────┐   │
       │    │ Partition Pruning:                          │   │
       │    │ hash('C123') % 16 = 7                       │   │
       │    │ → Scan ONLY documents_p7                    │   │
       │    │                                              │   │
       │    │ Result:                                      │   │
       │    │ - Statement_Jan.pdf                         │   │
       │    │ - Statement_Feb.pdf                         │   │
       │    │ - Loan_Application.pdf                      │   │
       │    └─────────────────────────────────────────────┘   │
       │                                                        │
       ├─── Query 2: Shared Docs (All Customers) ─────────────┤
       │                                                        │
       │    SELECT * FROM documents                            │
       │    WHERE customer_id = '00000000-SHARED'              │
       │      AND is_shared = true                             │
       │      AND sharing_scope = 'all_customers'              │
       │      AND effective_from <= NOW()                      │
       │      AND (effective_to IS NULL OR effective_to >= NOW());
       │                                                        │
       │    ┌─────────────────────────────────────────────┐   │
       │    │ Partition Pruning:                          │   │
       │    │ hash('00000000-SHARED') % 16 = 5            │   │
       │    │ → Scan ONLY documents_p5                    │   │
       │    │                                              │   │
       │    │ Result:                                      │   │
       │    │ - Privacy_Policy_2024.pdf                   │   │
       │    │ - Terms_of_Service_2024.pdf                 │   │
       │    └─────────────────────────────────────────────┘   │
       │                                                        │
       ├─── Query 3: Shared Docs (Specific Customers) ────────┤
       │                                                        │
       │    SELECT d.* FROM documents d                        │
       │    JOIN document_customer_mapping dcm                 │
       │      ON d.document_id = dcm.document_id               │
       │    WHERE d.customer_id = '00000000-SHARED'            │
       │      AND d.is_shared = true                           │
       │      AND d.sharing_scope = 'specific_customers'       │
       │      AND dcm.customer_id = 'C123'                     │
       │      AND dcm.is_active = true                         │
       │      AND effective_from <= NOW()                      │
       │      AND (effective_to IS NULL OR effective_to >= NOW());
       │                                                        │
       │    ┌─────────────────────────────────────────────┐   │
       │    │ Partition Pruning:                          │   │
       │    │ documents_p5 (shared docs)                  │   │
       │    │ + mapping_p7 (C123's mappings)              │   │
       │    │                                              │   │
       │    │ Result:                                      │   │
       │    │ - Regulatory_Notice_CA.pdf                  │   │
       │    └─────────────────────────────────────────────┘   │
       │                                                        │
       ▼                                                        │
┌─────────────────────────────────────────────────────────────┐
│  UNION ALL Results (Combined)                                │
├─────────────────────────────────────────────────────────────┤
│  1. Statement_Jan.pdf          (customer-specific)           │
│  2. Statement_Feb.pdf          (customer-specific)           │
│  3. Loan_Application.pdf       (customer-specific)           │
│  4. Privacy_Policy_2024.pdf    (shared - all customers)      │
│  5. Terms_of_Service_2024.pdf  (shared - all customers)      │
│  6. Regulatory_Notice_CA.pdf   (shared - specific customers) │
└──────┬──────────────────────────────────────────────────────┘
       │ Total: 6 documents
       │ Partitions scanned: p7 (customer), p5 (shared)
       ▼
┌──────────────┐
│   Client     │
│  (6 results) │
└──────────────┘

Performance:
✓ Only 2-3 partitions scanned (out of 16)
✓ Indexes used for filtering
✓ Sub-100ms response time
```

---

## Shared Document Storage

### Visual Comparison: Traditional vs Shared Document Approach

#### Traditional Approach (Duplicate Storage)

```
Privacy Policy 2024 needs to go to 1,000,000 customers

┌─────────────────────────────────────────────────────────────┐
│  documents Table                                             │
├─────────────────────────────────────────────────────────────┤
│  doc-1, ecms-1, C001, Privacy_Policy_2024.pdf  (100 KB)     │
│  doc-2, ecms-2, C002, Privacy_Policy_2024.pdf  (100 KB)     │
│  doc-3, ecms-3, C003, Privacy_Policy_2024.pdf  (100 KB)     │
│  ... (997,997 more rows)                                     │
│  doc-1M, ecms-1M, C1000000, Privacy_Policy_2024.pdf (100 KB)│
└─────────────────────────────────────────────────────────────┘

Storage:
- 1,000,000 database rows
- 1,000,000 S3 files (or references)
- 100 KB × 1,000,000 = 100 GB of metadata
- Impossible to update policy for all customers atomically
```

#### Shared Document Approach (Efficient)

```
Privacy Policy 2024 for 1,000,000 customers

┌─────────────────────────────────────────────────────────────┐
│  documents Table                                             │
├─────────────────────────────────────────────────────────────┤
│  doc-shared-1, ecms-99, '00000000-SHARED', Privacy_Policy... │
│  is_shared = true, sharing_scope = 'all_customers'          │
└─────────────────────────────────────────────────────────────┘

Storage:
- 1 database row
- 1 S3 file
- 100 KB of metadata
- Update once, applies to all customers
- Query includes this doc for any customer automatically

Savings:
✓ 999,999 fewer database rows
✓ 999,999 fewer S3 references
✓ ~100 GB saved
✓ Single source of truth
```

---

## Query Execution Paths

### Path 1: Fetch by customer_id + document_type (Most Common)

```
Query: SELECT * FROM documents
       WHERE customer_id = 'C123' AND document_type = 'LOAN_APPLICATION';

Execution Plan:
┌─────────────────────────────────────────────────────────────┐
│ 1. Partition Pruning                                         │
│    hash('C123') % 16 = 7                                     │
│    → Scan ONLY documents_p7 (skip other 15 partitions)      │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Index Scan                                                │
│    USE idx_documents_customer_type                           │
│    (customer_id, document_type)                              │
│    → Fast lookup (B-tree index)                              │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Return Matching Rows                                      │
│    - Loan_Application_v1.pdf                                 │
│    - Loan_Application_v2.pdf                                 │
└─────────────────────────────────────────────────────────────┘

Performance: ~5-10ms (index scan on 1 partition)
```

### Path 2: Fetch shared documents (Timeline query)

```
Query: SELECT * FROM documents
       WHERE is_shared = true
         AND sharing_scope = 'all_customers'
         AND effective_from <= NOW()
         AND (effective_to IS NULL OR effective_to >= NOW());

Execution Plan:
┌─────────────────────────────────────────────────────────────┐
│ 1. Partition Scan                                            │
│    customer_id = '00000000-SHARED'                           │
│    → Scan documents_p5 only                                  │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Index Scan                                                │
│    USE idx_documents_shared_all                              │
│    (is_shared, sharing_scope, effective_from, effective_to)  │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Filter by Timeline                                        │
│    Check: effective_from <= NOW() AND                        │
│           (effective_to IS NULL OR effective_to >= NOW())    │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Return Active Shared Documents                            │
│    - Privacy_Policy_2024.pdf (effective: 2024-01-01 → ∞)    │
│    - Terms_of_Service_2024.pdf (effective: 2024-06-01 → ∞)  │
└─────────────────────────────────────────────────────────────┘

Performance: ~10-20ms (index scan + timeline filter)
```

### Path 3: Full-text search across all documents

```
Query: SELECT * FROM documents
       WHERE to_tsvector('english', document_name) @@ to_tsquery('loan');

Execution Plan:
┌─────────────────────────────────────────────────────────────┐
│ 1. Parallel Seq Scan                                         │
│    Scan all 16 partitions in parallel                        │
│    (p0, p1, ... p15)                                         │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. GIN Index Scan                                            │
│    USE idx_documents_name_gin                                │
│    Full-text search on document_name                         │
└──────┬──────────────────────────────────────────────────────┘
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Aggregate Results from All Partitions                     │
│    - Loan_Application.pdf (C123)                             │
│    - Loan_Agreement.pdf (C456)                               │
│    - Auto_Loan_Disclosure.pdf (shared)                       │
└─────────────────────────────────────────────────────────────┘

Performance: ~50-100ms (parallel scan across partitions)
```

---

## Summary

### Key Design Decisions Visualized

1. **Denormalized Template Versioning**
   - Each version = 1 row in templates table
   - No joins needed for version history
   - Simple foreign key from documents → templates

2. **Hash Partitioning**
   - Even distribution across 16 partitions
   - Partition pruning when customer_id in WHERE clause
   - Only 1-2 partitions scanned per query

3. **Shared Document Storage**
   - 1 document row for all customers (sharing_scope='all_customers')
   - N:M mapping table for specific customer lists
   - Timeline support with effective_from/effective_to

4. **Composite Indexes**
   - (customer_id, document_type) for most common queries
   - (is_shared, sharing_scope, effective_from) for shared docs
   - GIN indexes for full-text and JSONB searches

5. **Query Performance**
   - Customer-specific queries: ~5-10ms
   - Shared document queries: ~10-20ms
   - Full-text search: ~50-100ms
   - Target: Sub-100ms for all queries
