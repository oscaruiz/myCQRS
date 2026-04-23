package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.author.application.command.CreateAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.command.DeleteAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
import com.oscaruiz.mycqrs.demo.book.application.command.AddAuthorToBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.RemoveAuthorFromBookCommand;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorNotFoundException;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorRetiredException;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = BookAuthorRelationshipIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class BookAuthorRelationshipIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void addAuthorAttachesAuthorIdToBook() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "1984"));

        commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, authorId));

        BookAggregate book = bookRepository.load(bookId);
        assertThat(book.getAuthorIds()).containsExactly(authorId);
    }

    @Test
    void addAuthorTwiceIsIdempotent() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "1984"));

        commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, authorId));
        commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, authorId));

        BookAggregate book = bookRepository.load(bookId);
        assertThat(book.getAuthorIds()).containsExactly(authorId);
    }

    @Test
    void addNonexistentAuthorThrowsAuthorNotFound() {
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "Orphan"));

        assertThatThrownBy(() -> commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, UUID.randomUUID().toString())))
                .isInstanceOf(AuthorNotFoundException.class);
    }

    @Test
    void addRetiredAuthorThrowsAuthorRetired() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),authorId, "Retired", "Author", 1900));
        commandBus.send(new DeleteAuthorCommand(UUID.randomUUID(),authorId));
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "Stranded"));

        assertThatThrownBy(() -> commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, authorId)))
                .isInstanceOf(AuthorRetiredException.class);
    }

    @Test
    void removeAuthorEmptiesTheSet() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "1984"));
        commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, authorId));

        commandBus.send(new RemoveAuthorFromBookCommand(UUID.randomUUID(),bookId, authorId));

        BookAggregate book = bookRepository.load(bookId);
        assertThat(book.getAuthorIds()).isEmpty();
    }

    @Test
    void removeAuthorThatNeverExistedIsLegitimateNoOp() {
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "Clean"));

        commandBus.send(new RemoveAuthorFromBookCommand(UUID.randomUUID(),bookId, UUID.randomUUID().toString()));

        BookAggregate book = bookRepository.load(bookId);
        assertThat(book.getAuthorIds()).isEmpty();
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
