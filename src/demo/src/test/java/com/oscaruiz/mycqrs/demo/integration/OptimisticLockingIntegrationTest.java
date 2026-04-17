package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.integration.support.MongoTestcontainersTest;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = OptimisticLockingIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class OptimisticLockingIntegrationTest extends MongoTestcontainersTest {

    @Autowired
    private SpringDataBookRepository springDataBookRepository;

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
