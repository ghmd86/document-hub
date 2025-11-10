# Shared Document Catalog - Worked Example

**Purpose:** Step-by-step example of how to document a shared document using real business scenario
**Document Type:** Premium Travel Insurance Benefits Disclosure
**Complexity:** Medium (Custom Rule with Multiple Conditions)

---

## Step 1: SME Interview (Completed)

### Interview Summary

**Date:** 2024-11-09
**SME:** Sarah Johnson, Product Manager - Credit Cards
**Duration:** 35 minutes

**Key Points:**
- Document: Premium Travel Insurance Benefits Disclosure
- Only for premium credit card customers
- Must have spent on travel in last 6 months
- Account must be in good standing
- Not for customers who already enrolled in travel insurance

---

## Step 2: Extracted Requirements

### From Questionnaire Answers:

**Q1: Document Identification**
- Name: "Premium Travel Insurance Benefits Disclosure"
- Type: Disclosure
- Category: Benefits
- Line of Business: Credit Card
- Regulatory: No
- Owner: Product Marketing

**Q2: Target Audience**
- Premium credit card customers (Platinum and Black tiers)
- Who have travel spending in last 6 months
- Good account standing (no delinquency)
- Not already enrolled in travel insurance product
- Estimated reach: ~8% of credit card customers

**Q3: Eligibility Criteria**
1. Customer Tier = PLATINUM or BLACK
2. Account Status = ACTIVE
3. Travel spending in last 6 months > $500
4. Account delinquency days = 0
5. NOT enrolled in TRAVEL_INSURANCE product

**Q4: Data Sources**
- Customer Service API: tier, status
- Account Service API: accountStatus, delinquencyDays
- Transaction Service API: merchantCategorySpend (travel category)
- Product Enrollment API: enrolledProducts

**Q7: Compliance**
- Not regulatory
- No special timing requirements
- Audit logging required (track who sees disclosure)

---

## Step 3: Completed Catalog Entry

```yaml
---
# ============================================================================
# DOCUMENT METADATA
# ============================================================================
document_id: "SHARED-DOC-TRAVEL-001"
document_name: "Premium Travel Insurance Benefits Disclosure"
document_type: "disclosure"
category: "Benefits"
subcategory: "Travel Insurance"
line_of_business: "credit_card"
regulatory: false
owner_department: "Product Marketing"
contact_person: "Sarah Johnson (sjohnson@company.com)"

# ============================================================================
# SHARING CONFIGURATION
# ============================================================================
sharing_scope: "custom_rule"
effective_date: "2024-12-01"
valid_until: null  # Ongoing, no expiration

# ============================================================================
# ELIGIBILITY RULE
# ============================================================================
eligibility_rule:
  rule_name: "premium_travel_insurance_disclosure_eligibility"
  rule_description: "Show travel insurance benefits to premium cardholders with recent travel spend"
  rule_type: "composite"
  logic_operator: "AND"  # All conditions must be met

  # --------------------------------------------------------------------------
  # DATA SOURCES
  # --------------------------------------------------------------------------
  data_sources:
    # Customer Service API
    - source_id: "customer_service"
      source_type: "REST_API"
      endpoint_config:
        method: "GET"
        url: "https://api.company.com/v1/customers/${customerId}"
        timeout_ms: 5000
        retry_policy:
          max_attempts: 2
          wait_duration_ms: 500
      response_mapping:
        customerTier: "$.data.tier"
        customerStatus: "$.data.status"
      cache_config:
        enabled: true
        ttl_seconds: 300  # 5 minutes

    # Account Service API
    - source_id: "account_service"
      source_type: "REST_API"
      endpoint_config:
        method: "GET"
        url: "https://api.company.com/v1/accounts/${accountId}"
        timeout_ms: 3000
        retry_policy:
          max_attempts: 2
          wait_duration_ms: 500
      response_mapping:
        accountStatus: "$.data.accountStatus"
        delinquencyDays: "$.data.delinquency.daysDelinquent"
        accountType: "$.data.accountType"
      cache_config:
        enabled: true
        ttl_seconds: 60  # 1 minute (balance changes frequently)

    # Transaction Service API
    - source_id: "transaction_service"
      source_type: "REST_API"
      endpoint_config:
        method: "POST"
        url: "https://api.company.com/v1/transactions/merchant-category-spend"
        timeout_ms: 10000
        body_template:
          accountId: "${accountId}"
          merchantCategory: "TRAVEL"
          startDate: "${currentDate-6M}"  # 6 months ago
          endDate: "${currentDate}"
      response_mapping:
        travelSpendLast6M: "$.data.totalSpend"
        travelTransactionCount: "$.data.transactionCount"
      cache_config:
        enabled: true
        ttl_seconds: 3600  # 1 hour (aggregation is expensive)

    # Product Enrollment API
    - source_id: "product_enrollment_service"
      source_type: "REST_API"
      endpoint_config:
        method: "GET"
        url: "https://api.company.com/v1/customers/${customerId}/enrollments"
        timeout_ms: 3000
      response_mapping:
        enrolledProducts: "$.data.enrollments[*].productCode"
      cache_config:
        enabled: true
        ttl_seconds: 900  # 15 minutes

  # --------------------------------------------------------------------------
  # TRANSFORMATIONS (if needed)
  # --------------------------------------------------------------------------
  transformations:
    - field: "travelSpendLast6M"
      operations:
        - type: "TO_NUMBER"
        - type: "ROUND"
          decimals: 2

  # --------------------------------------------------------------------------
  # ELIGIBILITY CONDITIONS
  # --------------------------------------------------------------------------
  conditions:
    # Condition 1: Must be premium tier
    - condition_id: "COND-001"
      description: "Customer must be Platinum or Black tier"
      field: "customerTier"
      operator: "IN"
      value: ["PLATINUM", "BLACK"]
      logical_operator: "AND"
      priority: 1
      error_message: "Not a premium tier customer"

    # Condition 2: Account must be active
    - condition_id: "COND-002"
      description: "Account must be in active status"
      field: "accountStatus"
      operator: "EQUALS"
      value: "ACTIVE"
      logical_operator: "AND"
      priority: 1
      error_message: "Account is not active"

    # Condition 3: Must have travel spending
    - condition_id: "COND-003"
      description: "Must have spent on travel in last 6 months"
      field: "travelSpendLast6M"
      operator: "GREATER_THAN"
      value: 500.00
      logical_operator: "AND"
      priority: 2
      error_message: "Insufficient travel spending"

    # Condition 4: Account must be in good standing
    - condition_id: "COND-004"
      description: "Account must have no delinquency"
      field: "delinquencyDays"
      operator: "EQUALS"
      value: 0
      logical_operator: "AND"
      priority: 1
      error_message: "Account has delinquency"

    # Condition 5: Must NOT already be enrolled in travel insurance
    - condition_id: "COND-005"
      description: "Customer must not already have travel insurance"
      field: "enrolledProducts"
      operator: "NOT_CONTAINS"
      value: "TRAVEL_INSURANCE"
      logical_operator: "AND"
      priority: 3
      error_message: "Already enrolled in travel insurance"

  # --------------------------------------------------------------------------
  # ERROR HANDLING
  # --------------------------------------------------------------------------
  error_handling:
    on_api_failure: "exclude"  # Don't show document if API fails
    fallback_result: false
    log_level: "WARN"
    retry_on_timeout: true
    circuit_breaker:
      enabled: true
      failure_threshold: 5
      wait_duration_ms: 30000

  # --------------------------------------------------------------------------
  # PERFORMANCE CONFIGURATION
  # --------------------------------------------------------------------------
  performance:
    execution_timeout_ms: 15000  # Total timeout for all API calls
    parallel_execution: true     # Execute API calls in parallel
    cache_results: true
    result_cache_ttl_seconds: 900  # Cache eligibility result for 15 min

# ============================================================================
# BUSINESS CONTEXT
# ============================================================================
business_justification: |
  This disclosure is shown to premium credit card customers who demonstrate
  travel behavior to:

  1. Increase awareness of premium travel insurance benefits
  2. Drive enrollment in travel insurance product (revenue opportunity)
  3. Improve customer satisfaction by highlighting relevant benefits
  4. Reduce customer service inquiries about travel coverage

  We specifically target customers with recent travel spending because they
  are most likely to value and utilize travel insurance benefits.

target_audience: "Premium cardholders (Platinum/Black) with travel spending"

expected_volume:
  total_credit_card_customers: 1000000
  platinum_black_customers: 120000  # 12% are premium
  with_travel_spending: 48000       # 40% of premium have travel spend
  not_enrolled: 38400               # 80% not enrolled yet
  final_eligible: 38400             # ~3.8% of all credit card customers

revenue_opportunity:
  enrollment_conversion_rate: "15%"  # Expected 15% will enroll
  expected_enrollments_per_month: 480  # 38,400 * 15% / 12 months
  revenue_per_enrollment: "$120/year"
  annual_revenue_potential: "$691,200"

# ============================================================================
# TESTING SCENARIOS
# ============================================================================
test_scenarios:
  # ---------------------------------------------------------------------------
  # SCENARIO 1: Typical Eligible Customer (POSITIVE)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-001"
    scenario_name: "Eligible - Platinum customer with travel spending"
    description: "Premium customer with recent travel, good standing, not enrolled"

    mock_data:
      customer_service:
        customerTier: "PLATINUM"
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 0
        accountType: "CREDIT_CARD"

      transaction_service:
        travelSpendLast6M: 1250.50
        travelTransactionCount: 8

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY", "PAPERLESS_STATEMENTS"]

    expected_result: true
    expected_reason: "All conditions met: Premium tier, active, travel spend > $500, no delinquency, not enrolled"

    validation_steps:
      - "Verify customerTier IN [PLATINUM, BLACK] → PASS"
      - "Verify accountStatus = ACTIVE → PASS"
      - "Verify travelSpendLast6M > 500 → PASS ($1,250.50)"
      - "Verify delinquencyDays = 0 → PASS"
      - "Verify TRAVEL_INSURANCE NOT IN enrolledProducts → PASS"
      - "Final result: SHOW DOCUMENT"

  # ---------------------------------------------------------------------------
  # SCENARIO 2: Low Tier Customer (NEGATIVE)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-002"
    scenario_name: "Ineligible - Standard tier customer"
    description: "Customer with travel spending but not premium tier"

    mock_data:
      customer_service:
        customerTier: "STANDARD"  # Not premium
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 0

      transaction_service:
        travelSpendLast6M: 2000.00  # Has travel spend
        travelTransactionCount: 10

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY"]

    expected_result: false
    expected_reason: "Condition COND-001 failed: Not a premium tier customer"

    validation_steps:
      - "Verify customerTier IN [PLATINUM, BLACK] → FAIL (is STANDARD)"
      - "Short-circuit: Don't check other conditions (AND logic)"
      - "Final result: DO NOT SHOW DOCUMENT"

  # ---------------------------------------------------------------------------
  # SCENARIO 3: No Travel Spending (NEGATIVE)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-003"
    scenario_name: "Ineligible - No travel spending"
    description: "Premium customer but no travel activity"

    mock_data:
      customer_service:
        customerTier: "BLACK"
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 0

      transaction_service:
        travelSpendLast6M: 0.00  # No travel spending
        travelTransactionCount: 0

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY"]

    expected_result: false
    expected_reason: "Condition COND-003 failed: Insufficient travel spending"

    validation_steps:
      - "Verify customerTier IN [PLATINUM, BLACK] → PASS (BLACK)"
      - "Verify accountStatus = ACTIVE → PASS"
      - "Verify travelSpendLast6M > 500 → FAIL ($0.00)"
      - "Final result: DO NOT SHOW DOCUMENT"

  # ---------------------------------------------------------------------------
  # SCENARIO 4: Already Enrolled (NEGATIVE)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-004"
    scenario_name: "Ineligible - Already enrolled"
    description: "Premium customer with travel spend but already enrolled"

    mock_data:
      customer_service:
        customerTier: "PLATINUM"
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 0

      transaction_service:
        travelSpendLast6M: 1500.00
        travelTransactionCount: 12

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY", "TRAVEL_INSURANCE"]  # Already enrolled

    expected_result: false
    expected_reason: "Condition COND-005 failed: Already enrolled in travel insurance"

    validation_steps:
      - "Verify customerTier IN [PLATINUM, BLACK] → PASS"
      - "Verify accountStatus = ACTIVE → PASS"
      - "Verify travelSpendLast6M > 500 → PASS"
      - "Verify delinquencyDays = 0 → PASS"
      - "Verify TRAVEL_INSURANCE NOT IN enrolledProducts → FAIL (found)"
      - "Final result: DO NOT SHOW DOCUMENT"

  # ---------------------------------------------------------------------------
  # SCENARIO 5: Edge Case - Exactly $500 Travel Spend (NEGATIVE per spec)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-005"
    scenario_name: "Edge case - Exactly $500 spend"
    description: "Customer with exactly the threshold amount"

    mock_data:
      customer_service:
        customerTier: "PLATINUM"
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 0

      transaction_service:
        travelSpendLast6M: 500.00  # Exactly $500
        travelTransactionCount: 3

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY"]

    expected_result: false
    expected_reason: "Condition COND-003: GREATER_THAN (not >=), so 500.00 fails"

    validation_steps:
      - "Verify customerTier IN [PLATINUM, BLACK] → PASS"
      - "Verify accountStatus = ACTIVE → PASS"
      - "Verify travelSpendLast6M > 500 → FAIL (500.00 is not > 500.00)"
      - "Final result: DO NOT SHOW DOCUMENT"

    notes: |
      SME confirmed: Must be GREATER THAN $500, not greater than or equal.
      Rationale: $500 threshold represents substantive travel behavior.
      Edge case $500.00 is treated as not meeting threshold.

  # ---------------------------------------------------------------------------
  # SCENARIO 6: Delinquent Account (NEGATIVE)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-006"
    scenario_name: "Ineligible - Account delinquent"
    description: "Premium customer with travel spend but account is delinquent"

    mock_data:
      customer_service:
        customerTier: "PLATINUM"
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 15  # 15 days past due

      transaction_service:
        travelSpendLast6M: 2000.00
        travelTransactionCount: 8

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY"]

    expected_result: false
    expected_reason: "Condition COND-004 failed: Account has delinquency"

    validation_steps:
      - "Verify customerTier IN [PLATINUM, BLACK] → PASS"
      - "Verify accountStatus = ACTIVE → PASS"
      - "Verify travelSpendLast6M > 500 → PASS"
      - "Verify delinquencyDays = 0 → FAIL (is 15)"
      - "Final result: DO NOT SHOW DOCUMENT"

  # ---------------------------------------------------------------------------
  # SCENARIO 7: API Failure Handling (ERROR CASE)
  # ---------------------------------------------------------------------------
  - scenario_id: "TEST-007"
    scenario_name: "Error - Transaction API timeout"
    description: "Test error handling when transaction API times out"

    mock_data:
      customer_service:
        customerTier: "PLATINUM"
        customerStatus: "ACTIVE"

      account_service:
        accountStatus: "ACTIVE"
        delinquencyDays: 0

      transaction_service:
        error: "TIMEOUT"
        status_code: 504

      product_enrollment_service:
        enrolledProducts: ["AUTOPAY"]

    expected_result: false
    expected_reason: "API failure, error_handling.on_api_failure = exclude"

    validation_steps:
      - "Transaction API call → TIMEOUT"
      - "Apply error_handling.on_api_failure policy → exclude"
      - "Log warning with request details"
      - "Final result: DO NOT SHOW DOCUMENT (fail safe)"

    monitoring:
      - "Increment metric: eligibility_api_failures"
      - "Alert if failure rate > 5%"
      - "Include in daily error report"

# ============================================================================
# MONITORING & ALERTING
# ============================================================================
monitoring:
  metrics:
    - name: "eligibility_checks_total"
      type: "counter"
      description: "Total eligibility checks performed"
      labels: ["result"]

    - name: "eligibility_check_duration_ms"
      type: "histogram"
      description: "Duration of eligibility check"
      buckets: [50, 100, 200, 500, 1000, 2000, 5000]

    - name: "api_call_failures"
      type: "counter"
      description: "API call failures by source"
      labels: ["source_id", "error_type"]

    - name: "eligible_customers_count"
      type: "gauge"
      description: "Number of currently eligible customers"

  alerts:
    - name: "HighAPIFailureRate"
      condition: "api_call_failures > 5% of total checks"
      severity: "WARNING"
      action: "Notify on-call engineer"

    - name: "SlowEligibilityCheck"
      condition: "p95 duration > 2000ms"
      severity: "WARNING"
      action: "Review API performance"

    - name: "ZeroEligibleCustomers"
      condition: "eligible_customers_count = 0 for > 1 hour"
      severity: "CRITICAL"
      action: "Page on-call, possible rule misconfiguration"

# ============================================================================
# AUDIT & COMPLIANCE
# ============================================================================
audit_logging:
  enabled: true
  log_level: "INFO"
  retention_days: 365

  log_fields:
    - customerId
    - accountId
    - documentId
    - eligibility_result
    - evaluation_timestamp
    - rule_version
    - api_sources_called
    - execution_duration_ms
    - errors_encountered

  pii_handling: "Hash customer/account IDs before logging"

  access_control:
    read: ["AUDIT_TEAM", "COMPLIANCE_TEAM", "SECURITY_TEAM"]
    delete: ["COMPLIANCE_ADMIN_ONLY"]

# ============================================================================
# VERSION CONTROL & CHANGE HISTORY
# ============================================================================
version: "1.0"
created_date: "2024-11-09"
created_by: "Sarah Johnson (Product) + Technical Team"
approved_by: "Jane Smith (VP Product)"
last_modified_date: "2024-11-09"
last_modified_by: "Technical Team"

change_history:
  - version: "1.0"
    date: "2024-11-09"
    author: "Technical Team"
    changes: "Initial version based on SME interview"
    approval: "Jane Smith (VP Product)"

planned_changes:
  - version: "1.1"
    planned_date: "2025-01-15"
    description: "Lower travel spend threshold to $250 (business request)"
    requestor: "Marketing Team"
    status: "PENDING_APPROVAL"

---
```

---

## Step 4: Implementation Checklist

### Before Implementation
- [x] SME interview completed
- [x] Catalog entry documented
- [x] Test scenarios defined
- [x] Data sources identified
- [ ] API endpoints verified (IT team)
- [ ] Performance impact assessed
- [ ] Security review completed
- [ ] Business approval obtained

### During Implementation
- [ ] Create ExtractionRule JSON from catalog
- [ ] Implement custom rule in CustomRuleEngine
- [ ] Create mock services for testing
- [ ] Write unit tests (7 scenarios)
- [ ] Write integration tests
- [ ] Performance testing (target < 500ms)
- [ ] Error handling testing

### After Implementation
- [ ] QA testing with real data
- [ ] UAT with Product team
- [ ] Monitor metrics for 1 week in QA
- [ ] Production deployment
- [ ] Monitor first 24 hours closely
- [ ] Business validation (check eligible count matches estimate)

---

## Step 5: JSON Rule for Implementation

This YAML catalog entry translates to the following JSON for the `data_extraction_schema` column:

```json
{
  "rule_name": "premium_travel_insurance_disclosure_eligibility",
  "rule_type": "composite",
  "logic_operator": "AND",
  "data_sources": [
    {
      "source_id": "customer_service",
      "source_type": "REST_API",
      "endpoint_config": {
        "url": "https://api.company.com/v1/customers/${customerId}",
        "method": "GET",
        "timeout_ms": 5000
      },
      "response_mapping": {
        "customerTier": "$.data.tier",
        "customerStatus": "$.data.status"
      }
    },
    {
      "source_id": "account_service",
      "source_type": "REST_API",
      "endpoint_config": {
        "url": "https://api.company.com/v1/accounts/${accountId}",
        "method": "GET",
        "timeout_ms": 3000
      },
      "response_mapping": {
        "accountStatus": "$.data.accountStatus",
        "delinquencyDays": "$.data.delinquency.daysDelinquent"
      }
    },
    {
      "source_id": "transaction_service",
      "source_type": "REST_API",
      "endpoint_config": {
        "url": "https://api.company.com/v1/transactions/merchant-category-spend",
        "method": "POST",
        "timeout_ms": 10000,
        "body_template": {
          "accountId": "${accountId}",
          "merchantCategory": "TRAVEL",
          "startDate": "${currentDate-6M}",
          "endDate": "${currentDate}"
        }
      },
      "response_mapping": {
        "travelSpendLast6M": "$.data.totalSpend"
      }
    },
    {
      "source_id": "product_enrollment_service",
      "source_type": "REST_API",
      "endpoint_config": {
        "url": "https://api.company.com/v1/customers/${customerId}/enrollments",
        "method": "GET",
        "timeout_ms": 3000
      },
      "response_mapping": {
        "enrolledProducts": "$.data.enrollments[*].productCode"
      }
    }
  ],
  "conditions": [
    {
      "field": "customerTier",
      "operator": "IN",
      "value": ["PLATINUM", "BLACK"],
      "logical_operator": "AND"
    },
    {
      "field": "accountStatus",
      "operator": "EQUALS",
      "value": "ACTIVE",
      "logical_operator": "AND"
    },
    {
      "field": "travelSpendLast6M",
      "operator": "GREATER_THAN",
      "value": 500.00,
      "logical_operator": "AND"
    },
    {
      "field": "delinquencyDays",
      "operator": "EQUALS",
      "value": 0,
      "logical_operator": "AND"
    },
    {
      "field": "enrolledProducts",
      "operator": "NOT_CONTAINS",
      "value": "TRAVEL_INSURANCE"
    }
  ],
  "error_handling": {
    "on_api_failure": "exclude",
    "fallback_result": false
  }
}
```

This JSON is stored in the `data_extraction_schema` column of the `master_template_definition` table.

---

## Key Takeaways

### What Makes This Example Good:

1. **Complete Business Context**
   - Clear business justification
   - Revenue opportunity quantified
   - Target audience defined

2. **Well-Defined Conditions**
   - 5 specific conditions
   - Clear operators and values
   - Priority assigned

3. **Comprehensive Testing**
   - 7 test scenarios
   - Positive, negative, edge cases
   - Error handling tested

4. **Performance Considered**
   - Caching strategy defined
   - Parallel API execution
   - Timeout values specified

5. **Production-Ready**
   - Monitoring metrics defined
   - Alerts configured
   - Audit logging specified

### Use This as Template

When documenting your shared documents:
- Copy this structure
- Fill in your specific values
- Include all test scenarios
- Get SME validation
- Implement and test

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Status:** Example/Template
