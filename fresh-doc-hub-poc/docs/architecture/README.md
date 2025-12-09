# Document Hub - Architecture Documentation

This directory contains detailed architecture documentation for the Document Hub service.

## Documents

| Document | Description |
|----------|-------------|
| [Document Enquiry Flow](./document-enquiry-flow.md) | Comprehensive diagrams showing the `/documents-enquiry` endpoint logic |
| [Use Cases & Design Review](./use-cases-design-review.md) | Complete analysis of use cases, database, and API design |

## Quick Links

### Diagrams in document-enquiry-flow.md

1. **High-Level Sequence Diagram** - Main interaction flow between all components
2. **Data Extraction Chain Flow** - How multi-step API chains work
3. **Rule Evaluation Logic** - AND/OR operator processing flowchart
4. **Document Matching Strategy** - Reference key and conditional matching
5. **Sharing Scope Decision Tree** - Complete decision logic for all sharing types
6. **Complete End-to-End Flow** - Full request-to-response process

### Key Concepts

#### Sharing Scopes

| Scope | Use Case | Example |
|-------|----------|---------|
| `NULL` | Account-specific documents | Monthly statements |
| `ALL` | Universal documents | Privacy policy |
| `ACCOUNT_TYPE` | Product-based sharing | Credit card offers |
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
