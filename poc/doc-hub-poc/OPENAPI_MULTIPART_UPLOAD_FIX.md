# OpenAPI Multipart File Upload Issues and Solutions

## Document Information
- **Date**: 2025-11-28
- **Project**: Document Hub POC
- **OpenAPI Spec**: `src/main/resources/api.yaml`
- **Affected Endpoint**: `POST /documents` (Document Upload)

## Problem Summary

The OpenAPI specification for the document upload endpoint was generating incorrect Java code for Spring WebFlux reactive applications, causing runtime errors related to:

1. **Content field type mismatch**: Generated `MultipartFile` instead of `FilePart`
2. **Metadata complex object**: Missing encoding specification for JSON serialization in multipart requests

---

## Issue #1: Content Field - Wrong Type for Reactive WebFlux

### Current OpenAPI Specification
```yaml
# Line 531-534 in api.yaml
content:
  type: string
  format: binary
  description: The actual body or data of the document (e.g., PDF, text, or JSON).
```

### Generated Code (Incorrect for WebFlux)
```java
// Line 81 in DocumentsApi.java
@RequestPart(value="content", required=true) MultipartFile content
```

### Problem Description
- **Spring MVC (Servlet-based)**: Uses `MultipartFile` for file uploads (blocking I/O)
- **Spring WebFlux (Reactive)**: Requires `FilePart` for file uploads (non-blocking I/O)
- The OpenAPI code generator defaults to `MultipartFile` even when `reactive=true` is set
- This causes runtime errors when the reactive server expects `FilePart` but receives `MultipartFile`

### Root Cause
The OpenAPI generator doesn't automatically map `type: string, format: binary` to `FilePart` in reactive mode without additional configuration or custom templates.

---

## Issue #2: Metadata Field - Complex Object in Multipart Form-Data

### Current OpenAPI Specification
```yaml
# Line 535-536 in api.yaml
metadata:
  $ref: "#/components/schemas/Metadata"
```

### Metadata Schema Definition
```yaml
# Lines 776-790 in api.yaml
Metadata:
  type: "array"
  description: "Each document type has its own set of required metadata..."
  items:
    $ref: "#/components/schemas/MetadataNode"

MetadataNode:
  required:
  - "key"
  - "value"
  type: "object"
  properties:
    key:
      type: "string"
      pattern: "^[a-zA-Z0-9_]+"
    value:
      type: "string"
    dataType:
      type: "string"
      enum: ["STRING", "NUMBER", "BOOLEAN", "DATE"]
```

### Generated Code
```java
// Line 82 in DocumentsApi.java
@RequestPart(value="metadata", required=true) Metadata metadata
```

### Problem Description
- **Multipart form-data** doesn't natively support complex nested JSON structures
- Clients need to know that the `metadata` part should be serialized as JSON
- Without explicit encoding specification, clients may send it incorrectly
- Spring expects JSON deserialization but the spec doesn't declare it

### Example of the Issue
Without proper encoding specification, clients might send metadata like:
```
Content-Disposition: form-data; name="metadata"

[object Object]
```

Instead of:
```
Content-Disposition: form-data; name="metadata"
Content-Type: application/json

[{"key":"accountId","value":"123"},{"key":"customerId","value":"456"}]
```

---

## Solutions

### Solution #1: Add Encoding Specification for Metadata

Update the request body definition to include encoding information:

#### Before (Lines 367-372)
```yaml
requestBody:
  content:
    multipart/form-data:
      schema:
        $ref: "#/components/schemas/documentUploadRequest"
  required: true
```

#### After (Recommended Fix)
```yaml
requestBody:
  content:
    multipart/form-data:
      schema:
        $ref: "#/components/schemas/documentUploadRequest"
      encoding:
        metadata:
          contentType: application/json
        content:
          contentType: application/octet-stream
  required: true
```

#### What This Does
- Explicitly tells code generators and API clients that `metadata` must be sent as JSON
- Specifies that `content` is binary data (octet-stream)
- Ensures proper serialization/deserialization on both client and server sides
- Generates better documentation in Swagger UI

---

### Solution #2: Fix Content Field Type for WebFlux

There are **three approaches** to resolve the `MultipartFile` vs `FilePart` issue:

#### **Approach A: OpenAPI Generator Configuration (Recommended)**

Ensure your Maven plugin configuration has the correct reactive settings:

```xml
<!-- In pom.xml -->
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>6.6.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <library>spring-boot</library>
                <output>${project.build.directory}/generated-sources</output>
                <configOptions>
                    <reactive>true</reactive>
                    <interfaceOnly>true</interfaceOnly>
                    <delegatePattern>false</delegatePattern>
                    <useSpringBoot3>false</useSpringBoot3>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Note**: As of OpenAPI Generator 6.6.0, the reactive mode may still generate `MultipartFile`. Newer versions (7.x) have better WebFlux support.

#### **Approach B: Use OpenAPI Generator 7.x or Later**

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.10.0</version> <!-- Use latest version -->
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <library>spring-boot</library>
                <output>${project.build.directory}/generated-sources</output>
                <configOptions>
                    <reactive>true</reactive>
                    <interfaceOnly>true</interfaceOnly>
                    <useSpringBoot3>false</useSpringBoot3>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### **Approach C: Manual Override (Temporary Workaround)**

If code generation still produces `MultipartFile`, manually edit the generated interface:

```java
// Change this:
@RequestPart(value="content", required=true) MultipartFile content

// To this:
@RequestPart(value="content", required=true) org.springframework.http.codec.multipart.FilePart content
```

Import statement needed:
```java
import org.springframework.http.codec.multipart.FilePart;
```

**Drawback**: This change will be overwritten if you regenerate the code.

---

## Complete Fixed OpenAPI Specification

Here's the complete corrected endpoint definition:

```yaml
/documents:
  post:
    tags:
    - Document Management
    summary: Upload a document
    operationId: add-document
    description: |
      Uploads a document for storage.

        The files currently are not scanned for viruses on the server end. It'll be part of a future implementation. Currently it'll be the responibility of the client.

        For text based files, if Byte Order Mark (BOM) is present, it will be preserved. It is the responsibility of the client to correclty handle the BOM at the time of retrieval.
    parameters:
    - name: "X-version"
      in: "header"
      description: "Api version"
      required: true
      schema:
        $ref: "#/components/schemas/Int32"
    - name: "X-correlation-id"
      in: "header"
      description: "Correlation ID for request tracing"
      required: true
      schema:
        type: "string"
        maxLength: 36
    - name: "X-requestor-id"
      in: "header"
      description: "Id of the requestor"
      required: true
      schema:
        type: "string"
        format: "uuid"
    - name: "X-requestor-type"
      in: "header"
      description: "Type of the requestor"
      required: true
      schema:
        $ref: "#/components/schemas/X-requestor-type"
    requestBody:
      content:
        multipart/form-data:
          schema:
            $ref: "#/components/schemas/documentUploadRequest"
          encoding:
            metadata:
              contentType: application/json
            content:
              contentType: application/octet-stream
      required: true
    responses:
      "200":
        description: OK
        content:
          application/json:
            schema:
              type: object
              properties:
                id:
                  type: string
                  format: uuid
      "400":
        description: Bad Request
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "401":
        description: Unauthorized access to an endpoint
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "404":
        description: Not Found
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "409":
        description: Cannot be processed. Antivirus scan failed.
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "500":
        description: Internal Server Error
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
      "503":
        description: Service Unavailable
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ErrorResponse"
```

---

## Implementation Steps

### Step 1: Update OpenAPI Specification
1. Open `src/main/resources/api.yaml`
2. Locate the `/documents` POST endpoint (around line 328)
3. Update the `requestBody` section to include the `encoding` specification
4. Save the file

### Step 2: Update Maven Plugin Configuration
1. Open `pom.xml`
2. Uncomment the OpenAPI generator plugin (lines 33-57)
3. Verify or update the version to 7.10.0 or later
4. Ensure `<reactive>true</reactive>` is set in `configOptions`
5. Save the file

### Step 3: Regenerate Code
```bash
mvn clean compile
```

This will:
- Delete old generated code
- Regenerate API interfaces from the updated OpenAPI spec
- Compile the project

### Step 4: Verify Generated Code
Check the generated `DocumentsApi.java`:
```bash
# Windows
type target\generated-sources\src\main\java\org\openapitools\api\DocumentsApi.java

# Or manually inspect the file
```

Look for:
- `FilePart content` (not `MultipartFile content`)
- `@RequestPart(value="metadata", required=true) Metadata metadata`

### Step 5: Update Implementation
If you have a custom implementation of `DocumentsApiController`, update the method signature to match the generated interface.

---

## Testing the Fix

### Test Case 1: Upload with Curl

```bash
curl -X POST "http://localhost:8080/documents" \
  -H "X-version: 1" \
  -H "X-correlation-id: test-correlation-123" \
  -H "X-requestor-id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-requestor-type: SYSTEM" \
  -F "documentType=PaymentLetter" \
  -F "createdBy=550e8400-e29b-41d4-a716-446655440001" \
  -F "templateId=550e8400-e29b-41d4-a716-446655440002" \
  -F "content=@/path/to/document.pdf" \
  -F 'metadata=[{"key":"accountId","value":"123"},{"key":"customerId","value":"456"}];type=application/json'
```

### Test Case 2: Upload with Postman

1. Set method to `POST`
2. URL: `http://localhost:8080/documents`
3. Headers:
   - `X-version: 1`
   - `X-correlation-id: test-123`
   - `X-requestor-id: 550e8400-e29b-41d4-a716-446655440000`
   - `X-requestor-type: SYSTEM`
4. Body (form-data):
   - `documentType`: PaymentLetter (Text)
   - `createdBy`: 550e8400-e29b-41d4-a716-446655440001 (Text)
   - `templateId`: 550e8400-e29b-41d4-a716-446655440002 (Text)
   - `content`: [Select File] document.pdf (File)
   - `metadata`: (Text, manually set Content-Type to application/json)
     ```json
     [{"key":"accountId","value":"123"},{"key":"customerId","value":"456"}]
     ```

### Expected Response

```json
{
  "id": "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6"
}
```

---

## Additional Considerations

### WebFlux vs MVC Decision

If you're using **Spring WebFlux** (reactive):
- ✅ Use `FilePart`
- ✅ Set `reactive=true` in OpenAPI generator
- ✅ Use reactive repository implementations (R2DBC, Reactive MongoDB, etc.)
- ✅ Return `Mono<ResponseEntity<T>>` or `Flux<T>` from controllers

If you're using **Spring MVC** (servlet-based):
- ✅ Use `MultipartFile`
- ✅ Set `reactive=false` in OpenAPI generator
- ✅ Use blocking repository implementations (JPA, JDBC, etc.)
- ✅ Return `ResponseEntity<T>` from controllers

**Current Project Status**: Your `pom.xml` shows both `spring-boot-starter-webflux` AND `spring-boot-starter-web`, which is unusual. Typically, you should use one or the other.

### Performance Considerations

- **FilePart** with WebFlux: Better for handling large files and high concurrency
- **MultipartFile** with MVC: Simpler programming model, adequate for most use cases

### Security Considerations

1. **File Size Limits**: Configure max file size in `application.properties`:
   ```properties
   spring.servlet.multipart.max-file-size=10MB
   spring.servlet.multipart.max-request-size=10MB
   ```

2. **Content Type Validation**: Always validate file types server-side
3. **Virus Scanning**: As noted in the spec, implement virus scanning before accepting files

---

## References

- [OpenAPI 3.0 Specification - Encoding Object](https://swagger.io/specification/#encoding-object)
- [Spring WebFlux File Upload](https://docs.spring.io/spring-framework/reference/web/webflux/reactive-spring.html#webflux-multipart)
- [OpenAPI Generator Configuration Options](https://openapi-generator.tech/docs/generators/spring)
- [RFC 7578 - Multipart Form Data](https://tools.ietf.org/html/rfc7578)

---

## Troubleshooting

### Issue: Still generating MultipartFile after fixes
**Solution**:
- Clear Maven cache: `mvn clean`
- Delete generated sources manually
- Upgrade OpenAPI Generator to 7.x
- Consider using custom mustache templates

### Issue: Metadata not deserializing properly
**Solution**:
- Verify the `encoding.metadata.contentType` is set to `application/json`
- Check client is sending proper JSON in the metadata part
- Enable debug logging: `logging.level.org.springframework.web=DEBUG`

### Issue: FilePart vs MultipartFile compatibility
**Solution**:
- Ensure consistent use of either WebFlux or MVC, not both
- Remove conflicting dependencies from `pom.xml`
- Use appropriate reactive or blocking data access libraries

---

## Version History

| Version | Date       | Changes                                        |
|---------|------------|------------------------------------------------|
| 1.0     | 2025-11-28 | Initial documentation of issues and solutions  |

---

## Contact

For questions or issues related to this fix, please contact the API development team.
