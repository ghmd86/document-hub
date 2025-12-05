package com.documenthub.service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JSONPath extraction patterns used in data extraction config.
 * Tests different JSONPath expressions against the actual API response format.
 */
public class JsonPathExtractionTest {

    /**
     * Mock API response from /creditcard/accounts/{accountId}/arrangements
     * This is the actual format returned by MockApiController
     */
    private static final String ARRANGEMENTS_API_RESPONSE = """
        {
          "accountId": "aaaa0000-0000-0000-0000-000000000001",
          "totalItems": 2,
          "content": [
            {
              "domain": "PRICING",
              "domainId": "PRC-12345",
              "effectiveDate": "2024-01-01",
              "status": "ACTIVE"
            },
            {
              "domain": "REWARDS",
              "domainId": "RWD-VIP-001",
              "status": "ACTIVE"
            }
          ]
        }
        """;

    @Test
    public void testSimpleArrayIndex_extractsStringCorrectly() {
        // This is the RECOMMENDED approach
        String jsonPath = "$.content[0].domainId";

        Object result = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("✅ Simple array index: " + jsonPath);
        System.out.println("   Result type: " + result.getClass().getSimpleName());
        System.out.println("   Result value: " + result);

        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof String, "Result should be a String, got: " + result.getClass());
        assertEquals("PRC-12345", result, "Should extract the pricingId from first element");
    }

    @Test
    public void testFilterWithoutIndex_returnsArray() {
        // This returns an ARRAY, not a string
        String jsonPath = "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId";

        Object result = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("\n❌ Filter without index: " + jsonPath);
        System.out.println("   Result type: " + result.getClass().getSimpleName());
        System.out.println("   Result value: " + result);

        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof net.minidev.json.JSONArray,
            "Result should be JSONArray, got: " + result.getClass());

        net.minidev.json.JSONArray array = (net.minidev.json.JSONArray) result;
        assertEquals(1, array.size(), "Array should have one element");
        assertEquals("PRC-12345", array.get(0), "Array should contain PRC-12345");
    }

    @Test
    public void testFilterWithIndexBefore_returnsEmptyOrNull() {
        // This doesn't work as expected - filter[0].property doesn't work
        String jsonPath = "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")][0].domainId";

        try {
            Object result = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

            System.out.println("\n⚠️  Filter with [0] before property: " + jsonPath);
            System.out.println("   Result type: " + (result != null ? result.getClass().getSimpleName() : "null"));
            System.out.println("   Result value: " + result);

            // Depending on JSONPath library version, this might return null, empty, or throw
            // In our case, it returns empty JSONArray
            if (result instanceof net.minidev.json.JSONArray) {
                net.minidev.json.JSONArray array = (net.minidev.json.JSONArray) result;
                assertTrue(array.isEmpty(), "This syntax returns empty array");
            }
        } catch (Exception e) {
            System.out.println("\n⚠️  Filter with [0] before property throws: " + e.getMessage());
        }
    }

    @Test
    public void testFilterWithIndexAfter_returnsArray() {
        // This also doesn't work - you can't index after property access
        String jsonPath = "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId[0]";

        Object result = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("\n❌ Filter with [0] after property: " + jsonPath);
        System.out.println("   Result type: " + result.getClass().getSimpleName());
        System.out.println("   Result value: " + result);

        // This returns an empty JSONArray because .domainId[0] tries to index a string
        assertNotNull(result);
        assertTrue(result instanceof net.minidev.json.JSONArray);
        net.minidev.json.JSONArray array = (net.minidev.json.JSONArray) result;
        assertTrue(array.isEmpty(), "This syntax returns empty array");
    }

    @Test
    public void testAlternativeApproach_filterThenIndex() {
        // Alternative: Use array indexing on the filtered results
        // This requires two-step extraction
        String filterPath = "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")]";

        Object filteredResult = JsonPath.read(ARRANGEMENTS_API_RESPONSE, filterPath);

        System.out.println("\n✅ Two-step extraction:");
        System.out.println("   Step 1 - Filter: " + filterPath);
        System.out.println("   Result: " + filteredResult);

        assertTrue(filteredResult instanceof net.minidev.json.JSONArray);
        net.minidev.json.JSONArray array = (net.minidev.json.JSONArray) filteredResult;
        assertEquals(1, array.size());

        // Step 2: Extract domainId from first result using LinkedHashMap
        @SuppressWarnings("unchecked")
        java.util.LinkedHashMap<String, Object> firstElement =
            (java.util.LinkedHashMap<String, Object>) array.get(0);
        String domainId = (String) firstElement.get("domainId");
        assertEquals("PRC-12345", domainId);

        System.out.println("   Step 2 - Extract domainId: " + domainId);
    }

    @Test
    public void testRecommendedSolution_summary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SUMMARY: JSONPath Expression Recommendations");
        System.out.println("=".repeat(60));

        System.out.println("\n✅ RECOMMENDED (works correctly):");
        System.out.println("   $.content[0].domainId");
        System.out.println("   → Returns: \"PRC-12345\" (String)");
        System.out.println("   → Use when: PRICING is always first element");

        System.out.println("\n❌ AVOID (returns array instead of string):");
        System.out.println("   $.content[?(@.domain == \"PRICING\")].domainId");
        System.out.println("   → Returns: [\"PRC-12345\"] (JSONArray)");
        System.out.println("   → Problem: String URL substitution fails");

        System.out.println("\n❌ AVOID (returns empty array):");
        System.out.println("   $.content[?(@.domain == \"PRICING\")][0].domainId");
        System.out.println("   → Returns: [] (Empty JSONArray)");
        System.out.println("   → Problem: Filter[index] syntax doesn't work");

        System.out.println("\n❌ AVOID (returns empty array):");
        System.out.println("   $.content[?(@.domain == \"PRICING\")].domainId[0]");
        System.out.println("   → Returns: [] (Empty JSONArray)");
        System.out.println("   → Problem: Can't index after property access");

        System.out.println("\n" + "=".repeat(60));
    }
}
