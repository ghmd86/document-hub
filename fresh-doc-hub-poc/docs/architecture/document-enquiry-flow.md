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

### Key Concept: Two-Step Filtering

Based on John's clarification, the API implements **two separate filters**:

| Step | Filter | Purpose | Values |
|------|--------|---------|--------|
| **STEP 1** | `line_of_business` | Which business unit's templates to load | `CREDIT_CARD`, `DIGITAL_BANK`, `ENTERPRISE` |
| **STEP 2** | `sharing_scope` | Who can access within those templates | `NULL`, `ALL`, `ACCOUNT_TYPE`, `CUSTOM_RULES` |

- `ENTERPRISE` in line_of_business = template applies to ALL business units
- `ALL` in sharing_scope = document is accessible to ALL users (no eligibility check)

**These are NOT interchangeable** - they are applied in sequence.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as DocumentEnquiryController
    participant Service as DocumentEnquiryService
    participant AccountSvc as AccountMetadataService
    participant ExtractSvc as DataExtractionService
    participant RuleSvc as RuleEvaluationService
    participant TemplateRepo as TemplateRepository
    participant StorageRepo as StorageIndexRepository
    participant ExternalAPI as External APIs

    Client->>Controller: POST /documents-enquiry
    Note over Client,Controller: Headers: X-version, X-correlation-id,<br/>X-requestor-id, X-requestor-type<br/>Body: customerId, accountId[],<br/>lineOfBusiness (optional)

    Controller->>Controller: Validate request
    Controller->>Service: getDocuments(request)

    rect rgb(255, 250, 230)
        Note over Service,TemplateRepo: STEP 1: LINE OF BUSINESS FILTER

        alt lineOfBusiness in request
            Service->>Service: Use request.lineOfBusiness
        else lineOfBusiness not provided
            Service->>AccountSvc: getAccountMetadata(firstAccountId)
            AccountSvc-->>Service: AccountMetadata
            Service->>Service: Derive lineOfBusiness from accountType
        end

        Service->>TemplateRepo: findActiveTemplatesByLineOfBusiness(lob, currentDate)
        Note over TemplateRepo: WHERE line_of_business = :lob<br/>OR line_of_business = 'ENTERPRISE'
        TemplateRepo-->>Service: List<Template> (filtered by LOB)
    end

    rect rgb(230, 250, 255)
        Note over Service,StorageRepo: STEP 2: SHARING SCOPE FILTER (per template)

        loop For each accountId
            Service->>AccountSvc: getAccountMetadata(accountId)
            AccountSvc-->>Service: AccountMetadata

            loop For each Template (already filtered by LOB)
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
    end

    Service->>Service: Aggregate & deduplicate documents
    Service->>Service: Apply pagination (pageNumber, pageSize)
    Service->>Service: Build HATEOAS links

    Service-->>Controller: DocumentRetrievalResponse
    Controller-->>Client: 200 OK + JSON Response
```

### Two-Step Filtering Flowchart

```mermaid
flowchart TD
    A[Request arrives] --> B{lineOfBusiness<br/>in request?}

    B -->|Yes| C[Use request.lineOfBusiness]
    B -->|No| D[Get AccountMetadata]
    D --> E[Derive LOB from accountType]
    E --> C

    C --> F[STEP 1: Query Templates]
    F --> G["findActiveTemplatesByLineOfBusiness()<br/>WHERE line_of_business = :lob<br/>OR line_of_business = 'ENTERPRISE'"]

    G --> H[Templates filtered by LOB]
    H --> I[STEP 2: For each Template]

    I --> J{Check sharing_scope}

    J -->|NULL| K[Account-specific docs]
    J -->|ALL| L[Shared docs - no check]
    J -->|ACCOUNT_TYPE| M[Check accountType rule]
    J -->|CUSTOM_RULES| N[Full eligibility evaluation]

    K --> O[Aggregate results]
    L --> O
    M --> O
    N --> O

    O --> P[Return response]

    style F fill:#fff3cd
    style G fill:#fff3cd
    style I fill:#cfe2ff
    style J fill:#cfe2ff
```

---

## 2. Data Extraction Chain Flow

This diagram details how the system **loops through each shared document template** and uses the `ConfigurableDataExtractionService` to process multi-step API chains for extracting fields needed for eligibility evaluation and document matching.

### 2.1 Template Looping Overview

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Service as DocumentEnquiryService
    participant TemplateRepo as TemplateRepository
    participant ExtractSvc as DataExtractionService
    participant RuleSvc as RuleEvaluationService
    participant StorageRepo as StorageIndexRepository

    Client->>Service: getDocuments(accountId, customerId)

    Service->>TemplateRepo: findSharedTemplates(communicationType)
    TemplateRepo-->>Service: List<Template> [T1, T2, T3]
    Note over Service: Found 3 shared document templates:<br/>T1: Cardholder Agreement (CUSTOM_RULES)<br/>T2: Privacy Statement (ALL)<br/>T3: Credit Card Offer (CUSTOM_RULES)

    rect rgb(255, 250, 240)
        Note over Service: LOOP: For each Template

        loop Template T1: Cardholder Agreement
            Service->>Service: Get sharing_scope = CUSTOM_RULES
            Service->>Service: Get data_extraction_config
            Note over Service: Template T1 needs: disclosureCode<br/>via accountArrangements → pricing API chain

            Service->>ExtractSvc: extractFields(T1.config, context)
            Note over ExtractSvc: Execute API chain for T1...<br/>(see detailed flow below)
            ExtractSvc-->>Service: {disclosureCode: "D164"}

            Service->>RuleSvc: evaluate(T1.eligibility, context)
            RuleSvc-->>Service: eligible = true

            Service->>StorageRepo: findByReferenceKey("D164", T1.templateId)
            StorageRepo-->>Service: [Cardholder_Agreement_D164.pdf]
        end

        loop Template T2: Privacy Statement
            Service->>Service: Get sharing_scope = ALL
            Note over Service: No extraction needed for ALL scope
            Service->>StorageRepo: findSharedDocs(T2.templateId)
            StorageRepo-->>Service: [Privacy_Statement_2024.pdf]
        end

        loop Template T3: Credit Card Offer
            Service->>Service: Get sharing_scope = CUSTOM_RULES
            Service->>Service: Get data_extraction_config
            Note over Service: Template T3 needs: creditLimit<br/>via credit info API

            Service->>ExtractSvc: extractFields(T3.config, context)
            Note over ExtractSvc: Execute API chain for T3...
            ExtractSvc-->>Service: {creditLimit: 35000}

            Service->>RuleSvc: evaluate(T3.eligibility, context)
            Note over RuleSvc: creditLimit >= 25000? YES
            RuleSvc-->>Service: eligible = true

            Note over Service: Apply conditional matching:<br/>35000 >= 50000? No (not Platinum)<br/>35000 >= 25000? Yes → GOLD
            Service->>StorageRepo: findByReferenceKey("GOLD", T3.templateId)
            StorageRepo-->>Service: [Gold_Card_Offer.pdf]
        end
    end

    Service->>Service: Aggregate results from all templates
    Service-->>Client: [Cardholder_Agreement_D164.pdf,<br/>Privacy_Statement_2024.pdf,<br/>Gold_Card_Offer.pdf]
```

### 2.2 Detailed API Chain Extraction (Per Template)

This diagram shows the detailed extraction process that happens **for each template** during the loop above.

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

    Note over Service,JSONPath: Called once per template with CUSTOM_RULES scope

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
    Note over Service: Continue to rule evaluation for this template...
```

### 2.3 Looping Logic Pseudocode

```java
// DocumentEnquiryService.getDocuments()
public Flux<Document> getDocuments(DocumentEnquiryRequest request) {

    // 1. Load all active shared document templates
    List<Template> sharedTemplates = templateRepository
        .findActiveSharedTemplates(request.getCommunicationType());

    List<Document> allDocuments = new ArrayList<>();

    // 2. LOOP through each template
    for (Template template : sharedTemplates) {

        // Build initial context with request data
        Map<String, Object> context = buildContext(request);

        // 3. Check sharing scope and process accordingly
        switch (template.getSharingScope()) {

            case NULL:
                // Account-specific: query directly by accountId
                documents = storageRepo.findByAccountKey(request.getAccountId());
                break;

            case ALL:
                // Shared with everyone: no eligibility check
                documents = storageRepo.findSharedDocs(template.getId());
                break;

            case ACCOUNT_TYPE:
                // Check simple account type rule
                if (ruleService.evaluate(template.getEligibility(), context)) {
                    documents = storageRepo.findSharedDocs(template.getId());
                }
                break;

            case CUSTOM_RULES:
                // 4. Extract external data if configured
                if (template.hasDataExtractionConfig()) {
                    Map<String, Object> extracted = dataExtractionService
                        .extractFields(template.getDataExtractionConfig(), context);
                    context.putAll(extracted);  // Merge extracted fields
                }

                // 5. Evaluate eligibility with merged context
                if (ruleService.evaluate(template.getEligibility(), context)) {

                    // 6. Match documents based on configuration
                    if (template.hasDocumentMatching()) {
                        String refKey = getMatchedReferenceKey(template, context);
                        documents = storageRepo.findByReferenceKey(refKey, template.getId());
                    } else {
                        documents = storageRepo.findSharedDocs(template.getId());
                    }
                }
                break;
        }

        // 7. Filter by validity dates
        documents = filterByValidity(documents, LocalDate.now());

        // 8. Add to aggregated results
        allDocuments.addAll(documents);
    }

    // 9. Deduplicate, sort, and paginate
    return buildResponse(allDocuments, request.getPagination());
}
```

### 2.4 Example 2: Credit Tier Offer Selection

This example shows a different scenario - selecting a credit card offer based on the customer's credit limit tier (Platinum/Gold/Standard).

```mermaid
sequenceDiagram
    autonumber
    participant Service as DocumentEnquiryService
    participant ExtractSvc as DataExtractionService
    participant CreditAPI as Credit Info API
    participant RuleSvc as RuleEvaluationService
    participant StorageRepo as StorageIndexRepository

    Note over Service: Processing Template: "Credit Card Offer"<br/>sharing_scope = CUSTOM_RULES

    Service->>Service: Build initial context
    Note over Service: context = {<br/>  accountId: "ACC-001",<br/>  customerId: "CUST-123"<br/>}

    rect rgb(230, 255, 230)
        Note over Service,CreditAPI: STEP 1: Extract creditLimit from Credit Info API

        Service->>ExtractSvc: extractFields(config, context)

        ExtractSvc->>ExtractSvc: Resolve URL template
        Note over ExtractSvc: /credit-info/${accountId}<br/>→ /credit-info/ACC-001

        ExtractSvc->>CreditAPI: GET /credit-info/ACC-001
        CreditAPI-->>ExtractSvc: {"creditLimit": 35000, "creditScore": 720}

        ExtractSvc->>ExtractSvc: Extract via JSONPath: $.creditLimit
        ExtractSvc->>ExtractSvc: context.put("creditLimit", 35000)

        ExtractSvc-->>Service: {creditLimit: 35000}
    end

    Service->>Service: Merge extracted fields into context
    Note over Service: context = {<br/>  accountId: "ACC-001",<br/>  customerId: "CUST-123",<br/>  creditLimit: 35000<br/>}

    rect rgb(255, 255, 230)
        Note over Service,RuleSvc: STEP 2: Evaluate Eligibility

        Service->>RuleSvc: evaluate(eligibility_criteria, context)
        Note over RuleSvc: Rule: creditLimit >= 10000
        RuleSvc->>RuleSvc: 35000 >= 10000? YES
        RuleSvc-->>Service: eligible = true
    end

    rect rgb(230, 240, 255)
        Note over Service,StorageRepo: STEP 3: Conditional Document Matching

        Service->>Service: Apply conditional matching rules
        Note over Service: Conditions (evaluated in order):<br/>1. creditLimit >= 50000 → PLATINUM<br/>2. creditLimit >= 25000 → GOLD<br/>3. creditLimit >= 10000 → STANDARD

        Service->>Service: Evaluate condition 1
        Note over Service: 35000 >= 50000? NO

        Service->>Service: Evaluate condition 2
        Note over Service: 35000 >= 25000? YES → Use "GOLD"

        Service->>StorageRepo: findByReferenceKey("GOLD", "BALANCE_TIER", templateId)
        StorageRepo-->>Service: [Gold_Card_Offer_2024.pdf]
    end

    Service->>Service: Filter by validity dates
    Note over Service: valid_from: 2024-01-01<br/>valid_until: 2024-12-31<br/>Today: 2024-12-09 ✓ Valid

    Service-->>Service: Return [Gold_Card_Offer_2024.pdf]
```

### 2.5 Example 3: Privacy Statement with Region-Based Rules

This example shows eligibility based on customer region without external API calls (static rules).

```mermaid
sequenceDiagram
    autonumber
    participant Service as DocumentEnquiryService
    participant RuleSvc as RuleEvaluationService
    participant StorageRepo as StorageIndexRepository

    Note over Service: Processing Template: "Regional Privacy Statement"<br/>sharing_scope = CUSTOM_RULES<br/>No data_extraction_config (uses request data only)

    Service->>Service: Build context from request
    Note over Service: context = {<br/>  accountId: "ACC-001",<br/>  customerId: "CUST-123",<br/>  state: "CA",<br/>  accountType: "CREDIT_CARD"<br/>}

    rect rgb(255, 240, 245)
        Note over Service,RuleSvc: Evaluate Eligibility (No Extraction Needed)

        Service->>RuleSvc: evaluate(eligibility_criteria, context)
        Note over RuleSvc: Rule (OR):<br/>  - state IN ["CA", "NY", "TX"]<br/>  - accountType = "PREMIUM"

        RuleSvc->>RuleSvc: Evaluate rule 1: state IN ["CA", "NY", "TX"]
        Note over RuleSvc: "CA" IN ["CA", "NY", "TX"]? YES

        RuleSvc-->>Service: eligible = true (short-circuit on first OR match)
    end

    rect rgb(240, 255, 240)
        Note over Service,StorageRepo: Query Documents (No Reference Key Matching)

        Service->>StorageRepo: findSharedDocs(templateId)
        Note over StorageRepo: No documentMatching config,<br/>return all shared docs for template

        StorageRepo-->>Service: [Privacy_Statement_CA_2024.pdf,<br/>Privacy_Statement_General_2024.pdf]
    end

    Service->>Service: Filter by validity dates
    Service-->>Service: Return documents
```

### 2.6 Key Points About the Loop

| Aspect | Description |
|--------|-------------|
| **What triggers the loop** | A document inquiry request with one or more accountIds |
| **What is looped over** | Each active shared document template |
| **When extraction runs** | Only for templates with `sharing_scope = CUSTOM_RULES` AND `data_extraction_config` is present |
| **Context accumulation** | Each template starts with initial request context; extracted fields are merged in |
| **Document aggregation** | Results from all templates are collected, deduplicated, and returned together |
| **Performance consideration** | API calls are made per-template; consider caching common API responses |

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
