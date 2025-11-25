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
public class RuleGroup {
    private String groupId;
    private List<String> rules; // References to rule IDs
    private String logic; // AND or OR
}
