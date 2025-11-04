# Shared Document Creation & Assignment Flow - Sequence Diagram

This sequence diagram illustrates the creation of shared documents and their assignment to customers in the Document Hub API.

## Mermaid Sequence Diagram - Create Shared Document

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Portal
    participant API as Document Hub API
    participant Auth as Authentication<br/>Service
    participant SharedDocService as Shared Document<br/>Service
    participant TemplateService as Template<br/>Service
    participant RuleEngine as Rule<br/>Engine
    participant Storage as ECMS/S3<br/>Storage
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Create Shared Document (Company-wide)

    Admin->>Client: Upload Shared Document<br/>(e.g., Privacy Policy)
    Client->>+API: POST /shared-documents<br/>Authorization: Bearer <token><br/>multipart/form-data:<br/>- file: privacy_policy.pdf<br/>- metadata: {<br/>    document_name,<br/>    sharing_scope: "all_customers",<br/>    effective_from, effective_to<br/>  }

    API->>+Auth: Validate JWT & Permissions<br/>(requires: shared_document:create)
    Auth-->>-API: Authorized (Admin Role)

    alt Insufficient Permissions
        API-->>Client: 403 Forbidden
        Client-->>Admin: Access Denied
    end

    API->>API: Validate Request<br/>- file present<br/>- sharing_scope required<br/>- effective_from required

    alt Validation Failed
        API-->>Client: 400 Bad Request<br/>{error: "VALIDATION_ERROR"}
        Client-->>Admin: Fix Validation Errors
    end

    API->>+TemplateService: Get Template<br/>by template_code
    TemplateService->>+DB: SELECT template, rules<br/>WHERE template_code = ?<br/>AND status = 'active'
    DB-->>-TemplateService: Template + Rules
    TemplateService-->>-API: Template Details

    API->>+RuleEngine: Execute Validation Rules<br/>(file, metadata, rules)
    RuleEngine->>RuleEngine: Validate:<br/>- File size<br/>- File extension<br/>- Metadata requirements

    alt Validation Failed
        RuleEngine-->>API: Validation Errors
        API-->>Client: 422 Unprocessable Entity
        Client-->>Admin: Validation Failed
    end

    RuleEngine-->>-API: All Rules Passed

    API->>+Storage: Upload File
    Storage->>Storage: Generate ECMS ID<br/>(ECMS-SHARED-2024-11-01-001)
    Storage-->>-API: File URL + ECMS ID

    API->>+SharedDocService: Create Shared Document
    SharedDocService->>+DB: BEGIN TRANSACTION

    SharedDocService->>DB: INSERT INTO documents<br/>(document_id,<br/>ecms_document_id,<br/>customer_id = '00000000-SHARED',<br/>is_shared = true,<br/>sharing_scope = 'all_customers',<br/>effective_from, effective_to,<br/>document_name, template_id,<br/>file_info, status = 'active',<br/>created_by)

    DB-->>SharedDocService: Document Created

    alt sharing_scope = 'all_customers'
        Note over SharedDocService: No mappings needed<br/>All customers auto-included
    end

    alt sharing_scope = 'specific_customers'
        Note over SharedDocService: Will assign customers<br/>in separate API call
    end

    SharedDocService->>DB: INSERT INTO document_audit_log<br/>(document_id, action,<br/>performed_by, timestamp)
    DB-->>SharedDocService: Audit Logged

    SharedDocService->>DB: COMMIT TRANSACTION
    DB-->>-SharedDocService: Transaction Success

    SharedDocService-->>-API: Shared Document Created

    API-->>-Client: 201 Created<br/>{<br/>  document_id,<br/>  ecms_document_id,<br/>  customer_id: "00000000-SHARED",<br/>  is_shared: true,<br/>  sharing_scope: "all_customers",<br/>  effective_from,<br/>  effective_to: null,<br/>  document_name,<br/>  status: "active",<br/>  uploaded_at<br/>}

    Client-->>Admin: Shared Document Created<br/>(Visible to All Customers)

    Note over Admin,DB: Document Active and Visible to All Customers
```

## Mermaid Sequence Diagram - Assign Shared Document to Specific Customers

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Portal
    participant API as Document Hub API
    participant Auth as Authentication<br/>Service
    participant SharedDocService as Shared Document<br/>Service
    participant NotificationService as Notification<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Assign Shared Document to Specific Customers

    Admin->>Client: Select Customers<br/>(e.g., California residents)
    Client->>+API: POST /shared-documents/{documentId}/assign<br/>Authorization: Bearer <token><br/>{<br/>  customer_ids: [<br/>    "C123456", "C789012",<br/>    "C345678", ... (1000 customers)<br/>  ],<br/>  assigned_by: "admin@example.com"<br/>}

    API->>+Auth: Validate JWT & Permissions<br/>(requires: shared_document:assign)
    Auth-->>-API: Authorized

    API->>+SharedDocService: Get Shared Document
    SharedDocService->>+DB: SELECT FROM documents<br/>WHERE document_id = ?<br/>AND is_shared = true
    DB-->>-SharedDocService: Document Record<br/>(sharing_scope: "specific_customers")

    alt Document Not Found
        SharedDocService-->>API: Not Found
        API-->>Client: 404 Not Found
        Client-->>Admin: Document Not Found
    end

    alt Invalid Sharing Scope
        SharedDocService-->>API: Invalid Scope
        API-->>Client: 400 Bad Request<br/>{error: "INVALID_SHARING_SCOPE",<br/>message: "Document must have<br/>sharing_scope=specific_customers"}
        Client-->>Admin: Cannot Assign
    end

    SharedDocService->>+DB: Validate Customer IDs<br/>SELECT customer_id<br/>FROM customers<br/>WHERE customer_id IN (...)<br/>AND active = true

    DB-->>-SharedDocService: Valid Customers<br/>(998 found, 2 invalid)

    SharedDocService->>+DB: BEGIN TRANSACTION

    SharedDocService->>DB: Check Existing Assignments<br/>SELECT customer_id<br/>FROM shared_document_mappings<br/>WHERE document_id = ?<br/>AND customer_id IN (...)

    DB-->>SharedDocService: Existing Assignments<br/>(50 already assigned)

    SharedDocService->>SharedDocService: Calculate New Assignments<br/>Valid: 998<br/>Already Assigned: 50<br/>New: 948

    loop For Each New Customer (Batch Insert)
        SharedDocService->>DB: INSERT INTO<br/>shared_document_mappings<br/>(mapping_id, document_id,<br/>customer_id, assigned_at,<br/>assigned_by)<br/>VALUES (batch of 100)
        DB-->>SharedDocService: Batch Inserted
    end

    SharedDocService->>DB: INSERT INTO assignment_audit_log<br/>(document_id, customer_count,<br/>assigned_by, timestamp)
    DB-->>SharedDocService: Audit Logged

    SharedDocService->>DB: COMMIT TRANSACTION
    DB-->>-SharedDocService: Transaction Success

    SharedDocService->>+NotificationService: Notify Customers<br/>(async, background job)<br/>customer_ids: [...],<br/>document_id, notification_type
    NotificationService-->>-SharedDocService: Notification Queued

    SharedDocService-->>-API: Assignment Complete

    API-->>-Client: 200 OK<br/>{<br/>  document_id,<br/>  assignments_created: 948,<br/>  assignments_skipped: 50,<br/>  invalid_customers: 2,<br/>  assignments: [<br/>    {mapping_id, customer_id,<br/>     assigned_at}, ...<br/>  ] (first 100)<br/>}

    Client-->>Admin: Assignment Complete<br/>948 customers assigned

    Note over Admin,DB: Customers can now view the document

    Note over NotificationService: Background: Send Email/SMS<br/>to assigned customers
```

## Mermaid Sequence Diagram - Unassign Shared Document

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Portal
    participant API as Document Hub API
    participant Auth as Authentication<br/>Service
    participant SharedDocService as Shared Document<br/>Service
    participant CacheService as Redis<br/>Cache
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Remove Shared Document Assignment

    Admin->>Client: Select Customers to Unassign<br/>(e.g., moved out of California)
    Client->>+API: POST /shared-documents/{documentId}/unassign<br/>Authorization: Bearer <token><br/>{<br/>  customer_ids: [<br/>    "C123456", "C789012"<br/>  ]<br/>}

    API->>+Auth: Validate JWT & Permissions<br/>(requires: shared_document:unassign)
    Auth-->>-API: Authorized

    API->>+SharedDocService: Unassign Customers

    SharedDocService->>+DB: SELECT FROM documents<br/>WHERE document_id = ?<br/>AND is_shared = true
    DB-->>-SharedDocService: Document Record

    alt Invalid Sharing Scope
        SharedDocService-->>API: Cannot Unassign
        API-->>Client: 400 Bad Request<br/>{error: "INVALID_OPERATION",<br/>message: "Cannot unassign from<br/>all_customers scope"}
        Client-->>Admin: Cannot Unassign
    end

    SharedDocService->>+DB: BEGIN TRANSACTION

    SharedDocService->>DB: DELETE FROM<br/>shared_document_mappings<br/>WHERE document_id = ?<br/>AND customer_id IN (?, ?)

    DB-->>SharedDocService: Deleted Rows: 2

    SharedDocService->>DB: INSERT INTO assignment_audit_log<br/>(document_id, action: 'unassign',<br/>customer_count: 2,<br/>performed_by, timestamp)
    DB-->>SharedDocService: Audit Logged

    SharedDocService->>DB: COMMIT TRANSACTION
    DB-->>-SharedDocService: Transaction Success

    SharedDocService->>+CacheService: Invalidate Customer Document Caches<br/>DEL customer:C123456:documents<br/>DEL customer:C789012:documents
    CacheService-->>-SharedDocService: Caches Invalidated

    SharedDocService-->>-API: Unassignment Complete

    API-->>-Client: 200 OK<br/>{<br/>  document_id,<br/>  assignments_removed: 2<br/>}

    Client-->>Admin: Unassignment Complete<br/>2 customers removed

    Note over Admin,DB: Customers can no longer view the document
```

## Mermaid Sequence Diagram - Get Assigned Customers

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Portal
    participant API as Document Hub API
    participant SharedDocService as Shared Document<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: View Customers Assigned to Shared Document

    Admin->>Client: View Assigned Customers<br/>(for a shared document)
    Client->>+API: GET /shared-documents/{documentId}/customers?<br/>page=0&size=20<br/>Authorization: Bearer <token>

    API->>+SharedDocService: Get Assigned Customers

    SharedDocService->>+DB: SELECT d.sharing_scope<br/>FROM documents d<br/>WHERE d.document_id = ?

    DB-->>-SharedDocService: Document<br/>(sharing_scope: "specific_customers")

    alt sharing_scope = 'all_customers'
        SharedDocService-->>API: All Customers
        API-->>Client: 200 OK<br/>{<br/>  sharing_scope: "all_customers",<br/>  message: "Available to all customers",<br/>  total_customers: "ALL"<br/>}
    end

    SharedDocService->>+DB: SELECT COUNT(*)<br/>FROM shared_document_mappings<br/>WHERE document_id = ?

    DB-->>-SharedDocService: Total Count: 1245

    SharedDocService->>+DB: SELECT<br/>  sdm.customer_id,<br/>  c.customer_name,<br/>  sdm.assigned_at,<br/>  sdm.assigned_by<br/>FROM shared_document_mappings sdm<br/>JOIN customers c<br/>  ON sdm.customer_id = c.customer_id<br/>WHERE sdm.document_id = ?<br/>ORDER BY sdm.assigned_at DESC<br/>LIMIT 20 OFFSET 0

    DB->>DB: Use Index:<br/>idx_shared_mappings_document_id

    DB-->>-SharedDocService: Customer List<br/>(20 records)

    SharedDocService-->>-API: Assigned Customers

    API-->>-Client: 200 OK<br/>{<br/>  document_id,<br/>  document_name,<br/>  sharing_scope: "specific_customers",<br/>  total_customers: 1245,<br/>  customers: [<br/>    {customer_id, customer_name,<br/>     assigned_at, assigned_by}, ...<br/>  ],<br/>  page: {number: 0, size: 20,<br/>         total_elements: 1245,<br/>         total_pages: 63}<br/>}

    Client-->>Admin: Display Customer List<br/>(showing 1-20 of 1245)

    Note over Admin,DB: Admin can see who has access
```

## Flow Descriptions

### Create Shared Document Flow

1. **Authentication & Authorization** (Steps 1-4)
   - Admin uploads shared document
   - Validate JWT token and permissions
   - Require `shared_document:create` permission
   - If unauthorized → 403 Forbidden

2. **Request Validation** (Steps 5-6)
   - Validate file present
   - Validate sharing_scope (required)
   - Validate effective_from (required)
   - If invalid → 400 Bad Request

3. **Template & Rule Validation** (Steps 7-11)
   - Retrieve template by template_code
   - Execute validation rules
   - Check file size, extension, metadata
   - If validation fails → 422 Unprocessable Entity

4. **File Storage** (Steps 12-13)
   - Upload file to S3/ECMS
   - Generate shared document ID (ECMS-SHARED-*)
   - Store file in shared documents path

5. **Database Creation** (Steps 14-20)
   - Begin transaction
   - Create document record with:
     - `customer_id = '00000000-SHARED'` (special ID for shared docs)
     - `is_shared = true`
     - `sharing_scope` (all_customers, specific_customers, etc.)
     - `effective_from` and `effective_to` dates
   - Log audit trail
   - Commit transaction

6. **Response** (Steps 21-22)
   - Return created shared document
   - Document immediately available based on scope

### Assign Shared Document Flow

1. **Authorization** (Steps 1-3)
   - Admin selects customers to assign
   - Validate permissions
   - Require `shared_document:assign` permission

2. **Document Validation** (Steps 4-8)
   - Retrieve shared document
   - Verify `is_shared = true`
   - Verify `sharing_scope = 'specific_customers'`
   - If wrong scope → 400 Bad Request

3. **Customer Validation** (Steps 9-10)
   - Validate all customer IDs exist
   - Check customers are active
   - Report invalid customer IDs

4. **Assignment Logic** (Steps 11-17)
   - Check for existing assignments (avoid duplicates)
   - Calculate new assignments needed
   - Batch insert mappings (100 at a time)
   - Log audit trail
   - Commit transaction

5. **Notifications** (Steps 18-19)
   - Queue background job to notify customers
   - Send email/SMS notifications
   - Non-blocking, async operation

6. **Response** (Steps 20-22)
   - Return assignment results
   - Show created, skipped, invalid counts
   - Return first 100 assignments for confirmation

### Unassign Shared Document Flow

1. **Authorization** (Steps 1-3)
   - Admin selects customers to remove
   - Validate permissions

2. **Document Validation** (Steps 4-7)
   - Verify document exists and is shared
   - Verify can unassign (not all_customers scope)
   - If invalid → 400 Bad Request

3. **Unassignment** (Steps 8-12)
   - Delete mapping records
   - Log audit trail
   - Commit transaction

4. **Cache Invalidation** (Steps 13-14)
   - Invalidate affected customer document caches
   - Ensure customers don't see stale data

5. **Response** (Steps 15-17)
   - Return removal count
   - Customers immediately lose access

### Get Assigned Customers Flow

1. **Request** (Steps 1-3)
   - Admin views assigned customers
   - Query specific shared document

2. **Scope Check** (Steps 4-6)
   - Check sharing_scope
   - If 'all_customers', return special response
   - If 'specific_customers', proceed with query

3. **Count & List** (Steps 7-11)
   - Get total customer count
   - Query paginated customer list
   - Join with customers table for names
   - Use index for performance

4. **Response** (Steps 12-14)
   - Return paginated customer list
   - Include assignment metadata
   - Show total and pages

## API Endpoint Details

### Create Shared Document

```
POST /api/v1/shared-documents
Authorization: Bearer <token>
Content-Type: multipart/form-data

Form Data:
- file: privacy_policy.pdf
- metadata: {
    "document_name": "Privacy Policy 2024",
    "document_type": "PRIVACY_POLICY",
    "document_category": "REGULATORY",
    "template_code": "PRIVACY_POLICY_TEMPLATE",
    "sharing_scope": "all_customers",
    "effective_from": "2024-01-01T00:00:00Z",
    "effective_to": null,
    "tags": ["regulatory", "privacy"]
  }
```

**Success Response (201 Created):**
```json
{
  "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
  "ecms_document_id": "ECMS-SHARED-2024-01-01-001",
  "customer_id": "00000000-SHARED",
  "is_shared": true,
  "sharing_scope": "all_customers",
  "effective_from": "2024-01-01T00:00:00Z",
  "effective_to": null,
  "document_name": "Privacy Policy 2024",
  "status": "active",
  "uploaded_at": "2024-01-01T10:00:00Z"
}
```

### Assign Shared Document

```
POST /api/v1/shared-documents/{documentId}/assign
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "customer_ids": ["C123456", "C789012", "C345678"],
  "assigned_by": "admin@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
  "assignments_created": 948,
  "assignments_skipped": 50,
  "invalid_customers": 2,
  "assignments": [
    {
      "mapping_id": "dd0e8400-e29b-41d4-a716-446655440040",
      "customer_id": "C123456",
      "assigned_at": "2024-11-01T14:00:00Z"
    }
  ]
}
```

### Unassign Shared Document

```
POST /api/v1/shared-documents/{documentId}/unassign
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "customer_ids": ["C123456", "C789012"]
}
```

**Success Response (200 OK):**
```json
{
  "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
  "assignments_removed": 2
}
```

### Get Assigned Customers

```
GET /api/v1/shared-documents/{documentId}/customers?page=0&size=20
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
  "document_name": "Regulatory Notice - California",
  "sharing_scope": "specific_customers",
  "total_customers": 1245,
  "customers": [
    {
      "customer_id": "C123456",
      "customer_name": "John Doe",
      "assigned_at": "2024-11-01T14:00:00Z",
      "assigned_by": "admin@example.com"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 1245,
    "total_pages": 63
  }
}
```

## Sharing Scope Strategies

### 1. All Customers
- **Best For**: Company-wide policies, terms of service
- **Assignment**: Automatic, no mappings needed
- **Performance**: Excellent (no JOIN needed)
- **Management**: Simple, no customer list to maintain

### 2. Specific Customers
- **Best For**: Targeted communications, regional notices
- **Assignment**: Manual via API
- **Performance**: Good (indexed JOIN)
- **Management**: Flexible, precise control

### 3. Account Type
- **Best For**: Account-specific terms, tier benefits
- **Assignment**: Automatic based on customer account_type
- **Performance**: Good (attribute match)
- **Management**: Dynamic, updates automatically

### 4. Customer Segment
- **Best For**: VIP communications, loyalty programs
- **Assignment**: Automatic based on customer segment
- **Performance**: Good (attribute match)
- **Management**: Dynamic, segment-driven

## Performance Considerations

### Bulk Assignment Performance
- **Batch Insert**: 100 records at a time
- **Transaction**: Single transaction for consistency
- **Async Notifications**: Don't block response
- **Target**: <2 seconds for 1000 assignments

### Database Indexes
```sql
-- Shared document mappings
CREATE INDEX idx_shared_mappings_document_id
ON shared_document_mappings(document_id, customer_id);

CREATE INDEX idx_shared_mappings_customer_id
ON shared_document_mappings(customer_id, document_id);

-- Shared documents
CREATE INDEX idx_documents_shared_scope
ON documents(is_shared, sharing_scope, status)
WHERE is_shared = true;

-- Effective dates
CREATE INDEX idx_documents_effective_dates
ON documents(effective_from, effective_to, status)
WHERE is_shared = true;
```

## Error Scenarios

| Scenario | HTTP Status | Error Code | Action |
|----------|-------------|------------|--------|
| Unauthorized | 401 | UNAUTHORIZED | Login required |
| Insufficient permissions | 403 | INSUFFICIENT_PERMISSIONS | Need admin role |
| Invalid sharing scope | 400 | INVALID_SHARING_SCOPE | Use specific_customers |
| Cannot unassign all_customers | 400 | INVALID_OPERATION | Scope doesn't support unassign |
| Document not found | 404 | DOCUMENT_NOT_FOUND | Check document ID |
| Invalid customers | 400 | INVALID_CUSTOMERS | Some customer IDs invalid |

## Best Practices

1. **Use Appropriate Sharing Scope**
   - `all_customers` for universal documents
   - `specific_customers` for targeted communications
   - `account_type` for account-specific documents
   - `customer_segment` for segment-based documents

2. **Set Effective Dates**
   - Always set `effective_from`
   - Use `effective_to` for time-limited documents
   - Supports compliance and auditing

3. **Batch Assignments**
   - Send customer IDs in batches of 1000
   - Monitor response times
   - Use async notifications

4. **Audit Everything**
   - Log all assignments/unassignments
   - Track who performed action
   - Record timestamps

5. **Cache Invalidation**
   - Invalidate customer caches on assignment changes
   - Ensure customers see updates immediately
   - Use targeted cache invalidation
