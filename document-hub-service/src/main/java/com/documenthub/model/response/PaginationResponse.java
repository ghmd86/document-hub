package com.documenthub.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination response metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResponse {

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("totalItems")
    private Long totalItems;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("pageNumber")
    private Integer pageNumber;
}
