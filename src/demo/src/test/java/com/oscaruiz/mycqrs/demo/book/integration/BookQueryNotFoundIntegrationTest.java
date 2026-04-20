package com.oscaruiz.mycqrs.demo.book.integration;

import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.book.integration.support.AbstractFullStackIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.book.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BookQueryNotFoundIntegrationTest.TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookQueryNotFoundIntegrationTest extends AbstractFullStackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getBookById_whenRepositoryEmpty_returns404() throws Exception {
        mockMvc.perform(get("/books/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBookByTitle_whenRepositoryEmpty_returns404() throws Exception {
        mockMvc.perform(get("/books").param("title", "Nonexistent-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCqrs
    @ComponentScan(basePackages = {
            "com.oscaruiz.mycqrs.demo.book.application",
            "com.oscaruiz.mycqrs.demo.book.domain",
            "com.oscaruiz.mycqrs.demo.book.infrastructure"
    })
    @EnableJpaRepositories(basePackageClasses = SpringDataBookRepository.class)
    @EntityScan(basePackageClasses = BookEntity.class)
    static class TestConfig {
    }
}
