package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorRemovedFromBookEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Pulls the Author from {@code BookReadModel.authors} and the Book from
 * {@code AuthorReadModel.books}. Uses native {@code $pull} so the operation
 * is idempotent under outbox retries and preserves concurrent updates to
 * other fields of the same document. Removing a non-referenced entry is a
 * legitimate no-op — matches the {@code RemoveAuthorFromBookCommandHandler}
 * semantic.
 */
@EventHandlerComponent
public class AuthorRemovedFromBookMongoProjection implements EventHandler<AuthorRemovedFromBookEvent> {

    private final MongoTemplate mongoTemplate;

    public AuthorRemovedFromBookMongoProjection(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void handle(AuthorRemovedFromBookEvent event) {
        Query bookQuery = new Query(Criteria.where("_id").is(event.getAggregateId()));
        mongoTemplate.updateFirst(bookQuery,
                new Update().pull("authors",
                        new Query(Criteria.where("authorId").is(event.getAuthorId())).getQueryObject()),
                "books");

        Query authorQuery = new Query(Criteria.where("_id").is(event.getAuthorId()));
        mongoTemplate.updateFirst(authorQuery,
                new Update().pull("books",
                        new Query(Criteria.where("bookId").is(event.getAggregateId())).getQueryObject()),
                "authors");
    }
}
