# Shared Document Eligibility Catalog

**Purpose:** Comprehensive catalog of all shared document types with eligibility rules and extraction logic
**Last Updated:** 2025-11-09
**Status:** Template - Ready to be populated with actual documents

---

## Table of Contents

1. [Overview](#overview)
2. [Document Structure Template](#document-structure-template)
3. [Questionnaire for SMEs](#questionnaire-for-smes)
4. [Required Fields](#required-fields)
5. [Example Documents](#example-documents)
6. [Data Sources Available](#data-sources-available)
7. [Catalog Entries](#catalog-entries)

---

## Overview

### What is a Shared Document?

A **shared document** is a template that is shown to multiple customers based on eligibility criteria, rather than being created specifically for one account.

**Types:**
1. **Universal (scope: "all")** - Everyone sees it (e.g., Privacy Policy)
2. **Account-Type Based (scope: "credit_card_account_only")** - Based on account classification
3. **Custom Rule Based (scope: "custom_rule")** - Dynamic eligibility using data extraction

### Why This Catalog?

This catalog provides:
- ✅ Complete inventory of all shared documents
- ✅ Clear eligibility rules for each document
- ✅ Data extraction requirements
- ✅ Testing scenarios
- ✅ Reference for implementation

---

## Document Structure Template

Use this template for each shared document:

```yaml
---
# Document Metadata
document_id: "SHARED-DOC-001"
document_name: "Premium Cardholder Benefits Disclosure"
document_type: "disclosure"
category: "Benefits"
line_of_business: "credit_card"
regulatory: false
effective_date: "2024-01-01"
owner_department: "Product Marketing"

# Sharing Configuration
sharing_scope: "custom_rule"  # "all" | "credit_card_account_only" | "digital_bank_customer_only" | "custom_rule"

# Eligibility Criteria (for custom_rule only)
eligibility_rule:
  rule_name: "premium_cardholder_benefits_eligibility"
  rule_type: "composite"  # "composite" | "simple"
  logic_operator: "AND"  # "AND" | "OR"

  # Data Sources Needed
  data_sources:
    - source_id: "account_service"
      source_type: "REST_API"
      endpoint: "GET /api/v1/accounts/{accountId}"
      fields_needed:
        - accountBalance
        - accountStatus
        - accountType
        - creditLimit

    - source_id: "customer_service"
      source_type: "REST_API"
      endpoint: "GET /api/v1/customers/{customerId}"
      fields_needed:
        - customerTier
        - enrollmentDate
        - creditScore

  # Eligibility Conditions
  conditions:
    - field: "accountBalance"
      operator: "GREATER_THAN"
      value: 10000
      logical_operator: "AND"

    - field: "customerTier"
      operator: "IN"
      value: ["GOLD", "PLATINUM", "BLACK"]
      logical_operator: "AND"

    - field: "accountStatus"
      operator: "EQUALS"
      value: "ACTIVE"

  # Error Handling
  error_handling:
    on_api_failure: "exclude"  # "exclude" | "include" | "fallback"
    fallback_result: false

# Business Context
business_justification: |
  Show premium benefits disclosure only to high-value customers to:
  - Increase engagement with premium features
  - Drive upgrade conversions
  - Comply with benefit disclosure requirements

# Testing Scenarios
test_scenarios:
  - scenario: "Eligible customer"
    customer_data:
      customerId: "test-001"
      accountId: "acct-001"
      accountBalance: 15000
      customerTier: "GOLD"
      accountStatus: "ACTIVE"
    expected_result: true

  - scenario: "Low balance customer"
    customer_data:
      customerId: "test-002"
      accountId: "acct-002"
      accountBalance: 5000
      customerTier: "GOLD"
      accountStatus: "ACTIVE"
    expected_result: false

# Change History
version: 1.0
created_by: "Product Team"
approved_by: "Compliance"
last_modified: "2025-11-09"
---
```

---

## Questionnaire for SMEs

### For Each Shared Document, Ask:

#### Section 1: Document Identification

**Q1.1: What is the document name?**
- Example: "Privacy Policy 2024", "Cardholder Agreement", "Rate Change Notice"

**Q1.2: What type of document is this?**
- Options: disclosure, agreement, notice, statement, policy, terms_and_conditions, regulatory, marketing

**Q1.3: Which department owns this document?**
- Example: Legal, Compliance, Marketing, Product, Operations

**Q1.4: Is this a regulatory document?**
- Yes/No
- If yes, which regulation? (e.g., TILA, CCPA, GDPR, SOX)

**Q1.5: Which line of business does this apply to?**
- Options: credit_card, digital_banking, enterprise, all

---

#### Section 2: Sharing Scope

**Q2.1: Who should see this document?**
- [ ] Everyone (all customers)
- [ ] Credit card customers only
- [ ] Digital banking customers only
- [ ] Enterprise customers only
- [ ] Specific criteria (custom rule)

**Q2.2: If custom criteria, what determines eligibility?**
- Customer attributes (tier, status, enrollment date, credit score)
- Account attributes (balance, type, status, credit limit, age)
- Transaction attributes (activity, spending, payment history)
- Product attributes (enrolled products, features activated)
- Relationship attributes (tenure, value, segment)

**Q2.3: What is the business reason for this eligibility rule?**
- Example: "Only show to high-value customers to drive engagement"

---

#### Section 3: Eligibility Criteria (for custom rules)

**Q3.1: What data do we need to determine eligibility?**

For each data point:
- **Field Name:** (e.g., accountBalance)
- **Data Source:** (e.g., Account Service API, Customer Service, Database)
- **API Endpoint:** (e.g., GET /api/v1/accounts/{accountId})
- **JSON Path:** (e.g., $.data.balance.currentBalance)
- **Data Type:** (string, number, boolean, date, array)
- **Required or Optional:** Required/Optional
- **Default Value if Missing:** (e.g., 0, null, exclude customer)

**Q3.2: What are the specific conditions?**

For each condition:
- **Field:** (e.g., accountBalance)
- **Operator:** EQUALS | NOT_EQUALS | GREATER_THAN | LESS_THAN | GREATER_THAN_OR_EQUALS | LESS_THAN_OR_EQUALS | IN | NOT_IN | CONTAINS
- **Value:** (e.g., 10000, "ACTIVE", ["GOLD", "PLATINUM"])
- **Logical Operator:** AND | OR
- **Priority:** (if multiple conditions)

**Q3.3: How should multiple conditions be combined?**
- [ ] ALL conditions must be true (AND logic)
- [ ] ANY condition can be true (OR logic)
- [ ] Complex logic (specify: e.g., "(A AND B) OR (C AND D)")

**Q3.4: Are there any transformations needed on the data?**
- Example: Convert string to uppercase, calculate age from date, round numbers
- Transformation type: TO_UPPER | TO_LOWER | TO_NUMBER | DATE_DIFF | ABS | ROUND

---

#### Section 4: Error Handling

**Q4.1: What if the API call fails?**
- [ ] Exclude the document (safe default)
- [ ] Include the document (optimistic)
- [ ] Use fallback rule (specify)
- [ ] Show error to user

**Q4.2: What if data is missing or invalid?**
- [ ] Exclude the document
- [ ] Use default value (specify)
- [ ] Treat as condition failed
- [ ] Log and investigate

**Q4.3: How critical is this document?**
- [ ] Critical (must be shown if eligible, escalate errors)
- [ ] Important (show if possible, log errors)
- [ ] Nice to have (best effort, silent failure ok)

---

#### Section 5: Time-Based Rules

**Q5.1: Is this document time-sensitive?**
- Yes/No

**Q5.2: When should this document be shown?**
- [ ] Always (no time restriction)
- [ ] Specific date range (from YYYY-MM-DD to YYYY-MM-DD)
- [ ] Relative to customer event (e.g., 30 days before account anniversary)
- [ ] Recurring (e.g., quarterly, annually)

**Q5.3: What happens when time period expires?**
- [ ] Document automatically hidden
- [ ] New version appears
- [ ] Manual review required

---

#### Section 6: Testing & Validation

**Q6.1: Provide 3 test scenarios:**

**Scenario 1: Should See Document (Positive)**
- Customer characteristics:
- Expected result: Document is shown
- Reason:

**Scenario 2: Should NOT See Document (Negative)**
- Customer characteristics:
- Expected result: Document is NOT shown
- Reason:

**Q6.2: Provide edge cases:**
- What if account balance is exactly the threshold?
- What if customer is in transition (e.g., tier upgrade in progress)?
- What if data is 1 day old vs real-time?

---

#### Section 7: Compliance & Legal

**Q7.1: Are there legal requirements for showing this document?**
- Yes/No
- If yes, cite regulation:

**Q7.2: Are there legal requirements for NOT showing this document?**
- Example: "Do not show to customers in California" (state restrictions)

**Q7.3: What is the disclosure timing requirement?**
- [ ] Real-time (must be shown immediately when eligible)
- [ ] Within 24 hours
- [ ] Within 30 days
- [ ] No specific timing

**Q7.4: Do we need to track/audit who sees this document?**
- Yes/No
- If yes, what to log: (customer ID, timestamp, eligibility reason, etc.)

---

## Required Fields

### Minimum Required Fields for Each Entry

**Category A: Document Metadata (REQUIRED)**
```yaml
- document_id: Unique identifier
- document_name: Display name
- document_type: Type classification
- category: Business category
- line_of_business: Which LOB
- regulatory: true/false
- owner_department: Owning team
```

**Category B: Sharing Configuration (REQUIRED)**
```yaml
- sharing_scope: "all" | "credit_card_account_only" | "custom_rule"
- effective_date: When it becomes active
- valid_until: When it expires (optional)
```

**Category C: Eligibility Rule (REQUIRED for custom_rule)**
```yaml
- rule_name: Unique name for the rule
- rule_type: "simple" | "composite"
- data_sources: List of APIs/services needed
- conditions: Eligibility criteria
- error_handling: What to do on failure
```

**Category D: Business Context (RECOMMENDED)**
```yaml
- business_justification: Why this rule
- target_audience: Who should see it
- expected_volume: How many customers (~%)
```

**Category E: Testing (RECOMMENDED)**
```yaml
- test_scenarios: At least 2 (positive & negative)
- edge_cases: Known boundary conditions
```

---

## Data Sources Available

### 1. Customer Service API
**Endpoint:** `GET /api/v1/customers/{customerId}`

**Available Fields:**
```json
{
  "customerId": "uuid",
  "customerTier": "STANDARD | GOLD | PLATINUM | BLACK",
  "status": "ACTIVE | INACTIVE | SUSPENDED | CLOSED",
  "enrollmentDate": "2020-01-15",
  "creditScore": 750,
  "riskRating": "LOW | MEDIUM | HIGH",
  "segment": "MASS | AFFLUENT | WEALTH",
  "communicationPreferences": {
    "email": true,
    "sms": true,
    "mail": false
  },
  "demographics": {
    "state": "CA",
    "zipCode": "94102",
    "ageGroup": "35-44"
  }
}
```

**Response Time:** ~50ms
**Cache TTL:** 5 minutes

---

### 2. Account Service API
**Endpoint:** `GET /api/v1/accounts/{accountId}`

**Available Fields:**
```json
{
  "accountId": "uuid",
  "accountType": "CHECKING | SAVINGS | CREDIT_CARD | LOAN",
  "accountStatus": "ACTIVE | INACTIVE | CLOSED | DELINQUENT",
  "balance": {
    "currentBalance": 15000.50,
    "availableBalance": 12000.00
  },
  "creditLimit": 25000.00,
  "interestRate": 15.99,
  "openDate": "2020-01-15",
  "lastActivityDate": "2024-11-05",
  "accountAge": 58,  // months
  "productCode": "CC-PREMIUM",
  "features": ["cashback", "travel_insurance", "concierge"]
}
```

**Response Time:** ~30ms
**Cache TTL:** 1 minute (balance changes frequently)

---

### 3. Transaction Service API
**Endpoint:** `POST /api/v1/transactions/summary`

**Request:**
```json
{
  "accountId": "uuid",
  "startDate": "2024-10-01",
  "endDate": "2024-11-01"
}
```

**Available Fields:**
```json
{
  "totalTransactions": 45,
  "totalSpend": 5400.25,
  "averageTransactionAmount": 120.00,
  "largestTransaction": 500.00,
  "merchantCategories": ["dining", "travel", "retail"],
  "internationalTransactions": 5,
  "lastTransactionDate": "2024-11-05"
}
```

**Response Time:** ~100ms (aggregation query)
**Cache TTL:** 1 hour

---

### 4. Product Enrollment API
**Endpoint:** `GET /api/v1/customers/{customerId}/enrollments`

**Available Fields:**
```json
{
  "enrolledProducts": [
    {
      "productCode": "AUTOPAY",
      "enrolledDate": "2024-01-15",
      "status": "ACTIVE"
    },
    {
      "productCode": "PAPERLESS_STATEMENTS",
      "enrolledDate": "2024-02-20",
      "status": "ACTIVE"
    }
  ],
  "eligibleProducts": ["REWARDS_PLUS", "TRAVEL_INSURANCE"]
}
```

**Response Time:** ~40ms
**Cache TTL:** 15 minutes

---

### 5. Database Direct Query (For Simple Lookups)
**Table:** `customer_attributes`

**Available via SQL:**
```sql
SELECT
  vip_status,
  relationship_tenure_months,
  lifetime_value,
  churn_risk_score,
  last_contact_date
FROM customer_attributes
WHERE customer_id = :customerId
```

**Response Time:** ~5ms
**Use When:** Simple lookup, no complex aggregation needed

---

## Example Documents

### Example 1: Universal Document (Scope: "all")

```yaml
---
document_id: "SHARED-DOC-001"
document_name: "Privacy Policy 2024"
document_type: "policy"
category: "Legal"
line_of_business: "all"
regulatory: true
regulation: "CCPA, GDPR"
owner_department: "Legal"

sharing_scope: "all"
effective_date: "2024-01-01"
valid_until: null

business_justification: |
  Required by law to make privacy policy available to all customers.
  No eligibility criteria - everyone must have access.

expected_volume: "100% of customers"

test_scenarios:
  - scenario: "Any customer"
    expected_result: true
    reason: "Universal document, always shown"
---
```

---

### Example 2: Account-Type Based (Scope: "credit_card_account_only")

```yaml
---
document_id: "SHARED-DOC-002"
document_name: "Cardholder Agreement"
document_type: "agreement"
category: "Terms"
line_of_business: "credit_card"
regulatory: true
regulation: "TILA"
owner_department: "Legal"

sharing_scope: "credit_card_account_only"
effective_date: "2024-01-01"
valid_until: null

business_justification: |
  Legal requirement to provide cardholder agreement to all credit card customers.
  Only applicable to credit card accounts.

expected_volume: "~60% of customers (credit card holders)"

test_scenarios:
  - scenario: "Credit card customer"
    account_type: "CREDIT_CARD"
    expected_result: true

  - scenario: "Checking account customer"
    account_type: "CHECKING"
    expected_result: false
---
```

---

### Example 3: Custom Rule - Simple Condition

```yaml
---
document_id: "SHARED-DOC-003"
document_name: "High Balance Benefits Guide"
document_type: "disclosure"
category: "Benefits"
line_of_business: "credit_card"
regulatory: false
owner_department: "Product Marketing"

sharing_scope: "custom_rule"
effective_date: "2024-01-01"
valid_until: null

eligibility_rule:
  rule_name: "high_balance_benefits_eligibility"
  rule_type: "simple"

  data_sources:
    - source_id: "account_service"
      source_type: "REST_API"
      endpoint: "GET /api/v1/accounts/{accountId}"
      timeout_ms: 5000
      response_mapping:
        accountBalance: "$.balance.currentBalance"
        accountStatus: "$.accountStatus"

  conditions:
    - field: "accountBalance"
      operator: "GREATER_THAN_OR_EQUALS"
      value: 10000
      logical_operator: "AND"

    - field: "accountStatus"
      operator: "EQUALS"
      value: "ACTIVE"

  error_handling:
    on_api_failure: "exclude"
    fallback_result: false
    log_level: "WARN"

business_justification: |
  Show benefits guide only to customers with high balances to:
  - Increase engagement with premium features
  - Drive product upgrades
  - Reduce churn among high-value customers

expected_volume: "~15% of credit card customers"

test_scenarios:
  - scenario: "High balance active customer"
    account_data:
      accountBalance: 15000
      accountStatus: "ACTIVE"
    expected_result: true

  - scenario: "Low balance customer"
    account_data:
      accountBalance: 5000
      accountStatus: "ACTIVE"
    expected_result: false

  - scenario: "High balance but inactive"
    account_data:
      accountBalance: 15000
      accountStatus: "INACTIVE"
    expected_result: false
---
```

---

### Example 4: Custom Rule - Composite (AND/OR Logic)

```yaml
---
document_id: "SHARED-DOC-004"
document_name: "Premium Cardholder Benefits Disclosure"
document_type: "disclosure"
category: "Benefits"
line_of_business: "credit_card"
regulatory: false
owner_department: "Product Marketing"

sharing_scope: "custom_rule"
effective_date: "2024-01-01"
valid_until: null

eligibility_rule:
  rule_name: "premium_cardholder_benefits_eligibility"
  rule_type: "composite"
  logic_operator: "AND"

  data_sources:
    - source_id: "account_service"
      source_type: "REST_API"
      endpoint: "GET /api/v1/accounts/{accountId}"
      response_mapping:
        accountBalance: "$.balance.currentBalance"
        accountStatus: "$.accountStatus"
        creditLimit: "$.creditLimit"

    - source_id: "customer_service"
      source_type: "REST_API"
      endpoint: "GET /api/v1/customers/{customerId}"
      response_mapping:
        customerTier: "$.customerTier"
        enrollmentDate: "$.enrollmentDate"

  # Composite condition: High balance OR Premium tier
  rules:
    - rule_type: "simple"
      logic_operator: "OR"
      conditions:
        - field: "accountBalance"
          operator: "GREATER_THAN"
          value: 50000

        - field: "customerTier"
          operator: "IN"
          value: ["PLATINUM", "BLACK"]

    # AND must be active
    - rule_type: "simple"
      conditions:
        - field: "accountStatus"
          operator: "EQUALS"
          value: "ACTIVE"

  error_handling:
    on_api_failure: "exclude"
    fallback_result: false

business_justification: |
  Show premium benefits to:
  1. Very high balance customers (>$50K) OR
  2. Premium tier customers (Platinum/Black)
  AND account must be active

  Goal: Increase premium feature utilization and retention.

expected_volume: "~5% of credit card customers"

test_scenarios:
  - scenario: "High balance, standard tier, active"
    account_data:
      accountBalance: 60000
      accountStatus: "ACTIVE"
    customer_data:
      customerTier: "STANDARD"
    expected_result: true
    reason: "Balance > 50K (first OR condition satisfied)"

  - scenario: "Low balance, platinum tier, active"
    account_data:
      accountBalance: 10000
      accountStatus: "ACTIVE"
    customer_data:
      customerTier: "PLATINUM"
    expected_result: true
    reason: "Platinum tier (second OR condition satisfied)"

  - scenario: "High balance, platinum tier, inactive"
    account_data:
      accountBalance: 60000
      accountStatus: "INACTIVE"
    customer_data:
      customerTier: "PLATINUM"
    expected_result: false
    reason: "Account not active (AND condition failed)"
---
```

---

### Example 5: Time-Based Custom Rule

```yaml
---
document_id: "SHARED-DOC-005"
document_name: "Annual Fee Waiver Notice"
document_type: "notice"
category: "Fees"
line_of_business: "credit_card"
regulatory: false
owner_department: "Product"

sharing_scope: "custom_rule"
effective_date: "2024-12-01"
valid_until: "2024-12-31"

eligibility_rule:
  rule_name: "annual_fee_waiver_eligibility"
  rule_type: "composite"
  logic_operator: "AND"

  data_sources:
    - source_id: "account_service"
      source_type: "REST_API"
      endpoint: "GET /api/v1/accounts/{accountId}"
      response_mapping:
        accountOpenDate: "$.openDate"
        totalSpendYTD: "$.yearToDateSpend"

    - source_id: "transaction_service"
      source_type: "REST_API"
      endpoint: "POST /api/v1/transactions/summary"
      request_body:
        accountId: "${accountId}"
        startDate: "${currentYear}-01-01"
        endDate: "${currentYear}-12-31"
      response_mapping:
        totalSpend: "$.totalSpend"

  conditions:
    # Account anniversary is in December
    - field: "accountOpenDate"
      operator: "MONTH_EQUALS"
      value: 12
      logical_operator: "AND"

    # Spent at least $25K this year
    - field: "totalSpend"
      operator: "GREATER_THAN_OR_EQUALS"
      value: 25000

  error_handling:
    on_api_failure: "exclude"
    fallback_result: false

business_justification: |
  Show fee waiver notice only during December for customers whose:
  1. Account anniversary is in December (annual fee due)
  2. Spent at least $25K this year (qualify for waiver)

  Goal: Retain high-spending customers by automatically waiving fees.

expected_volume: "~8% of credit card customers (only in December)"

test_scenarios:
  - scenario: "Anniversary in December, high spend"
    account_data:
      accountOpenDate: "2020-12-15"
      totalSpend: 30000
    expected_result: true

  - scenario: "Anniversary in December, low spend"
    account_data:
      accountOpenDate: "2020-12-15"
      totalSpend: 15000
    expected_result: false
---
```

---

## Catalog Entries

### Instructions for Completion

1. **Gather all shared document types** from:
   - Product teams
   - Marketing teams
   - Legal/Compliance teams
   - Operations teams

2. **For each document, create an entry** using the template above

3. **Schedule SME interviews** using the questionnaire

4. **Document eligibility rules** with examples

5. **Validate with test scenarios**

6. **Get approval** from:
   - Business owner
   - Compliance (if regulatory)
   - IT/Architecture
   - Security

---

### Catalog Entry Template (Copy for Each Document)

```yaml
---
# DOCUMENT METADATA
document_id: "SHARED-DOC-XXX"
document_name: "[Document Name]"
document_type: "[Type]"
category: "[Category]"
line_of_business: "[LOB]"
regulatory: [true/false]
owner_department: "[Department]"

# SHARING CONFIGURATION
sharing_scope: "[all|credit_card_account_only|custom_rule]"
effective_date: "YYYY-MM-DD"
valid_until: "YYYY-MM-DD or null"

# ELIGIBILITY RULE (for custom_rule only)
eligibility_rule:
  rule_name: "[unique_rule_name]"
  rule_type: "[simple|composite]"
  logic_operator: "[AND|OR]"  # for composite only

  data_sources:
    - source_id: "[name]"
      source_type: "[REST_API|DATABASE|CACHE]"
      endpoint: "[API endpoint]"
      response_mapping:
        [fieldName]: "[JSONPath]"

  conditions:
    - field: "[field_name]"
      operator: "[operator]"
      value: [value]
      logical_operator: "[AND|OR]"

  error_handling:
    on_api_failure: "[exclude|include|fallback]"
    fallback_result: [true|false]

# BUSINESS CONTEXT
business_justification: |
  [Why this rule? What business goal does it serve?]

expected_volume: "[Percentage or number of customers]"

# TESTING
test_scenarios:
  - scenario: "[Description]"
    [test data]
    expected_result: [true|false]
    reason: "[Why]"

# METADATA
version: 1.0
created_by: "[Name/Team]"
approved_by: "[Name/Team]"
last_modified: "YYYY-MM-DD"
---
```

---

## Next Steps

### Phase 1: Discovery (Week 1)
- [ ] Identify all shared document types (Marketing, Legal, Product teams)
- [ ] Prioritize by business impact
- [ ] Schedule SME interviews

### Phase 2: Documentation (Week 2-3)
- [ ] Interview SMEs using questionnaire
- [ ] Document eligibility rules for each document
- [ ] Define test scenarios
- [ ] Get business approval

### Phase 3: Technical Review (Week 4)
- [ ] Review data source availability
- [ ] Validate API endpoints exist
- [ ] Check performance impact
- [ ] Security review

### Phase 4: Implementation (Week 5-8)
- [ ] Implement rules in order of priority
- [ ] Create test data
- [ ] Execute test scenarios
- [ ] Deploy to QA

### Phase 5: Validation (Week 9-10)
- [ ] QA testing with real data
- [ ] UAT with business users
- [ ] Performance testing
- [ ] Production deployment

---

## Appendix: Operator Reference

### Comparison Operators

| Operator | Description | Example | Data Types |
|----------|-------------|---------|------------|
| EQUALS | Exact match | status = "ACTIVE" | All |
| NOT_EQUALS | Not equal | status ≠ "CLOSED" | All |
| GREATER_THAN | Numeric greater than | balance > 10000 | Number |
| LESS_THAN | Numeric less than | age < 65 | Number |
| GREATER_THAN_OR_EQUALS | Numeric ≥ | creditScore ≥ 700 | Number |
| LESS_THAN_OR_EQUALS | Numeric ≤ | riskScore ≤ 50 | Number |
| IN | Value in list | tier IN ["GOLD", "PLATINUM"] | All |
| NOT_IN | Value not in list | state NOT IN ["CA", "NY"] | All |
| CONTAINS | String contains | name CONTAINS "Premium" | String |
| NOT_CONTAINS | String not contains | status NOT CONTAINS "CLOSED" | String |
| STARTS_WITH | String starts with | productCode STARTS WITH "CC-" | String |
| ENDS_WITH | String ends with | email ENDS WITH "@company.com" | String |
| IS_NULL | Value is null | middleName IS NULL | All |
| IS_NOT_NULL | Value is not null | ssn IS NOT NULL | All |
| DATE_BEFORE | Date before | openDate < "2020-01-01" | Date |
| DATE_AFTER | Date after | enrollmentDate > "2024-01-01" | Date |
| MONTH_EQUALS | Month of date equals | openDate.month = 12 | Date |
| REGEX_MATCH | Matches regex | zipCode MATCHES "^94\d{3}$" | String |

---

**Document Owner:** Technical Team + Product Team
**Review Frequency:** Quarterly
**Last Updated:** 2025-11-09
