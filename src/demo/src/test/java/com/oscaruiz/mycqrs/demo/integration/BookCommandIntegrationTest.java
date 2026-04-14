package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;
import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BookCommandIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
@Disabled("Day 2: persistence migration to native UUID PK")
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
        commandBus.send(new CreateBookCommand("Domain-Driven Design", "Eric Evans"));

        BookAggregate saved = bookRepository.findByTitle("Domain-Driven Design").orElseThrow();

        assertEquals("Eric Evans", saved.getAuthor());
        assertFalse(saved.isDeleted());
        assertTrue(Long.parseLong(saved.getId()) > 0);
    }

    @Test
    void updateBookCommandEmitsEventAndChangesValues() {
        commandBus.send(new CreateBookCommand("Refactoring", "Martin Fowler"));
        BookAggregate existing = bookRepository.findByTitle("Refactoring").orElseThrow();

        commandBus.send(new UpdateBookCommand(Long.parseLong(existing.getId()), "Refactoring 2nd", "Martin Fowler"));

        BookAggregate updated = bookRepository.load(Long.parseLong(existing.getId()));

        assertEquals("Refactoring 2nd", updated.getTitle());
        assertEquals(1, updatedEventRecorder.events().size());
        assertEquals(String.valueOf(existing.getId()), updatedEventRecorder.events().get(0).getAggregateId());
    }

    @Test
    void deleteBookCommandMarksAggregateDeleted() {
        commandBus.send(new CreateBookCommand("Patterns", "GoF"));
        BookAggregate existing = bookRepository.findByTitle("Patterns").orElseThrow();

        commandBus.send(new DeleteBookCommand(Long.parseLong(existing.getId())));

        BookAggregate deleted = bookRepository.load(Long.parseLong(existing.getId()));

        assertTrue(deleted.isDeleted());
    }

    @Test
    void deleteTwiceThrowsException() {
        commandBus.send(new CreateBookCommand("Effective Java", "Joshua Bloch"));
        BookAggregate existing = bookRepository.findByTitle("Effective Java").orElseThrow();

        commandBus.send(new DeleteBookCommand(Long.parseLong(existing.getId())));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new DeleteBookCommand(Long.parseLong(existing.getId()))));
    }

    @Test
    void updateAfterDeleteThrowsException() {
        commandBus.send(new CreateBookCommand("Clean Architecture", "Robert C. Martin"));
        BookAggregate existing = bookRepository.findByTitle("Clean Architecture").orElseThrow();

        commandBus.send(new DeleteBookCommand(Long.parseLong(existing.getId())));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new UpdateBookCommand(Long.parseLong(existing.getId()), "Any", "Any")));
    }


    @Test
    void saveWithNonExistingIdOnUpdateThrowsException() {
        BookAggregate detached = BookAggregate.rehydrate("88888", "Ghost", "Nobody", false);

        assertThrows(NoSuchElementException.class, () -> bookRepository.save(detached));
    }

    @Test
    void loadNonExistingIdThrowsException() {
        assertThrows(NoSuchElementException.class, () -> bookRepository.load(99999L));
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
