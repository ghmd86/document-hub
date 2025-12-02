package com.documenthub.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.util.ArrayList;
import java.util.List;

/**
 * R2DBC Database Configuration
 * Initializes H2 database with schema and data
 */
@Configuration
public class DatabaseConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    /**
     * Register custom converters for JSON handling
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new JsonNodeReadingConverter());
        converters.add(new JsonNodeWritingConverter());
        return R2dbcCustomConversions.of(dialect, converters);
    }

    /**
     * Initialize database schema and data on startup
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.addScript(new ClassPathResource("data.sql"));

        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
