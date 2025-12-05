package com.documenthub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.lang.reflect.Method;

/**
 * Converter to read PostgreSQL JSON type and convert to JsonNode
 * Uses reflection to handle io.r2dbc.postgresql.codec.Json without compile-time dependency
 * This handles the Json type that exists at runtime but not at compile time
 */
@ReadingConverter
public class PostgresJsonToJsonNodeConverter implements Converter<Object, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode convert(Object source) {
        if (source == null) {
            return null;
        }

        try {
            String jsonString = null;

            // Check if this is a PostgreSQL Json type using reflection
            // The class will be io.r2dbc.postgresql.codec.Json or one of its inner classes
            String className = source.getClass().getName();
            if (className.startsWith("io.r2dbc.postgresql.codec.Json")) {
                // Try to call asString() method using reflection
                Method asStringMethod = source.getClass().getMethod("asString");
                jsonString = (String) asStringMethod.invoke(source);
            } else if (source instanceof String) {
                // Handle direct String conversion (for H2 or other databases)
                jsonString = (String) source;
            }

            if (jsonString != null) {
                return objectMapper.readTree(jsonString);
            }

            throw new IllegalArgumentException("Cannot convert type " + className + " to JsonNode");
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JsonNode: " + e.getMessage(), e);
        }
    }
}
