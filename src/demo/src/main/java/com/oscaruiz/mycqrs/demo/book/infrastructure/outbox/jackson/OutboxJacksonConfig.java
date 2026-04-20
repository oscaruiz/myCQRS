package com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.jackson;

import com.oscaruiz.mycqrs.core.ddd.DomainEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookDeletedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Jackson mixins that teach the ObjectMapper how to deserialize
 * domain events from the outbox table.
 *
 * This keeps all Jackson serialization knowledge in the infrastructure layer.
 * The core module and domain events remain framework-agnostic — no Jackson
 * annotations, no Jackson dependency.
 */
@Configuration
public class OutboxJacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer domainEventMixinCustomizer() {
        return builder -> builder
                .mixIn(DomainEvent.class, DomainEventMixin.class)
                .mixIn(BookCreatedEvent.class, BookCreatedEventMixin.class)
                .mixIn(BookUpdatedEvent.class, BookUpdatedEventMixin.class)
                .mixIn(BookDeletedEvent.class, BookDeletedEventMixin.class);
    }
}
