package com.documenthub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

/**
 * Custom converters for R2DBC JSON handling with PostgreSQL.
 */
@Slf4j
public class R2dbcJsonConverters {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converter from String to Jackson JsonNode for reading JSON columns.
     */
    @ReadingConverter
    public static class StringToJsonNodeConverter implements Converter<String, JsonNode> {

        @Override
        public JsonNode convert(@NonNull String source) {
            try {
                return objectMapper.readTree(source);
            } catch (Exception e) {
                log.error("Failed to convert String to JsonNode", e);
                return null;
            }
        }
    }

    /**
     * Converter from Jackson JsonNode to String for writing JSON columns.
     */
    @WritingConverter
    public static class JsonNodeToStringConverter implements Converter<JsonNode, String> {

        @Override
        public String convert(@NonNull JsonNode source) {
            try {
                return objectMapper.writeValueAsString(source);
            } catch (Exception e) {
                log.error("Failed to convert JsonNode to String", e);
                return null;
            }
        }
    }
}
