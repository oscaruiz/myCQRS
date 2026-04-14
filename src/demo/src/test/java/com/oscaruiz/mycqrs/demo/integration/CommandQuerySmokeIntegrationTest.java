package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByTitleQuery;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = CommandQuerySmokeIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class CommandQuerySmokeIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Test
    void createCommandThenFindQueryReturnsCreatedBook() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Clean Architecture", "Robert C. Martin"));

        Book found = queryBus.handle(new FindBookByTitleQuery("Clean Architecture"));

        assertNotNull(found);
        assertEquals("Clean Architecture", found.getTitle());
        assertEquals("Robert C. Martin", found.getAuthor());
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
    }
}
