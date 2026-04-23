package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.observability.CorrelationIdMdc;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import com.oscaruiz.mycqrs.demo.shared.infrastructure.observability.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CorrelationIdEndToEndIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class CorrelationIdEndToEndIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private MdcCapturingHandler mdcCapturingHandler;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilter(new CorrelationIdFilter(), "/*")
            .build();
    }

    @AfterEach
    void resetHandler() {
        mdcCapturingHandler.reset();
        MDC.clear();
    }

    @Test
    void client_supplied_correlation_id_flows_through_http_outbox_and_poller() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(put("/books/" + bookId)
                .header(CorrelationIdFilter.HEADER, correlationId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"E2E Trace Target\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string(CorrelationIdFilter.HEADER, correlationId.toString()))
            .andReturn();

        String storedCorrelationId = jdbcTemplate.queryForObject(
            "SELECT correlation_id FROM outbox WHERE aggregate_id = ?",
            String.class, bookId.toString()
        );
        assertThat(storedCorrelationId)
            .as("outbox row must carry the same correlation id the HTTP request arrived with")
            .isEqualTo(correlationId.toString());

        outboxPoller.poll();

        assertThat(mdcCapturingHandler.capturedCorrelationId())
            .as("projector must run with the original correlation id restored to MDC")
            .isEqualTo(correlationId.toString());

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
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
    @Import(CorrelationIdEndToEndIntegrationTest.HandlerConfig.class)
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
