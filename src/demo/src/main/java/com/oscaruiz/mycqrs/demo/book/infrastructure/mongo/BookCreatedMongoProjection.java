package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookCreatedEvent;

import java.util.ArrayList;

@EventHandlerComponent
public class BookCreatedMongoProjection implements EventHandler<BookCreatedEvent> {

    private final BookMongoRepository repository;

    public BookCreatedMongoProjection(BookMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(BookCreatedEvent event) {
        BookReadModel model = new BookReadModel(
                event.getAggregateId(),
                event.getTitle(),
                new ArrayList<>()
        );
        repository.save(model);
    }
}
