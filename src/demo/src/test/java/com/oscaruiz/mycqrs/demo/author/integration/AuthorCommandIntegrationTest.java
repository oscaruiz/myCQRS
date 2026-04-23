package com.oscaruiz.mycqrs.demo.author.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.author.application.command.CreateAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.command.DeleteAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.command.RenameAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;
import com.oscaruiz.mycqrs.demo.author.domain.repository.AuthorRepository;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AuthorCommandIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class AuthorCommandIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createAuthorCommandSavesAuthor() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),id, "George", "Orwell", 1903));

        AuthorAggregate saved = authorRepository.load(id);

        assertEquals(id, saved.getId());
        assertEquals("George", saved.getFirstName());
        assertEquals("Orwell", saved.getLastName());
        assertEquals(1903, saved.getBirthYear());
        assertEquals(false, saved.isDeleted());

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?", Integer.class, id);
        assertEquals(1, outboxCount);
    }

    @Test
    void renameAuthorCommandChangesValues() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),id, "George", "Orwell", 1903));

        commandBus.send(new RenameAuthorCommand(UUID.randomUUID(),id, "Eric", "Blair"));

        AuthorAggregate renamed = authorRepository.load(id);
        assertEquals("Eric", renamed.getFirstName());
        assertEquals("Blair", renamed.getLastName());

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?", Integer.class, id);
        assertEquals(2, outboxCount);
    }

    @Test
    void renameNoOpWhenNothingChanges() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),id, "George", "Orwell", 1903));

        commandBus.send(new RenameAuthorCommand(UUID.randomUUID(),id, "George", "Orwell"));

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?", Integer.class, id);
        assertEquals(1, outboxCount);
    }

    @Test
    void deleteAuthorCommandMarksSoftDeleted() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),id, "George", "Orwell", 1903));

        commandBus.send(new DeleteAuthorCommand(UUID.randomUUID(),id));

        AuthorAggregate deleted = authorRepository.load(id);
        assertTrue(deleted.isDeleted());
    }

    @Test
    void deleteTwiceThrowsException() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),id, "George", "Orwell", 1903));
        commandBus.send(new DeleteAuthorCommand(UUID.randomUUID(),id));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new DeleteAuthorCommand(UUID.randomUUID(),id)));
    }

    @Test
    void renameAfterDeleteThrowsException() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),id, "George", "Orwell", 1903));
        commandBus.send(new DeleteAuthorCommand(UUID.randomUUID(),id));

        assertThrows(IllegalStateException.class,
                () -> commandBus.send(new RenameAuthorCommand(UUID.randomUUID(),id, "Eric", "Blair")));
    }

    @Test
    void loadNonExistingIdThrowsException() {
        assertThrows(NoSuchElementException.class,
                () -> authorRepository.load(UUID.randomUUID().toString()));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.author.application",
            "com.oscaruiz.mycqrs.demo.author.domain",
            "com.oscaruiz.mycqrs.demo.author.infrastructure",
            "com.oscaruiz.mycqrs.demo.book.infrastructure.outbox"
    })
    @EnableJpaRepositories(basePackageClasses = {
            SpringDataAuthorRepository.class,
            SpringDataBookRepository.class
    })
    @EntityScan(basePackageClasses = {AuthorEntity.class, BookEntity.class})
    static class TestConfig {
    }
}
