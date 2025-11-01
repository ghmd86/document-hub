# Document Hub - PostgreSQL Schema Design

Complete database schema design for a high-performance document metadata indexing system.

## Overview

The Document Hub is a metadata indexing and retrieval layer that sits on top of an existing Enterprise Content Management System (ECMS). It provides fast, scalable access to document metadata while the actual files remain in the ECMS (Amazon S3).

### Key Features

- **Hybrid Normalization**: Documents denormalized for speed, templates/rules normalized for flexibility
- **Denormalized Versioning**: Template versions stored as rows (not separate tables)
- **Hash Partitioning**: 16 partitions for horizontal scalability
- **Composite Indexes**: Optimized for common query patterns
- **JSONB Support**: Flexible metadata storage
- **Audit Trail**: Complete tracking of all changes
- **Scalability**: Designed to handle millions of document records

---

## Project Deliverables

### 1. `document_hub_schema.sql`
Complete PostgreSQL DDL script including:
- **3 Core Tables**: documents, templates, template_rules
- **Primary/Foreign Keys**: All relationships and constraints
- **16 Partitions**: Hash partitioning strategy for documents table
- **20+ Indexes**: Composite, partial, and GIN indexes
- **10 Sample Queries**: Common retrieval patterns with explanations
- **Triggers**: Auto-update timestamps
- **Materialized View**: Pre-aggregated customer summaries
- **Utility Functions**: Template history and latest version views

### 2. `schema_design_documentation.md`
Comprehensive design documentation covering:
- **Design Principles**: Hybrid normalization approach explained
- **Denormalized Versioning Logic**: Why it's better than normalized versioning
- **Table Architecture**: Detailed breakdown of each table
- **Indexing Strategy**: Composite, partial, and GIN indexes explained
- **Partitioning Strategy**: Hash vs. range partitioning analysis
- **Caching Recommendations**: Redis + materialized views strategies
- **Performance Optimization**: Connection pooling, prepared statements, PostgreSQL tuning
- **Scalability Considerations**: Horizontal scaling, sharding, archival

### 3. `implementation_examples.md`
Practical code examples including:
- **Database Setup**: Initial creation and extension installation
- **Sample Data**: Templates, rules, and documents
- **SQL Functions**: Template versioning, document insertion, search
- **Python/FastAPI Integration**: Complete API examples
- **Redis Caching**: Cache layer implementation
- **Testing Queries**: Performance analysis and validation

---

## Quick Start

### 1. Create Database

```bash
psql -U postgres
```

```sql
CREATE DATABASE document_hub;
\c document_hub
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
\i document_hub_schema.sql
```

### 2. Verify Installation

```sql
-- Check tables
\dt

-- Check partitions
\d+ documents

-- Check indexes
\di

-- Verify sample queries work
SELECT * FROM v_latest_active_templates;
```

### 3. Insert Sample Data

See `implementation_examples.md` for complete sample data insertion scripts.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│                  Client Application                  │
└─────────────────────┬───────────────────────────────┘
                      │
           ┌──────────┴──────────┐
           │                     │
    ┌──────▼─────┐        ┌─────▼──────┐
    │   Redis    │        │ PostgreSQL │
    │   Cache    │        │ Document   │
    │            │        │    Hub     │
    └──────┬─────┘        └─────┬──────┘
           │                     │
           └──────────┬──────────┘
                      │
           ┌──────────▼──────────┐
           │       ECMS          │
           │  (Files in S3)      │
           └─────────────────────┘
```

## Schema Overview

### Tables

| Table | Type | Purpose | Rows (Est.) |
|-------|------|---------|-------------|
| **documents** | Partitioned | Document metadata (denormalized) | Millions |
| **templates** | Standard | Template versions (denormalized) | Thousands |
| **template_rules** | Standard | Business rules (normalized) | Thousands |

### Relationships

```
templates (1) ───< (many) template_rules
    │
    │
    └───< (many) documents
```

---

## Query Performance

### Expected Query Times (with proper indexing)

| Query Type | Avg Latency | Index Used |
|------------|-------------|------------|
| Customer + Type | 10-30ms | `idx_documents_customer_type` |
| Account + Category | 10-30ms | `idx_documents_account_category` |
| Template Lookup | 5-15ms | `idx_documents_template_code` |
| ECMS ID Lookup | 5-10ms | `idx_documents_ecms_id` (unique) |
| Full-text Search | 50-200ms | `idx_documents_name_gin` |

### Optimization Strategies

1. **Always filter by partition key** (`customer_id`) when possible
2. **Use Redis caching** for frequently accessed templates and documents
3. **Leverage materialized views** for dashboard/reporting queries
4. **Monitor index usage** with `pg_stat_user_indexes`
5. **Run VACUUM ANALYZE** regularly

---

## Key Design Decisions

### 1. Why Denormalized Versioning?

**Traditional Approach** (NOT used):
```
templates → template_versions
  (requires extra join for every query)
```

**Our Approach**:
```
templates (each row is a version)
  - Simpler queries (no joins)
  - Immutable versions
  - Implicit audit trail
```

**Trade-off**: Slightly more storage (negligible) for significantly better query performance.

### 2. Why Hash Partitioning?

- **Even distribution** across customers
- **Automatic load balancing**
- **No hot partitions** (unlike range partitioning by date)
- **Easy to scale** (add more partitions)

### 3. Why Denormalize Documents?

Customer and account fields are duplicated in documents table to avoid joins:

```sql
-- Without denormalization (slow)
SELECT d.*, c.customer_name, a.account_type
FROM documents d
JOIN customers c ON d.customer_id = c.customer_id
JOIN accounts a ON d.account_id = a.account_id
WHERE d.customer_id = 'CUST-001';

-- With denormalization (fast)
SELECT * FROM documents
WHERE customer_id = 'CUST-001';
```

**Trade-off**: Data consistency responsibility moves to application layer.

---

## Caching Strategy

### Three-Tier Cache

```
Application → Redis (L1) → Materialized Views (L2) → PostgreSQL (L3)
```

### Cache Hit Rates (Target)

| Layer | Target Hit Rate | TTL |
|-------|-----------------|-----|
| Redis - Active Templates | 95%+ | 1 hour |
| Redis - Document Metadata | 70-80% | 5 minutes |
| Redis - Customer Lists | 60-70% | 2 minutes |
| Materialized Views | 90%+ | 1 hour refresh |

---

## Scalability Path

### Current Design (Phase 1)
- Single PostgreSQL instance
- 16 hash partitions
- Redis caching
- Expected: Up to 50M documents

### Phase 2 (10M-100M documents)
- Add read replicas
- Increase to 32 partitions
- Implement connection pooling (PgBouncer)
- Optimize materialized view refresh

### Phase 3 (100M+ documents)
- Consider Citus (distributed PostgreSQL)
- Shard by customer_id ranges
- Move old partitions to archive database
- Implement cold storage for archived documents

---

## Maintenance Tasks

### Daily
- Monitor slow query log
- Check cache hit rates

### Weekly
- `ANALYZE` all tables
- Review index usage statistics
- Check partition distribution

### Monthly
- `VACUUM ANALYZE` (especially documents table)
- `REINDEX CONCURRENTLY` on high-write indexes
- Review and archive old documents
- Refresh materialized views

### Quarterly
- Review and optimize query patterns
- Consider adding/removing indexes based on usage
- Evaluate partition count and distribution
- Capacity planning review

---

## Common Operations

### Get Active Template with Rules
```sql
SELECT * FROM get_active_template_with_rules('LOAN_APPLICATION');
```

### Search Documents
```sql
SELECT * FROM search_documents(
    p_customer_id => 'CUST-001',
    p_document_type => 'LOAN_APPLICATION',
    p_status => 'active'
);
```

### Create New Template Version
```sql
SELECT create_template_version(
    'LOAN_APPLICATION',
    'Loan App v3',
    'New version with enhanced validation',
    'LOAN_APPLICATION',
    'LOANS',
    '{"max_file_size_mb": 20}'::jsonb,
    'admin@bank.com'
);
```

### Activate Template Version
```sql
SELECT activate_template_version(
    '550e8400-e29b-41d4-a716-446655440000',
    'admin@bank.com'
);
```

---

## Monitoring Queries

### Table Sizes
```sql
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Index Usage
```sql
SELECT
    tablename,
    indexname,
    idx_scan,
    idx_tup_read
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

### Cache Hit Ratio
```sql
SELECT
    sum(heap_blks_read) as heap_read,
    sum(heap_blks_hit)  as heap_hit,
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) as cache_ratio
FROM pg_statio_user_tables;
```

---

## Technology Stack

- **Database**: PostgreSQL 14+ (requires UUID generation and JSONB support)
- **Caching**: Redis 6+
- **API Framework**: FastAPI (Python) or equivalent
- **Connection Pooling**: PgBouncer (recommended)
- **Monitoring**: pg_stat_statements, pgAdmin, or Datadog

---

## File Reference

| File | Purpose | Size |
|------|---------|------|
| `document_hub_schema.sql` | Complete DDL with all tables, indexes, queries | ~15KB |
| `schema_design_documentation.md` | Design rationale and architecture docs | ~25KB |
| `implementation_examples.md` | Code examples and integration guides | ~20KB |
| `README.md` | Project overview and quick start (this file) | ~8KB |

---

## Support and Contribution

For questions or issues:
1. Review the design documentation
2. Check implementation examples
3. Run test queries to validate setup
4. Adjust indexes/partitions based on your specific workload

---

## License

This schema design is provided as-is for the Document Hub system. Adapt as needed for your specific requirements.

---

## Version History

- **v1.0** (2024): Initial schema design with denormalized versioning
  - 3 core tables
  - Hash partitioning (16 partitions)
  - 20+ indexes
  - Comprehensive documentation
