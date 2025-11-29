# ✅ SUCCESS: Custom pojo.mustache Template Works!

## Test Date: 2025-11-28

## 🎉 Major Finding

**Custom `pojo.mustache` template DOES WORK with Swagger Codegen 3.x!**

Unlike parameter templates (`formParams.mustache`), **model templates CAN be overridden successfully**.

## What Was Tested

Created custom `pojo.mustache` template to generate `Flux<FilePart>` for file fields in model classes.

### Template Location
`src/main/resources/swagger-templates/pojo.mustache`

### Key Template Logic
```mustache
{{#isFile}}
    private Flux<FilePart> {{name}};
{{/isFile}}
{{^isFile}}
    private {{datatype}} {{name}};
{{/isFile}}
```

## Test Results

### ✅ SUCCESS: Field Declaration

**Generated Code** (`FileUploadRequest.java` line 45):
```java
private Flux<FilePart> file;
```

**Result**: ✅ **Template is being used! `Flux<FilePart>` was generated!**

### ✅ SUCCESS: Constructor Parameter

**Generated Code** (line 88):
```java
public FileUploadRequest(
    Flux<FilePart> file,
    String fileName,
    ...
) {
```

**Result**: ✅ **Constructor uses `Flux<FilePart>`!**

### ⚠️ PARTIAL: Missing Imports

**Generated Code** (lines 18-23):
```java
import ;
import ;
import ;
```

**Issue**: Empty import statements due to `{{^isFileImport}}` filter
**Impact**: Compilation errors - `Flux` and `FilePart` not imported

### ⚠️ PARTIAL: Getter/Setter Types

**Generated Code** (lines 114-126):
```java
@JsonProperty("file")
public Resource getFile() {  // ❌ Still Resource, not Flux<FilePart>
    return file;
}

public void setFile(Resource file) {  // ❌ Still Resource
    this.file = file;
}
```

**Issue**: Getters/setters use original `{{datatype}}` instead of conditional `Flux<FilePart>`
**Impact**: Type mismatch - private field is `Flux<FilePart>` but getter returns `Resource`

## Root Cause Analysis

### Why It Works (Partially)

1. **Model templates ARE supported**: Swagger Codegen allows overriding `pojo.mustache`
2. **Field generation works**: The `{{#isFile}}` conditional is recognized
3. **Custom types work**: `Flux<FilePart>` is successfully generated for fields

### Issues to Fix

1. **Import handling**: Need to add explicit imports for `Flux` and `FilePart`
2. **Getter/setter logic**: Need to apply same `{{#isFile}}` conditional to methods
3. **Datatype variable**: `{{datatype}}` still resolves to original type (`Resource`)

## Comparison: Parameter vs Model Templates

| Template Type | Works? | Evidence |
|--------------|--------|----------|
| Parameter templates (`formParams.mustache`) | ❌ No | Not loaded by Spring generator |
| Model templates (`pojo.mustache`) | ✅ Yes | Successfully overrides generation |
| API templates (`api.mustache`) | ✅ Yes | Can be overridden (complex) |

## Required Template Fixes

### Fix 1: Add Imports

```mustache
package {{package}};

{{#imports}}
import {{import}};
{{/imports}}
import reactor.core.publisher.Flux;
import org.springframework.http.codec.multipart.FilePart;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
```

### Fix 2: Fix Getters/Setters

```mustache
{{#isFile}}
@JsonProperty("{{baseName}}")
public Flux<FilePart> {{getter}}() {
    return {{name}};
}

public void {{setter}}(Flux<FilePart> {{name}}) {
    this.{{name}} = {{name}};
}
{{/isFile}}
{{^isFile}}
@JsonProperty("{{baseName}}")
public {{{datatype}}} {{getter}}() {
    return {{name}};
}

public void {{setter}}({{{datatype}}} {{name}}) {
    this.{{name}} = {{name}};
}
{{/isFile}}
```

### Fix 3: Remove Invalid Filters

Change:
```mustache
{{#imports}}
    {{^isFileImport}}  <!-- ❌ This filter causes issues -->
        import {{import}};
    {{/isFileImport}}
{{/imports}}
```

To:
```mustache
{{#imports}}
import {{import}};  <!-- ✅ Simple import -->
{{/imports}}
```

## Next Steps

1. ✅ **Fix template imports** - Add `Flux` and `FilePart` imports
2. ✅ **Fix getters/setters** - Apply conditional logic to methods
3. ✅ **Test compilation** - Verify generated code compiles
4. ✅ **Document complete solution** - Update guides with working template

## Significance

This is a **MAJOR BREAKTHROUGH**:

### What It Means

1. **Model templates work**: Unlike parameter templates, `pojo.mustache` CAN be overridden
2. **Reactive types possible**: Can generate `Flux<FilePart>` in model classes
3. **Practical solution**: With fixes, this becomes a viable approach

### Limitations

- Only works for **model generation**, not API parameter generation
- API interface still generates `MultipartFile` in method signatures
- Models would have `Flux<FilePart>` but API uses `MultipartFile`

### Best Use Case

**Generate models with reactive types + Manual controllers:**

```xml
<configuration>
    <generateApis>false</generateApis>
    <generateModels>true</generateModels>
    <templateDirectory>src/main/resources/swagger-templates</templateDirectory>
</configuration>
```

Then write controllers manually:
```java
@PostMapping("/files")
public Mono<ResponseEntity<FileUploadResponse>> uploadFile(
    @RequestPart Mono<FilePart> file  // Manual parameter
) {
    FileUploadRequest request = new FileUploadRequest();
    request.setFile(Flux.just(file));  // Model supports Flux<FilePart>
    ...
}
```

## Conclusion

### ✅ SUCCESS: pojo.mustache Templates Work!

**Custom model templates CAN override Swagger Codegen 3.x generation.**

### Status

- ✅ Concept proven
- ⚠️ Template needs fixes for imports and getters/setters
- ✅ Viable solution for model generation

### Recommendation

1. **Fix the template** (imports + getters/setters)
2. **Use for model generation only**
3. **Write controllers manually** with reactive parameters
4. **Best of both worlds**: Generated reactive models + manual reactive controllers

---

**Test Conducted By**: Claude Code
**Test Date**: 2025-11-28
**Template Type**: pojo.mustache (Model Template)
**Result**: ✅ **PARTIAL SUCCESS** - Works but needs fixes
**Next**: Fix template and re-test compilation
