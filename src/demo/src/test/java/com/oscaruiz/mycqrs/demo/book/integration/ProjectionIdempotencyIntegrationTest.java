package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorDeletedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorRenamedEvent;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
import com.oscaruiz.mycqrs.demo.author.infrastructure.mongo.AuthorDeletedMongoProjection;
import com.oscaruiz.mycqrs.demo.author.infrastructure.mongo.AuthorReadModel;
import com.oscaruiz.mycqrs.demo.author.infrastructure.mongo.AuthorRenamedMongoProjection;
import com.oscaruiz.mycqrs.demo.book.application.query.AuthorSummary;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorAddedToBookEvent;
import com.oscaruiz.mycqrs.demo.book.domain.event.AuthorRemovedFromBookEvent;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.mongo.AuthorAddedToBookMongoProjection;
import com.oscaruiz.mycqrs.demo.book.infrastructure.mongo.AuthorRemovedFromBookMongoProjection;
import com.oscaruiz.mycqrs.demo.book.infrastructure.mongo.BookReadModel;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ProjectionIdempotencyIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class ProjectionIdempotencyIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AuthorAddedToBookMongoProjection authorAddedToBook;

    @Autowired
    private AuthorRemovedFromBookMongoProjection authorRemovedFromBook;

    @Autowired
    private AuthorRenamedMongoProjection authorRenamed;

    @Autowired
    private AuthorDeletedMongoProjection authorDeleted;

    @Test
    void authorAddedToBookProjection_isIdempotent_underRetry() {
        String bookId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        mongoTemplate.save(new AuthorReadModel(authorId, "George", "Orwell", 1903, false, new ArrayList<>()));
        mongoTemplate.save(new BookReadModel(bookId, "1984", new ArrayList<>()));

        AuthorAddedToBookEvent event = new AuthorAddedToBookEvent(bookId, authorId);

        authorAddedToBook.handle(event);
        BookReadModel bookAfterFirst = mongoTemplate.findById(bookId, BookReadModel.class);
        AuthorReadModel authorAfterFirst = mongoTemplate.findById(authorId, AuthorReadModel.class);

        assertThat(bookAfterFirst).isNotNull();
        assertThat(bookAfterFirst.getAuthors()).hasSize(1);
        assertThat(bookAfterFirst.getAuthors().get(0).authorId()).isEqualTo(authorId);
        assertThat(authorAfterFirst).isNotNull();
        assertThat(authorAfterFirst.getBooks()).hasSize(1);
        assertThat(authorAfterFirst.getBooks().get(0).bookId()).isEqualTo(bookId);

        authorAddedToBook.handle(event);
        BookReadModel bookAfterReplay = mongoTemplate.findById(bookId, BookReadModel.class);
        AuthorReadModel authorAfterReplay = mongoTemplate.findById(authorId, AuthorReadModel.class);

        assertThat(bookAfterReplay).isNotNull();
        assertThat(bookAfterReplay.getAuthors())
                .as("AuthorSummary list must stay at exactly one element after retry")
                .hasSize(1);
        assertThat(bookAfterReplay.getAuthors().get(0).authorId()).isEqualTo(authorId);
        assertThat(bookAfterReplay.getAuthors().get(0).fullName()).isEqualTo("George Orwell");
        assertThat(bookAfterReplay.getAuthors().get(0).retired()).isFalse();

        assertThat(authorAfterReplay).isNotNull();
        assertThat(authorAfterReplay.getBooks())
                .as("BookSummary list must stay at exactly one element after retry")
                .hasSize(1);
        assertThat(authorAfterReplay.getBooks().get(0).bookId()).isEqualTo(bookId);
        assertThat(authorAfterReplay.getBooks().get(0).title()).isEqualTo("1984");
    }

    @Test
    void authorRemovedFromBookProjection_isIdempotent_underRetry() {
        String bookId = UUID.randomUUID().toString();
        String authorId = UUID.randomUUID().toString();
        AuthorSummary existingSummary = new AuthorSummary(authorId, "George Orwell", false);
        AuthorResponse.BookSummary existingBook = new AuthorResponse.BookSummary(bookId, "1984");
        mongoTemplate.save(new AuthorReadModel(authorId, "George", "Orwell", 1903, false, List.of(existingBook)));
        mongoTemplate.save(new BookReadModel(bookId, "1984", List.of(existingSummary)));

        AuthorRemovedFromBookEvent event = new AuthorRemovedFromBookEvent(bookId, authorId);

        authorRemovedFromBook.handle(event);
        BookReadModel bookAfterFirst = mongoTemplate.findById(bookId, BookReadModel.class);
        AuthorReadModel authorAfterFirst = mongoTemplate.findById(authorId, AuthorReadModel.class);
        assertThat(bookAfterFirst).isNotNull();
        assertThat(bookAfterFirst.getAuthors()).isEmpty();
        assertThat(authorAfterFirst).isNotNull();
        assertThat(authorAfterFirst.getBooks()).isEmpty();

        authorRemovedFromBook.handle(event);
        BookReadModel bookAfterReplay = mongoTemplate.findById(bookId, BookReadModel.class);
        AuthorReadModel authorAfterReplay = mongoTemplate.findById(authorId, AuthorReadModel.class);
        assertThat(bookAfterReplay).isNotNull();
        assertThat(bookAfterReplay.getAuthors()).isEmpty();
        assertThat(authorAfterReplay).isNotNull();
        assertThat(authorAfterReplay.getBooks()).isEmpty();
    }

    @Test
    void authorRenamedProjection_isIdempotent_underRetry() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        mongoTemplate.save(new AuthorReadModel(authorId, "George", "Orwell", 1903, false, new ArrayList<>()));
        mongoTemplate.save(new BookReadModel(bookId, "1984",
                List.of(new AuthorSummary(authorId, "George Orwell", false))));

        AuthorRenamedEvent event = new AuthorRenamedEvent(authorId, "Eric", "Blair");

        authorRenamed.handle(event);
        AuthorReadModel authorAfterFirst = mongoTemplate.findById(authorId, AuthorReadModel.class);
        BookReadModel bookAfterFirst = mongoTemplate.findById(bookId, BookReadModel.class);
        assertThat(authorAfterFirst).isNotNull();
        assertThat(authorAfterFirst.getFirstName()).isEqualTo("Eric");
        assertThat(authorAfterFirst.getLastName()).isEqualTo("Blair");
        assertThat(bookAfterFirst).isNotNull();
        assertThat(bookAfterFirst.getAuthors()).hasSize(1);
        assertThat(bookAfterFirst.getAuthors().get(0).fullName()).isEqualTo("Eric Blair");

        authorRenamed.handle(event);
        AuthorReadModel authorAfterReplay = mongoTemplate.findById(authorId, AuthorReadModel.class);
        BookReadModel bookAfterReplay = mongoTemplate.findById(bookId, BookReadModel.class);
        assertThat(authorAfterReplay).isNotNull();
        assertThat(authorAfterReplay.getFirstName()).isEqualTo("Eric");
        assertThat(authorAfterReplay.getLastName()).isEqualTo("Blair");
        assertThat(bookAfterReplay).isNotNull();
        assertThat(bookAfterReplay.getAuthors()).hasSize(1);
        assertThat(bookAfterReplay.getAuthors().get(0).fullName()).isEqualTo("Eric Blair");
    }

    @Test
    void authorDeletedProjection_isIdempotent_underRetry() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        mongoTemplate.save(new AuthorReadModel(authorId, "George", "Orwell", 1903, false, new ArrayList<>()));
        mongoTemplate.save(new BookReadModel(bookId, "1984",
                List.of(new AuthorSummary(authorId, "George Orwell", false))));

        AuthorDeletedEvent event = new AuthorDeletedEvent(authorId);

        authorDeleted.handle(event);
        AuthorReadModel authorAfterFirst = mongoTemplate.findById(authorId, AuthorReadModel.class);
        BookReadModel bookAfterFirst = mongoTemplate.findById(bookId, BookReadModel.class);
        assertThat(authorAfterFirst).isNotNull();
        assertThat(authorAfterFirst.isDeleted()).isTrue();
        assertThat(bookAfterFirst).isNotNull();
        assertThat(bookAfterFirst.getAuthors()).hasSize(1);
        assertThat(bookAfterFirst.getAuthors().get(0).retired()).isTrue();

        authorDeleted.handle(event);
        AuthorReadModel authorAfterReplay = mongoTemplate.findById(authorId, AuthorReadModel.class);
        BookReadModel bookAfterReplay = mongoTemplate.findById(bookId, BookReadModel.class);
        assertThat(authorAfterReplay).isNotNull();
        assertThat(authorAfterReplay.isDeleted()).isTrue();
        assertThat(bookAfterReplay).isNotNull();
        assertThat(bookAfterReplay.getAuthors()).hasSize(1);
        assertThat(bookAfterReplay.getAuthors().get(0).retired()).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.book.application",
            "com.oscaruiz.mycqrs.demo.book.domain",
            "com.oscaruiz.mycqrs.demo.book.infrastructure",
            "com.oscaruiz.mycqrs.demo.author.application",
            "com.oscaruiz.mycqrs.demo.author.domain",
            "com.oscaruiz.mycqrs.demo.author.infrastructure"
    })
    @EnableJpaRepositories(basePackageClasses = {SpringDataBookRepository.class, SpringDataAuthorRepository.class})
    @EntityScan(basePackageClasses = {BookEntity.class, AuthorEntity.class})
    static class TestConfig {
    }
}
