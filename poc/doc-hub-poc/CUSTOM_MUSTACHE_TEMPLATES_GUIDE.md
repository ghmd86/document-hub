# Custom Mustache Templates for Swagger Codegen

## Goal
Fix Swagger Codegen 3.x to generate `FilePart` instead of `MultipartFile` for reactive WebFlux applications.

---

## Directory Structure

```
project-root/
├── src/
│   └── main/
│       └── resources/
│           └── swagger-templates/
│               ├── api.mustache                 # Main API interface template
│               ├── apiController.mustache       # Controller template (if needed)
│               └── README.md                    # Template documentation
├── pom.xml
└── src/main/resources/api.yaml
```

---

## How Mustache Templates Work

Swagger Codegen uses Mustache templates to generate code. Variables are provided by the code generator:

```mustache
{{#operations}}              <!-- Loop through operations -->
  {{#operation}}             <!-- Each operation (endpoint) -->
    {{#allParams}}           <!-- All parameters -->
      {{paramName}}          <!-- Parameter name -->
      {{dataType}}           <!-- Parameter data type -->
      {{isFile}}             <!-- Boolean: is this a file? -->
    {{/allParams}}
  {{/operation}}
{{/operations}}
```

---

## Template Variables Available

### For File Upload Parameters:

| Variable | Value | Description |
|----------|-------|-------------|
| `{{isFile}}` | `true` | Indicates this is a file parameter |
| `{{dataType}}` | `MultipartFile` | Default data type (we need to override) |
| `{{baseName}}` | `content` | Parameter name from spec |
| `{{required}}` | `true/false` | Is parameter required? |
| `{{#reactive}}` | boolean | Is reactive mode enabled? |

---

## Custom Template Files

These templates will fix the `FilePart` issue.

---

### File 1: `api.mustache`

This is the main template that generates the API interface.

```mustache
package {{package}};

{{#imports}}import {{import}};
{{/imports}}
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
{{#reactive}}
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.server.ServerWebExchange;
{{/reactive}}
{{^reactive}}
import org.springframework.web.multipart.MultipartFile;
{{/reactive}}

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;
{{#swagger2AnnotationLibrary}}
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
{{/swagger2AnnotationLibrary}}

@Validated
{{>generatedAnnotation}}
{{#operations}}
public interface {{classname}} {
{{#operation}}

    /**
     * {{httpMethod}} {{path}}{{#summary}} : {{.}}{{/summary}}
     {{#notes}}
     * {{.}}
     {{/notes}}
     {{#allParams}}
     * @param {{paramName}} {{description}}{{#required}} (required){{/required}}{{^required}} (optional{{#defaultValue}}, default to {{.}}{{/defaultValue}}){{/required}}
     {{/allParams}}
     * @return {{#responses}}{{message}} (status code {{code}}){{^-last}}, {{/-last}}{{/responses}}
     */
    {{#swagger2AnnotationLibrary}}
    @Operation(
        operationId = "{{operationId}}",
        summary = "{{summary}}",
        description = "{{notes}}"{{#hasAuthMethods}},
        security = {
            {{#authMethods}}
            @SecurityRequirement(name = "{{name}}"{{#scopes}}, scopes={ {{#scopes}}"{{scope}}"{{^-last}}, {{/-last}}{{/scopes}} }{{/scopes}}){{^-last}},{{/-last}}
            {{/authMethods}}
        }{{/hasAuthMethods}}
    )
    @ApiResponses(value = {
        {{#responses}}
        @ApiResponse(responseCode = "{{code}}", description = "{{message}}"{{#baseType}}, content = @Content(schema = @Schema(implementation = {{{baseType}}}.class)){{/baseType}}){{^-last}},{{/-last}}
        {{/responses}}
    })
    {{/swagger2AnnotationLibrary}}
    @RequestMapping(
        method = RequestMethod.{{httpMethod}},
        value = "{{path}}"{{#hasProduces}},
        produces = { {{#produces}}"{{{mediaType}}}"{{^-last}}, {{/-last}}{{/produces}} }{{/hasProduces}}{{#hasConsumes}},
        consumes = { {{#consumes}}"{{{mediaType}}}"{{^-last}}, {{/-last}}{{/consumes}} }{{/hasConsumes}}
    )
    {{#reactive}}
    default Mono<ResponseEntity<{{{returnType}}}{{^returnType}}Void{{/returnType}}>> {{operationId}}(
    {{/reactive}}
    {{^reactive}}
    ResponseEntity<{{{returnType}}}{{^returnType}}Void{{/returnType}}> {{operationId}}(
    {{/reactive}}
        {{#allParams}}
        {{>queryParams}}{{>pathParams}}{{>headerParams}}{{>bodyParams}}{{>formParams}}{{^-last}},
        {{/-last}}
        {{/allParams}}{{#reactive}}{{#allParams.0}},
        {{/allParams.0}}@Parameter(hidden = true) final ServerWebExchange exchange{{/reactive}}
    ){{#reactive}} {
        {{#returnType}}
        return Mono.error(new UnsupportedOperationException("Not implemented"));
        {{/returnType}}
        {{^returnType}}
        return Mono.empty();
        {{/returnType}}
    }{{/reactive}};
{{/operation}}
}
{{/operations}}
```

---

### File 2: `formParams.mustache` (THE KEY FIX!)

This template handles form parameters including file uploads:

```mustache
{{#isFormParam}}
{{#isFile}}
{{#reactive}}
@Parameter(description = "{{description}}"{{#required}}, required = true{{/required}})
@RequestPart(value = "{{baseName}}", required = {{required}})
{{#isArray}}
Flux<FilePart> {{paramName}}
{{/isArray}}
{{^isArray}}
Mono<FilePart> {{paramName}}
{{/isArray}}
{{/reactive}}
{{^reactive}}
@Parameter(description = "{{description}}"{{#required}}, required = true{{/required}})
@Valid @RequestPart(value = "{{baseName}}", required = {{required}})
MultipartFile {{paramName}}
{{/reactive}}
{{/isFile}}
{{^isFile}}
@Parameter(description = "{{description}}"{{#required}}, required = true{{/required}})
@RequestPart(value = "{{baseName}}", required = {{required}})
{{#useBeanValidation}}@Valid {{/useBeanValidation}}{{{dataType}}} {{paramName}}
{{/isFile}}
{{/isFormParam}}
```

**Key Changes**:
- `{{#reactive}}` check for reactive mode
- If reactive AND file: `Mono<FilePart>` or `Flux<FilePart>`
- If NOT reactive AND file: `MultipartFile`
- For arrays: `Flux<FilePart>` (multiple files)

---

### File 3: `queryParams.mustache`

```mustache
{{#isQueryParam}}
@Parameter(description = "{{description}}"{{#required}}, required = true{{/required}}{{^required}}, required = false{{/required}})
@Valid @RequestParam(value = "{{baseName}}"{{^required}}, required = false{{/required}}{{#defaultValue}}, defaultValue = "{{.}}"{{/defaultValue}}) {{{dataType}}} {{paramName}}
{{/isQueryParam}}
```

---

### File 4: `pathParams.mustache`

```mustache
{{#isPathParam}}
@Parameter(description = "{{description}}", required = true)
@PathVariable("{{baseName}}") {{{dataType}}} {{paramName}}
{{/isPathParam}}
```

---

### File 5: `headerParams.mustache`

```mustache
{{#isHeaderParam}}
@Parameter(description = "{{description}}"{{#required}}, required = true{{/required}})
@RequestHeader(value = "{{baseName}}", required = {{required}}{{#defaultValue}}, defaultValue = "{{.}}"{{/defaultValue}}) {{{dataType}}} {{paramName}}
{{/isHeaderParam}}
```

---

### File 6: `bodyParams.mustache`

```mustache
{{#isBodyParam}}
@Parameter(description = "{{description}}"{{#required}}, required = true{{/required}})
@Valid @RequestBody {{{dataType}}} {{paramName}}
{{/isBodyParam}}
```

---

## Updated `pom.xml` Configuration

```xml
<plugin>
    <groupId>io.swagger.codegen.v3</groupId>
    <artifactId>swagger-codegen-maven-plugin</artifactId>
    <version>3.0.52</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
                <language>spring</language>
                <library>spring-boot</library>
                <output>${project.build.directory}/generated-sources</output>
                <apiPackage>io.swagger.api</apiPackage>
                <modelPackage>io.swagger.model</modelPackage>

                <!-- Point to custom templates -->
                <templateDirectory>${project.basedir}/src/main/resources/swagger-templates</templateDirectory>

                <configOptions>
                    <reactive>true</reactive>
                    <interfaceOnly>true</interfaceOnly>
                    <java8>true</java8>
                    <dateLibrary>java8</dateLibrary>
                    <useBeanValidation>true</useBeanValidation>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Key Addition**: `<templateDirectory>` pointing to custom templates.

---

## Testing the Templates

### Step 1: Create the template directory
```bash
mkdir -p src/main/resources/swagger-templates
```

### Step 2: Add the template files
Copy all the `.mustache` files into `src/main/resources/swagger-templates/`

### Step 3: Generate code
```bash
mvn clean generate-sources
```

### Step 4: Verify the generated code
```bash
cat target/generated-sources/src/main/java/io/swagger/api/DocumentsApi.java | grep "content"
```

You should see:
```java
Mono<FilePart> content  // ✅ SUCCESS!
```

Instead of:
```java
MultipartFile content   // ❌ OLD WAY
```

---

## Expected Generated Code

With custom templates, Swagger Codegen 3.x will generate:

```java
@RequestMapping(
    method = RequestMethod.POST,
    value = "/documents",
    produces = { "application/json" },
    consumes = { "multipart/form-data" }
)
default Mono<ResponseEntity<InlineResponse200>> addDocument(
    @RequestHeader(value = "X-version", required = true) Integer xVersion,
    @RequestHeader(value = "X-correlation-id", required = true) String xCorrelationId,
    @RequestHeader(value = "X-requestor-id", required = true) UUID xRequestorId,
    @RequestHeader(value = "X-requestor-type", required = true) XRequestorType xRequestorType,
    @RequestPart(value = "documentType", required = true) String documentType,
    @RequestPart(value = "createdBy", required = true) UUID createdBy,
    @RequestPart(value = "content", required = true) Mono<FilePart> content,  // ✅ FIXED!
    @RequestPart(value = "metadata", required = true) Metadata metadata,
    @Parameter(hidden = true) final ServerWebExchange exchange
) {
    return Mono.error(new UnsupportedOperationException("Not implemented"));
}
```

---

## Troubleshooting

### Issue 1: Templates not being used

**Check**:
```bash
ls src/main/resources/swagger-templates/
```

Should show:
```
api.mustache
bodyParams.mustache
formParams.mustache
headerParams.mustache
pathParams.mustache
queryParams.mustache
```

### Issue 2: Still generating MultipartFile

**Solution**: Delete the generated sources and regenerate:
```bash
rm -rf target/generated-sources
mvn clean generate-sources
```

### Issue 3: Compilation errors

**Check imports**: Make sure these are imported:
```java
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
```

---

## Advantages of Custom Templates

✅ **Pros**:
- Works with Swagger Codegen (company requirement)
- Generates `FilePart` for reactive
- Generates `MultipartFile` for non-reactive
- Reusable across projects
- Version controlled

❌ **Cons**:
- Requires template maintenance
- Can break with Swagger Codegen updates
- More complex than OpenAPI Generator
- Need to understand Mustache syntax

---

## Maintenance

### When to Update Templates

1. **Swagger Codegen version upgrade** - Check if templates still work
2. **New parameter types** - May need template adjustments
3. **API spec changes** - Ensure templates handle new patterns

### Version Control

```bash
git add src/main/resources/swagger-templates/
git commit -m "Add custom Mustache templates for FilePart generation"
```

---

## Alternative: Simpler Partial Override

If you don't want to maintain full templates, you can override just the problematic part:

### Minimal `formParams.mustache`:

```mustache
{{#isFormParam}}
{{#isFile}}
{{#reactive}}
@RequestPart(value = "{{baseName}}", required = {{required}}) Mono<org.springframework.http.codec.multipart.FilePart> {{paramName}}
{{/reactive}}
{{^reactive}}
@RequestPart(value = "{{baseName}}", required = {{required}}) MultipartFile {{paramName}}
{{/reactive}}
{{/isFile}}
{{^isFile}}
@RequestPart(value = "{{baseName}}", required = {{required}}) {{{dataType}}} {{paramName}}
{{/isFile}}
{{/isFormParam}}
```

This is much smaller and easier to maintain!

---

## Comparison: Custom Templates vs OpenAPI Generator

| Aspect | Custom Templates | OpenAPI Generator |
|--------|------------------|-------------------|
| **Setup Effort** | ⭐⭐⭐⭐ High | ⭐ Low |
| **Maintenance** | ⭐⭐⭐ Medium | ⭐⭐⭐⭐⭐ Low |
| **Flexibility** | ⭐⭐⭐⭐⭐ Full control | ⭐⭐⭐ Good |
| **Company Policy** | ✅ Uses Swagger Codegen | ⚠️ Needs approval |
| **Breaking Changes** | ❌ Can break on updates | ✅ Better stability |

---

## Recommendation

If you MUST use Swagger Codegen due to company policy:
1. ✅ Use custom Mustache templates (this approach)
2. ✅ Start with minimal override (just `formParams.mustache`)
3. ✅ Test thoroughly
4. ✅ Version control templates

If you CAN get approval:
1. ✅ Use OpenAPI Generator (what you have now)
2. ✅ Much simpler
3. ✅ Better maintained

Your current approach (manual controller + OpenAPI Generator for models) is still the best! 🎯
