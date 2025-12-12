# Document Hub - TODO Backlog

**Created:** December 2024
**Source:** Demo Feedback Sessions (Day 1, Day 2, Day 3)
**Last Updated:** December 2024

This document contains all actionable items extracted from the demo feedback sessions with John Drum. Items are prioritized and ready for assignment.

---

## Priority Legend

| Priority | Description | Timeline |
|----------|-------------|----------|
| P0 | Critical - Blocking other work | Immediate |
| P1 | High - Required for MVP | Before Jan 15 |
| P2 | Medium - Important for completeness | Q1 2025 |
| P3 | Low - Nice to have | Future |

## Status Legend

| Status | Description |
|--------|-------------|
| TODO | Not started |
| IN_PROGRESS | Currently being worked on |
| BLOCKED | Waiting on external dependency |
| DONE | Completed |

---

## P0 - Critical (Blocking)

### API Parameters - Document Enquiry

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P0-001 | Add `message_center_doc_flag` parameter | Add to DocumentListRequest. Default: `true`. If `false`, return both message center and non-message center docs. | TBD | TODO | Day 3 |
| P0-002 | Add `communication_type` parameter | Add to DocumentListRequest. Default: `LETTER`. Values: LETTER, EMAIL, SMS, PUSH. | TBD | TODO | Day 3 |
| P0-003 | Implement `message_center_doc_flag` filter | Add filter to MasterTemplateRepository queries based on API parameter. | TBD | TODO | Day 2, Day 3 |
| P0-004 | Implement `communication_type` filter | Filter templates by communication_type (maps to template_type). | TBD | TODO | Day 1, Day 3 |
| P0-005 | Verify `line_of_business` default | Ensure default is `CREDIT_CARD` when not provided. | TBD | TODO | Day 3 |

### Database Schema

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P0-006 | Add `workflow` column to master_template_definition | For WCM workflow selection (2_EYES, 4_EYES). | TBD | TODO | Day 3 |
| P0-007 | Add `doc_creation_date` column to storage_index | If not exists. Used for postedFromDate/postedToDate filtering. | TBD | TODO | Day 3 |

### Core Functionality

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P0-008 | Implement postedFromDate/postedToDate filter | Filter on `doc_creation_date` column (not `created_timestamp`). | TBD | TODO | Day 3 |
| P0-009 | Ensure single document return for current queries | Query must return only ONE document for a given template at current time. | TBD | TODO | Day 3 |

---

## P1 - High Priority (MVP)

### API Implementation

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P1-001 | Add `accessible_flag` filter | Add to all template queries. Clarify meaning first. | TBD | TODO | Day 2 |
| P1-002 | Implement date overlap validation on upload | Prevent overlapping date ranges for same template type. | TBD | TODO | Day 3 |
| P1-003 | Add HATEOAS link expiration | Add `expires_at` timestamp to download links. | TBD | TODO | Day 2 |
| P1-004 | Remove delete link from enquiry response | Delete action should not appear in document enquiry response. | TBD | TODO | Day 2 |
| P1-005 | Filter actions by template metadata | Only show permitted actions based on `access_control` field. | TBD | TODO | Day 2 |

### Template Management

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P1-006 | Create basic statement template | For test environment - allow statement uploads. | TBD | TODO | Day 3 |
| P1-007 | Create basic shared document templates | Privacy policy, disclosures, etc. for testing. | TBD | TODO | Day 3 |
| P1-008 | Add communication_type to GET templates endpoint | Filter templates by communication type in query params. | Jagadeesh | TODO | Day 1, Day 3 |

### Vendor Configuration

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P1-009 | Categorize vendors by communication type | Map vendors to allowed communication types (e.g., Smartcom -> Letter). | TBD | TODO | Day 1 |
| P1-010 | Implement soft delete cascade for vendors | When template is soft-deleted, cascade to vendor mappings. | Jagadeesh | TODO | Day 1 |

### Integration

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P1-011 | Create Kafka topic for letter print | Topic: `communication_delivery_letter_print`. | Prachi | TODO | Day 3 |
| P1-012 | Implement PDF file drop to directory | Drop PDF to legacy directory, then API call to update DB. | Prachi | TODO | Day 3 |
| P1-013 | Integrate with Sentista/Smartcom | Connect to print fulfillment system. | TBD | TODO | Day 3 |

---

## P2 - Medium Priority (Completeness)

### PDF Generation (Prachi)

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P2-001 | Design swappable PDF template architecture | Templates per SDK, easy to swap out. | Prachi | TODO | Day 3 |
| P2-002 | Create PDF Box template for cover page | Match legacy system exactly (logo + address position critical). | Prachi | TODO | Day 3 |
| P2-003 | Verify cover page fits in envelope | Test print and check with back office. | Prachi | TODO | Day 3 |
| P2-004 | Get compliance approval for cover page | Work with Alison's team. | Prachi | BLOCKED | Day 3 |
| P2-005 | Get security approval for PDF Box | Waiting on security team response. | Prachi | BLOCKED | Day 3 |

### Documentation & Standards

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P2-006 | Research trace_id header purpose | Document what trace_id is for, check if C365 standard. | TBD | TODO | Day 1 |
| P2-007 | Verify X-version header implementation | Confirm it's API version per EA standard. Check with Usharani. | Jagadeesh | TODO | Day 3 |
| P2-008 | Add headers to data dictionary | Document all header parameters and their purposes. | TBD | TODO | Day 1 |
| P2-009 | Document domain_id source | Where does domain_id come from in data extraction? | TBD | TODO | Day 2 |

### Performance Optimization

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P2-010 | Evaluate storage_index date fields | Column vs JSON for start_date/end_date - which is faster? | Taher | TODO | Day 3 |
| P2-011 | Optimize queries for speed | Ensure all queries are performant per John's requirement. | TBD | TODO | Day 3 |

### API Enhancements

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P2-012 | Implement customerId filter fully | Currently partial - make fully functional. | TBD | TODO | Day 2 |
| P2-013 | Add regulatory_flag handling | Determine if it overrides message_center_doc_flag. | TBD | TODO | Day 2 |

---

## P3 - Low Priority (Future)

### CRUD Endpoints

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P3-001 | Implement document upload endpoint | POST /documents | TBD | TODO | Day 1 |
| P3-002 | Implement document download endpoint | GET /documents/{id} | TBD | TODO | Day 1 |
| P3-003 | Implement document delete endpoint | DELETE /documents/{id} | TBD | TODO | Day 1 |
| P3-004 | Implement template CRUD endpoints | Full template management API. | TBD | TODO | Day 1 |

### Developer Experience

| ID | Task | Description | Owner | Status | Source |
|----|------|-------------|-------|--------|--------|
| P3-005 | Create developer quickstart guide | Step-by-step onboarding documentation. | TBD | TODO | Day 2 |
| P3-006 | Update Postman collection | Ready-to-use request examples. | TBD | TODO | Day 2 |
| P3-007 | Document error codes | Common errors and resolutions. | TBD | TODO | Day 2 |
| P3-008 | Create video walkthrough | Record demo for async review. | TBD | TODO | Day 2 |

---

## Completed Items

| ID | Task | Description | Owner | Completed Date |
|----|------|-------------|-------|----------------|
| DONE-001 | Add tracking headers to all endpoints | X-correlation-id, X-requestor-id | Jagadeesh | Day 2 |
| DONE-002 | Remove template_version from POST | Version auto-assigned on creation | Jagadeesh | Day 2 |
| DONE-003 | Document looping operation | Created sequence diagrams in architecture docs | Taher | Day 2 |
| DONE-004 | Implement two-step filtering | line_of_business (STEP 1) + sharing_scope (STEP 2) | Taher | Post-Day 2 |
| DONE-005 | Remove ACCOUNT_TYPE from sharing_scope | Redundant with line_of_business | Taher | Post-Day 2 |
| DONE-006 | Implement documentTypeCategoryGroup filter | Maps to template_type in database | Taher | Post-Day 2 |
| DONE-007 | Add lineOfBusiness to API | Added enum to DocumentListRequest | Taher | Post-Day 2 |
| DONE-008 | Add communication_type to GET templates | Added as query parameter | Jagadeesh | Day 3 |

---

## Blocked Items

| ID | Task | Blocked By | Owner | Notes |
|----|------|------------|-------|-------|
| P2-004 | Compliance approval for cover page | Waiting on Alison's team | Prachi | Need to schedule review |
| P2-005 | Security approval for PDF Box | Waiting on security team | Prachi | Response pending |

---

## Dependencies

```
P0-001 (message_center_doc_flag param) → P0-003 (implement filter)
P0-002 (communication_type param) → P0-004 (implement filter)
P0-006 (workflow column) → Template management features
P0-007 (doc_creation_date) → P0-008 (posted date filter)
P1-006 (statement template) → P1-013 (Sentista integration)
P2-001 (swappable templates) → P2-002 (PDF Box template)
P2-002 (cover page) → P2-003 (envelope test) → P2-004 (compliance)
```

---

## Sprint Recommendations

### Sprint 1 (Current - Before Jan 15)

**Focus:** Get system operational for test environment

| ID | Task | Assignee |
|----|------|----------|
| P0-001 | Add message_center_doc_flag parameter | |
| P0-002 | Add communication_type parameter | |
| P0-003 | Implement message_center_doc_flag filter | |
| P0-004 | Implement communication_type filter | |
| P0-005 | Verify line_of_business default | |
| P0-008 | Implement posted date filter | |
| P1-006 | Create basic statement template | |

### Sprint 2 (Jan 15 - Jan 31)

**Focus:** Complete MVP features

| ID | Task | Assignee |
|----|------|----------|
| P0-006 | Add workflow column | |
| P0-009 | Ensure single document return | |
| P1-001 | Add accessible_flag filter | |
| P1-002 | Date overlap validation | |
| P1-007 | Create shared document templates | |
| P1-011 | Create Kafka topic | |

### Sprint 3 (Feb)

**Focus:** Integration and polish

| ID | Task | Assignee |
|----|------|----------|
| P1-003 | HATEOAS link expiration | |
| P1-004 | Remove delete link | |
| P1-005 | Filter actions by metadata | |
| P1-012 | PDF file drop | |
| P1-013 | Sentista integration | |

---

## Metrics

| Category | Total | Done | In Progress | TODO | Blocked |
|----------|-------|------|-------------|------|---------|
| P0 - Critical | 9 | 0 | 0 | 9 | 0 |
| P1 - High | 13 | 0 | 0 | 13 | 0 |
| P2 - Medium | 13 | 0 | 0 | 11 | 2 |
| P3 - Low | 8 | 0 | 0 | 8 | 0 |
| **Total** | **43** | **0** | **0** | **41** | **2** |

**Completed separately:** 8 items (tracked in Completed Items section)

---

## Notes

1. **Deep dive with Saurabh:** January 15th, 1:00 PM
2. **John returns:** January 3rd (available for questions)
3. **Critical path:** P0 items → Basic templates → Integration → Production

---

## Change Log

| Date | Change | Author |
|------|--------|--------|
| Dec 2024 | Initial creation from Day 1-3 feedback | Team |
