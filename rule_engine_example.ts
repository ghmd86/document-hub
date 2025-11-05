/**
 * Rule Engine for Document Selection
 *
 * This demonstrates how to implement a flexible rule engine that works
 * with your API orchestration schema.
 */

// ============================================================================
// Types and Interfaces
// ============================================================================

interface RuleCondition {
  field: string;
  operator: 'equals' | 'notEquals' | 'in' | 'notIn' | 'greaterThan' | 'lessThan' | 'greaterThanOrEqual' | 'lessThanOrEqual' | 'contains' | 'matches';
  value: any;
}

interface Rule {
  id: string;
  priority: number;
  name: string;
  description?: string;
  conditions: {
    all?: RuleCondition[];  // AND conditions
    any?: RuleCondition[];  // OR conditions
  };
  output: {
    documentId: string;
    disclosureCode: string;
    documentVersion: string;
    metadata?: Record<string, any>;
  };
  validFrom?: string;
  validUntil?: string;
  enabled: boolean;
}

interface RuleSet {
  id: string;
  version: string;
  rules: Rule[];
  defaultOutput: Rule['output'];
}

interface EvaluationContext {
  accountType?: string;
  state?: string;
  balance?: number;
  creditLimit?: number;
  customerSegment?: string;
  [key: string]: any;
}

interface EvaluationResult {
  matched: boolean;
  rule?: Rule;
  output?: Rule['output'];
  executionTime: number;
  evaluatedRules: number;
}

// ============================================================================
// Rule Engine Implementation
// ============================================================================

class RuleEngine {
  private ruleSet: RuleSet;
  private cache: Map<string, EvaluationResult>;
  private cacheTTL: number;

  constructor(ruleSet: RuleSet, cacheTTL: number = 300000) {
    this.ruleSet = ruleSet;
    this.cache = new Map();
    this.cacheTTL = cacheTTL;
    this.sortRulesByPriority();
  }

  private sortRulesByPriority(): void {
    this.ruleSet.rules.sort((a, b) => a.priority - b.priority);
  }

  /**
   * Evaluate a single condition
   */
  private evaluateCondition(condition: RuleCondition, context: EvaluationContext): boolean {
    const fieldValue = this.getFieldValue(condition.field, context);

    switch (condition.operator) {
      case 'equals':
        return fieldValue === condition.value;

      case 'notEquals':
        return fieldValue !== condition.value;

      case 'in':
        return Array.isArray(condition.value) && condition.value.includes(fieldValue);

      case 'notIn':
        return Array.isArray(condition.value) && !condition.value.includes(fieldValue);

      case 'greaterThan':
        return typeof fieldValue === 'number' && fieldValue > condition.value;

      case 'lessThan':
        return typeof fieldValue === 'number' && fieldValue < condition.value;

      case 'greaterThanOrEqual':
        return typeof fieldValue === 'number' && fieldValue >= condition.value;

      case 'lessThanOrEqual':
        return typeof fieldValue === 'number' && fieldValue <= condition.value;

      case 'contains':
        return typeof fieldValue === 'string' && fieldValue.includes(condition.value);

      case 'matches':
        return typeof fieldValue === 'string' && new RegExp(condition.value).test(fieldValue);

      default:
        return false;
    }
  }

  /**
   * Get field value from context (supports nested paths like "account.balance")
   */
  private getFieldValue(path: string, context: EvaluationContext): any {
    return path.split('.').reduce((obj, key) => obj?.[key], context as any);
  }

  /**
   * Evaluate a rule against context
   */
  private evaluateRule(rule: Rule, context: EvaluationContext): boolean {
    if (!rule.enabled) {
      return false;
    }

    // Check validity period
    const now = new Date();
    if (rule.validFrom && new Date(rule.validFrom) > now) {
      return false;
    }
    if (rule.validUntil && new Date(rule.validUntil) < now) {
      return false;
    }

    // Evaluate ALL conditions (AND)
    if (rule.conditions.all) {
      const allMatch = rule.conditions.all.every(condition =>
        this.evaluateCondition(condition, context)
      );
      if (!allMatch) {
        return false;
      }
    }

    // Evaluate ANY conditions (OR)
    if (rule.conditions.any) {
      const anyMatch = rule.conditions.any.some(condition =>
        this.evaluateCondition(condition, context)
      );
      if (!anyMatch) {
        return false;
      }
    }

    return true;
  }

  /**
   * Generate cache key from context
   */
  private getCacheKey(context: EvaluationContext): string {
    const sortedKeys = Object.keys(context).sort();
    const keyParts = sortedKeys.map(key => `${key}:${context[key]}`);
    return keyParts.join('|');
  }

  /**
   * Evaluate context against all rules
   */
  evaluate(context: EvaluationContext, useCache: boolean = true): EvaluationResult {
    const startTime = Date.now();

    // Check cache
    if (useCache) {
      const cacheKey = this.getCacheKey(context);
      const cached = this.cache.get(cacheKey);
      if (cached) {
        return {
          ...cached,
          executionTime: Date.now() - startTime
        };
      }
    }

    // Evaluate rules in priority order
    let evaluatedRules = 0;
    for (const rule of this.ruleSet.rules) {
      evaluatedRules++;
      if (this.evaluateRule(rule, context)) {
        const result: EvaluationResult = {
          matched: true,
          rule,
          output: rule.output,
          executionTime: Date.now() - startTime,
          evaluatedRules
        };

        // Cache result
        if (useCache) {
          const cacheKey = this.getCacheKey(context);
          this.cache.set(cacheKey, result);
          setTimeout(() => this.cache.delete(cacheKey), this.cacheTTL);
        }

        return result;
      }
    }

    // No rule matched, return default
    const result: EvaluationResult = {
      matched: false,
      output: this.ruleSet.defaultOutput,
      executionTime: Date.now() - startTime,
      evaluatedRules
    };

    return result;
  }

  /**
   * Reload rules from external source
   */
  async reloadRules(newRuleSet: RuleSet): Promise<void> {
    this.ruleSet = newRuleSet;
    this.sortRulesByPriority();
    this.cache.clear();
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { size: number; hitRate: number } {
    // This would track hits/misses in a real implementation
    return {
      size: this.cache.size,
      hitRate: 0 // Would calculate from tracked metrics
    };
  }
}

// ============================================================================
// Example Rule Set
// ============================================================================

const documentSelectionRules: RuleSet = {
  id: "document-selection-v2",
  version: "2.0.0",
  rules: [
    {
      id: "rule-cc-california",
      priority: 1,
      name: "Credit Card - California Specific",
      description: "California requires additional disclosures for credit cards",
      enabled: true,
      conditions: {
        all: [
          { field: "accountType", operator: "equals", value: "CREDIT_CARD" },
          { field: "state", operator: "equals", value: "CA" }
        ]
      },
      output: {
        documentId: "CC_AGREEMENT_CA_v2.1",
        disclosureCode: "DISC_CC_CA_001",
        documentVersion: "2.1",
        metadata: {
          requiresAdditionalConsent: true,
          jurisdiction: "California"
        }
      }
    },
    {
      id: "rule-cc-high-limit",
      priority: 2,
      name: "Credit Card - High Limit",
      description: "High credit limit accounts get premium agreement",
      enabled: true,
      conditions: {
        all: [
          { field: "accountType", operator: "equals", value: "CREDIT_CARD" },
          { field: "creditLimit", operator: "greaterThanOrEqual", value: 50000 }
        ]
      },
      output: {
        documentId: "CC_AGREEMENT_PREMIUM_v1.5",
        disclosureCode: "DISC_CC_PREM_001",
        documentVersion: "1.5",
        metadata: {
          tier: "premium"
        }
      }
    },
    {
      id: "rule-cc-standard",
      priority: 3,
      name: "Credit Card - Standard",
      description: "Standard credit card agreement",
      enabled: true,
      conditions: {
        all: [
          { field: "accountType", operator: "equals", value: "CREDIT_CARD" }
        ]
      },
      output: {
        documentId: "CC_AGREEMENT_STANDARD_v2.0",
        disclosureCode: "DISC_CC_STD_001",
        documentVersion: "2.0"
      }
    },
    {
      id: "rule-checking-premium",
      priority: 4,
      name: "Checking - Premium",
      description: "Premium checking for high balance accounts",
      enabled: true,
      conditions: {
        all: [
          { field: "accountType", operator: "equals", value: "CHECKING" },
          { field: "balance", operator: "greaterThan", value: 10000 }
        ]
      },
      output: {
        documentId: "CHECKING_PREMIUM_AGREEMENT_v1.5",
        disclosureCode: "DISC_CHK_PREM_001",
        documentVersion: "1.5"
      }
    },
    {
      id: "rule-checking-standard",
      priority: 5,
      name: "Checking - Standard",
      enabled: true,
      conditions: {
        all: [
          { field: "accountType", operator: "in", value: ["CHECKING", "SAVINGS"] }
        ]
      },
      output: {
        documentId: "CHECKING_STANDARD_AGREEMENT_v1.2",
        disclosureCode: "DISC_CHK_STD_001",
        documentVersion: "1.2"
      }
    }
  ],
  defaultOutput: {
    documentId: "STANDARD_AGREEMENT_v1.0",
    disclosureCode: "DISC_DEFAULT",
    documentVersion: "1.0"
  }
};

// ============================================================================
// Usage Example
// ============================================================================

async function selectDocumentForAccount(accountData: EvaluationContext): Promise<Rule['output']> {
  const engine = new RuleEngine(documentSelectionRules);

  const result = engine.evaluate(accountData);

  console.log(`Evaluated ${result.evaluatedRules} rules in ${result.executionTime}ms`);
  console.log(`Matched: ${result.matched}`);
  console.log(`Selected document: ${result.output?.documentId}`);

  return result.output!;
}

// ============================================================================
// Test Cases
// ============================================================================

async function runTests() {
  console.log("=== Test 1: California Credit Card ===");
  const result1 = await selectDocumentForAccount({
    accountType: "CREDIT_CARD",
    state: "CA",
    creditLimit: 5000
  });
  console.log(result1);
  // Expected: CC_AGREEMENT_CA_v2.1

  console.log("\n=== Test 2: High Limit Credit Card (NY) ===");
  const result2 = await selectDocumentForAccount({
    accountType: "CREDIT_CARD",
    state: "NY",
    creditLimit: 75000
  });
  console.log(result2);
  // Expected: CC_AGREEMENT_PREMIUM_v1.5

  console.log("\n=== Test 3: Premium Checking ===");
  const result3 = await selectDocumentForAccount({
    accountType: "CHECKING",
    balance: 25000,
    state: "TX"
  });
  console.log(result3);
  // Expected: CHECKING_PREMIUM_AGREEMENT_v1.5

  console.log("\n=== Test 4: Standard Savings ===");
  const result4 = await selectDocumentForAccount({
    accountType: "SAVINGS",
    balance: 2000,
    state: "FL"
  });
  console.log(result4);
  // Expected: CHECKING_STANDARD_AGREEMENT_v1.2
}

// ============================================================================
// Integration with API Orchestration
// ============================================================================

class DocumentOrchestratorService {
  private ruleEngine: RuleEngine;
  private apiClient: any; // Your API client

  constructor(ruleSet: RuleSet, apiClient: any) {
    this.ruleEngine = new RuleEngine(ruleSet);
    this.apiClient = apiClient;
  }

  /**
   * Complete flow: Fetch data from API, then apply rules
   */
  async getDocumentForCustomer(customerId: string): Promise<{
    document: Rule['output'];
    customerData: any;
    executionMetrics: any;
  }> {
    const startTime = Date.now();

    // Step 1: Fetch account data from API (with caching)
    const accountData = await this.fetchAccountData(customerId);

    // Step 2: Apply rules to select document
    const result = this.ruleEngine.evaluate({
      accountType: accountData.type,
      state: accountData.state,
      balance: accountData.balance,
      creditLimit: accountData.creditLimit,
      customerSegment: accountData.customerSegment
    });

    return {
      document: result.output!,
      customerData: accountData,
      executionMetrics: {
        totalTime: Date.now() - startTime,
        ruleEvaluationTime: result.executionTime,
        rulesEvaluated: result.evaluatedRules,
        ruleMatched: result.matched
      }
    };
  }

  private async fetchAccountData(customerId: string): Promise<any> {
    // This would use your API orchestration layer
    // Check cache first, then fetch if needed
    return {
      customerId,
      type: "CREDIT_CARD",
      state: "CA",
      balance: 0,
      creditLimit: 10000,
      customerSegment: "retail"
    };
  }
}

// Export for use in your application
export {
  RuleEngine,
  DocumentOrchestratorService,
  type Rule,
  type RuleSet,
  type EvaluationContext,
  type EvaluationResult
};
