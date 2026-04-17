package com.oscaruiz.mycqrs.demo.integration;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByTitleQuery;
import com.oscaruiz.mycqrs.demo.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.outbox.OutboxPoller;
import com.oscaruiz.mycqrs.demo.integration.support.MongoTestcontainersTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EnableCqrs;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = CommandQuerySmokeIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class CommandQuerySmokeIntegrationTest extends MongoTestcontainersTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private OutboxPoller outboxPoller;

    @Test
    void createCommandThenFindQueryReturnsCreatedBook() {
        String id = UUID.randomUUID().toString();
        commandBus.send(new CreateBookCommand(id, "Clean Architecture", "Robert C. Martin"));

        outboxPoller.poll();

        BookResponse found = queryBus.handle(new FindBookByTitleQuery("Clean Architecture"));

        assertNotNull(found);
        assertEquals("Clean Architecture", found.title());
        assertEquals("Robert C. Martin", found.author());
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
