package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.author.application.command.CreateAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.command.DeleteAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import com.oscaruiz.mycqrs.demo.author.application.query.FindAuthorByIdQuery;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity;
import com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository;
import com.oscaruiz.mycqrs.demo.book.application.command.AddAuthorToBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.query.AuthorSummary;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.book.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorRetiredException;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * TOCTOU race between {@code AddAuthorToBookCommand} and
 * {@code DeleteAuthorCommand} on the same author. The test does not try to
 * force a specific outcome — it launches both commands concurrently and
 * verifies that <em>regardless of which won</em>, the system converges to a
 * consistent state:
 *
 * <ul>
 *   <li>If the add was rejected ({@link AuthorRetiredException}) the book
 *       must not reference the author.</li>
 *   <li>If the add succeeded, the book may reference the author, but only
 *       with {@code retired=true} (the delete's projection must eventually
 *       flag every embedded reference).</li>
 * </ul>
 *
 * A referenced author that is live in its own read model but whose embedded
 * entry in a book still has {@code retired=false} — or, symmetrically, an
 * author referenced by a book without appearing in {@code AuthorReadModel}'s
 * {@code books} list — would be corrupt state. This test asserts neither
 * happens.
 */
@SpringBootTest(classes = BookAuthorEventualConsistencyTest.TestConfig.class)
@ActiveProfiles("test")
class BookAuthorEventualConsistencyTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private OutboxPoller outboxPoller;

    @Test
    void toctouRace_addAuthorVsDeleteAuthor_convergesToConsistentState() {
        String authorId = UUID.randomUUID().toString();
        String bookId = UUID.randomUUID().toString();

        commandBus.send(new CreateAuthorCommand(UUID.randomUUID(),authorId, "George", "Orwell", 1903));
        commandBus.send(new CreateBookCommand(UUID.randomUUID(),bookId, "1984"));
        outboxPoller.poll();

        CyclicBarrier start = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Throwable> addError = new AtomicReference<>();
        AtomicReference<Throwable> deleteError = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            pool.submit(() -> {
                try {
                    start.await(5, TimeUnit.SECONDS);
                    commandBus.send(new AddAuthorToBookCommand(UUID.randomUUID(),bookId, authorId));
                } catch (Throwable t) {
                    addError.set(t);
                } finally {
                    done.countDown();
                }
            });
            pool.submit(() -> {
                try {
                    start.await(5, TimeUnit.SECONDS);
                    commandBus.send(new DeleteAuthorCommand(UUID.randomUUID(),authorId));
                } catch (Throwable t) {
                    deleteError.set(t);
                } finally {
                    done.countDown();
                }
            });

            assertTimeoutPreemptively(Duration.ofSeconds(10),
                    () -> done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // Drain the outbox: projections may produce new work, and cross-
        // aggregate denormalization runs in a subsequent poll.
        drainOutbox();

        boolean addRejected = isAuthorRetired(addError.get());
        assertThat(deleteError.get())
                .as("delete must succeed regardless of ordering")
                .isNull();

        AuthorResponse author = queryBus.handle(new FindAuthorByIdQuery(authorId));
        assertThat(author.deleted())
                .as("author must be soft-deleted in the read model after the race drains")
                .isTrue();

        Optional<BookResponse> bookOpt = tryFindBook(bookId);
        assertThat(bookOpt).isPresent();
        BookResponse book = bookOpt.get();

        if (addRejected) {
            assertThat(book.authors())
                    .as("add was rejected by existence checker; book must not reference the author")
                    .isEmpty();
            assertThat(author.books())
                    .as("author was never linked to this book; its books list must not contain it")
                    .noneMatch(b -> bookId.equals(b.bookId()));
        } else {
            assertThat(book.authors())
                    .as("add succeeded; book must reference the author exactly once")
                    .hasSize(1);
            AuthorSummary summary = book.authors().get(0);
            assertThat(summary.authorId()).isEqualTo(authorId);
            assertThat(summary.retired())
                    .as("the author was soft-deleted; every book reference must be flagged retired=true — the critical invariant")
                    .isTrue();
        }
    }

    /**
     * Drains the outbox in a small loop. A single {@link OutboxPoller#poll()}
     * is enough for one wave of projections, but the TOCTOU race can produce
     * two waves (add event + delete event) whose projections touch the same
     * documents in either order. Poll a few times to let the system quiesce.
     */
    private void drainOutbox() {
        for (int i = 0; i < 5; i++) {
            outboxPoller.poll();
        }
    }

    private boolean isAuthorRetired(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AuthorRetiredException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Optional<BookResponse> tryFindBook(String id) {
        try {
            return Optional.of(queryBus.handle(new FindBookByIdQuery(id)));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
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
