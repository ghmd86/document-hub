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
public class DataSourceConfig {
    private String id;
    private String description;
    private EndpointConfig endpoint;
    private CacheConfig cache;
    private ResponseMapping responseMapping;
    private List<String> dependencies;
    private List<NextCall> nextCalls;
    private ErrorHandlingConfig errorHandling;
}
