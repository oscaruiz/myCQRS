package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorReadModelRepository;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import com.oscaruiz.mycqrs.demo.book.application.query.AuthorSummary;
import com.oscaruiz.mycqrs.demo.book.application.query.BookReadModelRepository;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorAddedToBookEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Denormalizes both sides of the Book–Author association on every
 * {@link AuthorAddedToBookEvent}. Reads from each aggregate's published query
 * port ({@link AuthorReadModelRepository}, {@link BookReadModelRepository}),
 * never from another aggregate's infrastructure — ArchUnit enforces that edge.
 * If either read-model document is missing this projection throws; the outbox
 * will retry, letting the missing side converge first.
 *
 * <p>Both denormalizations use {@code $pull}-then-{@code $push} against the
 * embedded arrays to guarantee idempotency under outbox retries: if the same
 * event is dispatched twice, the second pass removes any prior embedding and
 * re-inserts the current summary, leaving the array with exactly one entry.
 * This also preserves concurrent updates to other fields of the same document
 * (a {@code findById} + in-memory mutation + {@code save} pattern would
 * replace the whole document and lose unrelated mutations).
 */
@EventHandlerComponent
public class AuthorAddedToBookMongoProjection implements EventHandler<AuthorAddedToBookEvent> {

    private final AuthorReadModelRepository authorReadModelRepository;
    private final BookReadModelRepository bookReadModelRepository;
    private final MongoTemplate mongoTemplate;

    public AuthorAddedToBookMongoProjection(AuthorReadModelRepository authorReadModelRepository,
                                            BookReadModelRepository bookReadModelRepository,
                                            MongoTemplate mongoTemplate) {
        this.authorReadModelRepository = authorReadModelRepository;
        this.bookReadModelRepository = bookReadModelRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void handle(AuthorAddedToBookEvent event) {
        AuthorResponse author = authorReadModelRepository.findById(event.getAuthorId())
                .orElseThrow(() -> new IllegalStateException(
                        "AuthorReadModel not found for id " + event.getAuthorId()
                                + " while projecting AuthorAddedToBookEvent; out-of-order delivery — outbox will retry"));
        BookResponse book = bookReadModelRepository.findById(event.getAggregateId())
                .orElseThrow(() -> new IllegalStateException(
                        "BookReadModel not found for id " + event.getAggregateId()
                                + " while projecting AuthorAddedToBookEvent; out-of-order delivery — outbox will retry"));

        AuthorSummary authorSummary = new AuthorSummary(
                author.id(),
                author.firstName() + " " + author.lastName(),
                author.deleted()
        );
        AuthorResponse.BookSummary bookSummary = new AuthorResponse.BookSummary(
                book.id(),
                book.title()
        );

        Query bookQuery = new Query(Criteria.where("_id").is(book.id()));
        mongoTemplate.updateFirst(bookQuery,
                new Update().pull("authors",
                        new Query(Criteria.where("authorId").is(author.id())).getQueryObject()),
                "books");
        mongoTemplate.updateFirst(bookQuery,
                new Update().push("authors", authorSummary),
                "books");

        Query authorQuery = new Query(Criteria.where("_id").is(author.id()));
        mongoTemplate.updateFirst(authorQuery,
                new Update().pull("books",
                        new Query(Criteria.where("bookId").is(book.id())).getQueryObject()),
                "authors");
        mongoTemplate.updateFirst(authorQuery,
                new Update().push("books", bookSummary),
                "authors");
    }
}
