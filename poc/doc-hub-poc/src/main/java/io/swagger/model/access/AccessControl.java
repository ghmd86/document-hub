package io.swagger.model.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessControl {
    private String eligibilityType; // ACCOUNT_OWNED, SHARED_WITH_ALL, SHARED_BY_ACCOUNT_TYPE, CONDITIONAL
    private Map<String, EligibilityCriteria> eligibilityCriteria;
    private String logic; // AND or OR
    private List<RuleGroup> ruleGroups;
    private String groupLogic; // AND or OR
}
