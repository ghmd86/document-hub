package com.example.droolspoc.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Document Eligibility Result - Output from Drools rules
 *
 * Drools rules will call addEligibleDocument() to add documents
 * that the customer/account is eligible for.
 */
@Data
public class DocumentEligibilityResult {

    private Set<String> eligibleDocumentIds = new HashSet<>();

    /**
     * Add an eligible document (called by Drools rules)
     */
    public void addEligibleDocument(String documentId) {
        eligibleDocumentIds.add(documentId);
    }

    /**
     * Check if any documents are eligible
     */
    public boolean hasEligibleDocuments() {
        return !eligibleDocumentIds.isEmpty();
    }

    /**
     * Get count of eligible documents
     */
    public int getEligibleCount() {
        return eligibleDocumentIds.size();
    }
}
