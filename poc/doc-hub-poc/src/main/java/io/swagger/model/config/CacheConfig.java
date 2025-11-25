package io.swagger.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheConfig {
    private Boolean enabled;
    private Integer ttl; // Time to live in seconds
    private String keyPattern;
}
