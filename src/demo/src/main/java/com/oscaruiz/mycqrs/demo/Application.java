package com.oscaruiz.mycqrs.demo;

import com.oscaruiz.mycqrs.core.spring.EnableCqrs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCqrs
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
