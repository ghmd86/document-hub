# Document Hub - Implementation Examples

This document provides practical code examples for implementing the Document Hub schema.

## Table of Contents
1. [Database Setup](#database-setup)
2. [Template Management](#template-management)
3. [Document Operations](#document-operations)
4. [API Integration Examples](#api-integration-examples)
5. [Redis Caching Examples](#redis-caching-examples)
6. [Testing Queries](#testing-queries)

---

## Database Setup

### Initial Database Creation

```sql
-- Create database
CREATE DATABASE document_hub
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Connect to database
\c document_hub

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For fuzzy text search
CREATE EXTENSION IF NOT EXISTS "btree_gin"; -- For composite GIN indexes

-- Run the schema script
\i document_hub_schema.sql
```

### Sample Data Insertion

```sql
-- Insert sample templates
INSERT INTO templates (
    template_code,
    version_number,
    template_name,
    description,
    document_type,
    document_category,
    status,
    retention_period_days,
    requires_signature,
    configuration,
    created_by
) VALUES
(
    'LOAN_APPLICATION',
    1,
    'Personal Loan Application Form v1',
    'Initial version of personal loan application',
    'LOAN_APPLICATION',
    'LOANS',
    'deprecated',
    2555,  -- 7 years
    true,
    '{"max_file_size_mb": 10, "allowed_formats": ["pdf", "docx"]}'::jsonb,
    'admin@bank.com'
),
(
    'LOAN_APPLICATION',
    2,
    'Personal Loan Application Form v2',
    'Updated version with enhanced validation',
    'LOAN_APPLICATION',
    'LOANS',
    'active',
    2555,
    true,
    '{"max_file_size_mb": 15, "allowed_formats": ["pdf", "docx", "jpg"]}'::jsonb,
    'admin@bank.com'
);

-- Insert template rules for version 2
INSERT INTO template_rules (
    template_id,
    rule_name,
    rule_type,
    rule_expression,
    rule_description,
    scope,
    execution_order,
    is_active,
    parameters,
    created_by
) VALUES
(
    (SELECT template_id FROM templates WHERE template_code = 'LOAN_APPLICATION' AND version_number = 2),
    'Validate SSN Format',
    'validation',
    '^\\d{3}-\\d{2}-\\d{4}$',
    'Ensures SSN follows XXX-XX-XXXX format',
    'upload',
    1,
    true,
    '{"field": "ssn", "required": true}'::jsonb,
    'admin@bank.com'
),
(
    (SELECT template_id FROM templates WHERE template_code = 'LOAN_APPLICATION' AND version_number = 2),
    'Validate Income Range',
    'validation',
    'income >= 25000 AND income <= 10000000',
    'Ensures income is within acceptable range',
    'upload',
    2,
    true,
    '{"field": "annual_income", "min": 25000, "max": 10000000}'::jsonb,
    'admin@bank.com'
);

-- Insert sample documents
INSERT INTO documents (
    ecms_document_id,
    customer_id,
    customer_name,
    account_id,
    account_type,
    document_type,
    document_category,
    template_id,
    template_code,
    template_version,
    document_name,
    document_description,
    file_extension,
    file_size_bytes,
    mime_type,
    status,
    document_date,
    is_confidential,
    requires_signature,
    signature_status,
    tags,
    metadata,
    created_by
) VALUES
(
    'ECMS-2024-001',
    'CUST-001',
    'John Doe',
    'ACC-12345',
    'SAVINGS',
    'LOAN_APPLICATION',
    'LOANS',
    (SELECT template_id FROM templates WHERE template_code = 'LOAN_APPLICATION' AND version_number = 2),
    'LOAN_APPLICATION',
    2,
    'Personal_Loan_Application_JohnDoe.pdf',
    'Personal loan application for home renovation',
    'pdf',
    2048576,  -- 2MB
    'application/pdf',
    'active',
    CURRENT_DATE,
    true,
    true,
    'pending',
    ARRAY['urgent', 'high-priority'],
    '{"loan_amount": 50000, "loan_purpose": "home_renovation", "loan_term_months": 60}'::jsonb,
    'upload-service'
);
```

---

## Template Management

### Create New Template Version

```sql
-- Function to create a new template version
CREATE OR REPLACE FUNCTION create_template_version(
    p_template_code VARCHAR,
    p_template_name VARCHAR,
    p_description TEXT,
    p_document_type VARCHAR,
    p_document_category VARCHAR,
    p_configuration JSONB,
    p_created_by VARCHAR
)
RETURNS UUID AS $$
DECLARE
    v_new_version_number INTEGER;
    v_new_template_id UUID;
    v_previous_template_id UUID;
BEGIN
    -- Get the next version number
    SELECT COALESCE(MAX(version_number), 0) + 1
    INTO v_new_version_number
    FROM templates
    WHERE template_code = p_template_code;

    -- Insert new template version as 'draft'
    INSERT INTO templates (
        template_code,
        version_number,
        template_name,
        description,
        document_type,
        document_category,
        status,
        configuration,
        created_by
    ) VALUES (
        p_template_code,
        v_new_version_number,
        p_template_name,
        p_description,
        p_document_type,
        p_document_category,
        'draft',
        p_configuration,
        p_created_by
    )
    RETURNING template_id INTO v_new_template_id;

    -- Optionally copy rules from previous version
    SELECT template_id INTO v_previous_template_id
    FROM templates
    WHERE template_code = p_template_code
      AND version_number = v_new_version_number - 1;

    IF v_previous_template_id IS NOT NULL THEN
        INSERT INTO template_rules (
            template_id,
            rule_name,
            rule_type,
            rule_expression,
            rule_description,
            scope,
            execution_order,
            is_active,
            parameters,
            created_by
        )
        SELECT
            v_new_template_id,
            rule_name,
            rule_type,
            rule_expression,
            rule_description,
            scope,
            execution_order,
            is_active,
            parameters,
            p_created_by
        FROM template_rules
        WHERE template_id = v_previous_template_id
          AND is_active = true;
    END IF;

    RETURN v_new_template_id;
END;
$$ LANGUAGE plpgsql;

-- Usage example
SELECT create_template_version(
    'LOAN_APPLICATION',
    'Personal Loan Application Form v3',
    'Enhanced version with biometric validation',
    'LOAN_APPLICATION',
    'LOANS',
    '{"max_file_size_mb": 20, "allowed_formats": ["pdf"], "biometric_required": true}'::jsonb,
    'admin@bank.com'
);
```

### Activate Template Version

```sql
-- Function to activate a template version (and deprecate others)
CREATE OR REPLACE FUNCTION activate_template_version(
    p_template_id UUID,
    p_activated_by VARCHAR
)
RETURNS VOID AS $$
DECLARE
    v_template_code VARCHAR;
BEGIN
    -- Get template code
    SELECT template_code INTO v_template_code
    FROM templates
    WHERE template_id = p_template_id;

    -- Deprecate all active versions of this template_code
    UPDATE templates
    SET status = 'deprecated',
        updated_by = p_activated_by,
        updated_at = NOW()
    WHERE template_code = v_template_code
      AND status = 'active';

    -- Activate the specified version
    UPDATE templates
    SET status = 'active',
        updated_by = p_activated_by,
        updated_at = NOW()
    WHERE template_id = p_template_id;
END;
$$ LANGUAGE plpgsql;

-- Usage example
SELECT activate_template_version(
    '550e8400-e29b-41d4-a716-446655440000',
    'admin@bank.com'
);
```

### Get Active Template with Rules

```sql
-- Function to get active template with all its rules
CREATE OR REPLACE FUNCTION get_active_template_with_rules(p_template_code VARCHAR)
RETURNS TABLE (
    template_id UUID,
    template_code VARCHAR,
    version_number INTEGER,
    template_name VARCHAR,
    document_type VARCHAR,
    document_category VARCHAR,
    configuration JSONB,
    rules JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.template_id,
        t.template_code,
        t.version_number,
        t.template_name,
        t.document_type,
        t.document_category,
        t.configuration,
        COALESCE(
            jsonb_agg(
                jsonb_build_object(
                    'rule_id', tr.rule_id,
                    'rule_name', tr.rule_name,
                    'rule_type', tr.rule_type,
                    'rule_expression', tr.rule_expression,
                    'execution_order', tr.execution_order,
                    'parameters', tr.parameters
                )
                ORDER BY tr.execution_order
            ) FILTER (WHERE tr.rule_id IS NOT NULL),
            '[]'::jsonb
        ) AS rules
    FROM templates t
    LEFT JOIN template_rules tr ON t.template_id = tr.template_id AND tr.is_active = true
    WHERE t.template_code = p_template_code
      AND t.status = 'active'
    GROUP BY t.template_id, t.template_code, t.version_number, t.template_name,
             t.document_type, t.document_category, t.configuration;
END;
$$ LANGUAGE plpgsql;

-- Usage example
SELECT * FROM get_active_template_with_rules('LOAN_APPLICATION');
```

---

## Document Operations

### Insert Document (Upload Flow)

```sql
-- Function to insert document after ECMS upload
CREATE OR REPLACE FUNCTION insert_document(
    p_ecms_document_id VARCHAR,
    p_customer_id VARCHAR,
    p_customer_name VARCHAR,
    p_account_id VARCHAR,
    p_account_type VARCHAR,
    p_document_type VARCHAR,
    p_document_category VARCHAR,
    p_template_code VARCHAR,
    p_document_name VARCHAR,
    p_file_extension VARCHAR,
    p_file_size_bytes BIGINT,
    p_mime_type VARCHAR,
    p_metadata JSONB,
    p_tags TEXT[],
    p_created_by VARCHAR
)
RETURNS UUID AS $$
DECLARE
    v_document_id UUID;
    v_template_id UUID;
    v_template_version INTEGER;
    v_requires_signature BOOLEAN;
BEGIN
    -- Get active template details
    SELECT template_id, version_number, requires_signature
    INTO v_template_id, v_template_version, v_requires_signature
    FROM templates
    WHERE template_code = p_template_code
      AND status = 'active'
    LIMIT 1;

    -- Insert document
    INSERT INTO documents (
        ecms_document_id,
        customer_id,
        customer_name,
        account_id,
        account_type,
        document_type,
        document_category,
        template_id,
        template_code,
        template_version,
        document_name,
        file_extension,
        file_size_bytes,
        mime_type,
        status,
        document_date,
        requires_signature,
        signature_status,
        tags,
        metadata,
        created_by
    ) VALUES (
        p_ecms_document_id,
        p_customer_id,
        p_customer_name,
        p_account_id,
        p_account_type,
        p_document_type,
        p_document_category,
        v_template_id,
        p_template_code,
        v_template_version,
        p_document_name,
        p_file_extension,
        p_file_size_bytes,
        p_mime_type,
        'active',
        CURRENT_DATE,
        v_requires_signature,
        CASE WHEN v_requires_signature THEN 'pending' ELSE NULL END,
        p_tags,
        p_metadata,
        p_created_by
    )
    RETURNING document_id INTO v_document_id;

    RETURN v_document_id;
END;
$$ LANGUAGE plpgsql;

-- Usage example
SELECT insert_document(
    'ECMS-2024-002',
    'CUST-002',
    'Jane Smith',
    'ACC-67890',
    'CHECKING',
    'ACCOUNT_STATEMENT',
    'STATEMENTS',
    'ACCOUNT_STATEMENT',
    'Statement_January_2024.pdf',
    'pdf',
    1048576,
    'application/pdf',
    '{"statement_month": "2024-01", "transaction_count": 42}'::jsonb,
    ARRAY['monthly', 'statement'],
    'upload-service'
);
```

### Search Documents

```sql
-- Function for advanced document search
CREATE OR REPLACE FUNCTION search_documents(
    p_customer_id VARCHAR DEFAULT NULL,
    p_account_id VARCHAR DEFAULT NULL,
    p_document_type VARCHAR DEFAULT NULL,
    p_document_category VARCHAR DEFAULT NULL,
    p_template_code VARCHAR DEFAULT NULL,
    p_status VARCHAR DEFAULT 'active',
    p_date_from DATE DEFAULT NULL,
    p_date_to DATE DEFAULT NULL,
    p_tags TEXT[] DEFAULT NULL,
    p_search_text VARCHAR DEFAULT NULL,
    p_limit INTEGER DEFAULT 100,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    document_id UUID,
    ecms_document_id VARCHAR,
    document_name VARCHAR,
    document_type VARCHAR,
    document_category VARCHAR,
    document_date DATE,
    uploaded_at TIMESTAMP WITH TIME ZONE,
    file_size_bytes BIGINT,
    template_code VARCHAR,
    template_version INTEGER,
    status VARCHAR,
    metadata JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.document_id,
        d.ecms_document_id,
        d.document_name,
        d.document_type,
        d.document_category,
        d.document_date,
        d.uploaded_at,
        d.file_size_bytes,
        d.template_code,
        d.template_version,
        d.status,
        d.metadata
    FROM documents d
    WHERE (p_customer_id IS NULL OR d.customer_id = p_customer_id)
      AND (p_account_id IS NULL OR d.account_id = p_account_id)
      AND (p_document_type IS NULL OR d.document_type = p_document_type)
      AND (p_document_category IS NULL OR d.document_category = p_document_category)
      AND (p_template_code IS NULL OR d.template_code = p_template_code)
      AND (p_status IS NULL OR d.status = p_status)
      AND (p_date_from IS NULL OR d.document_date >= p_date_from)
      AND (p_date_to IS NULL OR d.document_date <= p_date_to)
      AND (p_tags IS NULL OR d.tags && p_tags)  -- Array overlap operator
      AND (p_search_text IS NULL OR d.document_name ILIKE '%' || p_search_text || '%')
    ORDER BY d.uploaded_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Usage examples
-- 1. Get all loan applications for a customer
SELECT * FROM search_documents(
    p_customer_id => 'CUST-001',
    p_document_type => 'LOAN_APPLICATION'
);

-- 2. Get statements for an account in date range
SELECT * FROM search_documents(
    p_account_id => 'ACC-12345',
    p_document_category => 'STATEMENTS',
    p_date_from => '2024-01-01',
    p_date_to => '2024-12-31'
);

-- 3. Search by tag
SELECT * FROM search_documents(
    p_customer_id => 'CUST-001',
    p_tags => ARRAY['urgent', 'high-priority']
);
```

### Update Document Status

```sql
-- Function to archive old documents
CREATE OR REPLACE FUNCTION archive_old_documents(
    p_days_old INTEGER,
    p_archived_by VARCHAR
)
RETURNS INTEGER AS $$
DECLARE
    v_affected_count INTEGER;
BEGIN
    UPDATE documents
    SET status = 'archived',
        archived_at = NOW(),
        updated_by = p_archived_by,
        updated_at = NOW()
    WHERE status = 'active'
      AND uploaded_at < NOW() - (p_days_old || ' days')::INTERVAL;

    GET DIAGNOSTICS v_affected_count = ROW_COUNT;
    RETURN v_affected_count;
END;
$$ LANGUAGE plpgsql;

-- Archive documents older than 7 years (2555 days)
SELECT archive_old_documents(2555, 'archival-service');
```

---

## API Integration Examples

### Python/FastAPI Example

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import asyncpg
import json
from typing import Optional, List
from datetime import date

app = FastAPI()

# Database connection pool
async def get_db_pool():
    return await asyncpg.create_pool(
        host='localhost',
        port=5432,
        database='document_hub',
        user='your_user',
        password='your_password',
        min_size=10,
        max_size=50
    )

# Pydantic models
class DocumentUploadRequest(BaseModel):
    ecms_document_id: str
    customer_id: str
    customer_name: str
    account_id: Optional[str]
    account_type: Optional[str]
    document_type: str
    document_category: str
    template_code: str
    document_name: str
    file_extension: str
    file_size_bytes: int
    mime_type: str
    metadata: Optional[dict] = {}
    tags: Optional[List[str]] = []

class DocumentSearchRequest(BaseModel):
    customer_id: Optional[str] = None
    account_id: Optional[str] = None
    document_type: Optional[str] = None
    document_category: Optional[str] = None
    template_code: Optional[str] = None
    status: Optional[str] = 'active'
    date_from: Optional[date] = None
    date_to: Optional[date] = None
    tags: Optional[List[str]] = None
    search_text: Optional[str] = None
    limit: int = 100
    offset: int = 0

# API Endpoints
@app.post("/documents")
async def create_document(request: DocumentUploadRequest):
    """Create a new document record after ECMS upload"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        try:
            document_id = await conn.fetchval(
                """
                SELECT insert_document($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
                """,
                request.ecms_document_id,
                request.customer_id,
                request.customer_name,
                request.account_id,
                request.account_type,
                request.document_type,
                request.document_category,
                request.template_code,
                request.document_name,
                request.file_extension,
                request.file_size_bytes,
                request.mime_type,
                json.dumps(request.metadata),
                request.tags,
                'api-service'
            )
            return {"document_id": str(document_id), "status": "success"}
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

@app.post("/documents/search")
async def search_documents(request: DocumentSearchRequest):
    """Search documents with multiple filters"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT * FROM search_documents($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
            """,
            request.customer_id,
            request.account_id,
            request.document_type,
            request.document_category,
            request.template_code,
            request.status,
            request.date_from,
            request.date_to,
            request.tags,
            request.search_text,
            request.limit,
            request.offset
        )
        return [dict(row) for row in rows]

@app.get("/documents/{document_id}")
async def get_document(document_id: str):
    """Get document by ID"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            SELECT * FROM documents WHERE document_id = $1
            """,
            document_id
        )
        if row:
            return dict(row)
        raise HTTPException(status_code=404, detail="Document not found")

@app.get("/templates/{template_code}/active")
async def get_active_template(template_code: str):
    """Get active template with rules"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            SELECT * FROM get_active_template_with_rules($1)
            """,
            template_code
        )
        if row:
            return dict(row)
        raise HTTPException(status_code=404, detail="Template not found")
```

---

## Redis Caching Examples

### Python Redis Integration

```python
import redis
import json
from typing import Optional

# Redis client
redis_client = redis.Redis(
    host='localhost',
    port=6379,
    db=0,
    decode_responses=True
)

class DocumentCache:
    """Redis caching layer for documents"""

    @staticmethod
    def cache_active_template(template_code: str, template_data: dict, ttl: int = 3600):
        """Cache active template with 1-hour TTL"""
        key = f"template:active:{template_code}"
        redis_client.setex(key, ttl, json.dumps(template_data))

    @staticmethod
    def get_active_template(template_code: str) -> Optional[dict]:
        """Get active template from cache"""
        key = f"template:active:{template_code}"
        data = redis_client.get(key)
        return json.loads(data) if data else None

    @staticmethod
    def cache_document(ecms_document_id: str, document_data: dict, ttl: int = 300):
        """Cache document with 5-minute TTL"""
        key = f"document:{ecms_document_id}"
        redis_client.setex(key, ttl, json.dumps(document_data))

    @staticmethod
    def get_document(ecms_document_id: str) -> Optional[dict]:
        """Get document from cache"""
        key = f"document:{ecms_document_id}"
        data = redis_client.get(key)
        return json.loads(data) if data else None

    @staticmethod
    def invalidate_document(ecms_document_id: str):
        """Invalidate document cache"""
        key = f"document:{ecms_document_id}"
        redis_client.delete(key)

    @staticmethod
    def cache_customer_documents(customer_id: str, document_type: str, documents: list, ttl: int = 120):
        """Cache customer document list with 2-minute TTL"""
        key = f"customer:documents:{customer_id}:{document_type}"
        redis_client.setex(key, ttl, json.dumps(documents))

    @staticmethod
    def get_customer_documents(customer_id: str, document_type: str) -> Optional[list]:
        """Get customer documents from cache"""
        key = f"customer:documents:{customer_id}:{document_type}"
        data = redis_client.get(key)
        return json.loads(data) if data else None

# Usage in API endpoint
@app.get("/documents/{ecms_document_id}")
async def get_document_cached(ecms_document_id: str):
    # Try cache first
    cached = DocumentCache.get_document(ecms_document_id)
    if cached:
        return {"source": "cache", "data": cached}

    # Cache miss - query database
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT * FROM documents WHERE ecms_document_id = $1",
            ecms_document_id
        )
        if row:
            data = dict(row)
            # Cache for future requests
            DocumentCache.cache_document(ecms_document_id, data)
            return {"source": "database", "data": data}

    raise HTTPException(status_code=404, detail="Document not found")
```

---

## Testing Queries

### Performance Testing

```sql
-- Test query performance with EXPLAIN ANALYZE
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM documents
WHERE customer_id = 'CUST-001'
  AND document_type = 'LOAN_APPLICATION'
  AND status = 'active'
ORDER BY document_date DESC
LIMIT 100;

-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'documents'
ORDER BY idx_scan DESC;

-- Find unused indexes
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'
ORDER BY tablename, indexname;
```

### Data Validation Queries

```sql
-- Check for orphaned documents (template doesn't exist)
SELECT COUNT(*) AS orphaned_documents
FROM documents d
LEFT JOIN templates t ON d.template_id = t.template_id
WHERE t.template_id IS NULL;

-- Check for templates with no rules
SELECT t.template_code, t.version_number, t.status
FROM templates t
LEFT JOIN template_rules tr ON t.template_id = tr.template_id
WHERE tr.rule_id IS NULL
  AND t.status = 'active';

-- Verify partition distribution
SELECT
    'documents_p' || generate_series(0, 15) AS partition_name,
    COUNT(*) AS row_count
FROM documents
GROUP BY customer_id
ORDER BY partition_name;

-- Check template version consistency
SELECT
    template_code,
    COUNT(*) AS version_count,
    COUNT(*) FILTER (WHERE status = 'active') AS active_count,
    MAX(version_number) AS latest_version
FROM templates
GROUP BY template_code
HAVING COUNT(*) FILTER (WHERE status = 'active') > 1;  -- Should be 0 rows
```

---

## Summary

This implementation guide provides:
- Database setup and initial data seeding
- Template versioning functions
- Document CRUD operations
- API integration examples (Python/FastAPI)
- Redis caching patterns
- Performance testing queries

These examples can be adapted to your specific technology stack and requirements.
