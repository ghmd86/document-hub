package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Central registry that catalogs all available data sources and their field mappings
 * This solves the problem: "We have accountId/customerId, but where do we get disclosureCode?"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceRegistry {
    /**
     * Map of field names to their source configuration
     * Key: field name (e.g., "disclosureCode", "customerLocation")
     * Value: Field source mapping with API details
     */
    private Map<String, FieldSourceMapping> fieldSourceMappings;

    /**
     * Available data sources by ID
     * Key: data source ID (e.g., "accountDetailsApi", "customerProfileApi")
     * Value: Complete data source configuration
     */
    private Map<String, DataSourceConfig> availableDataSources;

    /**
     * Dependencies graph showing which fields depend on others
     * Key: field name
     * Value: list of prerequisite fields needed
     */
    private Map<String, List<String>> fieldDependencies;

    /**
     * Lookup a field's source configuration
     */
    public FieldSourceMapping getFieldSource(String fieldName) {
        return fieldSourceMappings != null ? fieldSourceMappings.get(fieldName) : null;
    }

    /**
     * Get the data source configuration by ID
     */
    public DataSourceConfig getDataSource(String dataSourceId) {
        return availableDataSources != null ? availableDataSources.get(dataSourceId) : null;
    }

    /**
     * Get all fields that can be retrieved from a specific data source
     */
    public List<String> getFieldsFromDataSource(String dataSourceId) {
        if (fieldSourceMappings == null) {
            return List.of();
        }

        return fieldSourceMappings.entrySet().stream()
            .filter(entry -> dataSourceId.equals(entry.getValue().getSourceDataSourceId()))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Check if all required inputs are available for retrieving a field
     */
    public boolean canRetrieveField(String fieldName, Map<String, Object> availableFields) {
        FieldSourceMapping mapping = getFieldSource(fieldName);
        if (mapping == null || mapping.getRequiredInputs() == null) {
            return false;
        }

        return mapping.getRequiredInputs().stream()
            .allMatch(input -> availableFields.containsKey(input) &&
                             availableFields.get(input) != null);
    }
}
