# Template Management API - Gap Analysis

**Document Version:** 1.0
**API Spec Analyzed:** `docs/api-specs/document_template.yml` (v0.0.4)
**Analysis Date:** 2026-01-06
**Status:** Draft

---

## Executive Summary

The current Template Management API (`document_template.yml`) provides solid foundational CRUD operations but has gaps in workflow management, bulk operations, and security definitions that will be needed for production deployment.

**Overall Coverage:** ~70% of typical business scenarios

| Category | Coverage |
|----------|----------|
| Basic CRUD Operations | ✅ Complete |
| Template Versioning | ✅ Complete |
| Filtering & Search | ✅ Mostly Complete |
| Vendor Management | ✅ Complete |
| Configuration Schemas | ✅ Complete |
| Workflow/Approval | ⚠️ Partial |
| Bulk Operations | ❌ Missing |
| Security Definitions | ❌ Missing |

---

## 1. Critical Bugs in Current Spec

### 1.1 Property Name Typo (CRITICAL)

**Location:** Line 763-765 in `document_template.yml`

```yaml
# CURRENT (BUG)
notification required:    # <-- Space in property name!
  type: boolean

# SHOULD BE
notificationNeeded:
  type: boolean
  description: 'Indicates whether notification is required for this template.'
```

**Impact:** JSON parsing will fail or create unexpected property name.

---

### 1.2 Incorrect Property Capitalization (MEDIUM)

**Location:** Line 888 in `document_template.yml`

```yaml
# CURRENT (BUG)
DocumentMatchingConfig:    # <-- Should be camelCase
  type: object

# SHOULD BE
documentMatchingConfig:
  type: object
```

**Impact:** Inconsistent with other properties; may cause mapping issues in code generation.

---

### 1.3 Restrictive templateType Enum (MEDIUM)

**Location:** Line 748-752

```yaml
# CURRENT (Too restrictive)
templateType:
  enum:
    - monthly_statement
    - disclosure
    - electronic_disclosure
    - SV0019 (Indexed)

# SHOULD BE (Open string or expanded enum)
templateType:
  type: string
  description: 'Type of template (e.g., Statement, PrivacyNotice, ChangeInTerms)'
  example: 'Statement'
```

**Impact:** Cannot create templates for many document types (Statement, PrivacyNotice, ChangeInTermsNotice, etc.)

---

### 1.4 Missing CUSTOMER in X-requestor-type (MEDIUM)

**Location:** Line 650-652

```yaml
# CURRENT (Missing CUSTOMER)
XRequestorTypeParam:
  schema:
    enum:
      - AGENT
      - SYSTEM

# SHOULD BE
XRequestorTypeParam:
  schema:
    enum:
      - CUSTOMER
      - AGENT
      - SYSTEM
```

**Impact:** Cannot make API calls with CUSTOMER requestor type.

---

## 2. Missing Endpoints

### 2.1 Template Lifecycle Management

| Endpoint | Method | Description | Priority |
|----------|--------|-------------|----------|
| `/templates/{id}/clone` | POST | Clone an existing template | High |
| `/templates/{id}/activate` | POST | Activate a template version | High |
| `/templates/{id}/deactivate` | POST | Deactivate a template | High |
| `/templates/{id}/latest` | GET | Get latest active version | High |
| `/templates/{id}/archive` | POST | Archive a template | Medium |

#### Proposed Schema: Clone Template

```yaml
/templates/{templateId}/clone:
  post:
    tags:
      - Templates
    summary: Clone template
    description: Create a copy of an existing template with a new ID
    operationId: cloneTemplate
    parameters:
      - $ref: '#/components/parameters/MasterTemplateIdParam'
    requestBody:
      content:
        application/json:
          schema:
            type: object
            properties:
              newTemplateName:
                type: string
                description: Name for the cloned template
              newTemplateType:
                type: string
                description: Type for the cloned template
              copyVendorMappings:
                type: boolean
                default: false
                description: Whether to copy vendor mappings
    responses:
      '200':
        description: Template cloned successfully
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TemplateResponse'
```

---

### 2.2 Workflow/Approval Endpoints

| Endpoint | Method | Description | Priority |
|----------|--------|-------------|----------|
| `/templates/{id}/submit` | POST | Submit template for approval | High |
| `/templates/{id}/approve` | POST | Approve a submitted template | High |
| `/templates/{id}/reject` | POST | Reject a submitted template | High |
| `/templates/{id}/workflow-history` | GET | Get workflow state transitions | Medium |

#### Proposed Schema: Workflow Endpoints

```yaml
/templates/{templateId}/versions/{templateVersion}/submit:
  post:
    tags:
      - Template Workflow
    summary: Submit for approval
    description: Submit a draft template for review and approval
    operationId: submitTemplateForApproval
    parameters:
      - $ref: '#/components/parameters/MasterTemplateIdParam'
      - $ref: '#/components/parameters/TemplateVersionParam'
    requestBody:
      content:
        application/json:
          schema:
            type: object
            properties:
              comments:
                type: string
                description: Submission comments
              assignTo:
                type: string
                format: uuid
                description: Approver user ID
    responses:
      '200':
        description: Template submitted for approval
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/WorkflowResponse'

/templates/{templateId}/versions/{templateVersion}/approve:
  post:
    tags:
      - Template Workflow
    summary: Approve template
    description: Approve a submitted template
    operationId: approveTemplate
    requestBody:
      content:
        application/json:
          schema:
            type: object
            properties:
              comments:
                type: string
              effectiveDate:
                type: integer
                format: int64
                description: When the template becomes active (epoch ms)
    responses:
      '200':
        description: Template approved

/templates/{templateId}/versions/{templateVersion}/reject:
  post:
    tags:
      - Template Workflow
    summary: Reject template
    description: Reject a submitted template with reason
    operationId: rejectTemplate
    requestBody:
      content:
        application/json:
          schema:
            type: object
            required:
              - reason
            properties:
              reason:
                type: string
                description: Rejection reason
    responses:
      '200':
        description: Template rejected
```

---

### 2.3 Bulk Operations

| Endpoint | Method | Description | Priority |
|----------|--------|-------------|----------|
| `/templates/bulk` | POST | Bulk create templates | Medium |
| `/templates/bulk` | PATCH | Bulk update templates | Medium |
| `/templates/bulk/delete` | POST | Bulk delete templates | Medium |
| `/templates/bulk/activate` | POST | Bulk activate templates | Medium |

#### Proposed Schema: Bulk Operations

```yaml
/templates/bulk:
  post:
    tags:
      - Templates
    summary: Bulk create templates
    description: Create multiple templates in a single request
    operationId: bulkCreateTemplates
    requestBody:
      content:
        application/json:
          schema:
            type: object
            properties:
              templates:
                type: array
                maxItems: 100
                items:
                  $ref: '#/components/schemas/TemplateRequest'
    responses:
      '200':
        description: Bulk creation results
        content:
          application/json:
            schema:
              type: object
              properties:
                successful:
                  type: array
                  items:
                    $ref: '#/components/schemas/TemplateResponse'
                failed:
                  type: array
                  items:
                    type: object
                    properties:
                      index:
                        type: integer
                      error:
                        type: string
```

---

### 2.4 Analysis & History Endpoints

| Endpoint | Method | Description | Priority |
|----------|--------|-------------|----------|
| `/templates/{id}/history` | GET | Get audit history | Medium |
| `/templates/{id}/dependencies` | GET | Get dependent documents/systems | Medium |
| `/templates/{id}/versions/compare` | GET | Compare two versions | Low |
| `/templates/stats` | GET | Get template statistics | Low |

#### Proposed Schema: History Endpoint

```yaml
/templates/{templateId}/history:
  get:
    tags:
      - Templates
    summary: Get template history
    description: Retrieve audit history of all changes to a template
    operationId: getTemplateHistory
    parameters:
      - $ref: '#/components/parameters/MasterTemplateIdParam'
      - $ref: '#/components/parameters/PageParam'
      - $ref: '#/components/parameters/SizeParam'
    responses:
      '200':
        description: Template history retrieved
        content:
          application/json:
            schema:
              type: object
              properties:
                content:
                  type: array
                  items:
                    $ref: '#/components/schemas/AuditEntry'
                page:
                  $ref: '#/components/schemas/PageInfo'

# New Schema
AuditEntry:
  type: object
  properties:
    id:
      type: string
      format: uuid
    timestamp:
      type: integer
      format: int64
    action:
      type: string
      enum: [CREATED, UPDATED, DELETED, ACTIVATED, DEACTIVATED, APPROVED, REJECTED]
    userId:
      type: string
    changes:
      type: object
      description: JSON diff of changes
    comments:
      type: string
```

---

### 2.5 Export/Import Endpoints

| Endpoint | Method | Description | Priority |
|----------|--------|-------------|----------|
| `/templates/export` | GET | Export templates to JSON/YAML | Low |
| `/templates/import` | POST | Import templates from file | Low |

---

## 3. Missing Query Parameters

### 3.1 Date Range Filtering

```yaml
# Add to GET /templates
- name: startDateFrom
  in: query
  description: Filter templates with start date >= this value (epoch ms)
  schema:
    type: integer
    format: int64

- name: startDateTo
  in: query
  description: Filter templates with start date <= this value (epoch ms)
  schema:
    type: integer
    format: int64

- name: endDateFrom
  in: query
  description: Filter templates with end date >= this value (epoch ms)
  schema:
    type: integer
    format: int64

- name: endDateTo
  in: query
  description: Filter templates with end date <= this value (epoch ms)
  schema:
    type: integer
    format: int64
```

### 3.2 Workflow Status Filter

```yaml
- name: workflow
  in: query
  description: Filter by workflow status
  schema:
    type: string
    enum: [DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, ARCHIVED]
```

### 3.3 Created/Updated Date Filters

```yaml
- name: createdAfter
  in: query
  description: Filter templates created after this date (epoch ms)
  schema:
    type: integer
    format: int64

- name: createdBefore
  in: query
  description: Filter templates created before this date (epoch ms)
  schema:
    type: integer
    format: int64

- name: updatedAfter
  in: query
  description: Filter templates updated after this date (epoch ms)
  schema:
    type: integer
    format: int64
```

---

## 4. Security Definitions (Missing)

The API spec has no security scheme defined. Add:

```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT token for authentication

    apiKey:
      type: apiKey
      in: header
      name: X-API-Key
      description: API key for service-to-service calls

security:
  - bearerAuth: []
  - apiKey: []
```

---

## 5. Missing Response Headers

### 5.1 Rate Limiting Headers

```yaml
# Add to all responses
headers:
  X-RateLimit-Limit:
    description: Request limit per hour
    schema:
      type: integer
  X-RateLimit-Remaining:
    description: Remaining requests in current window
    schema:
      type: integer
  X-RateLimit-Reset:
    description: Time when the rate limit resets (epoch)
    schema:
      type: integer
```

### 5.2 ETag for Caching/Concurrency

```yaml
# Add to GET responses
headers:
  ETag:
    description: Entity tag for caching
    schema:
      type: string

# Add to PATCH/DELETE parameters
- name: If-Match
  in: header
  description: ETag for optimistic concurrency control
  schema:
    type: string
```

---

## 6. Schema Enhancements Needed

### 6.1 Workflow Status Enum

```yaml
WorkflowStatus:
  type: string
  description: Template workflow status
  enum:
    - DRAFT
    - PENDING_APPROVAL
    - APPROVED
    - REJECTED
    - ARCHIVED
    - ACTIVE
    - INACTIVE
```

### 6.2 Workflow Response Schema

```yaml
WorkflowResponse:
  type: object
  properties:
    templateId:
      type: string
      format: uuid
    templateVersion:
      type: integer
    previousStatus:
      $ref: '#/components/schemas/WorkflowStatus'
    currentStatus:
      $ref: '#/components/schemas/WorkflowStatus'
    transitionedBy:
      type: string
    transitionedAt:
      type: integer
      format: int64
    comments:
      type: string
```

---

## 7. Recommendations Summary

### High Priority (Before Production)

| # | Item | Effort |
|---|------|--------|
| 1 | Fix `notification required` typo | 5 min |
| 2 | Fix `DocumentMatchingConfig` capitalization | 5 min |
| 3 | Expand `templateType` enum or make open string | 10 min |
| 4 | Add CUSTOMER to X-requestor-type | 5 min |
| 5 | Add security scheme definitions | 30 min |
| 6 | Add `/templates/{id}/latest` endpoint | 1 hr |
| 7 | Add `/templates/{id}/activate` endpoint | 1 hr |

### Medium Priority (Phase 2)

| # | Item | Effort |
|---|------|--------|
| 8 | Add workflow endpoints (submit/approve/reject) | 4 hrs |
| 9 | Add `/templates/{id}/clone` endpoint | 2 hrs |
| 10 | Add date range query parameters | 1 hr |
| 11 | Add workflow status filter | 30 min |
| 12 | Add audit history endpoint | 2 hrs |

### Low Priority (Phase 3)

| # | Item | Effort |
|---|------|--------|
| 13 | Add bulk operations endpoints | 4 hrs |
| 14 | Add export/import endpoints | 4 hrs |
| 15 | Add version comparison endpoint | 2 hrs |
| 16 | Add ETag/conditional request support | 2 hrs |
| 17 | Add rate limiting headers | 1 hr |

---

## 8. Database Schema Alignment

The following database columns are NOT exposed in the API (intentionally for internal use):

| Column | Reason for Exclusion |
|--------|---------------------|
| `created_timestamp` | Auto-generated by database |
| `updated_by` | Auto-set from X-requestor-id |
| `updated_timestamp` | Auto-updated by database |
| `archive_indicator` | Managed via DELETE endpoint |
| `archive_timestamp` | Auto-set on archive |
| `version_number` | Optimistic locking (internal) |
| `template_type_old` | Legacy/deprecated |
| `document_channel_old` | Legacy/deprecated |

---

## Appendix A: Current vs Proposed Endpoint Coverage

```
CURRENT ENDPOINTS:
├── POST   /templates                              ✅ Create
├── GET    /templates                              ✅ List
├── GET    /templates/{id}                         ✅ Get by ID
├── GET    /templates/{id}/versions/{v}            ✅ Get version
├── PATCH  /templates/{id}/version/{v}             ✅ Update
├── DELETE /templates/{id}/version/{v}             ✅ Delete
├── POST   /templates/vendors                      ✅ Create vendor mapping
├── GET    /templates/vendors                      ✅ List vendor mappings
├── GET    /templates/{id}/vendors                 ✅ Get template vendors
├── GET    /templates/vendors/{vendorId}           ✅ Get vendor by ID
├── PATCH  /templates/vendors/{vendorId}           ✅ Update vendor
└── DELETE /templates/vendors/{vendorId}           ✅ Delete vendor

PROPOSED ADDITIONS:
├── GET    /templates/{id}/latest                  🆕 Get latest version
├── POST   /templates/{id}/clone                   🆕 Clone template
├── POST   /templates/{id}/activate                🆕 Activate
├── POST   /templates/{id}/deactivate              🆕 Deactivate
├── POST   /templates/{id}/versions/{v}/submit     🆕 Submit for approval
├── POST   /templates/{id}/versions/{v}/approve    🆕 Approve
├── POST   /templates/{id}/versions/{v}/reject     🆕 Reject
├── GET    /templates/{id}/history                 🆕 Audit history
├── GET    /templates/{id}/dependencies            🆕 Dependencies
├── GET    /templates/{id}/versions/compare        🆕 Compare versions
├── POST   /templates/bulk                         🆕 Bulk create
├── PATCH  /templates/bulk                         🆕 Bulk update
├── POST   /templates/bulk/delete                  🆕 Bulk delete
├── GET    /templates/export                       🆕 Export
└── POST   /templates/import                       🆕 Import
```

---

**Document maintained by:** Document Hub Team
**Last updated:** 2026-01-06
