# Drools vs Custom Rule Engine: Dynamic Configuration Analysis

**Document Type:** Technical Decision Analysis
**Date:** 2025-11-12
**Purpose:** Evaluate approaches for configuration-driven rule engine with dynamic external API calls
**Status:** CRITICAL DECISION

---

## Table of Contents

1. [The Problem](#the-problem)
2. [Requirements](#requirements)
3. [Approach 1: Drools with Typed Facts](#approach-1-drools-with-typed-facts)
4. [Approach 2: Drools with Dynamic Facts](#approach-2-drools-with-dynamic-facts)
5. [Approach 3: Custom Rule Engine](#approach-3-custom-rule-engine)
6. [Approach 4: Hybrid](#approach-4-hybrid)
7. [Head-to-Head Comparison](#head-to-head-comparison)
8. [Recommendation](#recommendation)

---

## The Problem

### Your Original Approach (Drools + Typed Java Classes)

```java
// ❌ PROBLEM: New API = New Java Class + Code Deployment

// For Arrangements API
public class ArrangementResponse { ... }

// For Cardholder Agreements API
public class CardholderAgreementResponse { ... }

// For Product API (NEW API = NEW CODE!)
public class ProductResponse { ... }  // ← Code change required!

// For Offers API (NEW API = NEW CODE!)
public class OfferResponse { ... }   // ← Code change required!
```

**Every new external API requires:**
1. ❌ Create new Java class
2. ❌ Update service layer
3. ❌ Update fact models
4. ❌ Update DRL rules
5. ❌ Recompile and redeploy application

### Your Real Requirement

**Add new external API calls via CONFIGURATION, not CODE:**

```yaml
# ✅ DESIRED: Add new API by editing config file, NO code changes

data_sources:
  - id: arrangements_api
    type: REST_API
    url: http://api.example.com/arrangements/{arrangementId}
    fields:
      - name: pricingId
        path: $.pricingId

  - id: cardholder_agreements_api
    type: REST_API
    url: http://api.example.com/agreements/{pricingId}
    depends_on: arrangements_api.pricingId  # ⭐ Chained call
    fields:
      - name: tncCode
        path: $.cardholderAgreementsTNCCode

  # ✅ Add new API - NO CODE CHANGE!
  - id: product_api
    type: REST_API
    url: http://api.example.com/products/{productId}
    fields:
      - name: productCategory
        path: $.category
      - name: features
        path: $.features
```

**Add new rules via CONFIGURATION:**

```yaml
rules:
  - rule_id: RULE-001
    conditions:
      - field: arrangements_api.pricingId      # ⭐ Dynamic field reference
        operator: EQUALS
        value: PRICING_GOLD
      - field: cardholder_agreements_api.tncCode
        operator: EQUALS
        value: TNC_GOLD_2024
      - field: account.balance
        operator: GREATER_THAN
        value: 10000
    action:
      add_document: DOC-GOLD-BENEFITS
```

---

## Requirements

### Must-Have

1. **✅ Add new external APIs without code changes**
   - Configure API endpoints, fields, mappings in YAML/DB
   - Support chained API calls (API A → use result in API B)

2. **✅ Add new rule conditions without code changes**
   - Reference any field from any configured API
   - Add operators without recompiling

3. **✅ Reactive and non-blocking**
   - All API calls reactive (Spring WebFlux)
   - High performance (1,000+ req/sec)

4. **✅ Support complex logic**
   - AND/OR conditions
   - Chained data dependencies
   - Pattern matching

### Nice-to-Have

5. **Database-driven configuration**
   - Store API configs in database
   - Store rules in database
   - Admin UI to manage both

6. **Version control**
   - Track rule changes
   - Rollback capability

---

## Approach 1: Drools with Typed Facts

### How It Works

```java
// Typed Java classes for each API
public class ArrangementFact {
    private String pricingId;
}

public class AgreementFact {
    private String tncCode;
}

// DRL rules
rule "Gold Benefits"
when
    ArrangementFact(pricingId == "PRICING_GOLD")
    AgreementFact(tncCode == "TNC_GOLD_2024")
then
    result.addDocument("DOC-GOLD-BENEFITS");
end
```

### Pros ✅

1. **Type Safety:** Compile-time checking
2. **IDE Support:** Autocomplete, refactoring
3. **Performance:** Optimized Rete algorithm
4. **Mature:** Battle-tested framework

### Cons ❌

1. **❌ DEALBREAKER: Every new API requires code changes**
   - New Java class
   - Redeploy application
   - No configuration-driven

2. **❌ Not flexible**
   - Can't add APIs at runtime
   - Can't change field mappings without code

3. **❌ Rule changes require DRL updates**
   - DRL files need to reference Java class names
   - Recompilation required

### Verdict

**❌ Does NOT meet requirement #1 (add APIs without code changes)**

---

## Approach 2: Drools with Dynamic Facts

### How It Works

Use `Map<String, Object>` instead of typed classes:

```java
// Generic fact holder
public class DynamicFact {
    private String sourceId;  // "arrangements_api", "cardholder_api"
    private Map<String, Object> data;

    public Object get(String field) {
        return data.get(field);
    }
}

// DRL rules with dynamic access
rule "Gold Benefits Dynamic"
when
    $arr: DynamicFact(sourceId == "arrangements_api")
    $agr: DynamicFact(sourceId == "cardholder_api")
    eval($arr.get("pricingId").equals("PRICING_GOLD"))
    eval($agr.get("tncCode").equals("TNC_GOLD_2024"))
then
    result.addDocument("DOC-GOLD-BENEFITS");
end
```

### Configuration-Driven API Calls

```yaml
# application.yml or database
data_sources:
  - id: arrangements_api
    url: http://api.example.com/arrangements/{arrangementId}
    response_mapping:
      pricingId: $.pricingId
      productCode: $.productCode

  - id: cardholder_api
    url: http://api.example.com/agreements/{pricingId}
    depends_on: arrangements_api.pricingId
    response_mapping:
      tncCode: $.cardholderAgreementsTNCCode
```

### Service Implementation

```java
@Service
public class DynamicDataService {

    public Mono<List<DynamicFact>> fetchAllData(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        // Load API configurations from database/YAML
        List<DataSourceConfig> configs = loadDataSourceConfigs();

        // Build reactive chain dynamically
        return buildReactiveChain(configs,
            Map.of(
                "customerId", customerId,
                "accountId", accountId,
                "arrangementId", arrangementId
            ))
            .collectList();
    }

    private Flux<DynamicFact> buildReactiveChain(
        List<DataSourceConfig> configs,
        Map<String, String> context
    ) {
        // Dynamically create reactive pipeline based on config
        // Handle dependencies (chained calls)
        // Return flux of DynamicFact objects
    }
}
```

### Pros ✅

1. **✅ Add new APIs via configuration**
   - No Java classes needed
   - Configure in YAML/database

2. **✅ Flexible field mappings**
   - JSONPath or similar
   - Runtime configuration

3. **✅ Drools performance**
   - Still uses Rete algorithm
   - Pattern matching optimized

### Cons ❌

1. **❌ Loss of type safety**
   - Runtime errors instead of compile-time
   - `eval()` is slow in Drools

2. **❌ Poor IDE support for rules**
   - No autocomplete
   - Harder to write rules

3. **❌ Complex DRL syntax**
   ```drl
   eval($arr.get("pricingId").equals("PRICING_GOLD"))  // Ugly!
   ```

4. **❌ Still requires DRL changes for new rules**
   - Rules still in DRL files
   - Need to reference sourceId strings

5. **❌ Performance hit**
   - `eval()` disables some Rete optimizations
   - Map lookups slower than field access

### Verdict

**⚠️ Partially meets requirements but awkward**
- ✅ APIs configurable
- ❌ Rules still need DRL updates
- ❌ Poor developer experience

---

## Approach 3: Custom Rule Engine (Configuration-Driven)

### How It Works

**Everything configured in YAML/Database:**

```yaml
# API Configuration
data_sources:
  - id: arrangements_api
    type: REST_API
    endpoint: http://api.example.com/arrangements/{arrangementId}
    method: GET
    timeout_ms: 5000
    response_mapping:
      - field_name: pricingId
        json_path: $.pricingId
      - field_name: productCode
        json_path: $.productCode

  - id: cardholder_api
    type: REST_API
    endpoint: http://api.example.com/agreements/{pricingId}
    method: GET
    depends_on:
      source: arrangements_api
      field: pricingId        # ⭐ Use this from previous call
    response_mapping:
      - field_name: tncCode
        json_path: $.cardholderAgreementsTNCCode
      - field_name: effectiveDate
        json_path: $.effectiveDate

  - id: account_service
    type: REST_API
    endpoint: http://api.example.com/accounts/{accountId}
    response_mapping:
      - field_name: balance
        json_path: $.balance
      - field_name: status
        json_path: $.status

# Rule Configuration
rules:
  - rule_id: RULE-001
    name: "Gold TNC Benefits"
    priority: 100
    conditions:
      logic_operator: AND
      conditions:
        - source: cardholder_api
          field: tncCode
          operator: EQUALS
          value: TNC_GOLD_2024

        - source: account_service
          field: balance
          operator: GREATER_THAN
          value: 10000

        - source: account_service
          field: status
          operator: EQUALS
          value: ACTIVE

    action:
      add_documents:
        - DOC-GOLD-BENEFITS
        - DOC-GOLD-EXCLUSIVE

  # ✅ Add new rule - NO CODE CHANGE!
  - rule_id: RULE-002
    name: "Premium Pricing Package"
    conditions:
      - source: arrangements_api
        field: pricingId
        operator: STARTS_WITH
        value: PRICING_PREMIUM
    action:
      add_documents:
        - DOC-PREMIUM-PACKAGE
```

### Implementation

```java
// 1. Configuration Models
@Data
public class DataSourceConfig {
    private String id;
    private String type;
    private String endpoint;
    private String method;
    private Integer timeoutMs;
    private List<ResponseMapping> responseMapping;
    private Dependency dependsOn;
}

@Data
public class RuleConfig {
    private String ruleId;
    private String name;
    private Integer priority;
    private ConditionGroup conditions;
    private RuleAction action;
}

// 2. Dynamic Data Fetcher
@Service
public class DynamicDataFetcher {

    public Mono<Map<String, Object>> fetchAllData(
        Map<String, String> inputs,
        List<DataSourceConfig> dataSourceConfigs
    ) {
        Map<String, Object> aggregatedData = new ConcurrentHashMap<>();

        // Build dependency graph
        Map<String, List<String>> dependencies = buildDependencyGraph(dataSourceConfigs);

        // Topological sort for execution order
        List<String> executionOrder = topologicalSort(dependencies);

        // Build reactive chain
        Mono<Map<String, Object>> chain = Mono.just(aggregatedData);

        for (String sourceId : executionOrder) {
            DataSourceConfig config = findConfig(dataSourceConfigs, sourceId);

            chain = chain.flatMap(data -> {
                // Resolve endpoint with placeholders
                String resolvedUrl = resolvePlaceholders(config.getEndpoint(), inputs, data);

                // Make API call
                return webClient.get()
                    .uri(resolvedUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(response -> {
                        // Extract fields using JSONPath
                        for (ResponseMapping mapping : config.getResponseMapping()) {
                            Object value = extractValue(response, mapping.getJsonPath());
                            String key = config.getId() + "." + mapping.getFieldName();
                            data.put(key, value);
                        }
                        return data;
                    });
            });
        }

        return chain;
    }
}

// 3. Custom Rule Evaluator
@Service
public class CustomRuleEvaluator {

    public Set<String> evaluateRules(
        Map<String, Object> data,
        List<RuleConfig> rules
    ) {
        Set<String> eligibleDocuments = new HashSet<>();

        // Sort by priority
        rules.sort(Comparator.comparing(RuleConfig::getPriority).reversed());

        for (RuleConfig rule : rules) {
            if (evaluateConditions(rule.getConditions(), data)) {
                eligibleDocuments.addAll(rule.getAction().getAddDocuments());
            }
        }

        return eligibleDocuments;
    }

    private boolean evaluateConditions(ConditionGroup group, Map<String, Object> data) {
        List<Boolean> results = new ArrayList<>();

        for (Condition condition : group.getConditions()) {
            String key = condition.getSource() + "." + condition.getField();
            Object value = data.get(key);

            boolean result = evaluateCondition(condition, value);
            results.add(result);
        }

        // Apply logic operator (AND/OR)
        if ("AND".equals(group.getLogicOperator())) {
            return results.stream().allMatch(Boolean::booleanValue);
        } else {
            return results.stream().anyMatch(Boolean::booleanValue);
        }
    }

    private boolean evaluateCondition(Condition condition, Object value) {
        switch (condition.getOperator()) {
            case "EQUALS":
                return Objects.equals(value, condition.getValue());
            case "GREATER_THAN":
                return ((Comparable) value).compareTo(condition.getValue()) > 0;
            case "STARTS_WITH":
                return value.toString().startsWith(condition.getValue().toString());
            // ... more operators
            default:
                throw new UnsupportedOperationException("Unknown operator: " +
                    condition.getOperator());
        }
    }
}

// 4. Main Service
@Service
public class ConfigDrivenEligibilityService {

    @Autowired
    private DynamicDataFetcher dataFetcher;

    @Autowired
    private CustomRuleEvaluator ruleEvaluator;

    @Autowired
    private RuleConfigRepository ruleConfigRepository;  // Load from DB

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    public Mono<Set<String>> getEligibleDocuments(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        return Mono.fromCallable(() -> {
            // Load configurations from database
            List<DataSourceConfig> dataSources = dataSourceConfigRepository.findAll();
            List<RuleConfig> rules = ruleConfigRepository.findAll();
            return new ConfigBundle(dataSources, rules);
        })
        .flatMap(config -> {
            // Fetch all data dynamically
            Map<String, String> inputs = Map.of(
                "customerId", customerId,
                "accountId", accountId,
                "arrangementId", arrangementId
            );

            return dataFetcher.fetchAllData(inputs, config.getDataSources())
                .map(data -> {
                    // Evaluate rules
                    return ruleEvaluator.evaluateRules(data, config.getRules());
                });
        });
    }
}
```

### Pros ✅

1. **✅ ✅ ✅ Fully configuration-driven**
   - Add new APIs via config (YAML/DB)
   - Add new rules via config
   - NO code changes ever!

2. **✅ Database-driven**
   - Store everything in database
   - Admin UI for configuration
   - Version control built-in

3. **✅ Maximum flexibility**
   - Add operators easily
   - Change field mappings
   - Modify dependency chains

4. **✅ Reactive**
   - All API calls non-blocking
   - Dynamic chain building

5. **✅ Runtime updates**
   - Change rules without restart
   - Hot reload capability

### Cons ❌

1. **❌ No Rete optimization**
   - Sequential rule evaluation (O(n))
   - Slower for 200+ rules

2. **❌ Must implement everything**
   - Operator logic
   - Condition evaluation
   - Dependency resolution
   - Caching

3. **❌ More code to maintain**
   - Custom evaluation engine
   - Bug fixes your responsibility

4. **❌ No DRL benefits**
   - No advanced features (accumulate, temporal)
   - No decision tables

### Verdict

**✅ ✅ ✅ MEETS ALL requirements**
- ✅ Add APIs without code changes
- ✅ Add rules without code changes
- ✅ Fully configuration-driven
- ⚠️ Performance acceptable up to ~100 rules
- ❌ Performance issues beyond 200 rules

---

## Approach 4: Hybrid (Custom + Drools)

### How It Works

**Use custom engine for data fetching, Drools for rule evaluation:**

```java
// 1. Custom data fetcher (configuration-driven)
@Service
public class HybridDataService {
    public Mono<DynamicFact> fetchAllData(...) {
        // Load API configs from database
        // Build reactive chain dynamically
        // Return single DynamicFact with all data as Map
    }
}

// 2. Drools with generic fact
rule "Gold Benefits"
when
    $fact: DynamicFact(
        get("cardholder_api.tncCode") == "TNC_GOLD_2024",
        get("account.balance") > 10000
    )
then
    result.addDocument("DOC-GOLD-BENEFITS");
end
```

### Pros ✅

1. **✅ APIs configuration-driven**
   - No code changes for new APIs

2. **✅ Drools performance**
   - Rete algorithm for rules

### Cons ❌

1. **❌ Rules still in DRL**
   - Need to update DRL for new rules
   - Defeats purpose

2. **❌ Complexity**
   - Two systems to maintain

### Verdict

**⚠️ Doesn't fully solve the problem**

---

## Head-to-Head Comparison

| Criteria | Drools Typed | Drools Dynamic | Custom Engine | Hybrid |
|----------|--------------|----------------|---------------|--------|
| **Add API without code** | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| **Add rule without code** | ❌ No | ❌ No | ✅ Yes | ❌ No |
| **Type safety** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **IDE support** | ✅ Excellent | ❌ Poor | ⚠️ N/A | ❌ Poor |
| **Performance (10 rules)** | ✅ Fast | ✅ Fast | ✅ Fast | ✅ Fast |
| **Performance (50 rules)** | ✅ Fast | ✅ Fast | ✅ Good | ✅ Fast |
| **Performance (200 rules)** | ✅ Fast | ✅ Good | ❌ Slow | ✅ Good |
| **Runtime rule updates** | ❌ No | ⚠️ Difficult | ✅ Easy | ⚠️ Difficult |
| **Database-driven** | ❌ No | ⚠️ Partial | ✅ Yes | ⚠️ Partial |
| **Admin UI possible** | ❌ No | ⚠️ Difficult | ✅ Easy | ⚠️ Difficult |
| **Implementation effort** | Low | Medium | **High** | High |
| **Maintenance burden** | Low | Medium | **High** | High |
| **Flexibility** | ❌ Low | ⚠️ Medium | ✅ **Highest** | ⚠️ Medium |

---

## Recommendation

### Based on Your Requirements

**Your stated need: Add APIs and rules without code changes**

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│  IF: Need to add APIs/rules via config (no code)      │
│  THEN: Custom Rule Engine                             │
│                                                        │
│  Reason: Only approach that allows BOTH:              │
│  ✅ API configuration without code                    │
│  ✅ Rule configuration without code                   │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### Decision Matrix

| Your Scenario | Recommendation |
|---------------|----------------|
| **Need configuration-driven APIs AND rules** | ✅ **Custom Rule Engine** |
| **< 50 rules, config-driven** | ✅ **Custom Rule Engine** |
| **50-100 rules, config-driven** | ✅ **Custom Rule Engine** (with caching) |
| **100-200 rules, config-driven** | ⚠️ **Custom Engine** (optimize performance) |
| **200+ rules, config-driven** | ⚠️ **Consider Drools Dynamic** (sacrifice flexibility) |
| **OK with typed classes, few API changes** | ✅ **Drools with Typed Facts** |

### Your Use Case

Based on:
- ✅ Want to add APIs without code changes
- ✅ Want to add rules without code changes
- ✅ Expect 50-200 rules
- ✅ Database-driven configuration desired

**Recommendation: Custom Rule Engine**

### Implementation Effort

```
Custom Rule Engine: 80-120 hours (2-3 sprints)
├─ Dynamic data fetcher: 24 hours
├─ Configuration models: 16 hours
├─ Rule evaluator: 32 hours
├─ Dependency resolver: 24 hours
├─ Testing: 24 hours
└─ Admin UI (optional): +40 hours
```

---

## Hybrid Recommendation (Best of Both)

### Start Custom, Evaluate Later

**Phase 1: Custom Engine (Weeks 1-4)**
- Implement configuration-driven engine
- Validate it works for your use case
- Get to 50-100 rules

**Phase 2: Evaluate Performance (Week 5)**
- Load test with 100-200 rules
- Measure evaluation time
- If < 50ms: ✅ Stay with custom
- If > 100ms: ⚠️ Consider migration

**Phase 3: Optimize or Migrate (Weeks 6+)**
- **IF performance OK:** Add caching, optimize
- **IF performance poor:** Migrate to Drools Dynamic (keep config-driven APIs)

---

## Next Steps

### If Choosing Custom Engine

1. **Review YAML schema design**
2. **Implement POC:**
   - Dynamic data fetcher
   - Simple rule evaluator
   - 5-10 sample rules
3. **Load test**
4. **Decide: proceed or pivot**

### If Choosing Drools

1. **Accept limitation:** Rule changes require DRL updates
2. **Use Drools Dynamic approach** for API configuration
3. **Implement:**
   - Configuration-driven API calls
   - DynamicFact model
   - DRL rules with eval()

---

## Summary

### The Hard Truth

**You must choose:**

```
Configuration-Driven Everything
  ✅ Add APIs without code
  ✅ Add rules without code
  ❌ Slower performance (200+ rules)
  ❌ More code to maintain
  → Custom Rule Engine

OR

Better Performance + Framework
  ✅ Fast (Rete algorithm)
  ✅ Mature framework
  ❌ DRL changes for new rules
  ❌ Less flexible
  → Drools (Dynamic or Typed)
```

**For your requirement (config-driven), Custom Engine wins.**

---

**Questions?**
1. How often will you add new APIs? (daily/weekly/monthly)
2. How many total rules do you expect? (50/100/200+)
3. Is Admin UI a must-have?
4. Can rules be deployed with code? (if yes, Drools viable)

Based on answers, I can refine the recommendation.

---

**Last Updated:** 2025-11-12
**Decision:** Pending user input
