# Document Hub POC - Sample Data

This directory contains sample data and test scenarios for the Document Hub POC.

## Contents

| File | Description | Size |
|------|-------------|------|
| `00-setup-database.sh` | Automated setup script (Linux/Mac) | Bash script |
| `00-setup-database.bat` | Automated setup script (Windows) | Batch script |
| `01-templates.sql` | 4 template definitions | ~17 KB |
| `02-documents.sql` | Sample documents for testing | ~14 KB |
| `03-test-scenarios.md` | Comprehensive test scenarios | ~20 KB |
| `README.md` | This file | - |

---

## Quick Start

### Option 1: Automated Setup (Recommended)

**Linux/Mac**:
```bash
cd poc/sample-data
chmod +x 00-setup-database.sh
./00-setup-database.sh
```

**Windows**:
```cmd
cd poc\sample-data
00-setup-database.bat
```

The script will:
1. Check prerequisites (PostgreSQL client)
2. Test database connection
3. Create database (if needed)
4. Load schema
5. Load templates
6. Load sample documents
7. Create performance indices
8. Verify data

### Option 2: Manual Setup

```bash
# 1. Create database
psql -U postgres -c "CREATE DATABASE dochub_poc;"

# 2. Load schema
psql -U postgres -d dochub_poc -f ../../database/schemas/document_hub_schema.sql

# 3. Load templates
psql -U postgres -d dochub_poc -f 01-templates.sql

# 4. Load documents
psql -U postgres -d dochub_poc -f 02-documents.sql
```

---

## Sample Templates

This package includes 4 templates demonstrating different complexity levels:

### Template 1: Monthly Statement
- **Type**: `MONTHLY_STATEMENT`
- **Complexity**: Simple
- **External API Calls**: 0
- **Matching Strategy**: `account-key`
- **Use Case**: Basic account-based document retrieval

### Template 2: Privacy Policy
- **Type**: `PRIVACY_POLICY`
- **Complexity**: Medium
- **External API Calls**: 1
- **API Chain**: Customer Service → Location
- **Matching Strategy**: `metadata` (state-based)
- **Use Case**: Location-based shared document selection

### Template 3: Cardholder Agreement
- **Type**: `CARDHOLDER_AGREEMENT`
- **Complexity**: High
- **External API Calls**: 2 (sequential)
- **API Chain**: Account Service → Pricing Service → Disclosure
- **Matching Strategy**: `reference_key` (disclosure code)
- **Use Case**: Multi-step data extraction with chaining

### Template 4: VIP Customer Letter
- **Type**: `VIP_LETTER`
- **Complexity**: Very High
- **External API Calls**: 3 (conditional)
- **API Chain**: Customer Tier → VIP Benefits → Personalized Content
- **Matching Strategy**: `composite` (tier + offer code)
- **Use Case**: Complex eligibility rules with conditional chaining

---

## Sample Documents

The sample data includes **13 documents** across 4 categories:

### Monthly Statements (3 documents)
- Account A001: Jan 2024, Feb 2024
- Account A002: Feb 2024
- **Customer-specific**: Yes
- **Shared**: No

### Privacy Policies (3 documents)
- California (CA) - CCPA compliant
- New York (NY) - NY SHIELD Act
- Texas (TX) - Standard
- **Customer-specific**: No
- **Shared**: Yes

### Cardholder Agreements (3 documents)
- D001 - Standard tier (APR: 19.99%, Fee: $0)
- D002 - Premium tier (APR: 15.99%, Fee: $95)
- D003 - VIP tier (APR: 11.99%, Fee: $495)
- **Customer-specific**: No
- **Shared**: Yes (via reference key)

### VIP Letters (4 documents)
- VIP Summer 2024 offer (Customer C003)
- VIP Fall 2024 offer (Customer C004)
- VIP General benefits (Shared)
- **Customer-specific**: Mixed
- **Shared**: Mixed

---

## Test Customers

The sample data references these test customers:

| Customer ID | State | Tier | Account | Notes |
|-------------|-------|------|---------|-------|
| C001 | CA | STANDARD | A001 | Has 2 monthly statements |
| C002 | NY | PREMIUM | A002 | Has 1 monthly statement |
| C003 | TX | VIP | A003 | VIP customer, Summer 2024 offer |
| C004 | CA | VIP | A004 | VIP customer, Fall 2024 offer |

**Note**: These customers don't exist in the database. The POC expects external services (mocked) to return customer/account data.

---

## Testing

See `03-test-scenarios.md` for:
- ✅ 20+ detailed test cases
- ✅ Expected API requests/responses
- ✅ Mock API setup instructions
- ✅ Performance benchmarks
- ✅ Error handling scenarios

### Quick Test

After setup, test with:

```bash
curl -X POST http://localhost:8080/api/documents/enquiry \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "C001",
    "accountId": "A001",
    "templateType": "MONTHLY_STATEMENT"
  }'
```

**Expected**: Returns 2 monthly statements for account A001

---

## Database Schema

The sample data works with the main database schema located at:
- `database/schemas/document_hub_schema.sql`

### Key Tables

1. **master_template_definition**
   - Template configurations
   - Data extraction strategies
   - Access control rules
   - Document matching strategies

2. **storage_index**
   - Document metadata
   - S3 storage locations
   - Reference keys
   - Custom metadata (JSONB)

---

## Configuration Details

### Data Extraction Config Structure

Each template's `data_extraction_config` (JSONB) includes:

```json
{
  "executionRules": {
    "executionMode": "sequential",
    "maxParallelCalls": 1,
    "timeout": 15000
  },
  "extractionStrategy": [
    {
      "id": "step_1",
      "description": "Description of this step",
      "endpoint": {
        "url": "http://localhost:8081/api/...",
        "method": "GET",
        "timeout": 5000
      },
      "cache": {
        "enabled": true,
        "ttl": 3600,
        "keyPattern": "cache:key:${variable}"
      },
      "responseMapping": {
        "extract": {
          "$variable": "$.jsonPath"
        },
        "validate": {
          "$variable": {
            "required": true,
            "pattern": "^regex$"
          }
        }
      },
      "nextCalls": [
        {
          "condition": {
            "field": "$variable",
            "operator": "notNull",
            "value": null
          },
          "targetDataSource": "step_2"
        }
      ]
    }
  ],
  "documentMatchingStrategy": {
    "strategy": "metadata",
    "conditions": [...]
  }
}
```

### Access Control Config Structure

```json
{
  "eligibilityType": "criteria-based",
  "eligibilityCriteria": {
    "rule_name": {
      "field": "$variable",
      "operator": "equals",
      "value": "expected_value"
    }
  },
  "logic": "AND"
}
```

---

## Troubleshooting

### Database Connection Issues

**Problem**: Cannot connect to database

**Solution**:
```bash
# Check if PostgreSQL is running
pg_isready -h localhost -p 5432

# Test connection manually
psql -h localhost -p 5432 -U postgres -d postgres
```

### Schema Load Errors

**Problem**: Schema file not found

**Solution**: Ensure you're running the script from the correct directory:
```bash
cd poc/sample-data
pwd  # Should end with /poc/sample-data
```

### Template Load Errors

**Problem**: Duplicate key violation

**Solution**: Templates already exist. Either:
1. Drop and recreate database, or
2. Delete existing templates:
```sql
DELETE FROM master_template_definition
WHERE template_type IN ('MONTHLY_STATEMENT', 'PRIVACY_POLICY', 'CARDHOLDER_AGREEMENT', 'VIP_LETTER');
```

### Empty Results

**Problem**: API returns empty document list

**Possible Causes**:
1. Mock services not running
2. Customer/account IDs don't match sample data
3. External API returning unexpected data
4. Cache issue (try clearing Redis)

**Debugging**:
```bash
# Check application logs
tail -f logs/application.log

# Check database
psql -U postgres -d dochub_poc -c "SELECT COUNT(*) FROM storage_index;"

# Verify templates
psql -U postgres -d dochub_poc -c "SELECT template_type, is_active FROM master_template_definition;"
```

---

## Next Steps

After loading sample data:

1. **Review Test Scenarios**: Read `03-test-scenarios.md`
2. **Setup Mock Services**: Configure external API mocks
3. **Start Application**: `mvn spring-boot:run`
4. **Run Tests**: Execute test scenarios
5. **Monitor Performance**: Check response times
6. **Verify Cache**: Ensure caching is working

---

## Data Cleanup

To remove all sample data:

```sql
-- Remove documents
DELETE FROM storage_index;

-- Remove templates
DELETE FROM master_template_definition;

-- Reset sequences (optional)
ALTER SEQUENCE storage_index_id_seq RESTART WITH 1;
```

Or drop the entire database:
```bash
psql -U postgres -c "DROP DATABASE dochub_poc;"
```

---

## File Sizes

- **Total SQL**: ~31 KB
- **Documentation**: ~20 KB
- **Scripts**: ~8 KB
- **Total Package**: ~60 KB

---

## Support

For questions or issues:
1. Check `03-test-scenarios.md` for detailed examples
2. Review main POC documentation in `poc/README.md`
3. Check application logs for errors
4. Verify database schema matches expectations

---

**Version**: 1.0
**Date**: 2025-11-27
**Compatibility**: PostgreSQL 14+, Spring Boot 2.7+
