package com.oscaruiz.mycqrs.demo.book.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.bus.event.SimpleEventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Wiring for the outbox-based event publication strategy.
 *
 * - outboxEventBus is {@code @Primary}: because internalEventBus (SimpleEventBus)
 *   also implements EventBus, {@code @Primary} is required to disambiguate
 *   injection points that request EventBus without a qualifier. Both beans
 *   satisfy {@code @ConditionalOnMissingBean(EventBus.class)} in
 *   CqrsConfiguration, preventing the core default from being created.
 *   Command handlers receive outboxEventBus. Calls to publish() go to the
 *   outbox table within the active transaction.
 *
 * - internalEventBus (SimpleEventBus) is the bus that event handlers register
 *   against via EventHandlerBeanPostProcessor. The OutboxPoller uses this bus
 *   to dispatch events read from the outbox.
 *
 * See docs/adr/0003-outbox-pattern.md.
 */
@Configuration
public class OutboxConfig {

    @Bean
    @Primary
    public EventBus outboxEventBus(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new OutboxEventBus(jdbcTemplate, objectMapper);
    }

    @Bean("internalEventBus")
    public SimpleEventBus internalEventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public EventHandlerBeanPostProcessor eventHandlerBeanPostProcessor(
            @Qualifier("internalEventBus") SimpleEventBus internalEventBus) {
        return new EventHandlerBeanPostProcessor(internalEventBus);
    }
}
