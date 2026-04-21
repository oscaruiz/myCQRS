package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.JpaBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OptimisticLockingIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OptimisticLockingIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private SpringDataBookRepository springDataBookRepository;

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private LoadBarrierHolder barrierHolder;

    @AfterEach
    void clearBarrier() {
        barrierHolder.clear();
    }

    @Test
    void staleSecondSave_throwsOptimisticLockException() {
        UUID id = UUID.randomUUID();
        springDataBookRepository.save(new BookEntity(id, "Original", false));

        BookEntity first = springDataBookRepository.findById(id).orElseThrow();
        BookEntity second = springDataBookRepository.findById(id).orElseThrow();

        first.update("First writer wins", false);
        springDataBookRepository.save(first);

        second.update("Second writer is stale", false);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> springDataBookRepository.save(second));

        BookEntity persisted = springDataBookRepository.findById(id).orElseThrow();
        assertEquals("First writer wins", persisted.getTitle());
    }

    /**
     * Concurrent writers via the command bus. The race window inside a single
     * handler is too short to reliably hit under hot JVM + warm Spring context:
     * the two handler invocations serialize naturally and both commit cleanly.
     *
     * To make the race deterministic we inject a {@link LoadBookRepositoryBarrier}
     * decorator as the primary {@link BookRepository}. Every call to
     * {@code load(id)} awaits on a shared {@link CyclicBarrier}, forcing both
     * worker threads to have read the same {@code version} before either
     * proceeds to mutate and commit. The commit order is still decided by the
     * scheduler, but exactly one commit succeeds — the other hits the
     * {@code @Version} check and fails.
     */
    @Test
    void concurrentUpdateCommands_exactlyOneFailsWithOptimisticLock() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Original"));

        int workers = 2;
        CyclicBarrier bothLoaded = new CyclicBarrier(workers);
        barrierHolder.install(bothLoaded);

        CountDownLatch done = new CountDownLatch(workers);
        AtomicReferenceArray<Throwable> errors = new AtomicReferenceArray<>(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        try {
            for (int i = 0; i < workers; i++) {
                final int idx = i;
                pool.submit(() -> {
                    try {
                        commandBus.send(new UpdateBookCommand(id, "Writer-" + idx));
                    } catch (Throwable t) {
                        errors.set(idx, t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertTimeoutPreemptively(Duration.ofSeconds(10),
                    () -> done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        int failures = 0;
        Throwable failure = null;
        for (int i = 0; i < workers; i++) {
            Throwable t = errors.get(i);
            if (t != null) {
                failures++;
                failure = t;
            }
        }

        assertEquals(1, failures,
                "Expected exactly one writer to fail; errors were " + dump(errors));
        assertTrue(isOptimisticLockFailure(failure),
                "Expected ObjectOptimisticLockingFailureException in cause chain, got: " + failure);

        BookEntity persisted = springDataBookRepository.findById(UUID.fromString(id)).orElseThrow();
        assertTrue(persisted.getTitle().startsWith("Writer-"),
                "Persisted title should come from the winning writer, was: " + persisted.getTitle());
    }

    private static boolean isOptimisticLockFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ObjectOptimisticLockingFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String dump(AtomicReferenceArray<Throwable> errors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < errors.length(); i++) {
            if (i > 0) sb.append(", ");
            Throwable t = errors.get(i);
            sb.append(i).append("=").append(t == null ? "OK" : t.getClass().getSimpleName());
        }
        return sb.append("]").toString();
    }

    /**
     * Test-only shared handle: tests install a {@link CyclicBarrier}, the
     * decorator below reads it on every {@code load()}. When unset, the
     * decorator is transparent.
     */
    static class LoadBarrierHolder {
        private final AtomicReference<CyclicBarrier> barrier = new AtomicReference<>();

        void install(CyclicBarrier barrier) {
            this.barrier.set(barrier);
        }

        void clear() {
            barrier.set(null);
        }

        CyclicBarrier current() {
            return barrier.get();
        }
    }

    /**
     * {@link BookRepository} decorator that awaits {@link LoadBarrierHolder}'s
     * barrier (if any) AFTER the real {@code load()} returns — ensuring every
     * concurrent reader has captured the current {@code @Version} before any
     * mutation can proceed.
     */
    static class LoadBookRepositoryBarrier implements BookRepository {
        private final JpaBookRepository delegate;
        private final LoadBarrierHolder holder;

        LoadBookRepositoryBarrier(JpaBookRepository delegate, LoadBarrierHolder holder) {
            this.delegate = delegate;
            this.holder = holder;
        }

        @Override
        public void save(BookAggregate bookAggregate) {
            delegate.save(bookAggregate);
        }

        @Override
        public BookAggregate load(String id) {
            BookAggregate aggregate = delegate.load(id);
            CyclicBarrier barrier = holder.current();
            if (barrier != null) {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Barrier wait failed in test decorator", e);
                }
            }
            return aggregate;
        }

        @Override
        public Optional<BookAggregate> findByTitle(String title) {
            return delegate.findByTitle(title);
        }
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
    @EnableJpaRepositories(basePackageClasses = {SpringDataBookRepository.class, com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.SpringDataAuthorRepository.class})
    @EntityScan(basePackageClasses = {BookEntity.class, com.oscaruiz.mycqrs.demo.author.infrastructure.jpa.AuthorEntity.class})
    static class TestConfig {

        @Bean
        LoadBarrierHolder barrierHolder() {
            return new LoadBarrierHolder();
        }

        @Bean
        @Primary
        BookRepository barrierBookRepository(JpaBookRepository delegate, LoadBarrierHolder holder) {
            return new LoadBookRepositoryBarrier(delegate, holder);
        }
    }
}
