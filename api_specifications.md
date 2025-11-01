# Document Hub - API Specifications

## Overview

This document provides complete API specifications for the Document Hub system. All APIs follow RESTful principles and return JSON responses.

**Base URL**: `https://api.documenthub.example.com/api/v1`

**Authentication**: Bearer token (JWT) in Authorization header

**Content-Type**: `application/json`

---

## Table of Contents

1. [Authentication](#authentication)
2. [Template Management APIs](#template-management-apis)
3. [Template Rules APIs](#template-rules-apis)
4. [Document Management APIs](#document-management-apis)
5. [Shared Document APIs](#shared-document-apis)
6. [Customer Document Retrieval APIs](#customer-document-retrieval-apis)
7. [Analytics & Reporting APIs](#analytics--reporting-apis)
8. [Common Responses](#common-responses)
9. [Error Codes](#error-codes)
10. [Rate Limiting](#rate-limiting)

---

## Authentication

All API requests require authentication via JWT Bearer token.

### Request Header

```http
Authorization: Bearer <JWT_TOKEN>
```

### Authentication Endpoints

#### POST /auth/login

Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "username": "admin@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

#### POST /auth/refresh

Refresh access token using refresh token.

**Request Body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

---

## Template Management APIs

### 1. Create Template

Create a new document template.

**Endpoint:** `POST /templates`

**Request Body:**
```json
{
  "template_code": "LOAN_APPLICATION",
  "template_name": "Loan Application Form",
  "description": "Standard loan application template for personal loans",
  "document_type": "APPLICATION",
  "document_category": "LENDING",
  "is_shared_document": false,
  "sharing_scope": null,
  "retention_period_days": 2555,
  "requires_signature": true,
  "requires_approval": true,
  "configuration": {
    "max_file_size_mb": 10,
    "allowed_extensions": ["pdf", "docx"],
    "auto_archive_days": 90
  },
  "rules": [
    {
      "rule_name": "Validate File Size",
      "rule_type": "validation",
      "rule_expression": "file_size_bytes <= 10485760",
      "rule_scope": "upload",
      "execution_order": 1,
      "severity": "error",
      "error_message": "File size must not exceed 10 MB"
    },
    {
      "rule_name": "Validate File Extension",
      "rule_type": "validation",
      "rule_expression": "file_extension IN ('pdf', 'docx')",
      "rule_scope": "upload",
      "execution_order": 2,
      "severity": "error",
      "error_message": "Only PDF and DOCX files are allowed"
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "template_code": "LOAN_APPLICATION",
  "version_number": 1,
  "template_name": "Loan Application Form",
  "description": "Standard loan application template for personal loans",
  "document_type": "APPLICATION",
  "document_category": "LENDING",
  "status": "draft",
  "is_shared_document": false,
  "sharing_scope": null,
  "retention_period_days": 2555,
  "requires_signature": true,
  "requires_approval": true,
  "configuration": {
    "max_file_size_mb": 10,
    "allowed_extensions": ["pdf", "docx"],
    "auto_archive_days": 90
  },
  "rules_count": 2,
  "created_at": "2024-11-01T10:30:00Z",
  "created_by": "admin@example.com",
  "updated_at": "2024-11-01T10:30:00Z",
  "updated_by": null
}
```

**Validation Errors (400 Bad Request):**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "template_code",
      "message": "Template code is required and must be 3-100 characters"
    },
    {
      "field": "rules[0].execution_order",
      "message": "Execution order must be a positive integer"
    }
  ]
}
```

---

### 2. List Templates

Retrieve all templates with optional filtering.

**Endpoint:** `GET /templates`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Filter by status: `active`, `deprecated`, `draft` |
| `document_type` | string | No | Filter by document type |
| `document_category` | string | No | Filter by document category |
| `is_shared_document` | boolean | No | Filter shared templates |
| `search` | string | No | Search in template_name and template_code |
| `page` | integer | No | Page number (default: 0) |
| `size` | integer | No | Page size (default: 20, max: 100) |
| `sort` | string | No | Sort field (default: `created_at,desc`) |

**Example Request:**
```http
GET /templates?status=active&document_type=APPLICATION&page=0&size=20
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "template_id": "550e8400-e29b-41d4-a716-446655440000",
      "template_code": "LOAN_APPLICATION",
      "version_number": 3,
      "template_name": "Loan Application Form",
      "document_type": "APPLICATION",
      "document_category": "LENDING",
      "status": "active",
      "is_shared_document": false,
      "rules_count": 5,
      "created_at": "2024-01-15T09:00:00Z",
      "updated_at": "2024-06-20T14:30:00Z"
    },
    {
      "template_id": "660e8400-e29b-41d4-a716-446655440001",
      "template_code": "ACCOUNT_STATEMENT",
      "version_number": 2,
      "template_name": "Monthly Account Statement",
      "document_type": "STATEMENT",
      "document_category": "BANKING",
      "status": "active",
      "is_shared_document": false,
      "rules_count": 3,
      "created_at": "2024-02-10T11:00:00Z",
      "updated_at": "2024-08-15T16:45:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 45,
    "total_pages": 3
  }
}
```

---

### 3. Get Template by ID

Retrieve a specific template with full details including rules.

**Endpoint:** `GET /templates/{templateId}`

**Path Parameters:**
- `templateId` (UUID, required): Template identifier

**Response (200 OK):**
```json
{
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "template_code": "LOAN_APPLICATION",
  "version_number": 3,
  "template_name": "Loan Application Form",
  "description": "Standard loan application template for personal loans",
  "document_type": "APPLICATION",
  "document_category": "LENDING",
  "status": "active",
  "is_shared_document": false,
  "sharing_scope": null,
  "retention_period_days": 2555,
  "requires_signature": true,
  "requires_approval": true,
  "configuration": {
    "max_file_size_mb": 10,
    "allowed_extensions": ["pdf", "docx"],
    "auto_archive_days": 90
  },
  "rules": [
    {
      "rule_id": "770e8400-e29b-41d4-a716-446655440002",
      "rule_name": "Validate File Size",
      "rule_type": "validation",
      "rule_expression": "file_size_bytes <= 10485760",
      "rule_scope": "upload",
      "execution_order": 1,
      "is_active": true,
      "severity": "error",
      "error_message": "File size must not exceed 10 MB",
      "created_at": "2024-01-15T09:00:00Z"
    },
    {
      "rule_id": "880e8400-e29b-41d4-a716-446655440003",
      "rule_name": "Validate File Extension",
      "rule_type": "validation",
      "rule_expression": "file_extension IN ('pdf', 'docx')",
      "rule_scope": "upload",
      "execution_order": 2,
      "is_active": true,
      "severity": "error",
      "error_message": "Only PDF and DOCX files are allowed",
      "created_at": "2024-01-15T09:00:00Z"
    }
  ],
  "created_at": "2024-01-15T09:00:00Z",
  "created_by": "admin@example.com",
  "updated_at": "2024-06-20T14:30:00Z",
  "updated_by": "admin@example.com"
}
```

**Response (404 Not Found):**
```json
{
  "error": "TEMPLATE_NOT_FOUND",
  "message": "Template with ID 550e8400-e29b-41d4-a716-446655440000 not found"
}
```

---

### 4. Update Template

Update an existing template (creates a new version if status is active).

**Endpoint:** `PUT /templates/{templateId}`

**Path Parameters:**
- `templateId` (UUID, required): Template identifier

**Request Body:**
```json
{
  "template_name": "Updated Loan Application Form",
  "description": "Updated description",
  "retention_period_days": 3650,
  "configuration": {
    "max_file_size_mb": 15,
    "allowed_extensions": ["pdf", "docx", "jpg"],
    "auto_archive_days": 120
  }
}
```

**Response (200 OK):**
```json
{
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "template_code": "LOAN_APPLICATION",
  "version_number": 3,
  "template_name": "Updated Loan Application Form",
  "description": "Updated description",
  "status": "draft",
  "updated_at": "2024-11-01T11:00:00Z",
  "updated_by": "admin@example.com"
}
```

---

### 5. Publish Template Version

Publish a draft template or create a new version of an active template.

**Endpoint:** `POST /templates/{templateId}/publish`

**Path Parameters:**
- `templateId` (UUID, required): Template identifier

**Request Body (optional):**
```json
{
  "create_new_version": true,
  "deprecate_previous": true
}
```

**Response (200 OK):**
```json
{
  "template_id": "990e8400-e29b-41d4-a716-446655440010",
  "template_code": "LOAN_APPLICATION",
  "version_number": 4,
  "status": "active",
  "previous_version": {
    "template_id": "550e8400-e29b-41d4-a716-446655440000",
    "version_number": 3,
    "status": "deprecated"
  },
  "published_at": "2024-11-01T11:15:00Z",
  "published_by": "admin@example.com"
}
```

---

### 6. Get Template Version History

Retrieve all versions of a template.

**Endpoint:** `GET /templates/code/{templateCode}/versions`

**Path Parameters:**
- `templateCode` (string, required): Template code (e.g., "LOAN_APPLICATION")

**Response (200 OK):**
```json
{
  "template_code": "LOAN_APPLICATION",
  "total_versions": 4,
  "versions": [
    {
      "template_id": "990e8400-e29b-41d4-a716-446655440010",
      "version_number": 4,
      "status": "active",
      "template_name": "Loan Application Form v4",
      "created_at": "2024-11-01T11:15:00Z",
      "created_by": "admin@example.com"
    },
    {
      "template_id": "550e8400-e29b-41d4-a716-446655440000",
      "version_number": 3,
      "status": "deprecated",
      "template_name": "Loan Application Form v3",
      "created_at": "2024-06-20T14:30:00Z",
      "created_by": "admin@example.com"
    },
    {
      "template_id": "440e8400-e29b-41d4-a716-446655440009",
      "version_number": 2,
      "status": "deprecated",
      "template_name": "Loan Application Form v2",
      "created_at": "2024-03-10T10:00:00Z",
      "created_by": "admin@example.com"
    },
    {
      "template_id": "330e8400-e29b-41d4-a716-446655440008",
      "version_number": 1,
      "status": "deprecated",
      "template_name": "Loan Application Form",
      "created_at": "2024-01-15T09:00:00Z",
      "created_by": "admin@example.com"
    }
  ]
}
```

---

### 7. Delete Template

Delete a template (soft delete - marks as deleted).

**Endpoint:** `DELETE /templates/{templateId}`

**Path Parameters:**
- `templateId` (UUID, required): Template identifier

**Response (204 No Content)**

**Response (409 Conflict):**
```json
{
  "error": "TEMPLATE_IN_USE",
  "message": "Cannot delete template. 1,245 documents are using this template.",
  "details": {
    "document_count": 1245,
    "template_id": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## Template Rules APIs

### 1. Add Rule to Template

Add a new rule to an existing template.

**Endpoint:** `POST /templates/{templateId}/rules`

**Request Body:**
```json
{
  "rule_name": "Validate Customer SSN",
  "rule_type": "validation",
  "rule_expression": "REGEX_MATCH(metadata.ssn, '^\\d{3}-\\d{2}-\\d{4}$')",
  "rule_scope": "upload",
  "execution_order": 3,
  "is_active": true,
  "severity": "error",
  "error_message": "SSN must be in format XXX-XX-XXXX"
}
```

**Response (201 Created):**
```json
{
  "rule_id": "aa0e8400-e29b-41d4-a716-446655440011",
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "rule_name": "Validate Customer SSN",
  "rule_type": "validation",
  "rule_expression": "REGEX_MATCH(metadata.ssn, '^\\d{3}-\\d{2}-\\d{4}$')",
  "rule_scope": "upload",
  "execution_order": 3,
  "is_active": true,
  "severity": "error",
  "error_message": "SSN must be in format XXX-XX-XXXX",
  "created_at": "2024-11-01T11:30:00Z",
  "created_by": "admin@example.com"
}
```

---

### 2. Update Template Rule

Update an existing rule.

**Endpoint:** `PUT /templates/{templateId}/rules/{ruleId}`

**Request Body:**
```json
{
  "rule_expression": "REGEX_MATCH(metadata.ssn, '^\\d{9}$')",
  "is_active": true,
  "error_message": "SSN must be 9 digits"
}
```

**Response (200 OK):**
```json
{
  "rule_id": "aa0e8400-e29b-41d4-a716-446655440011",
  "rule_name": "Validate Customer SSN",
  "rule_expression": "REGEX_MATCH(metadata.ssn, '^\\d{9}$')",
  "is_active": true,
  "error_message": "SSN must be 9 digits",
  "updated_at": "2024-11-01T11:35:00Z",
  "updated_by": "admin@example.com"
}
```

---

### 3. Delete Template Rule

Delete a rule from a template.

**Endpoint:** `DELETE /templates/{templateId}/rules/{ruleId}`

**Response (204 No Content)**

---

## Document Management APIs

### 1. Upload Document

Upload a new document with metadata.

**Endpoint:** `POST /documents`

**Content-Type:** `multipart/form-data`

**Form Data:**
- `file` (file, required): Document file
- `metadata` (JSON, required): Document metadata

**Metadata JSON:**
```json
{
  "customer_id": "C123456",
  "customer_name": "John Doe",
  "account_id": "ACC-789012",
  "account_type": "CHECKING",
  "document_name": "Loan Application - John Doe",
  "document_type": "LOAN_APPLICATION",
  "document_category": "LENDING",
  "template_code": "LOAN_APPLICATION",
  "document_date": "2024-11-01",
  "is_confidential": true,
  "access_level": "CUSTOMER_ONLY",
  "tags": ["urgent", "new-customer"],
  "metadata": {
    "loan_amount": 50000,
    "loan_term_months": 60,
    "interest_rate": 5.5,
    "ssn": "123-45-6789"
  }
}
```

**Response (201 Created):**
```json
{
  "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
  "ecms_document_id": "ECMS-2024-11-01-12345",
  "customer_id": "C123456",
  "customer_name": "John Doe",
  "account_id": "ACC-789012",
  "account_type": "CHECKING",
  "document_name": "Loan Application - John Doe",
  "document_type": "LOAN_APPLICATION",
  "document_category": "LENDING",
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "template_code": "LOAN_APPLICATION",
  "template_version": 3,
  "file_extension": "pdf",
  "file_size_bytes": 1048576,
  "mime_type": "application/pdf",
  "status": "active",
  "is_confidential": true,
  "access_level": "CUSTOMER_ONLY",
  "document_date": "2024-11-01",
  "tags": ["urgent", "new-customer"],
  "metadata": {
    "loan_amount": 50000,
    "loan_term_months": 60,
    "interest_rate": 5.5
  },
  "uploaded_at": "2024-11-01T12:00:00Z",
  "created_by": "admin@example.com",
  "validation_results": {
    "passed": true,
    "rules_executed": 5,
    "warnings": []
  }
}
```

**Response (422 Unprocessable Entity - Validation Failed):**
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Document validation failed",
  "validation_errors": [
    {
      "rule_name": "Validate File Size",
      "rule_type": "validation",
      "severity": "error",
      "message": "File size must not exceed 10 MB",
      "actual_value": "15728640 bytes"
    }
  ]
}
```

---

### 2. Get Document by ID

Retrieve document metadata by ID.

**Endpoint:** `GET /documents/{documentId}`

**Response (200 OK):**
```json
{
  "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
  "ecms_document_id": "ECMS-2024-11-01-12345",
  "is_shared": false,
  "customer_id": "C123456",
  "customer_name": "John Doe",
  "account_id": "ACC-789012",
  "account_type": "CHECKING",
  "document_name": "Loan Application - John Doe",
  "document_type": "LOAN_APPLICATION",
  "document_category": "LENDING",
  "template": {
    "template_id": "550e8400-e29b-41d4-a716-446655440000",
    "template_code": "LOAN_APPLICATION",
    "template_name": "Loan Application Form",
    "version_number": 3
  },
  "file_extension": "pdf",
  "file_size_bytes": 1048576,
  "mime_type": "application/pdf",
  "status": "active",
  "is_confidential": true,
  "access_level": "CUSTOMER_ONLY",
  "document_date": "2024-11-01",
  "tags": ["urgent", "new-customer"],
  "metadata": {
    "loan_amount": 50000,
    "loan_term_months": 60,
    "interest_rate": 5.5
  },
  "uploaded_at": "2024-11-01T12:00:00Z",
  "modified_at": null,
  "created_by": "admin@example.com"
}
```

---

### 3. Get Document Download URL

Get a temporary download URL for the document file.

**Endpoint:** `GET /documents/{documentId}/download`

**Response (200 OK):**
```json
{
  "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
  "download_url": "https://s3.amazonaws.com/ecms-bucket/documents/ECMS-2024-11-01-12345.pdf?AWSAccessKeyId=...&Expires=1730462400&Signature=...",
  "expires_at": "2024-11-01T13:00:00Z",
  "content_type": "application/pdf",
  "file_size_bytes": 1048576
}
```

---

### 4. List Documents

List documents with filtering and pagination.

**Endpoint:** `GET /documents`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `customer_id` | string | No | Filter by customer ID |
| `account_id` | string | No | Filter by account ID |
| `document_type` | string | No | Filter by document type |
| `document_category` | string | No | Filter by category |
| `status` | string | No | Filter by status |
| `is_confidential` | boolean | No | Filter confidential docs |
| `date_from` | date | No | Document date from (YYYY-MM-DD) |
| `date_to` | date | No | Document date to (YYYY-MM-DD) |
| `tags` | string | No | Filter by tags (comma-separated) |
| `search` | string | No | Full-text search in document_name |
| `page` | integer | No | Page number (default: 0) |
| `size` | integer | No | Page size (default: 20, max: 100) |
| `sort` | string | No | Sort field (default: `uploaded_at,desc`) |

**Example Request:**
```http
GET /documents?customer_id=C123456&document_type=LOAN_APPLICATION&date_from=2024-01-01&page=0&size=20
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
      "document_name": "Loan Application - John Doe",
      "document_type": "LOAN_APPLICATION",
      "document_category": "LENDING",
      "customer_id": "C123456",
      "customer_name": "John Doe",
      "account_id": "ACC-789012",
      "file_extension": "pdf",
      "file_size_bytes": 1048576,
      "status": "active",
      "is_confidential": true,
      "document_date": "2024-11-01",
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

---

### 5. Update Document Metadata

Update document metadata (not the file itself).

**Endpoint:** `PATCH /documents/{documentId}`

**Request Body:**
```json
{
  "document_name": "Updated Loan Application - John Doe",
  "tags": ["urgent", "new-customer", "approved"],
  "is_confidential": false,
  "metadata": {
    "loan_amount": 55000,
    "loan_term_months": 72,
    "interest_rate": 5.0,
    "approval_status": "approved"
  }
}
```

**Response (200 OK):**
```json
{
  "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
  "document_name": "Updated Loan Application - John Doe",
  "tags": ["urgent", "new-customer", "approved"],
  "is_confidential": false,
  "metadata": {
    "loan_amount": 55000,
    "loan_term_months": 72,
    "interest_rate": 5.0,
    "approval_status": "approved"
  },
  "modified_at": "2024-11-01T13:00:00Z",
  "updated_by": "admin@example.com"
}
```

---

### 6. Delete Document

Delete a document (soft delete - marks as deleted).

**Endpoint:** `DELETE /documents/{documentId}`

**Response (204 No Content)**

---

## Shared Document APIs

### 1. Create Shared Document

Create a document that applies to multiple customers.

**Endpoint:** `POST /shared-documents`

**Content-Type:** `multipart/form-data`

**Form Data:**
- `file` (file, required): Document file
- `metadata` (JSON, required): Shared document metadata

**Metadata JSON:**
```json
{
  "document_name": "Privacy Policy 2024",
  "document_type": "PRIVACY_POLICY",
  "document_category": "REGULATORY",
  "template_code": "PRIVACY_POLICY_TEMPLATE",
  "sharing_scope": "all_customers",
  "effective_from": "2024-01-01T00:00:00Z",
  "effective_to": null,
  "is_confidential": false,
  "tags": ["regulatory", "privacy", "2024"],
  "metadata": {
    "regulation": "GDPR",
    "version": "2024.1",
    "approved_by": "Legal Department"
  }
}
```

**Response (201 Created):**
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
  "document_type": "PRIVACY_POLICY",
  "document_category": "REGULATORY",
  "template_id": "aa0e8400-e29b-41d4-a716-446655440005",
  "template_code": "PRIVACY_POLICY_TEMPLATE",
  "template_version": 1,
  "file_extension": "pdf",
  "file_size_bytes": 524288,
  "status": "active",
  "tags": ["regulatory", "privacy", "2024"],
  "metadata": {
    "regulation": "GDPR",
    "version": "2024.1",
    "approved_by": "Legal Department"
  },
  "uploaded_at": "2024-01-01T10:00:00Z",
  "created_by": "admin@example.com"
}
```

---

### 2. Assign Shared Document to Customers

Assign a shared document to specific customers (for sharing_scope = 'specific_customers').

**Endpoint:** `POST /shared-documents/{documentId}/assign`

**Request Body:**
```json
{
  "customer_ids": ["C123456", "C789012", "C345678"],
  "account_ids": null,
  "assigned_by": "admin@example.com"
}
```

**Response (200 OK):**
```json
{
  "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
  "assignments_created": 3,
  "assignments": [
    {
      "mapping_id": "dd0e8400-e29b-41d4-a716-446655440040",
      "customer_id": "C123456",
      "assigned_at": "2024-11-01T14:00:00Z"
    },
    {
      "mapping_id": "ee0e8400-e29b-41d4-a716-446655440041",
      "customer_id": "C789012",
      "assigned_at": "2024-11-01T14:00:00Z"
    },
    {
      "mapping_id": "ff0e8400-e29b-41d4-a716-446655440042",
      "customer_id": "C345678",
      "assigned_at": "2024-11-01T14:00:00Z"
    }
  ]
}
```

---

### 3. Unassign Shared Document from Customers

Remove shared document assignment from specific customers.

**Endpoint:** `POST /shared-documents/{documentId}/unassign`

**Request Body:**
```json
{
  "customer_ids": ["C123456", "C789012"]
}
```

**Response (200 OK):**
```json
{
  "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
  "assignments_removed": 2
}
```

---

### 4. Get Customers Assigned to Shared Document

Get list of customers who have access to a shared document.

**Endpoint:** `GET /shared-documents/{documentId}/customers`

**Query Parameters:**
- `page` (integer): Page number
- `size` (integer): Page size

**Response (200 OK):**
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
    },
    {
      "customer_id": "C789012",
      "customer_name": "Jane Smith",
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

---

### 5. List Shared Documents

List all shared documents with filtering.

**Endpoint:** `GET /shared-documents`

**Query Parameters:**
- `sharing_scope`: Filter by scope
- `document_type`: Filter by type
- `status`: active, expired
- `effective_as_of`: Date (returns docs effective at this date)
- `page`, `size`, `sort`

**Response (200 OK):**
```json
{
  "content": [
    {
      "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
      "document_name": "Privacy Policy 2024",
      "document_type": "PRIVACY_POLICY",
      "sharing_scope": "all_customers",
      "effective_from": "2024-01-01T00:00:00Z",
      "effective_to": null,
      "status": "active",
      "uploaded_at": "2024-01-01T10:00:00Z"
    },
    {
      "document_id": "dd0e8400-e29b-41d4-a716-446655440031",
      "document_name": "Regulatory Notice - California",
      "document_type": "REGULATORY_NOTICE",
      "sharing_scope": "specific_customers",
      "effective_from": "2024-06-01T00:00:00Z",
      "effective_to": "2024-12-31T23:59:59Z",
      "assigned_customers_count": 1245,
      "status": "active",
      "uploaded_at": "2024-05-15T10:00:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 45,
    "total_pages": 3
  }
}
```

---

## Customer Document Retrieval APIs

### 1. Get Customer Documents

Retrieve all documents for a specific customer (includes customer-specific + shared).

**Endpoint:** `GET /customers/{customerId}/documents`

**Path Parameters:**
- `customerId` (string, required): Customer identifier

**Query Parameters:**
- `document_type`: Filter by type
- `document_category`: Filter by category
- `as_of_date`: Date (ISO 8601) - returns documents effective at this date
- `include_shared`: boolean (default: true)
- `page`, `size`, `sort`

**Example Request:**
```http
GET /customers/C123456/documents?document_type=LOAN_APPLICATION&as_of_date=2024-11-01T12:00:00Z&page=0&size=20
```

**Response (200 OK):**
```json
{
  "customer_id": "C123456",
  "customer_name": "John Doe",
  "content": [
    {
      "document_id": "bb0e8400-e29b-41d4-a716-446655440020",
      "document_name": "Loan Application - John Doe",
      "document_type": "LOAN_APPLICATION",
      "document_category": "LENDING",
      "is_shared": false,
      "file_extension": "pdf",
      "file_size_bytes": 1048576,
      "document_date": "2024-11-01",
      "uploaded_at": "2024-11-01T12:00:00Z"
    },
    {
      "document_id": "cc0e8400-e29b-41d4-a716-446655440030",
      "document_name": "Privacy Policy 2024",
      "document_type": "PRIVACY_POLICY",
      "document_category": "REGULATORY",
      "is_shared": true,
      "sharing_scope": "all_customers",
      "effective_from": "2024-01-01T00:00:00Z",
      "effective_to": null,
      "file_extension": "pdf",
      "file_size_bytes": 524288,
      "uploaded_at": "2024-01-01T10:00:00Z"
    }
  ],
  "summary": {
    "total_documents": 156,
    "customer_specific": 154,
    "shared": 2
  },
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 156,
    "total_pages": 8
  }
}
```

---

### 2. Get Account Documents

Retrieve all documents for a specific account.

**Endpoint:** `GET /accounts/{accountId}/documents`

**Response structure same as customer documents**

---

## Analytics & Reporting APIs

### 1. Document Statistics

Get document statistics and counts.

**Endpoint:** `GET /analytics/documents/stats`

**Query Parameters:**
- `customer_id`: Filter by customer
- `date_from`, `date_to`: Date range
- `group_by`: Group results by (type, category, customer, month)

**Response (200 OK):**
```json
{
  "total_documents": 1245678,
  "customer_specific_documents": 1245100,
  "shared_documents": 578,
  "by_type": [
    {
      "document_type": "ACCOUNT_STATEMENT",
      "count": 456789
    },
    {
      "document_type": "LOAN_APPLICATION",
      "count": 123456
    }
  ],
  "by_category": [
    {
      "document_category": "BANKING",
      "count": 678901
    },
    {
      "document_category": "LENDING",
      "count": 234567
    }
  ],
  "storage_usage": {
    "total_bytes": 5368709120,
    "total_gb": 5.0
  },
  "recent_activity": {
    "documents_uploaded_today": 1245,
    "documents_uploaded_this_week": 8901,
    "documents_uploaded_this_month": 34567
  }
}
```

---

### 2. Template Usage Statistics

Get template usage statistics.

**Endpoint:** `GET /analytics/templates/usage`

**Response (200 OK):**
```json
{
  "total_templates": 400,
  "active_templates": 385,
  "deprecated_templates": 15,
  "templates_with_documents": [
    {
      "template_code": "ACCOUNT_STATEMENT",
      "template_name": "Monthly Account Statement",
      "version_number": 2,
      "document_count": 456789,
      "status": "active"
    },
    {
      "template_code": "LOAN_APPLICATION",
      "template_name": "Loan Application Form",
      "version_number": 3,
      "document_count": 123456,
      "status": "active"
    }
  ]
}
```

---

## Common Responses

### Success Response Structure

All successful responses follow this structure:

**Single Resource:**
```json
{
  "field1": "value1",
  "field2": "value2",
  ...
}
```

**Collection (Paginated):**
```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "total_elements": 100,
    "total_pages": 5
  }
}
```

---

## Error Codes

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation error, malformed request |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | User doesn't have permission |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., duplicate) |
| 422 | Unprocessable Entity | Business logic validation failed |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

### Error Response Structure

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {
    "field1": "detail1",
    "field2": "detail2"
  },
  "timestamp": "2024-11-01T12:00:00Z",
  "path": "/api/v1/documents/123"
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Access denied |
| `RESOURCE_NOT_FOUND` | 404 | Resource doesn't exist |
| `TEMPLATE_NOT_FOUND` | 404 | Template not found |
| `DOCUMENT_NOT_FOUND` | 404 | Document not found |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists |
| `TEMPLATE_IN_USE` | 409 | Cannot delete template in use |
| `VALIDATION_FAILED` | 422 | Business rule validation failed |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Internal server error |

---

## Rate Limiting

### Rate Limit Headers

All responses include rate limit headers:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1730462400
```

### Rate Limit Response (429)

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "API rate limit exceeded. Try again in 60 seconds.",
  "details": {
    "limit": 1000,
    "remaining": 0,
    "reset_at": "2024-11-01T13:00:00Z"
  }
}
```

### Rate Limits by User Type

| User Type | Requests per Minute | Requests per Hour |
|-----------|---------------------|-------------------|
| Admin | 1000 | 50,000 |
| Standard User | 500 | 10,000 |
| Public API | 100 | 5,000 |

---

## Summary

This API specification covers:

1. **Authentication** - JWT-based authentication
2. **Template Management** - Full CRUD for templates and versioning
3. **Template Rules** - Manage validation and business rules
4. **Document Management** - Upload, retrieve, update documents
5. **Shared Documents** - Create and manage documents for multiple customers
6. **Customer Retrieval** - Get all documents for a customer (including shared)
7. **Analytics** - Statistics and reporting endpoints

**Total Endpoints**: ~35+ RESTful endpoints

**Documentation Format**: Can be converted to OpenAPI 3.0 specification for Swagger UI

**Next Steps**:
1. Generate OpenAPI 3.0 YAML file
2. Set up Swagger UI for interactive documentation
3. Create Postman collection for testing
4. Add WebFlux reactive examples for Spring implementation
