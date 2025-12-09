# Document Enquiry Flow - Architecture Documentation

This document provides detailed visual diagrams of the document-enquiry endpoint logic flow.

## Table of Contents
1. [High-Level Sequence Diagram](#1-high-level-sequence-diagram)
2. [Data Extraction Chain Flow](#2-data-extraction-chain-flow)
3. [Rule Evaluation Logic](#3-rule-evaluation-logic)
4. [Document Matching Strategy](#4-document-matching-strategy)
5. [Sharing Scope Decision Tree](#5-sharing-scope-decision-tree)
6. [Complete End-to-End Flow](#6-complete-end-to-end-flow)

---

## 1. High-Level Sequence Diagram

This diagram shows the main interaction flow between components when processing a document enquiry request.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as DocumentEnquiryController
    participant Service as DocumentEnquiryService
    participant ExtractSvc as DataExtractionService
    participant RuleSvc as RuleEvaluationService
    participant TemplateRepo as TemplateRepository
    participant StorageRepo as StorageIndexRepository
    participant ExternalAPI as External APIs

    Client->>Controller: POST /documents-enquiry
    Note over Client,Controller: Headers: X-version, X-correlation-id,<br/>X-requestor-id, X-requestor-type<br/>Body: customerId, accountId[], filters

    Controller->>Controller: Validate request
    Controller->>Service: getDocuments(request)

    Service->>TemplateRepo: findActiveTemplates()
    TemplateRepo-->>Service: List<Template>

    loop For each accountId
        Service->>Service: Build AccountMetadata

        loop For each Template
            alt sharing_scope = NULL (Account-Specific)
                Service->>StorageRepo: findAccountSpecificDocuments(accountId, templateType)
                StorageRepo-->>Service: Account documents

            else sharing_scope = ALL
                Service->>StorageRepo: findSharedDocuments(templateType)
                StorageRepo-->>Service: Shared documents (no rules check)

            else sharing_scope = ACCOUNT_TYPE
                Service->>RuleSvc: evaluateEligibility(criteria, accountMetadata)
                RuleSvc-->>Service: eligible: boolean
                alt eligible = true
                    Service->>StorageRepo: findSharedDocuments(templateType)
                    StorageRepo-->>Service: Shared documents
                end

            else sharing_scope = CUSTOM_RULES
                alt has data_extraction_config
                    Service->>ExtractSvc: extractFields(config, context)

                    loop For each API in dependency order
                        ExtractSvc->>ExternalAPI: HTTP GET/POST
                        ExternalAPI-->>ExtractSvc: API Response
                        ExtractSvc->>ExtractSvc: Extract fields via JSONPath
                    end

                    ExtractSvc-->>Service: extractedFields Map
                    Service->>Service: Merge extractedFields into context
                end

                Service->>RuleSvc: evaluateEligibility(criteria, mergedContext)
                RuleSvc-->>Service: eligible: boolean

                alt eligible = true
                    alt has documentMatching.referenceKey
                        Service->>StorageRepo: findByReferenceKey(extractedKey, templateType)
                    else
                        Service->>StorageRepo: findSharedDocuments(templateType)
                    end
                    StorageRepo-->>Service: Matching documents
                end
            end

            Service->>Service: filterByValidity(documents)
        end
    end

    Service->>Service: Aggregate & deduplicate documents
    Service->>Service: Apply pagination (pageNumber, pageSize)
    Service->>Service: Build HATEOAS links

    Service-->>Controller: DocumentRetrievalResponse
    Controller-->>Client: 200 OK + JSON Response
```

---

## 2. Data Extraction Chain Flow

This diagram details how the `ConfigurableDataExtractionService` processes multi-step API chains to extract fields needed for eligibility evaluation and document matching.

```mermaid
sequenceDiagram
    autonumber
    participant Service as DocumentEnquiryService
    participant ExtractSvc as ConfigurableDataExtractionService
    participant Resolver as PlaceholderResolver
    participant WebClient as WebClient
    participant API1 as API 1<br/>(accountArrangementsApi)
    participant API2 as API 2<br/>(pricingApi)
    participant JSONPath as JSONPath Parser

    Service->>ExtractSvc: extractFields(dataExtractionConfig, initialContext)
    Note over Service,ExtractSvc: initialContext contains:<br/>accountId, customerId, correlationId

    ExtractSvc->>ExtractSvc: Parse fieldsToExtract[]
    Note over ExtractSvc: Fields needed: [pricingId, disclosureCode]

    ExtractSvc->>ExtractSvc: Build dependency graph
    Note over ExtractSvc: pricingId requires: accountId (available)<br/>disclosureCode requires: pricingId (not available)

    ExtractSvc->>ExtractSvc: Determine execution order
    Note over ExtractSvc: Step 1: accountArrangementsApi → pricingId<br/>Step 2: pricingApi → disclosureCode

    rect rgb(240, 248, 255)
        Note over ExtractSvc,API1: STEP 1: Extract pricingId
        ExtractSvc->>Resolver: resolveUrl(url, context)
        Note over Resolver: URL: /accounts/${accountId}/arrangements<br/>→ /accounts/550e8400.../arrangements
        Resolver-->>ExtractSvc: resolved URL

        ExtractSvc->>WebClient: GET /accounts/{accountId}/arrangements
        WebClient->>API1: HTTP GET
        API1-->>WebClient: JSON Response
        WebClient-->>ExtractSvc: Response body

        ExtractSvc->>JSONPath: extract(response, path)
        Note over JSONPath: Path: $.content[?(@.domain=="PRICING"<br/>&& @.status=="ACTIVE")].domainId
        JSONPath-->>ExtractSvc: "PRC-12345"

        ExtractSvc->>ExtractSvc: context.put("pricingId", "PRC-12345")
    end

    rect rgb(255, 248, 240)
        Note over ExtractSvc,API2: STEP 2: Extract disclosureCode
        ExtractSvc->>Resolver: resolveUrl(url, context)
        Note over Resolver: URL: /pricing-service/prices/${pricingId}<br/>→ /pricing-service/prices/PRC-12345
        Resolver-->>ExtractSvc: resolved URL

        ExtractSvc->>WebClient: GET /pricing-service/prices/PRC-12345
        WebClient->>API2: HTTP GET
        API2-->>WebClient: JSON Response
        WebClient-->>ExtractSvc: Response body

        ExtractSvc->>JSONPath: extract(response, path)
        Note over JSONPath: Path: $.cardholderAgreementsTncCode
        JSONPath-->>ExtractSvc: "D164"

        ExtractSvc->>ExtractSvc: context.put("disclosureCode", "D164")
    end

    ExtractSvc-->>Service: extractedFields = {pricingId: "PRC-12345", disclosureCode: "D164"}
```

### Data Extraction Configuration Example

```json
{
  "fieldsToExtract": ["pricingId", "disclosureCode"],
  "fieldSources": {
    "pricingId": {
      "sourceApi": "accountArrangementsApi",
      "extractionPath": "$.content[?(@.domain==\"PRICING\" && @.status==\"ACTIVE\")].domainId",
      "requiredInputs": ["accountId"]
    },
    "disclosureCode": {
      "sourceApi": "pricingApi",
      "extractionPath": "$.cardholderAgreementsTncCode",
      "requiredInputs": ["pricingId"]
    }
  },
  "dataSources": {
    "accountArrangementsApi": {
      "endpoint": {
        "url": "http://api/accounts/${accountId}/arrangements",
        "method": "GET"
      }
    },
    "pricingApi": {
      "endpoint": {
        "url": "http://api/pricing-service/prices/${pricingId}",
        "method": "GET"
      }
    }
  }
}
```

---

## 3. Rule Evaluation Logic

This diagram shows how the `RuleEvaluationService` processes eligibility criteria with AND/OR operators.

```mermaid
flowchart TD
    subgraph Input
        A[eligibility_criteria JSON] --> B{Parse operator}
        C[context Map] --> D[Available fields]
    end

    B -->|AND| E[evaluateWithAND]
    B -->|OR| F[evaluateWithOR]

    subgraph "AND Evaluation"
        E --> G[For each rule]
        G --> H{Evaluate rule}
        H -->|Pass| I{More rules?}
        I -->|Yes| G
        I -->|No| J[Return TRUE]
        H -->|Fail| K[Return FALSE immediately]
    end

    subgraph "OR Evaluation"
        F --> L[For each rule]
        L --> M{Evaluate rule}
        M -->|Pass| N[Return TRUE immediately]
        M -->|Fail| O{More rules?}
        O -->|Yes| L
        O -->|No| P[Return FALSE]
    end

    subgraph "Single Rule Evaluation"
        H --> Q[Get field value from context]
        Q --> R{Field exists?}
        R -->|No| S[Return FALSE]
        R -->|Yes| T{Match operator}

        T -->|EQUALS| U[value == ruleValue]
        T -->|NOT_EQUALS| V[value != ruleValue]
        T -->|IN| W[ruleValues.contains value]
        T -->|NOT_IN| X[!ruleValues.contains value]
        T -->|GREATER_THAN| Y[value > ruleValue]
        T -->|LESS_THAN| Z[value < ruleValue]
        T -->|CONTAINS| AA[value.contains ruleValue]
        T -->|STARTS_WITH| AB[value.startsWith ruleValue]
        T -->|ENDS_WITH| AC[value.endsWith ruleValue]
    end

    style J fill:#90EE90
    style N fill:#90EE90
    style K fill:#FFB6C1
    style P fill:#FFB6C1
```

### Supported Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `EQUALS` | Exact match | `accountType == "credit_card"` |
| `NOT_EQUALS` | Not equal | `region != "RESTRICTED"` |
| `IN` | Value in list | `state IN ["CA", "NY", "TX"]` |
| `NOT_IN` | Value not in list | `segment NOT_IN ["BLOCKED"]` |
| `GREATER_THAN` | Numeric greater | `creditScore > 700` |
| `GREATER_THAN_OR_EQUAL` | Numeric >= | `income >= 50000` |
| `LESS_THAN` | Numeric less | `age < 65` |
| `LESS_THAN_OR_EQUAL` | Numeric <= | `balance <= 10000` |
| `CONTAINS` | String contains | `email CONTAINS "@company"` |
| `STARTS_WITH` | String prefix | `zipcode STARTS_WITH "94"` |
| `ENDS_WITH` | String suffix | `phone ENDS_WITH "0000"` |

### Example Eligibility Criteria

```json
{
  "operator": "AND",
  "rules": [
    { "field": "customerSegment", "operator": "EQUALS", "value": "VIP" },
    { "field": "region", "operator": "IN", "value": ["US_WEST", "US_EAST"] },
    { "field": "creditScore", "operator": "GREATER_THAN_OR_EQUAL", "value": 750 }
  ]
}
```

---

## 4. Document Matching Strategy

This diagram shows how documents are matched based on the `documentMatching` configuration in `data_extraction_config`.

```mermaid
flowchart TD
    A[Template with CUSTOM_RULES] --> B{Has documentMatching config?}

    B -->|No| C[Query all shared docs for template]

    B -->|Yes| D{matchBy type?}

    D -->|reference_key| E[Reference Key Matching]
    D -->|conditional| F[Conditional Matching]

    subgraph "Reference Key Matching"
        E --> E1[Get referenceKeyField from config]
        E1 --> E2[Look up value in extractedFields]
        E2 --> E3[Query: reference_key = extractedValue<br/>AND reference_key_type = configuredType]
        E3 --> E4[Return matching documents]
    end

    subgraph "Conditional Matching"
        F --> F1[Get conditions array]
        F1 --> F2[For each condition in order]
        F2 --> F3{Evaluate condition<br/>against extractedFields}
        F3 -->|Match| F4[Use condition's referenceKey]
        F3 -->|No Match| F5{More conditions?}
        F5 -->|Yes| F2
        F5 -->|No| F6[No document matched]
        F4 --> F7[Query by matched referenceKey]
    end

    C --> G[Filter by validity dates]
    E4 --> G
    F7 --> G
    G --> H[Return to caller]

    style E4 fill:#90EE90
    style F7 fill:#90EE90
    style F6 fill:#FFB6C1
```

### Reference Key Matching Example

```json
{
  "documentMatching": {
    "matchBy": "reference_key",
    "referenceKeyField": "disclosureCode",
    "referenceKeyType": "DISCLOSURE_CODE"
  }
}
```

**Flow:**
1. Extract `disclosureCode` = "D164" from APIs
2. Query: `SELECT * FROM storage_index WHERE reference_key = 'D164' AND reference_key_type = 'DISCLOSURE_CODE'`
3. Returns: `Credit_Card_Terms_D164_v1.pdf`

### Conditional Matching Example

```json
{
  "documentMatching": {
    "matchBy": "conditional",
    "referenceKeyType": "BALANCE_TIER",
    "conditions": [
      { "field": "accountBalance", "operator": ">=", "value": 50000, "referenceKey": "PLATINUM" },
      { "field": "accountBalance", "operator": ">=", "value": 25000, "referenceKey": "GOLD" },
      { "field": "accountBalance", "operator": ">=", "value": 10000, "referenceKey": "SILVER" },
      { "field": "accountBalance", "operator": ">=", "value": 0, "referenceKey": "STANDARD" }
    ]
  }
}
```

**Flow:**
1. Extract `accountBalance` = 35000 from API
2. Evaluate conditions in order:
   - 35000 >= 50000? No
   - 35000 >= 25000? **Yes** → Use "GOLD"
3. Query: `SELECT * FROM storage_index WHERE reference_key = 'GOLD' AND reference_key_type = 'BALANCE_TIER'`

---

## 5. Sharing Scope Decision Tree

This diagram provides a complete decision tree for how documents are retrieved based on template configuration.

```mermaid
flowchart TD
    A[Process Template] --> B{shared_document_flag?}

    B -->|false| C[ACCOUNT-SPECIFIC]
    C --> C1[Query by account_key = requestedAccountId]
    C1 --> C2[Only owner can access]

    B -->|true| D{sharing_scope value?}

    D -->|NULL or empty| E[Treat as ACCOUNT-SPECIFIC]
    E --> C1

    D -->|ALL| F[SHARED WITH EVERYONE]
    F --> F1[No eligibility check needed]
    F1 --> F2[Query all shared docs for template]

    D -->|ACCOUNT_TYPE| G[ACCOUNT TYPE BASED]
    G --> G1[Get eligibility_criteria from template_config]
    G1 --> G2{accountType matches rule?}
    G2 -->|Yes| G3[Query shared docs]
    G2 -->|No| G4[Skip - not eligible]

    D -->|CUSTOM_RULES| H[CUSTOM RULES]
    H --> H1{Has data_extraction_config?}

    H1 -->|Yes| I[DYNAMIC EXTRACTION]
    I --> I1[Call external APIs]
    I1 --> I2[Extract fields via JSONPath]
    I2 --> I3[Merge into evaluation context]
    I3 --> J

    H1 -->|No| J[STATIC RULES]
    J --> J1[Get eligibility_criteria]
    J1 --> J2{Evaluate rules against context}
    J2 -->|Pass| K{Has documentMatching?}
    J2 -->|Fail| L[Skip - not eligible]

    K -->|Yes| M[Query by reference_key]
    K -->|No| N[Query all shared docs]

    C2 --> O[Filter by validity dates]
    F2 --> O
    G3 --> O
    M --> O
    N --> O

    O --> P[Add to results]

    style C2 fill:#E6E6FA
    style F2 fill:#98FB98
    style G3 fill:#87CEEB
    style M fill:#FFD700
    style N fill:#FFD700
    style G4 fill:#FFB6C1
    style L fill:#FFB6C1
```

### Sharing Scope Summary

| Scope | Description | Eligibility Check | Document Query |
|-------|-------------|-------------------|----------------|
| `NULL` | Account-specific | None | By `account_key` |
| `ALL` | Everyone | None | All shared docs |
| `ACCOUNT_TYPE` | Product-based | `accountType` rule | Shared if eligible |
| `CUSTOM_RULES` | Complex criteria | Full rule evaluation | By reference_key or all shared |

---

## 6. Complete End-to-End Flow

This comprehensive diagram shows the entire document enquiry process from request to response.

```mermaid
flowchart TB
    subgraph "1. REQUEST HANDLING"
        A[Client POST /documents-enquiry] --> B[DocumentEnquiryController]
        B --> C{Validate Headers}
        C -->|Missing| D[400 Bad Request]
        C -->|Valid| E{Validate Body}
        E -->|Invalid| D
        E -->|Valid| F[Call DocumentEnquiryService]
    end

    subgraph "2. TEMPLATE LOADING"
        F --> G[Load active templates]
        G --> H[TemplateRepository.findActiveTemplates]
        H --> I[Filter by accessible_flag = true]
    end

    subgraph "3. ACCOUNT PROCESSING"
        I --> J[For each accountId in request]
        J --> K[Build AccountMetadata]
        K --> L[For each Template]
    end

    subgraph "4. ELIGIBILITY & EXTRACTION"
        L --> M{Check sharing_scope}

        M -->|Account-Specific| N1[Direct query by account]

        M -->|ALL| N2[No check - get shared docs]

        M -->|ACCOUNT_TYPE| N3[Check accountType rule]
        N3 --> N3a{Eligible?}
        N3a -->|Yes| N3b[Get shared docs]
        N3a -->|No| SKIP1[Skip template]

        M -->|CUSTOM_RULES| N4{Has extraction config?}
        N4 -->|Yes| N4a[DataExtractionService]
        N4a --> N4b[Call External APIs]
        N4b --> N4c[Extract via JSONPath]
        N4c --> N4d[Merge context]
        N4 -->|No| N4d
        N4d --> N4e[RuleEvaluationService]
        N4e --> N4f{Eligible?}
        N4f -->|Yes| N4g{Has documentMatching?}
        N4f -->|No| SKIP2[Skip template]
        N4g -->|Yes| N4h[Query by reference_key]
        N4g -->|No| N4i[Get shared docs]
    end

    subgraph "5. DOCUMENT FILTERING"
        N1 --> O[Collected Documents]
        N2 --> O
        N3b --> O
        N4h --> O
        N4i --> O

        O --> P[Filter by validity dates]
        P --> Q{valid_from <= today?}
        Q -->|No| R[Exclude - future doc]
        Q -->|Yes| S{valid_until >= today?}
        S -->|No| T[Exclude - expired]
        S -->|Yes| U[Include in results]
    end

    subgraph "6. RESPONSE BUILDING"
        U --> V[Aggregate all documents]
        V --> W[Deduplicate]
        W --> X[Apply sorting]
        X --> Y[Apply pagination]
        Y --> Z[Build HATEOAS links]
        Z --> AA[DocumentRetrievalResponse]
    end

    AA --> AB[200 OK Response]

    style D fill:#FFB6C1
    style SKIP1 fill:#FFB6C1
    style SKIP2 fill:#FFB6C1
    style R fill:#FFB6C1
    style T fill:#FFB6C1
    style AB fill:#90EE90
```

---

## Key Components Reference

| Component | File | Responsibility |
|-----------|------|----------------|
| `DocumentEnquiryController` | `controller/DocumentEnquiryController.java` | HTTP handling, validation |
| `DocumentEnquiryService` | `service/DocumentEnquiryService.java` | Main orchestration logic |
| `ConfigurableDataExtractionService` | `service/ConfigurableDataExtractionService.java` | External API calls, JSONPath extraction |
| `RuleEvaluationService` | `service/RuleEvaluationService.java` | Eligibility criteria evaluation |
| `MasterTemplateDefinitionRepository` | `repository/MasterTemplateDefinitionRepository.java` | Template data access |
| `StorageIndexRepository` | `repository/StorageIndexRepository.java` | Document data access |

---

## Related Documentation

- [Template Onboarding Guide](../Template_Onboarding_Guide.md) - How to configure templates
- [Interactive Template Builder](../Interactive_Template_Builder_Concept.md) - UI for template creation
- [Current Status](../CURRENT_STATUS.md) - Project status and roadmap
