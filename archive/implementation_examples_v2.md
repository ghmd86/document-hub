# Document Hub v2 - Implementation Examples (Shared Documents)

This document provides practical code examples for implementing the v2 schema with shared document support.

## Table of Contents
1. [Database Setup](#database-setup)
2. [Document Operations with Access Control](#document-operations-with-access-control)
3. [Sharing Documents](#sharing-documents)
4. [Access Control Functions](#access-control-functions)
5. [API Integration Examples](#api-integration-examples)
6. [Redis Caching for Shared Documents](#redis-caching-for-shared-documents)
7. [Common Query Patterns](#common-query-patterns)

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
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- Run the v2 schema script
\i document_hub_schema_v2.sql
```

### Sample Data Insertion

```sql
-- Insert sample templates (same as v1)
INSERT INTO templates (
    template_code, version_number, template_name, description,
    document_type, document_category, status, created_by
) VALUES
('LOAN_APPLICATION', 1, 'Loan Application v1', 'Personal loan application',
 'LOAN_APPLICATION', 'LOANS', 'active', 'admin@bank.com'),
('ACCOUNT_STATEMENT', 1, 'Monthly Statement v1', 'Monthly account statement',
 'ACCOUNT_STATEMENT', 'STATEMENTS', 'active', 'admin@bank.com');

-- Insert template rules
INSERT INTO template_rules (
    template_id, rule_name, rule_type, rule_expression,
    scope, execution_order, created_by
)
SELECT
    template_id,
    'Validate File Size',
    'validation',
    'file_size_bytes <= 15728640',  -- 15 MB
    'upload',
    1,
    'admin@bank.com'
FROM templates
WHERE template_code = 'LOAN_APPLICATION' AND version_number = 1;

-- Insert sample documents (v2 structure - no customer_id)
INSERT INTO documents (
    ecms_document_id,
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
    sharing_scope,  -- NEW in v2
    document_date,
    is_confidential,
    created_by
) VALUES
(
    'ECMS-2024-001',
    'LOAN_APPLICATION',
    'LOANS',
    (SELECT template_id FROM templates WHERE template_code = 'LOAN_APPLICATION' AND version_number = 1),
    'LOAN_APPLICATION',
    1,
    'Personal_Loan_Application_JohnDoe.pdf',
    'pdf',
    2048576,
    'application/pdf',
    'active',
    'private',  -- Single owner
    CURRENT_DATE,
    true,
    'upload-service'
),
(
    'ECMS-2024-002',
    'ACCOUNT_STATEMENT',
    'STATEMENTS',
    (SELECT template_id FROM templates WHERE template_code = 'ACCOUNT_STATEMENT' AND version_number = 1),
    'ACCOUNT_STATEMENT',
    1,
    'Joint_Account_Statement_Jan2024.pdf',
    'pdf',
    512000,
    'application/pdf',
    'active',
    'shared',  -- Shared document
    '2024-01-31',
    false,
    'statement-service'
),
(
    'ECMS-2024-003',
    'TERMS_CONDITIONS',
    'LEGAL',
    NULL,
    NULL,
    NULL,
    'Terms_and_Conditions_2024.pdf',
    'pdf',
    1024000,
    'application/pdf',
    'active',
    'public',  -- Public document
    CURRENT_DATE,
    false,
    'legal-team'
);

-- Grant access to documents

-- Document 1: Private (single owner)
INSERT INTO document_access (document_id, entity_type, entity_id, entity_name, access_level, granted_by)
SELECT document_id, 'customer', 'CUST-001', 'John Doe', 'owner', 'upload-service'
FROM documents WHERE ecms_document_id = 'ECMS-2024-001';

-- Document 2: Shared (joint account - multiple customers)
INSERT INTO document_access (document_id, entity_type, entity_id, entity_name, access_level, granted_by)
SELECT document_id, 'customer', 'CUST-002', 'Jane Smith', 'owner', 'statement-service'
FROM documents WHERE ecms_document_id = 'ECMS-2024-002'
UNION ALL
SELECT document_id, 'customer', 'CUST-003', 'Bob Johnson', 'view', 'CUST-002'
FROM documents WHERE ecms_document_id = 'ECMS-2024-002'
UNION ALL
SELECT document_id, 'account', 'ACC-12345', 'Joint Savings Account', 'view', 'system'
FROM documents WHERE ecms_document_id = 'ECMS-2024-002';

-- Document 3: Public (everyone can access)
INSERT INTO document_access (document_id, entity_type, entity_id, access_level, granted_by)
SELECT document_id, 'public', NULL, 'view', 'legal-team'
FROM documents WHERE ecms_document_id = 'ECMS-2024-003';
```

---

## Document Operations with Access Control

### Create Document with Access Grant

```sql
-- Function to create document and grant initial access
CREATE OR REPLACE FUNCTION create_document_with_access(
    p_ecms_document_id VARCHAR,
    p_document_type VARCHAR,
    p_document_category VARCHAR,
    p_template_code VARCHAR,
    p_document_name VARCHAR,
    p_file_extension VARCHAR,
    p_file_size_bytes BIGINT,
    p_mime_type VARCHAR,
    p_sharing_scope VARCHAR,
    p_metadata JSONB,
    p_tags TEXT[],
    p_created_by VARCHAR,
    -- Access grant parameters
    p_owner_entity_type VARCHAR,
    p_owner_entity_id VARCHAR,
    p_owner_entity_name VARCHAR
)
RETURNS TABLE (
    document_id UUID,
    access_id UUID
) AS $$
DECLARE
    v_document_id UUID;
    v_access_id UUID;
    v_template_id UUID;
    v_template_version INTEGER;
BEGIN
    -- Get active template
    SELECT t.template_id, t.version_number
    INTO v_template_id, v_template_version
    FROM templates t
    WHERE t.template_code = p_template_code
      AND t.status = 'active'
    LIMIT 1;

    -- Insert document
    INSERT INTO documents (
        ecms_document_id,
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
        sharing_scope,
        document_date,
        metadata,
        tags,
        created_by
    ) VALUES (
        p_ecms_document_id,
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
        p_sharing_scope,
        CURRENT_DATE,
        p_metadata,
        p_tags,
        p_created_by
    )
    RETURNING documents.document_id INTO v_document_id;

    -- Grant access to owner
    IF p_owner_entity_type != 'public' THEN
        INSERT INTO document_access (
            document_id,
            entity_type,
            entity_id,
            entity_name,
            access_level,
            granted_by
        ) VALUES (
            v_document_id,
            p_owner_entity_type,
            p_owner_entity_id,
            p_owner_entity_name,
            'owner',
            p_created_by
        )
        RETURNING document_access.access_id INTO v_access_id;
    ELSE
        -- Public document
        INSERT INTO document_access (
            document_id,
            entity_type,
            entity_id,
            access_level,
            granted_by
        ) VALUES (
            v_document_id,
            'public',
            NULL,
            'view',
            p_created_by
        )
        RETURNING document_access.access_id INTO v_access_id;
    END IF;

    RETURN QUERY SELECT v_document_id, v_access_id;
END;
$$ LANGUAGE plpgsql;

-- Usage example: Create private document
SELECT * FROM create_document_with_access(
    'ECMS-2024-100',
    'LOAN_APPLICATION',
    'LOANS',
    'LOAN_APPLICATION',
    'Mortgage_Application.pdf',
    'pdf',
    3145728,
    'application/pdf',
    'private',
    '{"loan_amount": 250000, "property_type": "house"}'::jsonb,
    ARRAY['mortgage', 'urgent'],
    'api-service',
    'customer',
    'CUST-123',
    'Alice Williams'
);
```

---

## Sharing Documents

### Share Document with Additional Entities

```sql
-- Function to share document (grant additional access)
CREATE OR REPLACE FUNCTION share_document(
    p_document_id UUID,
    p_entity_type VARCHAR,
    p_entity_id VARCHAR,
    p_entity_name VARCHAR,
    p_access_level VARCHAR DEFAULT 'view',
    p_granted_by VARCHAR DEFAULT 'system',
    p_expires_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_access_id UUID;
    v_current_scope VARCHAR;
BEGIN
    -- Get current sharing scope
    SELECT sharing_scope INTO v_current_scope
    FROM documents
    WHERE document_id = p_document_id;

    -- Update sharing scope if needed
    IF v_current_scope = 'private' THEN
        UPDATE documents
        SET sharing_scope = 'shared',
            updated_at = NOW()
        WHERE document_id = p_document_id;
    END IF;

    -- Grant access (use function from schema)
    v_access_id := grant_document_access(
        p_document_id,
        p_entity_type,
        p_entity_id,
        p_entity_name,
        p_access_level,
        p_granted_by,
        p_expires_at
    );

    RETURN v_access_id;
END;
$$ LANGUAGE plpgsql;

-- Usage example: Share document with another customer
SELECT share_document(
    document_id => (SELECT document_id FROM documents WHERE ecms_document_id = 'ECMS-2024-001'),
    entity_type => 'customer',
    entity_id => 'CUST-456',
    entity_name => 'Bob Smith',
    access_level => 'view',
    granted_by => 'CUST-001',
    expires_at => NOW() + INTERVAL '90 days'
);

-- Usage example: Share document with account (all account holders get access)
SELECT share_document(
    document_id => (SELECT document_id FROM documents WHERE ecms_document_id = 'ECMS-2024-001'),
    entity_type => 'account',
    entity_id => 'ACC-789',
    entity_name => 'Checking Account',
    access_level => 'view',
    granted_by => 'CUST-001'
);
```

### Revoke Access

```sql
-- Usage example: Revoke access from customer
SELECT revoke_document_access(
    p_document_id => (SELECT document_id FROM documents WHERE ecms_document_id = 'ECMS-2024-001'),
    p_entity_type => 'customer',
    p_entity_id => 'CUST-456'
);

-- Update sharing scope if only one entity remains
CREATE OR REPLACE FUNCTION update_sharing_scope_after_revoke()
RETURNS TRIGGER AS $$
DECLARE
    v_access_count INTEGER;
BEGIN
    -- Count remaining access entries (excluding public)
    SELECT COUNT(*)
    INTO v_access_count
    FROM document_access
    WHERE document_id = OLD.document_id
      AND entity_type != 'public';

    -- If only 1 entity left, change to private
    IF v_access_count = 1 THEN
        UPDATE documents
        SET sharing_scope = 'private',
            updated_at = NOW()
        WHERE document_id = OLD.document_id;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_sharing_scope
AFTER DELETE ON document_access
FOR EACH ROW
EXECUTE FUNCTION update_sharing_scope_after_revoke();
```

---

## Access Control Functions

### Check Access

```sql
-- Check if entity has access (using function from schema)
SELECT has_document_access(
    (SELECT document_id FROM documents WHERE ecms_document_id = 'ECMS-2024-001'),
    'customer',
    'CUST-123'
);  -- Returns true/false
```

### Get Access Level

```sql
-- Function to get access level for entity
CREATE OR REPLACE FUNCTION get_access_level(
    p_document_id UUID,
    p_entity_type VARCHAR,
    p_entity_id VARCHAR
)
RETURNS VARCHAR AS $$
DECLARE
    v_access_level VARCHAR;
BEGIN
    SELECT access_level
    INTO v_access_level
    FROM document_access
    WHERE document_id = p_document_id
      AND entity_type = p_entity_type
      AND entity_id = p_entity_id
      AND (expires_at IS NULL OR expires_at > NOW());

    RETURN COALESCE(v_access_level, 'none');
END;
$$ LANGUAGE plpgsql;

-- Usage
SELECT get_access_level(
    (SELECT document_id FROM documents WHERE ecms_document_id = 'ECMS-2024-001'),
    'customer',
    'CUST-123'
);  -- Returns 'owner', 'view', 'edit', 'admin', or 'none'
```

### Search Documents with Access Control

```sql
-- Enhanced search function with access control
CREATE OR REPLACE FUNCTION search_documents_with_access(
    p_entity_type VARCHAR,
    p_entity_id VARCHAR,
    p_document_type VARCHAR DEFAULT NULL,
    p_document_category VARCHAR DEFAULT NULL,
    p_status VARCHAR DEFAULT 'active',
    p_include_shared BOOLEAN DEFAULT TRUE,
    p_include_public BOOLEAN DEFAULT TRUE,
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
    sharing_scope VARCHAR,
    access_level VARCHAR,
    uploaded_at TIMESTAMP WITH TIME ZONE
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
        d.sharing_scope,
        da.access_level,
        d.uploaded_at
    FROM document_access da
    JOIN documents d ON da.document_id = d.document_id
    WHERE (
        -- Entity's direct access
        (da.entity_type = p_entity_type AND da.entity_id = p_entity_id)
        -- Include public documents if requested
        OR (p_include_public AND da.entity_type = 'public')
    )
      AND (p_document_type IS NULL OR d.document_type = p_document_type)
      AND (p_document_category IS NULL OR d.document_category = p_document_category)
      AND (p_status IS NULL OR d.status = p_status)
      AND (da.expires_at IS NULL OR da.expires_at > NOW())
      AND (
        p_include_shared
        OR d.sharing_scope = 'private'
        OR da.access_level = 'owner'
      )
    ORDER BY d.uploaded_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Usage example: Get all documents for customer (including shared and public)
SELECT * FROM search_documents_with_access(
    'customer',
    'CUST-001',
    p_document_type => 'LOAN_APPLICATION',
    p_include_public => true
);
```

---

## API Integration Examples

### Python/FastAPI Example (v2)

```python
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
import asyncpg
from typing import Optional, List
from datetime import datetime, date
import json

app = FastAPI()

# Database connection pool
async def get_db_pool():
    return await asyncpg.create_pool(
        host='localhost',
        database='document_hub',
        user='your_user',
        password='your_password',
        min_size=10,
        max_size=50
    )

# Pydantic models
class AccessGrant(BaseModel):
    entity_type: str
    entity_id: str
    entity_name: str
    access_level: str = 'view'

class DocumentCreateRequest(BaseModel):
    ecms_document_id: str
    document_type: str
    document_category: str
    template_code: str
    document_name: str
    file_extension: str
    file_size_bytes: int
    mime_type: str
    sharing_scope: str = 'private'  # NEW in v2
    metadata: Optional[dict] = {}
    tags: Optional[List[str]] = []
    # Access control
    owner: AccessGrant  # NEW in v2

class DocumentShareRequest(BaseModel):
    entity_type: str
    entity_id: str
    entity_name: str
    access_level: str = 'view'
    expires_at: Optional[datetime] = None

class DocumentSearchRequest(BaseModel):
    entity_type: str
    entity_id: str
    document_type: Optional[str] = None
    document_category: Optional[str] = None
    status: str = 'active'
    include_shared: bool = True
    include_public: bool = True
    limit: int = 100
    offset: int = 0

# API Endpoints

@app.post("/documents")
async def create_document(request: DocumentCreateRequest):
    """Create document with initial access grant"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        try:
            result = await conn.fetchrow(
                """
                SELECT * FROM create_document_with_access(
                    $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15
                )
                """,
                request.ecms_document_id,
                request.document_type,
                request.document_category,
                request.template_code,
                request.document_name,
                request.file_extension,
                request.file_size_bytes,
                request.mime_type,
                request.sharing_scope,
                json.dumps(request.metadata),
                request.tags,
                'api-service',
                request.owner.entity_type,
                request.owner.entity_id,
                request.owner.entity_name
            )
            return {
                "document_id": str(result['document_id']),
                "access_id": str(result['access_id']),
                "status": "success"
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

@app.post("/documents/{document_id}/share")
async def share_document(document_id: str, request: DocumentShareRequest):
    """Share document with additional entity"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        try:
            access_id = await conn.fetchval(
                """
                SELECT share_document($1, $2, $3, $4, $5, $6, $7)
                """,
                document_id,
                request.entity_type,
                request.entity_id,
                request.entity_name,
                request.access_level,
                'api-service',
                request.expires_at
            )
            return {
                "access_id": str(access_id),
                "status": "shared"
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

@app.delete("/documents/{document_id}/access/{entity_type}/{entity_id}")
async def revoke_access(document_id: str, entity_type: str, entity_id: str):
    """Revoke document access"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        success = await conn.fetchval(
            """
            SELECT revoke_document_access($1, $2, $3)
            """,
            document_id,
            entity_type,
            entity_id
        )
        if success:
            return {"status": "revoked"}
        raise HTTPException(status_code=404, detail="Access not found")

@app.post("/documents/search")
async def search_documents(request: DocumentSearchRequest):
    """Search documents with access control"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT * FROM search_documents_with_access(
                $1, $2, $3, $4, $5, $6, $7, $8, $9
            )
            """,
            request.entity_type,
            request.entity_id,
            request.document_type,
            request.document_category,
            request.status,
            request.include_shared,
            request.include_public,
            request.limit,
            request.offset
        )
        return [dict(row) for row in rows]

@app.get("/documents/{document_id}")
async def get_document(document_id: str, entity_type: str, entity_id: str):
    """Get document with access check"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        # Check access
        has_access = await conn.fetchval(
            "SELECT has_document_access($1, $2, $3)",
            document_id, entity_type, entity_id
        )

        if not has_access:
            raise HTTPException(status_code=403, detail="Access denied")

        # Get document
        row = await conn.fetchrow(
            """
            SELECT d.*, da.access_level
            FROM documents d
            JOIN document_access da ON d.document_id = da.document_id
            WHERE d.document_id = $1
              AND da.entity_type = $2
              AND da.entity_id = $3
            """,
            document_id, entity_type, entity_id
        )

        if row:
            return dict(row)
        raise HTTPException(status_code=404, detail="Document not found")

@app.get("/documents/{document_id}/access")
async def get_document_access_list(document_id: str):
    """Get all entities with access to document"""
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT
                entity_type,
                entity_id,
                entity_name,
                access_level,
                granted_at,
                granted_by,
                expires_at
            FROM document_access
            WHERE document_id = $1
              AND (expires_at IS NULL OR expires_at > NOW())
            ORDER BY access_level DESC, granted_at ASC
            """,
            document_id
        )
        return [dict(row) for row in rows]
```

---

## Redis Caching for Shared Documents

### Python Redis Integration (v2)

```python
import redis
import json
from typing import Optional, List

redis_client = redis.Redis(
    host='localhost',
    port=6379,
    db=0,
    decode_responses=True
)

class DocumentCacheV2:
    """Redis caching layer for v2 schema with shared documents"""

    @staticmethod
    def cache_document_with_access(document_id: str, document_data: dict, access_list: list, ttl: int = 300):
        """Cache document with its access control list"""
        key = f"document:full:{document_id}"
        data = {
            "document": document_data,
            "access": access_list,
            "cached_at": datetime.now().isoformat()
        }
        redis_client.setex(key, ttl, json.dumps(data))

    @staticmethod
    def get_document_with_access(document_id: str) -> Optional[dict]:
        """Get document with access list from cache"""
        key = f"document:full:{document_id}"
        data = redis_client.get(key)
        return json.loads(data) if data else None

    @staticmethod
    def cache_entity_documents(entity_type: str, entity_id: str, documents: list, ttl: int = 120):
        """Cache list of documents accessible to entity"""
        key = f"entity:documents:{entity_type}:{entity_id}"
        redis_client.setex(key, ttl, json.dumps(documents))

    @staticmethod
    def get_entity_documents(entity_type: str, entity_id: str) -> Optional[list]:
        """Get entity's accessible documents from cache"""
        key = f"entity:documents:{entity_type}:{entity_id}"
        data = redis_client.get(key)
        return json.loads(data) if data else None

    @staticmethod
    def invalidate_document_cache(document_id: str):
        """Invalidate all caches related to a document"""
        # Invalidate full document cache
        redis_client.delete(f"document:full:{document_id}")

        # Find and invalidate all entity caches that might contain this document
        # (This is expensive - better to use pub/sub or cache with short TTL)
        pattern = "entity:documents:*"
        for key in redis_client.scan_iter(match=pattern):
            redis_client.delete(key)

    @staticmethod
    def invalidate_entity_cache(entity_type: str, entity_id: str):
        """Invalidate cache for specific entity"""
        redis_client.delete(f"entity:documents:{entity_type}:{entity_id}")

    @staticmethod
    def cache_access_check(document_id: str, entity_type: str, entity_id: str, has_access: bool, ttl: int = 300):
        """Cache access check result"""
        key = f"access:{document_id}:{entity_type}:{entity_id}"
        redis_client.setex(key, ttl, "1" if has_access else "0")

    @staticmethod
    def get_access_check(document_id: str, entity_type: str, entity_id: str) -> Optional[bool]:
        """Get cached access check result"""
        key = f"access:{document_id}:{entity_type}:{entity_id}"
        result = redis_client.get(key)
        if result is None:
            return None
        return result == "1"


# Usage in API with caching
@app.get("/documents/{document_id}/cached")
async def get_document_cached(document_id: str, entity_type: str, entity_id: str):
    # Check access (with cache)
    cached_access = DocumentCacheV2.get_access_check(document_id, entity_type, entity_id)

    if cached_access is None:
        # Cache miss - check database
        pool = await get_db_pool()
        async with pool.acquire() as conn:
            has_access = await conn.fetchval(
                "SELECT has_document_access($1, $2, $3)",
                document_id, entity_type, entity_id
            )
            # Cache result
            DocumentCacheV2.cache_access_check(document_id, entity_type, entity_id, has_access)
            cached_access = has_access

    if not cached_access:
        raise HTTPException(status_code=403, detail="Access denied")

    # Try full document cache
    cached_doc = DocumentCacheV2.get_document_with_access(document_id)
    if cached_doc:
        return {"source": "cache", "data": cached_doc}

    # Cache miss - query database
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        doc_row = await conn.fetchrow(
            "SELECT * FROM documents WHERE document_id = $1",
            document_id
        )
        access_rows = await conn.fetch(
            "SELECT * FROM document_access WHERE document_id = $1",
            document_id
        )

        if doc_row:
            doc_data = dict(doc_row)
            access_list = [dict(row) for row in access_rows]

            # Cache for future requests
            DocumentCacheV2.cache_document_with_access(document_id, doc_data, access_list)

            return {
                "source": "database",
                "data": {"document": doc_data, "access": access_list}
            }

    raise HTTPException(status_code=404, detail="Document not found")
```

---

## Common Query Patterns

### Get Shared Documents

```sql
-- Find all shared documents (with multiple access entries)
SELECT
    d.document_id,
    d.document_name,
    d.sharing_scope,
    COUNT(da.access_id) AS entity_count,
    array_agg(da.entity_type || ':' || COALESCE(da.entity_id, 'public')) AS shared_with
FROM documents d
JOIN document_access da ON d.document_id = da.document_id
WHERE d.sharing_scope = 'shared'
GROUP BY d.document_id, d.document_name, d.sharing_scope
HAVING COUNT(da.access_id) > 1
ORDER BY entity_count DESC;
```

### Get Public Documents

```sql
-- Get all public documents
SELECT
    d.document_id,
    d.document_name,
    d.document_type,
    d.uploaded_at
FROM documents d
WHERE d.sharing_scope = 'public'
  AND d.status = 'active'
ORDER BY d.uploaded_at DESC;
```

### Get Documents Shared By Customer

```sql
-- Find documents owned by customer that are shared with others
SELECT
    d.document_id,
    d.document_name,
    owner_access.entity_name AS owner_name,
    COUNT(other_access.access_id) AS shared_count,
    array_agg(other_access.entity_type || ':' || other_access.entity_id) AS shared_with
FROM documents d
JOIN document_access owner_access ON d.document_id = owner_access.document_id
    AND owner_access.access_level = 'owner'
    AND owner_access.entity_id = 'CUST-001'
LEFT JOIN document_access other_access ON d.document_id = other_access.document_id
    AND other_access.access_id != owner_access.access_id
WHERE d.sharing_scope = 'shared'
GROUP BY d.document_id, d.document_name, owner_access.entity_name
HAVING COUNT(other_access.access_id) > 0;
```

### Get Expiring Access

```sql
-- Find access grants expiring in next 7 days
SELECT
    d.document_name,
    da.entity_type,
    da.entity_id,
    da.entity_name,
    da.expires_at,
    EXTRACT(DAY FROM (da.expires_at - NOW())) AS days_remaining
FROM document_access da
JOIN documents d ON da.document_id = d.document_id
WHERE da.expires_at IS NOT NULL
  AND da.expires_at > NOW()
  AND da.expires_at < NOW() + INTERVAL '7 days'
ORDER BY da.expires_at ASC;
```

---

## Summary

The v2 implementation provides:
- **Flexible access control** via `document_access` junction table
- **Shared document support** for multiple customers/accounts
- **Public documents** for global access
- **Time-limited access** with expiration
- **Granular permissions** (owner/view/edit/admin)
- **Comprehensive caching** for performance

All code examples are production-ready and can be adapted to your specific stack.
