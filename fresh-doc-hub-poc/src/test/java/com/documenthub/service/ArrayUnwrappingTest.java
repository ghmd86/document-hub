package com.documenthub.service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify that single-element arrays from JSONPath are properly unwrapped
 * This tests the logic added to ConfigurableDataExtractionService
 */
public class ArrayUnwrappingTest {

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
    public void testArrayUnwrapping_singleElementArray() {
        // This is what JSONPath filter returns
        String jsonPath = "$.content[?(@.domain == \"PRICING\" && @.status == \"ACTIVE\")].domainId";

        Object value = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("Original value type: " + value.getClass().getSimpleName());
        System.out.println("Original value: " + value);

        assertTrue(value instanceof List, "Should be List");
        List<?> array = (List<?>) value;
        assertEquals(1, array.size(), "Should have one element");

        // Simulate the unwrapping logic from ConfigurableDataExtractionService
        if (value instanceof List) {
            List<?> arr = (List<?>) value;
            if (arr.size() == 1) {
                value = arr.get(0);
                System.out.println("Unwrapped value type: " + value.getClass().getSimpleName());
                System.out.println("Unwrapped value: " + value);
            }
        }

        assertTrue(value instanceof String, "After unwrapping, should be String");
        assertEquals("PRC-12345", value, "Should be the pricingId");
    }

    @Test
    public void testArrayUnwrapping_multipleElements() {
        // If filter matches multiple elements, we should NOT unwrap
        String jsonPath = "$.content[?(@.status == \"ACTIVE\")].domainId";

        Object value = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("\nMultiple elements - Original value type: " + value.getClass().getSimpleName());
        System.out.println("Multiple elements - Original value: " + value);

        assertTrue(value instanceof List, "Should be List");
        List<?> array = (List<?>) value;
        assertEquals(2, array.size(), "Should have two elements");

        // Simulate the unwrapping logic - should NOT unwrap since size != 1
        if (value instanceof List) {
            List<?> arr = (List<?>) value;
            if (arr.size() == 1) {
                value = arr.get(0);
            }
        }

        assertTrue(value instanceof List, "Should remain as List");
        assertEquals(2, ((List<?>) value).size(), "Should still have two elements");
    }

    @Test
    public void testArrayUnwrapping_emptyArray() {
        // If filter matches nothing, we should NOT unwrap
        String jsonPath = "$.content[?(@.domain == \"NONEXISTENT\")].domainId";

        Object value = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("\nEmpty array - Original value type: " + value.getClass().getSimpleName());
        System.out.println("Empty array - Original value: " + value);

        assertTrue(value instanceof List, "Should be List");
        List<?> array = (List<?>) value;
        assertEquals(0, array.size(), "Should be empty");

        // Simulate the unwrapping logic - should NOT unwrap since size != 1
        if (value instanceof List) {
            List<?> arr = (List<?>) value;
            if (arr.size() == 1) {
                value = arr.get(0);
            }
        }

        assertTrue(value instanceof List, "Should remain as List");
        assertEquals(0, ((List<?>) value).size(), "Should still be empty");
    }

    @Test
    public void testSimplePathWithoutFilter_noUnwrappingNeeded() {
        // Simple path returns string directly, no array
        String jsonPath = "$.content[0].domainId";

        Object value = JsonPath.read(ARRANGEMENTS_API_RESPONSE, jsonPath);

        System.out.println("\nSimple path - Original value type: " + value.getClass().getSimpleName());
        System.out.println("Simple path - Original value: " + value);

        assertTrue(value instanceof String, "Should already be String");
        assertEquals("PRC-12345", value, "Should be the pricingId");

        // Simulate the unwrapping logic - should do nothing since not an array
        if (value instanceof List) {
            List<?> arr = (List<?>) value;
            if (arr.size() == 1) {
                value = arr.get(0);
            }
        }

        assertTrue(value instanceof String, "Should remain as String");
        assertEquals("PRC-12345", value, "Should still be the pricingId");
    }
}
