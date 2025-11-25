package io.swagger.model.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCriteria {
    private String type; // ACCOUNT_TYPE, REGION, SEGMENT, TENURE, ACCOUNT_STATUS, DISCLOSURE_CODE, etc.
    private String field;
    private String operator; // IN_LIST, EQUALS, GREATER_THAN, LESS_THAN, CONTAINS
    private List<String> values;
    private Object value;
    private String dataSource;
    private Boolean computed;
    private String computation;
}
