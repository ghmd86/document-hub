package com.documenthub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.Clob;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import reactor.core.publisher.Mono;

/**
 * Converter to read JSON strings from H2 database and convert to JsonNode
 */
@ReadingConverter
public class JsonNodeReadingConverter implements Converter<String, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readTree(source);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON string to JsonNode", e);
        }
    }
}
