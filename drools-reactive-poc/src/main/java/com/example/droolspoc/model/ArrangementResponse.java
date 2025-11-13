package com.example.droolspoc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from Arrangements API
 *
 * Example API call:
 * GET /api/v1/arrangements/{arrangementId}
 *
 * Response:
 * {
 *   "arrangementId": "ARR123",
 *   "pricingId": "PRICING456",
 *   "productCode": "GOLD_CARD",
 *   "status": "ACTIVE"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementResponse {

    @JsonProperty("arrangementId")
    private String arrangementId;

    @JsonProperty("pricingId")
    private String pricingId;        // ⭐ Used for next API call

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("status")
    private String status;
}
