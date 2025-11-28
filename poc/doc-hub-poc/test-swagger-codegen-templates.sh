#!/bin/bash

# Test Script for Swagger Codegen Custom Templates
# This script tests if the custom Mustache templates work correctly

echo "========================================="
echo "Testing Swagger Codegen Custom Templates"
echo "========================================="
echo ""

# Check if templates exist
echo "Step 1: Checking if custom templates exist..."
TEMPLATE_DIR="src/main/resources/swagger-templates"

if [ ! -d "$TEMPLATE_DIR" ]; then
    echo "❌ ERROR: Template directory not found: $TEMPLATE_DIR"
    exit 1
fi

REQUIRED_TEMPLATES=(
    "formParams.mustache"
    "queryParams.mustache"
    "pathParams.mustache"
    "headerParams.mustache"
    "bodyParams.mustache"
)

for template in "${REQUIRED_TEMPLATES[@]}"; do
    if [ -f "$TEMPLATE_DIR/$template" ]; then
        echo "✅ Found: $template"
    else
        echo "❌ Missing: $template"
        exit 1
    fi
done

echo ""
echo "Step 2: Checking pom.xml configuration..."

# Check if pom.xml has templateDirectory configured
if grep -q "templateDirectory" pom.xml; then
    echo "✅ pom.xml has templateDirectory configuration"
else
    echo "⚠️  WARNING: pom.xml does not have templateDirectory configured"
    echo "   Add this to your swagger-codegen-maven-plugin configuration:"
    echo "   <templateDirectory>\${project.basedir}/src/main/resources/swagger-templates</templateDirectory>"
fi

echo ""
echo "Step 3: Checking if reactive is enabled..."

if grep -q "<reactive>true</reactive>" pom.xml; then
    echo "✅ Reactive mode is enabled"
else
    echo "⚠️  WARNING: Reactive mode is not enabled"
    echo "   Add this to your configOptions:"
    echo "   <reactive>true</reactive>"
fi

echo ""
echo "Step 4: Testing code generation..."
echo "Running: mvn clean generate-sources"
echo ""

mvn clean generate-sources -q

if [ $? -eq 0 ]; then
    echo "✅ Code generation successful"
else
    echo "❌ Code generation failed"
    exit 1
fi

echo ""
echo "Step 5: Checking generated code..."

GENERATED_API="target/generated-sources/src/main/java/io/swagger/api/DocumentsApi.java"

if [ -f "$GENERATED_API" ]; then
    echo "✅ Generated API file found"

    # Check for FilePart (should be present with custom templates)
    if grep -q "FilePart" "$GENERATED_API"; then
        echo "✅ SUCCESS: Generated code uses FilePart (reactive)"
        echo ""
        echo "Sample from generated code:"
        grep -A 2 "FilePart" "$GENERATED_API" | head -5
    else
        echo "❌ FAILURE: Generated code does not use FilePart"
        echo "   This means custom templates are not being used"
        echo ""
        echo "Checking for MultipartFile instead:"
        if grep -q "MultipartFile" "$GENERATED_API"; then
            echo "❌ Found MultipartFile (non-reactive) - templates not working"
        fi
    fi

    # Check for Mono return type
    echo ""
    if grep -q "Mono<ResponseEntity" "$GENERATED_API"; then
        echo "✅ SUCCESS: Generated code uses Mono<ResponseEntity> (reactive)"
    else
        echo "❌ FAILURE: Generated code does not use Mono<ResponseEntity>"
    fi
else
    echo "❌ Generated API file not found"
    echo "   Expected: $GENERATED_API"
fi

echo ""
echo "========================================="
echo "Test Complete"
echo "========================================="
