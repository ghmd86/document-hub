package com.documenthub.integration.ecms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ECMS client
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ecms")
public class EcmsClientConfig {

    private String baseUrl = "https://devapi.vda.dv01.c1busw2.aws/private/enterprise/ecms";

    private String apiKey;

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 30000;

    private int maxRetries = 3;
}
