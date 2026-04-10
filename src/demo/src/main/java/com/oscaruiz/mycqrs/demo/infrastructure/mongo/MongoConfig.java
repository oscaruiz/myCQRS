package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.oscaruiz.mycqrs.demo.infrastructure.mongo")
public class MongoConfig {
}
