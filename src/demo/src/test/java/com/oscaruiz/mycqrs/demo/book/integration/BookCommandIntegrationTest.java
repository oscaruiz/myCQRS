package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.book.domain.event.BookUpdatedEvent;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BookCommandIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class BookCommandIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private UpdatedEventRecorder updatedEventRecorder;

    @BeforeEach
    void clearRecorder() {
        updatedEventRecorder.clear();
    }

    @Test
    void createBookCommandSavesBook() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Domain-Driven Design"));

        BookAggregate saved = bookRepository.findByTitle("Domain-Driven Design").orElseThrow();

        assertEquals(id, saved.getId());
        // Original assertion on saved.getAuthor() removed: BookAggregate no longer carries a single-author field.
        assertFalse(saved.isDeleted());
    }

    @Test
    void updateBookCommandEmitsEventAndChangesValues() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Refactoring"));
        outboxPoller.poll();

        BookAggregate existing = bookRepository.findByTitle("Refactoring").orElseThrow();

        commandBus.send(new UpdateBookCommand(existing.getId(), "Refactoring 2nd"));
        outboxPoller.poll();

        BookAggregate updated = bookRepository.load(existing.getId());

        assertEquals("Refactoring 2nd", updated.getTitle());
        assertEquals(1, updatedEventRecorder.events().size());
        assertEquals(existing.getId(), updatedEventRecorder.events().get(0).getAggregateId());
    }

    @Test
    void deleteBookCommandMarksAggregateDeleted() {
        commandBus.send(new CreateBookCommand(UUID.randomUUID().toString(), "Patterns"));
        BookAggregate existing = bookRepository.findByTitle("Patterns").orElseThrow();

        commandBus.send(new DeleteBookCommand(existing.getId()));

        BookAggregate deleted = bookRepository.load(existing.getId());

        assertTrue(deleted.isDeleted());
    }

    @Test
    void deleteTwiceThrowsException() {
        commandBus.send(new CreateBookCommand(UUID.randomUUID().toString(), "Effective Java"));
        BookAggregate existing = bookRepository.findByTitle("Effective Java").orElseThrow();

        commandBus.send(new DeleteBookCommand(existing.getId()));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new DeleteBookCommand(existing.getId())));
    }

    @Test
    void updateAfterDeleteThrowsException() {
        commandBus.send(new CreateBookCommand(UUID.randomUUID().toString(), "Clean Architecture"));
        BookAggregate existing = bookRepository.findByTitle("Clean Architecture").orElseThrow();

        commandBus.send(new DeleteBookCommand(existing.getId()));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new UpdateBookCommand(existing.getId(), "Any")));
    }


    @Test
    void saveWithDetachedAggregateInsertsNewRow() {
        String id = UUID.randomUUID().toString();
        BookAggregate detached = BookAggregate.rehydrate(id, "Ghost", false, Set.of());

        bookRepository.save(detached);

        BookAggregate loaded = bookRepository.load(id);
        assertEquals("Ghost", loaded.getTitle());
        // Original assertion on loaded.getAuthor() removed: BookAggregate no longer carries a single-author field.
    }

    @Test
    void loadNonExistingIdThrowsException() {
        assertThrows(NoSuchElementException.class,
                () -> bookRepository.load(UUID.randomUUID().toString()));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.book.application",
            "com.oscaruiz.mycqrs.demo.book.domain",
            "com.oscaruiz.mycqrs.demo.book.infrastructure",
            "com.oscaruiz.mycqrs.demo.author.infrastructure.jpa",
            "com.oscaruiz.mycqrs.demo.author.infrastructure.mongo"
    })
    @EnableJpaRepositories(basePackageClasses = {SpringDataBookRepository.class, SpringDataAuthorRepository.class})
    @EntityScan(basePackageClasses = {BookEntity.class, AuthorEntity.class})
    @Import(TestEventsConfig.class)
    static class TestConfig {
    }

    @TestConfiguration
    static class TestEventsConfig {
        @Bean
        UpdatedEventRecorder updatedEventRecorder() {
            return new UpdatedEventRecorder();
        }
    }

    static class UpdatedEventRecorder implements EventHandler<BookUpdatedEvent> {

        private final List<BookUpdatedEvent> events = new ArrayList<>();

        @Override
        public void handle(BookUpdatedEvent event) {
            events.add(event);
        }

        List<BookUpdatedEvent> events() {
            return events;
        }

        void clear() {
            events.clear();
        }
    }
}
