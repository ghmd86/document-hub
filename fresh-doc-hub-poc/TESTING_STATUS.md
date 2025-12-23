# Document Hub POC - Testing Status

## Current State

### Database Integration: ✅ COMPLETED
- **Database**: H2 in-memory database (chosen as PostgreSQL was not installed)
- **Status**: Schema and sample data configured
- **Location**:
  - Schema: `src/main/resources/schema.sql`
  - Data: `src/main/resources/data.sql`
  - Config: `src/main/java/com/documenthub/config/DatabaseConfig.java`

### Sample Data Loaded
- **5 Templates**:
  1. Privacy Policy (Shared with ALL)
  2. Credit Card Statement (Account-Specific)
  3. Balance Transfer Offer (Shared by Account Type)
  4. VIP Exclusive Offer (Shared with Custom Rules)
  5. Regulatory Disclosure D164 (Shared with Custom Rules)

- **6 Documents**:
  1. privacy_policy_2024.pdf
  2. statement_jan_2024.pdf (Account 1)
  3. statement_jan_2024_acct2.pdf (Account 2)
  4. balance_transfer_offer_q1_2024.pdf
  5. vip_exclusive_offer.pdf
  6. disclosure_d164.pdf

### Recent Fixes Applied
- ✅ Fixed PostgreSQL to H2 SQL syntax (TRUNCATE, ::uuid, ::jsonb, EXTRACT)
- ✅ Created JSON converters for H2 (JsonNodeReadingConverter, JsonNodeWritingConverter)
- ✅ Fixed timestamp type mismatch: Changed `OffsetDateTime` to `LocalDateTime` in:
  - `src/main/java/com/documenthub/entity/MasterTemplateDefinitionEntity.java`
  - `src/main/java/com/documenthub/entity/StorageIndexEntity.java`

## Known Issues

### Multiple Maven Processes Running
There are multiple background Maven/Spring Boot processes still running:
- Background Bash 16178d (timeout 15 mvn spring-boot:run)
- Background Bash a52451 (mvn spring-boot:run)
- Background Bash 48d662 (mvn spring-boot:run)
- Background Bash 2080f1 (mvn spring-boot:run)
- Background Bash cb17c1 (mvn spring-boot:run)

**Port 8080 is currently in use** - need to kill these processes before restarting.

## Quick Start Instructions for Next Session

### Step 1: Kill Running Processes
```bash
# Windows command to kill all Java processes
taskkill /F /IM java.exe

# Or use Claude's KillShell tool on each background process ID
```

### Step 2: Start Application
```bash
cd C:\Users\ghmd8\Documents\AI\fresh-doc-hub-poc
mvn spring-boot:run
```

Wait for this message in logs:
```
Started DocumentHubApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

### Step 3: Test API Endpoints

All requests use base URL: `http://localhost:8080/api/v1/documents-enquiry`

Required headers for all requests:
```
Content-Type: application/json
X-version: 1
X-correlation-id: test-123
X-requestor-id: 12345678-1234-1234-1234-123456789012
X-requestor-type: CUSTOMER
```

#### Test Scenario 1: VIP Customer in US_WEST Region
**Expected**: 4 documents
- Privacy Policy (shared with ALL)
- Statement for Account 1 (account-specific)
- Balance Transfer Offer (shared by account type: credit_card)
- VIP Exclusive Offer (custom rules: VIP + US_WEST)

```bash
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-123" \
  -H "X-requestor-id: 12345678-1234-1234-1234-123456789012" \
  -H "X-requestor-type: CUSTOMER" \
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000001\"]}"
```

#### Test Scenario 2: Standard Customer in US_EAST Region
**Expected**: 3 documents
- Privacy Policy (shared with ALL)
- Statement for Account 2 (account-specific)
- Balance Transfer Offer (shared by account type: credit_card)

```bash
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-124" \
  -H "X-requestor-id: 12345678-1234-1234-1234-123456789012" \
  -H "X-requestor-type: CUSTOMER" \
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000001\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000002\"]}"
```

#### Test Scenario 3: Digital Bank Customer (No Credit Card)
**Expected**: 1 document
- Privacy Policy (shared with ALL)

```bash
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-125" \
  -H "X-requestor-id: 12345678-1234-1234-1234-123456789012" \
  -H "X-requestor-type: CUSTOMER" \
  -d "{\"customerId\": \"cccc0000-0000-0000-0000-000000000002\", \"accountId\": [\"aaaa0000-0000-0000-0000-000000000003\"]}"
```

## Test Data Reference

### Mock Accounts (from AccountRepository.java)
```java
Account 1: aaaa0000-0000-0000-0000-000000000001
  - Customer: cccc0000-0000-0000-0000-000000000001
  - Type: credit_card
  - Segment: VIP
  - Region: US_WEST

Account 2: aaaa0000-0000-0000-0000-000000000002
  - Customer: cccc0000-0000-0000-0000-000000000001
  - Type: credit_card
  - Segment: STANDARD
  - Region: US_EAST

Account 3: aaaa0000-0000-0000-0000-000000000003
  - Customer: cccc0000-0000-0000-0000-000000000002
  - Type: checking
  - Segment: STANDARD
  - Region: US_CENTRAL

Account 4: aaaa0000-0000-0000-0000-000000000004
  - Customer: cccc0000-0000-0000-0000-000000000002
  - Type: savings
  - Segment: STANDARD
  - Region: US_CENTRAL
```

### Document Storage Index IDs
```
a0000000-0000-0000-0000-000000000001 - Privacy Policy
a0000000-0000-0000-0000-000000000002 - Statement Account 1
a0000000-0000-0000-0000-000000000003 - Statement Account 2
a0000000-0000-0000-0000-000000000004 - Balance Transfer Offer
a0000000-0000-0000-0000-000000000005 - VIP Exclusive Offer
a0000000-0000-0000-0000-000000000006 - Regulatory Disclosure D164
```

## Verification Checklist

- [ ] Kill existing Java processes
- [ ] Start application successfully
- [ ] Verify database initialization (check logs for "Executed SQL script")
- [ ] Test Scenario 1 (VIP customer) - expect 4 documents
- [ ] Test Scenario 2 (Standard customer) - expect 3 documents
- [ ] Test Scenario 3 (Digital bank customer) - expect 1 document
- [ ] Verify response structure matches API spec
- [ ] Check logs for any errors or warnings

## Unit Test Coverage

### Test Summary (202 tests total)

| Test Class | Tests | Description |
|------------|-------|-------------|
| `StorageIndexDaoTest` | 9 | Single document flag overlap detection |
| `DocumentEnquiryProcessorTest` | 10 | Document enquiry flow with account/customer |
| `DocumentManagementProcessorTest` | 8 | Upload flow with single_document_flag |
| `AccountMetadataServiceTest` | 14 | Account metadata retrieval |
| `ArrayUnwrappingTest` | 4 | JSON array unwrapping |
| `ConfigurableDataExtractionServiceTest` | 11 | Data extraction from external APIs |
| `DocumentAccessControlServiceTest` | 20 | Access control and permissions |
| `DocumentMatchingServiceTest` | 15 | Document matching logic |
| `DocumentResponseBuilderTest` | 21 | Response building and pagination |
| `DocumentUploadServiceTest` | 15 | Upload service |
| `DocumentValidityServiceTest` | 22 | Document validity checks |
| `JsonPathExtractionTest` | 6 | JSONPath extraction |
| `RuleEvaluationServiceTest` | 47 | Rule evaluation engine |

### Single Document Flag Tests (New - December 2025)

**StorageIndexDaoTest:**
- ✅ `shouldUpdateEndDate` - Verifies end_date update
- ✅ `shouldReturnOnlyAccessibleDocuments` - Filters inaccessible docs
- ✅ `shouldReturnEmptyWhenNoAccessibleDocuments` - Empty results
- ✅ `shouldUpdateDocsWithNullEndDate` - null = overlapping
- ✅ `shouldUpdateDocsWithEndDateAfterNewStartDate` - end > start = overlapping
- ✅ `shouldNotUpdateDocsWithEndDateBeforeNewStartDate` - end < start = NOT overlapping
- ✅ `shouldOnlyUpdateOverlappingDocsWhenMixed` - Mixed scenario
- ✅ `shouldReturnZeroWhenNoDocumentsFound` - No docs
- ✅ `shouldTreatAllAsOverlappingWhenNewStartDateIsNull` - null start = all overlap

**DocumentManagementProcessorTest:**
- ✅ `shouldCloseExistingDocsWhenSingleDocFlagTrue` - Flag true = close
- ✅ `shouldNotCloseExistingDocsWhenSingleDocFlagFalse` - Flag false = no close
- ✅ `shouldNotCloseExistingDocsWhenSingleDocFlagNull` - Flag null = no close
- ✅ `shouldNotCloseWhenReferenceKeyIsNull` - Missing ref_key
- ✅ `shouldNotCloseWhenReferenceKeyTypeIsNull` - Missing ref_key_type
- ✅ `shouldUseActiveStartDateAsNewEndDate` - Uses provided start_date
- ✅ `shouldUseCurrentTimeWhenActiveStartDateIsNull` - Falls back to current time
- ✅ `shouldProceedWithUploadWhenNoDocsToClose` - Continue even if 0 closed

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest="StorageIndexDaoTest"

# Run tests with verbose output
mvn test -Dtest="**/StorageIndexDaoTest,**/DocumentManagementProcessorTest"
```

---

## Notes

- Application uses port 8080
- H2 database is in-memory (data resets on each restart)
- All test data is automatically loaded from `data.sql` on startup
- JSON fields are stored as VARCHAR(4000) in H2
- Custom converters handle JsonNode ↔ String conversion
