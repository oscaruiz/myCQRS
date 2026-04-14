package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;

@EventHandlerComponent
public class BookMongoProjection implements EventHandler<BookUpdatedEvent> {

    private final BookMongoRepository repository;

    public BookMongoProjection(BookMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(BookUpdatedEvent event) {
        BookReadModel model = new BookReadModel(
                event.getAggregateId(),
                event.getTitle(),
                event.getAuthor()
        );
        repository.save(model);
    }
}
