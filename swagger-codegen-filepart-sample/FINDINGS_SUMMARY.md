# Test Summary: Custom Mustache Templates with Swagger Codegen 3.x

## Quick Answer

❌ **Custom Mustache parameter templates DO NOT WORK with Swagger Codegen 3.x Spring generator.**

## What We Tested

Attempted to use custom Mustache templates to make Swagger Codegen 3.0.52 generate `Mono<FilePart>` instead of `MultipartFile` for reactive Spring WebFlux file uploads.

## Setup

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

Created custom templates:
- `formParams.mustache` - Should generate `Mono<FilePart>`
- `queryParams.mustache`
- `pathParams.mustache`
- `headerParams.mustache`
- `bodyParams.mustache`

## Expected Result

```java
public interface FilesApi {
    ResponseEntity<FileUploadResponse> uploadFile(
        @RequestPart Mono<FilePart> file,  // ✅ Expected
        ...
    );
}
```

## Actual Result

```java
public interface FilesApi {
    default ResponseEntity<FileUploadResponse> uploadFile(
        @RequestPart MultipartFile file,  // ❌ Still MultipartFile
        ...
    );
}
```

## Why It Failed

### Root Cause

Swagger Codegen 3.x Spring generator:

1. **Uses Handlebars templates** from `/handlebars/JavaSpring/api.mustache`
2. **Does NOT load custom parameter templates** like `formParams.mustache`
3. **Parameter generation is embedded** within `api.mustache`, not separate files
4. **Template override only works for** `api.mustache`, `model.mustache`, `pojo.mustache` - not parameter snippets

### Evidence

From `mvn -X` (debug mode):

```
[DEBUG] (f) templateDirectory = .../src/main/resources/swagger-templates
[DEBUG] About to parse: /handlebars/JavaSpring/api.mustache
[DEBUG] About to parse: /handlebars/JavaSpring/model.mustache
```

Custom `formParams.mustache` is **never loaded or parsed**.

## What Works Instead

### Option 1: Manual Controller ✅ (This Project)

```java
@RestController
public class FilesController {
    @PostMapping("/files")
    public Mono<ResponseEntity<FileUploadResponse>> uploadFile(
        @RequestPart Mono<FilePart> file,  // ✅ Works
        ...
    ) {
        return file.flatMap(filePart -> {
            // Reactive processing
        });
    }
}
```

**Configuration**:
```xml
<generateApis>false</generateApis>
<generateModels>true</generateModels>
```

### Option 2: OpenAPI Generator ✅ (Recommended)

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.10.0</version>
    <configuration>
        <configOptions>
            <reactive>true</reactive>
        </configOptions>
    </configuration>
</plugin>
```

**Result**: Automatically generates `Mono<FilePart>` without custom templates.

### Option 3: Override Entire api.mustache ⚠️ (Complex)

Copy Swagger Codegen's `/handlebars/JavaSpring/api.mustache` and modify entire template.

**Pros**: Can customize everything
**Cons**: Very complex, breaks with updates, high maintenance

## Comparison Matrix

| Approach | FilePart Generated | Complexity | Maintenance | Recommended |
|----------|-------------------|------------|-------------|-------------|
| Custom parameter templates | ❌ No | Low | Low | ❌ Doesn't work |
| Manual controllers | ✅ Yes | Low | Low | ✅ Yes |
| OpenAPI Generator | ✅ Yes | Low | Low | ✅ Yes (best) |
| Override api.mustache | ✅ Yes | High | High | ⚠️ Only if necessary |

## Recommendations

### For Immediate Use
**Generate models only + Manual controllers**
- Configure: `<generateApis>false</generateApis>`
- Write reactive controllers manually
- Use generated model classes
- Full control over reactive types

### For Long Term
**Migrate to OpenAPI Generator**
- Drop-in replacement for Swagger Codegen
- Better reactive support
- Actively maintained
- Generates correct reactive types automatically

### NOT Recommended
- ❌ Custom parameter templates (doesn't work)
- ❌ Staying with Swagger Codegen for reactive APIs
- ❌ Complex template overrides

## Files in This Project

| File | Purpose | Status |
|------|---------|--------|
| `TEST_RESULTS.md` | Detailed test findings | ✅ Documented |
| `README.md` | Project overview | ✅ Updated with results |
| `src/main/resources/swagger-templates/*.mustache` | Custom templates | ❌ Not working |
| `src/main/java/com/example/controller/FilesController.java` | Manual controller | ✅ Working solution |
| `target/generated-sources/.../FilesApi.java` | Generated interface | ⚠️ Uses MultipartFile |
| `target/generated-sources/.../model/*.java` | Generated models | ✅ Working |

## Conclusion

**The custom Mustache template approach for parameter generation DOES NOT WORK with Swagger Codegen 3.x.**

Use one of the working alternatives:
1. **Manual controllers** (demonstrated in this project)
2. **OpenAPI Generator** (recommended for new projects)

This project serves as proof that the approach fails and provides a working manual implementation as reference.

---

**Test Date**: 2025-11-28
**Swagger Codegen Version**: 3.0.52
**Final Verdict**: ❌ Custom parameter templates approach FAILED
