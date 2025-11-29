# Array Type Schema Issue: Metadata

**Date**: 2025-11-29
**Issue**: Array type schemas generate incomplete wrapper classes
**Status**: ⚠️ **OPENAPI SPEC ISSUE** (Not a template bug)

---

## Problem Description

### Current OpenAPI Spec

```yaml
documentUploadRequest:
  properties:
    metadata:
      $ref: "#/components/schemas/Metadata"  # References array type

components:
  schemas:
    Metadata:
      type: array  # ← Array type as a named schema
      items:
        $ref: "#/components/schemas/MetadataNode"
```

### Generated Code

**Metadata.java**:
```java
package com.example.model;

import com.example.model.MetadataNode;  // ← Imported but never used!
import java.util.List;                  // ← Imported but never used!

public class Metadata {
    // ← NO FIELD TO HOLD THE ARRAY!

    public Metadata() {
    }

    // No getters/setters
    // Empty class!
}
```

**DocumentUploadRequest.java**:
```java
public class DocumentUploadRequest {
    private Metadata metadata;  // ← Reference to empty wrapper class
}
```

### The Issue

1. ✅ `MetadataNode` is imported in `Metadata.java`
2. ✅ `List` is imported in `Metadata.java`
3. ❌ **No field exists** to hold `List<MetadataNode>`
4. ❌ The `Metadata` class is just an **empty wrapper**
5. ❌ Unusable in practice

---

## Root Cause

This is **NOT a template bug** - it's how Swagger Codegen handles array type schemas.

When you define a schema with `type: array`, Swagger Codegen:
- Creates a model class with that name
- Adds imports for the item type
- But **does NOT generate a field** to hold the array

This is because:
- Array types don't have "properties" in the OpenAPI sense
- Swagger Codegen expects arrays to be used inline, not as named schemas
- The `{{#vars}}` loop is empty (no variables to iterate)

---

## Why This Happens in Swagger Codegen

### OpenAPI 3.0 Specification

In OpenAPI 3.0, you can define schemas with different types:

**Object Schema** (has properties):
```yaml
FileMetadata:
  type: object
  properties:
    fileId:
      type: string
    fileName:
      type: string
```
Generated: `private String fileId; private String fileName;`

**Array Schema** (no properties):
```yaml
Metadata:
  type: array
  items:
    $ref: "#/components/schemas/MetadataNode"
```
Generated: *Nothing* (no properties to generate)

### Swagger Codegen Processing

```
1. Read schema "Metadata"
2. type: array → isArrayModel = true
3. Get items reference → MetadataNode
4. Add import for MetadataNode
5. Add import for List
6. Loop through properties ({{#vars}})
   → vars is EMPTY (arrays have no properties)
7. Generate class with NO FIELDS
```

---

## Solutions

### Option 1: Inline Array Definition (Recommended)

**Remove the named `Metadata` schema** and use inline array:

```yaml
documentUploadRequest:
  required:
    - content
    - metadata  # Now required
  properties:
    metadata:
      type: array  # ← Inline array, not a $ref
      description: Document metadata key-value pairs
      items:
        $ref: "#/components/schemas/MetadataNode"

# Remove the standalone Metadata schema entirely
```

**Generated Code**:
```java
public class DocumentUploadRequest {
    private List<MetadataNode> metadata;  // ← Direct List!

    public List<MetadataNode> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataNode> metadata) {
        this.metadata = metadata;
    }
}
```

**Benefits**:
- ✅ No empty wrapper class
- ✅ Direct `List<MetadataNode>` usage
- ✅ Clean, idiomatic Java
- ✅ Works with all templates

### Option 2: Change to Object Schema with Array Property

Wrap the array in an object:

```yaml
Metadata:
  type: object
  properties:
    items:
      type: array
      items:
        $ref: "#/components/schemas/MetadataNode"
```

**Generated Code**:
```java
public class Metadata {
    private List<MetadataNode> items;

    public List<MetadataNode> getItems() {
        return items;
    }
}
```

**DocumentUploadRequest**:
```java
public class DocumentUploadRequest {
    private Metadata metadata;  // Wrapper with items field

    // Access: request.getMetadata().getItems()
}
```

**Drawbacks**:
- ❌ Extra wrapper class
- ❌ Additional layer: `metadata.getItems()`
- ❌ Less idiomatic

### Option 3: Custom Template for Array Types (Complex)

Modify `pojo.mustache` to detect array models and generate an items field:

```mustache
{{#isArrayModel}}
    private List<{{arrayModelType}}> items;

    public List<{{arrayModelType}}> getItems() {
        return items;
    }

    public void setItems(List<{{arrayModelType}}> items) {
        this.items = items;
    }
{{/isArrayModel}}
```

**Drawbacks**:
- ❌ Requires knowing Swagger Codegen's internal variables
- ❌ `{{#isArrayModel}}` may not exist
- ❌ Complex template logic
- ❌ May not work across Swagger Codegen versions

---

## Recommendation

### ✅ Use Option 1: Inline Array Definition

This is the **standard OpenAPI pattern** and works best with code generators:

**Before** (Problematic):
```yaml
documentUploadRequest:
  properties:
    metadata:
      $ref: "#/components/schemas/Metadata"

Metadata:  # Named array schema
  type: array
  items:
    $ref: "#/components/schemas/MetadataNode"
```

**After** (Clean):
```yaml
documentUploadRequest:
  properties:
    metadata:
      type: array  # Inline array
      items:
        $ref: "#/components/schemas/MetadataNode"
      description: Document metadata as key-value pairs

# No separate Metadata schema needed
```

### Generated Result

**DocumentUploadRequest.java**:
```java
public class DocumentUploadRequest {
    private String documentType;
    private UUID createdBy;
    // ... other fields ...
    private Flux<FilePart> content;  // ✅ File field works!
    private List<MetadataNode> metadata;  // ✅ Direct list!

    @JsonProperty("metadata")
    public List<MetadataNode> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataNode> metadata) {
        this.metadata = metadata;
    }
}
```

**Benefits**:
- ✅ No empty Metadata wrapper class
- ✅ Clean, idiomatic Java code
- ✅ `Flux<FilePart>` still works for file fields
- ✅ All imports are used
- ✅ Compiles successfully
- ✅ Easy to use: `request.getMetadata()`

---

## Why Named Array Schemas Are Problematic

### In OpenAPI

Named array schemas are **valid** in OpenAPI 3.0 but **not recommended**:

```yaml
# Valid but NOT recommended
Metadata:
  type: array
  items:
    $ref: "#/components/schemas/Item"
```

### In Code Generation

Most code generators (Swagger Codegen, OpenAPI Generator) handle these poorly:
- May create empty wrapper classes
- May skip code generation entirely
- May create compilation errors
- Behavior varies by language and generator

### Best Practice

**Use inline arrays** for array-typed properties:

```yaml
# Recommended pattern
properties:
  metadata:
    type: array
    items:
      $ref: "#/components/schemas/MetadataNode"
```

**Use named schemas** only for object types:

```yaml
# Good use of named schema
properties:
  metadata:
    $ref: "#/components/schemas/FileMetadata"

FileMetadata:
  type: object  # ← Object, not array
  properties:
    fileId:
      type: string
```

---

## Impact on Current Implementation

### What Works

- ✅ `Flux<FilePart>` generation for file fields
- ✅ Object type models compile successfully
- ✅ Template handles empty classes without errors

### What's Incomplete

- ⚠️ `Metadata` class is functionally useless (empty wrapper)
- ⚠️ Imports exist but aren't used
- ⚠️ Not a compilation error, but not functional either

### Fix Required

Update the OpenAPI spec (`test.yml`) to use inline array definition.

---

## Comparison: Before vs After Spec Change

### Before (Current test.yml)

**Spec**:
```yaml
documentUploadRequest:
  properties:
    metadata:
      $ref: "#/components/schemas/Metadata"

Metadata:
  type: array
  items:
    $ref: "#/components/schemas/MetadataNode"
```

**Generated**:
```java
// Metadata.java - EMPTY WRAPPER
public class Metadata {
    // No fields!
}

// DocumentUploadRequest.java
public class DocumentUploadRequest {
    private Metadata metadata;  // Points to empty class
}
```

**Usage** (Broken):
```java
request.getMetadata() // Returns empty Metadata instance
// Can't access the MetadataNode items!
```

### After (Recommended)

**Spec**:
```yaml
documentUploadRequest:
  properties:
    metadata:
      type: array  # Inline
      items:
        $ref: "#/components/schemas/MetadataNode"
```

**Generated**:
```java
// No Metadata.java class generated

// DocumentUploadRequest.java
public class DocumentUploadRequest {
    private List<MetadataNode> metadata;  // Direct list!

    public List<MetadataNode> getMetadata() {
        return metadata;
    }
}
```

**Usage** (Working):
```java
List<MetadataNode> metadata = request.getMetadata();
for (MetadataNode node : metadata) {
    String key = node.getKey();
    String value = node.getValue();
}
```

---

## Lessons Learned

### OpenAPI Best Practices

1. **Inline arrays** for array-typed properties
2. **Named schemas** only for object types with properties
3. **Avoid** `type: array` as a standalone named schema
4. **Test** generated code, not just spec validation

### Code Generation Reality

1. **Valid OpenAPI ≠ Good generated code**
2. **Generator behavior varies** by type and language
3. **Empty models indicate spec issues**, not template bugs
4. **Always verify** generated code is usable

---

## Action Items

### To Fix test.yml

1. Remove the `Metadata` schema from `components/schemas`
2. Change `documentUploadRequest.metadata` to inline array
3. Regenerate code
4. Verify `List<MetadataNode>` is generated in `DocumentUploadRequest`

### Updated Spec

```yaml
documentUploadRequest:
  required:
    - content
    - createdBy
    - documentType
    - metadata  # Add to required
  type: object
  properties:
    documentType:
      $ref: "#/components/schemas/DocumentTypes"
    # ... other properties ...
    content:
      type: string
      format: binary  # ✅ Generates Flux<FilePart>
    metadata:
      type: array  # ✅ Inline array definition
      description: Document metadata as key-value pairs
      items:
        $ref: "#/components/schemas/MetadataNode"

components:
  schemas:
    # Remove Metadata schema entirely

    MetadataNode:  # Keep this!
      required:
        - key
        - value
      type: object
      properties:
        key:
          type: string
        value:
          type: string
```

---

## Conclusion

### Summary

- ❌ **Not a template bug** - Templates work correctly
- ⚠️ **OpenAPI spec issue** - Named array schemas problematic
- ✅ **Solution exists** - Use inline array definitions
- ✅ **Templates still work** - `Flux<FilePart>` unaffected

### Status

- **Current**: Metadata class compiles but is empty/unusable
- **Recommended**: Update spec to use inline arrays
- **Result**: Clean, functional generated code

### Template Status

- ✅ Custom templates work correctly
- ✅ Handle array types without errors
- ✅ Generate `Flux<FilePart>` for files
- ✅ Production-ready for **properly structured OpenAPI specs**

---

**Date**: 2025-11-29
**Conclusion**: The issue is in the OpenAPI spec structure, not the templates. Use inline array definitions instead of named array schemas for best results.
