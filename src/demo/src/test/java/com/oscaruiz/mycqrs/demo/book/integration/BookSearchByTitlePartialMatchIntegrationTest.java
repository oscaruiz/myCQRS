package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.application.query.BookReadModelRepository;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.mongo.BookMongoRepository;
import com.oscaruiz.mycqrs.demo.book.infrastructure.mongo.BookReadModel;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BookSearchByTitlePartialMatchIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class BookSearchByTitlePartialMatchIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private BookReadModelRepository bookReadModelRepository;

    @Autowired
    private BookMongoRepository bookMongoRepository;

    @BeforeEach
    void seed() {
        bookMongoRepository.save(new BookReadModel(UUID.randomUUID().toString(),
                "Domain-Driven Design", List.of()));
        bookMongoRepository.save(new BookReadModel(UUID.randomUUID().toString(),
                "Clean Code", List.of()));
    }

    @Test
    void partialLowercaseMatchReturnsDdd() {
        List<BookResponse> result = bookReadModelRepository.findByTitle("domain");

        assertEquals(1, result.size());
        assertEquals("Domain-Driven Design", result.get(0).title());
    }

    @Test
    void partialUppercaseMatchReturnsCleanCode() {
        List<BookResponse> result = bookReadModelRepository.findByTitle("CODE");

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).title());
    }

    @Test
    void noMatchReturnsEmptyList() {
        List<BookResponse> result = bookReadModelRepository.findByTitle("nothing-matches");

        assertTrue(result.isEmpty());
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
    }
}
