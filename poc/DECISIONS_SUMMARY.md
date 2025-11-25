# Document Hub POC - Key Decisions Summary

## Overview

This document summarizes all the key architectural and design decisions made during the planning phase of the Document Hub POC.

---

## 1. Technology Stack Decisions

### Decision: Use Spring Boot WebFlux (Reactive)
**Rationale:**
- Non-blocking I/O for better resource utilization
- Handle multiple external API calls efficiently
- Better scalability for high-volume document requests
- Aligns with existing `document-hub-service` implementation

**Alternative Considered:** Spring MVC (Servlet-based)
**Why Rejected:** Blocking I/O would be inefficient for multi-step external API calls

---

### Decision: Use R2DBC for Database Access
**Rationale:**
- Reactive database driver for PostgreSQL
- Consistent reactive programming model throughout stack
- Non-blocking database queries

**Alternative Considered:** JDBC with blocking queries
**Why Rejected:** Would break reactive chain and cause thread blocking

---

### Decision: Use PostgreSQL with JSONB
**Rationale:**
- JSONB for flexible configuration storage (`data_extraction_config`, `access_control`)
- Native JSON operators for querying metadata
- Proven at scale in existing implementation
- GIN indexes for fast JSONB queries

**Alternative Considered:** MongoDB (NoSQL)
**Why Rejected:** Existing infrastructure uses PostgreSQL; no need for schema-less document store

---

### Decision: Use Redis for Caching
**Rationale:**
- Cache external API responses to reduce latency
- Configurable TTL per data source
- Fast in-memory lookups
- Supports reactive operations

**Alternative Considered:** In-memory cache (Caffeine)
**Why Rejected:** Need distributed cache for multi-instance deployments

---

## 2. Architecture Decisions

### Decision: Configuration-Driven Approach
**Rationale:**
- Store extraction logic and rules as JSONB in database
- Non-technical teams can configure document retrieval without code changes
- Each template type defines only what it needs (no over-engineering)
- Easy to add new document types

**Key Benefit:** No deployment needed to add new document types or change rules

**Alternative Considered:** Code-based configuration
**Why Rejected:** Requires developer involvement for every change; slower iteration

---

### Decision: Multi-Step Chained Data Extraction
**Rationale:**
- Real-world scenarios require fetching data sequentially (e.g., pricingId → disclosureCode)
- Support conditional chaining (execute Step 2 only if Step 1 succeeds)
- Placeholder resolution for dynamic URLs
- Based on proven pattern from existing `document-hub-service`

**Example Use Case:**
1. Call Account Service → Get `pricingId`
2. Call Pricing Service with `pricingId` → Get `disclosureCode`
3. Use `disclosureCode` to match documents

**Alternative Considered:** Single API call per template
**Why Rejected:** Many document types require data from multiple services

---

### Decision: JSONPath for Data Extraction
**Rationale:**
- Powerful query language for extracting fields from JSON responses
- Supports filters, array operations, nested paths
- Standard library available (Jayway JSONPath)
- Non-developers can write expressions

**Example:**
```json
"pricingId": "$.content[?(@.domain == 'PRICING' && @.status == 'ACTIVE')].domainId | [0]"
```

**Alternative Considered:** Java code for parsing
**Why Rejected:** Requires code changes for every new extraction pattern

---

### Decision: Template-Specific Configurations
**Rationale:**
- Each document type (Statement, Privacy Policy, Cardholder Agreement) has unique data needs
- Avoid "one-size-fits-all" configuration
- Simple templates don't need complex extraction logic
- Reduces unnecessary API calls

**Example:**
- **Statement**: No external calls, match by account_key
- **Privacy Policy**: One call to Customer Service, match by state
- **Cardholder Agreement**: Two chained calls, match by disclosure code

**Alternative Considered:** Shared extraction configuration across templates
**Why Rejected:** Over-fetching data, performance impact, unnecessary complexity

---

### Decision: Separate `data_extraction_config` and `access_control`
**Rationale:**
- Clear separation of concerns:
  - `data_extraction_config`: What data to fetch and how to match documents
  - `access_control`: Who can see this document
- Independent evolution of extraction and eligibility logic
- Easier to understand and maintain

**Alternative Considered:** Single unified configuration
**Why Rejected:** Mixing concerns makes configuration harder to understand

---

### Decision: Support Parallel and Sequential Execution
**Rationale:**
- Sequential: For dependent steps (Step 2 needs output from Step 1)
- Parallel: For independent steps (fetch account balance and transaction summary simultaneously)
- Configurable per template via `executionMode`

**Alternative Considered:** Always sequential
**Why Rejected:** Misses performance optimization opportunities

---

## 3. Data Modeling Decisions

### Decision: Use JSONB for Configuration Storage
**Rationale:**
- Flexible schema for different template types
- PostgreSQL JSONB supports indexing and querying
- No need to modify database schema for new rule types
- Can store nested structures (rule groups, chaining conditions)

**Alternative Considered:** Relational tables for rules
**Why Rejected:** Too rigid, requires schema changes for new rule types

---

### Decision: Store Document Metadata as JSONB
**Rationale:**
- Each document type has different metadata requirements
- JSONB allows flexible metadata without schema changes
- Supports complex queries (e.g., `doc_metadata @> '{"state": "CA"}'`)

**Example Metadata:**
```json
{
  "target_state": "CA",
  "pricing_version": "2024.1",
  "customer_segment": "VIP",
  "disclosure_code": "DISC_CC_2024"
}
```

**Alternative Considered:** Separate metadata table with key-value pairs
**Why Rejected:** Complex joins, harder to query

---

### Decision: Separate Tables for Templates and Documents
**Rationale:**
- `master_template_definition`: Define document types and rules
- `storage_index`: Store actual document metadata and storage references
- Clear separation: One template → Many documents
- Easier to version templates independently

**Alternative Considered:** Single denormalized table
**Why Rejected:** Data duplication, harder to maintain

---

## 4. Service Layer Decisions

### Decision: Service Responsibilities
**Clear separation of concerns:**

| Service | Responsibility |
|---------|---------------|
| `DataExtractionEngine` | Execute multi-step API calls, extract data |
| `RuleEvaluationService` | Evaluate access control rules |
| `DocumentMatchingService` | Match documents based on extracted data |
| `DocumentEnquiryService` | Orchestrate entire flow, merge results |

**Rationale:** Single Responsibility Principle, easier to test and maintain

---

### Decision: Use Strategy Pattern for Extensibility
**Rationale:**
- Different matching strategies (reference_key, metadata_fields, composite)
- Different rule evaluators (account_type, region, tenure, etc.)
- Easy to add new strategies without modifying existing code
- Follows Open/Closed Principle

**Alternative Considered:** Switch-case statements
**Why Rejected:** Hard to extend, violates Open/Closed Principle

---

## 5. Error Handling Decisions

### Decision: Configurable Error Handling Per Data Source
**Rationale:**
- Different API calls have different criticality
- Some failures should stop execution (`fail-fast`)
- Some can use defaults (`use-default`)
- Some should be retried (`retry`)

**Supported Strategies:**
- `fail-fast`: Stop immediately on error
- `skip`: Continue with other data sources
- `use-default`: Use configured default value
- `retry`: Retry with exponential backoff

**Alternative Considered:** Global error handling
**Why Rejected:** Not flexible enough for different scenarios

---

### Decision: Circuit Breaker for External Services
**Rationale:**
- Prevent cascade failures when external service is down
- Fail fast instead of waiting for timeout
- Automatic recovery when service is back
- Uses Resilience4j library

**Alternative Considered:** Simple retry logic
**Why Rejected:** Doesn't protect against cascading failures

---

## 6. Caching Decisions

### Decision: Configurable Caching Per Data Source
**Rationale:**
- Different data has different freshness requirements
- Account arrangements: Cache for 30 minutes
- Pricing data: Cache for 1 hour
- Customer profile: Cache for 15 minutes
- Per-request caching to avoid duplicate calls within same request

**Cache Key Pattern:**
```
"arrangements:${accountId}"
"pricing:${pricingId}"
"customer:${customerId}"
```

**Alternative Considered:** No caching
**Why Rejected:** Poor performance, excessive external API calls

---

### Decision: Redis for Distributed Caching
**Rationale:**
- Multiple application instances need shared cache
- Avoid cache stampede
- Centralized invalidation

**Alternative Considered:** In-memory cache per instance
**Why Rejected:** Cache inconsistency across instances

---

## 7. Query and Matching Decisions

### Decision: Support Multiple Matching Strategies
**Rationale:**
- Different document types require different matching logic

**Supported Strategies:**
1. **reference_key**: Simple key-based lookup
2. **metadata_fields**: JSONB field matching
3. **composite**: Combine multiple conditions
4. **account_key**: Direct account ownership

**Alternative Considered:** Single matching strategy
**Why Rejected:** Too rigid for diverse document types

---

### Decision: JSONB Queries for Metadata Matching
**Rationale:**
- PostgreSQL JSONB operators (`@>`, `->`, `->>`)
- Fast with GIN indexes
- Flexible for different metadata structures

**Example Query:**
```sql
SELECT * FROM storage_index
WHERE doc_metadata @> '{"target_state": "CA", "segment": "VIP"}'::jsonb
```

**Alternative Considered:** Relational columns for all metadata
**Why Rejected:** Schema changes needed for each new metadata field

---

### Decision: Selection Strategies for Multiple Matches
**Rationale:**
- Sometimes multiple documents match the criteria
- Need to decide which one(s) to return

**Supported Strategies:**
- `LATEST_VERSION`: Return most recent version
- `LATEST_BY_DATE`: Return newest by creation date
- `ALL_MATCHES`: Return all matching documents

**Alternative Considered:** Always return all matches
**Why Rejected:** Some use cases need only the latest

---

## 8. Performance Decisions

### Decision: Process Templates in Parallel
**Rationale:**
- Multiple templates can be processed independently
- Use reactive Flux for parallel processing
- Merge results at the end

**Alternative Considered:** Sequential template processing
**Why Rejected:** Slower, doesn't utilize reactive benefits

---

### Decision: Database Indexes
**Rationale:**
- Fast lookups for common queries

**Recommended Indexes:**
```sql
-- JSONB indexes
CREATE INDEX idx_doc_metadata_gin ON storage_index USING GIN (doc_metadata);
CREATE INDEX idx_extraction_config_gin ON master_template_definition USING GIN (data_extraction_config);

-- Composite indexes
CREATE INDEX idx_storage_template_type_version ON storage_index (template_type, template_version);
CREATE INDEX idx_storage_reference_key ON storage_index (reference_key, reference_key_type);
CREATE INDEX idx_storage_account_key ON storage_index (account_key, is_shared);

-- Active templates
CREATE INDEX idx_template_active ON master_template_definition (is_active, start_date, end_date);
```

---

## 9. Security Decisions

### Decision: Store Configurations in Database (Not Code)
**Rationale:**
- Configurations are data, not code
- Easier to audit changes
- Role-based access control for config changes
- Version history in database

**Alternative Considered:** Configuration files in repository
**Why Rejected:** Harder to manage, requires deployment for changes

---

### Decision: Validate External Data
**Rationale:**
- Don't trust external services blindly
- Validate data types, patterns, required fields
- Configurable validation rules per field

**Example:**
```json
"validate": {
  "disclosureCode": {
    "type": "string",
    "required": true,
    "pattern": "^DISC_[A-Z0-9_]+$"
  }
}
```

---

## 10. Testability Decisions

### Decision: Mock External Services for Testing
**Rationale:**
- Unit tests shouldn't depend on external services
- Use MockWebServer for HTTP mocking
- Faster test execution
- Reproducible tests

**Alternative Considered:** Test against real services
**Why Rejected:** Slow, flaky, requires service availability

---

### Decision: Separate Test Configuration Profile
**Rationale:**
- `application-mock.yml` for testing
- Different service URLs, disabled cache
- H2 in-memory database for tests

**Alternative Considered:** Share production config
**Why Rejected:** Tests would depend on external infrastructure

---

## 11. Future-Proofing Decisions

### Decision: Extensible Rule Engine
**Rationale:**
- New rule types can be added without modifying core engine
- Strategy pattern for rule evaluators
- Configuration-driven rule definitions

**Example New Rule:**
- Add `CREDIT_SCORE` rule type
- Implement `CreditScoreEvaluator`
- Register in factory
- Configure in JSON

---

### Decision: Support for Custom Scripts (Future)
**Rationale:**
- Some scenarios may need complex logic
- Support Groovy/JavaScript scripts in configuration
- `extraction_type: CUSTOM` for advanced use cases

**Not Implemented in POC:** Keep it simple for now

---

## 12. Documentation Decisions

### Decision: Configuration Schema as Code
**Rationale:**
- POJOs define the configuration structure
- JSON Schema can be auto-generated
- Type safety and validation

**Alternative Considered:** Free-form JSON
**Why Rejected:** No validation, prone to errors

---

### Decision: Example-Driven Documentation
**Rationale:**
- Provide real-world examples for each template type
- Show complete configuration with explanations
- Document execution flow step-by-step

**Examples Provided:**
1. Simple Statement (no external calls)
2. Privacy Policy (location-based)
3. Cardholder Agreement (2-step chain)
4. VIP Letter (3-step chain)

---

## Summary Table

| Decision Area | Decision Made | Key Rationale |
|--------------|---------------|---------------|
| **Stack** | Spring Boot WebFlux + R2DBC | Reactive, non-blocking for multiple API calls |
| **Database** | PostgreSQL with JSONB | Flexible schema, powerful JSON queries |
| **Cache** | Redis | Distributed, configurable TTL |
| **Configuration** | JSONB in database | No code changes for new rules |
| **Extraction** | Multi-step chaining | Real-world requirements |
| **Data Parsing** | JSONPath | Powerful, configuration-driven |
| **Template Design** | Template-specific configs | No over-engineering |
| **Error Handling** | Per-source strategies | Flexibility |
| **Caching** | Per-source TTL | Performance vs freshness |
| **Matching** | Multiple strategies | Different document types |
| **Performance** | Parallel processing | Reactive benefits |
| **Security** | Input validation | Don't trust external data |
| **Testing** | Mock external services | Fast, reliable tests |

---

## What We Are NOT Doing (Out of Scope for POC)

1. **Authentication/Authorization**: Assume caller is authenticated
2. **Admin UI**: Configuration done via SQL inserts
3. **Document Upload**: Only retrieval, not upload
4. **Document Versioning**: Simple version field, no complex versioning
5. **Audit Logging**: Basic logging only
6. **Multi-tenancy**: Single tenant
7. **Document Transformation**: Return documents as-is
8. **Real-time Notifications**: No event-driven updates
9. **Machine Learning**: No ML-based recommendations
10. **Advanced Analytics**: Basic metrics only

---

## Key Takeaways for Implementation

1. **Follow the Architecture**: Use the patterns defined in `ARCHITECTURE.md`
2. **Start Simple**: Implement basic scenarios first (Statement), then complex ones
3. **Test Incrementally**: Unit tests → Integration tests → End-to-end tests
4. **Use Sample Data**: Load sample templates and documents early
5. **Mock External Services**: Don't depend on real services during development
6. **Log Extensively**: Debug-level logging for extraction flow
7. **Monitor Performance**: Track API call latencies, cache hit rates
8. **Document Configurations**: Add comments to sample JSON configs

---

## Decision Log Format

For future decisions, use this format:

```
## Decision: [Title]

**Date**: YYYY-MM-DD
**Status**: Proposed | Accepted | Rejected | Superseded

**Context**:
[Describe the situation requiring a decision]

**Options Considered**:
1. Option A
2. Option B

**Decision**:
[What was decided]

**Rationale**:
[Why this decision was made]

**Consequences**:
- Positive: [Benefits]
- Negative: [Drawbacks]
- Neutral: [Trade-offs]

**Related Decisions**:
[Links to related decisions]
```

---

**Document Version**: 1.0
**Last Updated**: 2025-11-24
**Status**: Final
