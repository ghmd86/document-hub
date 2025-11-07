/**
 * Disclosure Code Extractor - Production Implementation
 *
 * This demonstrates how to implement the chained API extraction
 * for disclosure codes with all recommended enhancements.
 */

import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import Redis from 'ioredis';

// ============================================================================
// Types
// ============================================================================

interface ExtractionConfig {
  extractionStrategy: DataSource[];
  executionRules: ExecutionRules;
}

interface DataSource {
  id: string;
  description: string;
  endpoint: EndpointConfig;
  cache?: CacheConfig;
  responseMapping: ResponseMapping;
  errorHandling?: ErrorHandling;
  nextCalls?: NextCall[];
  storeAs?: string;
}

interface EndpointConfig {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  headers?: Record<string, string>;
  timeout?: number;
  retryPolicy?: RetryPolicy;
}

interface RetryPolicy {
  maxAttempts: number;
  backoffStrategy: 'linear' | 'exponential';
  initialDelayMs: number;
  maxDelayMs: number;
  retryOn: number[];
}

interface CacheConfig {
  enabled: boolean;
  ttl: number;
  keyPattern: string;
}

interface ResponseMapping {
  extract: Record<string, string>;
  transform?: Record<string, Transform>;
  validate?: Record<string, Validation>;
}

interface Transform {
  type: 'selectFirst' | 'uppercase' | 'lowercase' | 'trim';
  fallback?: any;
}

interface Validation {
  type: 'string' | 'number' | 'array' | 'date';
  required?: boolean;
  pattern?: string;
  errorMessage?: string;
}

interface ErrorHandling {
  onValidationError?: ErrorAction;
  on404?: ErrorAction;
  on5xx?: ErrorAction;
}

interface ErrorAction {
  action: 'fail' | 'return-default' | 'retry';
  defaultValue?: any;
  message?: string;
}

interface NextCall {
  condition?: Condition;
  dependsOn: string;
  targetDataSource: string;
}

interface Condition {
  field: string;
  operator: 'notNull' | 'equals' | 'greaterThan';
  value?: any;
}

interface ExecutionRules {
  startFrom: string;
  executionMode: 'sequential';
  stopOnError: boolean;
  errorHandling: {
    strategy: 'fail-fast' | 'continue-on-error';
    defaultResponse?: any;
  };
  monitoring?: MonitoringConfig;
  circuitBreaker?: CircuitBreakerConfig;
}

interface MonitoringConfig {
  logLevel: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  trackMetrics: boolean;
  metrics?: {
    latency?: boolean;
    cacheHitRate?: boolean;
    errorRate?: boolean;
  };
}

interface CircuitBreakerConfig {
  enabled: boolean;
  failureThreshold: number;
  resetTimeoutMs: number;
}

interface ExtractionResult {
  success: boolean;
  data?: any;
  error?: {
    code: string;
    message: string;
    details?: string;
  };
  metadata: {
    executionTimeMs: number;
    cacheHits: number;
    apiCalls: number;
    traceId: string;
  };
}

// ============================================================================
// Circuit Breaker Implementation
// ============================================================================

class CircuitBreaker {
  private state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' = 'CLOSED';
  private failureCount = 0;
  private lastFailureTime?: number;

  constructor(
    private config: CircuitBreakerConfig
  ) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (!this.config.enabled) {
      return fn();
    }

    if (this.state === 'OPEN') {
      if (Date.now() - (this.lastFailureTime || 0) > this.config.resetTimeoutMs) {
        this.state = 'HALF_OPEN';
        console.log('[CircuitBreaker] Transitioning to HALF_OPEN');
      } else {
        throw new Error('Circuit breaker is OPEN');
      }
    }

    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  private onSuccess(): void {
    this.failureCount = 0;
    if (this.state === 'HALF_OPEN') {
      this.state = 'CLOSED';
      console.log('[CircuitBreaker] Transitioning to CLOSED');
    }
  }

  private onFailure(): void {
    this.failureCount++;
    this.lastFailureTime = Date.now();

    if (this.failureCount >= this.config.failureThreshold) {
      this.state = 'OPEN';
      console.log('[CircuitBreaker] Transitioning to OPEN');
    }
  }
}

// ============================================================================
// Disclosure Extractor Implementation
// ============================================================================

class DisclosureExtractor {
  private httpClient: AxiosInstance;
  private cache: Redis;
  private metrics: Map<string, number>;
  private circuitBreaker?: CircuitBreaker;

  constructor(
    private config: ExtractionConfig,
    cacheClient: Redis
  ) {
    this.httpClient = axios.create();
    this.cache = cacheClient;
    this.metrics = new Map();

    if (config.executionRules.circuitBreaker?.enabled) {
      this.circuitBreaker = new CircuitBreaker(config.executionRules.circuitBreaker);
    }
  }

  /**
   * Main extraction method
   */
  async extract(
    input: Record<string, any>,
    correlationId: string
  ): Promise<ExtractionResult> {
    const startTime = Date.now();
    const context: any = {
      input,
      correlationId,
      cacheHits: 0,
      apiCalls: 0
    };

    try {
      const result = await this.executeStrategy(context);

      return {
        success: true,
        data: result,
        metadata: {
          executionTimeMs: Date.now() - startTime,
          cacheHits: context.cacheHits,
          apiCalls: context.apiCalls,
          traceId: correlationId
        }
      };
    } catch (error: any) {
      console.error('[DisclosureExtractor] Extraction failed:', error);

      // Return default response if configured
      if (this.config.executionRules.errorHandling.defaultResponse) {
        return {
          success: false,
          data: this.config.executionRules.errorHandling.defaultResponse,
          error: {
            code: 'EXTRACTION_FAILED',
            message: error.message,
            details: error.stack
          },
          metadata: {
            executionTimeMs: Date.now() - startTime,
            cacheHits: context.cacheHits,
            apiCalls: context.apiCalls,
            traceId: correlationId
          }
        };
      }

      throw error;
    }
  }

  /**
   * Execute extraction strategy
   */
  private async executeStrategy(context: any): Promise<any> {
    const { startFrom } = this.config.executionRules;
    let currentDataSource = this.getDataSource(startFrom);
    const results: Record<string, any> = {};

    while (currentDataSource) {
      console.log(`[DisclosureExtractor] Executing: ${currentDataSource.id}`);

      // Execute current data source
      const result = await this.executeDataSource(currentDataSource, context, results);

      if (currentDataSource.storeAs) {
        results[currentDataSource.storeAs] = result;
      }
      results[currentDataSource.id] = result;

      // Determine next data source
      currentDataSource = this.getNextDataSource(currentDataSource, results);
    }

    return results;
  }

  /**
   * Execute a single data source
   */
  private async executeDataSource(
    dataSource: DataSource,
    context: any,
    previousResults: Record<string, any>
  ): Promise<any> {
    // Check cache first
    if (dataSource.cache?.enabled) {
      const cacheKey = this.interpolateString(
        dataSource.cache.keyPattern,
        context,
        previousResults
      );
      const cached = await this.getFromCache(cacheKey);

      if (cached) {
        context.cacheHits++;
        console.log(`[DisclosureExtractor] Cache hit: ${cacheKey}`);
        return cached;
      }
    }

    // Make API call
    const executeCall = async () => {
      const response = await this.makeAPICall(dataSource, context, previousResults);

      // Extract data from response
      const extracted = this.extractData(
        response.data,
        dataSource.responseMapping.extract
      );

      // Transform data
      const transformed = this.transformData(
        extracted,
        dataSource.responseMapping.transform
      );

      // Validate data
      this.validateData(
        transformed,
        dataSource.responseMapping.validate,
        dataSource.errorHandling
      );

      return transformed;
    };

    // Execute with circuit breaker if enabled
    const result = this.circuitBreaker
      ? await this.circuitBreaker.execute(executeCall)
      : await executeCall();

    // Store in cache
    if (dataSource.cache?.enabled) {
      const cacheKey = this.interpolateString(
        dataSource.cache.keyPattern,
        context,
        previousResults
      );
      await this.setInCache(cacheKey, result, dataSource.cache.ttl);
    }

    context.apiCalls++;
    return result;
  }

  /**
   * Make API call with retry logic
   */
  private async makeAPICall(
    dataSource: DataSource,
    context: any,
    previousResults: Record<string, any>
  ): Promise<any> {
    const { endpoint } = dataSource;
    const url = this.interpolateString(endpoint.url, context, previousResults);
    const headers = this.interpolateObject(endpoint.headers || {}, context, previousResults);

    const config: AxiosRequestConfig = {
      method: endpoint.method,
      url,
      headers,
      timeout: endpoint.timeout || 5000
    };

    const retryPolicy = endpoint.retryPolicy;
    let lastError: any;

    const maxAttempts = retryPolicy?.maxAttempts || 1;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        console.log(`[DisclosureExtractor] API call attempt ${attempt}/${maxAttempts}: ${url}`);
        const response = await this.httpClient.request(config);
        return response;
      } catch (error: any) {
        lastError = error;
        const statusCode = error.response?.status;

        // Check if we should retry
        const shouldRetry =
          attempt < maxAttempts &&
          retryPolicy &&
          retryPolicy.retryOn.includes(statusCode);

        if (shouldRetry) {
          const delay = this.calculateBackoff(
            attempt,
            retryPolicy.backoffStrategy,
            retryPolicy.initialDelayMs,
            retryPolicy.maxDelayMs
          );
          console.log(`[DisclosureExtractor] Retrying after ${delay}ms...`);
          await this.sleep(delay);
        } else {
          // Handle error based on config
          if (dataSource.errorHandling) {
            if (statusCode === 404 && dataSource.errorHandling.on404) {
              return this.handleError(dataSource.errorHandling.on404, error);
            }
            if (statusCode >= 500 && dataSource.errorHandling.on5xx) {
              return this.handleError(dataSource.errorHandling.on5xx, error);
            }
          }
          throw error;
        }
      }
    }

    throw lastError;
  }

  /**
   * Extract data using JSONPath-like expressions
   */
  private extractData(
    data: any,
    extractConfig: Record<string, string>
  ): Record<string, any> {
    const result: Record<string, any> = {};

    for (const [key, path] of Object.entries(extractConfig)) {
      result[key] = this.evaluateJSONPath(data, path);
    }

    return result;
  }

  /**
   * Simple JSONPath evaluator
   * Supports: $.field, $.array[0], $.array[?(@.prop == 'value')].field
   */
  private evaluateJSONPath(data: any, path: string): any {
    // Remove leading $
    path = path.replace(/^\$\.?/, '');

    // Check for array selector [0]
    const selectFirstMatch = path.match(/\|\s*\[0\]$/);
    if (selectFirstMatch) {
      path = path.replace(/\|\s*\[0\]$/, '');
    }

    // Check for filter [?(@.prop == 'value')]
    const filterMatch = path.match(/\[\?\(@\.(\w+)\s*==\s*'([^']+)'\)\]/);

    let current = data;
    const parts = path.split('.');

    for (let part of parts) {
      if (!current) return null;

      // Handle array with filter
      if (filterMatch && part.includes('[?')) {
        const [arrayPart, ...rest] = part.split('[');
        current = current[arrayPart];

        if (Array.isArray(current)) {
          const [, filterProp, filterValue] = filterMatch;
          current = current.filter((item: any) =>
            item[filterProp] === filterValue
          );
        }

        // Continue with remaining path
        if (rest.length > 0) {
          const remainingPath = rest.join('[').replace(/^\)\]\.?/, '');
          if (remainingPath) {
            current = current.map((item: any) =>
              this.evaluateJSONPath(item, remainingPath)
            );
          }
        }
      } else {
        current = current[part];
      }
    }

    // Apply [0] selector if specified
    if (selectFirstMatch && Array.isArray(current)) {
      current = current[0];
    }

    return current;
  }

  /**
   * Transform extracted data
   */
  private transformData(
    data: Record<string, any>,
    transformConfig?: Record<string, Transform>
  ): Record<string, any> {
    if (!transformConfig) return data;

    const result = { ...data };

    for (const [key, transform] of Object.entries(transformConfig)) {
      const value = result[key];

      switch (transform.type) {
        case 'selectFirst':
          result[key] = Array.isArray(value) ? value[0] : value;
          if (result[key] === undefined && transform.fallback !== undefined) {
            result[key] = transform.fallback;
          }
          break;
        case 'uppercase':
          result[key] = typeof value === 'string' ? value.toUpperCase() : value;
          break;
        case 'lowercase':
          result[key] = typeof value === 'string' ? value.toLowerCase() : value;
          break;
        case 'trim':
          result[key] = typeof value === 'string' ? value.trim() : value;
          break;
      }
    }

    return result;
  }

  /**
   * Validate extracted data
   */
  private validateData(
    data: Record<string, any>,
    validationConfig?: Record<string, Validation>,
    errorHandling?: ErrorHandling
  ): void {
    if (!validationConfig) return;

    for (const [key, validation] of Object.entries(validationConfig)) {
      const value = data[key];

      // Check required
      if (validation.required && (value === null || value === undefined)) {
        const error = new Error(
          validation.errorMessage || `Missing required field: ${key}`
        );
        if (errorHandling?.onValidationError) {
          this.handleError(errorHandling.onValidationError, error);
        } else {
          throw error;
        }
      }

      // Check pattern
      if (validation.pattern && typeof value === 'string') {
        const regex = new RegExp(validation.pattern);
        if (!regex.test(value)) {
          const error = new Error(
            validation.errorMessage || `Invalid format for field: ${key}`
          );
          if (errorHandling?.onValidationError) {
            this.handleError(errorHandling.onValidationError, error);
          } else {
            throw error;
          }
        }
      }
    }
  }

  /**
   * Handle errors based on configuration
   */
  private handleError(action: ErrorAction, error: any): any {
    if (action.action === 'return-default') {
      console.warn(`[DisclosureExtractor] Returning default value:`, action.defaultValue);
      return { data: action.defaultValue };
    }
    throw error;
  }

  /**
   * Get next data source based on conditions
   */
  private getNextDataSource(
    current: DataSource,
    results: Record<string, any>
  ): DataSource | null {
    if (!current.nextCalls || current.nextCalls.length === 0) {
      return null;
    }

    for (const nextCall of current.nextCalls) {
      // Check condition if specified
      if (nextCall.condition) {
        const fieldValue = results[current.id][nextCall.condition.field];

        if (nextCall.condition.operator === 'notNull') {
          if (fieldValue === null || fieldValue === undefined) {
            continue;
          }
        }
      }

      return this.getDataSource(nextCall.targetDataSource);
    }

    return null;
  }

  private getDataSource(id: string): DataSource {
    const ds = this.config.extractionStrategy.find(d => d.id === id);
    if (!ds) {
      throw new Error(`Data source not found: ${id}`);
    }
    return ds;
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private interpolateString(
    template: string,
    context: any,
    results: Record<string, any>
  ): string {
    return template.replace(/\$\{([^}]+)\}/g, (match, path) => {
      if (path.startsWith('$input.')) {
        return context.input[path.replace('$input.', '')];
      }
      if (path.startsWith('env.')) {
        return process.env[path.replace('env.', '')] || '';
      }
      if (path === 'x-correlation-Id') {
        return context.correlationId;
      }

      // Look in previous results
      const [dataSourceId, field] = path.split('.');
      return results[dataSourceId]?.[field] || match;
    });
  }

  private interpolateObject(
    obj: Record<string, string>,
    context: any,
    results: Record<string, any>
  ): Record<string, string> {
    const result: Record<string, string> = {};
    for (const [key, value] of Object.entries(obj)) {
      result[key] = this.interpolateString(value, context, results);
    }
    return result;
  }

  private async getFromCache(key: string): Promise<any> {
    const cached = await this.cache.get(key);
    return cached ? JSON.parse(cached) : null;
  }

  private async setInCache(key: string, value: any, ttl: number): Promise<void> {
    await this.cache.setex(key, ttl, JSON.stringify(value));
  }

  private calculateBackoff(
    attempt: number,
    strategy: 'linear' | 'exponential',
    initialDelay: number,
    maxDelay: number
  ): number {
    const delay =
      strategy === 'exponential'
        ? initialDelay * Math.pow(2, attempt - 1)
        : initialDelay * attempt;

    return Math.min(delay, maxDelay);
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// ============================================================================
// Usage Example
// ============================================================================

async function main() {
  // Configuration (from your JSON)
  const config: ExtractionConfig = {
    extractionStrategy: [
      {
        id: 'getAccountArrangements',
        description: 'Get account arrangements',
        endpoint: {
          url: 'https://devapi.vda.dv01.c1busw2.aws/private/creditcard/accounts/${$input.accountId}/arrangements',
          method: 'GET',
          headers: {
            'x-correlation-Id': '${x-correlation-Id}',
            'apikey': '${env.API_KEY}'
          },
          timeout: 5000,
          retryPolicy: {
            maxAttempts: 3,
            backoffStrategy: 'exponential',
            initialDelayMs: 100,
            maxDelayMs: 2000,
            retryOn: [500, 502, 503, 504]
          }
        },
        cache: {
          enabled: true,
          ttl: 1800,
          keyPattern: 'arrangements:${$input.accountId}'
        },
        responseMapping: {
          extract: {
            pricingId: "$.content[?(@.domain == 'PRICING')].domainId | [0]"
          },
          validate: {
            pricingId: {
              type: 'string',
              required: true,
              errorMessage: 'No pricing arrangement found'
            }
          }
        },
        nextCalls: [
          {
            condition: { field: 'pricingId', operator: 'notNull' },
            dependsOn: 'pricingId',
            targetDataSource: 'getPricingData'
          }
        ]
      },
      {
        id: 'getPricingData',
        description: 'Get pricing data',
        endpoint: {
          url: 'https://devapi.vda.dv01.c1busw2.aws/private/enterprise/product-management-system/pricing-management-service/prices/${pricingId}',
          method: 'GET',
          headers: {
            'x-correlation-Id': '${x-correlation-Id}',
            'apikey': '${env.API_KEY}'
          },
          timeout: 5000
        },
        cache: {
          enabled: true,
          ttl: 3600,
          keyPattern: 'pricing:${pricingId}'
        },
        responseMapping: {
          extract: {
            disclosureCode: '$.cardholderAgreementsTncCode'
          },
          validate: {
            disclosureCode: {
              type: 'string',
              required: true,
              pattern: '^DISC_[A-Z0-9_]+$'
            }
          }
        },
        storeAs: 'disclosureData'
      }
    ],
    executionRules: {
      startFrom: 'getAccountArrangements',
      executionMode: 'sequential',
      stopOnError: true,
      errorHandling: {
        strategy: 'fail-fast',
        defaultResponse: {
          disclosureCode: 'DEFAULT_DISCLOSURE'
        }
      },
      monitoring: {
        logLevel: 'INFO',
        trackMetrics: true
      },
      circuitBreaker: {
        enabled: true,
        failureThreshold: 5,
        resetTimeoutMs: 60000
      }
    }
  };

  // Initialize extractor
  const redisClient = new Redis({
    host: 'localhost',
    port: 6379
  });

  const extractor = new DisclosureExtractor(config, redisClient);

  // Extract disclosure code
  const result = await extractor.extract(
    { accountId: 'ACC_123456' },
    'correlation-id-abc-123'
  );

  console.log('Extraction result:', JSON.stringify(result, null, 2));

  await redisClient.quit();
}

// Run if executed directly
if (require.main === module) {
  main().catch(console.error);
}

export { DisclosureExtractor, ExtractionConfig, ExtractionResult };
