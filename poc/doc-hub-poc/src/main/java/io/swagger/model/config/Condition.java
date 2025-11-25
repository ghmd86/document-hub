package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Condition {
    private String field;
    private String operator; // notNull, equals, greaterThan, lessThan, in
    private Object value;
}
