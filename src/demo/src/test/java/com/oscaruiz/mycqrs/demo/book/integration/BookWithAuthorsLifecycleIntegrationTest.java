package com.oscaruiz.mycqrs.demo.book.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.author.application.command.CreateAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.command.DeleteAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.command.RenameAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import com.oscaruiz.mycqrs.demo.author.application.query.FindAuthorByIdQuery;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
import com.oscaruiz.mycqrs.demo.book.application.command.AddAuthorToBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.RemoveAuthorFromBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.book.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BookWithAuthorsLifecycleIntegrationTest.TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookWithAuthorsLifecycleIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void happyPath_createAuthor_createBook_addAuthor_queryShowsName() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(bookId, "1984"));
        outboxPoller.poll();

        commandBus.send(new AddAuthorToBookCommand(bookId, authorId));
        outboxPoller.poll();

        BookResponse book = queryBus.handle(new FindBookByIdQuery(bookId));
        assertThat(book.title()).isEqualTo("1984");
        assertThat(book.authors()).hasSize(1);
        assertThat(book.authors().get(0).authorId()).isEqualTo(authorId);
        assertThat(book.authors().get(0).fullName()).isEqualTo("George Orwell");
        assertThat(book.authors().get(0).retired()).isFalse();

        AuthorResponse author = queryBus.handle(new FindAuthorByIdQuery(authorId));
        assertThat(author.books()).hasSize(1);
        assertThat(author.books().get(0).bookId()).isEqualTo(bookId);
        assertThat(author.books().get(0).title()).isEqualTo("1984");
    }

    @Test
    void authorRename_propagatesToBookReadModel() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(bookId, "1984"));
        outboxPoller.poll();
        commandBus.send(new AddAuthorToBookCommand(bookId, authorId));
        outboxPoller.poll();

        commandBus.send(new RenameAuthorCommand(authorId, "Eric", "Blair"));
        outboxPoller.poll();

        BookResponse book = queryBus.handle(new FindBookByIdQuery(bookId));
        assertThat(book.authors()).hasSize(1);
        assertThat(book.authors().get(0).fullName()).isEqualTo("Eric Blair");
    }

    @Test
    void authorSoftDelete_propagatesRetiredFlag() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(bookId, "1984"));
        outboxPoller.poll();
        commandBus.send(new AddAuthorToBookCommand(bookId, authorId));
        outboxPoller.poll();

        commandBus.send(new DeleteAuthorCommand(authorId));
        outboxPoller.poll();

        BookResponse book = queryBus.handle(new FindBookByIdQuery(bookId));
        assertThat(book.authors()).hasSize(1);
        assertThat(book.authors().get(0).retired()).isTrue();

        AuthorResponse author = queryBus.handle(new FindAuthorByIdQuery(authorId));
        assertThat(author.deleted()).isTrue();
        assertThat(author.books())
                .as("soft-deleted author still keeps its book history")
                .hasSize(1);
        assertThat(author.books().get(0).bookId()).isEqualTo(bookId);
    }

    @Test
    void removeAuthor_removesFromBothSides() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(bookId, "1984"));
        outboxPoller.poll();
        commandBus.send(new AddAuthorToBookCommand(bookId, authorId));
        outboxPoller.poll();

        commandBus.send(new RemoveAuthorFromBookCommand(bookId, authorId));
        outboxPoller.poll();

        BookResponse book = queryBus.handle(new FindBookByIdQuery(bookId));
        assertThat(book.authors()).isEmpty();

        AuthorResponse author = queryBus.handle(new FindAuthorByIdQuery(authorId));
        assertThat(author.books()).isEmpty();
    }

    @Test
    void addAuthorTwice_isIdempotent_endToEnd() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(bookId, "1984"));
        outboxPoller.poll();

        commandBus.send(new AddAuthorToBookCommand(bookId, authorId));
        commandBus.send(new AddAuthorToBookCommand(bookId, authorId));
        outboxPoller.poll();

        BookResponse book = queryBus.handle(new FindBookByIdQuery(bookId));
        assertThat(book.authors())
                .as("second AddAuthorToBookCommand on the same authorId must be a no-op, even end-to-end through the projection")
                .hasSize(1);
        assertThat(book.authors().get(0).authorId()).isEqualTo(authorId);

        AuthorResponse author = queryBus.handle(new FindAuthorByIdQuery(authorId));
        assertThat(author.books()).hasSize(1);
    }

    @Test
    void addNonexistentAuthor_returns404() throws Exception {
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(bookId, "Orphan"));

        UUID unknownAuthor = UUID.randomUUID();
        mockMvc.perform(post("/books/{id}/authors/{authorId}", bookId, unknownAuthor))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void addSoftDeletedAuthor_returns409() throws Exception {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(authorId, "Retired", "Author", 1900));
        commandBus.send(new DeleteAuthorCommand(authorId));
        commandBus.send(new CreateBookCommand(bookId, "Stranded"));

        mockMvc.perform(post("/books/{id}/authors/{authorId}", bookId, authorId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
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
