# Multi-Step Execution Flow - Visual Examples

## Example 1: Simple Linear Chain (3 Steps)

### Scenario
Need to get disclosure requirements for an account's product

### Dependency Chain
```
accountId (input)
    ↓
productCode (from Account API)
    ↓
productCategory (from Product API)
    ↓
disclosureRequirements (from Regulatory API)
```

### Execution Timeline
```
Time →

T0: START
    ├─ Input: accountId = "ACC-001"

T1: [LEVEL 1] Call Account API
    ├─ GET /accounts/ACC-001
    ├─ Response: {"account": {"productCode": "PROD-123"}}
    └─ Extract: productCode = "PROD-123"

T2: [LEVEL 2] Call Product API (needs productCode)
    ├─ GET /products/PROD-123
    ├─ Response: {"product": {"category": "CREDIT_CARD"}}
    └─ Extract: productCategory = "CREDIT_CARD"

T3: [LEVEL 3] Call Regulatory API (needs productCategory)
    ├─ GET /regulatory?category=CREDIT_CARD
    ├─ Response: {"rules": {"disclosures": ["TILA", "CARD_ACT"]}}
    └─ Extract: disclosureRequirements = ["TILA", "CARD_ACT"]

T4: DONE
    └─ Return all extracted data
```

**Total Time**: ~T3 (Sequential execution, each waits for previous)

---

## Example 2: Parallel Branches with Merge (Diamond Pattern)

### Scenario
Get final disclosure rules that depend on BOTH product info AND customer location

### Dependency Graph
```
        ┌─── accountId ────┐         ┌─── customerId ───┐
        │                  │         │                  │
        ↓                  ↓         ↓                  ↓
  [Account API]      [Product API]  [Customer API]  [Region API]
        │                  │         │                  │
        ↓                  ↓         ↓                  ↓
   productCode       productCategory customerLocation  regionCode
        └──────────────┬──────────────┘                 │
                       ↓                                ↓
                  [Disclosure API] ←───────────────────┘
                       ↓
              disclosureRequirements
```

### Execution Timeline
```
Time →

T0: START
    ├─ Input: accountId = "ACC-001"
    └─ Input: customerId = "CUST-999"

T1: [LEVEL 1] Parallel execution (2 APIs)
    ├─ [Thread 1] GET /accounts/ACC-001
    │   └─ Extract: productCode = "PROD-123"
    │
    └─ [Thread 2] GET /customers/CUST-999
        └─ Extract: customerLocation = "CA"

T2: [LEVEL 2] Parallel execution (2 APIs, needs Level 1 results)
    ├─ [Thread 1] GET /products/PROD-123
    │   └─ Extract: productCategory = "CREDIT_CARD"
    │
    └─ [Thread 2] POST /regions/map {"state": "CA"}
        └─ Extract: regulatoryRegion = "US_WEST"

T3: [LEVEL 3] Single API (needs BOTH Level 2 results)
    └─ GET /disclosures?category=CREDIT_CARD&region=US_WEST
        └─ Extract: disclosureRequirements = ["TILA", "CARD_ACT", "CA_RULES"]

T4: DONE
```

**Total Time**: ~T3 (Much faster! Level 1 and Level 2 run in parallel)

**Speedup**:
- Without parallelization: T1 + T1 + T2 + T2 + T3 = 5 units
- With parallelization: max(T1, T1) + max(T2, T2) + T3 = 3 units
- **40% faster!**

---

## Example 3: Complex Multi-Source Merge (5 inputs → 1 output)

### Scenario
Calculate eligibility score that needs 5 different pieces of data

### Dependency Graph
```
      accountId                    customerId
          │                            │
          ├────────────┬───────────────┤
          │            │               │
          ↓            ↓               ↓
    [Account API]  [Product API]  [Customer API]
          │            │               │
          ↓            ↓               ↓
     accountType   productCode    customerTier
          │            │               │
          └────────────┼───────────────┘
                       │
          customerLocation (from Customer API)
                       │
          regionCode (from Customer API)
                       │
                       ↓
              [Eligibility API]
                       │
                       ↓
             eligibilityScore
```

### Execution Timeline
```
T0: START
    ├─ Input: accountId = "ACC-001"
    └─ Input: customerId = "CUST-999"

T1: [LEVEL 1] Parallel execution (2 APIs, multiple fields each)
    ├─ [Thread 1] GET /accounts/ACC-001
    │   ├─ Extract: accountType = "PREMIUM"
    │   └─ Extract: productCode = "PROD-123"
    │
    └─ [Thread 2] GET /customers/CUST-999
        ├─ Extract: customerTier = "VIP"
        ├─ Extract: customerLocation = "CA"
        └─ Extract: regionCode = "US-WEST"

T2: [LEVEL 2] Single API (needs ALL 5 fields from Level 1)
    └─ POST /eligibility/calculate
        Body: {
          "accountType": "PREMIUM",
          "productCode": "PROD-123",
          "customerTier": "VIP",
          "customerLocation": "CA",
          "regionCode": "US-WEST"
        }
        └─ Extract: eligibilityScore = 95

T3: DONE
```

**Key Point**: Even though 5 fields are needed, only 2 API calls in parallel!

---

## Example 4: Deep Chain (5 Levels)

### Scenario
Account → Branch → Region → Compliance → Documents

### Dependency Chain
```
accountId (input)
    ↓
branchCode (from Account API)
    ↓
regionCode (from Branch API)
    ↓
complianceRules (from Compliance API)
    ↓
applicableDocuments (from Document Rules API)
```

### Execution Timeline
```
T0: START
    └─ Input: accountId = "ACC-001"

T1: [LEVEL 1]
    └─ GET /accounts/ACC-001
        └─ Extract: branchCode = "BR-555"

T2: [LEVEL 2] (needs branchCode)
    └─ GET /branches/BR-555
        └─ Extract: regionCode = "WEST"

T3: [LEVEL 3] (needs regionCode)
    └─ GET /compliance/regions/WEST
        └─ Extract: complianceRules = {"rule1": "...", "rule2": "..."}

T4: [LEVEL 4] (needs complianceRules)
    └─ POST /document-rules
        Body: {"rules": {"rule1": "...", "rule2": "..."}}
        └─ Extract: applicableDocuments = ["DOC-1", "DOC-2", "DOC-3"]

T5: DONE
```

**Total Time**: ~T5 (Sequential - each level must wait)
**Use Case**: When data is inherently sequential (each truly depends on previous)

---

## Comparison: Sequential vs Parallel

### Scenario: Need 4 independent fields from 4 different APIs

### Sequential Execution (mode: "sequential")
```
Time: 0ms ─────────────────────────────────────────────────── 20000ms

      ├──API1──┤──API2──┤──API3──┤──API4──┤
      0      5000    10000   15000   20000ms

Total: 20 seconds
```

### Parallel Execution (mode: "auto" or "parallel")
```
Time: 0ms ─────────────────── 5000ms

      ├──API1──┤
      ├──API2──┤
      ├──API3──┤
      ├──API4──┤
      0      5000ms

Total: 5 seconds (assuming all APIs take ~5s)
```

**Speedup: 4x faster!**

---

## Real-World Performance Comparison

### Without Multi-Step Support (Old Way)
```
All data must be pre-fetched or hard-coded:
- Manual code for each API call
- No dependency management
- No parallelization
- Hard to add new fields

Result: Slow, inflexible, requires code changes
```

### With Multi-Step Support (New Way)
```
System automatically:
✅ Determines execution order
✅ Parallelizes independent calls
✅ Chains dependent calls
✅ Handles errors gracefully
✅ Caches results
✅ Resolves placeholders

Result: Fast, flexible, zero code changes
```

---

## Execution Mode Comparison

| Mode | When to Use | Execution Strategy |
|------|-------------|-------------------|
| `auto` (Recommended) | Always | System analyzes dependencies and optimizes |
| `sequential` | Testing, debugging | One API at a time, in order |
| `parallel` | No dependencies | All APIs simultaneously |

### Example: Same Config, Different Modes

**Config**: 4 fields, 2 parallel branches

**Mode: "auto"**
```
Level 1: API-A, API-B (parallel)
Level 2: API-C, API-D (parallel, needs Level 1)
Time: ~10s
```

**Mode: "sequential"**
```
API-A → API-B → API-C → API-D
Time: ~20s
```

**Mode: "parallel"** (Would fail if dependencies exist!)
```
All APIs simultaneously
Time: ~5s (but might fail if API-C needs API-A result!)
```

---

## Error Handling Examples

### Scenario: API fails mid-chain

```
T1: [LEVEL 1] Account API → SUCCESS (productCode = "PROD-123")

T2: [LEVEL 2] Product API → FAIL (timeout)

┌─────────────────────────────────────┐
│   continueOnError: false            │
│   Result: ABORT, return defaults    │
└─────────────────────────────────────┘

VS

┌─────────────────────────────────────┐
│   continueOnError: true             │
│   Result: Use defaultValue for      │
│   productCategory, continue to T3   │
└─────────────────────────────────────┘
```

---

## Summary: Execution Patterns

### Pattern 1: Linear Chain
```
A → B → C → D
Time: O(n) where n = number of steps
Use: Sequential dependencies
```

### Pattern 2: Fan-Out, Fan-In
```
    A
   ↙ ↘
  B   C
   ↘ ↙
    D
Time: O(log n)
Use: Gather multiple sources, merge for final decision
```

### Pattern 3: Multi-Path
```
A → B → C
    ↓
D → E → F
    ↓
  Final
Time: O(longest path)
Use: Complex workflows with multiple data streams
```

### Pattern 4: Star (Multiple Inputs → One Output)
```
A, B, C, D, E
      ↓
   [API-X]
      ↓
   Result
Time: O(1) if inputs independent
Use: Aggregation, eligibility checks
```

---

## Key Takeaway

The system **automatically** determines the optimal execution strategy based on your field dependencies in the JSON config. You just define WHAT you need and WHERE it comes from - the system handles HOW to execute it efficiently!
