# Swagger Codegen vs OpenAPI Generator Comparison

## For Multipart File Upload with WebFlux

---

## Test Configuration

### OpenAPI Spec (Same for both):
```yaml
/documents:
  post:
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

documentUploadRequest:
  properties:
    content:
      type: string
      format: binary
    metadata:
      $ref: "#/components/schemas/Metadata"
```

---

## Swagger Codegen 3.0.52 Configuration

```xml
<plugin>
    <groupId>io.swagger.codegen.v3</groupId>
    <artifactId>swagger-codegen-maven-plugin</artifactId>
    <version>3.0.52</version>
    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
        <language>spring</language>
        <library>spring-boot</library>
        <modelPackage>io.swagger.model</modelPackage>
        <apiPackage>io.swagger.api</apiPackage>
        <configOptions>
            <reactive>true</reactive>
            <interfaceOnly>true</interfaceOnly>
        </configOptions>
    </configuration>
</plugin>
```

### Generated API Interface:

```java
public interface DocumentsApi {

    @RequestMapping(
        value = "/documents",
        produces = { "application/json" },
        consumes = { "multipart/form-data" },
        method = RequestMethod.POST
    )
    ResponseEntity<InlineResponse200> addDocument(
        @RequestHeader(value="X-version", required=true) Integer xVersion,
        @RequestHeader(value="X-correlation-id", required=true) String xCorrelationId,
        @RequestHeader(value="X-requestor-id", required=true) UUID xRequestorId,
        @RequestHeader(value="X-requestor-type", required=true) XRequestorType xRequestorType,
        @RequestPart(value="documentType", required=true) String documentType,
        @RequestPart(value="createdBy", required=true) UUID createdBy,
        @Valid @RequestPart(value="content", required=true) MultipartFile content,  // ❌ BLOCKING
        @RequestPart(value="metadata", required=true) Metadata metadata
    );
}
```

### Issues:
- ❌ Returns `ResponseEntity<T>` (blocking) instead of `Mono<ResponseEntity<T>>`
- ❌ Uses `MultipartFile` (blocking) instead of `FilePart` or `Flux<Part>`
- ❌ No reactive types even with `reactive=true`
- ❌ Does NOT respect reactive configuration for multipart

---

## OpenAPI Generator 7.10.0 Configuration

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.10.0</version>
    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
        <generatorName>spring</generatorName>
        <library>spring-boot</library>
        <modelPackage>io.swagger.model</modelPackage>
        <apiPackage>io.swagger.api</apiPackage>
        <configOptions>
            <reactive>true</reactive>
            <interfaceOnly>true</interfaceOnly>
            <useSpringBoot3>false</useSpringBoot3>
        </configOptions>
    </configuration>
</plugin>
```

### Generated API Interface:

```java
public interface DocumentsApi {

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/documents",
        produces = { "application/json" },
        consumes = { "multipart/form-data" }
    )
    default Mono<ResponseEntity<AddDocument200Response>> addDocument(
        @RequestHeader(value = "X-version", required = true) Integer xVersion,
        @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,
        @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,
        @RequestHeader(value = "X-requestor-type", required = true) XRequestorType xRequestorType,
        @RequestPart(value = "documentType", required = true) String documentType,
        @RequestPart(value = "createdBy", required = true) UUID createdBy,
        @RequestPart(value = "content", required = true) Flux<Part> content,  // ✅ REACTIVE
        @RequestPart(value = "metadata", required = true) List<@Valid MetadataNode> metadata,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        // Default implementation
    }
}
```

### Advantages:
- ✅ Returns `Mono<ResponseEntity<T>>` (reactive)
- ✅ Uses `Flux<Part>` (reactive multipart)
- ✅ Includes `ServerWebExchange` for WebFlux
- ✅ Respects reactive configuration
- ✅ Default method implementation

---

## Side-by-Side Comparison

| Feature | Swagger Codegen 3.0.52 | OpenAPI Generator 7.10.0 |
|---------|------------------------|--------------------------|
| **Return Type** | `ResponseEntity<T>` | `Mono<ResponseEntity<T>>` |
| **File Upload Type** | `MultipartFile` | `Flux<Part>` |
| **WebFlux Support** | ❌ No | ✅ Yes |
| **Reactive Config Honored** | ❌ No (for multipart) | ✅ Yes |
| **ServerWebExchange** | ❌ No | ✅ Yes |
| **Default Implementation** | ❌ No | ✅ Yes |
| **Active Development** | ❌ Minimal | ✅ Very Active |
| **Spring Boot 3 Support** | ❌ No | ✅ Yes |
| **Last Update** | 2023 | 2024 (monthly) |

---

## Can Swagger Codegen Be Fixed?

### ✅ What CAN Be Fixed:

1. **Encoding Specification**: Already works
   ```yaml
   encoding:
     metadata:
       contentType: application/json
   ```

2. **Custom Templates**: Modify Mustache templates to generate `FilePart`
   - **Effort**: High (requires template expertise)
   - **Maintenance**: High (breaks with updates)

3. **Post-Processing Scripts**: Replace `MultipartFile` with `FilePart` after generation
   - **Effort**: Medium (Maven plugin configuration)
   - **Maintenance**: Medium (fragile, text replacement)

### ❌ What CANNOT Be Fixed (Without Major Work):

1. **Reactive Return Types**: Swagger Codegen doesn't generate `Mono<ResponseEntity<T>>`
2. **ServerWebExchange**: Not included in generated code
3. **Flux vs MultipartFile**: Hardcoded to use `MultipartFile`
4. **WebFlux Integration**: Limited support

---

## Recommendation Matrix

### Use Swagger Codegen IF:

- ✅ Company mandate (no choice)
- ✅ Using Spring MVC (not WebFlux)
- ✅ Willing to manually override generated code
- ✅ Can invest time in custom templates

### Use OpenAPI Generator IF:

- ✅ Using Spring WebFlux (reactive)
- ✅ Need modern Spring Boot features
- ✅ Want active maintenance and updates
- ✅ Can get approval to change tools

### Manual Controller (Current Approach) IF:

- ✅ Company generates models only
- ✅ Want full control over implementation
- ✅ Need to ensure consistency across team
- ✅ Can maintain manual code easily

---

## Hybrid Approach (Best of Both Worlds)

**What You're Doing Now**: ✅ Recommended

1. **Generate Models Only** (with either tool)
   ```xml
   <generateApis>false</generateApis>
   <generateModels>true</generateModels>
   ```

2. **Write Controllers Manually**
   - Full control over reactive types
   - Can use `Mono<FilePart>` or `Flux<Part>`
   - Maintain company standards

3. **Benefits**:
   - ✅ Type-safe DTOs from spec
   - ✅ Full control over reactive implementation
   - ✅ No need for custom templates
   - ✅ Works with both Swagger Codegen and OpenAPI Generator

---

## Proof: Testing Swagger Codegen

If you want to test Swagger Codegen to see the difference:

### Step 1: Backup Current Config
```bash
cp pom.xml pom.xml.backup
```

### Step 2: Change to Swagger Codegen
```xml
<plugin>
    <groupId>io.swagger.codegen.v3</groupId>
    <artifactId>swagger-codegen-maven-plugin</artifactId>
    <version>3.0.52</version>
    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
        <language>spring</language>
        <library>spring-boot</library>
        <output>${project.build.directory}/generated-sources-swagger</output>
        <apiPackage>io.swagger.api</apiPackage>
        <modelPackage>io.swagger.model</modelPackage>
        <configOptions>
            <reactive>true</reactive>
            <interfaceOnly>true</interfaceOnly>
        </configOptions>
    </configuration>
</plugin>
```

### Step 3: Generate
```bash
mvn clean generate-sources
```

### Step 4: Compare
```bash
# Check what was generated
cat target/generated-sources-swagger/src/main/java/io/swagger/api/DocumentsApi.java | grep "addDocument"
```

You'll see it generates `MultipartFile` regardless of `reactive=true`.

---

## Conclusion

### Can Swagger Codegen Fix the Issues?

**Short Answer**: Partially, but with significant effort.

**What Works**:
- ✅ Encoding specification (OpenAPI 3.0 standard)
- ✅ Model class generation
- ✅ Basic validation annotations

**What Doesn't Work**:
- ❌ Reactive multipart handling (always generates `MultipartFile`)
- ❌ WebFlux integration (no `Mono`, `Flux`, or `ServerWebExchange`)
- ❌ Modern Spring Boot patterns

**Best Solution** (Your Current Approach):
- Generate models only (works with both tools)
- Write controllers manually
- Full control over reactive implementation
- Follows company standards

---

## Final Recommendation

**Stick with your current approach**:
1. Use OpenAPI Generator 7.x for model generation (better support)
2. Set `generateApis=false` and `generateModels=true`
3. Write controllers manually with reactive types
4. Maintain full control and company standards

This gives you the best of both worlds without fighting with code generation limitations!
