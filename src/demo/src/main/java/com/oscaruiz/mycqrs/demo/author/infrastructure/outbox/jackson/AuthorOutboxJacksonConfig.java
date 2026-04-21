package com.oscaruiz.mycqrs.demo.author.infrastructure.outbox.jackson;

import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorCreatedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorDeletedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorRenamedEvent;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Jackson mixins for Author domain events so the outbox ObjectMapper
 * can deserialize them without tying the domain events to Jackson.
 *
 * Sibling of {@code book.infrastructure.outbox.jackson.OutboxJacksonConfig}:
 * each aggregate owns the Jackson registration for its own events.
 */
@Configuration
public class AuthorOutboxJacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer authorEventMixinCustomizer() {
        return builder -> builder
                .mixIn(AuthorCreatedEvent.class, AuthorCreatedEventMixin.class)
                .mixIn(AuthorRenamedEvent.class, AuthorRenamedEventMixin.class)
                .mixIn(AuthorDeletedEvent.class, AuthorDeletedEventMixin.class);
    }
}
