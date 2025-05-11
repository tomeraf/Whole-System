package com.halilovindustries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
        "Service",
        "Domain.Adapters_and_Interfaces",
        "Domain.Repositories",
        "Infrastructure"
})
public class RestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);


    }
}
