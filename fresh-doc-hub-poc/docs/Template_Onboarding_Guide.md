# Document Template Onboarding Guide

This guide explains how to use the Excel template to onboard new document templates into the Document Hub.

## Excel Template Structure

The onboarding Excel file contains 4 sheets:

### Sheet 1: Templates
Defines the document template metadata.

| Column | Required | Description | Example |
|--------|----------|-------------|---------|
| template_type | Yes | Unique identifier for the template | `CREDIT_CARD_STATEMENT` |
| template_version | Yes | Version number (integer) | `1` |
| template_category | Yes | Category for grouping | `Statement` |
| display_name | Yes | User-friendly name | `Credit Card Statement` |
| description | Yes | Brief description | `Monthly credit card statement` |
| line_of_business | Yes | Business line | `CREDIT_CARD`, `MORTGAGE`, `TAX` |
| shared_flag | Yes | Is this shared across all accounts? | `TRUE` or `FALSE` |
| mock_api_url | No | URL for eligibility check (if needed) | `http://localhost:8080/api/v1/mock-api/...` |

### Sheet 2: DataExtractionConfig
Defines what data fields should be extracted from documents of this template type.

| Column | Required | Description | Example |
|--------|----------|-------------|---------|
| template_type | Yes | Links to Templates sheet | `CREDIT_CARD_STATEMENT` |
| field_name | Yes | Name of the field | `statement_date` |
| field_path | No | Path to extract from (if automated) | `$.header.date` |
| data_type | Yes | Type of data | `STRING`, `DATE`, `NUMBER`, `BOOLEAN` |
| required | Yes | Is this field required? | `TRUE` or `FALSE` |
| display_label | No | User-friendly label | `Statement Date` |

### Sheet 3: EligibilityCriteria
Defines who can see documents of this template type (conditional access rules).

| Column | Required | Description | Example |
|--------|----------|-------------|---------|
| template_type | Yes | Links to Templates sheet | `PLATINUM_CARD_AGREEMENT` |
| criteria_field | Yes | Field to check | `membership_tier` |
| criteria_source | Yes | Where to get the value | `API_CALL`, `REQUEST_CONTEXT`, `DOCUMENT_METADATA` |
| operator | Yes | Comparison operator | `EQUALS`, `IN`, `GREATER_THAN`, `CONTAINS` |
| criteria_values | Yes | Expected value(s) | `PLATINUM` or `PLATINUM,GOLD` |
| api_endpoint | No | API to call (if source is API_CALL) | `/mock-api/credit-info` |

### Sheet 4: Documents
Sample documents to be indexed (optional - for testing or bulk import).

| Column | Required | Description | Example |
|--------|----------|-------------|---------|
| template_type | Yes | Links to Templates sheet | `CREDIT_CARD_STATEMENT` |
| template_version | Yes | Version number | `1` |
| file_name | Yes | Document file name | `statement_jan_2024.pdf` |
| file_location | Yes | Storage path/URL | `/documents/statements/` |
| account_key | Conditional | Account UUID (if account-specific) | `aaaa0000-0000-0000-0000-000000000001` |
| customer_key | Conditional | Customer UUID (if customer-specific) | `cccc0000-0000-0000-0000-000000000001` |
| reference_key | No | Reference identifier | `D164` |
| reference_key_type | No | Type of reference | `Disclosure_Code` |
| valid_from | No | Start date (YYYY-MM-DD) | `2024-01-01` |
| valid_until | No | End date (YYYY-MM-DD) | `2027-01-01` |
| extracted_* | No | Extracted field values (one column per field) | See below |

**Note:** For extracted fields, add columns named `extracted_<field_name>` matching the fields defined in DataExtractionConfig.
Example: `extracted_statement_date`, `extracted_balance`, `extracted_due_date`

---

## Ownership Rules

Documents can be owned/scoped in three ways:

| Ownership Type | account_key | customer_key | shared_flag | Description |
|----------------|-------------|--------------|-------------|-------------|
| Account-specific | Required | Optional | FALSE | Visible only to specific account |
| Customer-specific | Empty | Required | FALSE | Visible to all accounts of a customer |
| Shared | Empty | Empty | TRUE | Visible to all users (regulatory docs) |

---

## Eligibility Criteria Examples

### Example 1: Membership Tier Check
```
criteria_field: membership_tier
criteria_source: API_CALL
operator: EQUALS
criteria_values: PLATINUM
api_endpoint: /mock-api/credit-info
```
Only users with PLATINUM membership see this document.

### Example 2: Region-Based Access
```
criteria_field: region
criteria_source: REQUEST_CONTEXT
operator: IN
criteria_values: US,CA,MX
```
Only users in US, Canada, or Mexico see this document.

### Example 3: Account Balance Check
```
criteria_field: account_balance
criteria_source: API_CALL
operator: GREATER_THAN
criteria_values: 10000
api_endpoint: /mock-api/account-info
```
Only users with balance > $10,000 see this document.

---

## Processing Workflow

```
Excel Upload → Validation → Database Insert → Confirmation Report
     ↓              ↓              ↓                  ↓
  Parse sheets   Check rules   Insert into:      Summary of:
                 & formats     - master_template  - Templates added
                               - storage_index    - Documents indexed
                                                  - Errors/warnings
```

---

## Validation Rules

The import process validates:

1. **Templates Sheet:**
   - template_type must be unique (per version)
   - line_of_business must be valid enum
   - shared_flag must be TRUE/FALSE

2. **DataExtractionConfig Sheet:**
   - template_type must exist in Templates sheet
   - field_name must be unique per template
   - data_type must be valid (STRING, DATE, NUMBER, BOOLEAN)

3. **EligibilityCriteria Sheet:**
   - template_type must exist in Templates sheet
   - operator must be valid (EQUALS, IN, GREATER_THAN, LESS_THAN, CONTAINS)
   - api_endpoint required if criteria_source is API_CALL

4. **Documents Sheet:**
   - template_type must exist in Templates sheet
   - Either account_key OR customer_key required (unless shared)
   - Dates must be valid format (YYYY-MM-DD)
