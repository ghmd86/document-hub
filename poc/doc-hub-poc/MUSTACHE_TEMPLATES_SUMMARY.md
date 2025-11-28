# Custom Mustache Templates for Swagger Codegen - Complete Guide

## 🎯 Goal

Fix Swagger Codegen 3.x to generate **`FilePart`** instead of **`MultipartFile`** for reactive Spring WebFlux applications.

---

## 📦 What Was Created

### Mustache Template Files (5):

```
src/main/resources/swagger-templates/
├── formParams.mustache      ⭐ THE KEY FIX
├── queryParams.mustache
├── pathParams.mustache
├── headerParams.mustache
├── bodyParams.mustache
└── README.md
```

### Documentation Files (4):

```
project-root/
├── CUSTOM_MUSTACHE_TEMPLATES_GUIDE.md      # Complete guide
├── MUSTACHE_TEMPLATES_SUMMARY.md           # This file
├── pom-swagger-codegen-with-templates.xml.example  # Example config
└── test-swagger-codegen-templates.sh       # Test script
```

---

## 🔑 The Key Fix: formParams.mustache

This template detects if you're using reactive mode and generates the appropriate type:

```mustache
{{#isFile}}
  {{#reactive}}
    Mono<FilePart> {{paramName}}     <!-- ✅ Reactive -->
  {{/reactive}}
  {{^reactive}}
    MultipartFile {{paramName}}      <!-- Non-reactive -->
  {{/reactive}}
{{/isFile}}
```

### Before (Default Swagger Codegen):
```java
@RequestPart MultipartFile content  // ❌ Always blocking
```

### After (Custom Template):
```java
@RequestPart Mono<FilePart> content  // ✅ Reactive
```

---

## 🚀 How to Use

### Step 1: Verify Templates Exist

```bash
ls src/main/resources/swagger-templates/
```

Should show:
```
bodyParams.mustache
formParams.mustache
headerParams.mustache
pathParams.mustache
queryParams.mustache
README.md
```

✅ **Already created for you!**

---

### Step 2: Update pom.xml

Add `<templateDirectory>` to your Swagger Codegen plugin configuration:

```xml
<plugin>
    <groupId>io.swagger.codegen.v3</groupId>
    <artifactId>swagger-codegen-maven-plugin</artifactId>
    <version>3.0.52</version>
    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
        <language>spring</language>
        <library>spring-boot</library>
        <apiPackage>io.swagger.api</apiPackage>
        <modelPackage>io.swagger.model</modelPackage>

        <!-- Point to custom templates -->
        <templateDirectory>${project.basedir}/src/main/resources/swagger-templates</templateDirectory>

        <configOptions>
            <!-- MUST enable reactive mode -->
            <reactive>true</reactive>
            <interfaceOnly>true</interfaceOnly>
            <java8>true</java8>
            <dateLibrary>java8</dateLibrary>
            <useBeanValidation>true</useBeanValidation>
        </configOptions>
    </configuration>
</plugin>
```

**Key Settings**:
- `<templateDirectory>` - Points to custom templates
- `<reactive>true</reactive>` - Enables reactive mode

---

### Step 3: Generate Code

```bash
mvn clean generate-sources
```

---

### Step 4: Verify Generated Code

```bash
# Check if FilePart is generated
cat target/generated-sources/src/main/java/io/swagger/api/DocumentsApi.java | grep "FilePart"
```

Should output:
```java
Mono<FilePart> content
```

✅ **SUCCESS!**

---

## 🧪 Testing

### Automated Test Script

Run the provided test script:

```bash
bash test-swagger-codegen-templates.sh
```

This will:
1. ✅ Check if templates exist
2. ✅ Check pom.xml configuration
3. ✅ Run code generation
4. ✅ Verify generated code uses `FilePart`
5. ✅ Verify generated code uses `Mono<ResponseEntity>`

---

## 📊 Comparison

### Default Swagger Codegen Output:

```java
public interface DocumentsApi {

    @RequestMapping(method = RequestMethod.POST, value = "/documents")
    ResponseEntity<InlineResponse200> addDocument(
        @RequestHeader("X-version") Integer xVersion,
        @RequestHeader("X-correlation-id") String xCorrelationId,
        @RequestPart(value="content", required=true) MultipartFile content,  // ❌ Blocking
        @RequestPart(value="metadata", required=true) Metadata metadata
    );
}
```

### With Custom Templates:

```java
public interface DocumentsApi {

    @RequestMapping(method = RequestMethod.POST, value = "/documents")
    Mono<ResponseEntity<InlineResponse200>> addDocument(  // ✅ Reactive return
        @RequestHeader("X-version") Integer xVersion,
        @RequestHeader("X-correlation-id") String xCorrelationId,
        @RequestPart(value="content", required=true) Mono<FilePart> content,  // ✅ Reactive FilePart
        @RequestPart(value="metadata", required=true) Metadata metadata,
        @Parameter(hidden = true) final ServerWebExchange exchange  // ✅ WebFlux
    );
}
```

---

## ⚙️ How It Works

### Template Variables Flow:

1. **Swagger Codegen reads api.yaml**
   - Finds `content` parameter with `type: string, format: binary`
   - Sets `isFile = true`

2. **Checks configuration**
   - Sees `<reactive>true</reactive>` in pom.xml
   - Sets `reactive = true` variable

3. **Processes formParams.mustache**
   ```mustache
   {{#isFile}}          <!-- TRUE for content parameter -->
     {{#reactive}}      <!-- TRUE because reactive=true -->
       Mono<FilePart>   <!-- ✅ Generated! -->
     {{/reactive}}
   {{/isFile}}
   ```

4. **Generates code**
   ```java
   Mono<FilePart> content
   ```

---

## 🎓 Template Syntax Explained

### Basic Mustache Syntax:

```mustache
{{variableName}}              <!-- Insert variable value -->

{{#boolean}}                  <!-- If true -->
  Content when true
{{/boolean}}

{{^boolean}}                  <!-- If false -->
  Content when false
{{/boolean}}

{{#array}}                    <!-- Loop through array -->
  {{name}}                    <!-- Access item property -->
{{/array}}
```

### Our Template Usage:

```mustache
{{#isFormParam}}              <!-- Is this a form parameter? -->
  {{#isFile}}                 <!-- Is it a file upload? -->
    {{#reactive}}             <!-- Is reactive mode enabled? -->
      Mono<FilePart> {{paramName}}  <!-- Generate reactive type -->
    {{/reactive}}
    {{^reactive}}             <!-- Reactive mode disabled -->
      MultipartFile {{paramName}}   <!-- Generate blocking type -->
    {{/reactive}}
  {{/isFile}}
{{/isFormParam}}
```

---

## 🔧 Troubleshooting

### Issue 1: Still Generating MultipartFile

**Symptoms**:
```java
MultipartFile content  // ❌ Still blocking
```

**Solutions**:

1. **Check templateDirectory is set**:
   ```bash
   grep -n "templateDirectory" pom.xml
   ```

2. **Check reactive mode is enabled**:
   ```bash
   grep -n "<reactive>true</reactive>" pom.xml
   ```

3. **Clean and regenerate**:
   ```bash
   rm -rf target/generated-sources
   mvn clean generate-sources
   ```

4. **Check template file exists**:
   ```bash
   cat src/main/resources/swagger-templates/formParams.mustache
   ```

---

### Issue 2: Compilation Errors

**Symptoms**:
```
error: cannot find symbol
  symbol:   class FilePart
  location: interface DocumentsApi
```

**Solution**: Add dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

### Issue 3: Templates Not Being Used

**Symptoms**: No errors, but still generates old code.

**Solution**: Swagger Codegen might be caching. Try:

```bash
mvn clean
rm -rf ~/.m2/repository/io/swagger/codegen
mvn generate-sources
```

---

## 📈 Maintenance

### When to Update Templates:

| Event | Action |
|-------|--------|
| **Swagger Codegen upgrade** | Test templates still work |
| **New parameter types** | May need new template logic |
| **Spring Boot upgrade** | Verify FilePart still compatible |
| **Team members join** | Share template documentation |

### Version Control:

```bash
# Commit templates
git add src/main/resources/swagger-templates/
git commit -m "Add custom Mustache templates for FilePart generation"

# Tag for versioning
git tag -a mustache-v1.0 -m "Custom templates v1.0"
```

---

## 🎯 Benefits vs Alternatives

### Custom Templates ✅

**Pros**:
- ✅ Works with Swagger Codegen (company requirement)
- ✅ Generates correct reactive types
- ✅ Version controlled
- ✅ Reusable across projects
- ✅ Team can share

**Cons**:
- ⚠️ Requires maintenance
- ⚠️ Can break with Swagger Codegen updates
- ⚠️ Need to understand Mustache syntax

### OpenAPI Generator 🔄

**Pros**:
- ✅ Works out of the box (no templates needed)
- ✅ Actively maintained
- ✅ Better reactive support

**Cons**:
- ⚠️ May need company approval
- ⚠️ Different tool from Swagger Codegen

### Manual Controller 📝

**Pros**:
- ✅ Full control
- ✅ No code generation issues
- ✅ Current approach

**Cons**:
- ⚠️ More code to write
- ⚠️ No automatic sync with spec changes

---

## 🏁 Quick Start Checklist

- [ ] Templates created in `src/main/resources/swagger-templates/`
- [ ] `pom.xml` updated with `<templateDirectory>`
- [ ] `<reactive>true</reactive>` enabled in configOptions
- [ ] Run `mvn clean generate-sources`
- [ ] Verify `FilePart` in generated code
- [ ] Test with `test-swagger-codegen-templates.sh`
- [ ] Commit templates to version control

---

## 📚 Files Reference

| File | Purpose |
|------|---------|
| `formParams.mustache` | ⭐ Main fix - handles file uploads |
| `queryParams.mustache` | Query parameters |
| `pathParams.mustache` | Path variables |
| `headerParams.mustache` | Header parameters |
| `bodyParams.mustache` | Request body |
| `README.md` | Template documentation |
| `CUSTOM_MUSTACHE_TEMPLATES_GUIDE.md` | Complete guide |
| `pom-swagger-codegen-with-templates.xml.example` | Example pom.xml |
| `test-swagger-codegen-templates.sh` | Automated test |

---

## 🤔 Decision: Should You Use This?

### Use Custom Templates IF:

- ✅ Company mandates Swagger Codegen (cannot switch)
- ✅ Using Spring WebFlux (reactive)
- ✅ Want to keep using code generation
- ✅ Team can maintain Mustache templates

### Use OpenAPI Generator (Current) IF:

- ✅ Can get approval to switch tools
- ✅ Want out-of-box reactive support
- ✅ Prefer modern, maintained tool
- ⭐ **RECOMMENDED**

### Use Manual Controller (Current) IF:

- ✅ Generate models only
- ✅ Want full control over controller
- ✅ Company standard is manual controllers
- ⭐ **YOUR CURRENT APPROACH - ALREADY WORKING**

---

## 🎉 Conclusion

You now have:
1. ✅ Custom Mustache templates ready to use
2. ✅ Complete documentation
3. ✅ Test script to verify
4. ✅ Example pom.xml configuration

**To use them**: Just update your `pom.xml` to point to the templates and set `reactive=true`.

**Current recommendation**: Stick with your current approach (manual controller + OpenAPI Generator for models) - it's simpler and works great! Use these templates only if you absolutely must use Swagger Codegen.

---

**Created**: 2025-11-28
**Status**: ✅ Ready to Use
**Tested**: Not yet (awaiting your decision)
