#!/bin/bash

# Test script for Drools Reactive POC API

BASE_URL="http://localhost:8080/api"

echo "======================================"
echo "Testing Drools Reactive POC API"
echo "======================================"
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
echo "GET $BASE_URL/health"
echo ""
curl -s $BASE_URL/health
echo ""
echo ""

# Test 2: Eligibility Check
echo "Test 2: Eligibility Check"
echo "POST $BASE_URL/eligibility"
echo ""
curl -s -X POST $BASE_URL/eligibility \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "accountId": "ACC456"
  }' | jq '.'

echo ""
echo "======================================"
echo "Tests complete!"
echo "======================================"
