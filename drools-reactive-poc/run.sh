#!/bin/bash

# Quick Start Script for Drools Reactive POC

echo "======================================"
echo "Drools Reactive POC - Quick Start"
echo "======================================"
echo ""

echo "Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""
echo "Starting application..."
echo ""

mvn spring-boot:run
