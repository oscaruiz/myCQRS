package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxWriterIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OutboxWriterIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void successfulCommand_writesOneRowToOutbox() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Happy", "Author"));

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?",
            Integer.class, id);
        assertThat(count).isEqualTo(1);

        String eventType = jdbc.queryForObject(
            "SELECT event_type FROM outbox WHERE aggregate_id = ?",
            String.class, id);
        assertThat(eventType).endsWith("BookCreatedEvent");

        assertThat(bookRepository.findByTitle("Happy")).isPresent();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.application",
            "com.oscaruiz.mycqrs.demo.domain",
            "com.oscaruiz.mycqrs.demo.infrastructure"
    })
    @EnableJpaRepositories(basePackageClasses = SpringDataBookRepository.class)
    @EntityScan(basePackageClasses = BookEntity.class)
    static class TestConfig {
    }
}
