# Document Hub Service - Design Documentation

This folder contains all design documentation, schemas, and specifications for the Document Hub Service.

## Folder Structure

```
actual/
├── api/                    # API specifications
│   └── schema.yaml        # OpenAPI 3.0 specification
│
├── database/              # Database schemas
│   ├── database_schema.md                  # Original schema (legacy)
│   ├── database_schema_redesigned.md       # Normalized redesign
│   ├── database_schema_denormalized.md     # Final denormalized schema (CURRENT)
│   └── analytics_schema_simple.md          # Analytics tracking schema
│
├── config/                # Configuration files
│   └── extractor_logic.json                # Document extraction logic
│
├── documentation/         # General documentation
│   └── instructions.md                     # Implementation instructions
│
└── backup/               # Backup files
    └── schema.yaml.backup                  # Previous API schema version
```

## Key Documents

### Database Schema (CURRENT)
**File:** `database/database_schema_denormalized.md`

The production-ready database schema with:
- ✅ Composite primary keys (template_id, version)
- ✅ Denormalized design for zero-join queries
- ✅ JSONB fields for flexible metadata
- ✅ Multi-service data source configuration
- ✅ 3 main tables: master_template_definition, storage_index, template_vendor_mapping

### Analytics Schema
**File:** `database/analytics_schema_simple.md`

Simplified analytics tracking with:
- ✅ 2 tables: document_events, document_analytics
- ✅ Tracks views, downloads, prints, reprints, shares
- ✅ Who viewed, when, and how many times
- ✅ Monthly partitioned for performance
- ✅ Daily pre-aggregated statistics

### API Specification
**File:** `api/schema.yaml`

OpenAPI 3.0 specification with:
- ✅ Document management endpoints (upload, download, delete, metadata)
- ✅ Template management endpoints (CRUD, versioning)
- ✅ Vendor mapping endpoints (with template fields)
- ✅ Template fields endpoints (data source configuration)
- ✅ Category management endpoints
- ✅ Complete schema definitions for all models

## Database Design Evolution

1. **Original Schema** (`database_schema.md`)
   - Initial design with issues identified
   - Single template_id as primary key
   - Mixed data types and redundancies

2. **Normalized Redesign** (`database_schema_redesigned.md`)
   - Proper normalization with separate tables
   - Composite primary keys for versioning
   - Addressed all identified issues
   - NOT USED (management preferred denormalized)

3. **Denormalized Schema** (`database_schema_denormalized.md`) ⭐ **CURRENT**
   - Performance-optimized denormalized design
   - Zero-join queries for common operations
   - Strategic JSONB usage for flexibility
   - Best of both worlds: flexibility + performance

## Key Features

### Template Versioning
- Templates use composite key: `(template_id, version)`
- Each version has its own configuration
- Supports A/B testing and gradual rollouts
- Template vendor mappings reference both template_id and version

### Multi-Service Data Sources
- Template fields define data sources from multiple microservices
- Each field specifies:
  - Service name and endpoint
  - HTTP method and parameters
  - JSON path extraction
  - Transformations and validations
  - Fallback chains and caching

### Zero-Join Performance
- Document queries require no joins
- Template metadata denormalized into storage_index
- Category names, template names cached in events
- Fast reporting and analytics

### Analytics Tracking
- All document interactions logged in document_events
- Daily aggregation into document_analytics
- Track who, when, what, how for compliance
- Separate print vs reprint tracking
- Share tracking with recipient counts

## Implementation Status

| Component | Status | File |
|-----------|--------|------|
| Database Schema | ✅ Complete | database/database_schema_denormalized.md |
| Analytics Schema | ✅ Complete | database/analytics_schema_simple.md |
| API Specification | ✅ Complete | api/schema.yaml |
| Java Entities | ⏳ Pending | - |
| Repository Layer | ⏳ Pending | - |
| Service Layer | ⏳ Pending | - |
| Controllers | ⏳ Pending | - |

## Next Steps

1. **Generate JPA Entities** from database schema
2. **Implement Repository Layer** with composite key support
3. **Create Service Layer** for business logic
4. **Build REST Controllers** from OpenAPI spec
5. **Implement Analytics Logging** function calls
6. **Setup Scheduled Jobs** for daily aggregation
7. **Create Database Migration Scripts** (Flyway/Liquibase)

## Contact

For questions or clarifications about this design:
- Review the individual markdown files for detailed specifications
- Check the OpenAPI spec for API contract details
- Refer to JSONB examples in schema files for data structures

---

**Last Updated:** 2025-11-06
**Schema Version:** 2.0 (Denormalized)
**API Version:** 2.0.0
