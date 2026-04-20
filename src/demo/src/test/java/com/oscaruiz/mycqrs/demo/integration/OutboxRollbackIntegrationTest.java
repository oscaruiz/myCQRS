package com.oscaruiz.mycqrs.demo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerBeanPostProcessor;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.outbox.OutboxConfig;
import com.oscaruiz.mycqrs.demo.infrastructure.outbox.OutboxEventBus;
import com.oscaruiz.mycqrs.demo.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OutboxRollbackIntegrationTest.RollbackConfig.class)
@ActiveProfiles("test")
class OutboxRollbackIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void cleanOutbox() {
        jdbc.execute("TRUNCATE TABLE outbox");
    }

    @Test
    void whenOutboxWriteFails_aggregateSaveIsRolledBack() {
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() ->
            commandBus.send(new CreateBookCommand(id, "Doomed", "Author"))
        ).isInstanceOf(RuntimeException.class);

        Integer outboxCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?",
            Integer.class, id);
        assertThat(outboxCount).isZero();

        assertThat(bookRepository.findByTitle("Doomed")).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.application",
            "com.oscaruiz.mycqrs.demo.domain",
            "com.oscaruiz.mycqrs.demo.infrastructure"
    }, excludeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = OutboxConfig.class
    ))
    @EnableJpaRepositories(basePackageClasses = SpringDataBookRepository.class)
    @EntityScan(basePackageClasses = BookEntity.class)
    static class RollbackConfig {

        @Bean
        @Primary
        public EventBus failingOutboxEventBus(NamedParameterJdbcTemplate realTemplate,
                                              ObjectMapper objectMapper) {
            NamedParameterJdbcTemplate failingTemplate = new NamedParameterJdbcTemplate(
                realTemplate.getJdbcTemplate().getDataSource()
            ) {
                @Override
                public int update(String sql, SqlParameterSource paramSource) {
                    if (sql.contains("outbox")) {
                        throw new RuntimeException("Simulated outbox write failure");
                    }
                    return super.update(sql, paramSource);
                }
            };
            return new OutboxEventBus(failingTemplate, objectMapper);
        }

        @Bean("internalEventBus")
        public SimpleEventBus internalEventBus() {
            return new SimpleEventBus();
        }

        @Bean
        public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(SimpleEventBus internalEventBus) {
            return new EventHandlerBeanPostProcessor(internalEventBus);
        }
    }
}
