package com.oscaruiz.mycqrs.demo.integration.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;

public abstract class AbstractFullStackIntegrationTest {

    @ServiceConnection
    protected static final MongoDBContainer MONGO = SharedTestContainers.MONGO;

    @Autowired
    private MongoTemplate mongoTemplate;

    @AfterEach
    void cleanReadSide() {
        mongoTemplate.dropCollection("books");
        mongoTemplate.dropCollection("book_events");
    }
}
