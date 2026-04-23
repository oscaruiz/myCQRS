package com.oscaruiz.mycqrs.demo.book.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.observability.CorrelationIdMdc;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxPollerCorrelationIdIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OutboxPollerCorrelationIdIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MdcCapturingHandler mdcCapturingHandler;

    @AfterEach
    void resetHandler() {
        mdcCapturingHandler.reset();
        MDC.clear();
    }

    @Test
    void propagates_correlation_id_from_outbox_row_to_mdc() throws Exception {
        UUID correlationId = UUID.randomUUID();
        String aggregateId = UUID.randomUUID().toString();
        BookCreatedEvent event = new BookCreatedEvent(aggregateId, "Traceable Book");
        String payload = objectMapper.writeValueAsString(event);

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", UUID.fromString(event.getEventId()))
            .addValue("aggregateId", aggregateId)
            .addValue("eventType", BookCreatedEvent.class.getName())
            .addValue("payload", payload)
            .addValue("occurredAt", Timestamp.from(Instant.now()))
            .addValue("correlationId", correlationId);
        jdbc.update("""
            INSERT INTO outbox (id, aggregate_id, event_type, payload, occurred_at, correlation_id)
            VALUES (:id, :aggregateId, :eventType, :payload, :occurredAt, :correlationId)
            """, params);

        outboxPoller.poll();

        assertThat(mdcCapturingHandler.capturedCorrelationId())
            .as("poller must restore correlation id to MDC before invoking projector")
            .isEqualTo(correlationId.toString());
        assertThat(MDC.get(CorrelationIdMdc.KEY))
            .as("poller must clear MDC after dispatching the row")
            .isNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.book.application",
            "com.oscaruiz.mycqrs.demo.book.domain",
            "com.oscaruiz.mycqrs.demo.book.infrastructure",
            "com.oscaruiz.mycqrs.demo.author.infrastructure.jpa",
            "com.oscaruiz.mycqrs.demo.author.infrastructure.mongo"
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataBookRepository.class,
        com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository.class
    })
    @EntityScan(basePackageClasses = {
        BookEntity.class,
        com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity.class
    })
    @Import(OutboxPollerCorrelationIdIntegrationTest.HandlerConfig.class)
    static class TestConfig {
    }

    @TestConfiguration
    static class HandlerConfig {
        @Bean
        MdcCapturingHandler mdcCapturingHandler() {
            return new MdcCapturingHandler();
        }
    }

    static class MdcCapturingHandler implements EventHandler<BookCreatedEvent> {

        private final AtomicReference<String> captured = new AtomicReference<>();

        @Override
        public void handle(BookCreatedEvent event) {
            captured.set(MDC.get(CorrelationIdMdc.KEY));
        }

        String capturedCorrelationId() {
            return captured.get();
        }

        void reset() {
            captured.set(null);
        }
    }
}
