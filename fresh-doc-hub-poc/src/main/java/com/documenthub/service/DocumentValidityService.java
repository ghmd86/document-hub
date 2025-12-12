package com.documenthub.service;

import com.documenthub.entity.StorageIndexEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for document validity period checking.
 * Filters documents based on valid_from/valid_until dates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentValidityService {

    private final ObjectMapper objectMapper;

    private static final String[] START_DATE_FIELDS = {
            "valid_from", "validFrom", "effective_date", "effectiveDate"
    };

    private static final String[] END_DATE_FIELDS = {
            "valid_until", "validUntil", "expiry_date", "expiryDate"
    };

    /**
     * Filter documents by validity period.
     */
    public List<StorageIndexEntity> filterByValidity(
            List<StorageIndexEntity> documents) {

        LocalDate today = LocalDate.now();

        return documents.stream()
                .filter(doc -> isDocumentValid(doc, today))
                .collect(Collectors.toList());
    }

    /**
     * Check if a document is currently valid.
     */
    public boolean isDocumentValid(StorageIndexEntity document, LocalDate today) {
        if (document.getDocMetadata() == null) {
            return true;
        }

        try {
            JsonNode metadata = objectMapper.readTree(
                    document.getDocMetadata().asString());

            return isWithinValidityPeriod(document, metadata, today);
        } catch (Exception e) {
            log.warn("Failed to check validity for document {}: {}",
                    document.getStorageIndexId(), e.getMessage());
            return true;
        }
    }

    private boolean isWithinValidityPeriod(
            StorageIndexEntity document,
            JsonNode metadata,
            LocalDate today) {

        LocalDate validFrom = extractDate(metadata, START_DATE_FIELDS);
        if (validFrom != null && today.isBefore(validFrom)) {
            logNotYetValid(document, validFrom, today);
            return false;
        }

        LocalDate validUntil = extractDate(metadata, END_DATE_FIELDS);
        if (validUntil != null && today.isAfter(validUntil)) {
            logExpired(document, validUntil, today);
            return false;
        }

        return true;
    }

    private void logNotYetValid(
            StorageIndexEntity doc,
            LocalDate validFrom,
            LocalDate today) {

        log.debug("Document {} not yet valid (from: {}, today: {})",
                doc.getStorageIndexId(), validFrom, today);
    }

    private void logExpired(
            StorageIndexEntity doc,
            LocalDate validUntil,
            LocalDate today) {

        log.debug("Document {} expired (until: {}, today: {})",
                doc.getStorageIndexId(), validUntil, today);
    }

    private LocalDate extractDate(JsonNode metadata, String[] fieldNames) {
        for (String fieldName : fieldNames) {
            LocalDate date = tryExtractDate(metadata, fieldName);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private LocalDate tryExtractDate(JsonNode metadata, String fieldName) {
        if (!metadata.has(fieldName) || metadata.get(fieldName).isNull()) {
            return null;
        }

        String dateStr = metadata.get(fieldName).asText();
        return parseDate(dateStr);
    }

    /**
     * Parse date string supporting multiple formats.
     */
    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        LocalDate result = tryParseIsoDate(dateStr);
        if (result != null) return result;

        result = tryParseUsDate(dateStr);
        if (result != null) return result;

        result = tryParseEpochMillis(dateStr);
        if (result != null) return result;

        log.warn("Could not parse date: {}", dateStr);
        return null;
    }

    private LocalDate tryParseIsoDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDate tryParseUsDate(String dateStr) {
        try {
            DateTimeFormatter usFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            return LocalDate.parse(dateStr, usFormat);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDate tryParseEpochMillis(String dateStr) {
        try {
            long epochMillis = Long.parseLong(dateStr);
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
