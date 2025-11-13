package com.example.droolspoc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from Cardholder Agreements API
 *
 * Example API call:
 * GET /api/v1/cardholder-agreements/{pricingId}
 *
 * Response:
 * {
 *   "pricingId": "PRICING456",
 *   "cardholderAgreementsTNCCode": "TNC_GOLD_2024",
 *   "effectiveDate": "2024-01-01",
 *   "version": "1.0"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardholderAgreementResponse {

    @JsonProperty("pricingId")
    private String pricingId;

    @JsonProperty("cardholderAgreementsTNCCode")
    private String cardholderAgreementsTNCCode;  // ⭐ Needed for Drools rules

    @JsonProperty("effectiveDate")
    private String effectiveDate;

    @JsonProperty("version")
    private String version;
}
