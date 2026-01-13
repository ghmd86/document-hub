# Document Hub - Session Notes

**Date:** 2026-01-09
**Session Focus:** Template Wizard Enhancements, Documentation, API Analysis, Architecture

---

## Table of Contents

1. [Template Wizard Fixes](#1-template-wizard-fixes)
2. [Template Onboarding Documentation](#2-template-onboarding-documentation)
3. [Postman Collection Updates](#3-postman-collection-updates)
4. [Template API Gap Analysis](#4-template-api-gap-analysis)
5. [System Architecture Diagrams](#5-system-architecture-diagrams)
6. [Architecture Recommendations](#6-architecture-recommendations)
7. [Development Estimates](#7-development-estimates)
8. [Files Created/Modified](#8-files-createdmodified)

---

## 1. Template Wizard Fixes

### Location: `docs/template-wizard/index.html`

### Issues Fixed

#### 1.1 Validation Enhancements

Added validation for Step 3 (Required Fields) and Step 5 (Access Control):

```javascript
// Step 3: Validate required fields for category
} else if (currentStep === 3) {
    const category = document.getElementById('templateCategory').value || 'Other';
    const fields = fieldsByCategory[category] || fieldsByCategory['Other'];
    const requiredFields = fields.filter(f => f.required);
    for (const field of requiredFields) {
        const checkbox = document.querySelector(`.field-item input[data-name="${field.name}"]`);
        if (checkbox && !checkbox.checked) {
            alert(`Required field "${field.label}" must be selected for ${category} documents.`);
            return false;
        }
    }

    // Validate reference key source selection
    const refKeyType = document.getElementById('referenceKeyType').value;
    if (refKeyType && refKeyType !== '') {
        const refSource = document.querySelector('input[name="refKeySource"]:checked');
        if (!refSource) {
            alert('Please select where the reference key comes from.');
            return false;
        }
    }
}

// Step 5: Validate conditional access rules
} else if (currentStep === 5) {
    const ownership = document.querySelector('input[name="ownership"]:checked')?.value;
    if (ownership === 'conditional') {
        const rules = document.querySelectorAll('.access-rule');
        if (rules.length === 0) {
            alert('Please add at least one access rule for conditional access.');
            return false;
        }
    }
}
```

#### 1.2 SQL Generation Schema Alignment

Fixed SQL INSERT to match `master_template_definition` table schema:

**Removed (not in schema):**
- `accessible_flag`
- `mock_api_url`

**Added:**
- `display_name`
- `eligibility_criteria` (separate column)
- `document_matching_config`
- `communication_type`
- `single_document_flag`
- `workflow`

#### 1.3 New UI Fields Added

Added to Step 1 (Basic Info):
- Communication Type dropdown (Digital, Print, Both)
- Single Document Flag checkbox
- Workflow Status dropdown (Draft, Active, Inactive, Archived)

#### 1.4 Document Matching Config Auto-Generation

```javascript
let documentMatchingConfig = null;
if (templateData.referenceKeyType) {
    documentMatchingConfig = {
        matchBy: 'reference_key',
        referenceKeyType: templateData.referenceKeyType,
        referenceKeySource: templateData.referenceKeySource
    };
} else if (templateData.ownership === 'account') {
    documentMatchingConfig = { matchBy: 'account_key' };
} else if (templateData.ownership === 'customer') {
    documentMatchingConfig = { matchBy: 'customer_key' };
}
```

---

## 2. Template Onboarding Documentation

### Created Three Documentation Formats

| File | Format | Purpose |
|------|--------|---------|
| `onboarding-process.html` | Interactive HTML | 5-phase workflow with checklist |
| `onboarding-swimlane.html` | Swimlane Diagram | Lucidchart/Visio style flowchart |
| `onboarding-diagram.md` | Mermaid Diagrams | GitHub-compatible diagrams |

### Onboarding Phases

```
Phase 1: Request
├── Submit Template Request
├── Define Requirements
└── Provide Sample Document

Phase 2: Configuration
├── Use Template Wizard
├── Define Access Rules
├── Generate Config/SQL
└── IT Review & Approval

Phase 3: Technical Setup & UAT
├── Insert to DEV Database
├── Configure Vendor Mapping
├── Run Integration Tests
├── Load Test Data
└── Business UAT Testing

Phase 4: Deployment
├── Create Flyway Migration Script
├── Deploy to Production
├── Run Smoke Tests
└── Go-Live Confirmation
```

### Mermaid Diagrams Created

1. **Swimlane Flowchart** - Phase-based workflow
2. **Sequence Diagram** - Interaction between stakeholders
3. **State Diagram** - Template status transitions
4. **Gantt Chart** - Timeline visualization
5. **RACI Matrix** - Responsibility assignment

---

## 3. Postman Collection Updates

### Location: `postman/Document_Hub_POC_Demo.postman_collection.json`

### Added Section: "10 - Document Upload (doc-hub.yaml)"

Based on `src/main/resources/doc-hub.yaml` schema (NOT ecm.yaml).

| Request | Document Type | Key Fields |
|---------|--------------|------------|
| 10.1 | Statement | statementDate, accountNumber |
| 10.2 | Privacy Notice | effectiveDate, noticeType |
| 10.3 | Change In Terms | effectiveDate, changesDescription |
| 10.4 | Tax Document | taxYear, formType |
| 10.5 | Disclosure | disclosureType, version |
| 10.6 | Generic Upload | customFields |

### Required Fields (All Uploads)

```json
{
    "documentType": "Statement",
    "createdBy": "{{requestorId}}",
    "content": "<file>",
    "metadata": {
        "customerId": "uuid",
        "accountId": "uuid",
        "fileName": "string",
        "mimeType": "application/pdf"
    }
}
```

---

## 4. Template API Gap Analysis

### Location: `docs/api-specs/TEMPLATE_API_GAP_ANALYSIS.md`

### Critical Bugs Found

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| 1 | `notification required` (space in property) | Line 763 | JSON parsing fails |
| 2 | `DocumentMatchingConfig` (wrong case) | Line 888 | Code generation issues |
| 3 | Restrictive `templateType` enum | Line 748 | Cannot create many doc types |
| 4 | Missing CUSTOMER in requestor-type | Line 650 | Cannot make customer calls |

### Missing Endpoints

#### High Priority
- `GET /templates/{id}/latest` - Get latest active version
- `POST /templates/{id}/activate` - Activate template
- `POST /templates/{id}/deactivate` - Deactivate template
- `POST /templates/{id}/clone` - Clone template

#### Medium Priority (Workflow)
- `POST /templates/{id}/versions/{v}/submit` - Submit for approval
- `POST /templates/{id}/versions/{v}/approve` - Approve template
- `POST /templates/{id}/versions/{v}/reject` - Reject template
- `GET /templates/{id}/history` - Audit history

#### Low Priority
- Bulk operations (create/update/delete)
- Export/Import endpoints
- Version comparison

### Missing Features

- Security scheme definitions (JWT, API Key)
- Rate limiting headers
- ETag/conditional request support
- Date range query parameters
- Workflow status filter

### Coverage Summary

| Category | Status |
|----------|--------|
| Basic CRUD | ✅ Complete |
| Versioning | ✅ Complete |
| Filtering | ✅ Mostly Complete |
| Vendor Management | ✅ Complete |
| Workflow/Approval | ⚠️ Partial |
| Bulk Operations | ❌ Missing |
| Security | ❌ Missing |

**Overall Coverage:** ~70%

---

## 5. System Architecture Diagrams

### Location: `docs/architecture/document-hub-sequence-diagram.md`

### Components Documented

| Component | Type | Description |
|-----------|------|-------------|
| Client | External | Mobile/Web App, API Consumer |
| Document Hub API | Service | Spring Boot application |
| PostgreSQL | Database | Templates, storage index |
| ECMS | External | Document storage |
| Mock APIs | External | Customer, Account, Credit APIs |
| Kafka | Message Broker | Event streaming |
| Reporting DB | Database | `doc_reporting.document_events_ledger` |
| Event Consumer | Service | Kafka event processor |

### Sequence Diagrams Created

1. **Document Upload Flow** - Client → API → ECMS → Kafka
2. **Document Enquiry Flow** - With eligibility evaluation
3. **Document Download Flow** - With access control check
4. **Template Management Flow** - Admin operations
5. **Data Extraction Flow** - Parallel API calls for eligibility

### Kafka Event Schema

**Topic: `document-events`**
```json
{
    "eventId": "uuid",
    "eventType": "DOCUMENT_UPLOADED | DOWNLOADED | VIEWED | DELETED",
    "timestamp": 1704067200000,
    "payload": {
        "documentId": "uuid",
        "templateType": "Statement",
        "customerId": "uuid",
        "requestorType": "CUSTOMER | AGENT | SYSTEM",
        "action": "UPLOAD | DOWNLOAD | VIEW | DELETE"
    }
}
```

### Document Events Ledger Schema

```sql
CREATE TABLE doc_reporting.document_events_ledger (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    correlation_id UUID,
    document_id UUID,
    template_type VARCHAR(100),
    customer_id UUID,
    account_id UUID,
    requestor_id UUID,
    requestor_type VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## 6. Architecture Recommendations

### Question: Should Template Management and Document Management be in same repo?

### Recommendation: **Separate Repositories** (for high-scale scenarios)

### Proposed Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│    Template     │  │    Document     │  │    Reporting    │
│   Management    │  │   Management    │  │     Service     │
│    Service      │  │    Service      │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Template DB   │  │  Document DB    │  │  Reporting DB   │
│   (PostgreSQL)  │  │  (PostgreSQL)   │  │  (PostgreSQL)   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Reasons for Separation

| Factor | Template Management | Document Management |
|--------|--------------------|--------------------|
| Traffic Pattern | Low (admin only) | High (customer facing) |
| Scaling Needs | Minimal | Auto-scale required |
| Change Frequency | Rare | Frequent |
| Data Sensitivity | Configuration | Customer documents |
| Caching Strategy | Aggressive | Per-request |

### Shared Components

- **Common Library**: DTOs, utilities, validation
- **API Gateway**: Routing, authentication
- **Event Bus**: Kafka for cross-service events

---

## 7. Development Estimates

### For Template Management API (3-4 Developers)

### Phase Breakdown

| Phase | Duration | Focus |
|-------|----------|-------|
| Phase 1 | 2 weeks | Foundation & bug fixes |
| Phase 2 | 4 weeks | Core features (versioning, activate/clone) |
| Phase 3 | 4 weeks | Workflow (submit/approve/reject) |
| Phase 4 | 2 weeks | Production hardening |

### Story Points by Feature

| Feature Area | Story Points |
|--------------|--------------|
| Bug fixes & security | 8 |
| Base CRUD operations | 13 |
| Versioning logic | 8 |
| Lifecycle endpoints | 16 |
| Vendor mapping | 8 |
| Workflow state machine | 21 |
| Kafka integration | 8 |
| Testing (all types) | 18 |
| Documentation | 6 |

**Total: ~133 story points**

### Timeline Summary

| Scenario | Duration | Team Size |
|----------|----------|-----------|
| MVP (CRUD + versioning) | 6-8 weeks | 2-3 devs |
| Full Feature Set | 12-14 weeks | 3-4 devs |

### Team Allocation

```
Developer 1 (Senior): Architecture, workflow, security
Developer 2 (Mid):    CRUD, versioning, vendor mapping
Developer 3 (Mid):    Testing, Kafka integration
Developer 4 (Junior): Bug fixes, docs, query filters
```

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| Workflow complexity | +2 weeks | Start with 3-state workflow |
| Kafka delays | +1 week | Mock in dev environment |
| Security requirements | +1 week | Define early |
| Scope creep | +2 weeks | Keep Phase 4 optional |

---

## 8. Files Created/Modified

### New Files Created

| File | Description |
|------|-------------|
| `docs/template-wizard/onboarding-process.html` | Interactive onboarding workflow |
| `docs/template-wizard/onboarding-swimlane.html` | Swimlane diagram (Visio style) |
| `docs/template-wizard/onboarding-diagram.md` | Mermaid diagrams |
| `docs/api-specs/TEMPLATE_API_GAP_ANALYSIS.md` | API gap analysis document |
| `docs/architecture/document-hub-sequence-diagram.md` | System sequence diagrams |
| `docs/SESSION_NOTES_2026-01-09.md` | This session documentation |

### Modified Files

| File | Changes |
|------|---------|
| `docs/template-wizard/index.html` | Validation fixes, new fields, SQL generation |
| `postman/Document_Hub_POC_Demo.postman_collection.json` | Added upload examples |

---

## Quick Reference

### Key Decisions Made

1. **Template Wizard**: Must validate required fields per category and reference key source
2. **SQL Generation**: Must match exact database schema columns
3. **Onboarding Process**: 4-phase workflow (Request → Config → UAT → Deploy)
4. **API Gaps**: 30% missing functionality for production
5. **Architecture**: Separate microservices recommended for high scale
6. **Timeline**: 12-14 weeks for full template management API

### Next Steps

1. [ ] Fix critical bugs in `document_template.yml`
2. [ ] Implement missing high-priority endpoints
3. [ ] Add security scheme definitions
4. [ ] Set up Kafka integration for event streaming
5. [ ] Create Flyway migrations for production deployment

---

**Document maintained by:** Document Hub Team
**Session Date:** 2026-01-09
