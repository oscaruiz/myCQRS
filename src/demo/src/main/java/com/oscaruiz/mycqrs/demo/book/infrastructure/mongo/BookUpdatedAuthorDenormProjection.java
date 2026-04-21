package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Propagates a Book's new title into every embedded {@code books} entry of
 * every Author that references this book. A sibling of
 * {@link BookUpdatedMongoProjection} (which owns the Book's own read model)
 * kept separate to isolate the two denormalization concerns.
 */
@EventHandlerComponent
public class BookUpdatedAuthorDenormProjection implements EventHandler<BookUpdatedEvent> {

    private final MongoTemplate mongoTemplate;

    public BookUpdatedAuthorDenormProjection(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void handle(BookUpdatedEvent event) {
        Query authorsReferencingBook = new Query(Criteria.where("books.bookId").is(event.getAggregateId()));
        Update updateTitle = new Update().set("books.$[b].title", event.getTitle())
                .filterArray(Criteria.where("b.bookId").is(event.getAggregateId()));
        mongoTemplate.updateMulti(authorsReferencingBook, updateTitle, "authors");
    }
}
