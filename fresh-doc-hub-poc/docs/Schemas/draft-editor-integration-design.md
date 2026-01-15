# Draft Editor Integration - Design Document

## 1. Jira Story

### Story Title
**Draft Editor - Template Field Population from Customer Data APIs**

### Epic
Template Management Service Enhancement

### Story Description
```
AS A Draft Editor user
I WANT TO select a document type and have customer data automatically fetched
SO THAT I can generate letters with pre-populated template fields

BACKGROUND:
- Draft editor needs to search document types for a specific vendor
- Once a document type is selected, the system should fetch customer information from appropriate APIs
- The response should populate the template fields as defined in the XML schema (schema_core_v12.xsd)
- Different templates require different sets of variables (PR024, PR027, RF002, etc.)
```

### Acceptance Criteria
```gherkin
GIVEN I am a draft editor user
WHEN I search for document types for vendor "SmartComm"
THEN I should see a list of available templates for that vendor

GIVEN I have selected template "PR027"
AND I provide customer identifier (e.g., account number, case number)
WHEN I request template field population
THEN the system should:
  - Fetch data from configured APIs (Credit Service, Case Management, etc.)
  - Map the API responses to template variables
  - Return XML-compatible data matching the PR027 schema structure

GIVEN the template requires data from multiple APIs
WHEN fetching customer data
THEN all API calls should be made in parallel where possible
AND failures should be gracefully handled with fallback values
```

### Technical Tasks
1. Design `template_variables` JSON structure in `master_template_definition`
2. Design `response_mapping_config` JSON structure in `template_vendor_mapping`
3. Implement `GET /templates/vendors/{vendorCode}/document-types` endpoint
4. Implement `POST /templates/{templateId}/populate` endpoint
5. Create API integration service for data fetching
6. Create response mapper for XML schema generation

### Story Points: 8

---

## 2. System Architecture

### High-Level Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DRAFT EDITOR FLOW                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────┐     ┌─────────────────────────────────┐
│ Draft Editor │────►│ Template Management │────►│ master_template_definition      │
│     UI       │     │      Service        │     │ - template_variables (JSON)     │
└──────────────┘     └─────────────────────┘     │ - data_extraction_config (JSON) │
      │                       │                  └─────────────────────────────────┘
      │                       │                              │
      │                       ▼                              ▼
      │              ┌─────────────────────┐     ┌─────────────────────────────────┐
      │              │ template_vendor_    │     │ External APIs                   │
      │              │ mapping             │     │ - Credit Service API            │
      │              │ - template_fields   │     │ - Case Management API           │
      │              │ - api_config        │     │ - Account Service API           │
      │              │ - response_mapping  │     │ - Customer Profile API          │
      │              └─────────────────────┘     └─────────────────────────────────┘
      │                       │                              │
      │                       ▼                              ▼
      │              ┌─────────────────────────────────────────────────────────────┐
      │              │               Response Mapper                                │
      │              │  (Maps API responses to XML schema structure)               │
      │              └─────────────────────────────────────────────────────────────┘
      │                                      │
      ▼                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          XML Response (schema_core_v12.xsd)                      │
│  <data>                                                                          │
│    <template>...</template>                                                      │
│    <recipient>...</recipient>                                                    │
│    <entity>...</entity>                                                          │
│    <agency>...</agency>                                                          │
│  </data>                                                                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Sequence Diagram

```
┌──────────┐       ┌─────────────┐       ┌──────────────┐       ┌─────────────┐
│  Draft   │       │  Template   │       │   Database   │       │  External   │
│  Editor  │       │  Management │       │   (D-HUB)    │       │    APIs     │
└────┬─────┘       └──────┬──────┘       └──────┬───────┘       └──────┬──────┘
     │                    │                     │                      │
     │ 1. GET /templates/vendors/SMARTCOMM/document-types             │
     │───────────────────►│                     │                      │
     │                    │ 2. Query vendor     │                      │
     │                    │    mappings         │                      │
     │                    │────────────────────►│                      │
     │                    │◄────────────────────│                      │
     │◄───────────────────│                     │                      │
     │   [List of templates]                    │                      │
     │                    │                     │                      │
     │ 3. POST /templates/PR027/populate        │                      │
     │    { "caseNumber": "123454", ... }       │                      │
     │───────────────────►│                     │                      │
     │                    │ 4. Get template     │                      │
     │                    │    config           │                      │
     │                    │────────────────────►│                      │
     │                    │◄────────────────────│                      │
     │                    │  [template_variables, api_config]          │
     │                    │                     │                      │
     │                    │ 5. Parallel API calls                      │
     │                    │────────────────────────────────────────────►
     │                    │   GET /credit-service/account/{accountNo}  │
     │                    │   GET /case-mgmt/cases/{caseNumber}        │
     │                    │   GET /customer-profile/{customerId}       │
     │                    │◄────────────────────────────────────────────
     │                    │                     │                      │
     │                    │ 6. Map responses    │                      │
     │                    │    to XML schema    │                      │
     │◄───────────────────│                     │                      │
     │   [XML Response]   │                     │                      │
```

---

## 3. Data Model Design

### 3.1 `template_variables` (in master_template_definition)

This JSON column stores what variables a template requires and **where each value comes from**.

#### Current Assentis Platform Reality

In the current Assentis platform, only **3 fields** are fetched via API using either `accountId` OR `applicationId`:
- **Recipient Address** (name, addressLine1, addressLine2, city, state, zipcode)
- **Last 4 Digits** of account
- **Total Balance**

**All other fields are manually entered by the draft editor user.**

#### Lookup Key Options

| Lookup Key | Use Case |
|------------|----------|
| `accountId` | For existing accounts - fetches customer info from account records |
| `applicationId` | For new applications - fetches applicant info from application records |

Only ONE of these is required per request.

#### Field Source Types

| Source Type | Description | Example Fields |
|-------------|-------------|----------------|
| `API` | Fetched from Account API using accountId | recipient.*, last4digit, totalBalance |
| `MANUAL` | Entered by draft editor user | Everything else (employeeId, caseNumber, dates, amounts, bullets, etc.) |
| `SYSTEM` | Auto-generated by the system | date, letterReferenceNumber |
| `CONFIG` | From template/vendor configuration | resourceId, templateId, displayName |
| `DERIVED` | Calculated from other fields | totalCredit (sum of fee credits) |

#### Schema Structure (Reflecting Current Reality)

```json
{
  "schemaVersion": "1.0",
  "templateId": "PR027",
  "description": "DV Solicited - CO Optional - Goodwill Approved Declined",

  "lookupKeys": [
    { "name": "accountId", "type": "STRING", "required": false, "description": "Account ID for existing accounts" },
    { "name": "applicationId", "type": "STRING", "required": false, "description": "Application ID for new applications" }
  ],
  "lookupKeyValidation": {
    "rule": "ONE_OF_REQUIRED",
    "message": "Either accountId or applicationId must be provided"
  },

  "variableGroups": [
    {
      "groupName": "template",
      "description": "Template metadata",
      "variables": [
        { "name": "resourceId", "type": "STRING", "required": true, "source": "CONFIG", "configKey": "vendor.resourceId" },
        { "name": "templateId", "type": "STRING", "required": true, "source": "CONFIG", "configKey": "template.id" },
        { "name": "letterReferenceNumber", "type": "STRING", "required": true, "source": "SYSTEM", "generator": "LETTER_REF_SEQUENCE" },
        { "name": "employeeId", "type": "STRING", "required": true, "source": "MANUAL", "inputType": "TEXT", "label": "Employee ID" },
        { "name": "displayName", "type": "STRING", "required": true, "source": "CONFIG", "configKey": "template.displayName" },
        { "name": "date", "type": "DATE", "required": true, "source": "SYSTEM", "generator": "CURRENT_DATE", "format": "yyyy-MM-dd" }
      ]
    },
    {
      "groupName": "recipient",
      "description": "Customer recipient information - FETCHED FROM API",
      "variables": [
        { "name": "name", "type": "STRING", "required": true, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "customerName" },
        { "name": "addressLine1", "type": "STRING", "required": true, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "address.line1" },
        { "name": "addressLine2", "type": "STRING", "required": false, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "address.line2" },
        { "name": "city", "type": "STRING", "required": true, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "address.city" },
        { "name": "state", "type": "STRING", "required": true, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "address.state" },
        { "name": "zipcode", "type": "STRING", "required": true, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "address.zipCode" }
      ]
    },
    {
      "groupName": "entity",
      "description": "Case and account information",
      "variables": [
        { "name": "last4digit", "type": "STRING", "required": true, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "accountLast4" },
        { "name": "caseNumber", "type": "STRING", "required": true, "source": "MANUAL", "inputType": "TEXT", "label": "Case Number" },
        { "name": "agencyOrExec", "type": "ENUM", "required": true, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Agency or Exec",
          "allowedValues": ["BBB", "CFPB/OCC", "CFPB", "Exec", "OCC", "OGA"] },
        { "name": "onOrDated", "type": "ENUM", "required": true, "source": "MANUAL", "inputType": "DROPDOWN", "label": "On or Dated",
          "allowedValues": ["on", "dated"], "defaultValue": "on" },
        { "name": "communicationReceivedDate", "type": "DATE", "required": true, "source": "MANUAL", "inputType": "DATE_PICKER", "label": "Communication Received Date" },
        { "name": "otherGovtAgency", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Other Govt Agency Name",
          "conditionalDisplay": { "field": "agencyOrExec", "value": "OGA" } }
      ]
    },
    {
      "groupName": "primaryAccount",
      "description": "Primary account details",
      "variables": [
        { "name": "totalBalance", "type": "DECIMAL", "required": false, "source": "API", "apiSource": "ACCOUNT_API", "apiField": "currentBalance" },
        { "name": "reservationNumber", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Reservation Number" },
        { "name": "solicitationName", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Solicitation Name" },
        { "name": "solicitationDate", "type": "DATE", "required": false, "source": "MANUAL", "inputType": "DATE_PICKER", "label": "Solicitation Mail Date" },
        { "name": "fulfillment", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Fulfillment Type",
          "allowedValues": ["automated phone system", "online", "mail", "in-person"] },
        { "name": "accountOpenDate", "type": "DATE", "required": false, "source": "MANUAL", "inputType": "DATE_PICKER", "label": "Account Open Date" },
        { "name": "initialCreditLimit", "type": "DECIMAL", "required": false, "source": "MANUAL", "inputType": "NUMBER", "label": "Initial Credit Limit" },
        { "name": "chargeOffDate", "type": "DATE", "required": false, "source": "MANUAL", "inputType": "DATE_PICKER", "label": "Charge Off Date" },
        { "name": "chargeOffBalance", "type": "DECIMAL", "required": false, "source": "MANUAL", "inputType": "NUMBER", "label": "Charge Off Balance" },
        { "name": "chargeOffReason", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Charge Off Reason",
          "allowedValues": ["non-payment for more than 180 days", "bankruptcy", "fraud", "deceased"] },
        { "name": "routingNumber", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Routing Number" },
        { "name": "last4chkAccount", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Last 4 Checking Account" },
        { "name": "last4savAccount", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Last 4 Savings Account" },
        { "name": "recentActivity", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Recent Activity" },
        { "name": "recentActivityAmount", "type": "DECIMAL", "required": false, "source": "MANUAL", "inputType": "NUMBER", "label": "Recent Activity Amount" },
        { "name": "recentActivityDate", "type": "DATE", "required": false, "source": "MANUAL", "inputType": "DATE_PICKER", "label": "Recent Activity Date" },
        { "name": "debtValidationStatus", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Debt Validation Status",
          "allowedValues": ["Valid", "Invalid", "Pending"] },
        { "name": "stmtsSentSeparately", "type": "BOOLEAN", "required": false, "source": "MANUAL", "inputType": "CHECKBOX", "label": "Statements Sent Separately" },
        { "name": "cardAgreement", "type": "BOOLEAN", "required": false, "source": "MANUAL", "inputType": "CHECKBOX", "label": "Card Agreement Enclosed" },
        { "name": "enclosedSeparateCover", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Enclosed Under",
          "allowedValues": ["separate cover", "enclosed herewith"] },
        { "name": "gwCoOptions", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Goodwill CO Options",
          "allowedValues": ["Not reporting", "Reporting as agreed", "Will update"] },
        { "name": "gwDelinquencyDates", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Goodwill Delinquency Dates" }
      ]
    },
    {
      "groupName": "accountHolderDetails",
      "description": "Account holder personal details - ALL MANUAL",
      "variables": [
        { "name": "name", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Applicant Name" },
        { "name": "dateOfBirth", "type": "DATE", "required": false, "source": "MANUAL", "inputType": "DATE_PICKER", "label": "Date of Birth" },
        { "name": "sSNLast4", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "SSN Last 4", "maxLength": 4 },
        { "name": "primaryEmail", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "EMAIL", "label": "Primary Email" },
        { "name": "primaryPhone", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "PHONE", "label": "Primary Phone" },
        { "name": "addressLine1", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Address Line 1" },
        { "name": "addressLine2", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Address Line 2" },
        { "name": "city", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "City" },
        { "name": "state", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "State" },
        { "name": "zipcode", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Zipcode" }
      ]
    },
    {
      "groupName": "agency",
      "description": "Collection agency information - ALL MANUAL",
      "variables": [
        { "name": "agencyName", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Agency Name" },
        { "name": "addressLine1", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Agency Address Line 1" },
        { "name": "addressLine2", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Agency Address Line 2" },
        { "name": "city", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Agency City" },
        { "name": "state", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Agency State" },
        { "name": "zipcode", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "Agency Zipcode" },
        { "name": "phoneNumber", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "PHONE", "label": "Agency Phone" },
        { "name": "faxNumber", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "PHONE", "label": "Agency Fax" }
      ]
    },
    {
      "groupName": "manualSelections",
      "description": "User-selected options and toggles",
      "variables": [
        { "name": "courtesyWe", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Courtesy Phrase",
          "allowedValues": ["As a courtesy, we", "We", "Per your request, we"], "defaultValue": "As a courtesy, we" },
        { "name": "miniMiranda", "type": "BOOLEAN", "required": false, "source": "MANUAL", "inputType": "CHECKBOX", "label": "Include Mini-Miranda", "defaultValue": true },
        { "name": "addBBBCaseNumber", "type": "BOOLEAN", "required": false, "source": "MANUAL", "inputType": "CHECKBOX", "label": "Add BBB Case Number",
          "conditionalDisplay": { "field": "agencyOrExec", "value": "BBB" } },
        { "name": "caseNumberBBB", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "TEXT", "label": "BBB Case Number",
          "conditionalDisplay": { "field": "addBBBCaseNumber", "value": true } },
        { "name": "deptContactName", "type": "STRING", "required": false, "source": "MANUAL", "inputType": "DROPDOWN", "label": "Department Contact",
          "allowedValues": ["Customer Service", "Collections", "Fraud Department", "Executive Office"], "defaultValue": "Customer Service" }
      ]
    },
    {
      "groupName": "showBullets",
      "description": "Selectable bullet points - ALL MANUAL",
      "isArray": true,
      "source": "MANUAL",
      "inputType": "MULTI_SELECT_WITH_DETAILS",
      "variables": [
        { "name": "transactionAuthorized", "type": "BULLET", "label": "Transaction was authorized", "hasDate": false, "hasAmount": false },
        { "name": "merchantRecordChipEnabledCCPresent", "type": "BULLET", "label": "Merchant record shows chip-enabled card present", "hasDate": false, "hasAmount": false },
        { "name": "participatedReleasePII", "type": "BULLET", "label": "Participated in release of PII", "hasDate": true, "hasAmount": false },
        { "name": "unableToContact", "type": "BULLET", "label": "Unable to contact", "hasDate": true, "hasAmount": false },
        { "name": "oneTimePasswordSent", "type": "BULLET", "label": "One-time password sent", "hasDate": true, "hasAmount": false, "hasNotifiedBy": true },
        { "name": "noFraudIdentified", "type": "BULLET", "label": "No fraud identified", "hasDate": true, "hasAmount": false, "hasNotifiedBy": true },
        { "name": "receivedMultipleCalls", "type": "BULLET", "label": "Received multiple calls", "hasDate": true, "hasAmount": false, "hasNotes": true },
        { "name": "thirdPartyPayment", "type": "BULLET", "label": "Third party payment", "hasDate": true, "hasAmount": true },
        { "name": "unauthorizedPayment", "type": "BULLET", "label": "Unauthorized payment", "hasDate": true, "hasAmount": true },
        { "name": "fundsReturned", "type": "BULLET", "label": "Funds returned", "hasDate": false, "hasAmount": true },
        { "name": "annualFeeCredit", "type": "BULLET", "label": "Annual Fee Credit", "hasDate": false, "hasAmount": true },
        { "name": "lateFeeCredit", "type": "BULLET", "label": "Late Fee Credit", "hasDate": false, "hasAmount": true },
        { "name": "totalCredit", "type": "BULLET", "label": "Total Credit", "hasDate": false, "hasAmount": true, "source": "DERIVED", "derivedFrom": "SUM_OF_CREDITS" }
      ]
    },
    {
      "groupName": "otherDetails",
      "description": "Additional custom key-value pairs - MANUAL",
      "isArray": true,
      "source": "MANUAL",
      "inputType": "KEY_VALUE_PAIRS",
      "predefinedKeys": ["otherPaymentType", "merchant", "otherFeeType1", "otherFeeType2"]
    }
  ],

  "apiEndpoints": {
    "ACCOUNT_API": {
      "baseUrl": "${account.api.url}",
      "endpoint": "/v1/accounts/{accountId}",
      "method": "GET",
      "timeout": 5000,
      "triggeredBy": "accountId",
      "lookupKeyMapping": { "accountId": "accountId" },
      "responseMapping": {
        "customerName": "customer.fullName",
        "address.line1": "customer.mailingAddress.line1",
        "address.line2": "customer.mailingAddress.line2",
        "address.city": "customer.mailingAddress.city",
        "address.state": "customer.mailingAddress.state",
        "address.zipCode": "customer.mailingAddress.zipCode",
        "accountLast4": "accountNumber.last4",
        "currentBalance": "balance.current"
      }
    },
    "APPLICATION_API": {
      "baseUrl": "${application.api.url}",
      "endpoint": "/v1/applications/{applicationId}",
      "method": "GET",
      "timeout": 5000,
      "triggeredBy": "applicationId",
      "lookupKeyMapping": { "applicationId": "applicationId" },
      "responseMapping": {
        "customerName": "applicant.fullName",
        "address.line1": "applicant.address.line1",
        "address.line2": "applicant.address.line2",
        "address.city": "applicant.address.city",
        "address.state": "applicant.address.state",
        "address.zipCode": "applicant.address.zipCode",
        "accountLast4": "applicationNumber.last4",
        "currentBalance": "requestedAmount"
      }
    }
  }
}
```

### 3.2 Field Source Summary (Current Assentis Reality)

| Field Category | Source | Fields |
|----------------|--------|--------|
| **FROM API** (accountId) | `API` | recipient.name, recipient.address.*, last4digit, totalBalance |
| **SYSTEM GENERATED** | `SYSTEM` | date, letterReferenceNumber |
| **FROM CONFIG** | `CONFIG` | resourceId, templateId, displayName |
| **EVERYTHING ELSE** | `MANUAL` | caseNumber, agencyOrExec, communicationReceivedDate, chargeOff*, accountHolderDetails.*, agency.*, showBullets, statements.*, etc. |

### 3.3 API vs Manual Field Comparison

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    CURRENT ASSENTIS FIELD SOURCES                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌───────────────────────────────┐  ┌───────────────────────────────────────┐  │
│  │   FROM API                    │  │   MANUAL INPUT (Draft Editor)         │  │
│  │   (accountId OR applicationId)│  │                                       │  │
│  ├───────────────────────────────┤  ├───────────────────────────────────────┤  │
│  │ • Recipient Name              │  │ • Employee ID                         │  │
│  │ • Address Line 1              │  │ • Case Number                         │  │
│  │ • Address Line 2              │  │ • Agency or Exec                      │  │
│  │ • City                        │  │ • Communication Received Date         │  │
│  │ • State                       │  │ • All Account Holder Details          │  │
│  │ • Zipcode                     │  │ • All Agency Information              │  │
│  │ • Last 4 Digits               │  │ • Charge Off Date/Balance/Reason      │  │
│  │ • Total Balance               │  │ • Solicitation Info                   │  │
│  │                               │  │ • All Fee Credits (showBullets)       │  │
│  │   (8 fields)                  │  │ • Statements Info                     │  │
│  │                               │  │ • Courtesy Phrase, Mini-Miranda, etc. │  │
│  │ ┌───────────────────────────┐ │  │                                       │  │
│  │ │ accountId → ACCOUNT_API   │ │  │   (50+ fields)                        │  │
│  │ │ applicationId → APP_API   │ │  │                                       │  │
│  │ └───────────────────────────┘ │  │                                       │  │
│  └───────────────────────────────┘  └───────────────────────────────────────┘  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Response Mapping in `template_vendor_mapping`

Add a new JSON column `response_mapping_config` to store how API responses map to the final XML output.

**New Column Recommendation:**

| Column Name | Type | Description |
|-------------|------|-------------|
| `response_mapping_config` | JSONB | Maps API responses to XML schema output |
| `xml_schema_version` | VARCHAR(20) | Version of XML schema used (e.g., "v12") |

```json
{
  "schemaVersion": "1.0",
  "xmlSchemaVersion": "v12",
  "outputFormat": "XML",
  "rootElement": "data",
  "namespaces": {
    "xsi": "http://www.w3.org/2001/XMLSchema-instance"
  },
  "schemaLocation": "schema_core_v12.xsd",
  "elementMappings": [
    {
      "xmlPath": "/data/template",
      "mappings": [
        { "xmlElement": "resourceId", "sourceGroup": "template", "sourceField": "resourceId" },
        { "xmlElement": "templateId", "sourceGroup": "template", "sourceField": "templateId" },
        { "xmlElement": "letterReferenceNumber", "sourceGroup": "template", "sourceField": "letterReferenceNumber" },
        { "xmlElement": "employeeId", "sourceGroup": "template", "sourceField": "employeeId" },
        { "xmlElement": "displayName", "sourceGroup": "template", "sourceField": "displayName" },
        { "xmlElement": "date", "sourceGroup": "template", "sourceField": "date", "format": "yyyy-MM-dd" }
      ]
    },
    {
      "xmlPath": "/data/recipient",
      "mappings": [
        { "xmlElement": "name", "sourceGroup": "recipient", "sourceField": "name" }
      ],
      "nestedElements": [
        {
          "xmlPath": "recipientAddress",
          "mappings": [
            { "xmlElement": "addressLine1", "sourceGroup": "recipient", "sourceField": "addressLine1" },
            { "xmlElement": "addressLine2", "sourceGroup": "recipient", "sourceField": "addressLine2" },
            { "xmlElement": "city", "sourceGroup": "recipient", "sourceField": "city" },
            { "xmlElement": "state", "sourceGroup": "recipient", "sourceField": "state" },
            { "xmlElement": "zipcode", "sourceGroup": "recipient", "sourceField": "zipcode" }
          ]
        }
      ]
    },
    {
      "xmlPath": "/data/entity",
      "mappings": [
        { "xmlElement": "last4digit", "sourceGroup": "entity", "sourceField": "last4digit" },
        { "xmlElement": "caseNumber", "sourceGroup": "entity", "sourceField": "caseNumber" },
        { "xmlElement": "agencyOrExec", "sourceGroup": "entity", "sourceField": "agencyOrExec" },
        { "xmlElement": "onOrDated", "sourceGroup": "entity", "sourceField": "onOrDated" },
        { "xmlElement": "communicationReceivedDate", "sourceGroup": "entity", "sourceField": "communicationReceivedDate" },
        { "xmlElement": "otherGovtAgency", "sourceGroup": "entity", "sourceField": "otherGovtAgency", "conditional": true }
      ],
      "nestedElements": [
        {
          "xmlPath": "primaryAccount",
          "mappings": [
            { "xmlElement": "reservationNumber", "sourceGroup": "primaryAccount", "sourceField": "reservationNumber" },
            { "xmlElement": "solicitationName", "sourceGroup": "primaryAccount", "sourceField": "solicitationName" },
            { "xmlElement": "solicitationDate", "sourceGroup": "primaryAccount", "sourceField": "solicitationDate" },
            { "xmlElement": "fulfillment", "sourceGroup": "primaryAccount", "sourceField": "fulfillment" },
            { "xmlElement": "accountOpenDate", "sourceGroup": "primaryAccount", "sourceField": "accountOpenDate" },
            { "xmlElement": "chargeOffDate", "sourceGroup": "primaryAccount", "sourceField": "chargeOffDate" },
            { "xmlElement": "chargeOffBalance", "sourceGroup": "primaryAccount", "sourceField": "chargeOffBalance" },
            { "xmlElement": "totalBalance", "sourceGroup": "primaryAccount", "sourceField": "totalBalance" }
          ],
          "nestedElements": [
            {
              "xmlPath": "accountHolderDetails",
              "mappings": [
                { "xmlElement": "name", "sourceGroup": "accountHolderDetails", "sourceField": "name" },
                { "xmlElement": "dateOfBirth", "sourceGroup": "accountHolderDetails", "sourceField": "dateOfBirth" },
                { "xmlElement": "sSNLast4", "sourceGroup": "accountHolderDetails", "sourceField": "sSNLast4" },
                { "xmlElement": "primaryEmail", "sourceGroup": "accountHolderDetails", "sourceField": "primaryEmail" },
                { "xmlElement": "primaryPhone", "sourceGroup": "accountHolderDetails", "sourceField": "primaryPhone" }
              ]
            }
          ]
        }
      ]
    },
    {
      "xmlPath": "/data/agency",
      "mappings": [
        { "xmlElement": "agencyName", "sourceGroup": "agency", "sourceField": "agencyName" },
        { "xmlElement": "phoneNumber", "sourceGroup": "agency", "sourceField": "phoneNumber" },
        { "xmlElement": "faxNumber", "sourceGroup": "agency", "sourceField": "faxNumber" }
      ],
      "nestedElements": [
        {
          "xmlPath": "agencyAddress",
          "mappings": [
            { "xmlElement": "addressLine1", "sourceGroup": "agency", "sourceField": "addressLine1" },
            { "xmlElement": "addressLine2", "sourceGroup": "agency", "sourceField": "addressLine2" },
            { "xmlElement": "city", "sourceGroup": "agency", "sourceField": "city" },
            { "xmlElement": "state", "sourceGroup": "agency", "sourceField": "state" },
            { "xmlElement": "zipcode", "sourceGroup": "agency", "sourceField": "zipcode" }
          ]
        }
      ]
    }
  ],
  "staticElements": [
    { "xmlPath": "/data/miniMiranda", "value": "true", "type": "BOOLEAN" },
    { "xmlPath": "/data/deptContactName", "value": "Customer Service", "type": "STRING" }
  ],
  "conditionalElements": [
    {
      "xmlPath": "/data/addBBBCaseNumber",
      "condition": {
        "field": "agencyOrExec",
        "operator": "EQUALS",
        "value": "BBB"
      },
      "trueValue": "true",
      "falseValue": "false"
    }
  ]
}
```

---

## 4. API Endpoints Design

### 4.1 Search Document Types by Vendor

```yaml
GET /api/v1/templates/vendors/{vendorCode}/document-types

Path Parameters:
  vendorCode: string (required) - Vendor identifier (e.g., "SMARTCOMM")

Query Parameters:
  lineOfBusiness: string (optional) - Filter by LOB
  templateCategory: string (optional) - Filter by category
  activeOnly: boolean (optional, default: true) - Only return active templates

Response 200:
{
  "vendorCode": "SMARTCOMM",
  "documentTypes": [
    {
      "templateId": "PR024",
      "displayName": "Late Payment + Statement Validation - Goodwill Declined",
      "templateCategory": "GOODWILL",
      "lineOfBusiness": "CREDIT_CARD",
      "requiredInputs": ["caseNumber", "accountNumber"],
      "description": "PR024 Late Payment + Statement Validation - Goodwill Declined"
    },
    {
      "templateId": "PR027",
      "displayName": "DV Solicited - CO Optional - Goodwill Approved Declined",
      "templateCategory": "GOODWILL",
      "lineOfBusiness": "CREDIT_CARD",
      "requiredInputs": ["caseNumber", "accountNumber", "customerId"],
      "description": "PR027 DV Solicited - CO Optional - Goodwill Approved Declined"
    },
    {
      "templateId": "RF002",
      "displayName": "Investigation CM Responsible Letter",
      "templateCategory": "INVESTIGATION",
      "lineOfBusiness": "CREDIT_CARD",
      "requiredInputs": ["caseNumber", "accountNumber"],
      "description": "RF002 Investigation CM Responsible Letter"
    }
  ],
  "totalCount": 3
}
```

### 4.2 Populate Template Fields

```yaml
POST /api/v1/templates/{templateId}/populate

Path Parameters:
  templateId: string (required) - Template identifier (e.g., "PR027")

Headers:
  X-Correlation-Id: string (required)
  X-Vendor-Code: string (required) - Vendor code (e.g., "SMARTCOMM")
  Accept: string (optional) - Response format: "application/xml" (default) or "application/json"

Request Body:
{
  "lookupKeys": {
    "accountId": "ACC-123456789",      // Option 1: Use for existing accounts
    // OR
    "applicationId": "APP-987654321"   // Option 2: Use for new applications
    // (Provide ONE of the above, not both)
  },
  "manualInputs": {
    "employeeId": "320045065",
    "caseNumber": "123454",
    "agencyOrExec": "OCC",
    "onOrDated": "on",
    "communicationReceivedDate": "2019-12-19",
    "chargeOffDate": "2019-10-12",
    "chargeOffBalance": 9899,
    "chargeOffReason": "non-payment for more than 180 days",
    "reservationNumber": "1301239",
    "solicitationName": "Solicitation Name",
    "solicitationDate": "2019-12-22",
    "fulfillment": "automated phone system",
    "accountHolderDetails": {
      "name": "Applicant Name",
      "dateOfBirth": "2000-12-30",
      "sSNLast4": "9999",
      "primaryEmail": "test@test.com",
      "primaryPhone": "9987776667",
      "address": {
        "addressLine1": "8777 Test Rd",
        "addressLine2": "Suite 111",
        "city": "City",
        "state": "State",
        "zipcode": "123999"
      }
    },
    "agency": {
      "agencyName": "Agency Name INC.",
      "addressLine1": "99 Agency Lane",
      "addressLine2": "Suite 2129",
      "city": "City",
      "state": "State",
      "zipcode": "124433",
      "phoneNumber": "123903333",
      "faxNumber": "233335455"
    },
    "courtesyWe": "As a courtesy, we",
    "miniMiranda": true,
    "deptContactName": "Customer Service",
    "gwCoOptions": "Not reporting",
    "gwDelinquencyDates": "Date1 and Date2",
    "stmtsSentSeparately": true,
    "cardAgreement": true,
    "enclosedSeparateCover": "separate cover",
    "showBullets": [
      { "bullet": "transactionAuthorized" },
      { "bullet": "participatedReleasePII", "date": "2019-07-22" },
      { "bullet": "fundsReturned", "amount": 99982 }
    ],
    "otherDetails": [
      { "name": "otherPaymentType", "value": "Other" },
      { "name": "merchant", "value": "Merchant Name" }
    ]
  }
}
```

#### Response Format: XML (Default)

When `Accept: application/xml` or no Accept header is provided:

```xml
Response 200 (Content-Type: application/xml):

<?xml version="1.0" encoding="UTF-8"?>
<data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:noNamespaceSchemaLocation="schema_core_v12.xsd">
    <template>
        <resourceId>123123123</resourceId>
        <templateId>PR027</templateId>
        <letterReferenceNumber>A321316842</letterReferenceNumber>
        <employeeId>320045065</employeeId>
        <displayName>PR027</displayName>
        <date>2025-01-14</date>
    </template>
    <recipient>
        <name>Jane Smith</name>
        <recipientAddress>
            <addressLine1>456 Oak Avenue</addressLine1>
            <addressLine2>Suite 1222</addressLine2>
            <city>Somewhere</city>
            <state>CA</state>
            <zipcode>90210</zipcode>
        </recipientAddress>
    </recipient>
    <entity>
        <last4digit>2333</last4digit>
        <caseNumber>123454</caseNumber>
        <agencyOrExec>OCC</agencyOrExec>
        <onOrDated>on</onOrDated>
        <communicationReceivedDate>2019-12-19</communicationReceivedDate>
        <primaryAccount>
            <reservationNumber>1301239</reservationNumber>
            <solicitationName>Solicitation Name</solicitationName>
            <solicitationDate>2019-12-22</solicitationDate>
            <fulfillment>automated phone system</fulfillment>
            <accountHolderDetails>
                <name>Applicant Name</name>
                <dateOfBirth>2000-12-30</dateOfBirth>
                <sSNLast4>9999</sSNLast4>
                <primaryEmail>test@test.com</primaryEmail>
                <primaryPhone>9987776667</primaryPhone>
                <address>
                    <addressLine1>8777 Test Rd</addressLine1>
                    <addressLine2>Suite 111</addressLine2>
                    <city>City</city>
                    <state>State</state>
                    <zipcode>123999</zipcode>
                </address>
            </accountHolderDetails>
            <totalBalance>88.33</totalBalance>
            <chargeOffDate>2019-10-12</chargeOffDate>
            <chargeOffBalance>9899</chargeOffBalance>
            <chargeOffReason>non-payment for more than 180 days</chargeOffReason>
        </primaryAccount>
    </entity>
    <agency>
        <agencyName>Agency Name INC.</agencyName>
        <agencyAddress>
            <addressLine1>99 Agency Lane</addressLine1>
            <addressLine2>Suite 2129</addressLine2>
            <city>City</city>
            <state>State</state>
            <zipcode>124433</zipcode>
        </agencyAddress>
        <phoneNumber>123903333</phoneNumber>
        <faxNumber>233335455</faxNumber>
    </agency>
    <courtesyWe>As a courtesy, we</courtesyWe>
    <miniMiranda>true</miniMiranda>
    <deptContactName>Customer Service</deptContactName>
</data>
```

#### Response Format: JSON (Optional)

When `Accept: application/json` header is provided:

```json
Response 200 (Content-Type: application/json):

{
  "status": "SUCCESS",
  "templateId": "PR027",
  "vendorCode": "SMARTCOMM",
  "generatedAt": "2025-01-14T15:30:00Z",
  "letterReferenceNumber": "A321316842",
  "xmlData": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "populatedFields": {
    "template": {
      "resourceId": "123123123",
      "templateId": "PR027",
      "letterReferenceNumber": "A321316842",
      "employeeId": "320045065",
      "displayName": "PR027",
      "date": "2025-01-14"
    },
    "recipient": {
      "name": "Jane Smith",
      "addressLine1": "456 Oak Avenue",
      "addressLine2": "Suite 1222",
      "city": "Somewhere",
      "state": "CA",
      "zipcode": "90210"
    },
    "entity": {
      "last4digit": "2333",
      "caseNumber": "123454",
      "agencyOrExec": "OCC"
    }
  },
  "apiCallResults": [
    { "api": "CUSTOMER_PROFILE_API", "status": "SUCCESS", "responseTimeMs": 120 },
    { "api": "CASE_MANAGEMENT_API", "status": "SUCCESS", "responseTimeMs": 85 },
    { "api": "CREDIT_SERVICE_API", "status": "SUCCESS", "responseTimeMs": 150 },
    { "api": "AGENCY_SERVICE_API", "status": "SUCCESS", "responseTimeMs": 45 }
  ]
}
```

#### Error Responses

```xml
Response 400 (Content-Type: application/xml):

<?xml version="1.0" encoding="UTF-8"?>
<error>
    <status>ERROR</status>
    <errorCode>MISSING_REQUIRED_INPUT</errorCode>
    <message>Required input 'caseNumber' is missing</message>
    <requiredInputs>
        <input>caseNumber</input>
        <input>accountNumber</input>
    </requiredInputs>
</error>
```

```xml
Response 207 - Partial Success (Content-Type: application/xml):

<?xml version="1.0" encoding="UTF-8"?>
<response>
    <status>PARTIAL_SUCCESS</status>
    <warnings>
        <warning api="AGENCY_SERVICE_API">Agency not found, using default values</warning>
    </warnings>
    <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="schema_core_v12.xsd">
        <!-- Full XML data here -->
    </data>
</response>
```

#### Content Negotiation Implementation

```java
@RestController
@RequestMapping("/api/v1/templates")
public class TemplatePopulationController {

    @PostMapping(
        value = "/{templateId}/populate",
        produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public Mono<ResponseEntity<?>> populateTemplate(
            @PathVariable String templateId,
            @RequestHeader("X-Vendor-Code") String vendorCode,
            @RequestHeader(value = "Accept", defaultValue = "application/xml") String acceptHeader,
            @RequestBody PopulationRequest request) {

        return templatePopulationService.populateTemplate(templateId, vendorCode, request)
            .map(result -> {
                if (acceptHeader.contains("application/json")) {
                    return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(result.toJsonResponse());
                } else {
                    return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(result.toXmlString());
                }
            });
    }
}
```

---

## 5. Database Changes

### 5.1 New Columns for `template_vendor_mapping`

```sql
ALTER TABLE document_hub.template_vendor_mapping
ADD COLUMN response_mapping_config JSONB,
ADD COLUMN xml_schema_version VARCHAR(20) DEFAULT 'v12',
ADD COLUMN required_inputs TEXT[];

COMMENT ON COLUMN document_hub.template_vendor_mapping.response_mapping_config IS
  'JSON configuration for mapping API responses to XML output';
COMMENT ON COLUMN document_hub.template_vendor_mapping.xml_schema_version IS
  'Version of XML schema used for output generation';
COMMENT ON COLUMN document_hub.template_vendor_mapping.required_inputs IS
  'Array of required input parameters for template population';
```

### 5.2 XML Schema Registry Table

The master XSD schema (schema_core_v12.xsd) needs to be stored and versioned. Store in **DCMS** with a reference in the database:

```sql
CREATE TABLE document_hub.xml_schema_registry (
    schema_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_name VARCHAR(100) NOT NULL,              -- e.g., 'schema_core'
    schema_version VARCHAR(20) NOT NULL,            -- e.g., 'v12'
    dcms_document_id VARCHAR(100) NOT NULL,         -- DCMS reference for the XSD file
    description TEXT,                               -- Schema description
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(schema_name, schema_version)
);

COMMENT ON TABLE document_hub.xml_schema_registry IS
  'Registry of XML schemas stored in DCMS for template XML generation';
COMMENT ON COLUMN document_hub.xml_schema_registry.dcms_document_id IS
  'Reference to the XSD file stored in DCMS';

-- Insert current schema
INSERT INTO document_hub.xml_schema_registry (schema_name, schema_version, dcms_document_id, description, is_active)
VALUES ('schema_core', 'v12', 'DCMS-XSD-001', 'Master XML schema for all template types', true);
```

#### Schema Registry Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         XML SCHEMA MANAGEMENT                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────────┐ │
│  │ template_vendor │      │ xml_schema_     │      │       DCMS          │ │
│  │ _mapping        │─────►│ registry        │─────►│                     │ │
│  │                 │      │                 │      │  schema_core_v12.xsd│ │
│  │ xml_schema_     │      │ schema_name     │      │  schema_core_v13.xsd│ │
│  │ version='v12'   │      │ schema_version  │      │  (actual XSD files) │ │
│  │                 │      │ dcms_document_id│      │                     │ │
│  └─────────────────┘      └─────────────────┘      └─────────────────────┘ │
│                                                                             │
│  Usage Flow:                                                                │
│  1. Get xml_schema_version from template_vendor_mapping                     │
│  2. Lookup dcms_document_id from xml_schema_registry                        │
│  3. Fetch XSD from DCMS (cached in Redis/memory)                            │
│  4. Generate XML and validate against schema                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Why Store in DCMS?

| Consideration | DCMS Approach |
|---------------|---------------|
| **Versioning** | DCMS handles document versioning |
| **Access Control** | Consistent with other document access |
| **Caching** | Fetch once, cache in Redis/memory |
| **Updates** | No redeployment needed for schema changes |
| **Audit Trail** | DCMS provides audit logging |

### 5.3 Updated Column Purposes

| Table | Column | Purpose |
|-------|--------|---------|
| `master_template_definition` | `template_variables` | Defines what variables are needed and from which API to fetch them |
| `master_template_definition` | `data_extraction_config` | Legacy field - can be migrated to template_variables |
| `template_vendor_mapping` | `template_fields` | Vendor-specific field overrides and customizations |
| `template_vendor_mapping` | `api_config` | Vendor-specific API configuration (endpoints, auth, timeouts) |
| `template_vendor_mapping` | `response_mapping_config` | **NEW** - How to map API responses to XML output |
| `template_vendor_mapping` | `xml_schema_version` | **NEW** - Which XML schema version to use |
| `template_vendor_mapping` | `required_inputs` | **NEW** - What inputs are required from the caller |

---

## 6. Template Variable Comparison

### PR024 Variables (Late Payment + Statement Validation)
| Variable Group | Required Variables |
|----------------|-------------------|
| template | resourceId, templateId, letterReferenceNumber, employeeId, displayName, date |
| recipient | name, recipientAddress |
| entity | last4digit, caseNumber, agencyOrExec, onOrDated, communicationReceivedDate |
| primaryAccount | minimumDueDate, minimumDueAmount, esignEnrollDate, esignEmailAddress |
| statements | fromStatementDate, statementDelivery, statementClosure[], statementEnclosed |
| showBullets | annualFeeCredit, auFeeCredit, cashAdvCredit, cLICredit, cpCredit, expCredit, fcCredit, lateFeeCredit, premCardCredit, repCardCredit, rtnPmntCredit, otherFeeCredit1, otherFeeCredit2, totalCredit |

### PR027 Variables (DV Solicited - CO Optional)
| Variable Group | Required Variables |
|----------------|-------------------|
| template | resourceId, templateId, letterReferenceNumber, employeeId, displayName, date |
| recipient | name, recipientAddress |
| entity | last4digit, caseNumber, agencyOrExec, onOrDated, communicationReceivedDate, otherGovtAgency |
| primaryAccount | reservationNumber, solicitationName, solicitationDate, fulfillment, accountHolderDetails, recentActivity, recentActivityAmount, recentActivityDate, accountOpenDate, initialCreditLimit, accountStatus, debtValidationStatus, routingNumber, last4chkAccount, last4savAccount, chargeOffDate, chargeOffBalance, chargeOffReason, totalBalance, stmtsSentSeparately, cardAgreement, enclosedSeparateCover, gwCoOptions, gwDelinquencyDates, statementDates[], yearMonths[], otherDetails[] |
| agency | agencyName, agencyAddress, phoneNumber, faxNumber |

### RF002 Variables (Investigation CM Responsible Letter)
| Variable Group | Required Variables |
|----------------|-------------------|
| template | resourceId, templateId, letterReferenceNumber, employeeId, displayName, date |
| recipient | name, recipientAddress |
| entity | last4digit, caseNumber |
| primaryAccount | debtValidationStatus, totalBalance, paymentTransaction |
| showBullets | transactionAuthorized, merchantRecordChipEnabledCCPresent, participatedReleasePII, unableToContact, notAdmitToParticipateInFraudActivity, accessDifferentDevice, oneTimePasswordSent, noFraudIdentified, receivedMultipleCalls, otherTransactionsNotClaimedAsFraud, determinedPaymentsAuthorized, thirdPartyPayment, unauthorizedPayment, fundsReturned |

---

## 7. Implementation Approach

### Service Layer Flow

```java
@Service
public class TemplatePopulationService {

    public Mono<TemplatePopulationResponse> populateTemplate(
            String templateId,
            String vendorCode,
            PopulationRequest request) {

        return Mono.zip(
            // 1. Get template configuration
            masterTemplateDao.findByTemplateId(templateId),
            // 2. Get vendor mapping
            vendorMappingDao.findByTemplateIdAndVendor(templateId, vendorCode)
        )
        .flatMap(tuple -> {
            MasterTemplateDefinition template = tuple.getT1();
            TemplateVendorMapping vendorMapping = tuple.getT2();

            // 3. Parse template_variables JSON
            TemplateVariablesConfig variablesConfig =
                parseTemplateVariables(template.getTemplateVariables());

            // 4. Fetch data from all required APIs in parallel
            return fetchAllApiData(variablesConfig, request)
                .collectMap(ApiResponse::getApiName, Function.identity())
                .flatMap(apiResponses -> {
                    // 5. Parse response_mapping_config
                    ResponseMappingConfig mappingConfig =
                        parseResponseMapping(vendorMapping.getResponseMappingConfig());

                    // 6. Map responses to XML structure
                    return generateXmlResponse(apiResponses, mappingConfig, variablesConfig);
                });
        });
    }

    private Flux<ApiResponse> fetchAllApiData(
            TemplateVariablesConfig config,
            PopulationRequest request) {

        return Flux.fromIterable(config.getApiEndpoints().entrySet())
            .flatMap(entry -> callApi(entry.getKey(), entry.getValue(), request)
                .onErrorResume(e -> Mono.just(ApiResponse.failed(entry.getKey(), e))));
    }
}
```

### Similar to data_extraction_config Pattern

The implementation follows the same pattern as `data_extraction_config`:

1. **Configuration-driven**: Template variables and mappings are stored in JSON, not hard-coded
2. **Flexible API sources**: Each variable group can specify its own API source
3. **Field mapping**: Uses dot notation (e.g., `address.line1`) for nested fields
4. **Type conversion**: Supports type conversion (STRING, DATE, DECIMAL, BOOLEAN, ENUM)
5. **Conditional fields**: Some fields are only included based on conditions

---

## 8. Summary of Recommendations

### Where to Store What

| Data | Location | Column | Reason |
|------|----------|--------|--------|
| What variables are needed | `master_template_definition` | `template_variables` | Variables are template-specific, same across vendors |
| Which API to call | `master_template_definition` | `template_variables.apiEndpoints` | APIs are template-specific |
| How to map response to XML | `template_vendor_mapping` | `response_mapping_config` (NEW) | XML structure may vary by vendor |
| Vendor-specific API overrides | `template_vendor_mapping` | `api_config` | Different vendors may have different endpoints |
| Required inputs | `template_vendor_mapping` | `required_inputs` (NEW) | May vary by vendor configuration |

### Additional Columns Needed

1. `response_mapping_config` (JSONB) - For XML response mapping
2. `xml_schema_version` (VARCHAR) - To track schema version
3. `required_inputs` (TEXT[]) - List of required input parameters
