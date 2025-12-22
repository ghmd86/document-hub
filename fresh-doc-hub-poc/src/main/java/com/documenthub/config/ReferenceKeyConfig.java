package com.documenthub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration for reference key types.
 * Valid types are defined in application.properties and can be updated without code changes.
 */
@Slf4j
@Component
public class ReferenceKeyConfig {

    @Value("${app.reference-key-types:}")
    private String referenceKeyTypesConfig;

    private Set<String> allowedTypes;

    @PostConstruct
    public void init() {
        if (referenceKeyTypesConfig == null || referenceKeyTypesConfig.isBlank()) {
            allowedTypes = Collections.emptySet();
            log.warn("No reference key types configured. Set app.reference-key-types in application.properties");
        } else {
            allowedTypes = Arrays.stream(referenceKeyTypesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            log.info("Loaded {} allowed reference key types: {}", allowedTypes.size(), allowedTypes);
        }
    }

    /**
     * Check if a reference key type is valid
     */
    public boolean isValid(String referenceKeyType) {
        if (referenceKeyType == null || referenceKeyType.isBlank()) {
            return false;
        }
        return allowedTypes.contains(referenceKeyType.trim());
    }

    /**
     * Get all allowed reference key types
     */
    public Set<String> getAllowedTypes() {
        return Collections.unmodifiableSet(allowedTypes);
    }

    /**
     * Get allowed types as comma-separated string for error messages
     */
    public String getAllowedTypesString() {
        return String.join(", ", allowedTypes);
    }
}
