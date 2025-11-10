# Document Hub API - JIRA Quick Reference Guide

**Created:** 2025-11-09
**Purpose:** One-page summary for project managers and stakeholders

---

## Executive Summary

This document provides JIRA ticket breakdown for implementing a Document Hub API that was prototyped but cannot be used due to compliance requirements. All work must be re-implemented following company standards.

### Business Value
- **Unified Document Access:** Single API for all customer document retrieval
- **Dynamic Eligibility:** Rule-based document selection based on account type and custom criteria
- **Multi-Vendor Support:** Flexible vendor management for document delivery (print, email)
- **Scalability:** Supports multiple lines of business and document types

### Total Effort Estimate
- **Stories:** 16 user stories across 5 epics
- **Story Points:** 117 SP
- **Hours:** 102-128 developer hours
- **Timeline:** 8 sprints (16 weeks / 4 months)
- **Team Size:** 1 Senior Dev, 1 Mid-level Dev, 1 Junior Dev (part-time)

---

## Epic Overview

```
EPIC-DOC-001: Core Infrastructure (Foundation)
├── Database schema design and migrations
├── Entity models with R2DBC reactive support
├── Spring Security with JWT authentication
└── Base configuration and setup
   📊 23 SP | 18-22 hours | Sprint 1-2

EPIC-DOC-002: Template Management
├── Template CRUD operations
├── Template configuration management (JSONB)
├── Template versioning
└── Soft delete (archiving)
   📊 8 SP | 6-8 hours | Sprint 3

EPIC-DOC-003: Document Enquiry & Retrieval
├── Basic document enquiry API
├── Shared document eligibility logic
├── API spec compliance (OpenAPI 3.0)
└── Pagination, filtering, sorting
   📊 21 SP | 20-25 hours | Sprint 4-5

EPIC-DOC-004: Generic Data Extraction Engine (Most Complex)
├── Extraction schema parser (JSONB config)
├── Multi-source data orchestration (parallel API calls)
├── Rule evaluation engine (conditional logic)
└── Integration and error handling
   📊 34 SP | 32-40 hours | Sprint 6-8

EPIC-DOC-005: Production Readiness
├── Error handling and validation
├── Monitoring and observability (Prometheus)
├── Caching strategy (Redis)
└── Security audit
   📊 31 SP | 26-33 hours | Sprint 9-10
```

---

## Sprint Plan (2-week sprints)

### Sprint 1-2: Foundation (Weeks 1-4)
**Goal:** Database, entities, security setup
- STORY-001: Database Schema Design (8 SP)
- STORY-002: Flyway Migrations (2 SP)
- STORY-003: Entity Models with R2DBC (5 SP)
- STORY-004: Spring Security & JWT Auth (8 SP)
- **Deliverable:** Working database with authentication
- **Team:** Senior Dev + Junior Dev + DBA

### Sprint 3: Template Management (Weeks 5-6)
**Goal:** Template CRUD operations
- STORY-005: Template CRUD API (8 SP)
- **Deliverable:** Create, read, update, delete templates
- **Team:** Mid-level Dev

### Sprint 4-5: Document Enquiry (Weeks 7-10)
**Goal:** Core document retrieval API
- STORY-006: Basic Document Enquiry API (8 SP)
- STORY-007: Shared Document Eligibility (13 SP)
- STORY-014: API Spec Compliance (5 SP)
- **Deliverable:** Working document enquiry endpoint with shared docs
- **Team:** Senior Dev + Mid-level Dev

### Sprint 6-8: Generic Extraction Engine (Weeks 11-16)
**Goal:** Dynamic document eligibility rules
- STORY-008: Extraction Schema Parser (8 SP)
- STORY-009: Multi-Source Data Orchestration (13 SP)
- STORY-010: Rule Evaluation Engine (8 SP)
- STORY-011: Integration (5 SP)
- **Deliverable:** Custom rule-based document eligibility
- **Team:** Senior Dev

### Sprint 9-10: Production Readiness (Weeks 17-20)
**Goal:** Production deployment ready
- STORY-012: Error Handling & Validation (5 SP)
- STORY-013: Monitoring & Observability (8 SP)
- STORY-015: Caching Strategy (5 SP)
- STORY-016: Security Audit (8 SP)
- **Deliverable:** Production-ready application
- **Team:** Full team + Security Engineer

---

## User Stories at a Glance

### 🔴 Critical Priority (Must Have)

| ID | Story | SP | Hours | Owner |
|----|-------|----|----|-------|
| STORY-001 | Database Schema Design | 8 | 6-8 | Senior + DBA |
| STORY-002 | Flyway Migrations | 2 | 1-2 | Junior |
| STORY-003 | Entity Models (R2DBC) | 5 | 4-5 | Mid |
| STORY-004 | Security & JWT Auth | 8 | 6-8 | Senior |
| STORY-006 | Document Enquiry API | 8 | 8-10 | Senior |
| STORY-014 | API Spec Compliance | 5 | 4-5 | Mid |

**Subtotal:** 36 SP | 29.5-38 hours

### 🟡 High Priority (Should Have)

| ID | Story | SP | Hours | Owner |
|----|-------|----|----|-------|
| STORY-005 | Template CRUD Operations | 8 | 6-8 | Mid |
| STORY-007 | Shared Document Eligibility | 13 | 12-15 | Senior |
| STORY-008 | Extraction Schema Parser | 8 | 8-10 | Senior |
| STORY-009 | Data Orchestration | 13 | 12-15 | Senior |
| STORY-010 | Rule Evaluation Engine | 8 | 8-10 | Senior |
| STORY-011 | Extraction Integration | 5 | 4-5 | Senior |
| STORY-012 | Error Handling | 5 | 4-5 | Mid |
| STORY-013 | Monitoring | 8 | 6-8 | Senior |

**Subtotal:** 68 SP | 60-76 hours

### 🟢 Medium Priority (Nice to Have)

| ID | Story | SP | Hours | Owner |
|----|-------|----|----|-------|
| STORY-015 | Caching Strategy | 5 | 4-5 | Mid |
| STORY-016 | Security Audit | 8 | 8-10 | Security + Senior |

**Subtotal:** 13 SP | 12-15 hours

---

## Key Technical Components

### 1. Database Schema
**Tables:**
- `master_template_definition` - Template metadata with JSONB config
  - Composite PK: (template_id, version)
  - JSONB columns: doc_supporting_data, template_config, data_extraction_schema
- `storage_index` - Document storage metadata
  - Links to templates
  - Account/Customer references

**Special Features:**
- JSONB for flexible configuration
- GIN indexes for JSONB query performance
- Composite key support (workaround for R2DBC limitation)

### 2. Document Enquiry API
**Endpoint:** `POST /api/v1/documents-enquiry`

**Features:**
- Retrieve account-specific documents
- Merge with shared documents (3 sharing scopes):
  - `all` - Available to everyone
  - `credit_card_account_only` - Account type based
  - `custom_rule` - Dynamic eligibility via extraction engine
- Pagination, filtering, sorting
- HATEOAS links

**Performance Target:** < 200ms for 1000+ documents

### 3. Generic Data Extraction Engine
**Purpose:** Evaluate custom eligibility rules for shared documents

**Flow:**
1. Parse JSON schema from `data_extraction_schema` column
2. Execute parallel API calls to multiple data sources (customer, account, transaction services)
3. Apply transformations (convert types, extract fields)
4. Evaluate conditions (GREATER_THAN, EQUALS, IN, etc.)
5. Return eligibility decision (true/false)

**Example Use Case:**
Show "Premium Cardholder Benefits" disclosure only to customers with:
- Account balance > $10,000 AND
- Customer tier = "GOLD" OR "PLATINUM" AND
- Last transaction within 30 days

**Performance Target:** < 500ms including API calls

### 4. Template Configuration (JSONB)
**Purpose:** Store operational settings per template

**Schema:**
```json
{
  "defaultPrintVendor": "SMARTCOMM",
  "defaultEmailVendor": "SENDGRID",
  "printVendorFailover": {
    "action": "SWITCH_TO_SECONDARY",
    "secondaryVendor": "ASSENTIS"
  },
  "uploadReferenceKeyField": "disclosureCode"
}
```

**Use Cases:**
- Print service routing decisions
- Email vendor selection
- Failover handling
- Dynamic reference key mapping

---

## Technical Stack

### Backend Framework
- **Spring Boot 3.x** with WebFlux (Reactive)
- **Java 17+**
- **Maven** for build

### Database
- **PostgreSQL 16+** with JSONB support
- **R2DBC** for reactive database access
- **Flyway** for schema migrations

### Security
- **Spring Security** with OAuth 2.0 Resource Server
- **JWT** token validation
- **Role-based access control** (RBAC)

### Resilience
- **Resilience4j** for circuit breaker and retry
- **WebClient** for non-blocking HTTP calls

### Caching
- **Redis** (reactive) for shared documents and eligibility results

### Monitoring
- **Micrometer** for metrics
- **Prometheus** for metrics collection
- **Actuator** for health checks

### API Documentation
- **Springdoc OpenAPI** for automatic API docs generation
- **Swagger UI** for interactive testing

---

## Dependencies on External Systems

### Required Integrations

1. **Customer Service API**
   - Get customer profile (tier, status)
   - Endpoint: `GET /api/v1/customers/{customerId}`

2. **Account Service API**
   - Get account details (balance, type, status)
   - Endpoint: `GET /api/v1/accounts/{accountId}`

3. **Transaction Service API**
   - Get transaction history
   - Endpoint: `POST /api/v1/transactions/query`

4. **Company SSO/OAuth Provider**
   - JWT token validation
   - User authentication

5. **Document Storage Service** (Optional)
   - S3 or SFTP for actual document files
   - Endpoint: `GET /api/v1/storage/download/{documentId}`

### Mock Services for Testing
Create mock implementations for all external APIs to enable independent development and testing.

---

## Risk Assessment

### High Risk Items

| Risk | Impact | Mitigation |
|------|--------|------------|
| R2DBC composite key limitation | Medium | Use DatabaseClient with raw SQL for complex queries |
| Generic Extraction Engine complexity | High | Start with simple rules, iterate. Allocate senior dev |
| External API dependencies | High | Create mock services, implement circuit breakers |
| Performance with custom rules | Medium | Cache eligibility results (15-min TTL), parallel execution |
| Security vulnerabilities | Critical | Early security audit, follow OWASP guidelines |

### Medium Risk Items

| Risk | Impact | Mitigation |
|------|--------|------------|
| Team learning curve (reactive) | Medium | Pair programming, code reviews |
| JSONB schema validation | Medium | JSON Schema validation in application layer |
| Database migration errors | Medium | Test migrations on DEV/QA first, have rollback scripts |

---

## Testing Strategy

### Unit Testing
- **Target:** 80%+ code coverage
- **Tools:** JUnit 5, Mockito, Reactor Test (StepVerifier)
- **Focus:** Service layer logic, rule evaluation, data transformation

### Integration Testing
- **Target:** All major flows covered
- **Tools:** @SpringBootTest, WebTestClient, Testcontainers (PostgreSQL)
- **Focus:** End-to-end API flows, database operations

### Performance Testing
- **Target:** 1000 requests/sec sustained
- **Tools:** JMeter, Gatling
- **Scenarios:**
  - Document enquiry with 100 concurrent users
  - Custom rule evaluation under load
  - Database query performance

### Security Testing
- **Target:** OWASP Top 10 compliance
- **Tools:** OWASP ZAP, SonarQube
- **Focus:**
  - SQL injection prevention
  - Authentication bypass testing
  - Authorization testing
  - Sensitive data exposure

### API Contract Testing
- **Target:** 100% OpenAPI spec compliance
- **Tools:** Springdoc OpenAPI, Postman collections
- **Focus:** Request/response schema validation

---

## Definition of Done

### Story-level DoD
- [ ] Code written and follows company standards
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written
- [ ] Code reviewed and approved (2 reviewers)
- [ ] Documentation updated (README, API docs)
- [ ] Deployed to DEV environment
- [ ] Manual testing completed
- [ ] Acceptance criteria met

### Epic-level DoD
- [ ] All stories in epic completed
- [ ] End-to-end integration testing passed
- [ ] Performance testing passed
- [ ] Security scan passed (no high/critical issues)
- [ ] Deployed to QA environment
- [ ] QA testing completed
- [ ] Demo to stakeholders completed

### Release-level DoD
- [ ] All epics completed
- [ ] Load testing passed (1000 req/sec)
- [ ] Security audit passed
- [ ] Documentation complete (API docs, operations runbook)
- [ ] Deployed to STAGING environment
- [ ] UAT completed and signed off
- [ ] Production deployment plan approved
- [ ] Rollback plan tested

---

## Success Metrics

### Performance Metrics
- **Response Time:** < 200ms (p95) for document enquiry
- **Throughput:** 1000 requests/sec sustained
- **Availability:** 99.9% uptime (excluding planned maintenance)
- **Error Rate:** < 0.1% of requests

### Quality Metrics
- **Code Coverage:** 80%+ for unit tests
- **Bug Density:** < 5 bugs per 1000 lines of code
- **Security Vulnerabilities:** 0 high/critical issues
- **Technical Debt Ratio:** < 5%

### Business Metrics
- **API Adoption:** 100% of channels using unified API within 6 months
- **Document Retrieval Success Rate:** > 99%
- **Shared Document Visibility:** 50%+ increase in regulatory disclosure views
- **Vendor Flexibility:** Support for 3+ print/email vendors

---

## Glossary

**Terms and Abbreviations:**

- **R2DBC:** Reactive Relational Database Connectivity (reactive alternative to JDBC)
- **JSONB:** Binary JSON format in PostgreSQL (efficient storage and indexing)
- **GIN Index:** Generalized Inverted Index (for JSONB queries)
- **HATEOAS:** Hypermedia As The Engine Of Application State (REST principle)
- **Circuit Breaker:** Pattern to prevent cascading failures
- **Resilience4j:** Library for resilience patterns (retry, circuit breaker, rate limiter)
- **WebFlux:** Spring's reactive web framework
- **Mono/Flux:** Reactive types (Mono = 0 or 1 element, Flux = 0 to N elements)
- **Extraction Engine:** Component that executes custom eligibility rules
- **Sharing Scope:** Determines which customers see a shared document
- **Template Config:** JSONB configuration for vendor preferences and operational settings

---

## References

### Prototype Documentation (Read-Only)
These documents describe what was built in the prototype. Use for reference only; code cannot be copied.

1. **Testing Reports:**
   - `docs/testing/template_config_deployment_test_report.md`
   - `docs/testing/document_enquiry_rest_endpoint_test_report.md`
   - `docs/testing/api_spec_validation_and_pending_items.md`

2. **Sequence Diagrams:**
   - `docs/sequence-diagrams/complete_api_sequence_diagrams.md`

3. **API Specification:**
   - `actual/api/schema.yaml` (OpenAPI 3.0)

4. **Database Schema:**
   - `actual/database/database_schema.md`

5. **JIRA Tickets:**
   - `docs/project-management/JIRA_TICKETS_FOR_COMPANY.md` (Part 1)
   - `docs/project-management/JIRA_TICKETS_PART2_PRODUCTION_READINESS.md` (Part 2)

### Company Standards (To Be Followed)
- Company Secure Coding Standards
- DBA Naming Conventions
- API Design Guidelines
- Security Requirements
- Logging Standards

---

## Contact and Escalation

### Project Team
- **Tech Lead:** TBD
- **Senior Developer:** TBD
- **Mid-level Developer:** TBD
- **Junior Developer:** TBD
- **DBA:** TBD
- **Security Engineer:** TBD

### Stakeholders
- **Product Owner:** TBD
- **Business Analyst:** TBD
- **QA Lead:** TBD
- **DevOps Lead:** TBD

### Escalation Path
1. **Technical Issues:** Tech Lead → Engineering Manager
2. **Scope Changes:** Product Owner → Program Manager
3. **Security Concerns:** Security Engineer → CISO
4. **Timeline Risks:** Scrum Master → Program Manager

---

## Quick Start for New Team Members

### Day 1: Setup
1. Clone repository
2. Install Java 17+, Maven, PostgreSQL, Redis
3. Review prototype documentation (read-only)
4. Set up IDE (IntelliJ IDEA recommended)
5. Run `mvn clean install` to verify setup

### Day 2-3: Understanding
1. Read OpenAPI specification
2. Review database schema
3. Study sequence diagrams
4. Watch architecture walkthrough (if available)
5. Read JIRA tickets for assigned sprint

### Day 4-5: Contribution
1. Pick first story from sprint backlog
2. Attend daily standup
3. Pair with senior developer
4. Submit first pull request
5. Attend code review session

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Next Review:** Start of Sprint 1

---

**END OF QUICK REFERENCE GUIDE**
