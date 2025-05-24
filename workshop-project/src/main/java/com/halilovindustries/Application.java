package com.halilovindustries;

import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.halilovindustries.backend.Domain.Repositories")
@EntityScan("com.halilovindustries.backend.Domain.DTOs")
@Theme(value = "my-app")
public class Application implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

// @Bean
// CommandLineRunner init(UserService userService) {
//     return args -> {
//         String token1 = userService.enterToSystem().getData();
//         userService.registerUser(token1, "Lavi1", "Password", LocalDate.of(2000, 1, 1));
//         String token2 = userService.logoutRegistered(token1).getData();
//         userService.registerUser(token2, "Lavi2", "Password1", LocalDate.of(2000, 1, 1));
//         String token3 = userService.logoutRegistered(token2).getData();
//         userService.registerUser(token3, "Lavi3", "Password2", LocalDate.of(2000, 1, 1));
//         String token4 = userService.logoutRegistered(token3).getData();
//         userService.registerUser(token4, "Lavi4", "Password4", LocalDate.of(2000, 1, 1));
//         String token5 = userService.logoutRegistered(token4).getData();
//         userService.registerUser(token5, "Lavi5", "Password5", LocalDate.of(2000, 1, 1));
        
//     };
// }
}
