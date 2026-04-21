package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorCreatedEvent;

import java.util.ArrayList;

@EventHandlerComponent
public class AuthorCreatedMongoProjection implements EventHandler<AuthorCreatedEvent> {

    private final AuthorMongoRepository repository;

    public AuthorCreatedMongoProjection(AuthorMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(AuthorCreatedEvent event) {
        AuthorReadModel model = new AuthorReadModel(
                event.getAggregateId(),
                event.getFirstName(),
                event.getLastName(),
                event.getBirthYear(),
                false,
                new ArrayList<>()
        );
        repository.save(model);
    }
}
