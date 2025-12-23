# Document Enquiry Flow - Architecture Documentation

This document provides detailed visual diagrams of the document-enquiry endpoint logic flow, reflecting the refactored service architecture.

## Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [High-Level Sequence Diagram](#2-high-level-sequence-diagram)
3. [Data Extraction Flow](#3-data-extraction-flow)
4. [Access Control & HATEOAS Links](#4-access-control--hateoas-links)
5. [Document Matching Strategy](#5-document-matching-strategy)
6. [Complete End-to-End Flow](#6-complete-end-to-end-flow)
7. [Component Reference](#7-component-reference)

---

## 1. Architecture Overview

The document enquiry system has been refactored into focused, single-responsibility services:

### Service Architecture

```mermaid
flowchart TB
    subgraph "Controller Layer"
        Controller[DocumentEnquiryController]
    end

    subgraph "Processor Layer"
        Processor[DocumentEnquiryProcessor<br/><i>Orchestrator</i>]
    end

    subgraph "DAO Layer"
        TemplateDao[MasterTemplateDao<br/><i>Template Queries</i>]
        StorageDao[StorageIndexDao<br/><i>Document Queries</i>]
    end

    subgraph "Domain Services"
        MatchingSvc[DocumentMatchingService<br/><i>Query Logic</i>]
        ResponseBuilder[DocumentResponseBuilder<br/><i>Response Construction</i>]
        AccessControl[DocumentAccessControlService<br/><i>HATEOAS Links</i>]
        ValiditySvc[DocumentValidityService<br/><i>Date Filtering</i>]
    end

    subgraph "Data Extraction Services"
        ExtractSvc[ConfigurableDataExtractionService<br/><i>Coordinator</i>]
        PlanBuilder[ExtractionPlanBuilder<br/><i>Dependency Resolution</i>]
        ApiExecutor[ApiCallExecutor<br/><i>HTTP Execution</i>]
        FieldExtractor[FieldExtractor<br/><i>JSONPath Extraction</i>]
    end

    subgraph "Supporting Services"
        AccountSvc[AccountMetadataService]
        RuleSvc[RuleEvaluationService]
    end

    subgraph "Repositories"
        TemplateRepo[(MasterTemplateRepository)]
        StorageRepo[(StorageIndexRepository)]
    end

    Controller --> Processor
    Processor --> TemplateDao
    Processor --> MatchingSvc
    Processor --> ResponseBuilder
    Processor --> ExtractSvc
    Processor --> AccountSvc

    TemplateDao --> TemplateRepo
    StorageDao --> StorageRepo

    MatchingSvc --> StorageRepo
    MatchingSvc --> ValiditySvc

    ResponseBuilder --> AccessControl

    ExtractSvc --> PlanBuilder
    ExtractSvc --> ApiExecutor
    ApiExecutor --> FieldExtractor

    style Processor fill:#e1f5fe
    style ExtractSvc fill:#fff3e0
    style AccessControl fill:#f3e5f5
    style MatchingSvc fill:#e8f5e9
```

### Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | Each service handles one concern |
| **Separation of Concerns** | Orchestration separated from domain logic |
| **Dependency Injection** | All services are Spring-managed beans |
| **Reactive Streams** | Uses Project Reactor (Mono/Flux) throughout |

---

## 2. High-Level Sequence Diagram

This diagram shows the main interaction flow between the components based on the actual implementation.

### Key Filters Applied

| Step | Filter | Location | Purpose |
|------|--------|----------|---------|
| 1 | `accessible_flag` | Template query | Only accessible templates |
| 2 | `line_of_business` | Template query | Business unit filtering |
| 3 | `message_center_doc_flag` | Template query | Web display eligibility |
| 4 | `communication_type` | Template query | Letter/Email/SMS/Push |
| 5 | `start_date`/`end_date` | Template query | Template validity period |
| 6 | `sharing_scope` | Per template | Access control logic |
| 7 | `start_date`/`end_date` | Document query | Document validity (DB columns) |
| 8 | `valid_from`/`valid_until` | DocumentValidityService | Document validity (JSON metadata) |
| 9 | `single_document_flag` | Per template | Return only latest document |

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as DocumentEnquiryController
    participant Processor as DocumentEnquiryProcessor
    participant AccountSvc as AccountMetadataService
    participant TemplateDao as MasterTemplateDao
    participant ExtractSvc as ConfigurableDataExtractionService
    participant MatchingSvc as DocumentMatchingService
    participant ResponseBuilder as DocumentResponseBuilder
    participant AccessCtrl as DocumentAccessControlService

    Client->>Controller: POST /documents-enquiry
    Note over Client,Controller: Headers: X-requestor-type (CUSTOMER/AGENT/SYSTEM)<br/>Body: customerId, accountId[], lineOfBusiness,<br/>messageCenterDocFlag, communicationType

    Controller->>Controller: Validate request headers
    Controller->>Processor: processEnquiry(request, requestorType)

    rect rgb(255, 250, 230)
        Note over Processor,AccountSvc: STEP 1: RESOLVE ACCOUNT IDs

        alt accountId[] provided in request
            Processor->>Processor: Use request.accountId[]
        else only customerId provided
            Processor->>AccountSvc: getAccountsByCustomerId(customerId)
            AccountSvc-->>Processor: List<AccountMetadata>
            Processor->>Processor: Extract accountIds from metadata
        else neither provided
            Processor-->>Controller: Empty response
        end
    end

    rect rgb(255, 245, 238)
        Note over Processor,TemplateDao: STEP 2: DETERMINE LOB & QUERY TEMPLATES

        alt lineOfBusiness in request
            Processor->>Processor: Use request.lineOfBusiness
        else lineOfBusiness not provided
            Processor->>AccountSvc: getAccountMetadata(firstAccountId)
            AccountSvc-->>Processor: AccountMetadata
            Processor->>Processor: Derive LOB from metadata
        end

        Processor->>TemplateDao: findActiveTemplatesWithFilters(lob, msgCenterFlag, commType, currentDate)
        Note over TemplateDao: WHERE line_of_business IN (:lob, 'ENTERPRISE')<br/>AND accessible_flag = true<br/>AND message_center_doc_flag = :flag<br/>AND (communication_type = :type OR :type IS NULL)<br/>AND start_date <= :now AND end_date >= :now
        TemplateDao-->>Processor: List<Template>
    end

    rect rgb(230, 250, 255)
        Note over Processor,MatchingSvc: STEP 3: PROCESS EACH ACCOUNT & TEMPLATE

        loop For each accountId
            Processor->>AccountSvc: getAccountMetadata(accountId)
            AccountSvc-->>Processor: AccountMetadata

            loop For each Template
                Processor->>Processor: canAccessTemplate(template, metadata)
                Note over Processor: Check sharing_scope vs accountType<br/>(ALL, CUSTOM_RULES, or specific type)

                alt template not accessible
                    Processor->>Processor: Skip template
                end

                alt has data_extraction_config
                    Processor->>ExtractSvc: extractData(configJson, request)
                    ExtractSvc-->>Processor: Map<String, Object> extractedFields
                end

                Processor->>MatchingSvc: queryDocuments(template, accountId, extractedData, fromDate, toDate)
                Note over MatchingSvc: 1. Check document_matching_config<br/>2. Apply reference_key or conditional matching<br/>3. Query StorageIndexRepository<br/>4. Filter by DocumentValidityService
                MatchingSvc-->>Processor: List<StorageIndexEntity>

                alt single_document_flag = true
                    Processor->>Processor: applySingleDocumentFlag()
                    Note over Processor: Keep only most recent by doc_creation_date
                end

                Processor->>ResponseBuilder: convertToNodes(documents, template, requestorType)
                ResponseBuilder->>AccessCtrl: getPermittedActions(template, requestorType)
                AccessCtrl-->>ResponseBuilder: List<Action> (View, Download, Delete, Update)
                ResponseBuilder->>AccessCtrl: buildLinksForDocument(document, actions)
                AccessCtrl-->>ResponseBuilder: Links object with HATEOAS
                ResponseBuilder-->>Processor: List<DocumentDetailsNode>
            end
        end
    end

    rect rgb(240, 255, 240)
        Note over Processor,ResponseBuilder: STEP 4: BUILD FINAL RESPONSE

        Processor->>Processor: flattenDocuments(allDocuments)
        Processor->>ResponseBuilder: paginate(documents, pageNumber, pageSize)
        ResponseBuilder-->>Processor: Paginated list
        Processor->>ResponseBuilder: buildResponse(docs, total, pageNum, pageSize, processingTime)
        ResponseBuilder-->>Processor: DocumentRetrievalResponse
    end

    Processor-->>Controller: DocumentRetrievalResponse
    Controller-->>Client: 200 OK + JSON Response
```

---

## 3. Data Extraction Flow

The data extraction system has been refactored into specialized components:

### 3.1 Extraction Architecture

```mermaid
flowchart LR
    subgraph "ConfigurableDataExtractionService"
        A[extractData] --> B[Parse JSON Config]
        B --> C[Create Initial Context]
        C --> D[Build Extraction Plan]
    end

    subgraph "ExtractionPlanBuilder"
        D --> E[Analyze Dependencies]
        E --> F[Determine API Order]
        F --> G[Create ExtractionPlan]
    end

    subgraph "ApiCallExecutor"
        G --> H{Execution Mode?}
        H -->|Sequential| I[Execute in Order]
        H -->|Parallel| J[Execute Concurrently]
        I --> K[Resolve Placeholders]
        J --> K
        K --> L[Make HTTP Call]
        L --> M[Handle Response]
    end

    subgraph "FieldExtractor"
        M --> N[Execute JSONPath]
        N --> O[Unwrap Single Arrays]
        O --> P[Apply Defaults]
        P --> Q[Return Extracted Fields]
    end

    style A fill:#fff3e0
    style G fill:#e3f2fd
    style L fill:#fce4ec
    style Q fill:#e8f5e9
```

### 3.2 Detailed Extraction Sequence

```mermaid
sequenceDiagram
    autonumber
    participant Service as DocumentEnquiryService
    participant ExtractSvc as ConfigurableDataExtractionService
    participant PlanBuilder as ExtractionPlanBuilder
    participant Executor as ApiCallExecutor
    participant Extractor as FieldExtractor
    participant WebClient as WebClient
    participant ExternalAPI as External API

    Service->>ExtractSvc: extractData(configJson, request)

    ExtractSvc->>ExtractSvc: parseConfig(configJson)
    Note over ExtractSvc: Parse JSON to DataExtractionConfig<br/>fieldsToExtract: [pricingId, disclosureCode]

    ExtractSvc->>ExtractSvc: createInitialContext(request)
    Note over ExtractSvc: context = {<br/>  accountId: "ACC-001",<br/>  customerId: "CUST-123",<br/>  correlationId: UUID,<br/>  auth.token: "..."<br/>}

    ExtractSvc->>PlanBuilder: buildPlan(config, context)

    rect rgb(240, 248, 255)
        Note over PlanBuilder: DEPENDENCY RESOLUTION

        PlanBuilder->>PlanBuilder: Analyze field dependencies
        Note over PlanBuilder: pricingId requires: accountId ✓ (available)<br/>disclosureCode requires: pricingId ✗ (not available)

        PlanBuilder->>PlanBuilder: Build execution order
        Note over PlanBuilder: Step 1: accountApi → pricingId<br/>Step 2: pricingApi → disclosureCode

        PlanBuilder-->>ExtractSvc: ExtractionPlan (2 API calls)
    end

    ExtractSvc->>Executor: executeSequential(plan, context)

    rect rgb(255, 248, 240)
        Note over Executor,ExternalAPI: API CALL 1: Get pricingId

        Executor->>Executor: resolvePlaceholders(url, context)
        Note over Executor: /accounts/${accountId}/arrangements<br/>→ /accounts/ACC-001/arrangements

        Executor->>WebClient: GET /accounts/ACC-001/arrangements
        WebClient->>ExternalAPI: HTTP GET
        ExternalAPI-->>WebClient: JSON Response
        WebClient-->>Executor: Response body

        Executor->>Extractor: extractFields(responseBody, apiCall)
        Extractor->>Extractor: JsonPath.read(body, "$.content[?(@.domain=='PRICING')].domainId")
        Extractor->>Extractor: unwrapSingleElementArray([\"PRC-12345\"])
        Extractor-->>Executor: {pricingId: "PRC-12345"}

        Executor->>Executor: context.put("pricingId", "PRC-12345")
    end

    rect rgb(240, 255, 240)
        Note over Executor,ExternalAPI: API CALL 2: Get disclosureCode

        Executor->>Executor: resolvePlaceholders(url, context)
        Note over Executor: /pricing/${pricingId}<br/>→ /pricing/PRC-12345

        Executor->>WebClient: GET /pricing/PRC-12345
        WebClient->>ExternalAPI: HTTP GET
        ExternalAPI-->>WebClient: JSON Response
        WebClient-->>Executor: Response body

        Executor->>Extractor: extractFields(responseBody, apiCall)
        Extractor->>Extractor: JsonPath.read(body, "$.cardholderAgreementsTncCode")
        Extractor-->>Executor: {disclosureCode: "D164"}

        Executor->>Executor: context.put("disclosureCode", "D164")
    end

    Executor-->>ExtractSvc: context with all extracted fields
    ExtractSvc-->>Service: {pricingId: "PRC-12345", disclosureCode: "D164"}
```

### 3.3 Error Handling in Extraction

```mermaid
flowchart TD
    A[API Call] --> B{Success?}

    B -->|Yes| C[Extract Fields via JSONPath]
    B -->|No| D[handleApiError]

    C --> E{Value Found?}
    E -->|Yes| F[Add to Context]
    E -->|No| G{Has Default?}

    D --> G

    G -->|Yes| H[Use Default Value]
    G -->|No| I[Log Warning, Continue]

    H --> F
    I --> J[Next API Call]
    F --> J

    style B fill:#fff3e0
    style D fill:#ffcdd2
    style H fill:#c8e6c9
```

---

## 4. Access Control & HATEOAS Links

The access control system determines which actions are available based on the requestor type and template configuration.

### 4.1 Access Control Flow

```mermaid
sequenceDiagram
    autonumber
    participant ResponseBuilder as DocumentResponseBuilder
    participant AccessCtrl as DocumentAccessControlService
    participant Template as Template.access_control

    ResponseBuilder->>AccessCtrl: getPermittedActions(template, "BANKER")

    AccessCtrl->>AccessCtrl: Parse access_control JSON
    Note over AccessCtrl: {<br/>  "CUSTOMER": ["View", "Download"],<br/>  "BANKER": ["View", "Update", "Download"],<br/>  "AGENT": ["View", "Update", "Delete", "Download"]<br/>}

    AccessCtrl->>AccessCtrl: Look up actions for requestorType
    AccessCtrl-->>ResponseBuilder: ["View", "Update", "Download"]

    loop For each document
        ResponseBuilder->>AccessCtrl: buildLinksForDocument(document, permittedActions)

        AccessCtrl->>AccessCtrl: Generate links for permitted actions only

        Note over AccessCtrl: View → GET /documents/{id}<br/>Update → PUT /documents/{id}<br/>Download → GET /documents/{id}/download<br/>(Delete excluded - not permitted for BANKER)

        AccessCtrl-->>ResponseBuilder: Links {<br/>  view: {href: "/documents/123"},<br/>  update: {href: "/documents/123"},<br/>  download: {href: "/documents/123/download"}<br/>}
    end
```

### 4.2 Access Control Matrix

```mermaid
flowchart TD
    subgraph "Request Processing"
        A[Incoming Request] --> B{X-requestor-type header}
        B --> C[CUSTOMER]
        B --> D[BANKER]
        B --> E[AGENT]
    end

    subgraph "Permission Lookup"
        C --> F[Load template.access_control]
        D --> F
        E --> F
        F --> G{Lookup requestorType}
    end

    subgraph "Action Permissions"
        G --> H[Get permitted actions array]
        H --> I{For each action}
        I -->|View| J["GET /documents/:id"]
        I -->|Update| K["PUT /documents/:id"]
        I -->|Delete| L["DELETE /documents/:id"]
        I -->|Download| M["GET /documents/:id/download"]
    end

    subgraph "HATEOAS Link Generation"
        J --> N[Add to _links]
        K --> N
        L --> N
        M --> N
        N --> O[Return in response]
    end

    style C fill:#e3f2fd
    style D fill:#fff3e0
    style E fill:#fce4ec
```

### 4.3 Default Access Control

If no `access_control` is configured on the template, defaults are applied:

| Requestor Type | Default Permissions |
|----------------|---------------------|
| `CUSTOMER` | View, Download |
| `BANKER` | View, Update, Download |
| `AGENT` | View, Update, Delete, Download |

### 4.4 Response Example with HATEOAS Links

```json
{
  "documents": [
    {
      "documentId": "doc-123",
      "documentType": "STATEMENT",
      "documentName": "January 2024 Statement",
      "lineOfBusiness": "CREDIT_CARD",
      "_links": {
        "view": {
          "href": "/api/v1/documents/doc-123",
          "method": "GET"
        },
        "download": {
          "href": "/api/v1/documents/doc-123/download",
          "method": "GET"
        }
      }
    }
  ],
  "pagination": {
    "pageNumber": 1,
    "pageSize": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

---

## 5. Document Matching Strategy

The `DocumentMatchingService` handles document query logic based on template configuration.

### 5.1 Matching Decision Flow

```mermaid
flowchart TD
    A[DocumentMatchingService.queryDocuments] --> B{Has reference_key in request?}

    B -->|Yes| C[Reference Key Query Mode]
    B -->|No| D{Has conditional matching config?}

    D -->|Yes| E[Conditional Matching Mode]
    D -->|No| F[Standard Query Mode]

    subgraph "Reference Key Query"
        C --> C1[Get referenceKey from request/extractedData]
        C1 --> C2[Get referenceKeyType from template]
        C2 --> C3[Query: WHERE reference_key = ? AND reference_key_type = ?]
    end

    subgraph "Conditional Matching"
        E --> E1[Evaluate conditions in order]
        E1 --> E2{Condition matches?}
        E2 -->|Yes| E3[Use condition's referenceKey]
        E2 -->|No| E4{More conditions?}
        E4 -->|Yes| E1
        E4 -->|No| E5[No match - return empty]
        E3 --> C3
    end

    subgraph "Standard Query"
        F --> F1[Query by template_type and account_key]
        F1 --> F2[Apply DB date filters]
    end

    C3 --> G[Apply accessible_flag filter]
    F2 --> G

    G --> H[Apply postedFromDate/postedToDate filter]
    H --> I[Apply start_date/end_date filter]

    subgraph "Post-Query Validity Filter"
        I --> J[DocumentValidityService.filterByValidity]
        J --> K{Check doc_metadata JSON}
        K --> L[Parse valid_from/valid_until fields]
        L --> M{Document currently valid?}
        M -->|Yes| N[Include in results]
        M -->|No - Expired| O[Filter out document]
        M -->|No - Future| O
    end

    N --> P[Return filtered documents]
    O --> P

    style C fill:#e3f2fd
    style E fill:#fff3e0
    style F fill:#e8f5e9
    style J fill:#fff9c4
```

### 5.2 Two-Level Date Filtering

The system applies date filtering at two levels:

| Level | Location | Fields | Purpose |
|-------|----------|--------|---------|
| **Database Query** | `StorageIndexRepository` | `start_date`, `end_date` columns | Efficient filtering at DB level |
| **Post-Query Filter** | `DocumentValidityService` | `valid_from`, `valid_until` in `doc_metadata` JSON | Fine-grained validity from metadata |

**Important**: Documents can be filtered out at either level. A document must pass BOTH filters to be returned:
1. DB columns `start_date`/`end_date` must be NULL or within range
2. JSON metadata `valid_from`/`valid_until` must be NULL or within range

### 5.3 Query Filters Applied

| Filter | Location | Description |
|--------|----------|-------------|
| `template_type` | Storage query | Match document to template |
| `account_key` | Storage query | Filter by account |
| `accessible_flag` | Storage query | Only accessible documents |
| `reference_key` | Storage query | Match specific document version |
| `postedFromDate` | Storage query | Document creation date >= |
| `postedToDate` | Storage query | Document creation date <= |

---

## 6. Complete End-to-End Flow

This comprehensive diagram shows the entire document enquiry process with all filters and validations.

```mermaid
flowchart TB
    subgraph "1. REQUEST HANDLING"
        A[Client POST /documents-enquiry] --> B[DocumentEnquiryController]
        B --> C{Validate Headers}
        C -->|Missing| D[400 Bad Request]
        C -->|Valid| E{Validate Body}
        E -->|Invalid| D
        E -->|Valid| F[Extract requestorType from header]
        F --> G[Call DocumentEnquiryService]
    end

    subgraph "2. TEMPLATE FILTERING"
        G --> H{LOB in request?}
        H -->|No| I[Get AccountMetadata]
        I --> J[Derive LOB]
        H -->|Yes| K[Use request LOB]
        J --> K

        K --> L[TemplateRepository.findActiveTemplatesWithFilters]
        L --> L1[Filter: accessible_flag = true]
        L1 --> L2[Filter: line_of_business match]
        L2 --> L3[Filter: message_center_doc_flag match]
        L3 --> L4[Filter: communication_type match]
        L4 --> L5[Filter: template validity period]
        L5 --> M[Filtered Templates]
    end

    subgraph "3. TEMPLATE PROCESSING"
        M --> N[For each Account]
        N --> O[Get AccountMetadata]
        O --> P[For each Template]

        P --> Q{Check sharing_scope<br/>vs accountType}
        Q -->|Mismatch| R[Skip Template]
        Q -->|Match| S{Has data_extraction_config?}

        S -->|Yes| T[ConfigurableDataExtractionService]
        T --> T1[ExtractionPlanBuilder.buildPlan]
        T1 --> T2[ApiCallExecutor.execute]
        T2 --> T3[FieldExtractor.extractFields]
        T3 --> U[Extracted Fields]

        S -->|No| U
        U --> V[DocumentMatchingService.queryDocuments]
        V --> W[Apply reference_key matching]
        W --> X[Apply DB date filters: start_date/end_date]
        X --> X1[DocumentValidityService.filterByValidity]
        X1 --> X2{Check doc_metadata<br/>valid_from/valid_until}
        X2 -->|Valid| X3[Include document]
        X2 -->|Expired/Future| X4[Filter out document]
        X3 --> Y{single_document_flag?}
        X4 --> Y
        Y -->|Yes| Z[Keep only latest document]
        Y -->|No| AA[Keep all documents]
    end

    subgraph "4. RESPONSE BUILDING"
        Z --> AB[Documents collected]
        AA --> AB
        R --> AB

        AB --> AC[DocumentResponseBuilder.convertToNodes]
        AC --> AD[DocumentAccessControlService.getPermittedActions]
        AD --> AE[Generate HATEOAS links per action]
        AE --> AF[Add lineOfBusiness from template]
        AF --> AG[Apply pagination]
        AG --> AH[Build final response]
    end

    AH --> AI[200 OK Response]

    style D fill:#ffcdd2
    style R fill:#ffcdd2
    style X4 fill:#ffcdd2
    style AI fill:#c8e6c9
    style X3 fill:#c8e6c9
    style T fill:#fff3e0
    style X1 fill:#fff9c4
    style AD fill:#e1bee7
```

---

## 7. Component Reference

### Core Components

| Component | File | Responsibility |
|-----------|------|----------------|
| `DocumentEnquiryController` | `controller/DocumentEnquiryController.java` | HTTP handling, request validation |
| `DocumentEnquiryProcessor` | `processor/DocumentEnquiryProcessor.java` | **Orchestrator** - coordinates all services |
| `DocumentMatchingService` | `service/DocumentMatchingService.java` | Document query logic, reference key matching |
| `DocumentResponseBuilder` | `service/DocumentResponseBuilder.java` | Response construction, pagination |
| `DocumentAccessControlService` | `service/DocumentAccessControlService.java` | HATEOAS link generation, permission checking |
| `DocumentValidityService` | `service/DocumentValidityService.java` | Date validation, validity filtering |

### DAO Layer

| Component | File | Responsibility |
|-----------|------|----------------|
| `MasterTemplateDao` | `dao/MasterTemplateDao.java` | Template queries with filters |
| `StorageIndexDao` | `dao/StorageIndexDao.java` | Document storage operations |

### Data Extraction Services

| Component | File | Responsibility |
|-----------|------|----------------|
| `ConfigurableDataExtractionService` | `service/ConfigurableDataExtractionService.java` | **Coordinator** - orchestrates extraction |
| `ExtractionPlanBuilder` | `service/extraction/ExtractionPlanBuilder.java` | Dependency resolution, execution ordering |
| `ApiCallExecutor` | `service/extraction/ApiCallExecutor.java` | HTTP call execution (sequential/parallel) |
| `FieldExtractor` | `service/extraction/FieldExtractor.java` | JSONPath extraction, default value handling |
| `ExtractionPlan` | `service/extraction/ExtractionPlan.java` | Data class - ordered API calls |
| `ApiCall` | `service/extraction/ApiCall.java` | Data class - single API call config |

### Supporting Services

| Component | File | Responsibility |
|-----------|------|----------------|
| `AccountMetadataService` | `service/AccountMetadataService.java` | Account metadata lookup |
| `RuleEvaluationService` | `service/RuleEvaluationService.java` | Eligibility criteria evaluation |

### Repositories

| Component | File | Responsibility |
|-----------|------|----------------|
| `MasterTemplateRepository` | `repository/MasterTemplateRepository.java` | Template queries with filters |
| `StorageIndexRepository` | `repository/StorageIndexRepository.java` | Document queries |

---

## Related Documentation

- [Template Onboarding Guide](../Template_Onboarding_Guide.md) - How to configure templates
- [Interactive Template Builder](../Interactive_Template_Builder_Concept.md) - UI for template creation
- [Implementation Status](../IMPLEMENTATION_STATUS.md) - Current implementation status
