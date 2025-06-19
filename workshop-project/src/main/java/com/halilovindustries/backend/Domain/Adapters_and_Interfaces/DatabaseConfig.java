package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
@Import({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DatabaseConfig {
    // This class allows conditional database initialization
}