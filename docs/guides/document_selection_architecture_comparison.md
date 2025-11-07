# Document Selection Architecture Comparison

## Problem Statement
Select appropriate shared documents (e.g., CreditCardHolder agreements) based on account attributes, requiring data from multiple API endpoints.

## Approach Comparison

### 1. **Your Original Approach: Chained API Calls**

**Flow:**
```
Input → API Call 1 → Extract Data → API Call 2 → Extract Data → Select Document
```

**Pros:**
- Flexible and dynamic
- Handles complex data dependencies
- No hardcoded business logic

**Cons:**
- High latency (multiple sequential API calls)
- Network dependency for every request
- Difficult to test and debug
- No caching strategy
- Expensive at scale

**Best for:** Truly dynamic scenarios where document rules change frequently and are stored externally

---

### 2. **Recommended: Hybrid Approach (API + Rule Engine)**

**Flow:**
```
Input → API Call (cached) → Rule Engine (in-memory) → Select Document
```

**Architecture:**
```
┌─────────────────┐
│   API Request   │
└────────┬────────┘
         │
    ┌────▼─────┐
    │  Cache?  │◄──────┐
    └────┬─────┘       │
         │ Miss        │
    ┌────▼─────────┐   │
    │ Fetch Account│   │
    │   Details    │───┘
    └────┬─────────┘
         │
    ┌────▼──────────┐
    │  Rule Engine  │
    │  (Decision    │
    │   Table)      │
    └────┬──────────┘
         │
    ┌────▼──────────┐
    │   Document    │
    │   Selection   │
    └───────────────┘
```

**Implementation:**

```json
{
  "stage1_DataFetch": {
    "source": "accountAPI",
    "cache": { "ttl": 3600, "key": "account:${accountId}" },
    "extract": ["accountType", "state", "balance", "customerSegment"]
  },
  "stage2_RuleExecution": {
    "engine": "decision-table",
    "rulesSource": "database",
    "cacheRules": true,
    "refreshInterval": 300
  }
}
```

**Pros:**
- Fast (single API call + in-memory rules)
- Cacheable at multiple levels
- Easy to test (mock API, test rules separately)
- Rules can be updated without redeploying orchestrator
- Supports versioning and A/B testing

**Cons:**
- Rules must be pre-defined
- Requires separate rule management system

**Best for:** Your use case - document selection with relatively stable rules

---

### 3. **Alternative: Pre-computed Mapping Table**

**Flow:**
```
Input → Lookup Table (Redis/DB) → Document
```

**Architecture:**
```
Background Job (Daily):
  Fetch all accounts → Apply rules → Store mappings

Runtime:
  accountId → Redis lookup → documentId
```

**Pros:**
- Ultra-fast (single lookup)
- Zero API calls at runtime
- Highly cacheable
- Predictable performance

**Cons:**
- Not real-time (eventual consistency)
- Requires background job
- Storage overhead

**Best for:** High-volume scenarios where real-time data isn't critical

---

### 4. **Advanced: GraphQL Federation**

If you have multiple APIs and complex relationships:

```graphql
type Account {
  id: ID!
  type: AccountType!
  state: String!
  applicableDocuments: [Document!]! # Resolved via rules
}

type Document {
  id: ID!
  disclosureCode: String!
  version: String!
}
```

**Pros:**
- Single query for complex data
- Built-in caching
- Type safety

**Cons:**
- Requires GraphQL infrastructure
- Higher initial complexity

---

## Recommended Solution for Your Use Case

### Two-Tier Architecture

**Tier 1: API Orchestration Layer** (your enhanced schema)
```json
{
  "dataSources": [...],
  "cache": {...},
  "retryPolicy": {...}
}
```

**Tier 2: Rule Engine Layer** (separate service/library)
```json
{
  "ruleSet": "document-selection-v2",
  "rules": [...],
  "storage": "database",
  "cache": "redis"
}
```

### Key Enhancements to Your Design

#### 1. **Add Caching**
```json
{
  "cache": {
    "provider": "redis",
    "layers": [
      { "level": "L1", "type": "in-memory", "ttl": 300 },
      { "level": "L2", "type": "redis", "ttl": 3600 }
    ]
  }
}
```

#### 2. **Separate Rules from Orchestration**
```json
{
  "documentRules": {
    "storageType": "database",
    "table": "document_selection_rules",
    "version": "2.1",
    "lastUpdated": "2025-11-04T10:00:00Z"
  }
}
```

#### 3. **Add Observability**
```json
{
  "monitoring": {
    "traceId": "${context.traceId}",
    "metrics": ["latency", "cacheHitRate", "errorRate"],
    "logging": {
      "level": "INFO",
      "includePayload": false
    }
  }
}
```

#### 4. **Support Parallel Execution**
```json
{
  "executionGraph": {
    "parallel": [
      { "nodes": ["customerInfo", "accountInfo"] },
      { "nodes": ["creditCheck"], "dependsOn": ["customerInfo"] }
    ]
  }
}
```

---

## Implementation Roadmap

### Phase 1: Immediate (Week 1-2)
- ✅ Implement caching layer (Redis)
- ✅ Add retry logic with exponential backoff
- ✅ Implement response validation
- ✅ Add metrics and logging

### Phase 2: Short-term (Week 3-4)
- 📋 Implement rule engine with decision tables
- 📋 Move document selection rules to database
- 📋 Add rule versioning
- 📋 Create rule management UI

### Phase 3: Medium-term (Month 2-3)
- 📋 Implement pre-computed mappings for hot paths
- 📋 Add A/B testing support for rules
- 📋 Implement parallel execution for independent calls
- 📋 Add circuit breaker pattern

---

## Performance Comparison

| Approach | Avg Latency | Cache Hit | Scalability | Complexity |
|----------|-------------|-----------|-------------|------------|
| Sequential API Calls | 500-1000ms | N/A | Low | Medium |
| **API + Rule Engine** | **50-100ms** | **80-90%** | **High** | **Medium** |
| Pre-computed Table | 5-10ms | 95%+ | Very High | Low |
| GraphQL Federation | 100-200ms | 70-80% | High | High |

---

## Code Structure Recommendation

```
/api-orchestration-service
  /config
    - dataSources.json          # Your API orchestration config
    - executionRules.json       # Execution policies
  /rules
    - documentSelection.json    # Business rules
    - ruleEngine.ts            # Rule evaluation logic
  /cache
    - cacheManager.ts          # Multi-level caching
  /orchestrator
    - engine.ts                # Main orchestration engine
    - dataFetcher.ts           # API call handler
    - variableResolver.ts      # ${...} interpolation
  /monitoring
    - metrics.ts
    - logger.ts
```

---

## Testing Strategy

```javascript
// Mock mode for testing
{
  "executionRules": {
    "mode": "test",
    "mockResponses": {
      "customerInfo": {
        "data": { "name": "John Doe", "account": { "id": "123" } }
      }
    }
  }
}
```

---

## Conclusion

Your original design is a good starting point. For your **document selection use case**, I recommend:

1. **Keep your orchestration layer** for API calls (with enhancements: caching, retry, validation)
2. **Add a separate rule engine layer** for document selection logic
3. **Store rules in a database** (not in chained API calls)
4. **Implement multi-level caching** to reduce latency
5. **Add observability** from day one

This gives you the flexibility of dynamic configuration while maintaining performance and testability.
