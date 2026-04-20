package com.oscaruiz.mycqrs.demo.book.integration.support;

import com.oscaruiz.mycqrs.demo.integration.support.SharedTestContainers;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = SharedTestContainers.POSTGRES;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void truncateWriteSideTables() {
        jdbc.execute("TRUNCATE TABLE book_entity, outbox RESTART IDENTITY CASCADE");
    }
}
