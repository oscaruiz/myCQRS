package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.mongo.BookEventLog;
import com.oscaruiz.mycqrs.demo.infrastructure.mongo.BookEventLogRepository;
import com.oscaruiz.mycqrs.demo.infrastructure.mongo.BookOperation;
import com.oscaruiz.mycqrs.demo.integration.support.MongoTestcontainersTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Reactivated in Day 8 when the outbox poller drives projections")
@SpringBootTest(classes = BookLifecycleIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class BookLifecycleIntegrationTest extends MongoTestcontainersTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private BookEventLogRepository eventLogRepository;

    @Test
    void createThenUpdate_queryReflectsUpdate() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Original Title", "Author"));
        commandBus.send(new UpdateBookCommand(id, "Updated Title", "Author"));

        Book book = queryBus.handle(new FindBookByIdQuery(id));

        assertEquals("Updated Title", book.getTitle());
        assertEquals("Author", book.getAuthor());
    }

    @Test
    void createThenDelete_queryThrowsNotFound() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Soon to die", "Author"));
        commandBus.send(new DeleteBookCommand(id));

        assertThrows(NoSuchElementException.class,
                () -> queryBus.handle(new FindBookByIdQuery(id)));
    }

    @Test
    void deleteEmitsAuditLogEntry() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Audit Target", "Author"));
        commandBus.send(new DeleteBookCommand(id));

        List<BookEventLog> entriesForAggregate = eventLogRepository.findAll().stream()
                .filter(entry -> id.equals(entry.getAggregateId()))
                .toList();

        assertTrue(entriesForAggregate.stream()
                        .anyMatch(entry -> BookOperation.DELETE_BOOK.name().equals(entry.getOperation())),
                () -> "expected DELETE_BOOK audit entry for aggregate " + id + ", got " + entriesForAggregate);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.application",
            "com.oscaruiz.mycqrs.demo.domain",
            "com.oscaruiz.mycqrs.demo.infrastructure"
    })
    @EnableJpaRepositories(basePackageClasses = SpringDataBookRepository.class)
    @EntityScan(basePackageClasses = BookEntity.class)
    static class TestConfig {
    }
}
