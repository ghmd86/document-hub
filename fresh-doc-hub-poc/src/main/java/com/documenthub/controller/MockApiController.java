package com.documenthub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Mock API Controller for testing multi-step data extraction
 * Simulates external API responses
 */
@RestController
@RequestMapping("/mock-api")
public class MockApiController {

    // ========================================
    // ACCOUNT APIs
    // ========================================

    @GetMapping("/accounts/{accountId}/details")
    public ResponseEntity<Map<String, Object>> getAccountDetails(@PathVariable String accountId) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> account = new HashMap<>();

        account.put("accountId", accountId);
        account.put("accountType", "CHECKING");
        account.put("productCode", "CC-PREMIUM-001");
        account.put("status", "ACTIVE");
        account.put("branchCode", "BR-555");

        Map<String, Object> balance = new HashMap<>();
        balance.put("current", 5432.10);
        balance.put("available", 5432.10);
        account.put("balance", balance);

        response.put("account", account);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccount(@PathVariable String accountId) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> account = new HashMap<>();

        if ("999e8400-e29b-41d4-a716-446655440999".equals(accountId)) {
            // Error scenario
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ACCOUNT_NOT_FOUND");
            error.put("message", "Account with ID " + accountId + " not found");
            return ResponseEntity.status(404).body(error);
        }

        account.put("accountId", accountId);
        account.put("accountType", "PREMIUM_CHECKING");
        account.put("productCode", "CC-PREMIUM-001");
        account.put("status", "ACTIVE");
        account.put("customerId", "123e4567-e89b-12d3-a456-426614174000");
        account.put("branchCode", "BR-555");

        response.put("account", account);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // CUSTOMER APIs
    // ========================================

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable String customerId) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();

        customer.put("customerId", customerId);
        customer.put("firstName", "John");
        customer.put("lastName", "Doe");
        customer.put("tier", "VIP");
        customer.put("email", "john.doe@example.com");

        Map<String, Object> address = new HashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "San Francisco");
        address.put("state", "CA");
        address.put("zipCode", "94102");
        address.put("regionCode", "US-WEST");
        customer.put("address", address);

        response.put("customer", customer);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // PRODUCT APIs
    // ========================================

    @GetMapping("/products/{productCode}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productCode) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> product = new HashMap<>();

        if ("INVALID-CODE".equals(productCode)) {
            // Error scenario
            Map<String, Object> error = new HashMap<>();
            error.put("error", "PRODUCT_NOT_FOUND");
            error.put("message", "Product with code " + productCode + " not found");
            return ResponseEntity.status(404).body(error);
        }

        product.put("productCode", productCode);

        // Different product types
        switch (productCode) {
            case "CC-PREMIUM-001":
                product.put("productName", "Premium Credit Card");
                product.put("category", "CREDIT_CARD");
                product.put("tier", "PREMIUM");
                product.put("features", Arrays.asList("REWARDS", "TRAVEL_INSURANCE", "NO_FOREIGN_FEE"));
                break;
            case "SAV-PREMIUM-001":
                product.put("productName", "Premium Savings Account");
                product.put("category", "SAVINGS");
                product.put("interestRate", 4.5);
                product.put("minimumBalance", 1000);
                break;
            default:
                product.put("productName", "Standard Product");
                product.put("category", "GENERAL");
                break;
        }

        response.put("product", product);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // REGULATORY APIs
    // ========================================

    @GetMapping("/regulatory/requirements")
    public ResponseEntity<Map<String, Object>> getRegulatoryRequirements(@RequestParam String category) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> rules = new HashMap<>();

        rules.put("category", category);

        switch (category) {
            case "CREDIT_CARD":
                rules.put("disclosures", Arrays.asList("TILA", "CARD_ACT", "SCHUMER_BOX"));
                rules.put("minimumAge", 18);
                rules.put("requiredDocuments", Arrays.asList("TERMS_CONDITIONS", "PRIVACY_POLICY"));
                break;
            case "SAVINGS":
                rules.put("disclosures", Arrays.asList("REGULATION_D", "FDIC_NOTICE"));
                rules.put("minimumAge", 18);
                rules.put("requiredDocuments", Arrays.asList("TERMS_CONDITIONS", "PRIVACY_POLICY", "DEPOSIT_AGREEMENT"));
                break;
            default:
                rules.put("disclosures", Arrays.asList("STANDARD_DISCLOSURE"));
                rules.put("minimumAge", 18);
                rules.put("requiredDocuments", Arrays.asList("TERMS_CONDITIONS"));
                break;
        }

        response.put("rules", rules);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/regions/map")
    public ResponseEntity<Map<String, Object>> mapRegion(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mapping = new HashMap<>();

        String state = request.get("state");
        mapping.put("state", state);

        // Map states to regions
        switch (state) {
            case "CA":
            case "OR":
            case "WA":
                mapping.put("region", "US_WEST");
                mapping.put("timezone", "PST");
                mapping.put("regulatoryBody", "CFPB_WEST");
                break;
            case "NY":
            case "NJ":
            case "MA":
                mapping.put("region", "US_EAST");
                mapping.put("timezone", "EST");
                mapping.put("regulatoryBody", "CFPB_EAST");
                break;
            case "TX":
            case "LA":
            case "OK":
                mapping.put("region", "US_SOUTH");
                mapping.put("timezone", "CST");
                mapping.put("regulatoryBody", "CFPB_SOUTH");
                break;
            default:
                mapping.put("region", "US_CENTRAL");
                mapping.put("timezone", "CST");
                mapping.put("regulatoryBody", "CFPB_CENTRAL");
                break;
        }

        response.put("mapping", mapping);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disclosures")
    public ResponseEntity<Map<String, Object>> getDisclosures(
        @RequestParam String category,
        @RequestParam String region
    ) {
        Map<String, Object> response = new HashMap<>();
        List<String> disclosures = new ArrayList<>();

        // Base disclosures by category
        if ("CREDIT_CARD".equals(category)) {
            disclosures.add("TILA");
            disclosures.add("CARD_ACT");
        } else if ("SAVINGS".equals(category)) {
            disclosures.add("REGULATION_D");
            disclosures.add("FDIC_NOTICE");
        }

        // Regional disclosures
        if ("US_WEST".equals(region)) {
            disclosures.add("CA_DISCLOSURE");
            disclosures.add("CCPA");
        } else if ("US_EAST".equals(region)) {
            disclosures.add("NY_DFS_DISCLOSURE");
        }

        response.put("disclosures", disclosures);
        response.put("effectiveDate", "2024-01-01");
        response.put("region", region);
        response.put("category", category);

        return ResponseEntity.ok(response);
    }

    // ========================================
    // BRANCH APIs
    // ========================================

    @GetMapping("/branches/{branchCode}")
    public ResponseEntity<Map<String, Object>> getBranch(@PathVariable String branchCode) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> branch = new HashMap<>();

        branch.put("branchCode", branchCode);

        switch (branchCode) {
            case "BR-555":
                branch.put("branchName", "Downtown San Francisco");
                branch.put("regionCode", "WEST");
                branch.put("managerName", "Jane Smith");
                Map<String, Object> address1 = new HashMap<>();
                address1.put("city", "San Francisco");
                address1.put("state", "CA");
                branch.put("address", address1);
                break;
            case "BR-222":
                branch.put("branchName", "Manhattan Central");
                branch.put("regionCode", "EAST");
                branch.put("managerName", "Robert Johnson");
                Map<String, Object> address2 = new HashMap<>();
                address2.put("city", "New York");
                address2.put("state", "NY");
                branch.put("address", address2);
                break;
            default:
                branch.put("branchName", "General Branch");
                branch.put("regionCode", "CENTRAL");
                branch.put("managerName", "Unknown");
                break;
        }

        response.put("branch", branch);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // COMPLIANCE APIs
    // ========================================

    @GetMapping("/compliance/regions/{regionCode}")
    public ResponseEntity<Map<String, Object>> getComplianceRules(@PathVariable String regionCode) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> compliance = new HashMap<>();

        if ("INVALID".equals(regionCode)) {
            // Error scenario
            Map<String, Object> error = new HashMap<>();
            error.put("error", "REGION_NOT_FOUND");
            error.put("message", "Region " + regionCode + " not configured");
            return ResponseEntity.status(404).body(error);
        }

        compliance.put("regionCode", regionCode);

        Map<String, Object> rules = new HashMap<>();
        rules.put("dataRetention", "7_YEARS");
        rules.put("privacyStandard", regionCode.equals("WEST") ? "CCPA" : "STANDARD");
        rules.put("disclosureFrequency", "QUARTERLY");
        rules.put("requiresNotarization", false);
        rules.put("electronicSignatureAllowed", true);

        compliance.put("rules", rules);
        compliance.put("lastUpdated", "2024-01-01");

        response.put("compliance", compliance);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/document-rules")
    public ResponseEntity<Map<String, Object>> getDocumentRules(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> rules = (Map<String, Object>) request.get("rules");

        List<String> documents = new ArrayList<>();
        documents.add("PRIVACY_NOTICE");
        documents.add("DATA_RETENTION_POLICY");

        if (rules != null && "QUARTERLY".equals(rules.get("disclosureFrequency"))) {
            documents.add("QUARTERLY_DISCLOSURE");
        }

        if (rules != null && "CCPA".equals(rules.get("privacyStandard"))) {
            documents.add("CCPA_RIGHTS_NOTICE");
        }

        response.put("documents", documents);
        response.put("mandatory", true);
        response.put("deliveryMethod", "ELECTRONIC");

        return ResponseEntity.ok(response);
    }
}
