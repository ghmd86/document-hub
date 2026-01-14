# Template Management Service - Integration Sequence Diagrams

## Overview

This document illustrates how the Template Management Service integrates with external services including Print Partner Service, Letter API Service (SmartComm/Assentis), and other downstream systems for document generation and delivery.

---

## Components

| Component | Type | Description |
|-----------|------|-------------|
| **Client** | External | Mobile App, Web App, Agent Portal, or System |
| **Template Management Service** | Core Service | Manages templates, versions, and vendor mappings |
| **Letter API Service** | Vendor Service | Document generation engine (SmartComm, Assentis, Handlebars) |
| **Print Partner Service** | Vendor Service | Physical mail printing and delivery (LPS, etc.) |
| **Document Hub API** | Core Service | Document storage, retrieval, and enquiry |
| **Kafka** | Message Broker | Event streaming for async processing |
| **PostgreSQL** | Database | Template and document metadata storage |

---

## 1. Template Onboarding with Vendor Mapping

This flow shows how a new template is created and mapped to generation and print vendors.

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin Portal
    participant TMS as Template Management<br/>Service
    participant DB as PostgreSQL
    participant LetterAPI as Letter API Service<br/>(SmartComm/Assentis)
    participant PrintPartner as Print Partner<br/>Service
    participant Kafka as Kafka

    rect rgb(240, 248, 255)
        Note over Admin,DB: Phase 1: Create Template Definition
        Admin->>+TMS: POST /templates<br/>(template metadata)
        TMS->>+DB: INSERT master_template_definition
        DB-->>-TMS: Template ID + Version 1
        TMS->>Kafka: TemplateCreatedEvent
        TMS-->>-Admin: 201 Created (templateId)
    end

    rect rgb(255, 248, 240)
        Note over Admin,LetterAPI: Phase 2: Configure Generation Vendor
        Admin->>+TMS: POST /templates/vendors<br/>(vendor=SmartComm)
        TMS->>+LetterAPI: Validate template key exists
        LetterAPI-->>-TMS: Template validated
        TMS->>+DB: INSERT template_vendor_mapping<br/>(vendorType=GENERATION)
        DB-->>-TMS: Vendor mapping created
        TMS->>Kafka: VendorMappingCreatedEvent
        TMS-->>-Admin: 201 Created (vendorMappingId)
    end

    rect rgb(240, 255, 240)
        Note over Admin,PrintPartner: Phase 3: Configure Print Vendor
        Admin->>+TMS: POST /templates/vendors<br/>(vendor=LPS, vendorType=PRINT)
        TMS->>+PrintPartner: Validate print account
        PrintPartner-->>-TMS: Account validated
        TMS->>+DB: INSERT template_vendor_mapping<br/>(vendorType=PRINT)
        DB-->>-TMS: Print vendor mapping created
        TMS->>Kafka: VendorMappingCreatedEvent
        TMS-->>-Admin: 201 Created (printVendorMappingId)
    end
```

---

## 2. Document Generation Flow (Letter API Integration)

This flow shows how a document is generated using the Letter API Service based on template configuration.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client System
    participant DocHub as Document Hub API
    participant TMS as Template Management<br/>Service
    participant DB as PostgreSQL
    participant DataAPI as External Data APIs
    participant LetterAPI as Letter API Service<br/>(SmartComm)
    participant ECMS as ECMS Storage
    participant Kafka as Kafka

    Client->>+DocHub: POST /documents/generate<br/>{templateId, customerId, accountId}

    rect rgb(240, 248, 255)
        Note over DocHub,TMS: Get Template Configuration
        DocHub->>+TMS: GET /templates/{id}/versions/{version}
        TMS->>+DB: SELECT template + vendor mappings
        DB-->>-TMS: Template config + SmartComm mapping
        TMS-->>-DocHub: Template with vendorTemplateKey
    end

    rect rgb(255, 248, 240)
        Note over DocHub,DataAPI: Extract Required Data
        DocHub->>+DataAPI: GET /customers/{customerId}
        DataAPI-->>-DocHub: Customer data (name, address)
        DocHub->>+DataAPI: GET /accounts/{accountId}
        DataAPI-->>-DocHub: Account data (balance, dates)
    end

    rect rgb(240, 255, 240)
        Note over DocHub,LetterAPI: Generate Document via Letter API
        DocHub->>+LetterAPI: POST /generate<br/>{vendorTemplateKey, mergeData}
        Note right of LetterAPI: SmartComm processes<br/>template + data
        LetterAPI-->>-DocHub: Generated PDF (binary)
    end

    rect rgb(255, 240, 255)
        Note over DocHub,ECMS: Store Generated Document
        DocHub->>+ECMS: Upload document (PDF)
        ECMS-->>-DocHub: Storage document key
        DocHub->>+DB: INSERT storage_index
        DB-->>-DocHub: Document record created
    end

    DocHub->>Kafka: DocumentGeneratedEvent
    DocHub-->>-Client: 201 Created {documentId, downloadUrl}
```

---

## 3. Print Delivery Flow (Print Partner Integration)

This flow shows how a generated document is sent to the Print Partner Service for physical mailing.

```mermaid
sequenceDiagram
    autonumber
    participant DocHub as Document Hub API
    participant TMS as Template Management<br/>Service
    participant DB as PostgreSQL
    participant ECMS as ECMS Storage
    participant PrintPartner as Print Partner<br/>Service (LPS)
    participant Kafka as Kafka

    Note over DocHub: Document already generated and stored

    rect rgb(240, 248, 255)
        Note over DocHub,TMS: Get Print Vendor Configuration
        DocHub->>+TMS: GET /templates/vendors?templateId={id}&vendorType=PRINT
        TMS->>+DB: SELECT print vendor mapping
        DB-->>-TMS: Print vendor config (LPS)
        TMS-->>-DocHub: Print vendor details + API config
    end

    rect rgb(255, 248, 240)
        Note over DocHub,ECMS: Retrieve Document for Print
        DocHub->>+ECMS: GET document content
        ECMS-->>-DocHub: PDF binary
    end

    rect rgb(240, 255, 240)
        Note over DocHub,PrintPartner: Submit Print Job
        DocHub->>+PrintPartner: POST /print-jobs<br/>{document, recipientAddress, mailClass}
        PrintPartner-->>-DocHub: 202 Accepted {printJobId}
    end

    DocHub->>Kafka: PrintJobSubmittedEvent
    DocHub->>+DB: UPDATE storage_index<br/>(delivery_status = PRINT_SUBMITTED)
    DB-->>-DocHub: Updated

    rect rgb(255, 255, 224)
        Note over PrintPartner,Kafka: Async Status Updates
        PrintPartner-->>Kafka: PrintJobStatusEvent<br/>(PRINTING)
        PrintPartner-->>Kafka: PrintJobStatusEvent<br/>(MAILED)
        PrintPartner-->>Kafka: PrintJobStatusEvent<br/>(DELIVERED)
    end
```

---

## 4. Multi-Channel Delivery Flow

This flow shows generating and delivering the same content across multiple channels.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client System
    participant DocHub as Document Hub API
    participant TMS as Template Management<br/>Service
    participant LetterAPI as Letter API<br/>(SmartComm)
    participant PrintPartner as Print Partner<br/>(LPS)
    participant EmailService as Email Service<br/>(SendGrid)
    participant Kafka as Kafka

    Client->>+DocHub: POST /documents/generate<br/>{templateId, channels: [PRINT, EMAIL, DIGITAL]}

    rect rgb(240, 248, 255)
        Note over DocHub,TMS: Get Template & All Vendor Mappings
        DocHub->>+TMS: GET /templates/{id}/versions/{v}
        TMS-->>-DocHub: Template + all vendor mappings
    end

    rect rgb(255, 248, 240)
        Note over DocHub,LetterAPI: Generate PDF Document
        DocHub->>+LetterAPI: POST /generate (PDF)
        LetterAPI-->>-DocHub: PDF document
    end

    par Parallel Delivery
        rect rgb(240, 255, 240)
            Note over DocHub,PrintPartner: Print Channel
            DocHub->>+PrintPartner: POST /print-jobs
            PrintPartner-->>-DocHub: Print job accepted
        end
    and
        rect rgb(255, 240, 255)
            Note over DocHub,EmailService: Email Channel
            DocHub->>+EmailService: POST /send<br/>(PDF attachment)
            EmailService-->>-DocHub: Email queued
        end
    end

    DocHub->>Kafka: MultiChannelDeliveryEvent
    DocHub-->>-Client: 201 Created<br/>{documentId, deliveryStatus}
```

---

## 5. Template Version Update with Vendor Re-validation

This flow shows updating a template version and re-validating with vendors.

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin Portal
    participant TMS as Template Management<br/>Service
    participant DB as PostgreSQL
    participant LetterAPI as Letter API<br/>(SmartComm)
    participant PrintPartner as Print Partner
    participant Kafka as Kafka

    Admin->>+TMS: PATCH /templates/{id}/versions/{v}<br/>(updated template config)

    rect rgb(240, 248, 255)
        Note over TMS,DB: Create New Version
        TMS->>+DB: Check current version status
        DB-->>-TMS: Version 1 is ACTIVE
        TMS->>+DB: INSERT new version (v2) as DRAFT
        DB-->>-TMS: Version 2 created
    end

    rect rgb(255, 248, 240)
        Note over TMS,LetterAPI: Re-validate with Generation Vendor
        TMS->>+LetterAPI: POST /templates/validate<br/>{vendorTemplateKey, schema}
        LetterAPI-->>-TMS: Validation result
    end

    alt Validation Failed
        TMS-->>Admin: 400 Bad Request<br/>(validation errors)
    else Validation Passed
        TMS->>+DB: UPDATE version status = PENDING_APPROVAL
        DB-->>-TMS: Updated
        TMS->>Kafka: TemplateVersionCreatedEvent
        TMS-->>-Admin: 200 OK (new version details)
    end
```

---

## 6. Vendor Failover Flow

This flow shows how the system handles vendor failures with automatic failover.

```mermaid
sequenceDiagram
    autonumber
    participant DocHub as Document Hub API
    participant TMS as Template Management<br/>Service
    participant DB as PostgreSQL
    participant Primary as Primary Vendor<br/>(SmartComm)
    participant Backup as Backup Vendor<br/>(Assentis)
    participant Kafka as Kafka

    DocHub->>+TMS: GET /templates/vendors?templateId={id}
    TMS->>+DB: SELECT vendor mappings ORDER BY priority
    DB-->>-TMS: [SmartComm (primary), Assentis (backup)]
    TMS-->>-DocHub: Vendor list with priorities

    rect rgb(255, 200, 200)
        Note over DocHub,Primary: Primary Vendor Failure
        DocHub->>+Primary: POST /generate
        Primary-->>-DocHub: 503 Service Unavailable
    end

    DocHub->>Kafka: VendorFailureEvent (SmartComm)

    rect rgb(200, 255, 200)
        Note over DocHub,Backup: Failover to Backup Vendor
        DocHub->>+Backup: POST /generate
        Backup-->>-DocHub: 200 OK (generated document)
    end

    DocHub->>Kafka: DocumentGeneratedEvent<br/>(vendor=Assentis, failover=true)
```

---

## 7. Complete End-to-End Flow

This comprehensive diagram shows the full journey from template creation to document delivery.

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin
    participant TMS as Template Mgmt<br/>Service
    participant DocHub as Document Hub
    participant LetterAPI as Letter API
    participant PrintPartner as Print Partner
    participant Customer as Customer<br/>Portal
    participant Kafka as Kafka

    rect rgb(230, 230, 250)
        Note over Admin,TMS: 1. Template Setup (One-time)
        Admin->>TMS: Create template
        Admin->>TMS: Map to SmartComm (generation)
        Admin->>TMS: Map to LPS (print)
        Admin->>TMS: Activate template
    end

    rect rgb(255, 248, 220)
        Note over DocHub,LetterAPI: 2. Document Generation (Per Request)
        DocHub->>TMS: Get template config
        DocHub->>LetterAPI: Generate document
        LetterAPI-->>DocHub: PDF document
    end

    rect rgb(220, 255, 220)
        Note over DocHub,PrintPartner: 3. Print Delivery
        DocHub->>PrintPartner: Submit print job
        PrintPartner-->>Kafka: Status updates
    end

    rect rgb(255, 220, 220)
        Note over Customer,DocHub: 4. Digital Access
        Customer->>DocHub: View my documents
        DocHub-->>Customer: Document list + download
    end
```

---

## API Contracts Summary

### Template Management Service APIs

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/templates` | POST | Create new template |
| `/templates/{id}/versions/{v}` | GET | Get template version details |
| `/templates/{id}/versions/{v}` | PATCH | Update template version |
| `/templates/vendors` | POST | Create vendor mapping |
| `/templates/vendors` | GET | List vendor mappings |
| `/templates/vendors/{vendorId}` | PATCH | Update vendor mapping |

### Letter API Service APIs (SmartComm/Assentis)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/templates/validate` | POST | Validate template schema |
| `/generate` | POST | Generate document from template |
| `/preview` | POST | Generate preview with watermark |

### Print Partner Service APIs (LPS)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/print-jobs` | POST | Submit print job |
| `/print-jobs/{id}` | GET | Get job status |
| `/print-jobs/{id}/cancel` | POST | Cancel print job |
| `/accounts/validate` | POST | Validate print account |

---

## Event Schemas

### Kafka Topics

| Topic | Events |
|-------|--------|
| `template-events` | TemplateCreated, TemplateUpdated, TemplateActivated |
| `vendor-events` | VendorMappingCreated, VendorFailure, VendorRecovered |
| `document-events` | DocumentGenerated, DocumentDelivered |
| `print-events` | PrintJobSubmitted, PrintJobStatus, PrintJobCompleted |

---

## View These Diagrams

1. **GitHub/GitLab**: Mermaid diagrams render automatically
2. **VS Code**: Install "Markdown Preview Mermaid Support" extension
3. **Online**: Use [Mermaid Live Editor](https://mermaid.live)
