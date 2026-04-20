package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    @Test
    void staleSecondSave_throwsOptimisticLockException() {
        UUID id = UUID.randomUUID();
        springDataBookRepository.save(new BookEntity(id, "Original", "Author", false));

        BookEntity first = springDataBookRepository.findById(id).orElseThrow();
        BookEntity second = springDataBookRepository.findById(id).orElseThrow();

        first.update("First writer wins", "Author", false);
        springDataBookRepository.save(first);

        second.update("Second writer is stale", "Author", false);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> springDataBookRepository.save(second));

        BookEntity persisted = springDataBookRepository.findById(id).orElseThrow();
        assertEquals("First writer wins", persisted.getTitle());
    }

    @Test
    void concurrentUpdateCommands_exactlyOneFailsWithOptimisticLock() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Original", "Author"));

        int workers = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicReferenceArray<Throwable> errors = new AtomicReferenceArray<>(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        try {
            for (int i = 0; i < workers; i++) {
                final int idx = i;
                pool.submit(() -> {
                    try {
                        start.await();
                        commandBus.send(new UpdateBookCommand(id, "Writer-" + idx, "Author"));
                    } catch (Throwable t) {
                        errors.set(idx, t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();

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
