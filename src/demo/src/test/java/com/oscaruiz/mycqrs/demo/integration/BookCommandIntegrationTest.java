package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.BookReadModelRepository;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import java.util.Optional;
import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BookCommandIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class BookCommandIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UpdatedEventRecorder updatedEventRecorder;

    @BeforeEach
    void clearRecorder() {
        updatedEventRecorder.clear();
    }

    @Test
    void createBookCommandSavesBook() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Domain-Driven Design", "Eric Evans"));

        BookAggregate saved = bookRepository.findByTitle("Domain-Driven Design").orElseThrow();

        assertEquals(id, saved.getId());
        assertEquals("Eric Evans", saved.getAuthor());
        assertFalse(saved.isDeleted());
    }

    @Test
    void updateBookCommandEmitsEventAndChangesValues() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Refactoring", "Martin Fowler"));
        BookAggregate existing = bookRepository.findByTitle("Refactoring").orElseThrow();

        commandBus.send(new UpdateBookCommand(existing.getId(), "Refactoring 2nd", "Martin Fowler"));

        BookAggregate updated = bookRepository.load(existing.getId());

        assertEquals("Refactoring 2nd", updated.getTitle());
        assertEquals(1, updatedEventRecorder.events().size());
        assertEquals(existing.getId(), updatedEventRecorder.events().get(0).getAggregateId());
    }

    @Test
    void deleteBookCommandMarksAggregateDeleted() {
        commandBus.send(new CreateBookCommand(UUID.randomUUID().toString(), "Patterns", "GoF"));
        BookAggregate existing = bookRepository.findByTitle("Patterns").orElseThrow();

        commandBus.send(new DeleteBookCommand(existing.getId()));

        BookAggregate deleted = bookRepository.load(existing.getId());

        assertTrue(deleted.isDeleted());
    }

    @Test
    void deleteTwiceThrowsException() {
        commandBus.send(new CreateBookCommand(UUID.randomUUID().toString(), "Effective Java", "Joshua Bloch"));
        BookAggregate existing = bookRepository.findByTitle("Effective Java").orElseThrow();

        commandBus.send(new DeleteBookCommand(existing.getId()));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new DeleteBookCommand(existing.getId())));
    }

    @Test
    void updateAfterDeleteThrowsException() {
        commandBus.send(new CreateBookCommand(UUID.randomUUID().toString(), "Clean Architecture", "Robert C. Martin"));
        BookAggregate existing = bookRepository.findByTitle("Clean Architecture").orElseThrow();

        commandBus.send(new DeleteBookCommand(existing.getId()));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new UpdateBookCommand(existing.getId(), "Any", "Any")));
    }


    @Test
    void saveWithDetachedAggregateInsertsNewRow() {
        String id = UUID.randomUUID().toString();
        BookAggregate detached = BookAggregate.rehydrate(id, "Ghost", "Nobody", false);

        bookRepository.save(detached);

        BookAggregate loaded = bookRepository.load(id);
        assertEquals("Ghost", loaded.getTitle());
        assertEquals("Nobody", loaded.getAuthor());
    }

    @Test
    void loadNonExistingIdThrowsException() {
        assertThrows(NoSuchElementException.class,
                () -> bookRepository.load(UUID.randomUUID().toString()));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class
    })
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.core",
            "com.oscaruiz.mycqrs.demo.application",
            "com.oscaruiz.mycqrs.demo.domain",
            "com.oscaruiz.mycqrs.demo.infrastructure"
    }, excludeFilters = @ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.REGEX,
            pattern = "com\\.oscaruiz\\.mycqrs\\.demo\\.infrastructure\\.mongo\\..*"
    ))
    @EnableJpaRepositories(basePackages = "com.oscaruiz.mycqrs.demo.infrastructure.jpa")
    @EntityScan(basePackageClasses = BookEntity.class)
    @Import(TestEventsConfig.class)
    static class TestConfig {
    }

    @TestConfiguration
    static class TestEventsConfig {
        @Bean
        UpdatedEventRecorder updatedEventRecorder() {
            return new UpdatedEventRecorder();
        }

        @Bean
        BookReadModelRepository emptyBookReadModelRepository() {
            return new BookReadModelRepository() {
                @Override
                public Optional<Book> findById(String id) {
                    return Optional.empty();
                }

                @Override
                public Optional<Book> findByTitle(String title) {
                    return Optional.empty();
                }
            };
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
