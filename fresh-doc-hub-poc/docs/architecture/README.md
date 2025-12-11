# Document Hub - Architecture Documentation

This directory contains detailed architecture documentation for the Document Hub service.

## Implementation Status

See [IMPLEMENTATION_STATUS.md](../IMPLEMENTATION_STATUS.md) for current progress and next steps.

## Documents

| Document | Description |
|----------|-------------|
| [Questions for John](../QUESTIONS_FOR_JOHN.md) | **ACTION REQUIRED** - Consolidated questions needing clarification |
| [Implementation Status](../IMPLEMENTATION_STATUS.md) | Current progress, completed work, and next steps |
| [TODO Backlog](../TODO_BACKLOG.md) | **43 actionable items** organized by priority for team assignment |
| [P0 Implementation Specs](../implementation-specs/P0-IMPLEMENTATION-SPECS.md) | **Detailed specs** for 9 critical (P0) items with code examples |
| [P1 HATEOAS Links Access Control](../implementation-specs/P1-HATEOAS-LINKS-ACCESS-CONTROL.md) | Filter action links by requestor type and template permissions |
| [API Specification Changes](../implementation-specs/API-SPECIFICATION-CHANGES.md) | All required changes to OpenAPI spec (doc-hub.yaml) |
| [Document Upload Flow](../implementation-specs/DOCUMENT-UPLOAD-FLOW.md) | Complete guide for uploading documents to Document Hub |
| [Document Enquiry Flow](./document-enquiry-flow.md) | Comprehensive diagrams showing the `/documents-enquiry` endpoint logic |
| [Message Center Doc Flag Options](./message-center-doc-flag-options.md) | Implementation options for message_center_doc_flag filter (pending decision) |
| [Use Cases & Design Review](./use-cases-design-review.md) | Analysis of current 10 use cases, database, and API design |
| [Business Use Cases (Comprehensive)](./business-use-cases-comprehensive.md) | Full 53 use cases for complete document management platform |
| [Gap Analysis](./gap-analysis.md) | Comparison of current implementation vs comprehensive use cases |
| [Schema Analysis: Versioning & Vendors](./schema-analysis-versioning-vendor.md) | Deep dive into template versioning and vendor mapping schema |
| [Demo Feedback Summary](../demo-feedback-summary.md) | Feedback from demo sessions and action items |

## Quick Links

### Diagrams in document-enquiry-flow.md

1. **High-Level Sequence Diagram** - Main interaction flow between all components
2. **Data Extraction Chain Flow** - How multi-step API chains work
3. **Rule Evaluation Logic** - AND/OR operator processing flowchart
4. **Document Matching Strategy** - Reference key and conditional matching
5. **Sharing Scope Decision Tree** - Complete decision logic for all sharing types
6. **Complete End-to-End Flow** - Full request-to-response process

### Key Concepts

#### Two-Step Filtering

| Step | Filter | Purpose |
|------|--------|---------|
| STEP 1 | `line_of_business` | Which business unit's templates to load (`CREDIT_CARD`, `DIGITAL_BANK`, `ENTERPRISE`) |
| STEP 1 | `template_type` | Filter by document type via `documentTypeCategoryGroup` (documentTypes = template_type) |
| STEP 2 | `sharing_scope` | Who can access within those templates |

#### Sharing Scopes

| Scope | Use Case | Example |
|-------|----------|---------|
| `NULL` | Account-specific documents | Monthly statements |
| `ALL` | Universal documents | Privacy policy |
| `CUSTOM_RULES` | Complex eligibility | VIP + Region targeting |

#### Data Extraction

The service can dynamically fetch data from external APIs to:
- Evaluate eligibility criteria
- Match documents by reference keys (e.g., disclosure codes)

#### Rule Evaluation

Supports operators: `EQUALS`, `NOT_EQUALS`, `IN`, `NOT_IN`, `GREATER_THAN`, `LESS_THAN`, `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`

## Viewing Diagrams

The diagrams use [Mermaid](https://mermaid.js.org/) syntax and can be rendered in:
- GitHub (native support)
- VS Code with Mermaid extension
- IntelliJ with Mermaid plugin
- Online at [mermaid.live](https://mermaid.live)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Request                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                  DocumentEnquiryController                       │
│                 (Validation, Header Processing)                  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   DocumentEnquiryService                         │
│                    (Main Orchestrator)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Template   │  │   Account   │  │     Document            │  │
│  │  Processing │──│  Processing │──│     Aggregation         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
          │                   │                    │
          ▼                   ▼                    ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────────┐
│ DataExtraction  │ │ RuleEvaluation  │ │    StorageIndex         │
│    Service      │ │    Service      │ │    Repository           │
│ (External APIs) │ │ (Eligibility)   │ │ (Document Queries)      │
└─────────────────┘ └─────────────────┘ └─────────────────────────┘
          │
          ▼
┌─────────────────┐
│  External APIs  │
│ (Pricing, etc.) │
└─────────────────┘
```
