package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMatchingStrategy {
    private String matchBy; // reference_key, metadata_fields, composite, account_key
    private String referenceKeyType;
    private String referenceKeySource;
    private String strategy; // ALL_CONDITIONS_MATCH, ANY_CONDITIONS_MATCH
    private List<MatchingCondition> conditions;
    private String selectionStrategy; // LATEST_VERSION, LATEST_BY_DATE, ALL_MATCHES
    private List<MatchingCondition> filters;
}
