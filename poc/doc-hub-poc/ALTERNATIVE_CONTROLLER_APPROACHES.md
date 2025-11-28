# Alternative Controller Approaches for Request Body

## Current Approach (What You Have Now)

### Current Controller:
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<ResponseEntity<InlineResponse200>> uploadDocument(
    @RequestHeader("X-version") Integer xVersion,
    @RequestHeader("X-correlation-id") String xCorrelationId,
    @RequestHeader("X-requestor-id") UUID xRequestorId,
    @RequestHeader("X-requestor-type") XRequestorType xRequestorType,

    // 15+ individual parts! 😰
    @RequestPart("documentType") String documentType,
    @RequestPart("createdBy") UUID createdBy,
    @RequestPart("templateId") UUID templateId,
    @RequestPart("referenceKey") String referenceKey,
    @RequestPart("referenceKeyType") String referenceKeyType,
    @RequestPart("accountKey") UUID accountKey,
    @RequestPart("customerKey") UUID customerKey,
    @RequestPart("category") String category,
    @RequestPart("fileName") String fileName,
    @RequestPart("activeStartDate") Long activeStartDate,
    @RequestPart("activeEndDate") Long activeEndDate,
    @RequestPart("threadId") UUID threadId,
    @RequestPart("correlationId") UUID correlationId,
    @RequestPart("content") Mono<FilePart> content,
    @RequestPart("metadata") String metadataJson
) {
    // Parse metadata, build DocumentUploadRequest manually
}
```

**Problems**:
- ❌ 15+ method parameters
- ❌ Hard to read
- ❌ Manual parsing required
- ❌ Client must send many individual parts

---

## Approach 1: Single JSON Part (Recommended) ⭐

### Step 1: Use Existing DocumentUploadRequest Class

You already have this class! It's in:
```
src/main/java/io/swagger/model/DocumentUploadRequest.java
```

### Step 2: Update Controller to Accept JSON Part

```java
package io.swagger.api;

import io.swagger.model.*;
import io.swagger.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/documents")
@Validated
@RequiredArgsConstructor
public class DocumentsController {

    private final DocumentService documentService;

    /**
     * Upload a document
     * Uses a single JSON part for all metadata
     */
    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<InlineResponse200>> uploadDocument(
        // Headers
        @NotNull @RequestHeader("X-version") Integer xVersion,
        @NotNull @RequestHeader("X-correlation-id") String xCorrelationId,
        @NotNull @RequestHeader("X-requestor-id") UUID xRequestorId,
        @NotNull @RequestHeader("X-requestor-type") XRequestorType xRequestorType,

        // Only 2 parts! 🎉
        @NotNull @RequestPart("content") Mono<FilePart> content,
        @Valid @NotNull @RequestPart("request") DocumentUploadRequest request
    ) {
        log.info("Document upload request received. CorrelationId: {}, DocumentType: {}",
                 xCorrelationId, request.getDocumentType());

        // That's it! No parsing needed, Spring handles JSON deserialization
        return documentService.uploadDocument(
            xCorrelationId,
            xRequestorId,
            xRequestorType,
            request,
            content
        );
    }
}
```

**Advantages**:
- ✅ Only 2 parts: `content` and `request`
- ✅ Spring automatically deserializes JSON to `DocumentUploadRequest`
- ✅ Validation annotations work (`@Valid`)
- ✅ Much cleaner controller
- ✅ Type-safe with generated class

### Step 3: Update OpenAPI Spec

To make this work properly, update your `api.yaml`:

```yaml
/documents:
  post:
    requestBody:
      content:
        multipart/form-data:
          schema:
            type: object
            required:
              - content
              - request
            properties:
              content:
                type: string
                format: binary
                description: The file to upload
              request:
                $ref: "#/components/schemas/DocumentUploadMetadata"
          encoding:
            request:
              contentType: application/json
            content:
              contentType: application/octet-stream
```

Then add a new schema:

```yaml
components:
  schemas:
    DocumentUploadMetadata:
      required:
        - createdBy
        - documentType
        - metadata
      type: object
      properties:
        documentType:
          $ref: "#/components/schemas/DocumentTypes"
        createdBy:
          type: string
          format: uuid
        templateId:
          type: string
          format: uuid
        referenceKey:
          type: string
        referenceKeyType:
          $ref: "#/components/schemas/referenceKeyType"
        accountKey:
          type: string
          format: uuid
        customerKey:
          type: string
          format: uuid
        category:
          type: string
        fileName:
          type: string
        activeStartDate:
          $ref: "#/components/schemas/Date"
        activeEndDate:
          $ref: "#/components/schemas/Date"
        threadId:
          type: string
          format: uuid
        correlationId:
          type: string
          format: uuid
        metadata:
          $ref: "#/components/schemas/Metadata"
```

### Step 4: Client Request Example

**Curl**:
```bash
curl -X POST "http://localhost:8080/documents" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-123" \
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-requestor-type: SYSTEM" \
  -F "content=@document.pdf" \
  -F 'request={
    "documentType": "PaymentLetter",
    "createdBy": "550e8400-e29b-41d4-a716-446655440001",
    "metadata": [
      {"key": "accountId", "value": "123"}
    ]
  };type=application/json'
```

**Postman**:
1. Method: POST
2. URL: `http://localhost:8080/documents`
3. Body → form-data:
   - `content`: [Select File]
   - `request`: (Text, set Content-Type to `application/json`)
     ```json
     {
       "documentType": "PaymentLetter",
       "createdBy": "550e8400-e29b-41d4-a716-446655440001",
       "metadata": [
         {"key": "accountId", "value": "123"}
       ]
     }
     ```

---

## Approach 2: Use @ModelAttribute with Custom Binder

This is more advanced but gives you the best of both worlds.

### Step 1: Create a Custom Request Wrapper

```java
package io.swagger.model;

import lombok.Data;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class DocumentUploadRequestWrapper {

    @NotNull
    private Mono<FilePart> content;

    @Valid
    @NotNull
    private DocumentUploadRequest metadata;
}
```

### Step 2: Use @ModelAttribute

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<ResponseEntity<InlineResponse200>> uploadDocument(
    @RequestHeader("X-version") Integer xVersion,
    @RequestHeader("X-correlation-id") String xCorrelationId,
    @RequestHeader("X-requestor-id") UUID xRequestorId,
    @RequestHeader("X-requestor-type") XRequestorType xRequestorType,
    @Valid @ModelAttribute DocumentUploadRequestWrapper uploadRequest
) {
    return documentService.uploadDocument(
        xCorrelationId,
        xRequestorId,
        xRequestorType,
        uploadRequest.getMetadata(),
        uploadRequest.getContent()
    );
}
```

**Note**: This requires custom configuration for WebFlux multipart binding.

---

## Comparison

| Approach | Pros | Cons | Complexity |
|----------|------|------|------------|
| **Current (Individual Parts)** | ✅ Simple<br>✅ No spec changes | ❌ 15+ parameters<br>❌ Hard to read | ⭐ Easy |
| **Single JSON Part** | ✅ Clean code<br>✅ Auto deserialization<br>✅ Type-safe | ⚠️ Spec changes<br>⚠️ Client changes | ⭐⭐ Medium |
| **@ModelAttribute** | ✅ Very clean<br>✅ Automatic binding | ❌ Custom config needed<br>❌ Complex setup | ⭐⭐⭐⭐ Hard |

---

## Recommended: Approach 1 (Single JSON Part)

### Why?

1. **Cleaner Controller**: Only 2 parameters instead of 15+
2. **Type Safety**: Uses generated `DocumentUploadRequest` class
3. **Validation**: `@Valid` works automatically
4. **Spring Handles It**: Automatic JSON deserialization
5. **Client Friendly**: Send one JSON object instead of many parts

### Implementation Steps:

1. **Update `api.yaml`** (add `DocumentUploadMetadata` schema)
2. **Regenerate models**: `mvn clean generate-sources`
3. **Update controller** to use `@RequestPart("request") DocumentUploadRequest request`
4. **Update clients** to send metadata as single JSON part

---

## Quick Implementation

Want me to implement Approach 1 for you? I can:

1. ✅ Update `api.yaml` with new schema
2. ✅ Regenerate the model class
3. ✅ Update the controller
4. ✅ Provide client examples

This will make your controller much cleaner and easier to maintain!

---

## Example: Before vs After

### Before (Current):
```java
public Mono<ResponseEntity<T>> uploadDocument(
    @RequestPart String documentType,
    @RequestPart UUID createdBy,
    @RequestPart UUID templateId,
    @RequestPart String referenceKey,
    @RequestPart String referenceKeyType,
    @RequestPart UUID accountKey,
    @RequestPart UUID customerKey,
    @RequestPart String category,
    @RequestPart String fileName,
    @RequestPart Long activeStartDate,
    @RequestPart Long activeEndDate,
    @RequestPart UUID threadId,
    @RequestPart UUID correlationId,
    @RequestPart Mono<FilePart> content,
    @RequestPart String metadataJson
) {
    // Manual parsing and object building
    List<MetadataNode> metadata = parseMetadata(metadataJson);
    DocumentUploadRequest request = new DocumentUploadRequest();
    request.setDocumentType(documentType);
    request.setCreatedBy(createdBy);
    // ... 13 more setters
}
```

### After (Approach 1):
```java
public Mono<ResponseEntity<T>> uploadDocument(
    @RequestPart("content") Mono<FilePart> content,
    @RequestPart("request") @Valid DocumentUploadRequest request
) {
    // That's it! Spring does the rest
    return documentService.uploadDocument(..., request, content);
}
```

**Much cleaner!** 🎉
