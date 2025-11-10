# Shared Document Catalog - Excel Template Specification

**Purpose:** Specification for Excel workbook to collect shared document eligibility information
**Format:** Multi-sheet Excel workbook (.xlsx)
**Target Users:** Product Owners, Business Analysts, SMEs

---

## Excel Workbook Structure

### Sheet 1: INSTRUCTIONS
**Purpose:** How to use this workbook

### Sheet 2: DOCUMENT_LIST
**Purpose:** Master list of all shared documents

### Sheet 3: DOCUMENT_DETAILS
**Purpose:** One row per document with all metadata

### Sheet 4: ELIGIBILITY_CONDITIONS
**Purpose:** One row per condition across all documents

### Sheet 5: DATA_SOURCES
**Purpose:** API endpoints and field mappings

### Sheet 6: TEST_SCENARIOS
**Purpose:** Test cases for each document

### Sheet 7: LOOKUPS
**Purpose:** Dropdown values and reference data

---

## Detailed Sheet Specifications

### SHEET 1: INSTRUCTIONS

```
Row 1: SHARED DOCUMENT ELIGIBILITY CATALOG
Row 2: How to Use This Workbook
Row 3: [blank]

Row 4: STEP 1: Add all shared documents to DOCUMENT_LIST sheet
Row 5: - List each shared document type (Privacy Policy, Cardholder Agreement, etc.)
Row 6: - Assign a unique Document ID (e.g., DOC-001, DOC-002)
Row 7: - Mark priority (High/Medium/Low)
Row 8: [blank]

Row 9: STEP 2: Complete DOCUMENT_DETAILS for each document
Row 10: - Fill in metadata, business context, ownership
Row 11: - Select sharing scope from dropdown
Row 12: - Use dropdowns wherever possible
Row 13: [blank]

Row 14: STEP 3: Define eligibility conditions in ELIGIBILITY_CONDITIONS
Row 15: - For custom rules only
Row 16: - One row per condition
Row 17: - Link to Document ID
Row 18: - Select operator from dropdown
Row 19: [blank]

Row 20: STEP 4: Map data sources in DATA_SOURCES
Row 21: - Identify which APIs are needed
Row 22: - Specify endpoint and response fields
Row 23: - Note cache requirements
Row 24: [blank]

Row 25: STEP 5: Create test scenarios in TEST_SCENARIOS
Row 26: - At least 2 scenarios per document (positive & negative)
Row 27: - Include edge cases
Row 28: - Document expected results
Row 29: [blank]

Row 30: REFERENCE:
Row 31: - See LOOKUPS sheet for dropdown values
Row 32: - Colored cells indicate required fields
Row 33: - Use data validation to prevent errors
Row 34: - Export to YAML/JSON for implementation
Row 35: [blank]

Row 36: SUPPORT:
Row 37: Questions? Contact: [Technical Team Email]
Row 38: Documentation: docs/guides/SHARED_DOCUMENT_ELIGIBILITY_CATALOG.md
```

---

### SHEET 2: DOCUMENT_LIST

**Purpose:** Quick inventory of all shared documents

**Columns:**

| Column | Header | Data Type | Required | Dropdown Source | Notes |
|--------|--------|-----------|----------|-----------------|-------|
| A | Document ID | Text | Yes | - | Format: DOC-001, DOC-002, etc. |
| B | Document Name | Text | Yes | - | e.g., "Privacy Policy 2024" |
| C | Document Type | Dropdown | Yes | LOOKUPS!A:A | disclosure, agreement, notice, etc. |
| D | Line of Business | Dropdown | Yes | LOOKUPS!B:B | credit_card, digital_banking, etc. |
| E | Sharing Scope | Dropdown | Yes | LOOKUPS!C:C | all, credit_card_account_only, custom_rule |
| F | Regulatory? | Dropdown | Yes | LOOKUPS!D:D | Yes / No |
| G | Owner Department | Dropdown | Yes | LOOKUPS!E:E | Legal, Marketing, Product, etc. |
| H | Status | Dropdown | Yes | LOOKUPS!F:F | Draft, In Review, Approved, Implemented |
| I | Priority | Dropdown | Yes | LOOKUPS!G:G | High, Medium, Low |
| J | Estimated Volume (%) | Number | No | - | % of customers expected to see this |
| K | SME Name | Text | Yes | - | Subject matter expert |
| L | Interview Date | Date | No | - | When SME was interviewed |
| M | Notes | Text | No | - | Any additional comments |

**Data Validation:**
- Document ID: Must be unique, format DOC-###
- Estimated Volume: 0-100
- All dropdowns validated against LOOKUPS sheet

**Conditional Formatting:**
- Green: Status = "Approved"
- Yellow: Status = "In Review"
- Red: Status = "Draft"
- Blue highlight: Priority = "High"

---

### SHEET 3: DOCUMENT_DETAILS

**Purpose:** Complete metadata for each document

**Columns:**

| Column | Header | Data Type | Required | Width | Notes |
|--------|--------|-----------|----------|-------|-------|
| A | Document ID | Dropdown | Yes | 15 | Links to DOCUMENT_LIST |
| B | Document Name | Formula | Auto | 40 | =VLOOKUP(A2,DOCUMENT_LIST!A:B,2,FALSE) |
| C | Category | Dropdown | Yes | 20 | Benefits, Fees, Terms, Privacy, etc. |
| D | Subcategory | Text | No | 20 | More specific classification |
| E | Description | Text (long) | Yes | 60 | What this document is about |
| F | Regulatory | Dropdown | Yes | 12 | Yes / No |
| G | Regulation Name | Text | Conditional | 30 | Required if F="Yes". TILA, CCPA, etc. |
| H | Effective Date | Date | Yes | 15 | When document becomes active |
| I | Valid Until | Date | No | 15 | Expiration date (blank = no expiration) |
| J | Owner Dept | Dropdown | Yes | 20 | From LOOKUPS!E:E |
| K | Contact Person | Text | Yes | 25 | Name and email |
| L | Business Justification | Text (long) | Yes | 80 | Why show this document? |
| M | Target Audience | Text | Yes | 40 | Who should see it? |
| N | Expected Volume | Number | No | 15 | Estimated # of customers |
| O | Expected % | Formula | Auto | 10 | =N2/[total customers] |
| P | Revenue Opportunity | Currency | No | 20 | Annual revenue potential |
| Q | Compliance Requirement | Text | No | 40 | Legal requirement details |
| R | Audit Logging Required | Dropdown | Yes | 15 | Yes / No |
| S | Version | Number | Yes | 10 | 1.0, 1.1, etc. |
| T | Created By | Text | Yes | 25 | Name |
| U | Created Date | Date | Yes | 15 | When documented |
| V | Approved By | Text | No | 25 | Approver name |
| W | Approved Date | Date | No | 15 | Approval date |
| X | Last Modified | Date | Auto | 15 | =TODAY() |
| Y | Implementation Status | Dropdown | Yes | 20 | Not Started, In Progress, Testing, Live |
| Z | Notes | Text (long) | No | 60 | Any additional notes |

**Data Validation:**
- Document ID: Must exist in DOCUMENT_LIST
- Version: Format #.#
- Expected Volume: >= 0
- All dates: Valid date format
- Regulation Name: Required if Regulatory = "Yes"

**Conditional Formatting:**
- Required fields: Light yellow background
- Conditional required (Regulation Name): Light orange if Regulatory="Yes"
- Implementation Status:
  - Green: "Live"
  - Blue: "Testing"
  - Yellow: "In Progress"
  - Gray: "Not Started"

---

### SHEET 4: ELIGIBILITY_CONDITIONS

**Purpose:** Define all eligibility conditions (for custom_rule scope only)

**Columns:**

| Column | Header | Data Type | Required | Width | Notes |
|--------|--------|-----------|----------|-------|-------|
| A | Condition ID | Text | Yes | 15 | Format: COND-001, COND-002 |
| B | Document ID | Dropdown | Yes | 15 | Links to DOCUMENT_LIST |
| C | Document Name | Formula | Auto | 40 | =VLOOKUP(B2,DOCUMENT_LIST!A:B,2,FALSE) |
| D | Condition Priority | Number | Yes | 10 | 1=Critical, 2=Important, 3=Nice to have |
| E | Condition Description | Text | Yes | 60 | Human-readable description |
| F | Field Name | Text | Yes | 25 | e.g., accountBalance, customerTier |
| G | Data Source | Dropdown | Yes | 25 | See DATA_SOURCES sheet |
| H | Operator | Dropdown | Yes | 25 | EQUALS, GREATER_THAN, IN, etc. |
| I | Value | Text | Yes | 30 | The comparison value |
| J | Value Type | Dropdown | Yes | 15 | String, Number, Boolean, Array |
| K | Logical Operator | Dropdown | Yes | 15 | AND / OR |
| L | Required? | Dropdown | Yes | 12 | Yes / No |
| M | Error Message | Text | Yes | 50 | Message if condition fails |
| N | Example (Pass) | Text | No | 40 | Example data that passes |
| O | Example (Fail) | Text | No | 40 | Example data that fails |
| P | Notes | Text | No | 60 | Any clarifications |

**Data Validation:**
- Condition ID: Unique
- Document ID: Must exist in DOCUMENT_LIST
- Operator: From LOOKUPS!H:H
- Logical Operator: AND / OR
- Value Type: From LOOKUPS!I:I

**Conditional Formatting:**
- Priority 1: Red text (Critical)
- Priority 2: Orange text (Important)
- Priority 3: Blue text (Nice to have)
- Group by Document ID (alternate row colors)

**Example Rows:**

| Condition ID | Document ID | Field Name | Operator | Value | Logical Operator |
|-------------|-------------|------------|----------|-------|------------------|
| COND-001 | DOC-001 | customerTier | IN | ["PLATINUM","BLACK"] | AND |
| COND-002 | DOC-001 | accountBalance | GREATER_THAN | 10000 | AND |
| COND-003 | DOC-001 | accountStatus | EQUALS | ACTIVE | - |

---

### SHEET 5: DATA_SOURCES

**Purpose:** Map data sources and API endpoints

**Columns:**

| Column | Header | Data Type | Required | Width | Notes |
|--------|--------|-----------|----------|-------|-------|
| A | Data Source ID | Text | Yes | 20 | e.g., customer_service, account_service |
| B | Data Source Name | Text | Yes | 30 | Human-readable name |
| C | Source Type | Dropdown | Yes | 20 | REST_API, DATABASE, CACHE |
| D | API Endpoint | Text | Yes* | 80 | Full URL (* if REST_API) |
| E | HTTP Method | Dropdown | Yes* | 15 | GET, POST (* if REST_API) |
| F | Request Body | Text (JSON) | No | 60 | JSON template for POST |
| G | Timeout (ms) | Number | Yes | 15 | Default: 5000 |
| H | Retry Attempts | Number | Yes | 15 | Default: 2 |
| I | Field Name | Text | Yes | 25 | Field extracted from response |
| J | JSON Path | Text | Yes | 40 | JSONPath expression |
| K | Data Type | Dropdown | Yes | 15 | String, Number, Boolean, Array, Object |
| L | Cache Enabled | Dropdown | Yes | 15 | Yes / No |
| M | Cache TTL (sec) | Number | Conditional | 15 | Required if L="Yes" |
| N | Response Time (ms) | Number | No | 15 | Average response time |
| O | Owner Team | Text | Yes | 25 | Team that owns this API |
| P | API Documentation | URL | No | 60 | Link to API docs |
| Q | Notes | Text | No | 60 | Any special considerations |

**Multi-row per Data Source:**
Each data source can have multiple rows (one per field extracted)

**Example:**

| Data Source ID | API Endpoint | Field Name | JSON Path |
|---------------|--------------|------------|-----------|
| customer_service | GET /api/v1/customers/{customerId} | customerTier | $.data.tier |
| customer_service | GET /api/v1/customers/{customerId} | customerStatus | $.data.status |
| customer_service | GET /api/v1/customers/{customerId} | creditScore | $.data.creditScore |
| account_service | GET /api/v1/accounts/{accountId} | accountBalance | $.balance.currentBalance |
| account_service | GET /api/v1/accounts/{accountId} | accountStatus | $.accountStatus |

---

### SHEET 6: TEST_SCENARIOS

**Purpose:** Test cases for each document

**Columns:**

| Column | Header | Data Type | Required | Width | Notes |
|--------|--------|-----------|----------|-------|-------|
| A | Scenario ID | Text | Yes | 15 | TEST-001, TEST-002, etc. |
| B | Document ID | Dropdown | Yes | 15 | Links to DOCUMENT_LIST |
| C | Document Name | Formula | Auto | 40 | =VLOOKUP(B2,DOCUMENT_LIST!A:B,2,FALSE) |
| D | Scenario Type | Dropdown | Yes | 20 | Positive, Negative, Edge Case, Error |
| E | Scenario Name | Text | Yes | 50 | Brief description |
| F | Test Data (JSON) | Text (JSON) | Yes | 100 | Customer/account data |
| G | Expected Result | Dropdown | Yes | 20 | SHOW / DO NOT SHOW |
| H | Reason | Text | Yes | 80 | Why this result is expected |
| I | Validation Steps | Text (multi-line) | Yes | 100 | Step-by-step validation |
| J | Tested? | Dropdown | No | 12 | Yes / No / N/A |
| K | Test Date | Date | No | 15 | When tested |
| L | Test Result | Dropdown | No | 15 | Pass / Fail / Blocked |
| M | Tester Name | Text | No | 25 | Who tested |
| N | Notes | Text | No | 60 | Any issues or observations |

**Data Validation:**
- Document ID: Must exist in DOCUMENT_LIST
- Scenario Type: From LOOKUPS!J:J
- Expected Result: SHOW / DO NOT SHOW
- Test Result: Pass / Fail / Blocked

**Conditional Formatting:**
- Scenario Type color coding:
  - Green: Positive
  - Red: Negative
  - Orange: Edge Case
  - Purple: Error
- Test Result:
  - Green: Pass
  - Red: Fail
  - Yellow: Blocked

**Example Test Data (JSON format in column F):**
```json
{
  "customerId": "test-001",
  "accountId": "acct-001",
  "customerTier": "PLATINUM",
  "accountBalance": 15000,
  "accountStatus": "ACTIVE"
}
```

---

### SHEET 7: LOOKUPS

**Purpose:** Reference data for dropdowns

**Layout:** Multiple lookup lists side-by-side

**Column A: Document Types**
```
Header: DOCUMENT_TYPE
disclosure
agreement
notice
statement
policy
terms_and_conditions
regulatory
marketing
benefits
fees
```

**Column B: Line of Business**
```
Header: LINE_OF_BUSINESS
credit_card
digital_banking
enterprise
all
savings
checking
loan
investment
```

**Column C: Sharing Scope**
```
Header: SHARING_SCOPE
all
credit_card_account_only
digital_bank_customer_only
enterprise_customer_only
custom_rule
```

**Column D: Yes/No**
```
Header: YES_NO
Yes
No
```

**Column E: Owner Department**
```
Header: OWNER_DEPT
Legal
Compliance
Marketing
Product
Operations
IT
Risk Management
Customer Service
```

**Column F: Status**
```
Header: STATUS
Draft
In Review
Approved
Rejected
Implemented
Live
Deprecated
```

**Column G: Priority**
```
Header: PRIORITY
High
Medium
Low
```

**Column H: Operators**
```
Header: OPERATOR
EQUALS
NOT_EQUALS
GREATER_THAN
LESS_THAN
GREATER_THAN_OR_EQUALS
LESS_THAN_OR_EQUALS
IN
NOT_IN
CONTAINS
NOT_CONTAINS
STARTS_WITH
ENDS_WITH
IS_NULL
IS_NOT_NULL
DATE_BEFORE
DATE_AFTER
MONTH_EQUALS
REGEX_MATCH
```

**Column I: Value Types**
```
Header: VALUE_TYPE
String
Number
Boolean
Date
Array
Object
```

**Column J: Scenario Types**
```
Header: SCENARIO_TYPE
Positive
Negative
Edge Case
Error Handling
```

**Column K: Source Types**
```
Header: SOURCE_TYPE
REST_API
DATABASE
CACHE
```

**Column L: HTTP Methods**
```
Header: HTTP_METHOD
GET
POST
PUT
DELETE
```

**Column M: Logical Operators**
```
Header: LOGICAL_OPERATOR
AND
OR
```

**Column N: Implementation Status**
```
Header: IMPL_STATUS
Not Started
In Progress
Testing
Live
Deferred
```

---

## Excel Features to Enable

### 1. Data Validation
All dropdown columns linked to LOOKUPS sheet

### 2. Conditional Formatting
- Required fields: Light yellow fill
- Status-based coloring
- Priority-based text coloring

### 3. Formulas
- Auto-populate Document Name from Document ID
- Calculate percentages
- Auto-date last modified

### 4. Named Ranges
```
DocumentList = DOCUMENT_LIST!$A$2:$A$1000
DocumentTypes = LOOKUPS!$A$2:$A$20
Operators = LOOKUPS!$H$2:$H$25
```

### 5. Protection
- Lock formula cells
- Protect LOOKUPS sheet
- Allow editing only in data entry cells

### 6. Filtering
- Enable AutoFilter on all sheets
- Freeze top row (headers)
- Freeze first 3 columns (IDs and names)

---

## Excel Template Instructions for Creation

### Step 1: Create Workbook
1. Create new Excel workbook
2. Create 7 sheets with names above
3. Set tab colors:
   - INSTRUCTIONS: Blue
   - DOCUMENT_LIST: Green
   - DOCUMENT_DETAILS: Yellow
   - ELIGIBILITY_CONDITIONS: Orange
   - DATA_SOURCES: Purple
   - TEST_SCENARIOS: Red
   - LOOKUPS: Gray

### Step 2: Add Headers and Formatting
1. Add column headers per specifications
2. Set column widths
3. Bold headers, freeze panes
4. Add background color to headers (dark blue, white text)

### Step 3: Configure Data Validation
1. Create named ranges from LOOKUPS sheet
2. Add dropdown data validation to all specified columns
3. Add input messages ("Select from dropdown")
4. Add error alerts ("Invalid value")

### Step 4: Add Formulas
1. DOCUMENT_DETAILS!B2: `=VLOOKUP(A2,DOCUMENT_LIST!$A:$B,2,FALSE)`
2. DOCUMENT_DETAILS!O2: `=IF(N2="","",N2/1000000)` (assuming 1M customers)
3. DOCUMENT_DETAILS!X2: `=TODAY()`
4. Similar formulas for auto-populated fields

### Step 5: Conditional Formatting
1. Apply status-based colors
2. Apply priority-based colors
3. Highlight required fields
4. Add data bars for Expected Volume columns

### Step 6: Protection
1. Protect LOOKUPS sheet (read-only)
2. Lock formula cells
3. Set password protection (optional)

### Step 7: Testing
1. Add sample data (3-5 documents)
2. Test all dropdowns
3. Test formulas
4. Verify data validation

---

## Export Instructions

### Export to CSV (for each sheet)
File → Save As → CSV

Produces:
- `DOCUMENT_LIST.csv`
- `DOCUMENT_DETAILS.csv`
- `ELIGIBILITY_CONDITIONS.csv`
- `DATA_SOURCES.csv`
- `TEST_SCENARIOS.csv`

### Export to YAML/JSON
Use Python script or online converter:
1. Export to CSV
2. Convert CSV to JSON
3. Transform to YAML format per catalog template

---

## Python Script for Excel → YAML Conversion

```python
# See separate file: convert_excel_to_yaml.py
# Reads Excel file, outputs YAML catalog entries
```

---

## Maintenance

### Version Control
- Save Excel file in Git repository
- Track changes in commit messages
- Use Excel's "Track Changes" feature for collaboration

### Review Cycle
- Weekly: Update status of documents in review
- Monthly: Review and update test scenarios
- Quarterly: Archive implemented documents, add new ones

---

**File Name:** `Shared_Document_Eligibility_Catalog.xlsx`
**Location:** `docs/project-management/`
**Version:** 1.0
**Last Updated:** 2025-11-09
