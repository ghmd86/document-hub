# Account Update Email Notification Flow

## Overview

This document illustrates the complete flow when an account update event triggers an email notification. The flow involves multiple services working together to send a personalized email via Salesforce Email Service and store the record in Document Hub.

---

## Components

| Component | Type | Description |
|-----------|------|-------------|
| **Account Service** | Upstream Service | Manages account data, triggers events on updates |
| **Kafka** | Message Broker | Event streaming between services |
| **Email Notification Service** | Core Service | Orchestrates email sending workflow |
| **Template Management Service** | Core Service | Provides template config and vendor mappings |
| **Customer API** | Data Service | Customer contact information |
| **Salesforce Email Service** | Vendor Service | Sends transactional emails |
| **Document Hub API** | Core Service | Stores email delivery records |
| **PostgreSQL** | Database | Persistent storage |

---

## 1. Complete End-to-End Flow

```mermaid
sequenceDiagram
    autonumber
    participant AcctSvc as Account Service
    participant Kafka as Kafka
    participant EmailSvc as Email Notification<br/>Service
    participant TMS as Template Management<br/>Service
    participant CustAPI as Customer API
    participant SFDC as Salesforce<br/>Email Service
    participant DocHub as Document Hub API
    participant DB as PostgreSQL

    rect rgb(255, 248, 220)
        Note over AcctSvc,Kafka: Phase 1: Account Update Event
        AcctSvc->>Kafka: AccountUpdatedEvent<br/>{accountId, customerId, updateType, changes}
        Note right of Kafka: Topic: account-events
    end

    rect rgb(220, 240, 255)
        Note over Kafka,EmailSvc: Phase 2: Email Service Consumes Event
        Kafka->>+EmailSvc: AccountUpdatedEvent
        Note over EmailSvc: Determine notification type<br/>based on updateType
    end

    rect rgb(240, 255, 240)
        Note over EmailSvc,TMS: Phase 3: Get Template & Vendor Config
        EmailSvc->>+TMS: GET /templates?templateType=ACCOUNT_UPDATE_EMAIL<br/>&lineOfBusiness={lob}
        TMS->>+DB: SELECT template WHERE type='ACCOUNT_UPDATE_EMAIL'
        DB-->>-TMS: Template config
        TMS-->>-EmailSvc: Template {templateId, templateVersion}

        EmailSvc->>+TMS: GET /templates/vendors?templateId={id}<br/>&vendor=Salesforce&vendorType=EMAIL
        TMS->>+DB: SELECT vendor_mapping WHERE vendor='Salesforce'
        DB-->>-TMS: Vendor mapping config
        TMS-->>-EmailSvc: VendorConfig {vendorTemplateKey, apiConfig}
    end

    rect rgb(255, 240, 255)
        Note over EmailSvc,CustAPI: Phase 4: Fetch Customer Data
        EmailSvc->>+CustAPI: GET /customers/{customerId}
        CustAPI-->>-EmailSvc: Customer {name, email, preferences}

        Note over EmailSvc: Validate email opt-in status
        alt Customer opted out of emails
            EmailSvc->>Kafka: NotificationSkippedEvent
            EmailSvc-->>Kafka: ACK (end flow)
        end
    end

    rect rgb(255, 248, 220)
        Note over EmailSvc,SFDC: Phase 5: Send Email via Salesforce
        EmailSvc->>EmailSvc: Build email payload<br/>{recipient, templateKey, mergeFields}

        EmailSvc->>+SFDC: POST /services/data/v58.0/actions/standard/emailSimple<br/>{templateId, recipientEmail, mergeFields}

        Note over SFDC: Salesforce processes:<br/>1. Load email template<br/>2. Merge dynamic fields<br/>3. Send email

        SFDC-->>-EmailSvc: 200 OK {messageId, status: "SENT"}
    end

    rect rgb(230, 255, 230)
        Note over EmailSvc,DocHub: Phase 6: Store Record in Document Hub
        EmailSvc->>+DocHub: POST /documents<br/>{templateId, customerId, accountId,<br/>deliveryChannel: EMAIL, metadata}

        DocHub->>+DB: INSERT storage_index<br/>(email delivery record)
        DB-->>-DocHub: Document record created

        DocHub-->>-EmailSvc: 201 Created {documentId}
    end

    rect rgb(240, 240, 255)
        Note over EmailSvc,Kafka: Phase 7: Publish Completion Event
        EmailSvc->>Kafka: EmailNotificationSentEvent<br/>{documentId, messageId, status}
        Note right of Kafka: Topic: notification-events
    end

    EmailSvc-->>-Kafka: ACK
```

---

## 2. Detailed Template & Vendor Lookup

```mermaid
sequenceDiagram
    autonumber
    participant EmailSvc as Email Notification<br/>Service
    participant TMS as Template Management<br/>Service
    participant DB as PostgreSQL

    EmailSvc->>+TMS: GET /templates<br/>?templateType=ACCOUNT_UPDATE_EMAIL<br/>&lineOfBusiness=CREDIT_CARD<br/>&activeFlag=true

    TMS->>+DB: SELECT * FROM master_template_definition<br/>WHERE template_type = 'ACCOUNT_UPDATE_EMAIL'<br/>AND line_of_business = 'CREDIT_CARD'<br/>AND active = true<br/>AND start_date <= NOW()<br/>AND (end_date IS NULL OR end_date > NOW())
    DB-->>-TMS: Template record

    TMS-->>-EmailSvc: TemplateResponse
    Note right of EmailSvc: {<br/>  templateId: "uuid",<br/>  templateVersion: 2,<br/>  displayName: "Account Update Notification",<br/>  templateVariables: [...],<br/>  requiredFields: [...],<br/>  channels: {EMAIL: {enabled: true}}<br/>}

    EmailSvc->>+TMS: GET /templates/vendors<br/>?templateId={templateId}<br/>&templateVersion=2<br/>&vendor=Salesforce<br/>&isActive=true<br/>&isPrimary=true

    TMS->>+DB: SELECT * FROM template_vendor_mapping<br/>WHERE template_id = {templateId}<br/>AND vendor = 'Salesforce'<br/>AND active = true
    DB-->>-TMS: Vendor mapping record

    TMS-->>-EmailSvc: VendorMappingResponse
    Note right of EmailSvc: {<br/>  vendorId: "uuid",<br/>  vendor: "Salesforce",<br/>  vendorType: "EMAIL",<br/>  vendorTemplateKey: "00X5f000000XXXX",<br/>  apiConfig: {<br/>    baseUrl: "https://company.my.salesforce.com",<br/>    authType: "OAUTH2"<br/>  }<br/>}
```

---

## 3. Salesforce Email Service Integration

```mermaid
sequenceDiagram
    autonumber
    participant EmailSvc as Email Notification<br/>Service
    participant AuthSvc as Auth Service
    participant SFDC as Salesforce<br/>Email Service
    participant Kafka as Kafka

    rect rgb(255, 248, 220)
        Note over EmailSvc,AuthSvc: Authenticate with Salesforce
        EmailSvc->>+AuthSvc: Get Salesforce OAuth Token
        AuthSvc-->>-EmailSvc: Access Token (cached)
    end

    rect rgb(240, 255, 240)
        Note over EmailSvc,SFDC: Prepare & Send Email
        EmailSvc->>EmailSvc: Build request payload

        Note over EmailSvc: Request Payload:<br/>{<br/>  "inputs": [{<br/>    "emailTemplateId": "00X5f000000XXXX",<br/>    "recipientId": "003...",<br/>    "targetObjectId": "001...",<br/>    "whatId": "a]...",<br/>    "emailAddresses": "customer@email.com"<br/>  }]<br/>}

        EmailSvc->>+SFDC: POST /services/data/v58.0/actions/standard/emailSimple
        Note over SFDC: Headers:<br/>Authorization: Bearer {token}<br/>Content-Type: application/json
    end

    alt Success
        SFDC-->>EmailSvc: 200 OK
        Note right of SFDC: {<br/>  "actionName": "emailSimple",<br/>  "isSuccess": true,<br/>  "outputValues": {<br/>    "messageId": "msg_xxx"<br/>  }<br/>}
        EmailSvc->>Kafka: EmailSentEvent (SUCCESS)
    else Rate Limited
        SFDC-->>EmailSvc: 429 Too Many Requests
        EmailSvc->>Kafka: EmailRetryScheduledEvent
        Note over EmailSvc: Schedule retry with backoff
    else Failed
        SFDC-->>-EmailSvc: 400/500 Error
        EmailSvc->>Kafka: EmailFailedEvent
        Note over EmailSvc: Log error, alert team
    end
```

---

## 4. Document Hub Storage

```mermaid
sequenceDiagram
    autonumber
    participant EmailSvc as Email Notification<br/>Service
    participant DocHub as Document Hub API
    participant DB as PostgreSQL
    participant Kafka as Kafka

    EmailSvc->>+DocHub: POST /documents
    Note right of EmailSvc: Request Body:<br/>{<br/>  "templateId": "uuid",<br/>  "templateVersion": 2,<br/>  "customerId": "uuid",<br/>  "accountId": "uuid",<br/>  "referenceKey": "msg_salesforce_xxx",<br/>  "referenceKeyType": "EMAIL_MESSAGE_ID",<br/>  "deliveryChannel": "EMAIL",<br/>  "docMetadata": {<br/>    "recipientEmail": "customer@email.com",<br/>    "subject": "Account Update",<br/>    "sentAt": 1704067200000,<br/>    "vendor": "Salesforce",<br/>    "triggerEvent": "ACCOUNT_UPDATED"<br/>  }<br/>}

    DocHub->>DocHub: Validate request

    DocHub->>+DB: INSERT INTO storage_index<br/>(template_id, customer_id, account_id,<br/>reference_key, delivery_channel, doc_metadata)
    DB-->>-DocHub: Record created

    DocHub->>Kafka: DocumentCreatedEvent

    DocHub-->>-EmailSvc: 201 Created
    Note right of DocHub: Response:<br/>{<br/>  "documentId": "uuid",<br/>  "storageIndexId": "uuid",<br/>  "createdAt": 1704067200000<br/>}
```

---

## 5. Error Handling & Retry Flow

```mermaid
sequenceDiagram
    autonumber
    participant EmailSvc as Email Notification<br/>Service
    participant TMS as Template Management<br/>Service
    participant SFDC as Salesforce<br/>(Primary)
    participant SendGrid as SendGrid<br/>(Fallback)
    participant DocHub as Document Hub
    participant Kafka as Kafka
    participant DLQ as Dead Letter Queue

    EmailSvc->>+TMS: Get vendor mappings
    TMS-->>-EmailSvc: [Salesforce (primary), SendGrid (fallback)]

    rect rgb(255, 220, 220)
        Note over EmailSvc,SFDC: Primary Vendor Fails
        EmailSvc->>+SFDC: Send email
        SFDC-->>-EmailSvc: 503 Service Unavailable
        EmailSvc->>Kafka: VendorFailureEvent (Salesforce)
    end

    rect rgb(255, 255, 200)
        Note over EmailSvc: Retry Logic
        loop Retry 3 times with exponential backoff
            EmailSvc->>+SFDC: Retry send
            SFDC-->>-EmailSvc: 503 Still failing
        end
    end

    rect rgb(220, 255, 220)
        Note over EmailSvc,SendGrid: Failover to Backup Vendor
        EmailSvc->>+SendGrid: Send email (fallback)
        SendGrid-->>-EmailSvc: 200 OK {messageId}
        EmailSvc->>Kafka: EmailSentEvent (vendor=SendGrid, failover=true)
    end

    EmailSvc->>+DocHub: Store delivery record
    DocHub-->>-EmailSvc: 201 Created

    alt All Vendors Failed
        EmailSvc->>DLQ: Push to Dead Letter Queue
        EmailSvc->>Kafka: NotificationFailedEvent
        Note over DLQ: Manual intervention required
    end
```

---

## 6. Complete System Architecture

```mermaid
flowchart TB
    subgraph Upstream["Upstream Services"]
        AcctSvc[Account Service]
        LoanSvc[Loan Service]
        CardSvc[Card Service]
    end

    subgraph EventBus["Event Bus (Kafka)"]
        AcctEvents[account-events]
        NotifEvents[notification-events]
        DocEvents[document-events]
    end

    subgraph NotificationLayer["Notification Layer"]
        EmailSvc[Email Notification Service]
        SMSSvc[SMS Notification Service]
        PushSvc[Push Notification Service]
    end

    subgraph CoreServices["Core Services"]
        TMS[Template Management<br/>Service]
        DocHub[Document Hub API]
    end

    subgraph DataServices["Data Services"]
        CustAPI[Customer API]
        AcctAPI[Account API]
    end

    subgraph Vendors["Email Vendors"]
        SFDC[Salesforce<br/>Email Service]
        SendGrid[SendGrid<br/>Fallback]
    end

    subgraph Storage["Storage"]
        DB[(PostgreSQL)]
    end

    AcctSvc --> AcctEvents
    LoanSvc --> AcctEvents
    CardSvc --> AcctEvents

    AcctEvents --> EmailSvc
    AcctEvents --> SMSSvc
    AcctEvents --> PushSvc

    EmailSvc --> TMS
    EmailSvc --> CustAPI
    EmailSvc --> SFDC
    EmailSvc --> SendGrid
    EmailSvc --> DocHub
    EmailSvc --> NotifEvents

    TMS --> DB
    DocHub --> DB
    DocHub --> DocEvents

    style EmailSvc fill:#4CAF50,color:#fff
    style TMS fill:#2196F3,color:#fff
    style DocHub fill:#2196F3,color:#fff
    style SFDC fill:#00A1E0,color:#fff
    style AcctEvents fill:#FF9800,color:#fff
```

---

## 7. Event Schemas

### AccountUpdatedEvent (Input)

```json
{
  "eventId": "uuid",
  "eventType": "ACCOUNT_UPDATED",
  "timestamp": 1704067200000,
  "correlationId": "uuid",
  "payload": {
    "accountId": "uuid",
    "customerId": "uuid",
    "updateType": "ADDRESS_CHANGE",
    "lineOfBusiness": "CREDIT_CARD",
    "changes": {
      "previousAddress": {
        "street": "123 Old St",
        "city": "Old City"
      },
      "newAddress": {
        "street": "456 New Ave",
        "city": "New City"
      }
    },
    "updatedBy": "CUSTOMER",
    "updatedAt": 1704067200000
  }
}
```

### EmailNotificationSentEvent (Output)

```json
{
  "eventId": "uuid",
  "eventType": "EMAIL_NOTIFICATION_SENT",
  "timestamp": 1704067201000,
  "correlationId": "uuid",
  "payload": {
    "notificationId": "uuid",
    "documentId": "uuid",
    "templateId": "uuid",
    "templateVersion": 2,
    "customerId": "uuid",
    "accountId": "uuid",
    "channel": "EMAIL",
    "vendor": "Salesforce",
    "vendorMessageId": "msg_salesforce_xxx",
    "recipientEmail": "customer@email.com",
    "subject": "Your Account Has Been Updated",
    "status": "SENT",
    "triggerEvent": "ACCOUNT_UPDATED",
    "sentAt": 1704067201000,
    "failover": false
  }
}
```

---

## 8. Database Records

### Template Management Service - Template Record

```sql
-- master_template_definition
{
  template_id: 'uuid',
  template_version: 2,
  template_type: 'ACCOUNT_UPDATE_EMAIL',
  display_name: 'Account Update Notification',
  line_of_business: 'CREDIT_CARD',
  communication_type: 'TRANSACTIONAL',
  active: true,
  template_variables: [
    {name: 'customerName', type: 'STRING', source: 'CUSTOMER_API'},
    {name: 'accountLast4', type: 'STRING', source: 'ACCOUNT_API'},
    {name: 'updateType', type: 'STRING', source: 'EVENT_PAYLOAD'},
    {name: 'updateDate', type: 'DATE', source: 'EVENT_PAYLOAD'}
  ]
}
```

### Template Management Service - Vendor Mapping

```sql
-- template_vendor_mapping
{
  vendor_mapping_id: 'uuid',
  template_id: 'uuid',
  template_version: 2,
  vendor: 'Salesforce',
  vendor_type: 'EMAIL',
  vendor_template_key: '00X5f000000XXXX',
  primary: true,
  active: true,
  api_config: {
    base_url: 'https://company.my.salesforce.com',
    auth_type: 'OAUTH2',
    timeout_ms: 30000
  }
}
```

### Document Hub - Storage Index Record

```sql
-- storage_index
{
  storage_index_id: 'uuid',
  master_template_id: 'uuid',
  template_version: 2,
  customer_key: 'uuid',
  account_key: 'uuid',
  reference_key: 'msg_salesforce_xxx',
  reference_key_type: 'EMAIL_MESSAGE_ID',
  delivery_channel: 'EMAIL',
  doc_creation_date: 1704067201000,
  accessible_flag: true,
  doc_metadata: {
    recipientEmail: 'customer@email.com',
    subject: 'Your Account Has Been Updated',
    vendor: 'Salesforce',
    triggerEvent: 'ACCOUNT_UPDATED',
    status: 'SENT'
  }
}
```

---

## 9. Sequence Summary

| Step | Service | Action |
|------|---------|--------|
| 1 | Account Service | Publishes `AccountUpdatedEvent` to Kafka |
| 2 | Email Notification Service | Consumes event, determines notification needed |
| 3 | Email Notification Service | Calls Template Management Service for template config |
| 4 | Email Notification Service | Calls Template Management Service for Salesforce vendor mapping |
| 5 | Email Notification Service | Calls Customer API for customer email and preferences |
| 6 | Email Notification Service | Validates customer has not opted out |
| 7 | Email Notification Service | Sends email via Salesforce Email Service |
| 8 | Email Notification Service | Stores delivery record in Document Hub |
| 9 | Email Notification Service | Publishes `EmailNotificationSentEvent` to Kafka |

---

## View These Diagrams

1. **GitHub/GitLab**: Mermaid diagrams render automatically
2. **VS Code**: Install "Markdown Preview Mermaid Support" extension
3. **Online**: Use [Mermaid Live Editor](https://mermaid.live)
