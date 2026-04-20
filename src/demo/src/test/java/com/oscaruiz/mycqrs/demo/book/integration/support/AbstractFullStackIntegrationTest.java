package com.oscaruiz.mycqrs.demo.book.integration.support;

import com.oscaruiz.mycqrs.demo.integration.support.SharedTestContainers;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractFullStackIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = SharedTestContainers.POSTGRES;

    @ServiceConnection
    protected static final MongoDBContainer MONGO = SharedTestContainers.MONGO;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @AfterEach
    void cleanAll() {
        // Order is independent — no FK or cross-store dependency.
        // Stable ordering for log readability only.
        jdbc.execute("TRUNCATE TABLE book_entity, outbox RESTART IDENTITY CASCADE");
        mongoTemplate.dropCollection("books");
        mongoTemplate.dropCollection("book_events");
    }
}
