package com.documenthub.mock.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock Customer Service API.
 * Provides customer profile, segment, and address information.
 */
@Slf4j
@RestController
@RequestMapping("/customer-service/customers")
public class CustomerServiceController {

    // Mock data for known customers
    private static final Map<String, Map<String, Object>> MOCK_CUSTOMERS = new HashMap<>();

    static {
        // Customer 1: Credit card customer, loyal (8 years), California resident
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("customerId", "880e8400-e29b-41d4-a716-446655440001");
        customer1.put("firstName", "John");
        customer1.put("lastName", "Doe");
        customer1.put("email", "john.doe@example.com");
        customer1.put("customerType", "credit_card");
        customer1.put("segment", "premium");
        customer1.put("tier", "GOLD");
        customer1.put("isVip", false);
        customer1.put("customerSince", Instant.now().minus(8 * 365, ChronoUnit.DAYS).getEpochSecond());

        Map<String, Object> address1 = new HashMap<>();
        address1.put("street", "123 Main Street");
        address1.put("city", "Los Angeles");
        address1.put("state", "CA");
        address1.put("postalCode", "90001");
        address1.put("country", "USA");
        customer1.put("primaryAddress", address1);

        MOCK_CUSTOMERS.put("880e8400-e29b-41d4-a716-446655440001", customer1);

        // Customer 2: Digital banking customer, newer (2 years), New York resident
        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("customerId", "880e8400-e29b-41d4-a716-446655440002");
        customer2.put("firstName", "Jane");
        customer2.put("lastName", "Smith");
        customer2.put("email", "jane.smith@example.com");
        customer2.put("customerType", "digital_banking");
        customer2.put("segment", "standard");
        customer2.put("tier", "SILVER");
        customer2.put("isVip", false);
        customer2.put("customerSince", Instant.now().minus(2 * 365, ChronoUnit.DAYS).getEpochSecond());

        Map<String, Object> address2 = new HashMap<>();
        address2.put("street", "456 Broadway");
        address2.put("city", "New York");
        address2.put("state", "NY");
        address2.put("postalCode", "10001");
        address2.put("country", "USA");
        customer2.put("primaryAddress", address2);

        MOCK_CUSTOMERS.put("880e8400-e29b-41d4-a716-446655440002", customer2);

        // Customer 3: Enterprise customer, very loyal (10 years), San Francisco
        Map<String, Object> customer3 = new HashMap<>();
        customer3.put("customerId", "880e8400-e29b-41d4-a716-446655440003");
        customer3.put("firstName", "Bob");
        customer3.put("lastName", "Johnson");
        customer3.put("email", "bob.johnson@enterprise.com");
        customer3.put("customerType", "credit_card");
        customer3.put("segment", "enterprise");
        customer3.put("tier", "PLATINUM");
        customer3.put("isVip", true);
        customer3.put("customerSince", Instant.now().minus(10 * 365, ChronoUnit.DAYS).getEpochSecond());

        Map<String, Object> address3 = new HashMap<>();
        address3.put("street", "789 Market Street");
        address3.put("city", "San Francisco");
        address3.put("state", "CA");
        address3.put("postalCode", "94102");
        address3.put("country", "USA");
        customer3.put("primaryAddress", address3);

        MOCK_CUSTOMERS.put("880e8400-e29b-41d4-a716-446655440003", customer3);
    }

    /**
     * GET /customer-service/customers/{customerId}/profile
     * Returns complete customer profile including address, segment, and tenure.
     */
    @GetMapping("/{customerId}/profile")
    public ResponseEntity<Map<String, Object>> getCustomerProfile(@PathVariable String customerId) {
        log.info("Mock API: Getting customer profile for customerId: {}", customerId);

        Map<String, Object> customer = MOCK_CUSTOMERS.get(customerId);

        if (customer != null) {
            log.debug("Returning customer profile: {}", customer);
            return ResponseEntity.ok(customer);
        } else {
            // Return default customer for unknown IDs
            log.warn("Customer not found, returning default profile for: {}", customerId);
            Map<String, Object> defaultCustomer = new HashMap<>();
            defaultCustomer.put("customerId", customerId);
            defaultCustomer.put("firstName", "Unknown");
            defaultCustomer.put("lastName", "Customer");
            defaultCustomer.put("customerType", "credit_card");
            defaultCustomer.put("segment", "standard");
            defaultCustomer.put("tier", "BRONZE");
            defaultCustomer.put("customerSince", Instant.now().minus(1 * 365, ChronoUnit.DAYS).getEpochSecond());

            Map<String, Object> defaultAddress = new HashMap<>();
            defaultAddress.put("postalCode", "12345");
            defaultAddress.put("state", "CA");
            defaultCustomer.put("primaryAddress", defaultAddress);

            return ResponseEntity.ok(defaultCustomer);
        }
    }

    /**
     * GET /customer-service/customers/{customerId}/segment
     * Returns customer segment information.
     */
    @GetMapping("/{customerId}/segment")
    public ResponseEntity<Map<String, Object>> getCustomerSegment(@PathVariable String customerId) {
        log.info("Mock API: Getting customer segment for customerId: {}", customerId);

        Map<String, Object> customer = MOCK_CUSTOMERS.getOrDefault(customerId, new HashMap<>());

        Map<String, Object> segmentInfo = new HashMap<>();
        segmentInfo.put("customerId", customerId);
        segmentInfo.put("segment", customer.getOrDefault("segment", "standard"));
        segmentInfo.put("tier", customer.getOrDefault("tier", "BRONZE"));
        segmentInfo.put("isVip", customer.getOrDefault("isVip", false));

        return ResponseEntity.ok(segmentInfo);
    }

    /**
     * GET /customer-service/customers/{customerId}/address
     * Returns customer address information.
     */
    @GetMapping("/{customerId}/address")
    public ResponseEntity<Map<String, Object>> getCustomerAddress(@PathVariable String customerId) {
        log.info("Mock API: Getting customer address for customerId: {}", customerId);

        Map<String, Object> customer = MOCK_CUSTOMERS.get(customerId);

        if (customer != null && customer.containsKey("primaryAddress")) {
            return ResponseEntity.ok((Map<String, Object>) customer.get("primaryAddress"));
        } else {
            Map<String, Object> defaultAddress = new HashMap<>();
            defaultAddress.put("postalCode", "00000");
            defaultAddress.put("state", "XX");
            return ResponseEntity.ok(defaultAddress);
        }
    }
}
