package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxPollerIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OutboxPollerIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CreatedEventRecorder createdEventRecorder;

    @BeforeEach
    void cleanState() {
        createdEventRecorder.clear();
    }

    @Test
    void successfulDispatch_marksProcessedAt() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Poller Happy", "Author"));

        outboxPoller.poll();

        Integer processedCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ? AND processed_at IS NOT NULL",
            Integer.class, id);
        assertThat(processedCount).isEqualTo(1);

        assertThat(createdEventRecorder.events()).hasSize(1);
        assertThat(createdEventRecorder.events().get(0).getAggregateId()).isEqualTo(id);
    }

    @Test
    void failingHandler_incrementsAttemptsAndStoresError() {
        String id = UUID.randomUUID().toString();
        createdEventRecorder.failOnNext("simulated projection failure");

        commandBus.send(new CreateBookCommand(id, "Poller Sad", "Author"));

        outboxPoller.poll();

        Integer attempts = jdbc.queryForObject(
            "SELECT attempts FROM outbox WHERE aggregate_id = ?",
            Integer.class, id);
        assertThat(attempts).isEqualTo(1);

        String lastError = jdbc.queryForObject(
            "SELECT last_error FROM outbox WHERE aggregate_id = ?",
            String.class, id);
        assertThat(lastError).contains("simulated projection failure");

        // processed_at should still be NULL — row is pending for retry
        Integer processedCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ? AND processed_at IS NULL",
            Integer.class, id);
        assertThat(processedCount).isEqualTo(1);
    }

    @Test
    void secondPoll_skipsAlreadyProcessedRow() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Poller Once", "Author"));

        outboxPoller.poll();
        assertThat(createdEventRecorder.events()).hasSize(1);

        outboxPoller.poll();
        // handler must NOT be invoked a second time — row was already processed
        assertThat(createdEventRecorder.events()).hasSize(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.book.application",
            "com.oscaruiz.mycqrs.demo.book.domain",
            "com.oscaruiz.mycqrs.demo.book.infrastructure"
    })
    @EnableJpaRepositories(basePackageClasses = SpringDataBookRepository.class)
    @EntityScan(basePackageClasses = BookEntity.class)
    @Import(OutboxPollerIntegrationTest.RecorderConfig.class)
    static class TestConfig {
    }

    @TestConfiguration
    static class RecorderConfig {
        @Bean
        CreatedEventRecorder createdEventRecorder() {
            return new CreatedEventRecorder();
        }
    }

    static class CreatedEventRecorder implements EventHandler<BookCreatedEvent> {

        private final List<BookCreatedEvent> events = new ArrayList<>();
        private String failMessage;

        @Override
        public void handle(BookCreatedEvent event) {
            if (failMessage != null) {
                String msg = failMessage;
                failMessage = null;
                throw new RuntimeException(msg);
            }
            events.add(event);
        }

        List<BookCreatedEvent> events() {
            return events;
        }

        void failOnNext(String message) {
            this.failMessage = message;
        }

        void clear() {
            events.clear();
            failMessage = null;
        }
    }
}
