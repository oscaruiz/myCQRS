package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
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
class CommandValidationIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void createCommandWithBlankTitleIsRejectedByInterceptor() {
        String id = UUID.randomUUID().toString();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new CreateBookCommand(UUID.randomUUID(),id, "")));

        assertTrue(ex.getMessage().contains("title"), () -> "expected message to mention 'title': " + ex.getMessage());
        Optional<BookAggregate> persisted = bookRepository.findByTitle("");
        assertTrue(persisted.isEmpty(), "handler must not run when validation fails");
    }

    @Test
    void updateCommandWithBlankBookIdIsRejectedByInterceptor() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new UpdateBookCommand(UUID.randomUUID(),"", "Title")));

        assertTrue(ex.getMessage().contains("bookId"), () -> "expected message to mention 'bookId': " + ex.getMessage());
    }

    @Test
    void deleteCommandWithBlankBookIdIsRejectedByInterceptor() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandBus.send(new DeleteBookCommand(UUID.randomUUID(),"")));

        assertTrue(ex.getMessage().contains("bookId"), () -> "expected message to mention 'bookId': " + ex.getMessage());
    }

    @Test
    void validCreateCommandPassesValidation() {
        String id = UUID.randomUUID().toString();

        commandBus.send(new CreateBookCommand(UUID.randomUUID(),id, "Valid Title"));

        BookAggregate saved = bookRepository.load(id);
        assertEquals("Valid Title", saved.getTitle());
    }
}
