# Custom Swagger Codegen Mustache Templates

## Purpose

These custom Mustache templates fix the issue where Swagger Codegen 3.x generates `MultipartFile` instead of `FilePart` for reactive Spring WebFlux applications.

## Templates Included

1. **formParams.mustache** - Handles form parameters (including file uploads)
   - Generates `Mono<FilePart>` for single file (reactive)
   - Generates `Flux<FilePart>` for multiple files (reactive)
   - Generates `MultipartFile` for non-reactive mode

2. **queryParams.mustache** - Handles query parameters

3. **pathParams.mustache** - Handles path variables

4. **headerParams.mustache** - Handles header parameters

5. **bodyParams.mustache** - Handles request body parameters

## Key Fix: formParams.mustache

### The Problem:
Default Swagger Codegen template always generates:
```java
@RequestPart MultipartFile content
```

### Our Fix:
Custom template generates based on reactive mode:
```java
// When reactive=true
@RequestPart Mono<FilePart> content

// When reactive=false
@RequestPart MultipartFile content
```

## Usage

### In pom.xml:
```xml
<plugin>
    <groupId>io.swagger.codegen.v3</groupId>
    <artifactId>swagger-codegen-maven-plugin</artifactId>
    <version>3.0.52</version>
    <configuration>
        <templateDirectory>${project.basedir}/src/main/resources/swagger-templates</templateDirectory>
        <configOptions>
            <reactive>true</reactive>
        </configOptions>
    </configuration>
</plugin>
```

### Generate Code:
```bash
mvn clean generate-sources
```

## Expected Output

### Before (Default Template):
```java
ResponseEntity<InlineResponse200> addDocument(
    @RequestPart MultipartFile content,  // ❌ Blocking
    ...
)
```

### After (Custom Template):
```java
Mono<ResponseEntity<InlineResponse200>> addDocument(
    @RequestPart Mono<FilePart> content,  // ✅ Reactive
    ...
)
```

## Template Logic

The key logic in `formParams.mustache`:

```mustache
{{#isFile}}
  {{#reactive}}
    Mono<FilePart> {{paramName}}  <!-- Reactive mode -->
  {{/reactive}}
  {{^reactive}}
    MultipartFile {{paramName}}   <!-- Non-reactive mode -->
  {{/reactive}}
{{/isFile}}
```

## Maintenance

### When to Update:
- Swagger Codegen version changes
- New parameter types needed
- Spring framework updates

### Testing:
```bash
# Regenerate
mvn clean generate-sources

# Check generated file
cat target/generated-sources/src/main/java/io/swagger/api/DocumentsApi.java | grep "FilePart"
```

Should output:
```
Mono<FilePart> content
```

## Version History

- v1.0 (2025-11-28): Initial templates for FilePart support

## Author

Generated for fixing Spring WebFlux reactive file upload with Swagger Codegen 3.x
