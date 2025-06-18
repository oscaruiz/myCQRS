package com.oscaruiz.mycqrs.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// TODO - SPRING FACTOIRES / org.springframework.boot.autoconfigure.AutoConfiguration.imports.
@SpringBootApplication(scanBasePackages = {
        "com.oscaruiz.mycqrs.demo",
        "com.oscaruiz.mycqrs.core"
})

public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
