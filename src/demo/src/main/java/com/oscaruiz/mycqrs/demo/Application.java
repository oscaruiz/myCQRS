package com.oscaruiz.mycqrs.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

// TODO - SPRING FACTOIRES / org.springframework.boot.autoconfigure.AutoConfiguration.imports.
@SpringBootApplication(scanBasePackages = {
        "com.oscaruiz.mycqrs.demo",
        "com.oscaruiz.mycqrs.core"
})
@EnableMongoRepositories(basePackages = "com.oscaruiz.mycqrs.demo.infrastructure.mongo")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
