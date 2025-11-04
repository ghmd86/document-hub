# Document Hub API - Sequence Diagrams Index

This document provides a comprehensive overview of all API interaction flow sequence diagrams for the Document Hub system.

## Overview

The Document Hub is a high-performance metadata indexing and retrieval layer for Enterprise Content Management Systems (ECMS). These sequence diagrams illustrate the complete API interaction flows across all major endpoints.

**Technology Stack:**
- Backend: Spring WebFlux (Reactive)
- Database: PostgreSQL
- Cache: Redis
- Storage: AWS S3 / ECMS
- Authentication: JWT Bearer tokens

---

## Sequence Diagrams

### 1. [Authentication Flow](./authentication_flow_sequence_diagram.md)

**Purpose**: User authentication and token management

**Flows Covered:**
- User login with credentials
- JWT access token generation (1 hour expiration)
- JWT refresh token generation (7 days expiration)
- Token refresh mechanism
- Token revocation

**Key Components:**
- Authentication Service
- JWT Token Service
- PostgreSQL (user and token storage)

**Performance:**
- Login: <200ms
- Token refresh: <100ms

**Security Features:**
- BCrypt/Argon2 password hashing
- Token revocation support
- Rate limiting (5 login attempts per minute)
- Refresh token rotation

[View Authentication Flow Diagram →](./authentication_flow_sequence_diagram.md)

---

### 2. [Document Upload Flow](./document_upload_sequence_diagram.md)

**Purpose**: Upload documents with metadata and validation

**Flows Covered:**
- Document upload with multipart form data
- Template retrieval and rule validation
- Business rule execution (file size, type, metadata validation)
- File storage in S3/ECMS
- Metadata storage in PostgreSQL
- Error handling for validation failures

**Key Components:**
- Document Hub API
- Template Service
- Rule Engine
- ECMS/S3 Storage
- PostgreSQL Database

**Performance:**
- Target upload time: <2 seconds
- Rule execution: <100ms
- Database write: <50ms

**Validation Rules:**
1. File size validation
2. File extension validation
3. Metadata format validation
4. Business logic rules
5. Access level validation

[View Document Upload Flow Diagram →](./document_upload_sequence_diagram.md)

---

### 3. [Document Retrieval & Download Flow](./document_retrieval_download_flow_sequence_diagram.md)

**Purpose**: Retrieve document metadata and generate secure download URLs

**Flows Covered:**
- Get document metadata by ID
- Redis cache integration (15-minute TTL)
- Permission and access control checks
- Pre-signed S3 URL generation (1-hour expiration)
- Direct S3 download (no API proxying)
- List documents with filtering and pagination
- Audit logging for downloads

**Key Components:**
- Document Service
- Redis Cache
- Storage Service
- AWS S3
- PostgreSQL Database

**Performance:**
- Cached metadata retrieval: <10ms
- Uncached metadata retrieval: <50ms
- List documents query: <100ms
- Download URL generation: <200ms

**Caching Strategy:**
- Document metadata: 15-minute TTL
- Frequently accessed documents benefit most
- Cache invalidation on update/delete

[View Document Retrieval & Download Flow Diagram →](./document_retrieval_download_flow_sequence_diagram.md)

---

### 4. [Template Creation & Publishing Flow](./template_creation_publishing_flow_sequence_diagram.md)

**Purpose**: Template lifecycle management with versioning

**Flows Covered:**
- Create new template with business rules
- Template validation and duplicate checking
- Template publishing (draft → active)
- Version management and deprecation
- Update active templates (creates new draft version)
- Zero-downtime template updates
- Template version history

**Key Components:**
- Template Service
- Rule Service
- Version Service
- PostgreSQL Database

**Template States:**
- **Draft**: Being edited, not used for documents
- **Active**: Currently used for document uploads
- **Deprecated**: Previous version, still referenced by documents

**Versioning Strategy:**
- Only one active version per template_code
- Updating active template creates new draft version
- Original version remains active until new version published
- Documents reference specific template_id (version)

[View Template Creation & Publishing Flow Diagram →](./template_creation_publishing_flow_sequence_diagram.md)

---

### 5. [Customer Document Retrieval Flow](./customer_document_retrieval_flow_sequence_diagram.md)

**Purpose**: Retrieve all documents for a customer (customer-specific + shared)

**Flows Covered:**
- Get all documents for a specific customer
- Parallel retrieval of customer-specific and shared documents
- Shared document scopes:
  - `all_customers`: Company-wide documents
  - `specific_customers`: Targeted assignments
  - `account_type`: Based on customer's account type
  - `customer_segment`: Based on customer segment
- Timeline-based retrieval (`as_of_date` parameter)
- Effective date range filtering
- Account-specific document retrieval
- Pagination and sorting

**Key Components:**
- Document Service
- Shared Document Service
- PostgreSQL Database

**Performance:**
- Customer documents query: <50ms
- Shared documents query: <100ms (3 parallel queries)
- Total response time: <200ms

**Timeline Queries:**
- Historical: "What documents did customer see on date X?"
- Current: "What documents are active now?"
- Future: "What documents will be active in the future?"

[View Customer Document Retrieval Flow Diagram →](./customer_document_retrieval_flow_sequence_diagram.md)

---

### 6. [Shared Document Creation & Assignment Flow](./shared_document_creation_assignment_flow_sequence_diagram.md)

**Purpose**: Create and manage shared documents across multiple customers

**Flows Covered:**
- Create shared document (company-wide or targeted)
- Set effective date ranges (effective_from, effective_to)
- Assign shared document to specific customers
- Bulk assignment (1000+ customers)
- Unassign customers from shared document
- View assigned customer list
- Assignment audit logging
- Cache invalidation on assignment changes

**Key Components:**
- Shared Document Service
- Notification Service (async)
- Redis Cache
- PostgreSQL Database

**Sharing Scopes:**
1. **all_customers**: Automatic, no mappings needed
2. **specific_customers**: Manual assignment via API
3. **account_type**: Dynamic based on customer account type
4. **customer_segment**: Dynamic based on customer segment

**Performance:**
- Create shared document: <2 seconds
- Bulk assignment (1000 customers): <2 seconds
- Batch insert: 100 records at a time
- Async notifications: Non-blocking

[View Shared Document Creation & Assignment Flow Diagram →](./shared_document_creation_assignment_flow_sequence_diagram.md)

---

### 7. [Analytics & Reporting Flow](./analytics_reporting_flow_sequence_diagram.md)

**Purpose**: Analytics, statistics, and real-time monitoring

**Flows Covered:**
- Document statistics (counts, types, categories, storage)
- Template usage analytics
- Real-time dashboard with WebSocket
- Parallel analytical queries
- Redis caching (15-30 minute TTL)
- Recent activity metrics
- Storage usage and forecasting
- Unused template detection
- Version distribution analysis

**Key Components:**
- Analytics Service
- Redis Cache
- WebSocket Server
- Event Bus (Kafka/RabbitMQ)
- PostgreSQL Database
- Materialized Views (for performance)

**Performance:**
- Document statistics (cached): <10ms
- Document statistics (uncached): <500ms
- Template usage (cached): <5ms
- Template usage (uncached): <300ms
- Real-time update latency: <50ms

**Visualizations:**
- Pie charts (document type distribution)
- Line charts (upload activity timeline)
- Gauges (storage usage)
- Heatmaps (template usage)
- Real-time counters (live updates)

[View Analytics & Reporting Flow Diagram →](./analytics_reporting_flow_sequence_diagram.md)

---

## API Architecture Overview

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Applications                     │
│  (Web Portal, Mobile Apps, Admin Dashboard, External APIs)  │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS (JWT Bearer Token)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                 Document Hub API (Spring WebFlux)            │
│  ┌──────────────┬──────────────┬──────────────┬──────────┐  │
│  │ Auth Service │ Doc Service  │Template Svc  │Analytics │  │
│  │              │              │              │Service   │  │
│  └──────────────┴──────────────┴──────────────┴──────────┘  │
└────────┬────────────────────────┬──────────────────┬─────────┘
         │                        │                  │
         ▼                        ▼                  ▼
┌─────────────────┐    ┌─────────────────┐   ┌──────────────┐
│   PostgreSQL    │    │  Redis Cache    │   │   AWS S3/    │
│   Database      │    │  (Metadata)     │   │    ECMS      │
│  (Metadata)     │    │  15-30 min TTL  │   │  (Files)     │
└─────────────────┘    └─────────────────┘   └──────────────┘
```

### Performance Targets

| Operation | Target Response Time | Notes |
|-----------|---------------------|-------|
| Authentication (login) | <200ms | First login |
| Token refresh | <100ms | Cached user data |
| Document upload | <2s | Includes validation & storage |
| Document metadata retrieval | <50ms | Database query |
| Document metadata (cached) | <10ms | Redis cache hit |
| Download URL generation | <200ms | S3 pre-signed URL |
| List documents | <100ms | With pagination |
| Customer documents query | <200ms | Includes shared docs |
| Template creation | <500ms | With rules |
| Shared doc assignment (1000) | <2s | Batch insert |
| Analytics (cached) | <10ms | Redis cache |
| Analytics (uncached) | <500ms | Parallel queries |

### Database Optimization

**Key Indexes:**
```sql
-- Document retrieval
CREATE INDEX idx_documents_customer_id_type
ON documents(customer_id, document_type, uploaded_at DESC);

-- Shared document assignments
CREATE INDEX idx_shared_mappings_customer
ON shared_document_mappings(customer_id, document_id);

-- Template lookups
CREATE INDEX idx_templates_code_status
ON templates(template_code, status);

-- Analytics
CREATE INDEX idx_documents_uploaded_at
ON documents(uploaded_at DESC)
WHERE deleted_at IS NULL;
```

**Materialized Views:**
```sql
-- Fast analytics
CREATE MATERIALIZED VIEW document_statistics AS
SELECT
  DATE_TRUNC('day', uploaded_at) as date,
  document_type,
  COUNT(*) as count,
  SUM(file_size_bytes) as total_size
FROM documents
WHERE deleted_at IS NULL
GROUP BY date, document_type;
```

---

## Common Patterns Across All Flows

### 1. Authentication & Authorization
Every endpoint (except login/refresh) follows this pattern:
1. Validate JWT Bearer token
2. Extract user identity and roles
3. Check permissions for the requested operation
4. Proceed or return 401/403

### 2. Error Handling
Consistent error responses across all endpoints:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable message",
  "details": {},
  "timestamp": "2024-11-01T12:00:00Z",
  "path": "/api/v1/endpoint"
}
```

### 3. Pagination
List endpoints use consistent pagination:
```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 156,
    "total_pages": 8
  }
}
```

### 4. Caching Strategy
- **Redis Cache**: 5-30 minute TTL depending on data volatility
- **Cache Keys**: Structured with parameters (e.g., `document:{id}`)
- **Invalidation**: On create/update/delete operations
- **Cache-Control Headers**: Inform clients of caching behavior

### 5. Audit Logging
All state-changing operations logged:
- Who performed the action
- What action was performed
- When it occurred
- IP address and user agent
- Before/after state (where applicable)

### 6. Transaction Management
Database transactions for multi-step operations:
- BEGIN TRANSACTION
- Multiple inserts/updates
- COMMIT or ROLLBACK on error
- Ensures data consistency

---

## HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation error, malformed request |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | User doesn't have permission |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (duplicate) |
| 422 | Unprocessable Entity | Business validation failed |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

---

## Security Considerations

### Authentication
- JWT tokens with HS256 or RS256 signing
- Access tokens: 1-hour expiration
- Refresh tokens: 7-day expiration
- Refresh token revocation support

### Authorization
- Role-based access control (RBAC)
- Permission checks on every endpoint
- Customer data isolation (users can only access own data)
- Admin override for support operations

### Data Protection
- Encryption in transit (HTTPS/TLS 1.3)
- Encryption at rest (database and S3)
- Pre-signed URLs for secure file access
- No sensitive data in logs

### Audit & Compliance
- Complete audit trail for all operations
- Document access logging
- Retention policy enforcement
- GDPR compliance support

---

## Rate Limiting

| User Type | Requests/Minute | Requests/Hour |
|-----------|-----------------|---------------|
| Admin | 1000 | 50,000 |
| Standard User | 500 | 10,000 |
| Public API | 100 | 5,000 |

**Rate Limit Headers:**
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1730462400
```

---

## Testing the API

### Using cURL

**Login:**
```bash
curl -X POST https://api.documenthub.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@example.com", "password": "password"}'
```

**Upload Document:**
```bash
curl -X POST https://api.documenthub.example.com/api/v1/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@document.pdf" \
  -F 'metadata={"customer_id":"C123456","document_type":"LOAN_APPLICATION","template_code":"LOAN_APPLICATION"}'
```

**Get Customer Documents:**
```bash
curl -X GET "https://api.documenthub.example.com/api/v1/customers/C123456/documents?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

### Using Postman

Import the OpenAPI specification ([openapi.yaml](./openapi.yaml)) into Postman for interactive testing.

---

## Related Documentation

- [API Specifications](./api_specifications.md) - Complete REST API documentation
- [OpenAPI Specification](./openapi.yaml) - OpenAPI 3.0 YAML
- [Database Schema](./database_visualization.md) - Database structure and relationships

---

## Deployment & Monitoring

### Health Checks
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/info
```

### Metrics to Monitor
- Request latency (p50, p95, p99)
- Error rate by endpoint
- Cache hit ratio
- Database query time
- Storage usage
- Active WebSocket connections
- Queue depth (notifications)

### Alerting Thresholds
- Request latency p99 > 1 second
- Error rate > 1%
- Cache hit ratio < 80%
- Database connection pool exhausted
- Storage > 80% capacity

---

## Future Enhancements

1. **GraphQL API**: For flexible querying
2. **Webhook Support**: Event notifications to external systems
3. **Batch Operations**: Bulk document upload/delete
4. **OCR Integration**: Extract text from documents
5. **Document Versioning**: Track document changes
6. **Advanced Search**: Full-text search with Elasticsearch
7. **AI Classification**: Auto-categorize documents
8. **Blockchain Audit**: Immutable audit trail

---

## Support & Contact

- **Documentation**: https://docs.documenthub.example.com
- **API Status**: https://status.documenthub.example.com
- **Support Email**: support@documenthub.example.com
- **GitHub Issues**: https://github.com/company/document-hub/issues

---

## License

Copyright © 2024 Document Hub. All rights reserved.

Licensed under the Apache License, Version 2.0.
