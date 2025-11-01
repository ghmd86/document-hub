# Document Hub - Storage Capacity Planning

## Template Storage Requirements (400 Template Types)

### Assumptions

- **Template Types**: 400 different templates (template_code values)
- **Average Versions per Template**: 5 versions over time (lifecycle)
- **Average Rules per Template**: 10 rules (based on earlier input of 5-15 rules)

---

## 1. Templates Table Storage

### Row Count Calculation

```
Total Template Rows = Template Types × Average Versions
Total Template Rows = 400 × 5 = 2,000 rows
```

### Size per Row Estimate

| Field | Type | Estimated Size |
|-------|------|----------------|
| template_id | UUID | 16 bytes |
| template_code | VARCHAR(100) | ~30 bytes (avg) |
| version_number | INTEGER | 4 bytes |
| template_name | VARCHAR(255) | ~50 bytes (avg) |
| description | TEXT | ~200 bytes (avg) |
| document_type | VARCHAR(100) | ~30 bytes |
| document_category | VARCHAR(100) | ~30 bytes |
| status | VARCHAR(20) | ~10 bytes |
| is_shared_document | BOOLEAN | 1 byte |
| sharing_scope | VARCHAR(50) | ~20 bytes |
| retention_period_days | INTEGER | 4 bytes |
| requires_signature | BOOLEAN | 1 byte |
| requires_approval | BOOLEAN | 1 byte |
| configuration | JSONB | ~500 bytes (avg) |
| created_at | TIMESTAMP | 8 bytes |
| updated_at | TIMESTAMP | 8 bytes |
| created_by | VARCHAR(255) | ~50 bytes |
| updated_by | VARCHAR(255) | ~50 bytes |
| **TOTAL per row** | | **~1,013 bytes ≈ 1 KB** |

### Total Templates Table Storage

```
2,000 rows × 1 KB = 2 MB (data only)
```

### With Overhead (PostgreSQL TOAST, alignment, page overhead ~30%)

```
2 MB × 1.3 = 2.6 MB
```

---

## 2. Template Rules Table Storage

### Row Count Calculation

```
Total Rule Rows = Total Template Rows × Average Rules per Template
Total Rule Rows = 2,000 × 10 = 20,000 rows
```

### Size per Row Estimate

| Field | Type | Estimated Size |
|-------|------|----------------|
| rule_id | UUID | 16 bytes |
| template_id | UUID (FK) | 16 bytes |
| rule_name | VARCHAR(255) | ~50 bytes |
| rule_type | VARCHAR(50) | ~20 bytes |
| rule_expression | TEXT | ~200 bytes (avg) |
| rule_scope | VARCHAR(100) | ~30 bytes |
| execution_order | INTEGER | 4 bytes |
| is_active | BOOLEAN | 1 byte |
| error_message | TEXT | ~100 bytes |
| severity | VARCHAR(20) | ~10 bytes |
| created_at | TIMESTAMP | 8 bytes |
| updated_at | TIMESTAMP | 8 bytes |
| created_by | VARCHAR(255) | ~50 bytes |
| updated_by | VARCHAR(255) | ~50 bytes |
| **TOTAL per row** | | **~563 bytes** |

### Total Template Rules Storage

```
20,000 rows × 563 bytes = 11.26 MB (data only)
```

### With Overhead

```
11.26 MB × 1.3 = 14.6 MB
```

---

## 3. Index Storage for Templates & Rules

### Templates Table Indexes

| Index | Estimated Size |
|-------|----------------|
| Primary Key (template_id) | ~100 KB |
| idx_templates_code_version | ~150 KB |
| idx_templates_status | ~50 KB (partial) |
| idx_templates_type_category | ~150 KB |
| idx_templates_shared | ~30 KB (partial) |
| **TOTAL** | **~480 KB** |

### Template Rules Table Indexes

| Index | Estimated Size |
|-------|----------------|
| Primary Key (rule_id) | ~1 MB |
| idx_template_rules_template_id | ~1.2 MB |
| idx_template_rules_active | ~500 KB (partial) |
| idx_template_rules_type | ~800 KB |
| **TOTAL** | **~3.5 MB** |

**Total Index Storage**: ~4 MB

---

## 4. Total Storage for Templates (400 Types)

| Component | Storage |
|-----------|---------|
| Templates Table (data) | 2.6 MB |
| Template Rules Table (data) | 14.6 MB |
| Indexes | 4.0 MB |
| **TOTAL** | **~21.2 MB** |

### Conclusion for Templates

**Storage for 400 template types with versioning and rules: ~25 MB (with buffer)**

This is **negligible** in modern database terms. Templates are extremely lightweight.

---

## 5. Documents Table Storage (THE BIG ONE)

The documents table will consume **99%+ of total storage**. Let me calculate based on different scenarios:

### Scenario Parameters (Please provide your estimates)

To calculate document storage, I need:

1. **Number of customers**: How many customers in the system?
2. **Documents per customer**: Average documents per customer
3. **Shared documents**: How many shared documents (privacy policies, etc.)
4. **Growth rate**: Expected annual growth

### Example Calculation (1 Million Documents)

Assume:
- **1,000 customers**
- **Average 1,000 documents per customer** = 1,000,000 customer-specific documents
- **100 shared documents** (privacy policies, notices)

#### Size per Document Row

| Field | Type | Estimated Size |
|-------|------|----------------|
| document_id | UUID | 16 bytes |
| ecms_document_id | VARCHAR(255) | ~50 bytes |
| is_shared | BOOLEAN | 1 byte |
| sharing_scope | VARCHAR(50) | ~20 bytes |
| customer_id | VARCHAR(100) | ~30 bytes |
| customer_name | VARCHAR(255) | ~50 bytes |
| account_id | VARCHAR(100) | ~30 bytes |
| account_type | VARCHAR(100) | ~30 bytes |
| document_name | VARCHAR(500) | ~100 bytes |
| document_type | VARCHAR(100) | ~30 bytes |
| document_category | VARCHAR(100) | ~30 bytes |
| template_id | UUID | 16 bytes |
| template_code | VARCHAR(100) | ~30 bytes |
| template_version | INTEGER | 4 bytes |
| effective_from | TIMESTAMP | 8 bytes |
| effective_to | TIMESTAMP | 8 bytes |
| file_extension | VARCHAR(20) | ~5 bytes |
| file_size_bytes | BIGINT | 8 bytes |
| mime_type | VARCHAR(100) | ~30 bytes |
| status | VARCHAR(50) | ~20 bytes |
| is_confidential | BOOLEAN | 1 byte |
| access_level | VARCHAR(50) | ~20 bytes |
| document_date | DATE | 4 bytes |
| uploaded_at | TIMESTAMP | 8 bytes |
| modified_at | TIMESTAMP | 8 bytes |
| metadata | JSONB | ~300 bytes (avg) |
| tags | TEXT[] | ~100 bytes (avg) |
| created_at | TIMESTAMP | 8 bytes |
| updated_at | TIMESTAMP | 8 bytes |
| created_by | VARCHAR(255) | ~50 bytes |
| updated_by | VARCHAR(255) | ~50 bytes |
| **TOTAL per row** | | **~1,087 bytes ≈ 1.1 KB** |

#### Total Documents Storage (1M documents)

```
1,000,000 rows × 1.1 KB = 1,100 MB = 1.1 GB (data only)
With overhead: 1.1 GB × 1.3 = 1.43 GB
```

#### Document Indexes (1M documents)

| Index | Estimated Size |
|-------|----------------|
| Primary Key (document_id, customer_id) | ~50 MB |
| idx_documents_customer_type (partial) | ~60 MB |
| idx_documents_customer_category (partial) | ~60 MB |
| idx_documents_account_type (partial) | ~60 MB |
| idx_documents_shared_all (partial) | ~5 MB |
| idx_documents_shared_account_type (partial) | ~5 MB |
| idx_documents_shared_specific (partial) | ~5 MB |
| idx_documents_timeline (partial) | ~10 MB |
| idx_documents_template_code | ~60 MB |
| idx_documents_status | ~50 MB |
| idx_documents_date_range | ~50 MB |
| idx_documents_name_gin (GIN) | ~200 MB |
| idx_documents_metadata_gin (JSONB GIN) | ~150 MB |
| idx_documents_tags_gin (Array GIN) | ~100 MB |
| **TOTAL** | **~865 MB** |

**Total for 1M Documents**: ~1.43 GB (data) + 865 MB (indexes) = **~2.3 GB**

---

## 6. Document Customer Mapping Table (Shared Documents)

Assume:
- 100 shared documents
- Average 500 customers per shared document (for `sharing_scope = 'specific_customers'`)

### Row Count

```
100 shared docs × 500 customers = 50,000 mappings
```

### Size per Row

| Field | Type | Estimated Size |
|-------|------|----------------|
| mapping_id | UUID | 16 bytes |
| document_id | UUID | 16 bytes |
| customer_id | VARCHAR(100) | ~30 bytes |
| account_id | VARCHAR(100) | ~30 bytes |
| assigned_at | TIMESTAMP | 8 bytes |
| assigned_by | VARCHAR(255) | ~50 bytes |
| is_active | BOOLEAN | 1 byte |
| created_at | TIMESTAMP | 8 bytes |
| created_by | VARCHAR(255) | ~50 bytes |
| **TOTAL per row** | | **~209 bytes** |

### Total Mapping Storage

```
50,000 rows × 209 bytes = 10.45 MB
With overhead: 10.45 MB × 1.3 = 13.6 MB
```

### Mapping Indexes

```
Primary Key + 2 indexes ≈ 3 MB
```

**Total Mapping Storage**: ~17 MB

---

## 7. Materialized Views

### mv_customer_document_summary (1,000 customers)

```
1,000 rows × ~300 bytes = 300 KB
With index: ~500 KB
```

---

## 8. TOTAL Storage Summary

### For 400 Templates + 1 Million Documents

| Component | Storage |
|-----------|---------|
| Templates & Rules (data + indexes) | 25 MB |
| Documents (data) | 1.43 GB |
| Documents (indexes) | 865 MB |
| Document Customer Mapping | 17 MB |
| Materialized Views | 1 MB |
| **SUBTOTAL** | **~2.33 GB** |
| Safety Buffer (20%) | 466 MB |
| **TOTAL DATABASE SIZE** | **~2.8 GB** |

---

## 9. Scaling Projections

### Growth Scenarios

| Documents | Templates | Total DB Size | Notes |
|-----------|-----------|---------------|-------|
| 100K | 400 | ~300 MB | Small deployment |
| 1M | 400 | ~2.8 GB | Medium deployment |
| 10M | 400 | ~28 GB | Large deployment |
| 50M | 400 | ~140 GB | Enterprise scale |
| 100M | 400 | ~280 GB | Very large enterprise |

**Key Insight**: Storage scales almost linearly with document count. Templates are negligible.

---

## 10. Partition Storage Distribution (1M Documents)

With 16 hash partitions and even distribution:

```
Per Partition = 2.8 GB / 16 = ~175 MB per partition
```

This is excellent for:
- Parallel query execution
- Partition-level maintenance
- Easier backup/restore

---

## 11. Growth Over Time (5 Years)

### Assumptions
- Start: 1M documents
- Annual growth: 20%
- New templates: 50 per year

| Year | Documents | Templates | Total DB Size |
|------|-----------|-----------|---------------|
| Year 1 | 1.0M | 400 | 2.8 GB |
| Year 2 | 1.2M | 450 | 3.4 GB |
| Year 3 | 1.44M | 500 | 4.1 GB |
| Year 4 | 1.73M | 550 | 4.9 GB |
| Year 5 | 2.07M | 600 | 5.9 GB |

**Storage after 5 years**: ~6 GB (still very manageable)

---

## 12. Recommendations

### Storage Allocation

For **1M documents** on production:

| Purpose | Size | Notes |
|---------|------|-------|
| Database Data | 3 GB | Current data |
| Database Indexes | 1 GB | All indexes |
| Working Memory | 2 GB | Temp tables, sorts |
| WAL/Transaction Logs | 5 GB | Write-ahead logs |
| Backups | 10 GB | 2× full backups |
| **TOTAL DISK** | **21 GB** | Production allocation |

**Recommended**: Start with **50 GB SSD** for database volume (allows 2-3 years of growth)

### PostgreSQL Configuration (for 1M documents)

```ini
# For server with 32 GB RAM, 1M documents

# Memory
shared_buffers = 8GB              # 25% of RAM
effective_cache_size = 24GB       # 75% of RAM
work_mem = 64MB                   # Per operation
maintenance_work_mem = 2GB        # For VACUUM, CREATE INDEX

# Storage
max_wal_size = 4GB
min_wal_size = 1GB
checkpoint_completion_target = 0.9
```

### Backup Strategy

| Backup Type | Frequency | Retention | Size |
|-------------|-----------|-----------|------|
| Full Backup | Daily | 7 days | ~3 GB compressed |
| WAL Archiving | Continuous | 7 days | ~500 MB/day |
| Snapshot | Weekly | 4 weeks | ~3 GB |

**Total Backup Storage**: ~30 GB

---

## 13. Cloud Storage Costs (Estimate)

### AWS RDS PostgreSQL (us-east-1)

For 1M documents (~3 GB database):

| Component | Spec | Monthly Cost |
|-----------|------|--------------|
| RDS Instance | db.r6g.large (2 vCPU, 16 GB RAM) | ~$140 |
| Storage | 50 GB SSD (gp3) | ~$6 |
| Backup Storage | 30 GB | ~$3 |
| **TOTAL** | | **~$149/month** |

### Scaling Costs

| Documents | DB Size | RDS Instance | Monthly Cost |
|-----------|---------|--------------|--------------|
| 1M | 3 GB | db.r6g.large | $149 |
| 10M | 30 GB | db.r6g.xlarge | $320 |
| 50M | 150 GB | db.r6g.2xlarge | $640 |
| 100M | 300 GB | db.r6g.4xlarge | $1,280 |

---

## 14. Questions to Refine Estimates

To provide more accurate storage calculations, please provide:

1. **Expected number of customers** in the system?
2. **Average documents per customer** (e.g., 500, 1000, 5000)?
3. **Expected shared documents** (e.g., 50, 100, 500)?
4. **Annual document growth rate** (e.g., 10%, 20%, 50%)?
5. **Document retention policy** (how long to keep old documents)?
6. **Average JSONB metadata size** (simple or complex metadata)?

---

## Summary

### For 400 Template Types

- **Templates + Rules Storage**: ~25 MB (negligible)
- **Templates are NOT the storage concern** - they're tiny

### For Documents (Main Storage Driver)

- **1 Million documents**: ~2.8 GB total
- **10 Million documents**: ~28 GB total
- **100 Million documents**: ~280 GB total

### Key Takeaway

**Templates are cheap, documents are the scaling factor.**

With 400 template types, you'll use **< 30 MB** for templates regardless of document volume. The documents table will consume 99%+ of your storage.
