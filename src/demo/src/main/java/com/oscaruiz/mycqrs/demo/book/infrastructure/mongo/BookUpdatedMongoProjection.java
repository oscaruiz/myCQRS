package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;

@EventHandlerComponent
public class BookUpdatedMongoProjection implements EventHandler<BookUpdatedEvent> {

    private final BookMongoRepository repository;

    public BookUpdatedMongoProjection(BookMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(BookUpdatedEvent event) {
        BookReadModel existing = repository.findById(event.getAggregateId())
                .orElseThrow(() -> new IllegalStateException(
                        "BookReadModel not found for id " + event.getAggregateId()
                                + " while projecting BookUpdatedEvent; out-of-order delivery — outbox will retry"));
        existing.setTitle(event.getTitle());
        repository.save(existing);
    }
}
