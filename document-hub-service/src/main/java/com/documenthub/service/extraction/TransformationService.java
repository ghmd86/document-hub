package com.documenthub.service.extraction;

import com.documenthub.model.extraction.ExtractionConfig;
import com.documenthub.model.extraction.ExtractionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for applying transformations to extracted data
 */
@Slf4j
@Service
public class TransformationService {

    public Object transform(ExtractionConfig.TransformConfig config, ExtractionContext context) {
        Object sourceValue = context.getVariable(config.getSourceField());
        if (sourceValue == null) {
            return null;
        }

        String transformType = config.getType();

        switch (transformType) {
            case "calculateAge":
                return calculateAge(sourceValue);
            case "ageGroupClassification":
                return classifyAgeGroup(sourceValue, config.getClassifications());
            case "balanceTierClassification":
                return classifyTier(sourceValue, config.getTiers());
            case "uppercase":
                return String.valueOf(sourceValue).toUpperCase();
            case "lowercase":
                return String.valueOf(sourceValue).toLowerCase();
            case "trim":
                return String.valueOf(sourceValue).trim();
            default:
                log.warn("Unknown transformation type: {}", transformType);
                return sourceValue;
        }
    }

    private Integer calculateAge(Object dateOfBirth) {
        try {
            LocalDate dob = LocalDate.parse(String.valueOf(dateOfBirth));
            return Period.between(dob, LocalDate.now()).getYears();
        } catch (Exception e) {
            log.error("Failed to calculate age from: {}", dateOfBirth, e);
            return null;
        }
    }

    private String classifyAgeGroup(Object age, List<ExtractionConfig.TierMapping> classifications) {
        if (age == null || classifications == null) {
            return null;
        }

        int ageValue = ((Number) age).intValue();

        for (ExtractionConfig.TierMapping tier : classifications) {
            int min = ((Number) tier.getMin()).intValue();
            int max = ((Number) tier.getMax()).intValue();
            if (ageValue >= min && ageValue <= max) {
                return tier.getValue();
            }
        }

        return null;
    }

    private String classifyTier(Object value, List<ExtractionConfig.TierMapping> tiers) {
        if (value == null || tiers == null) {
            return null;
        }

        double numValue = ((Number) value).doubleValue();

        for (ExtractionConfig.TierMapping tier : tiers) {
            double min = ((Number) tier.getMin()).doubleValue();
            double max = ((Number) tier.getMax()).doubleValue();
            if (numValue >= min && numValue <= max) {
                return tier.getValue();
            }
        }

        return null;
    }
}
