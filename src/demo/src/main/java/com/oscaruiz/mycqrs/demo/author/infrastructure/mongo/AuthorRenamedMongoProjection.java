package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.mongodb.client.result.UpdateResult;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorRenamedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Renames the Author's own read model and propagates the new {@code fullName}
 * into every embedded {@code authors} entry of every Book that references
 * this author.
 *
 * <p>Both updates use atomic {@code $set} operations (per-field on the author
 * document, positional-filtered on the embedded book arrays). Running the
 * same event twice leaves both collections in the exact same state as a
 * single run — idempotent by construction, and it cannot overwrite unrelated
 * fields (e.g., {@code books}) mutated by concurrent projections on the same
 * document.
 */
@EventHandlerComponent
public class AuthorRenamedMongoProjection implements EventHandler<AuthorRenamedEvent> {

    private final MongoTemplate mongoTemplate;

    public AuthorRenamedMongoProjection(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void handle(AuthorRenamedEvent event) {
        UpdateResult result = mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(event.getAggregateId())),
                new Update()
                        .set("firstName", event.getFirstName())
                        .set("lastName", event.getLastName()),
                "authors");
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException(
                    "AuthorReadModel not found for id " + event.getAggregateId()
                            + " while projecting AuthorRenamedEvent; out-of-order delivery — outbox will retry");
        }

        String newFullName = event.getFirstName() + " " + event.getLastName();
        Query booksReferencingAuthor = new Query(Criteria.where("authors.authorId").is(event.getAggregateId()));
        Update updateFullName = new Update().set("authors.$[a].fullName", newFullName)
                .filterArray(Criteria.where("a.authorId").is(event.getAggregateId()));
        mongoTemplate.updateMulti(booksReferencingAuthor, updateFullName, "books");
    }
}
