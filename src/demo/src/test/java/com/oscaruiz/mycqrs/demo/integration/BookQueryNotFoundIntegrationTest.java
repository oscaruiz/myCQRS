package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.demo.application.query.BookReadModelRepository;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BookQueryNotFoundIntegrationTest.TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookQueryNotFoundIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getBookById_whenRepositoryEmpty_returns404() throws Exception {
        mockMvc.perform(get("/books/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBookByTitle_whenRepositoryEmpty_returns404() throws Exception {
        mockMvc.perform(get("/books").param("title", "Nonexistent"))
                .andExpect(status().isNotFound());
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
    static class TestConfig {
        @Bean
        BookReadModelRepository emptyBookReadModelRepository() {
            return new BookReadModelRepository() {
                @Override
                public Optional<Book> findById(String id) {
                    return Optional.empty();
                }

                @Override
                public Optional<Book> findByTitle(String title) {
                    return Optional.empty();
                }
            };
        }
    }
}
