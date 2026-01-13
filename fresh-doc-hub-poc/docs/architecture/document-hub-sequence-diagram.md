# Document Hub - System Sequence Diagrams

## Overview

This document provides sequence diagrams showing how Document Hub communicates with different components including databases, external services, and Kafka for event streaming.

---

## Components

| Component | Type | Description |
|-----------|------|-------------|
| **Client** | External | Mobile App, Web App, or API Consumer |
| **Document Hub API** | Service | Main application (Spring Boot) |
| **PostgreSQL** | Database | Primary database for templates and storage index |
| **ECMS** | External Service | Enterprise Content Management System (document storage) |
| **Mock APIs** | External Service | Customer, Account, Credit APIs for data enrichment |
| **Kafka** | Message Broker | Event streaming for document events |
| **Reporting DB** | Database | `doc_reporting.document_events_ledger` table |
| **Event Consumer** | Service | Consumes Kafka events and updates reporting |

---

## 1. Document Upload Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant API as Document Hub API
    participant DB as PostgreSQL
    participant ECMS as ECMS Storage
    participant Kafka as Kafka
    participant Consumer as Event Consumer
    participant ReportDB as Reporting DB

    Client->>+API: POST /documents (multipart)

    Note over API: Validate Request

    API->>+DB: Get Template by Type
    DB-->>-API: Template Config

    Note over API: Validate Required Fields<br/>Check Access Control

    alt Single Document Flag = true
        API->>DB: Close existing docs for account
    end

    API->>+ECMS: Upload Document (binary)
    ECMS-->>-API: Document ID + Storage Path

    API->>+DB: INSERT storage_index
    DB-->>-API: Storage Index Created

    rect rgb(255, 240, 200)
        Note over API,Kafka: Publish Event to Kafka
        API->>Kafka: DocumentUploadedEvent
        Note right of Kafka: Topic: document-events
    end

    API-->>-Client: 200 OK (Document ID)

    rect rgb(200, 255, 200)
        Note over Kafka,ReportDB: Async Event Processing
        Kafka->>+Consumer: DocumentUploadedEvent
        Consumer->>+ReportDB: INSERT document_events_ledger
        ReportDB-->>-Consumer: Success
        Consumer-->>-Kafka: ACK
    end
```

---

## 2. Document Enquiry Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant API as Document Hub API
    participant DB as PostgreSQL
    participant MockAPI as External APIs
    participant Kafka as Kafka
    participant Consumer as Event Consumer
    participant ReportDB as Reporting DB

    Client->>+API: POST /documents-enquiry

    Note over API: Parse Request<br/>(customerId, accountId, filters)

    API->>+DB: Get Templates (by LOB, category)
    DB-->>-API: List of Templates

    loop For each template with eligibility_criteria
        API->>+MockAPI: GET /api/customers/{id}
        MockAPI-->>-API: Customer Data
        Note over API: Evaluate Eligibility Rules
    end

    API->>+DB: Query storage_index<br/>(with filters, pagination)
    DB-->>-API: Documents List

    Note over API: Apply Access Control<br/>Filter by requestor type

    rect rgb(255, 240, 200)
        Note over API,Kafka: Publish Event to Kafka
        API->>Kafka: DocumentEnquiryEvent
        Note right of Kafka: Topic: document-events
    end

    API-->>-Client: 200 OK (Documents + Pagination)

    rect rgb(200, 255, 200)
        Note over Kafka,ReportDB: Async Event Processing
        Kafka->>+Consumer: DocumentEnquiryEvent
        Consumer->>+ReportDB: INSERT document_events_ledger
        ReportDB-->>-Consumer: Success
        Consumer-->>-Kafka: ACK
    end
```

---

## 3. Document Download Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant API as Document Hub API
    participant DB as PostgreSQL
    participant ECMS as ECMS Storage
    participant Kafka as Kafka
    participant Consumer as Event Consumer
    participant ReportDB as Reporting DB

    Client->>+API: GET /documents/{id}/content

    API->>+DB: Get storage_index by ID
    DB-->>-API: Storage Index Record

    API->>+DB: Get Template Config
    DB-->>-API: Template (access_control)

    Note over API: Check Download Permission<br/>by requestor type

    alt Permission Denied
        API-->>Client: 403 Forbidden
    end

    API->>+ECMS: GET Document Content
    ECMS-->>-API: Binary Content

    rect rgb(255, 240, 200)
        Note over API,Kafka: Publish Event to Kafka
        API->>Kafka: DocumentDownloadedEvent
        Note right of Kafka: Topic: document-events
    end

    API-->>-Client: 200 OK (Binary Stream)

    rect rgb(200, 255, 200)
        Note over Kafka,ReportDB: Async Event Processing
        Kafka->>+Consumer: DocumentDownloadedEvent
        Consumer->>+ReportDB: INSERT document_events_ledger
        ReportDB-->>-Consumer: Success
        Consumer-->>-Kafka: ACK
    end
```

---

## 4. Template Management Flow

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin Client
    participant API as Document Hub API
    participant DB as PostgreSQL
    participant Kafka as Kafka
    participant Consumer as Event Consumer
    participant ReportDB as Reporting DB

    Admin->>+API: POST /templates

    Note over API: Validate Template Request

    API->>+DB: INSERT master_template_definition
    DB-->>-API: Template Created

    rect rgb(255, 240, 200)
        API->>Kafka: TemplateCreatedEvent
        Note right of Kafka: Topic: template-events
    end

    API-->>-Admin: 200 OK (Template ID)

    rect rgb(200, 255, 200)
        Kafka->>+Consumer: TemplateCreatedEvent
        Consumer->>+ReportDB: INSERT document_events_ledger
        ReportDB-->>-Consumer: Success
        Consumer-->>-Kafka: ACK
    end
```

---

## 5. Data Extraction Flow (Eligibility Check)

```mermaid
sequenceDiagram
    autonumber
    participant API as Document Hub API
    participant DB as PostgreSQL
    participant CustomerAPI as Customer API
    participant AccountAPI as Account API
    participant CreditAPI as Credit API

    Note over API: Processing Document Enquiry

    API->>+DB: Get Template Config
    DB-->>-API: data_extraction_config

    Note over API: Build Extraction Plan<br/>from field sources

    par Parallel API Calls (if configured)
        API->>+CustomerAPI: GET /customers/{id}/profile
        CustomerAPI-->>-API: Customer Data
    and
        API->>+AccountAPI: GET /accounts/{id}/arrangements
        AccountAPI-->>-API: Account Data
    and
        API->>+CreditAPI: GET /credit/{id}
        CreditAPI-->>-API: Credit Score
    end

    Note over API: Extract Fields using JSONPath<br/>Evaluate Eligibility Criteria

    alt Eligible
        Note over API: Include template in results
    else Not Eligible
        Note over API: Exclude template from results
    end
```

---

## 6. Kafka Event Schema

### Topic: `document-events`

```json
{
  "eventId": "uuid",
  "eventType": "DOCUMENT_UPLOADED | DOCUMENT_DOWNLOADED | DOCUMENT_VIEWED | DOCUMENT_DELETED",
  "timestamp": 1704067200000,
  "correlationId": "uuid",
  "payload": {
    "documentId": "uuid",
    "templateType": "Statement",
    "templateVersion": 1,
    "customerId": "uuid",
    "accountId": "uuid",
    "requestorId": "uuid",
    "requestorType": "CUSTOMER | AGENT | SYSTEM",
    "action": "UPLOAD | DOWNLOAD | VIEW | DELETE",
    "metadata": {
      "fileName": "statement_jan_2024.pdf",
      "fileSize": 102400,
      "mimeType": "application/pdf"
    }
  }
}
```

### Topic: `template-events`

```json
{
  "eventId": "uuid",
  "eventType": "TEMPLATE_CREATED | TEMPLATE_UPDATED | TEMPLATE_ACTIVATED | TEMPLATE_DEACTIVATED",
  "timestamp": 1704067200000,
  "correlationId": "uuid",
  "payload": {
    "masterTemplateId": "uuid",
    "templateVersion": 1,
    "templateType": "Statement",
    "lineOfBusiness": "CREDIT_CARD",
    "action": "CREATE | UPDATE | ACTIVATE | DEACTIVATE",
    "changedBy": "admin-user-id",
    "changes": {}
  }
}
```

---

## 7. Document Events Ledger Schema

```sql
CREATE TABLE doc_reporting.document_events_ledger (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    correlation_id UUID,

    -- Document Info
    document_id UUID,
    template_type VARCHAR(100),
    template_version INTEGER,

    -- Actor Info
    customer_id UUID,
    account_id UUID,
    requestor_id UUID,
    requestor_type VARCHAR(20),

    -- Event Details
    action VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,

    -- Metadata
    metadata JSONB,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Indexes
    INDEX idx_event_timestamp (event_timestamp),
    INDEX idx_document_id (document_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_action (action)
);
```

---

## 8. Simplified Component Diagram

```mermaid
flowchart TB
    subgraph Clients
        Mobile[Mobile App]
        Web[Web App]
        System[System/Batch]
    end

    subgraph DocumentHub[Document Hub]
        API[REST API<br/>Spring Boot]
        Processor[Processors]
        Services[Services]
        EventPub[Event Publisher]
    end

    subgraph Databases
        PG[(PostgreSQL<br/>document_hub)]
        ReportDB[(PostgreSQL<br/>doc_reporting)]
    end

    subgraph ExternalServices
        ECMS[ECMS<br/>Document Storage]
        CustomerAPI[Customer API]
        AccountAPI[Account API]
        CreditAPI[Credit API]
    end

    subgraph Messaging
        Kafka[Apache Kafka]
        Consumer[Event Consumer]
    end

    Mobile --> API
    Web --> API
    System --> API

    API --> Processor
    Processor --> Services
    Services --> PG
    Services --> ECMS
    Services --> CustomerAPI
    Services --> AccountAPI
    Services --> CreditAPI

    Processor --> EventPub
    EventPub --> Kafka

    Kafka --> Consumer
    Consumer --> ReportDB

    style API fill:#4CAF50,color:#fff
    style Kafka fill:#FF9800,color:#fff
    style PG fill:#2196F3,color:#fff
    style ReportDB fill:#2196F3,color:#fff
    style ECMS fill:#9C27B0,color:#fff
```

---

## 9. Event Flow Summary

| Event | Trigger | Kafka Topic | Ledger Action |
|-------|---------|-------------|---------------|
| `DOCUMENT_UPLOADED` | POST /documents | document-events | INSERT |
| `DOCUMENT_DOWNLOADED` | GET /documents/{id}/content | document-events | INSERT |
| `DOCUMENT_VIEWED` | POST /documents-enquiry | document-events | INSERT |
| `DOCUMENT_DELETED` | DELETE /documents/{id} | document-events | INSERT |
| `TEMPLATE_CREATED` | POST /templates | template-events | INSERT |
| `TEMPLATE_UPDATED` | PATCH /templates/{id} | template-events | INSERT |
| `TEMPLATE_ACTIVATED` | POST /templates/{id}/activate | template-events | INSERT |
| `TEMPLATE_DEACTIVATED` | POST /templates/{id}/deactivate | template-events | INSERT |

---

## 10. Configuration

### Kafka Producer Config (application.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5

document-hub:
  kafka:
    topics:
      document-events: document-events
      template-events: template-events
    enabled: true
```

### Event Consumer Config

```yaml
spring:
  kafka:
    consumer:
      group-id: document-events-consumer
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.documenthub.event
```

---

## View These Diagrams

1. **GitHub/GitLab**: Mermaid diagrams render automatically
2. **VS Code**: Install "Markdown Preview Mermaid Support" extension
3. **Online**: Use [Mermaid Live Editor](https://mermaid.live)
