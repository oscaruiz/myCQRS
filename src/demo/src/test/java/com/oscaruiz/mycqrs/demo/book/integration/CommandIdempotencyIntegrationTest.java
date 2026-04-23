package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CommandIdempotencyIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class CommandIdempotencyIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private SpringDataBookRepository bookRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void sameCommandTwice_isNoOp_onSecondSend() {
        UUID commandId = UUID.randomUUID();
        String bookId = UUID.randomUUID().toString();
        CreateBookCommand command = new CreateBookCommand(commandId, bookId, "Exactly-Once");

        commandBus.send(command);
        commandBus.send(command);

        assertThat(bookRepository.count()).isEqualTo(1);
        assertThat(processedCommandsRowCount(commandId))
                .as("duplicate send must have inserted exactly one ledger row")
                .isEqualTo(1);
    }

    @Test
    void handlerFailure_rollsBackProcessedCommandsRow() {
        UUID commandId = UUID.randomUUID();
        String missingBookId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> commandBus.send(new DeleteBookCommand(commandId, missingBookId)))
                .as("deleting a non-existent book must bubble up from the handler");

        assertThat(processedCommandsRowCount(commandId))
                .as("ledger insert must roll back together with the handler's failed tx")
                .isZero();
    }

    /**
     * Behavioral twin of {@link #handlerFailure_rollsBackProcessedCommandsRow}. That test
     * asserts the ledger is clean at the SQL level after a failure; this one asserts the
     * observable consequence — a retry with the same {@code commandId} re-executes the
     * handler rather than being silently skipped, and the successful retry marks the
     * ledger exactly once. Together they pin ADR 0011's "exactly-once side effect" claim.
     */
    @Test
    void retryAfterHandlerFailure_reExecutesHandler_andMarksOnce() {
        UUID commandId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        assertThatThrownBy(() -> commandBus.send(new TestIdempotencyCommand(commandId, bookId, true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("simulated handler failure");

        assertThat(bookRepository.findById(bookId))
                .as("failed handler must not have persisted the book")
                .isEmpty();
        assertThat(processedCommandsRowCount(commandId))
                .as("failed handler must have rolled back the ledger row")
                .isZero();

        commandBus.send(new TestIdempotencyCommand(commandId, bookId, false));

        assertThat(bookRepository.findById(bookId))
                .as("retry must have re-executed the handler and persisted the book")
                .isPresent();
        assertThat(processedCommandsRowCount(commandId))
                .as("successful retry must mark the ledger exactly once")
                .isEqualTo(1);
    }

    private int processedCommandsRowCount(UUID commandId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM processed_commands WHERE command_id = :commandId",
                new MapSqlParameterSource("commandId", commandId),
                Integer.class);
        return count == null ? 0 : count;
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
    @Import(TestHandlerConfig.class)
    static class TestConfig {
    }

    @TestConfiguration
    static class TestHandlerConfig {
        @Bean
        TestIdempotencyCommandHandler testIdempotencyCommandHandler(BookRepository bookRepository) {
            return new TestIdempotencyCommandHandler(bookRepository);
        }
    }

    record TestIdempotencyCommand(UUID commandId, UUID bookId, boolean failMe) implements Command {}

    static class TestIdempotencyCommandHandler implements CommandHandler<TestIdempotencyCommand> {

        private final BookRepository bookRepository;

        TestIdempotencyCommandHandler(BookRepository bookRepository) {
            this.bookRepository = bookRepository;
        }

        @Override
        public void handle(TestIdempotencyCommand command) {
            if (command.failMe()) {
                throw new RuntimeException("simulated handler failure");
            }
            BookAggregate aggregate = BookAggregate.create(command.bookId().toString(), "retry-atomicity-test");
            bookRepository.save(aggregate);
        }
    }
}
