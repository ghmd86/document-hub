#!/bin/bash

echo "=========================================="
echo "CODE QUALITY ANALYSIS REPORT"
echo "=========================================="
echo ""

# Find all Java source files (excluding generated)
JAVA_FILES=$(find src/main/java -name "*.java" 2>/dev/null)

echo "=== METHOD LENGTH CHECK (Max 25 lines) ==="
echo ""

for file in $JAVA_FILES; do
    # Use awk to find methods and count lines
    awk '
    /^[[:space:]]*(public|private|protected)[^;{]*\{[[:space:]]*$/ || /^[[:space:]]*(public|private|protected)[^;{]*\([^)]*\)[^;{]*\{[[:space:]]*$/ {
        method_start = NR
        method_name = $0
        gsub(/^[[:space:]]+/, "", method_name)
        brace_count = 1
        while (brace_count > 0 && (getline line) > 0) {
            gsub(/"[^"]*"/, "", line)  # Remove strings
            n = gsub(/\{/, "{", line)
            brace_count += n
            n = gsub(/\}/, "}", line)
            brace_count -= n
        }
        method_length = NR - method_start
        if (method_length > 25) {
            printf "  VIOLATION: %s:%d - %d lines (max 25)\n", FILENAME, method_start, method_length
            # Print first 60 chars of method signature
            sig = method_name
            if (length(sig) > 70) sig = substr(sig, 1, 67) "..."
            printf "    Method: %s\n", sig
        }
    }
    ' "$file"
done

echo ""
echo "=== PARAMETER COUNT CHECK (Max 4 parameters) ==="
echo ""

for file in $JAVA_FILES; do
    grep -n "^\s*\(public\|private\|protected\).*(" "$file" 2>/dev/null | while read line; do
        linenum=$(echo "$line" | cut -d: -f1)
        content=$(echo "$line" | cut -d: -f2-)
        # Count commas between parentheses (rough parameter count)
        params=$(echo "$content" | sed 's/.*(\(.*\)).*/\1/' | tr ',' '\n' | grep -v "^$" | wc -l)
        if [ "$params" -gt 4 ]; then
            method=$(echo "$content" | sed 's/.*\s\+\([a-zA-Z_][a-zA-Z0-9_]*\)\s*(.*/\1/')
            echo "  VIOLATION: $file:$linenum - $params parameters (max 4)"
            echo "    Method: $method"
        fi
    done
done

echo ""
echo "=== CLASS LENGTH CHECK (Max 500 lines) ==="
echo ""

for file in $JAVA_FILES; do
    lines=$(wc -l < "$file")
    if [ "$lines" -gt 500 ]; then
        classname=$(basename "$file" .java)
        echo "  VIOLATION: $file - $lines lines (max 500)"
    fi
done

echo ""
echo "=== NESTED DEPTH CHECK (Max 3 levels) ==="
echo ""

for file in $JAVA_FILES; do
    awk '
    BEGIN { max_depth = 0; current_depth = 0; in_method = 0 }
    /^[[:space:]]*(public|private|protected).*\{/ { in_method = 1; current_depth = 0 }
    /\{/ && in_method { 
        current_depth++
        if (current_depth > 3) {
            printf "  VIOLATION: %s:%d - Nesting depth %d (max 3)\n", FILENAME, NR, current_depth
        }
    }
    /\}/ && in_method { current_depth-- }
    ' "$file"
done

echo ""
echo "=== METHOD COUNT PER CLASS (Max 20 methods) ==="
echo ""

for file in $JAVA_FILES; do
    count=$(grep -c "^\s*\(public\|private\|protected\)\s\+[a-zA-Z].*(" "$file" 2>/dev/null || echo 0)
    if [ "$count" -gt 20 ]; then
        classname=$(basename "$file" .java)
        echo "  VIOLATION: $file - $count methods (max 20)"
    fi
done

echo ""
echo "=========================================="
echo "ANALYSIS COMPLETE"
echo "=========================================="
