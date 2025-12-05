# Document Hub POC - Implementation Summary

## ✅ What Was Built

A complete, functional POC for a document retrieval system with dynamic rule-based access control.

## 📦 Deliverables

### 1. Complete Spring Boot Application
- **Main Class**: `DocumentHubApplication.java`
- **Framework**: Spring Boot 2.7.18 with WebFlux (Reactive)
- **Database**: R2DBC PostgreSQL (Reactive Database Access)
- **API Spec**: OpenAPI 3.0

### 2. Core Components

#### Entities (Database Models)
- ✅ `MasterTemplateDefinitionEntity` - Template definitions with access control rules
- ✅ `StorageIndexEntity` - Document storage index with metadata

#### Repositories (Data Access Layer)
- ✅ `MasterTemplateRepository` - Query templates with date filtering
- ✅ `StorageIndexRepository` - Query documents (account-specific & shared)

#### Models (Business Objects)
- ✅ `AccountMetadata` - Account information for rule evaluation
- ✅ `AccessControl` - Access control configuration
- ✅ `EligibilityCriteria` - Rule grouping with AND/OR logic
- ✅ `Rule` - Individual rule definition
- ✅ Generated OpenAPI models (17 classes)

#### Services (Business Logic)
- ✅ `AccountMetadataService` - Mock account data provider
- ✅ `RuleEvaluationService` - Dynamic rule evaluation engine
- ✅ `DocumentEnquiryService` - Main business logic orchestrator
- ✅ `ConfigurableDataExtractionService` - Dynamic API data extraction for CUSTOM_RULES templates

#### Controller (REST API)
- ✅ `DocumentEnquiryController` - POST `/documents-enquiry` endpoint

### 3. Configuration Files
- ✅ `pom.xml` - Maven configuration with all dependencies
- ✅ `application.properties` - Application configuration
- ✅ `database.sql` - PostgreSQL schema
- ✅ `data.sql` - Sample test data

### 4. Documentation
- ✅ `README.md` - Complete usage guide
- ✅ `IMPLEMENTATION_SUMMARY.md` (this file)

## 🎯 Features Implemented

### Document Sharing Strategies

#### 1. Account-Specific Documents (is_shared = false)
- Documents tied to specific `account_key`
- Only accessible by the owning account
- **Example**: Monthly statements

#### 2. Shared with ALL (sharing_scope = "ALL")
- Accessible to all customers
- No access control evaluation needed
- **Example**: Privacy policy, terms and conditions

#### 3. Shared by Account Type (sharing_scope = "ACCOUNT_TYPE")
- Accessible to accounts of specific types
- Rule-based evaluation using account metadata
- **Example**: Credit card promotional offers (only for credit_card accounts)

#### 4. Shared with Custom Rules (sharing_scope = "CUSTOM_RULES")
- Complex rule evaluation using JSON-defined criteria
- Supports multiple conditions with AND/OR logic
- **Example**: VIP offers (VIP segment + US_WEST region)

#### 5. Dynamic Data Extraction (CUSTOM_RULES Templates)
- Templates with `sharing_scope = "CUSTOM_RULES"` can include `data_extraction_config` (JSONB)
- Configurable data extraction from external APIs before rule evaluation
- Extracted data is merged into the request context for rule evaluation
- Supports JSONPath expressions for extracting specific fields from API responses
- **Example**: Extract disclosure code from external API, then check if it matches document metadata

**Data Extraction Flow:**
1. Template with CUSTOM_RULES is identified
2. `data_extraction_config` defines API endpoint, method, headers, and field mappings
3. Service makes external API call (GET/POST)
4. JSONPath expressions extract specific values from API response
5. Extracted fields are added to request context
6. Access control rules are evaluated with enhanced context
7. Documents are returned only if rules pass

### Rule Evaluation Engine

#### Supported Operators
- **Equality**: `EQUALS`, `NOT_EQUALS`
- **Set Operations**: `IN`, `NOT_IN`
- **Numeric Comparison**: `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`
- **String Operations**: `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`

#### Supported Fields
**From Account Metadata:**
- `accountType` (e.g., credit_card, digital_bank, savings)
- `region` (e.g., US_WEST, US_EAST)
- `state` (e.g., CA, NY, TX)
- `customerSegment` (e.g., VIP, ENTERPRISE, STANDARD)
- `lineOfBusiness`
- `accountId`, `customerId`

**From Request Context:**
- `$metadata.<field>` - Document metadata
- `$request.<field>` - Request parameters

#### Logic Combinations
- **AND**: All rules must pass
- **OR**: At least one rule must pass

## 📊 Sample Data

### Templates (5)
1. **Privacy Policy** - Shared with ALL
2. **Credit Card Statement** - Account-specific
3. **Balance Transfer Offer** - Shared by account type (credit_card)
4. **VIP Exclusive Offer** - Custom rules (VIP + US_WEST)
5. **Regulatory Disclosure** - Custom rules (disclosure code match)

### Documents (6)
1. Privacy Policy PDF
2. Statement for Account 1
3. Statement for Account 2
4. Balance Transfer Offer
5. VIP Exclusive Offer
6. Disclosure D164

### Mock Accounts (4)
| Account | Customer | Type | Segment | Region | Age |
|---------|----------|------|---------|--------|-----|
| ...001 | ...001 | credit_card | VIP | US_WEST | 2 years |
| ...002 | ...001 | credit_card | STANDARD | US_WEST | 3 months |
| ...003 | ...002 | digital_bank | ENTERPRISE | US_EAST | 1 year |
| ...004 | ...003 | savings | STANDARD | US_EAST | 1 month |

## 🔧 Configuration Examples

### Example 1: VIP Customers in Specific Region
```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "customerSegment",
        "operator": "EQUALS",
        "value": "VIP"
      },
      {
        "field": "region",
        "operator": "EQUALS",
        "value": "US_WEST"
      }
    ]
  }
}
```

### Example 2: Multiple Account Types
```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "accountType",
        "operator": "IN",
        "value": ["credit_card", "digital_bank"]
      }
    ]
  }
}
```

### Example 3: OR Logic Example
```json
{
  "eligibilityCriteria": {
    "operator": "OR",
    "rules": [
      {
        "field": "customerSegment",
        "operator": "EQUALS",
        "value": "VIP"
      },
      {
        "field": "customerSegment",
        "operator": "EQUALS",
        "value": "ENTERPRISE"
      }
    ]
  }
}
```

### Example 4: Data Extraction Configuration (CUSTOM_RULES)
```json
{
  "data_extraction_config": {
    "apiEndpoint": "https://api.example.com/card-details",
    "method": "GET",
    "headers": {
      "Authorization": "Bearer ${token}",
      "Content-Type": "application/json"
    },
    "fieldMappings": [
      {
        "sourcePath": "$.data.disclosureCode",
        "targetField": "disclosureCode"
      },
      {
        "sourcePath": "$.data.cardType",
        "targetField": "cardType"
      }
    ]
  },
  "access_control": {
    "eligibilityCriteria": {
      "operator": "AND",
      "rules": [
        {
          "field": "disclosureCode",
          "operator": "EQUALS",
          "value": "D164"
        }
      ]
    }
  }
}
```

This configuration:
1. Calls external API to get card details
2. Extracts `disclosureCode` from API response using JSONPath
3. Uses extracted `disclosureCode` in access control rule
4. Only shows document if disclosureCode matches "D164"

## 🚀 How to Run

```bash
# 1. Setup database
createdb document_hub
psql -d document_hub -f src/main/resources/database.sql
psql -d document_hub -f src/main/resources/data.sql

# 2. Build
mvn clean install

# 3. Run
mvn spring-boot:run

# 4. Test (with required headers)
curl -X POST http://localhost:8080/api/v1/documents-enquiry \
  -H "Content-Type: application/json" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-123" \
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440002" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId": "cccc0000-0000-0000-0000-000000000001", "accountId": ["aaaa0000-0000-0000-0000-000000000001"]}'
```

### Required HTTP Headers
All API requests must include these headers:
- **X-version**: API version (Integer, e.g., `1`)
- **X-correlation-id**: Correlation ID for request tracing (String, e.g., `test-123`)
- **X-requestor-id**: UUID of the requestor (UUID, e.g., `550e8400-e29b-41d4-a716-446655440002`)
- **X-requestor-type**: Type of requestor (Enum: `CUSTOMER`, `AGENT`, `SYSTEM`)

### Testing with Postman
A Postman collection is included: `Document_Hub_POC.postman_collection.json`
- Import the collection into Postman
- Two pre-configured scenarios included with all required headers
- Endpoints configured to use `http://localhost:8080/api/v1/documents-enquiry`

## 📈 Test Scenarios & Expected Results

### Scenario 1: VIP Customer, Credit Card, US_WEST
**Request Account**: `aaaa0000-0000-0000-0000-000000000001`

**Expected Documents**: 4
1. ✅ Privacy Policy (ALL)
2. ✅ Account Statement (account-specific)
3. ✅ Balance Transfer Offer (credit_card type)
4. ✅ VIP Exclusive Offer (VIP + US_WEST)

### Scenario 2: Standard Customer, Credit Card, US_WEST
**Request Account**: `aaaa0000-0000-0000-0000-000000000002`

**Expected Documents**: 3
1. ✅ Privacy Policy (ALL)
2. ✅ Account Statement for Account 2 (account-specific)
3. ✅ Balance Transfer Offer (credit_card type)
4. ❌ VIP Exclusive Offer (NOT VIP segment)

### Scenario 3: Enterprise Customer, Digital Bank, US_EAST
**Request Account**: `aaaa0000-0000-0000-0000-000000000003`

**Expected Documents**: 1
1. ✅ Privacy Policy (ALL)
2. ❌ Statements (no statements for this account)
3. ❌ Balance Transfer Offer (NOT credit_card type)
4. ❌ VIP Exclusive Offer (wrong segment & region)

## 🏗️ Architecture Highlights

### Reactive Stack
- **Spring WebFlux** for non-blocking I/O
- **R2DBC** for reactive database access
- **Reactor Core** for reactive streams (Mono/Flux)

### Separation of Concerns
- **Controllers**: HTTP request/response handling
- **Services**: Business logic and orchestration
- **Repositories**: Data access abstraction
- **Entities**: Database mapping
- **Models**: Business objects and DTOs

### Extensibility
- **Rule Engine**: Easy to add new operators and field types
- **Account Metadata**: Swappable with real API integration
- **Access Control**: JSON-based, no code changes for new rules

## 🔧 Technical Notes & Fixes

### PostgreSQL BIT/BOOLEAN Type Compatibility
**Issue**: PostgreSQL database columns `active_flag` and `shared_document_flag` are defined as BIT(1) type, but Java entities use `Boolean` type. This caused query errors:
```
operator does not exist: boolean = bit
```

**Solution**: Use PostgreSQL type casting in SQL queries to cast BIT columns to BOOLEAN before comparison:
```java
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag::boolean = true " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplates(Long currentDate);
```

**Pattern**: `column_name::boolean = true` - Applies to all BIT columns in queries

**Location**: `MasterTemplateRepository.java:20-24, 34-39`

## 🎓 Key Learnings & Design Decisions

### 1. Mock vs Real Integration
- **Decision**: Use mock `AccountMetadataService` for POC
- **Rationale**: Demonstrates concept without external dependencies
- **Future**: Easy to swap with real API/database integration

### 2. Rule Storage in JSON
- **Decision**: Store rules in JSONB column
- **Rationale**: Flexible, no schema changes for new rule types
- **Benefit**: Non-technical users can configure via JSON

### 3. Separate Queries vs Complex SQL
- **Decision**: Separate queries for account-specific and shared documents
- **Rationale**: Clearer logic, easier to test and debug
- **Trade-off**: Slightly more queries, but better maintainability

### 4. AND/OR at Top Level Only
- **Decision**: Single operator for all rules in criteria
- **Rationale**: Simpler implementation, covers most use cases
- **Future Enhancement**: Nested rule groups for complex logic

## 📝 Missing from POC (Production Requirements)

### Security
- [ ] Authentication/Authorization
- [ ] API key validation
- [ ] Rate limiting
- [ ] Input sanitization

### Performance
- [ ] Caching layer (Redis)
- [ ] Database indexing strategy
- [ ] Query optimization
- [ ] Connection pooling tuning

### Observability
- [ ] Metrics (Prometheus)
- [ ] Distributed tracing (Zipkin/Jaeger)
- [ ] Structured logging
- [ ] Health checks

### Testing
- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance tests
- [ ] Load tests

### Features
- [ ] Document versioning
- [ ] Date range filtering
- [ ] Advanced sorting
- [ ] Full-text search
- [ ] Bulk operations

## 💡 Next Steps for Production

1. **Replace Mock Service**
   - Integrate with real Account/Customer API
   - Add circuit breaker (Resilience4j)
   - Implement retry logic

2. **Add Caching**
   - Cache account metadata (Redis)
   - Cache template definitions
   - Cache-aside pattern

3. **Enhance Rule Engine**
   - Nested rule groups
   - Dynamic field resolution
   - Rule versioning
   - Rule testing UI

4. **Add Comprehensive Testing**
   - Unit tests for rule evaluation
   - Integration tests for all scenarios
   - Contract testing for APIs

5. **Implement Security**
   - OAuth2/JWT authentication
   - Role-based access control
   - API gateway integration

6. **Performance Optimization**
   - Database query optimization
   - Index strategy
   - Query result caching
   - Lazy loading

## 📊 Success Metrics

✅ **POC Success Criteria Met:**
1. ✅ Returns both account-specific and shared documents
2. ✅ Evaluates sharing rules (ALL/ACCOUNT_TYPE/CUSTOM_RULES)
3. ✅ Custom rules work correctly with multiple conditions
4. ✅ Supports AND/OR logic
5. ✅ Pagination implemented
6. ✅ Configurable via JSON (no code changes)
7. ✅ Clean architecture, easy to extend
8. ✅ Dynamic data extraction from external APIs (ConfigurableDataExtractionService)
9. ✅ PostgreSQL BIT/BOOLEAN compatibility resolved
10. ✅ Required HTTP headers validation implemented
11. ✅ Postman collection for API testing included

## 👥 For Non-Technical Users

### How to Configure New Rules

**Example: Share document with gold tier customers**
```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "customerSegment",
        "operator": "EQUALS",
        "value": "GOLD"
      }
    ]
  }
}
```

**Example: Share with California residents over 1 year**
```json
{
  "eligibilityCriteria": {
    "operator": "AND",
    "rules": [
      {
        "field": "state",
        "operator": "EQUALS",
        "value": "CA"
      }
    ]
  }
}
```

### Field Reference
- `accountType`: Type of account (credit_card, digital_bank, savings, etc.)
- `region`: Geographic region (US_WEST, US_EAST, etc.)
- `state`: US state code (CA, NY, TX, etc.)
- `customerSegment`: Customer tier (VIP, GOLD, ENTERPRISE, STANDARD)
- `lineOfBusiness`: Business line (CREDIT_CARD, BANKING, etc.)

### Operator Reference
- `EQUALS`: Exact match
- `IN`: Matches any value in list
- `GREATER_THAN`: Numeric comparison
- `CONTAINS`: String contains substring

---

**Implementation Date**: December 2, 2025
**Last Updated**: December 4, 2025
**Technology Stack**: Java 17, Spring Boot 2.7.18, WebFlux, R2DBC, PostgreSQL
**Status**: ✅ POC Complete and Functional

## Recent Updates (December 4, 2025)

1. **PostgreSQL BIT/BOOLEAN Compatibility Fix**
   - Fixed type mismatch in repository queries
   - Applied `::boolean` casting to BIT columns in SQL queries
   - File: `MasterTemplateRepository.java`

2. **ConfigurableDataExtractionService Integration**
   - Added dynamic data extraction from external APIs for CUSTOM_RULES templates
   - Supports JSONPath field extraction and mapping
   - Integrated into DocumentEnquiryService with comprehensive logging

3. **API Testing Enhancements**
   - Documented required HTTP headers (X-version, X-correlation-id, X-requestor-id, X-requestor-type)
   - Updated Postman collection with correct endpoint URL and all required headers
   - Two test scenarios pre-configured

4. **Documentation Updates**
   - Added data extraction configuration examples
   - Added technical notes section for BIT/BOOLEAN compatibility
   - Updated testing instructions with required headers
   - Added Postman collection reference
