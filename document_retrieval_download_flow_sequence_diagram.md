# Document Retrieval & Download Flow - Sequence Diagram

This sequence diagram illustrates the document retrieval and secure download URL generation flow in the Document Hub API.

## Mermaid Sequence Diagram - Get Document Metadata

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client as Client Application
    participant API as Document Hub API
    participant Auth as Authentication<br/>Service
    participant DocService as Document<br/>Service
    participant CacheService as Redis<br/>Cache
    participant DB as PostgreSQL<br/>Database

    Note over User,DB: Retrieve Document Metadata by ID

    User->>Client: View Document Details<br/>(click on document)
    Client->>+API: GET /documents/{documentId}<br/>Authorization: Bearer <token>

    API->>+Auth: Validate JWT Token
    Auth-->>-API: Token Valid<br/>(user_id, roles)

    API->>+DocService: Get Document by ID
    DocService->>+CacheService: Check Cache<br/>GET document:{documentId}

    alt Cache Hit
        CacheService-->>DocService: Document Metadata<br/>(from cache)
        Note over DocService: Skip database query
    end

    alt Cache Miss
        CacheService-->>-DocService: Cache Miss

        DocService->>+DB: SELECT d.*, t.*<br/>FROM documents d<br/>LEFT JOIN templates t<br/>ON d.template_id = t.template_id<br/>WHERE d.document_id = ?<br/>AND d.deleted_at IS NULL
        DB-->>-DocService: Document Record + Template Info

        alt Document Not Found
            DocService-->>API: Not Found
            API-->>Client: 404 Not Found<br/>{error: "DOCUMENT_NOT_FOUND"}
            Client-->>User: Document Not Found
        end

        DocService->>+CacheService: Store in Cache<br/>SET document:{documentId}<br/>TTL: 15 minutes
        CacheService-->>-DocService: Cached
    end

    DocService->>DocService: Check Access Permissions<br/>- Is document owner (customer_id)?<br/>- Has role-based access?<br/>- Check access_level

    alt Access Denied
        DocService-->>API: Access Denied
        API-->>Client: 403 Forbidden<br/>{error: "ACCESS_DENIED"}
        Client-->>User: You don't have permission<br/>to view this document
    end

    DocService-->>-API: Document Metadata<br/>{document_id, customer_id,<br/>template, file_info, metadata}

    API-->>-Client: 200 OK<br/>{document_id, ecms_document_id,<br/>customer_id, document_name,<br/>template: {...},<br/>file_extension, file_size_bytes,<br/>status, tags, metadata,<br/>uploaded_at}

    Client-->>User: Display Document Details

    Note over User,DB: User sees document metadata
```

## Mermaid Sequence Diagram - Generate Download URL

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client as Client Application
    participant API as Document Hub API
    participant Auth as Authentication<br/>Service
    participant DocService as Document<br/>Service
    participant DB as PostgreSQL<br/>Database
    participant StorageService as Storage<br/>Service
    participant S3 as AWS S3/<br/>ECMS

    Note over User,S3: Generate Secure Pre-signed Download URL

    User->>Client: Click "Download" Button
    Client->>+API: GET /documents/{documentId}/download<br/>Authorization: Bearer <token>

    API->>+Auth: Validate JWT Token
    Auth-->>-API: Token Valid<br/>(user_id, roles)

    API->>+DocService: Get Document for Download
    DocService->>+DB: SELECT document<br/>WHERE document_id = ?<br/>AND deleted_at IS NULL
    DB-->>-DocService: Document Record<br/>(ecms_document_id, file_path,<br/>customer_id, access_level)

    alt Document Not Found
        DocService-->>API: Not Found
        API-->>Client: 404 Not Found
        Client-->>User: Document Not Found
    end

    DocService->>DocService: Check Access Permissions<br/>- Customer owner check<br/>- Role-based access<br/>- Confidential flag check

    alt Access Denied
        DocService-->>API: Access Denied
        API-->>Client: 403 Forbidden<br/>{error: "ACCESS_DENIED",<br/>message: "Insufficient permissions"}
        Client-->>User: Access Denied
    end

    DocService-->>-API: Document Access Approved<br/>(ecms_document_id, file_path)

    API->>+StorageService: Generate Pre-signed URL<br/>(ecms_document_id, file_path,<br/>user_id, expires_in: 3600)

    StorageService->>+S3: Generate Pre-signed URL<br/>AWS SDK: generatePresignedUrl()<br/>- Bucket: ecms-documents<br/>- Key: file_path<br/>- Expires: 1 hour<br/>- Method: GET

    S3-->>-StorageService: Pre-signed URL<br/>(signed with AWS credentials)

    StorageService->>+DB: INSERT INTO download_audit_log<br/>(document_id, user_id,<br/>download_url_generated_at,<br/>expires_at, ip_address)
    DB-->>-StorageService: Audit Logged

    StorageService-->>-API: Download URL Details<br/>(url, expires_at)

    API-->>-Client: 200 OK<br/>{document_id,<br/>download_url: "https://s3...?AWSAccessKeyId=...&Expires=...&Signature=...",<br/>expires_at: "2024-11-01T13:00:00Z",<br/>content_type: "application/pdf",<br/>file_size_bytes: 1048576}

    Client->>Client: Display Download Link<br/>(valid for 1 hour)

    Note over User,Client: User can now download

    User->>Client: Click Download Link
    Client->>+S3: HTTP GET<br/>(pre-signed URL)<br/>No authentication needed
    S3->>S3: Validate Signature & Expiration
    S3-->>-Client: File Download Stream<br/>(Content-Type: application/pdf)
    Client-->>User: File Downloaded

    Note over User,S3: Download Complete (Audited)
```

## Mermaid Sequence Diagram - List Documents with Filters

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client as Client Application
    participant API as Document Hub API
    participant Auth as Authentication<br/>Service
    participant DocService as Document<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over User,DB: List Documents with Filtering & Pagination

    User->>Client: Search/Filter Documents<br/>(by type, date, customer, tags)
    Client->>+API: GET /documents?<br/>customer_id=C123456<br/>&document_type=LOAN_APPLICATION<br/>&date_from=2024-01-01<br/>&tags=urgent<br/>&page=0&size=20<br/>Authorization: Bearer <token>

    API->>+Auth: Validate JWT Token
    Auth-->>-API: Token Valid<br/>(user_id, roles)

    API->>+DocService: List Documents with Filters

    DocService->>DocService: Build Dynamic Query<br/>- Base query with filters<br/>- Apply pagination<br/>- Apply sorting<br/>- Check user permissions

    DocService->>+DB: SELECT d.*, t.template_code<br/>FROM documents d<br/>LEFT JOIN templates t ON d.template_id = t.template_id<br/>WHERE d.customer_id = ?<br/>AND d.document_type = ?<br/>AND d.document_date >= ?<br/>AND EXISTS (<br/>  SELECT 1 FROM document_tags<br/>  WHERE document_id = d.document_id<br/>  AND tag = 'urgent'<br/>)<br/>AND d.deleted_at IS NULL<br/>ORDER BY d.uploaded_at DESC<br/>LIMIT 20 OFFSET 0

    DB->>DB: Use Indexes:<br/>- idx_documents_customer_id<br/>- idx_documents_type_date<br/>- idx_document_tags_tag

    DB-->>-DocService: Document List<br/>(20 records) + Total Count

    DocService->>DocService: Filter by Access Permissions<br/>- Remove confidential docs<br/>  if user lacks permission<br/>- Apply role-based filtering

    DocService-->>-API: Filtered Document List<br/>(18 accessible documents)

    API-->>-Client: 200 OK<br/>{<br/>  content: [<br/>    {document_id, document_name,<br/>     document_type, customer_id,<br/>     file_extension, status,<br/>     uploaded_at}, ...],<br/>  page: {<br/>    number: 0, size: 20,<br/>    total_elements: 156,<br/>    total_pages: 8<br/>  }<br/>}

    Client-->>User: Display Document List<br/>(showing 1-18 of 156)

    Note over User,DB: Query Performance: <100ms
```

## Flow Descriptions

### Get Document Metadata Flow

1. **Authentication** (Steps 1-3)
   - User requests document details
   - API validates JWT token
   - Extract user identity and roles

2. **Cache Check** (Steps 4-6)
   - Check Redis cache for document metadata
   - If found, return from cache (faster response)
   - If not found, query database

3. **Database Query** (Steps 7-8)
   - Query documents with JOIN to templates table
   - Get complete document metadata
   - If not found → 404 Not Found

4. **Cache Update** (Steps 9-10)
   - Store result in Redis cache
   - Set TTL to 15 minutes
   - Subsequent requests are faster

5. **Permission Check** (Steps 11-12)
   - Verify user owns the document (customer_id match)
   - Check role-based permissions
   - Verify access_level (CUSTOMER_ONLY, INTERNAL, PUBLIC)
   - If denied → 403 Forbidden

6. **Response** (Steps 13-15)
   - Return complete document metadata
   - Include template information
   - Display to user

### Download URL Generation Flow

1. **Authentication & Retrieval** (Steps 1-5)
   - User clicks download button
   - Validate JWT token
   - Retrieve document from database
   - If not found → 404 Not Found

2. **Access Control** (Steps 6-8)
   - Verify user has download permission
   - Check customer_id ownership
   - Verify confidential document access
   - Check role-based permissions
   - If denied → 403 Forbidden

3. **Pre-signed URL Generation** (Steps 9-11)
   - Request pre-signed URL from Storage Service
   - Generate AWS S3 pre-signed URL (1 hour expiration)
   - URL includes signature for security
   - No additional authentication needed for download

4. **Audit Logging** (Steps 12-13)
   - Log download URL generation
   - Record user_id, timestamp, IP address
   - Track document access for compliance

5. **Response** (Steps 14-16)
   - Return download URL with expiration time
   - User can download within 1 hour
   - URL is single-use or time-limited

6. **File Download** (Steps 17-20)
   - User downloads file directly from S3
   - S3 validates signature and expiration
   - Stream file to user
   - No API involvement in actual download

### List Documents Flow

1. **Filter Request** (Steps 1-3)
   - User applies filters (type, date, customer, tags)
   - Client builds query parameters
   - API validates token

2. **Dynamic Query Building** (Steps 4-5)
   - Build SQL query based on filters
   - Apply pagination (page, size)
   - Add sorting (default: uploaded_at DESC)
   - Include user permission filters

3. **Optimized Database Query** (Steps 6-8)
   - Execute query with indexes
   - Use covering indexes for performance
   - Get results and total count
   - Target: <100ms query time

4. **Permission Filtering** (Steps 9-10)
   - Post-query permission check
   - Remove confidential documents if needed
   - Apply role-based filtering

5. **Paginated Response** (Steps 11-13)
   - Return document list with pagination metadata
   - Include total count and page info
   - Display to user

## API Endpoint Details

### Get Document by ID

```
GET /api/v1/documents/{documentId}
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
  "ecms_document_id": "ECMS-2024-11-01-12345",
  "is_shared": false,
  "customer_id": "C123456",
  "customer_name": "John Doe",
  "document_name": "Loan Application - John Doe",
  "document_type": "LOAN_APPLICATION",
  "template": {
    "template_id": "550e8400-e29b-41d4-a716-446655440000",
    "template_code": "LOAN_APPLICATION",
    "template_name": "Loan Application Form",
    "version_number": 3
  },
  "file_extension": "pdf",
  "file_size_bytes": 1048576,
  "status": "active",
  "is_confidential": true,
  "document_date": "2024-11-01",
  "tags": ["urgent", "new-customer"],
  "uploaded_at": "2024-11-01T12:00:00Z"
}
```

### Get Download URL

```
GET /api/v1/documents/{documentId}/download
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
  "download_url": "https://s3.amazonaws.com/ecms-bucket/documents/2024/11/01/ECMS-2024-11-01-12345.pdf?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1730462400&Signature=abc123xyz...",
  "expires_at": "2024-11-01T13:00:00Z",
  "content_type": "application/pdf",
  "file_size_bytes": 1048576
}
```

### List Documents

```
GET /api/v1/documents?customer_id=C123456&document_type=LOAN_APPLICATION&date_from=2024-01-01&tags=urgent&page=0&size=20
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "content": [
    {
      "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
      "document_name": "Loan Application - John Doe",
      "document_type": "LOAN_APPLICATION",
      "customer_id": "C123456",
      "file_extension": "pdf",
      "status": "active",
      "uploaded_at": "2024-11-01T12:00:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 156,
    "total_pages": 8
  }
}
```

## Performance Optimizations

### Caching Strategy
- **Cache Layer**: Redis
- **TTL**: 15 minutes for document metadata
- **Cache Key**: `document:{documentId}`
- **Invalidation**: On document update/delete

### Database Indexes
```sql
CREATE INDEX idx_documents_customer_id ON documents(customer_id);
CREATE INDEX idx_documents_type_date ON documents(document_type, document_date);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_document_tags_tag ON document_tags(tag, document_id);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at DESC);
```

### Query Performance
- **Target**: <50ms for single document retrieval
- **Target**: <100ms for list queries
- **Target**: <500ms for complex filtered searches

### Pre-signed URL Benefits
1. **No API Load**: Direct S3 download, no proxying
2. **Security**: Time-limited, signed URLs
3. **Scalability**: S3 handles all download traffic
4. **Cost**: No egress through API servers

## Security Considerations

### Access Control
- **Customer Ownership**: Verify customer_id matches user
- **Role-Based Access**: Admin/Manager can access all documents
- **Confidential Documents**: Require elevated permissions
- **Access Levels**: CUSTOMER_ONLY, INTERNAL, PUBLIC

### Audit Trail
- Log all document access attempts
- Record download URL generation
- Track successful downloads
- Monitor suspicious patterns

### URL Security
- **Pre-signed URLs**: Temporary, signed with AWS credentials
- **Expiration**: 1 hour default (configurable)
- **One-time Use**: Optional (can implement token tracking)
- **IP Restriction**: Optional (can bind to user IP)

## Error Scenarios

| Scenario | HTTP Status | Error Code | Action |
|----------|-------------|------------|--------|
| Document not found | 404 | DOCUMENT_NOT_FOUND | Check document ID |
| Access denied | 403 | ACCESS_DENIED | Insufficient permissions |
| Document deleted | 410 | DOCUMENT_DELETED | Document no longer available |
| URL expired | 403 | DOWNLOAD_URL_EXPIRED | Generate new URL |
| Invalid token | 401 | UNAUTHORIZED | Re-authenticate |

## Best Practices

1. **Cache Frequently Accessed Documents**
   - Popular documents benefit from caching
   - Reduces database load
   - Faster response times

2. **Use Pre-signed URLs**
   - Don't proxy downloads through API
   - Let S3 handle the traffic
   - Better performance and cost

3. **Implement Download Auditing**
   - Track all document access
   - Compliance and security monitoring
   - Detect unauthorized access patterns

4. **Set Appropriate URL Expiration**
   - 1 hour for general documents
   - Shorter for highly sensitive documents
   - Consider user experience vs security
