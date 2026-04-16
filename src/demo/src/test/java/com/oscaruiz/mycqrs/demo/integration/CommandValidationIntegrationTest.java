package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.integration.support.MongoTestcontainersTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BookCommandIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class CommandValidationIntegrationTest extends MongoTestcontainersTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void createCommandWithBlankTitleIsRejectedByInterceptor() {
        String id = UUID.randomUUID().toString();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new CreateBookCommand(id, "", "Some Author")));

        assertTrue(ex.getMessage().contains("title"), () -> "expected message to mention 'title': " + ex.getMessage());
        Optional<BookAggregate> persisted = bookRepository.findByTitle("");
        assertTrue(persisted.isEmpty(), "handler must not run when validation fails");
    }

    @Test
    void createCommandWithBlankAuthorIsRejectedByInterceptor() {
        String id = UUID.randomUUID().toString();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new CreateBookCommand(id, "Valid Title", "")));

        assertTrue(ex.getMessage().contains("author"), () -> "expected message to mention 'author': " + ex.getMessage());
    }

    @Test
    void updateCommandWithBlankBookIdIsRejectedByInterceptor() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new UpdateBookCommand("", "Title", "Author")));

        assertTrue(ex.getMessage().contains("bookId"), () -> "expected message to mention 'bookId': " + ex.getMessage());
    }

    @Test
    void deleteCommandWithBlankBookIdIsRejectedByInterceptor() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new DeleteBookCommand("")));

        assertTrue(ex.getMessage().contains("bookId"), () -> "expected message to mention 'bookId': " + ex.getMessage());
    }

    @Test
    void validCreateCommandPassesValidation() {
        String id = UUID.randomUUID().toString();

        commandBus.send(new CreateBookCommand(id, "Valid Title", "Valid Author"));

        BookAggregate saved = bookRepository.load(id);
        assertEquals("Valid Title", saved.getTitle());
    }
}
