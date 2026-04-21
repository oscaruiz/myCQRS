package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.mongodb.client.result.UpdateResult;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorDeletedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Marks the Author's own read model as {@code deleted} and flips the
 * {@code retired} flag on every embedded {@code authors} entry of every Book
 * that references this author. The {@code books} list inside AuthorReadModel
 * is left untouched — a soft-deleted author keeps the history of the books
 * it authored.
 *
 * <p>Both updates are atomic {@code $set} operations, idempotent by
 * construction and safe to replay on outbox retries without overwriting
 * unrelated fields.
 */
@EventHandlerComponent
public class AuthorDeletedMongoProjection implements EventHandler<AuthorDeletedEvent> {

    private final MongoTemplate mongoTemplate;

    public AuthorDeletedMongoProjection(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void handle(AuthorDeletedEvent event) {
        UpdateResult result = mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(event.getAggregateId())),
                new Update().set("deleted", true),
                "authors");
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException(
                    "AuthorReadModel not found for id " + event.getAggregateId()
                            + " while projecting AuthorDeletedEvent; out-of-order delivery — outbox will retry");
        }

        Query booksReferencingAuthor = new Query(Criteria.where("authors.authorId").is(event.getAggregateId()));
        Update markRetired = new Update().set("authors.$[a].retired", true)
                .filterArray(Criteria.where("a.authorId").is(event.getAggregateId()));
        mongoTemplate.updateMulti(booksReferencingAuthor, markRetired, "books");
    }
}
