# API Specification Changes Required

**Created:** December 2024
**File:** `src/main/resources/doc-hub.yaml`
**Source:** P0 and P1 Implementation Specs, John's Feedback

This document lists all required changes to the OpenAPI specification (`doc-hub.yaml`).

---

## Summary of Changes

| # | Change | Priority | Location | Source |
|---|--------|----------|----------|--------|
| 1 | Add `messageCenterDocFlag` parameter | P0-001 | DocumentListRequest | Day 3 |
| 2 | Add `CommunicationType` enum | P0-002 | New schema | Day 3 |
| 3 | Add `communicationType` parameter | P0-002 | DocumentListRequest | Day 3 |
| 4 | Add `expiresAt` to download links | P1-003 | Links_download | Day 1 |
| 5 | Remove delete link from enquiry | P1-004 | Links schema | Day 1 |
| 6 | Add `WorkflowType` enum | P0-006 | New schema (for template API) | Day 3 |

---

## Detailed Changes

### 1. Add `messageCenterDocFlag` to DocumentListRequest (P0-001)

**Location:** `components/schemas/DocumentListRequest/properties`

**Add after `lineOfBusiness`:**

```yaml
        messageCenterDocFlag:
          type: boolean
          description: |
            Filter documents by message center eligibility.
            - true (default): Return only documents displayable in message center
            - false: Return BOTH message center and non-message center documents
          default: true
          example: true
          x-data-classification: public
```

---

### 2. Add `CommunicationType` Enum (P0-002)

**Location:** `components/schemas` (new schema)

**Add after `X-requestor-type`:**

```yaml
    CommunicationType:
      type: string
      description: |
        Communication channel for document delivery.
        - LETTER: Physical mail delivery (default)
        - EMAIL: Electronic mail delivery
        - SMS: Text message delivery
        - PUSH: Push notification delivery
      enum:
        - LETTER
        - EMAIL
        - SMS
        - PUSH
      default: LETTER
      example: LETTER
      x-data-classification: public
```

---

### 3. Add `communicationType` to DocumentListRequest (P0-002)

**Location:** `components/schemas/DocumentListRequest/properties`

**Add after `messageCenterDocFlag`:**

```yaml
        communicationType:
          $ref: "#/components/schemas/CommunicationType"
```

---

### 4. Add `expiresAt` to Links_download (P1-003)

**Location:** `components/schemas/Links_download/properties`

**Add after `responseTypes`:**

```yaml
        expiresAt:
          type: integer
          format: int64
          description: |
            Epoch timestamp (seconds) when this download link expires.
            Client should request a new link if current time exceeds this value.
            Typical validity: 10 minutes from generation.
          example: 1740527443
          x-data-classification: public
```

---

### 5. Update Links Schema - Remove Delete from Enquiry Response (P1-004)

**Option A: Make delete optional (recommended)**

The current `Links` schema already has delete as optional. The change is in the **service layer** to NOT populate it for enquiry responses.

**Option B: Create separate schema for enquiry**

If strict schema enforcement is needed, create a new schema:

```yaml
    LinksEnquiry:
      type: object
      description: |
        HATEOAS links for document enquiry responses.
        Note: Delete action is NOT available in enquiry - use direct DELETE endpoint.
      properties:
        download:
          $ref: "#/components/schemas/Links_download"
```

Then use `LinksEnquiry` in `DocumentDetailsNode` for enquiry responses.

**Recommendation:** Use Option A (service-level filtering) as it's more flexible.

---

### 6. Add `WorkflowType` Enum (P0-006)

**Location:** `components/schemas` (new schema)

**For future template management API:**

```yaml
    WorkflowType:
      type: string
      description: |
        WCM workflow type for template approval process.
        - 2_EYES: Single person approval (maker only)
        - 4_EYES: Dual person approval (maker-checker)
      enum:
        - 2_EYES
        - 4_EYES
      example: 2_EYES
      x-data-classification: public
```

---

## Complete DocumentListRequest After Changes

```yaml
    DocumentListRequest:
      type: object
      properties:
        customerId:
          type: string
          description: Filter documents by customer ID. Combined with AND logic if accountId is provided.
          format: uuid
          x-data-classification: public
        accountId:
          type: array
          description: "Filter documents by account ID. Combined with AND logic if customerId is provided."
          format: uuid
          example:
            - 3fa85f64-5717-4562-b3fc-2c963f66afa6
          x-data-classification: confidential
          items:
            type: string
        lineOfBusiness:
          type: string
          description: "Filter templates by line of business. Default: CREDIT_CARD. ENTERPRISE templates always included."
          enum:
            - CREDIT_CARD
            - DIGITAL_BANK
            - ENTERPRISE
          example: CREDIT_CARD
          x-data-classification: public
        messageCenterDocFlag:
          type: boolean
          description: |
            Filter documents by message center eligibility.
            - true (default): Return only documents displayable in message center
            - false: Return BOTH message center and non-message center documents
          default: true
          example: true
          x-data-classification: public
        communicationType:
          $ref: "#/components/schemas/CommunicationType"
        referenceKey:
          type: string
          description: Filter documents based on reference key associated with the document
          example: D164
          x-data-classification: public
        referenceKeyType:
          type: string
          description: Indicates the type of reference key being used
          example: Disclosure_Code
          x-data-classification: public
        documentTypeCategoryGroup:
          type: array
          x-data-classification: public
          items:
            $ref: "#/components/schemas/DocumentCategoryGroup"
        postedFromDate:
          $ref: "#/components/schemas/Date"
        postedToDate:
          $ref: "#/components/schemas/Date"
        pageNumber:
          type: number
          x-data-classification: public
        pageSize:
          type: number
          x-data-classification: public
        sortOrder:
          $ref: "#/components/schemas/SortOrder"
```

---

## Complete Links_download After Changes

```yaml
    Links_download:
      type: object
      properties:
        href:
          type: string
          description: |
            Returns the binary content of the document. Client must set Accept header appropriately.
          example: /documents/eyJ21pbWFnZVBh2dGgiOi4IiL
          x-data-classification: public
        type:
          type: string
          example: GET
          x-data-classification: public
        rel:
          type: string
          example: download
          x-data-classification: public
        title:
          type: string
          example: Download this document
          x-data-classification: public
        responseTypes:
          type: array
          example:
            - application/pdf
            - application/octet-stream
          x-data-classification: public
          items:
            type: string
        expiresAt:
          type: integer
          format: int64
          description: |
            Epoch timestamp (seconds) when this download link expires.
            Client should request a new link if current time exceeds this value.
          example: 1740527443
          x-data-classification: public
```

---

## New Schemas Section

Add these new schemas to `components/schemas`:

```yaml
    # ====================================================================
    # New Schemas for P0/P1 Implementation
    # ====================================================================

    CommunicationType:
      type: string
      description: |
        Communication channel for document delivery.
        - LETTER: Physical mail delivery (default)
        - EMAIL: Electronic mail delivery
        - SMS: Text message delivery
        - PUSH: Push notification delivery
      enum:
        - LETTER
        - EMAIL
        - SMS
        - PUSH
      default: LETTER
      example: LETTER
      x-data-classification: public

    WorkflowType:
      type: string
      description: |
        WCM workflow type for template approval process.
        - 2_EYES: Single person approval (maker only)
        - 4_EYES: Dual person approval (maker-checker)
      enum:
        - 2_EYES
        - 4_EYES
      example: 2_EYES
      x-data-classification: public
```

---

## Implementation Checklist

### OpenAPI Changes

- [ ] Add `CommunicationType` enum schema
- [ ] Add `WorkflowType` enum schema
- [ ] Add `messageCenterDocFlag` to DocumentListRequest
- [ ] Add `communicationType` to DocumentListRequest
- [ ] Add `expiresAt` to Links_download
- [ ] Verify delete link is optional in Links schema

### Post-Change Steps

1. **Regenerate Models:**
   ```bash
   mvn clean generate-sources
   ```

2. **Update Service Layer:**
   - Read new parameters from request
   - Apply filters in repository queries
   - Populate `expiresAt` in response
   - Filter links based on access control

3. **Update Tests:**
   - Add tests for new parameters
   - Verify default values work correctly

---

## Migration Notes

### Breaking Changes

**None** - All changes are additive with sensible defaults:
- `messageCenterDocFlag` defaults to `true` (current behavior)
- `communicationType` defaults to `LETTER` (current behavior)
- `expiresAt` is optional in response

### Backward Compatibility

Existing clients will:
- Continue to work without sending new parameters
- Receive the same documents (default filters match current behavior)
- See new `expiresAt` field in responses (should be ignored if not needed)

---

## Example Request/Response

### Request (Using New Parameters)

```json
POST /documents-enquiry
Headers:
  X-version: 1
  X-correlation-id: abc-123
  X-requestor-id: 3fa85f64-5717-4562-b3fc-2c963f66afa6
  X-requestor-type: CUSTOMER

Body:
{
  "accountId": ["3fa85f64-5717-4562-b3fc-2c963f66afa6"],
  "lineOfBusiness": "CREDIT_CARD",
  "messageCenterDocFlag": true,
  "communicationType": "LETTER",
  "pageNumber": 1,
  "pageSize": 10
}
```

### Response (With New Fields)

```json
{
  "documentList": [
    {
      "documentId": "eyJ21pbWFnZVBh2dGgiOi4IiL",
      "displayName": "Privacy Policy 2024",
      "documentType": "PrivacyPolicy",
      "lineOfBusiness": "CREDIT_CARD",
      "datePosted": 1704067200,
      "_links": {
        "download": {
          "href": "/documents/eyJ21pbWFnZVBh2dGgiOi4IiL",
          "type": "GET",
          "rel": "download",
          "title": "Download this document",
          "responseTypes": ["application/pdf"],
          "expiresAt": 1740527443
        }
      }
    }
  ],
  "pagination": {
    "pageSize": 10,
    "totalItems": 1,
    "totalPages": 1,
    "pageNumber": 1
  }
}
```

**Note:** No `delete` link in response (per John's requirement).

---

## Document History

| Date | Author | Changes |
|------|--------|---------|
| Dec 2024 | Team | Initial creation |
