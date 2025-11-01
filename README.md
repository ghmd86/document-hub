# Document Hub - PostgreSQL Schema Design

A high-performance metadata indexing and retrieval layer for Enterprise Content Management Systems (ECMS), built on PostgreSQL.

## Overview

The Document Hub serves as a fast metadata index over an existing ECMS that stores documents in S3. It provides:

- **Customer-specific documents**: Statements, applications, forms tied to individual customers
- **Shared documents**: Privacy policies, notices, disclosures stored once but applicable to many customers
- **Template versioning**: Flexible document templates with business rules
- **High-performance queries**: Sub-100ms response times for millions of documents
- **Timeline support**: Query documents applicable at any point in time

## Project Structure

```
.
├── README.md                           # This file - Quick start guide
├── Requirements.md                     # Original requirements and specifications
├── schema_design_documentation.md      # Comprehensive design documentation
├── database_visualization.md           # Visual guide to data storage and retrieval
├── implementation_plan.md              # 16-20 week implementation roadmap
├── document_hub_schema.sql             # Final PostgreSQL schema (DDL)
└── archive/                            # Previous versions and iterations
```

## Quick Start

### 1. Review the Requirements

See `Requirements.md` for the complete problem statement and requirements.

### 2. Understand the Design

Read `schema_design_documentation.md` for:
- Design principles and rationale
- Table architecture details
- Shared document patterns
- Indexing strategy
- Partitioning approach
- Caching recommendations
- Sample queries

### 3. Deploy the Schema

Execute the SQL file to create the schema:

```bash
psql -U username -d database_name -f document_hub_schema.sql
```

## Key Features

### Hybrid Normalization Approach

| Component | Approach | Rationale |
|-----------|----------|-----------|
| **Documents** | Denormalized | Maximize query performance |
| **Shared Documents** | Hybrid (single doc + mapping table) | Storage efficiency + flexibility |
| **Templates** | Denormalized versioning | Simple queries, no joins needed |
| **Template Rules** | Normalized | Reusable across versions |

### Shared Document Architecture

Documents like privacy policies are stored **once** and associated with multiple customers:

**Sharing Scopes:**
- `all_customers` - Applies to all customers (e.g., privacy policy)
- `account_type` - Applies to specific account types (e.g., mortgage disclosure)
- `customer_segment` - Applies to customer segments (e.g., premium customers)
- `specific_customers` - Applies to explicit customer list (uses mapping table)

### Performance Optimizations

- **Hash partitioning** by `customer_id` (16 partitions)
- **Composite indexes** for common query patterns
- **Partial indexes** for filtered queries
- **GIN indexes** for full-text search and JSONB queries
- **Materialized views** for dashboards and reporting

## Common Queries

### Fetch all documents for a customer

```sql
SELECT * FROM get_customer_documents('C123');
```

This function returns both customer-specific documents and shared documents applicable to the customer.

### Fetch by customer + document type

```sql
SELECT * FROM get_customer_documents('C123', 'LOAN_APPLICATION');
```

### Fetch documents at a specific date

```sql
SELECT * FROM get_customer_documents('C123', NULL, '2024-01-15 00:00:00');
```

See `schema_design_documentation.md` for 10+ detailed query examples.

## Schema Tables

### Core Tables

1. **templates** - Template definitions with versioning
2. **template_rules** - Business rules associated with templates
3. **documents** - Document metadata (customer-specific and shared)
4. **document_customer_mapping** - Maps shared documents to specific customers

### Supporting Objects

- **get_customer_documents()** - PL/pgSQL function for retrieving documents
- **mv_customer_document_summary** - Materialized view for dashboard queries
- **update_updated_at_column()** - Trigger function for audit timestamps

## Scalability Considerations

- Supports **millions of documents** across thousands of customers
- **16 hash partitions** (can be increased to 32, 64, etc.)
- **Read replicas** for scaling read-heavy workloads
- **Redis caching** for active templates and recent documents
- **Materialized views** for expensive aggregations

## Design Rationale

### Why Denormalized Template Versioning?

Each template version is a **separate row** in the same table, not a separate version table.

**Benefits:**
- No joins needed for version queries
- Simpler foreign key relationships
- Immutable version history
- Better index efficiency

**Trade-off:**
- Minimal storage overhead (templates are small)
- Even with 100 versions × 1000 templates = only 100K rows

### Why Shared Documents?

Store **one privacy policy** instead of duplicating across 1M customers.

**Benefits:**
- Storage efficiency (1 document vs 1M copies)
- Consistency (update once, applies to all)
- Flexible targeting (all customers, segments, specific lists)
- Timeline support (effective dates)

## PostgreSQL Configuration

Recommended settings for high-performance workloads:

```ini
shared_buffers = 8GB              # 25% of RAM
effective_cache_size = 24GB       # 75% of RAM
work_mem = 64MB
maintenance_work_mem = 2GB
random_page_cost = 1.1            # SSD optimization
max_parallel_workers_per_gather = 4
```

## Monitoring

Track these metrics:

- p95/p99 query latency
- Table sizes: `SELECT pg_size_pretty(pg_total_relation_size('documents'));`
- Index usage: `pg_stat_user_indexes`
- Cache hit ratio (target >95%)
- Partition distribution

## Migration from Archive Versions

If you're upgrading from a previous version:

1. Review `archive/` folder for previous designs
2. Check `archive/schema_v2_changes_and_migration.md` for migration notes
3. Use `pg_dump` and `pg_restore` for data migration
4. Test queries before cutting over

## Documentation

### Core Documents
- **Requirements**: `Requirements.md` - Original problem statement and specifications
- **Full Design**: `schema_design_documentation.md` - Comprehensive design documentation (43KB)
  - Design principles and rationale
  - Complete table architecture
  - Shared document patterns
  - Indexing and partitioning strategy
  - Caching recommendations
  - 10+ sample SQL queries
- **Database Visualization**: `database_visualization.md` - Visual guide to storage and retrieval
  - Entity relationship diagrams
  - Data flow diagrams
  - Partition structure visualization
  - Query execution paths
- **Implementation Plan**: `implementation_plan.md` - 16-20 week roadmap
  - 8 phases from design to deployment
  - Technology stack
  - Team structure and roles
  - Risk management
  - Success metrics
- **Schema DDL**: `document_hub_schema.sql` - Final PostgreSQL schema

### Archive
- **Archive**: `archive/` - Previous iterations and versions

---

**Version**: Final (with Shared Document Support)
**Last Updated**: 2024
**PostgreSQL**: 14+ (uses gen_random_uuid(), partitioning, JSONB)
