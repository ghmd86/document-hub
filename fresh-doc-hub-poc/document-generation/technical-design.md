# Document Generation Service - Technical Design Document

## 1. Executive Summary

This document outlines the technical design for the Document Generation Service, which integrates with SmartComm to generate PDF documents from structured XML data. The service supports three document generation workflows:

1. **Draft Flow** - Interactive draft creation with preview and editing before finalization (REST API)
2. **Direct Generation Flow** - Immediate document generation without draft, triggered via Kafka topic (e.g., from upstream systems requesting document creation)
3. **Bulk Flow** - Batch document generation for high-volume processing (REST API + Kafka)

**Note:** A separate **Print Service** handles physical printing. Once documents are generated and stored in Document Hub, the Print Service retrieves them via API and sends to print vendors.

---

## 2. Context & Background

### 2.1 Current State
- Document Hub Service POC is complete with template management capabilities
- Database schema exists for template definitions, storage indexing, and vendor mappings
- SmartComm is the selected document generation vendor

### 2.2 Business Requirements
- Generate PDF documents from XML data using SmartComm API
- Support interactive draft creation with editor preview
- Support direct document generation requests via Kafka topic (no draft/preview)
- Support bulk document generation for batch processing
- Track document generation status and store generated documents
- Integrate with existing Document Hub infrastructure
- Publish generation completion events for downstream consumers

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
                                                    ┌──────────────────┐
                                                    │   Kafka Cluster  │
                                                    │                  │
┌─────────────────────────────────────┐             │ ┌──────────────┐ │
│          API Gateway                │             │ │ doc-request  │ │
└─────────────────────────────────────┘             │ │    topic     │ │
                    │                               │ └──────┬───────┘ │
                    │                               │        │         │
                    ▼                               │ ┌──────▼───────┐ │
┌───────────────────────────────────────────────────┼─┤doc-generated │ │
│                Document Generation Service        │ │    topic     │ │
│                                                   │ └──────────────┘ │
│  ┌──────────────┐      ┌──────────────────┐      └────────┼─────────┘
│  │   REST API   │      │  Kafka Consumers │               │
│  │  Controller  │      │  ┌────────────┐  │◀──────────────┘
│  └──────┬───────┘      │  │DocRequest  │  │
│         │              │  │ Consumer   │  │
│         │              │  └────────────┘  │
│         │              │  ┌────────────┐  │
│         │              │  │  BulkJob   │  │
│         │              │  │ Consumer   │  │
│         │              │  └────────────┘  │
│         │              └────────┬─────────┘
│         │                       │
│         ▼                       ▼
│  ┌─────────────────────────────────────────────────────────────────┐
│  │                      Service Layer                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  │DraftService │  │DirectGen    │  │BulkGeneration│             │
│  │  │             │  │Service      │  │Service       │             │
│  │  └─────────────┘  └─────────────┘  └──────────────┘             │
│  │         │               │                 │                      │
│  │         └───────────────┼─────────────────┘                      │
│  │                         ▼                                        │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  │ XmlBuilder   │  │  SmartComm   │  │   Storage    │           │
│  │  │   Service    │  │    Client    │  │   Service    │           │
│  │  └──────────────┘  └──────────────┘  └──────────────┘           │
│  └─────────────────────────────────────────────────────────────────┘
└───────────────────────────────────────────────────────────────────────
          │                                   │                  │
          ▼                                   ▼                  ▼
┌──────────────────┐              ┌──────────────────┐  ┌──────────────────┐
│    PostgreSQL    │              │    SmartComm     │  │   Object Store   │
│    Database      │              │    Cloud API     │  │   (S3/Azure)     │
└──────────────────┘              └──────────────────┘  └──────────────────┘
                                                                │
                                                                │ (Documents stored)
                                                                ▼
                                                        ┌──────────────────┐
                                                        │  Document Hub    │
                                                        │    Service       │
                                                        └────────┬─────────┘
                                                                 │
                                            ┌────────────────────┼────────────────────┐
                                            ▼                    ▼                    ▼
                                    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                                    │ Print Service│    │   Email      │    │  Other       │
                                    │ (Separate)   │    │   Service    │    │  Consumers   │
                                    └──────────────┘    └──────────────┘    └──────────────┘
```

### 3.2 Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Document Generation Service                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  Presentation Layer                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  DraftController  │  BulkGenerationController  │  StatusController  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│  Event Consumer Layer                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  DocumentRequestConsumer  │  BulkJobItemConsumer                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│  Service Layer                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  DraftService  │  DirectGenerationService  │  BulkGenerationService │    │
│  │  XmlBuilderService  │  ValidationService  │  StorageService         │    │
│  │  NotificationService  │  EventPublisherService                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│  Integration Layer                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  SmartCommClient  │  ObjectStorageClient  │  KafkaProducer/Consumer │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│  Data Access Layer                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  TemplateRepository  │  StorageIndexRepository  │  JobRepository    │    │
│  │  DocumentRequestRepository                                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Detailed Component Design

### 4.1 REST API Controllers

#### 4.1.1 DraftController
Handles interactive document draft operations.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/drafts` | POST | Create a new draft |
| `/api/v1/drafts/{draftId}` | GET | Get draft details with preview |
| `/api/v1/drafts/{draftId}` | PUT | Update draft data |
| `/api/v1/drafts/{draftId}/finalize` | POST | Finalize and generate PDF |
| `/api/v1/drafts/{draftId}` | DELETE | Discard draft |

#### 4.1.2 BulkGenerationController
Handles batch document generation.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/bulk-jobs` | POST | Submit bulk generation job |
| `/api/v1/bulk-jobs/{jobId}` | GET | Get job status |
| `/api/v1/bulk-jobs/{jobId}/cancel` | POST | Cancel pending job |
| `/api/v1/bulk-jobs/{jobId}/documents` | GET | List generated documents |

#### 4.1.3 DocumentController
Handles document retrieval and management.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/documents/{documentId}` | GET | Get document metadata |
| `/api/v1/documents/{documentId}/download` | GET | Download PDF |
| `/api/v1/documents/{documentId}/resend` | POST | Resend to notification channel |

### 4.2 Service Components

#### 4.2.1 DraftService
```java
public interface DraftService {
    DraftResponse createDraft(DraftRequest request);
    DraftResponse getDraft(UUID draftId);
    DraftResponse updateDraft(UUID draftId, DraftUpdateRequest request);
    DocumentResponse finalizeDraft(UUID draftId);
    void discardDraft(UUID draftId);
}
```

**Responsibilities:**
- Validate template and input data
- Create XML payload from request data
- Call SmartComm generateDraft API
- Store draft state for editing
- Handle draft finalization

#### 4.2.2 DirectGenerationService
```java
public interface DirectGenerationService {
    GenerationResult processDocumentRequest(DocumentRequestEvent event);
    DocumentRequestStatus getStatus(UUID requestId);
}
```

**Responsibilities:**
- Process incoming document generation requests from Kafka topic
- Validate template and input data
- Generate document directly via SmartComm finalizeDraft API (no draft step)
- Store generated PDF to object storage (Document Hub)
- Update storage_index with document metadata
- Publish completion event to `doc-generated` topic
- Handle failures with retry and dead-letter queue

#### 4.2.3 BulkGenerationService
```java
public interface BulkGenerationService {
    BulkJobResponse submitJob(BulkJobRequest request);
    BulkJobStatusResponse getJobStatus(UUID jobId);
    void cancelJob(UUID jobId);
    Page<DocumentSummary> getJobDocuments(UUID jobId, Pageable pageable);
}
```

**Responsibilities:**
- Accept bulk generation requests
- Validate all items before processing
- Publish items to processing queue
- Track job progress and status
- Handle job cancellation

#### 4.2.4 XmlBuilderService
```java
public interface XmlBuilderService {
    String buildXml(String templateId, DocumentDataRequest data);
    void validateXml(String xml, String schemaPath);
    String encodeToBase64(String xml);
}
```

**Responsibilities:**
- Build XML from structured request data
- Map request fields to schema elements
- Validate XML against XSD schema
- Handle dynamic/flexible fields
- Base64 encode for SmartComm API

#### 4.2.5 SmartCommClient
```java
public interface SmartCommClient {
    GenerateDraftResponse generateDraft(GenerateDraftRequest request);
    FinalizeDraftResponse finalizeDraft(FinalizeDraftRequest request);
    JobStatusResponse getJobStatus(String jobId);
}
```

**Responsibilities:**
- Handle OAuth1 authentication (HMAC-SHA256)
- Build and sign API requests
- Execute HTTP calls with retry logic
- Parse responses and handle errors
- Manage connection pooling

#### 4.2.6 StorageService
```java
public interface StorageService {
    StorageResult storeDocument(byte[] pdfContent, DocumentMetadata metadata);
    byte[] retrieveDocument(UUID storageId);
    void deleteDocument(UUID storageId);
    String generatePresignedUrl(UUID storageId, Duration expiry);
}
```

**Responsibilities:**
- Store PDF documents to object storage
- Update storage_index table
- Generate pre-signed download URLs
- Handle document lifecycle

### 4.3 Kafka Consumers

#### 4.3.1 DocumentRequestConsumer
```java
@Component
public class DocumentRequestConsumer {

    private final DirectGenerationService directGenerationService;
    private final EventPublisherService eventPublisher;

    @KafkaListener(topics = "${kafka.topics.doc-request}",
                   groupId = "${kafka.consumer.group-id}")
    public void consume(DocumentRequestEvent event) {
        try {
            GenerationResult result = directGenerationService.processDocumentRequest(event);
            eventPublisher.publishDocumentGenerated(result);
        } catch (Exception e) {
            // Handle with retry or send to DLQ
            handleFailure(event, e);
        }
    }
}
```

**Topic Configuration:**
| Topic | Purpose | Partitions | Retention |
|-------|---------|------------|-----------|
| `doc-request` | Incoming document generation requests | 10 | 7 days |
| `doc-generated` | Completion events (consumed by Print Service, Email Service, etc.) | 10 | 7 days |
| `doc-request-dlq` | Failed requests | 5 | 30 days |

#### 4.3.2 DocumentRequestEvent Schema
```json
{
  "requestId": "uuid",
  "correlationId": "string",
  "templateId": "string",
  "templateVersion": 1,
  "priority": "NORMAL|HIGH|LOW",
  "source": "string (e.g., 'LOAN_SYSTEM', 'CRM')",
  "recipient": { ... },
  "entity": { ... },
  "fields": { ... },
  "metadata": {
    "accountId": "string",
    "customerId": "string",
    "channel": "PRINT|EMAIL"
  },
  "timestamp": "2024-12-17T10:00:00Z"
}
```

#### 4.3.3 DocumentGeneratedEvent Schema
```json
{
  "requestId": "uuid",
  "correlationId": "string",
  "status": "SUCCESS|FAILED",
  "documentId": "uuid",
  "storageIndexId": "uuid",
  "templateId": "string",
  "letterReferenceNumber": "string",
  "metadata": {
    "accountId": "string",
    "customerId": "string",
    "fileSize": 12345,
    "pageCount": 2
  },
  "error": {
    "code": "string",
    "message": "string"
  },
  "generatedAt": "2024-12-17T10:01:00Z"
}
```

---

## 5. Data Flow Diagrams

### 5.1 Draft Generation Flow

```
┌──────┐     ┌─────────────┐     ┌─────────────┐     ┌──────────────┐     ┌───────────┐
│Client│     │DraftController│   │DraftService │     │SmartCommClient│    │SmartComm  │
└──┬───┘     └──────┬──────┘     └──────┬──────┘     └──────┬───────┘     └─────┬─────┘
   │                │                   │                   │                   │
   │ POST /drafts   │                   │                   │                   │
   │───────────────▶│                   │                   │                   │
   │                │ createDraft()     │                   │                   │
   │                │──────────────────▶│                   │                   │
   │                │                   │ buildXml()        │                   │
   │                │                   │──────────┐        │                   │
   │                │                   │          │        │                   │
   │                │                   │◀─────────┘        │                   │
   │                │                   │ validateXml()     │                   │
   │                │                   │──────────┐        │                   │
   │                │                   │          │        │                   │
   │                │                   │◀─────────┘        │                   │
   │                │                   │                   │                   │
   │                │                   │ generateDraft()   │                   │
   │                │                   │──────────────────▶│                   │
   │                │                   │                   │ POST /generateDraft
   │                │                   │                   │──────────────────▶│
   │                │                   │                   │                   │
   │                │                   │                   │   Draft Response  │
   │                │                   │                   │◀──────────────────│
   │                │                   │   DraftResult     │                   │
   │                │                   │◀──────────────────│                   │
   │                │                   │                   │                   │
   │                │                   │ saveDraftState()  │                   │
   │                │                   │──────────┐        │                   │
   │                │                   │          │        │                   │
   │                │                   │◀─────────┘        │                   │
   │                │   DraftResponse   │                   │                   │
   │                │◀──────────────────│                   │                   │
   │ DraftResponse  │                   │                   │                   │
   │◀───────────────│                   │                   │                   │
   │                │                   │                   │                   │
```

### 5.2 Draft Finalization Flow

```
┌──────┐     ┌─────────────┐     ┌─────────────┐     ┌──────────────┐     ┌───────────┐
│Client│     │DraftController│   │DraftService │     │SmartCommClient│    │SmartComm  │
└──┬───┘     └──────┬──────┘     └──────┬──────┘     └──────┬───────┘     └─────┬─────┘
   │                │                   │                   │                   │
   │POST /drafts/{id}/finalize          │                   │                   │
   │───────────────▶│                   │                   │                   │
   │                │ finalizeDraft()   │                   │                   │
   │                │──────────────────▶│                   │                   │
   │                │                   │ getDraftState()   │                   │
   │                │                   │──────────┐        │                   │
   │                │                   │          │        │                   │
   │                │                   │◀─────────┘        │                   │
   │                │                   │                   │                   │
   │                │                   │ finalizeDraft()   │                   │
   │                │                   │──────────────────▶│                   │
   │                │                   │                   │POST /finalizeDraft│
   │                │                   │                   │──────────────────▶│
   │                │                   │                   │                   │
   │                │                   │                   │   PDF Response    │
   │                │                   │                   │◀──────────────────│
   │                │                   │    PDF bytes      │                   │
   │                │                   │◀──────────────────│                   │
   │                │                   │                   │                   │
   │                │                   │ storeDocument()   │                   │
   │                │                   │──────────┐        │                   │
   │                │                   │          │ (S3/Azure)                 │
   │                │                   │◀─────────┘        │                   │
   │                │                   │                   │                   │
   │                │                   │ updateStorageIndex()                  │
   │                │                   │──────────┐        │                   │
   │                │                   │          │ (PostgreSQL)               │
   │                │                   │◀─────────┘        │                   │
   │                │ DocumentResponse  │                   │                   │
   │                │◀──────────────────│                   │                   │
   │DocumentResponse│                   │                   │                   │
   │◀───────────────│                   │                   │                   │
```

### 5.3 Direct Generation Flow (Kafka-Triggered)

```
┌───────────┐   ┌───────────┐   ┌─────────────────┐   ┌─────────────┐   ┌───────────┐
│  Source   │   │   Kafka   │   │DocumentRequest  │   │DirectGen    │   │SmartComm  │
│  System   │   │  Cluster  │   │Consumer         │   │Service      │   │           │
└─────┬─────┘   └─────┬─────┘   └───────┬─────────┘   └──────┬──────┘   └─────┬─────┘
      │               │                 │                    │               │
      │ Publish       │                 │                    │               │
      │ DocRequest    │                 │                    │               │
      │──────────────▶│                 │                    │               │
      │               │                 │                    │               │
      │               │ consume()       │                    │               │
      │               │────────────────▶│                    │               │
      │               │                 │                    │               │
      │               │                 │ processDocRequest()│               │
      │               │                 │───────────────────▶│               │
      │               │                 │                    │               │
      │               │                 │                    │ validateTemplate()
      │               │                 │                    │───────┐       │
      │               │                 │                    │       │(DB)   │
      │               │                 │                    │◀──────┘       │
      │               │                 │                    │               │
      │               │                 │                    │ buildXml()    │
      │               │                 │                    │───────┐       │
      │               │                 │                    │       │       │
      │               │                 │                    │◀──────┘       │
      │               │                 │                    │               │
      │               │                 │                    │ finalizeDraft()
      │               │                 │                    │──────────────▶│
      │               │                 │                    │               │
      │               │                 │                    │   PDF bytes   │
      │               │                 │                    │◀──────────────│
      │               │                 │                    │               │
      │               │                 │                    │ storeDocument()
      │               │                 │                    │───────┐       │
      │               │                 │                    │       │(S3)   │
      │               │                 │                    │◀──────┘       │
      │               │                 │                    │               │
      │               │                 │                    │ updateStorageIndex()
      │               │                 │                    │───────┐       │
      │               │                 │                    │       │(DB)   │
      │               │                 │                    │◀──────┘       │
      │               │                 │  GenerationResult  │               │
      │               │                 │◀───────────────────│               │
      │               │                 │                    │               │
      │               │ publish         │                    │               │
      │               │ DocGenerated    │                    │               │
      │               │◀────────────────│                    │               │
      │               │                 │                    │               │
      ▼               ▼                 ▼                    ▼               ▼
┌───────────┐   ┌───────────┐
│Downstream │◀──│doc-generated│
│ Systems   │   │   topic     │
└───────────┘   └─────────────┘
```

**Key Characteristics:**
- No draft/preview step - direct generation
- Async processing via Kafka consumer
- Completion event published for downstream systems
- Supports retry with exponential backoff
- Failed messages sent to Dead Letter Queue (DLQ)

### 5.4 Bulk Generation Flow

```
┌──────┐   ┌────────────────┐   ┌──────────────────┐   ┌───────┐   ┌──────────────┐
│Client│   │BulkController  │   │BulkGenService    │   │ Kafka │   │BulkProcessor │
└──┬───┘   └───────┬────────┘   └────────┬─────────┘   └───┬───┘   └──────┬───────┘
   │               │                     │                 │              │
   │POST /bulk-jobs│                     │                 │              │
   │──────────────▶│                     │                 │              │
   │               │ submitJob()         │                 │              │
   │               │────────────────────▶│                 │              │
   │               │                     │                 │              │
   │               │                     │ validateAll()   │              │
   │               │                     │────────┐        │              │
   │               │                     │        │        │              │
   │               │                     │◀───────┘        │              │
   │               │                     │                 │              │
   │               │                     │ createJob()     │              │
   │               │                     │────────┐        │              │
   │               │                     │        │(DB)    │              │
   │               │                     │◀───────┘        │              │
   │               │                     │                 │              │
   │               │                     │ publishItems()  │              │
   │               │                     │────────────────▶│              │
   │               │                     │                 │              │
   │               │   JobResponse       │                 │              │
   │               │◀────────────────────│                 │              │
   │ JobResponse   │                     │                 │              │
   │◀──────────────│                     │                 │              │
   │               │                     │                 │              │
   │               │                     │                 │ consume()    │
   │               │                     │                 │─────────────▶│
   │               │                     │                 │              │
   │               │                     │                 │              │──┐
   │               │                     │                 │              │  │ processItem()
   │               │                     │                 │              │  │ (SmartComm)
   │               │                     │                 │              │◀─┘
   │               │                     │                 │              │
   │               │                     │                 │              │──┐
   │               │                     │                 │              │  │ storeDocument()
   │               │                     │                 │              │◀─┘
   │               │                     │                 │              │
   │               │                     │                 │              │──┐
   │               │                     │                 │              │  │ updateJobProgress()
   │               │                     │                 │              │◀─┘
```

---

## 6. Data Models

### 6.1 API Request/Response Models

#### Draft Request
```json
{
  "templateId": "CI013",
  "templateVersion": 1,
  "recipient": {
    "name": "Jane Smith",
    "address": {
      "addressLine1": "456 Oak Avenue",
      "addressLine2": "APT 333",
      "city": "Somewhere",
      "state": "CA",
      "zipcode": "90210"
    }
  },
  "entity": {
    "last4digit": 2344,
    "caseNumber": "123812",
    "creditScore": 777,
    "scoreDate": "2024-10-12"
  },
  "fields": {
    "advrsActReasonList": {
      "advrsActReason1": "Reason A for Adverse Action",
      "advrsActReason2": "Reason B for Adverse Action"
    }
  },
  "options": {
    "language": "EN",
    "channel": "PRINT"
  }
}
```

#### Draft Response
```json
{
  "draftId": "550e8400-e29b-41d4-a716-446655440000",
  "templateId": "CI013",
  "status": "DRAFT",
  "previewUrl": "https://storage.example.com/previews/550e8400...?token=xyz",
  "createdAt": "2024-12-17T10:30:00Z",
  "expiresAt": "2024-12-17T11:30:00Z",
  "letterReferenceNumber": "LTR-2024-001234"
}
```

#### Bulk Job Request
```json
{
  "templateId": "CI013",
  "templateVersion": 1,
  "items": [
    {
      "referenceId": "CUST-001",
      "recipient": { ... },
      "entity": { ... },
      "fields": { ... }
    },
    {
      "referenceId": "CUST-002",
      "recipient": { ... },
      "entity": { ... },
      "fields": { ... }
    }
  ],
  "options": {
    "priority": "NORMAL",
    "notifyOnComplete": true,
    "callbackUrl": "https://api.example.com/webhooks/doc-gen"
  }
}
```

#### Bulk Job Status Response
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "PROCESSING",
  "templateId": "CI013",
  "totalItems": 100,
  "completedItems": 45,
  "failedItems": 2,
  "progress": 47,
  "submittedAt": "2024-12-17T10:00:00Z",
  "estimatedCompletionAt": "2024-12-17T10:15:00Z"
}
```

### 6.2 Domain Entities

#### GenerationJob Entity
```java
@Entity
@Table(name = "generation_job", schema = "document_hub")
public class GenerationJob {
    @Id
    private UUID jobId;

    private UUID masterTemplateId;
    private Integer templateVersion;

    @Enumerated(EnumType.STRING)
    private JobStatus status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED

    private Integer totalItems;
    private Integer completedItems;
    private Integer failedItems;

    private String callbackUrl;
    private String submittedBy;

    private Instant submittedAt;
    private Instant startedAt;
    private Instant completedAt;

    @Version
    private Long versionNumber;
}
```

#### GenerationJobItem Entity
```java
@Entity
@Table(name = "generation_job_item", schema = "document_hub")
public class GenerationJobItem {
    @Id
    private UUID itemId;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private GenerationJob job;

    private String referenceId; // Customer reference

    @Enumerated(EnumType.STRING)
    private ItemStatus status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(columnDefinition = "jsonb")
    private String requestData; // JSON of the item request

    private UUID storageIndexId; // Link to generated document

    private String errorCode;
    private String errorMessage;
    private Integer retryCount;

    private Instant processedAt;
}
```

---

## 7. Database Schema Updates

### 7.1 New Tables

```sql
-- Generation Job tracking
CREATE TABLE IF NOT EXISTS document_hub.generation_job
(
    job_id UUID NOT NULL,
    master_template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_items INTEGER NOT NULL DEFAULT 0,
    completed_items INTEGER NOT NULL DEFAULT 0,
    failed_items INTEGER NOT NULL DEFAULT 0,
    priority VARCHAR(10) DEFAULT 'NORMAL',
    callback_url VARCHAR(500),
    submitted_by VARCHAR(100) NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_timestamp TIMESTAMP,
    version_number BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT generation_job_pkey PRIMARY KEY (job_id),
    CONSTRAINT generation_job_template_fkey FOREIGN KEY (master_template_id, template_version)
        REFERENCES document_hub.master_template_definition (master_template_id, template_version)
);

CREATE INDEX idx_generation_job_status ON document_hub.generation_job(status);
CREATE INDEX idx_generation_job_submitted_at ON document_hub.generation_job(submitted_at);

-- Generation Job Items
CREATE TABLE IF NOT EXISTS document_hub.generation_job_item
(
    item_id UUID NOT NULL,
    job_id UUID NOT NULL,
    reference_id VARCHAR(100),
    sequence_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_data JSONB NOT NULL,
    storage_index_id UUID,
    error_code VARCHAR(50),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    processed_at TIMESTAMP,
    created_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_timestamp TIMESTAMP,
    CONSTRAINT generation_job_item_pkey PRIMARY KEY (item_id),
    CONSTRAINT generation_job_item_job_fkey FOREIGN KEY (job_id)
        REFERENCES document_hub.generation_job (job_id),
    CONSTRAINT generation_job_item_storage_fkey FOREIGN KEY (storage_index_id)
        REFERENCES document_hub.storage_index (storage_index_id)
);

CREATE INDEX idx_generation_job_item_job_id ON document_hub.generation_job_item(job_id);
CREATE INDEX idx_generation_job_item_status ON document_hub.generation_job_item(status);
CREATE INDEX idx_generation_job_item_reference ON document_hub.generation_job_item(reference_id);

-- Draft State (for interactive editing)
CREATE TABLE IF NOT EXISTS document_hub.draft_state
(
    draft_id UUID NOT NULL,
    master_template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    request_data JSONB NOT NULL,
    smartcomm_draft_id VARCHAR(100), -- SmartComm's draft reference
    preview_data BYTEA, -- Cached preview PDF
    letter_reference_number VARCHAR(50) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT draft_state_pkey PRIMARY KEY (draft_id),
    CONSTRAINT draft_state_template_fkey FOREIGN KEY (master_template_id, template_version)
        REFERENCES document_hub.master_template_definition (master_template_id, template_version)
);

CREATE INDEX idx_draft_state_status ON document_hub.draft_state(status);
CREATE INDEX idx_draft_state_expires ON document_hub.draft_state(expires_at);

-- Document Request tracking (for Kafka-triggered direct generation)
CREATE TABLE IF NOT EXISTS document_hub.document_request
(
    request_id UUID NOT NULL,
    correlation_id VARCHAR(100),
    master_template_id UUID NOT NULL,
    template_version INTEGER NOT NULL,
    source_system VARCHAR(50) NOT NULL,  -- e.g., 'LOAN_SYSTEM', 'CRM', 'CARD_SERVICES'
    priority VARCHAR(10) DEFAULT 'NORMAL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PROCESSING, COMPLETED, FAILED
    request_data JSONB NOT NULL,
    storage_index_id UUID,
    letter_reference_number VARCHAR(50),
    account_id VARCHAR(50),
    customer_id VARCHAR(50),
    channel VARCHAR(20),  -- PRINT, EMAIL, DIGITAL
    error_code VARCHAR(50),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    created_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_timestamp TIMESTAMP,
    version_number BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT document_request_pkey PRIMARY KEY (request_id),
    CONSTRAINT document_request_template_fkey FOREIGN KEY (master_template_id, template_version)
        REFERENCES document_hub.master_template_definition (master_template_id, template_version),
    CONSTRAINT document_request_storage_fkey FOREIGN KEY (storage_index_id)
        REFERENCES document_hub.storage_index (storage_index_id)
);

CREATE INDEX idx_document_request_status ON document_hub.document_request(status);
CREATE INDEX idx_document_request_correlation ON document_hub.document_request(correlation_id);
CREATE INDEX idx_document_request_source ON document_hub.document_request(source_system);
CREATE INDEX idx_document_request_account ON document_hub.document_request(account_id);
CREATE INDEX idx_document_request_customer ON document_hub.document_request(customer_id);
CREATE INDEX idx_document_request_received ON document_hub.document_request(received_at);
```

### 7.2 Updates to Existing Tables

```sql
-- Add generation tracking columns to storage_index
ALTER TABLE document_hub.storage_index
    ADD COLUMN IF NOT EXISTS generation_status VARCHAR(20) DEFAULT 'COMPLETED',
    ADD COLUMN IF NOT EXISTS smartcomm_job_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS letter_reference_number VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_storage_index_letter_ref
    ON document_hub.storage_index(letter_reference_number);
```

---

## 8. SmartComm Integration

### 8.1 Authentication

SmartComm uses **OAuth 1.0a** with **HMAC-SHA256** signature method.

```java
@Component
public class SmartCommOAuth1Signer {

    private final SmartCommConfig config;

    public Map<String, String> signRequest(String method, String url,
                                            Map<String, String> params) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonce();

        Map<String, String> oauthParams = new LinkedHashMap<>();
        oauthParams.put("oauth_consumer_key", config.getConsumerKey());
        oauthParams.put("oauth_signature_method", "HMAC-SHA256");
        oauthParams.put("oauth_timestamp", timestamp);
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_version", "1.0");

        String baseString = buildBaseString(method, url, oauthParams, params);
        String signature = calculateSignature(baseString, config.getConsumerSecret());

        oauthParams.put("oauth_signature", signature);
        return oauthParams;
    }
}
```

### 8.2 API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/one/oauth1/api/v13/job/generateDraft` | POST | Create draft with preview |
| `/one/oauth1/api/v13/job/finalizeDraft` | POST | Finalize and generate PDF |
| `/one/oauth1/api/v13/job/{jobId}/status` | GET | Check job status |

### 8.3 Request Format

```java
@Data
public class SmartCommRequest {
    private List<Property> properties;
    private Long batchConfigResId;
    private String transactionData; // Base64 encoded XML

    @Data
    public static class Property {
        private String name;
        private String value;
    }
}
```

**Key Properties:**
- `job.message.level`: Logging level (1-3)
- `merge.pdf`: Whether to merge multiple outputs
- `output.format`: PDF, HTML, etc.

### 8.4 Response Handling

```java
@Data
public class SmartCommResponse {
    private String jobId;
    private String status;
    private List<Document> documents;
    private List<Message> messages;

    @Data
    public static class Document {
        private String documentId;
        private String documentData; // Base64 encoded PDF
        private String mimeType;
    }
}
```

---

## 9. Error Handling & Resilience

### 9.1 Retry Strategy

```java
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate smartCommRetryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1000, 2.0, 10000)
            .retryOn(SmartCommTransientException.class)
            .retryOn(SocketTimeoutException.class)
            .build();
    }
}
```

### 9.2 Circuit Breaker

```java
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .build();

        return CircuitBreakerRegistry.of(config);
    }
}
```

### 9.3 Error Codes

| Code | Description | Retry |
|------|-------------|-------|
| `DOCGEN-001` | Template not found | No |
| `DOCGEN-002` | Invalid XML data | No |
| `DOCGEN-003` | SmartComm authentication failed | No |
| `DOCGEN-004` | SmartComm service unavailable | Yes |
| `DOCGEN-005` | SmartComm processing error | Depends |
| `DOCGEN-006` | Storage upload failed | Yes |
| `DOCGEN-007` | Database error | Yes |

---

## 10. Security Considerations

### 10.1 Data Protection

- **In Transit**: TLS 1.2+ for all API calls
- **At Rest**: AES-256 encryption for stored PDFs
- **Credentials**: Store SmartComm credentials in Vault/Secrets Manager

### 10.2 Access Control

```java
@PreAuthorize("hasRole('DOCUMENT_GENERATOR')")
public DraftResponse createDraft(DraftRequest request) { ... }

@PreAuthorize("hasRole('BULK_PROCESSOR')")
public BulkJobResponse submitJob(BulkJobRequest request) { ... }
```

### 10.3 Input Validation

- Validate all input against XSD schema before processing
- Sanitize user-provided content to prevent XML injection
- Validate file sizes and content types

### 10.4 Audit Logging

```java
@Aspect
@Component
public class AuditAspect {

    @Around("@annotation(Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) {
        AuditEvent event = AuditEvent.builder()
            .action(getAction(joinPoint))
            .user(SecurityContextHolder.getContext().getAuthentication().getName())
            .timestamp(Instant.now())
            .parameters(getParameters(joinPoint))
            .build();

        auditService.log(event);
        return joinPoint.proceed();
    }
}
```

---

## 11. Performance Considerations

### 11.1 Caching

| Cache | TTL | Purpose |
|-------|-----|---------|
| Template Config | 5 min | Reduce DB calls for template lookup |
| SmartComm Batch Config | 10 min | Cache vendor template mappings |
| Preview URLs | 1 hour | Pre-signed URL caching |

### 11.2 Connection Pooling

```yaml
# SmartComm HTTP Client
smartcomm:
  connection:
    max-total: 100
    max-per-route: 20
    connect-timeout: 5000
    read-timeout: 30000
    idle-timeout: 60000
```

### 11.3 Bulk Processing

- Batch size: 50 items per SmartComm call
- Parallel processing: 5 concurrent workers
- Rate limiting: 100 requests/minute to SmartComm

---

## 12. Monitoring & Observability

### 12.1 Metrics

```java
@Component
public class DocumentGenerationMetrics {

    private final Counter draftsCreated;
    private final Counter documentsGenerated;
    private final Timer generationLatency;
    private final Gauge activeJobs;

    // Prometheus metrics
    // - docgen_drafts_created_total
    // - docgen_documents_generated_total
    // - docgen_generation_latency_seconds
    // - docgen_active_jobs
    // - docgen_smartcomm_requests_total
    // - docgen_smartcomm_errors_total
}
```

### 12.2 Health Checks

```java
@Component
public class SmartCommHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            smartCommClient.healthCheck();
            return Health.up()
                .withDetail("smartcomm", "Available")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("smartcomm", "Unavailable")
                .withException(e)
                .build();
        }
    }
}
```

### 12.3 Distributed Tracing

- Use OpenTelemetry for distributed tracing
- Trace ID propagation across services
- Span creation for SmartComm API calls

---

## 13. Deployment Architecture

### 13.1 Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: document-generation-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: document-generation-service
  template:
    spec:
      containers:
      - name: app
        image: document-generation-service:latest
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SMARTCOMM_CONSUMER_KEY
          valueFrom:
            secretKeyRef:
              name: smartcomm-credentials
              key: consumer-key
```

### 13.2 Horizontal Pod Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: document-generation-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: document-generation-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 14. Project Structure

```
document-generation-service/
├── src/main/java/com/creditone/docgen/
│   ├── DocGenApplication.java
│   ├── api/
│   │   ├── controller/
│   │   │   ├── DraftController.java
│   │   │   ├── BulkGenerationController.java
│   │   │   └── DocumentController.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── DraftRequest.java
│   │   │   │   ├── BulkJobRequest.java
│   │   │   │   └── ...
│   │   │   └── response/
│   │   │       ├── DraftResponse.java
│   │   │       ├── BulkJobStatusResponse.java
│   │   │       └── ...
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java
│   │       └── ...
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── GenerationJob.java
│   │   │   ├── GenerationJobItem.java
│   │   │   ├── DraftState.java
│   │   │   └── DocumentRequest.java
│   │   ├── repository/
│   │   │   ├── GenerationJobRepository.java
│   │   │   ├── DocumentRequestRepository.java
│   │   │   └── ...
│   │   └── model/           # JAXB generated from XSD
│   │       ├── Data.java
│   │       ├── TemplateType.java
│   │       ├── RecipientType.java
│   │       └── ...
│   ├── service/
│   │   ├── DraftService.java
│   │   ├── DraftServiceImpl.java
│   │   ├── DirectGenerationService.java      # Kafka-triggered doc generation
│   │   ├── DirectGenerationServiceImpl.java
│   │   ├── BulkGenerationService.java
│   │   ├── BulkGenerationServiceImpl.java
│   │   ├── XmlBuilderService.java
│   │   ├── StorageService.java
│   │   ├── ValidationService.java
│   │   └── EventPublisherService.java
│   ├── integration/
│   │   ├── smartcomm/                        # Document generation vendor
│   │   │   ├── SmartCommClient.java
│   │   │   ├── SmartCommClientImpl.java
│   │   │   ├── SmartCommOAuth1Signer.java
│   │   │   ├── dto/
│   │   │   │   ├── SmartCommRequest.java
│   │   │   │   └── SmartCommResponse.java
│   │   │   └── exception/
│   │   │       └── SmartCommException.java
│   │   ├── storage/
│   │   │   ├── ObjectStorageClient.java
│   │   │   └── S3StorageClient.java
│   │   └── kafka/
│   │       ├── consumer/
│   │       │   ├── DocumentRequestConsumer.java  # Consumes doc generation requests
│   │       │   └── BulkJobItemConsumer.java
│   │       ├── producer/
│   │       │   ├── DocumentGeneratedProducer.java
│   │       │   └── BulkJobProducer.java
│   │       └── event/
│   │           ├── DocumentRequestEvent.java
│   │           └── DocumentGeneratedEvent.java
│   └── config/
│       ├── SmartCommConfig.java
│       ├── KafkaConfig.java
│       ├── CacheConfig.java
│       ├── SecurityConfig.java
│       └── RetryConfig.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── schema/
│   │   └── schema_core_v12.xsd
│   └── db/migration/
│       ├── V1__create_generation_tables.sql
│       └── V2__add_indexes.sql
├── src/test/
│   ├── java/
│   │   └── ...
│   └── resources/
│       └── test-data/
├── pom.xml
└── README.md
```

---

## 15. Implementation Phases

### Phase 1: Core Infrastructure
- Project setup with Spring Boot
- Generate JAXB classes from XSD
- SmartComm client with OAuth1 authentication
- Basic draft creation and finalization

### Phase 2: Draft Management
- Draft CRUD operations
- Preview generation
- Draft state persistence
- Draft expiration handling

### Phase 3: Bulk Processing
- Kafka integration
- Bulk job submission
- Async processing workers
- Progress tracking

### Phase 4: Storage & Retrieval
- Object storage integration (S3/Azure)
- Document retrieval APIs
- Pre-signed URL generation
- Storage cleanup jobs

### Phase 5: Production Readiness
- Comprehensive error handling
- Monitoring and alerting
- Performance optimization
- Security hardening

---

## 16. Appendix

### A. Configuration Reference

```yaml
# application.yml
spring:
  application:
    name: document-generation-service
  datasource:
    url: jdbc:postgresql://localhost:5432/document_hub
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    consumer:
      group-id: docgen-group

smartcomm:
  base-url: https://na16-sb.smartcommunications.cloud
  api-version: v13
  consumer-key: ${SMARTCOMM_CONSUMER_KEY}
  consumer-secret: ${SMARTCOMM_CONSUMER_SECRET}
  timeout:
    connect: 5000
    read: 30000

storage:
  provider: s3  # or 'azure'
  bucket: document-generation-output
  region: us-east-1

docgen:
  draft:
    expiry-minutes: 60
    cleanup-cron: "0 0 * * * *"
  bulk:
    batch-size: 50
    max-workers: 5
    rate-limit: 100
```

### B. API Error Response Format

```json
{
  "timestamp": "2024-12-17T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "DOCGEN-002",
  "message": "Invalid XML data: Element 'recipient' is required",
  "path": "/api/v1/drafts",
  "traceId": "abc123def456"
}
```
