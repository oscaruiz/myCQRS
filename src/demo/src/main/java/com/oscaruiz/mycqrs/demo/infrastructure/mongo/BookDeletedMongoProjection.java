package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookDeletedEvent;

@EventHandlerComponent
public class BookDeletedMongoProjection implements EventHandler<BookDeletedEvent> {

    private final BookMongoRepository repository;

    public BookDeletedMongoProjection(BookMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(BookDeletedEvent event) {
        repository.deleteById(event.getAggregateId());
    }
}
