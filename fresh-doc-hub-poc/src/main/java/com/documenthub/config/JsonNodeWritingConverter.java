package com.documenthub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converter to write JsonNode as JSON string to H2 database
 */
@WritingConverter
public class JsonNodeWritingConverter implements Converter<JsonNode, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convert(JsonNode source) {
        if (source == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JsonNode to JSON string", e);
        }
    }
}
