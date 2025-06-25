package com.halilovindustries;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@Push
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableAspectJAutoProxy
@EnableScheduling
@EnableConfigurationProperties
@Theme(value = "my-app")
public class Application implements AppShellConfigurator {
    
    @Bean
    public CommandLineRunner initDatabaseHealth(DatabaseHealthService databaseHealthService) {
        return args -> {
            // Check database connection at startup
            boolean connected = databaseHealthService.isDatabaseConnected();
            System.out.println("Initial database health check: " + 
                              (connected ? "Connected" : "Maintenance mode"));
        };
    }
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}