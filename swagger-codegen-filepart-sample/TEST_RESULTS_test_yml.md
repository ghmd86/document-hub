# Test Results: test.yml with Custom Mustache Templates

**Date**: 2025-11-29
**Swagger Codegen Version**: 3.0.52
**Input Spec**: `src/main/resources/test.yml`
**Custom Templates**: model.mustache + pojo.mustache

---

## Executive Summary

### ✅ SUCCESS: Flux<FilePart> Generation
**The custom templates successfully generated `Flux<FilePart>` for the `content` field!**

### ⚠️ PARTIAL FAILURE: Array Type Handling
**The templates have a bug when handling array-type schemas (`Metadata`).**

---

## Test Spec Analysis

### File Upload Field
```yaml
documentUploadRequest:
  properties:
    content:
      type: string
      format: binary  # ← Triggers {{#isFile}} = true
      description: "The actual body or data of the document"
```

###Array Type Schema
```yaml
Metadata:
  type: array  # ← PROBLEMATIC: Our template assumes object type
  items:
    $ref: "#/components/schemas/MetadataNode"
```

---

## Results

### ✅ SUCCESS: DocumentUploadRequest with Flux<FilePart>

**Generated File**: `target/generated-sources/src/main/java/com/example/model/DocumentUploadRequest.java`

#### Key Verification Points

**1. Field Declaration** (Line 71):
```java
private Flux<FilePart> content;
```
✅ **SUCCESS**: Field uses `Flux<FilePart>` instead of `Resource`

**2. Constructor Parameter** (Line 94):
```java
public DocumentUploadRequest(
    String documentType,
    UUID createdBy,
    // ... other parameters ...
    Flux<FilePart> content,  // ✅ Reactive type!
    Metadata metadata
) {
```
✅ **SUCCESS**: Constructor parameter is `Flux<FilePart>`

**3. Getter Method** (Line 297):
```java
@JsonProperty("content")
public Flux<FilePart> getContent() {
    return content;
}
```
✅ **SUCCESS**: Getter returns `Flux<FilePart>`

**4. Setter Method** (Line 301):
```java
public void setContent(Flux<FilePart> content) {
    this.content = content;
}
```
✅ **SUCCESS**: Setter accepts `Flux<FilePart>`

**5. Fluent Setter** (Line 305):
```java
public DocumentUploadRequest content(Flux<FilePart> content) {
    this.content = content;
    return this;
}
```
✅ **SUCCESS**: Fluent setter uses `Flux<FilePart>`

**6. Reactive Imports** (Lines 9-10):
```java
import reactor.core.publisher.Flux;
import org.springframework.http.codec.multipart.FilePart;
```
✅ **SUCCESS**: Conditional imports added only to models with file fields

#### All Flux<FilePart> Occurrences
```
Line 71:  private Flux<FilePart> content;
Line 94:  Flux<FilePart> content,
Line 297: public Flux<FilePart> getContent() {
Line 301: public void setContent(Flux<FilePart> content) {
Line 305: public DocumentUploadRequest content(Flux<FilePart> content) {
```

**Total**: 5 occurrences - all correct! ✅

---

### ❌ FAILURE: Metadata Model (Array Type)

**Generated File**: `target/generated-sources/src/main/java/com/example/model/Metadata.java`

#### Compilation Errors

**Error 1: Duplicate Constructor**
```
[ERROR] Metadata.java:[19,12] constructor Metadata() is already defined
```

**Generated Code** (Lines 16-21):
```java
public Metadata() {  // First constructor
}

public Metadata(     // Second constructor - DUPLICATE!
) {
}
```

**Root Cause**: Template generates both no-arg and parameterized constructors, but when there are no fields (array type has no properties), both constructors have the same signature.

**Error 2: Missing Return Value in equals()**
```
[ERROR] Metadata.java:[30,9] incompatible types: missing return value
```

**Generated Code** (Lines 26-31):
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Metadata)) return false;
    Metadata other = (Metadata) o;
    return ;  // ← EMPTY RETURN STATEMENT!
}
```

**Root Cause**: Template tries to generate `Objects.equals()` comparisons for all variables, but when there are no variables (array type), it generates an empty return statement.

---

## Generated Files

### ✅ Models That Compiled Successfully

1. **DocumentUploadRequest.java** - ✅ SUCCESS
   - Contains `Flux<FilePart> content` field
   - All methods use reactive types
   - Compiles without errors

2. **MetadataNode.java** - ✅ SUCCESS
   - Object type with properties
   - No file fields
   - Compiles without errors

3. **ResourceAddress.java** - ✅ SUCCESS
   - Nested object type
   - No file fields
   - Compiles without errors

### ❌ Models That Failed Compilation

1. **Metadata.java** - ❌ FAILURE
   - Array type (not object type)
   - Duplicate constructor
   - Missing return value in equals()
   - 2 compilation errors

---

##Template Bug Analysis

### Problem: Array Types Not Supported

The custom `pojo.mustache` template assumes all models are **object types** with properties (`{{#vars}}`). When Swagger Codegen processes an array type schema:

```yaml
Metadata:
  type: array
  items:
    $ref: "#/components/schemas/MetadataNode"
```

It generates a model class with **zero variables** (`{{#vars}}` is empty), causing:

1. **Empty constructor parameters** → Duplicate constructors
2. **Empty equals comparison** → Invalid return statement
3. **Empty hashCode** → Actually valid `Objects.hash()` but inconsistent

### Why This Happens

**OpenAPI Spec Definition**:
- `Metadata` is defined as `type: array`
- Arrays in OpenAPI should typically be represented as `List<T>` in generated code
- Swagger Codegen creates a wrapper class instead of using `List<MetadataNode>` directly

**Template Expectation**:
- Our `pojo.mustache` template was designed for object schemas with properties
- We didn't account for array or primitive type schemas

### Scope of the Bug

This bug affects:
- ❌ Array type schemas (`type: array`)
- ❌ Primitive type schemas (`type: string`, `type: integer` as models)
- ✅ Object type schemas work perfectly
- ✅ File fields in objects work perfectly

---

## Recommendations

### Option 1: Fix the Template (Complex)

Modify `pojo.mustache` to handle edge cases:

```mustache
// Constructors
public {{classname}}() {
}

{{#models}}
{{#model}}
{{#hasVars}}  <!-- Only generate parameterized constructor if vars exist -->
public {{classname}}(
{{#vars}}
    // ... parameters
{{/vars}}
) {
    // ... assignments
}
{{/hasVars}}
{{/model}}
{{/models}}

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof {{classname}})) return false;
    {{classname}} other = ({{classname}}) o;
{{#models}}{{#model}}
{{#hasVars}}
    return {{#vars}}Objects.equals(this.{{name}}, other.{{name}}){{#hasMore}} &&
           {{/hasMore}}{{/vars}};
{{/hasVars}}
{{^hasVars}}
    return true;  // No fields to compare
{{/hasVars}}
{{/model}}{{/models}}
}
```

**Pros**: Would handle all schema types
**Cons**: Complex, may introduce other edge cases

### Option 2: Avoid Array Types in OpenAPI Spec (Recommended)

Modify the OpenAPI spec to use `List<T>` directly in properties instead of creating array type schemas:

**Current (Problematic)**:
```yaml
documentUploadRequest:
  properties:
    metadata:
      $ref: "#/components/schemas/Metadata"  # References array type

Metadata:
  type: array
  items:
    $ref: "#/components/schemas/MetadataNode"
```

**Better Approach**:
```yaml
documentUploadRequest:
  properties:
    metadata:
      type: array  # Inline array definition
      items:
        $ref: "#/components/schemas/MetadataNode"

# No standalone Metadata schema needed
```

This generates:
```java
public class DocumentUploadRequest {
    private List<MetadataNode> metadata;  // Direct List, no wrapper
}
```

**Pros**: Cleaner, no wrapper class, works with current templates
**Cons**: Requires spec modification

### Option 3: Use Default Template for Array Types

Configure Swagger Codegen to use custom templates only for specific models:

**Not easily supported** - Swagger Codegen doesn't have per-model template selection.

---

## Comparison: Before vs After

### Before (Default Swagger Codegen)

```java
public class DocumentUploadRequest {
    private Resource content;  // ❌ Blocking type

    public Resource getContent() {
        return content;
    }

    public void setContent(Resource content) {
        this.content = content;
    }
}
```

### After (Custom Templates)

```java
public class DocumentUploadRequest {
    private Flux<FilePart> content;  // ✅ Reactive type

    public Flux<FilePart> getContent() {
        return content;
    }

    public void setContent(Flux<FilePart> content) {
        this.content = content;
    }
}
```

---

## Conclusion

### What Works ✅

1. **File field generation**: `Flux<FilePart>` successfully generated for `format: binary` fields
2. **Conditional imports**: Reactive imports only added when needed
3. **Object type models**: All object schemas compile successfully
4. **Non-file models**: Models without file fields work correctly (no unnecessary imports)

### What Doesn't Work ❌

1. **Array type schemas**: Generate invalid code with duplicate constructors and empty returns
2. **Compilation**: Cannot compile with array type models in the spec

### Overall Assessment

**Primary Goal**: ✅ **ACHIEVED**
- Custom templates successfully generate `Flux<FilePart>` for file upload fields
- Works perfectly for object type schemas

**Secondary Issue**: ⚠️ **Template Bug Discovered**
- Array types not supported
- Workaround: Use inline array definitions instead of array type schemas

---

## Next Steps

### Immediate

1. ✅ Document the array type bug
2. ✅ Add warning to LESSONS_LEARNED.md
3. ⏳ Decide on fix strategy (Option 1 or Option 2)

### Future

1. Consider migrating to OpenAPI Generator for better array type support
2. Or fix pojo.mustache to handle array types
3. Add integration tests for various schema types

---

## Test Environment

| Component | Value |
|-----------|-------|
| **Swagger Codegen** | 3.0.52 |
| **Java Version** | 11 |
| **Spring Boot** | 2.7.18 |
| **Input Spec** | test.yml |
| **Generator** | spring |
| **Library** | spring-boot |
| **Reactive Mode** | true |
| **Custom Templates** | model.mustache + pojo.mustache |

---

## Files Generated

```
target/generated-sources/src/main/java/com/example/
├── api/
│   ├── DocumentsApi.java        (generated, not compiled)
│   └── ResourceApi.java          (generated, not compiled)
└── model/
    ├── DocumentUploadRequest.java  ✅ SUCCESS (with Flux<FilePart>)
    ├── Metadata.java               ❌ FAILURE (array type bug)
    ├── MetadataNode.java           ✅ SUCCESS (object type)
    └── ResourceAddress.java        ✅ SUCCESS (nested object)
```

---

**Test Result**: ✅ **PRIMARY GOAL ACHIEVED** (Flux<FilePart> generation works!)
**Compilation**: ❌ **FAILED** (due to array type bug in template)
**Recommendation**: Modify spec to avoid array type schemas OR fix pojo.mustache template
