package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.mongo.BookEventLog;
import com.oscaruiz.mycqrs.demo.infrastructure.mongo.BookEventLogRepository;
import com.oscaruiz.mycqrs.demo.infrastructure.mongo.BookOperation;
import com.oscaruiz.mycqrs.demo.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.integration.support.AbstractFullStackIntegrationTest;
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

@SpringBootTest(classes = BookLifecycleIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class BookLifecycleIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private BookEventLogRepository eventLogRepository;

    @Test
    void createThenUpdate_queryReflectsUpdate() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Original Title", "Author"));
        outboxPoller.poll();

        commandBus.send(new UpdateBookCommand(id, "Updated Title", "Author"));
        outboxPoller.poll();

        BookResponse book = queryBus.handle(new FindBookByIdQuery(id));

        assertEquals("Updated Title", book.title());
        assertEquals("Author", book.author());
    }

    @Test
    void createThenDelete_queryThrowsNotFound() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Soon to die", "Author"));
        outboxPoller.poll();

        commandBus.send(new DeleteBookCommand(id));
        outboxPoller.poll();

        assertThrows(NoSuchElementException.class,
                () -> queryBus.handle(new FindBookByIdQuery(id)));
    }

    @Test
    void deleteEmitsAuditLogEntry() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Audit Target", "Author"));
        outboxPoller.poll();

        commandBus.send(new DeleteBookCommand(id));
        outboxPoller.poll();

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
