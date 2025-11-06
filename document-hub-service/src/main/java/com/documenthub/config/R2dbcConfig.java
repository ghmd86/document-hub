package com.documenthub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.ArrayList;
import java.util.List;

/**
 * R2DBC configuration for custom converters.
 */
@Configuration
public class R2dbcConfig {

    /**
     * Register custom converters for R2DBC.
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new R2dbcJsonConverters.StringToJsonNodeConverter());
        converters.add(new R2dbcJsonConverters.JsonNodeToStringConverter());
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }

    private org.springframework.data.r2dbc.convert.R2dbcCustomConversions.StoreConversions getStoreConversions() {
        return org.springframework.data.r2dbc.convert.R2dbcCustomConversions.STORE_CONVERSIONS;
    }
}
