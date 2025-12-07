package com.documenthub.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.util.ArrayList;
import java.util.List;

/**
 * R2DBC Database Configuration
 * Supports both H2 and PostgreSQL with automatic detection
 */
@Configuration
public class DatabaseConfig {

    /**
     * Register custom converters for JSON handling
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        List<Converter<?, ?>> converters = new ArrayList<>();
        // No custom converters needed when using io.r2dbc.postgresql.codec.Json directly
        return R2dbcCustomConversions.of(dialect, converters);
    }

    /**
     * Initialize database schema and data on startup
     * Automatically detects database type and loads appropriate scripts
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

//        // Detect database type and load appropriate scripts
//        if (r2dbcUrl.contains("postgresql")) {
//            System.out.println("=== Detected PostgreSQL - Loading PostgreSQL scripts ===");
//            populator.addScript(new ClassPathResource("schema-postgres.sql"));
//            populator.addScript(new ClassPathResource("test-data-postgres.sql"));
//        } else {
//            System.out.println("=== Detected H2 - Loading H2 scripts ===");
//            populator.addScript(new ClassPathResource("schema.sql"));
//            populator.addScript(new ClassPathResource("data.sql"));
//        }

        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
