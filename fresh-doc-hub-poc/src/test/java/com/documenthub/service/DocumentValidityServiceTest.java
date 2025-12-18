package com.documenthub.service;

import com.documenthub.entity.StorageIndexEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentValidityServiceTest {

    private DocumentValidityService validityService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validityService = new DocumentValidityService(objectMapper);
    }

    @Nested
    @DisplayName("filterByValidity Tests")
    class FilterByValidityTests {

        @Test
        @DisplayName("Should return all documents when all are valid")
        void shouldReturnAllDocuments_whenAllValid() {
            // Given
            LocalDate today = LocalDate.now();
            StorageIndexEntity doc1 = createDocumentWithValidity(
                    today.minusDays(10).toString(), today.plusDays(10).toString());
            StorageIndexEntity doc2 = createDocumentWithValidity(
                    today.minusDays(5).toString(), today.plusDays(5).toString());

            // When
            List<StorageIndexEntity> result = validityService.filterByValidity(
                    Arrays.asList(doc1, doc2));

            // Then
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should filter out expired documents")
        void shouldFilterOutExpiredDocuments() {
            // Given
            LocalDate today = LocalDate.now();
            StorageIndexEntity validDoc = createDocumentWithValidity(
                    today.minusDays(10).toString(), today.plusDays(10).toString());
            StorageIndexEntity expiredDoc = createDocumentWithValidity(
                    today.minusDays(30).toString(), today.minusDays(1).toString());

            // When
            List<StorageIndexEntity> result = validityService.filterByValidity(
                    Arrays.asList(validDoc, expiredDoc));

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter out not-yet-valid documents")
        void shouldFilterOutNotYetValidDocuments() {
            // Given
            LocalDate today = LocalDate.now();
            StorageIndexEntity validDoc = createDocumentWithValidity(
                    today.minusDays(10).toString(), today.plusDays(10).toString());
            StorageIndexEntity futureDoc = createDocumentWithValidity(
                    today.plusDays(1).toString(), today.plusDays(30).toString());

            // When
            List<StorageIndexEntity> result = validityService.filterByValidity(
                    Arrays.asList(validDoc, futureDoc));

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should include documents with no validity dates")
        void shouldIncludeDocumentsWithNoValidityDates() {
            // Given
            StorageIndexEntity docWithNoValidity = createStorageEntity();
            docWithNoValidity.setDocMetadata(Json.of("{}"));

            // When
            List<StorageIndexEntity> result = validityService.filterByValidity(
                    Arrays.asList(docWithNoValidity));

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should include documents with null metadata")
        void shouldIncludeDocumentsWithNullMetadata() {
            // Given
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(null);

            // When
            List<StorageIndexEntity> result = validityService.filterByValidity(
                    Arrays.asList(doc));

            // Then
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("isDocumentValid Tests")
    class IsDocumentValidTests {

        @Test
        @DisplayName("Should return true when document has no metadata")
        void shouldReturnTrue_whenNoMetadata() {
            // Given
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(null);

            // When
            boolean result = validityService.isDocumentValid(doc, LocalDate.now());

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when within validity period")
        void shouldReturnTrue_whenWithinValidityPeriod() {
            // Given
            LocalDate today = LocalDate.now();
            StorageIndexEntity doc = createDocumentWithValidity(
                    today.minusDays(5).toString(), today.plusDays(5).toString());

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when document expired")
        void shouldReturnFalse_whenDocumentExpired() {
            // Given
            LocalDate today = LocalDate.now();
            StorageIndexEntity doc = createDocumentWithValidity(
                    today.minusDays(30).toString(), today.minusDays(1).toString());

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when document not yet valid")
        void shouldReturnFalse_whenDocumentNotYetValid() {
            // Given
            LocalDate today = LocalDate.now();
            StorageIndexEntity doc = createDocumentWithValidity(
                    today.plusDays(1).toString(), today.plusDays(30).toString());

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return true when only valid_from is set and passed")
        void shouldReturnTrue_whenOnlyValidFromSetAndPassed() {
            // Given
            LocalDate today = LocalDate.now();
            String metadata = "{\"valid_from\":\"" + today.minusDays(5).toString() + "\"}";
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(Json.of(metadata));

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when only valid_until is set and not passed")
        void shouldReturnTrue_whenOnlyValidUntilSetAndNotPassed() {
            // Given
            LocalDate today = LocalDate.now();
            String metadata = "{\"valid_until\":\"" + today.plusDays(5).toString() + "\"}";
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(Json.of(metadata));

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle alternative field names - validFrom")
        void shouldHandleAlternativeFieldName_validFrom() {
            // Given
            LocalDate today = LocalDate.now();
            String metadata = "{\"validFrom\":\"" + today.minusDays(5).toString() + "\"}";
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(Json.of(metadata));

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle alternative field names - effective_date")
        void shouldHandleAlternativeFieldName_effectiveDate() {
            // Given
            LocalDate today = LocalDate.now();
            String metadata = "{\"effective_date\":\"" + today.minusDays(5).toString() + "\"}";
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(Json.of(metadata));

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle alternative field names - expiry_date")
        void shouldHandleAlternativeFieldName_expiryDate() {
            // Given
            LocalDate today = LocalDate.now();
            String metadata = "{\"expiry_date\":\"" + today.plusDays(5).toString() + "\"}";
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(Json.of(metadata));

            // When
            boolean result = validityService.isDocumentValid(doc, today);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when invalid JSON in metadata")
        void shouldReturnTrue_whenInvalidJsonInMetadata() {
            // Given
            StorageIndexEntity doc = createStorageEntity();
            doc.setDocMetadata(Json.of("invalid json"));

            // When
            boolean result = validityService.isDocumentValid(doc, LocalDate.now());

            // Then
            assertTrue(result); // Should default to valid on error
        }
    }

    @Nested
    @DisplayName("parseDate Tests")
    class ParseDateTests {

        @Test
        @DisplayName("Should parse ISO date format")
        void shouldParseIsoDate() {
            // When
            LocalDate result = validityService.parseDate("2024-06-15");

            // Then
            assertEquals(LocalDate.of(2024, 6, 15), result);
        }

        @Test
        @DisplayName("Should parse US date format")
        void shouldParseUsDate() {
            // When
            LocalDate result = validityService.parseDate("06/15/2024");

            // Then
            assertEquals(LocalDate.of(2024, 6, 15), result);
        }

        @Test
        @DisplayName("Should parse epoch milliseconds")
        void shouldParseEpochMillis() {
            // Given
            LocalDate expected = LocalDate.of(2024, 6, 15);
            long epochMillis = expected.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            // When
            LocalDate result = validityService.parseDate(String.valueOf(epochMillis));

            // Then
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNull_forNullInput() {
            assertNull(validityService.parseDate(null));
        }

        @Test
        @DisplayName("Should return null for empty string")
        void shouldReturnNull_forEmptyString() {
            assertNull(validityService.parseDate(""));
        }

        @Test
        @DisplayName("Should return null for unparseable date")
        void shouldReturnNull_forUnparseableDate() {
            assertNull(validityService.parseDate("not-a-date"));
        }

        @Test
        @DisplayName("Should return null for invalid format")
        void shouldReturnNull_forInvalidFormat() {
            assertNull(validityService.parseDate("15-06-2024"));
        }
    }

    // Helper methods
    private StorageIndexEntity createStorageEntity() {
        StorageIndexEntity entity = new StorageIndexEntity();
        entity.setStorageIndexId(UUID.randomUUID());
        entity.setStorageDocumentKey(UUID.randomUUID());
        entity.setFileName("test-file.pdf");
        return entity;
    }

    private StorageIndexEntity createDocumentWithValidity(String validFrom, String validUntil) {
        StorageIndexEntity entity = createStorageEntity();
        String metadata = String.format("{\"valid_from\":\"%s\",\"valid_until\":\"%s\"}",
                validFrom, validUntil);
        entity.setDocMetadata(Json.of(metadata));
        return entity;
    }
}
