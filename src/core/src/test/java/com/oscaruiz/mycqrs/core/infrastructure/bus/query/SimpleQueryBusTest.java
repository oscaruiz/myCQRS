package com.oscaruiz.mycqrs.core.infrastructure.bus.query;

import com.oscaruiz.mycqrs.core.domain.query.DuplicateQueryHandlerException;
import com.oscaruiz.mycqrs.core.domain.query.Query;
import com.oscaruiz.mycqrs.core.domain.query.QueryHandler;
import com.oscaruiz.mycqrs.core.domain.query.QueryHandlerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleQueryBusTest {

    private SimpleQueryBus bus;

    @BeforeEach
    void setUp() {
        bus = new SimpleQueryBus();
    }

    @Test
    void shouldReturnResult_whenHandlerIsRegistered() {
        bus.registerHandler(FakeQuery.class, new FakeQueryHandler());

        String result = bus.handle(new FakeQuery());

        assertEquals("result", result);
    }

    @Test
    void shouldThrowWithQueryTypeName_whenNoHandlerRegistered() {
        FakeQuery query = new FakeQuery();

        QueryHandlerNotFoundException exception = assertThrows(
            QueryHandlerNotFoundException.class,
            () -> bus.handle(query)
        );

        assertTrue(exception.getMessage().contains("FakeQuery"));
    }

    @Test
    void shouldThrowWithQueryTypeName_whenHandlerAlreadyRegistered() {
        bus.registerHandler(FakeQuery.class, new FakeQueryHandler());

        DuplicateQueryHandlerException exception = assertThrows(
            DuplicateQueryHandlerException.class,
            () -> bus.registerHandler(FakeQuery.class, new FakeQueryHandler())
        );

        assertTrue(exception.getMessage().contains("FakeQuery"));
    }

    static class FakeQuery implements Query<String> {
    }

    static class FakeQueryHandler implements QueryHandler<FakeQuery, String> {
        @Override
        public String handle(FakeQuery query) {
            return "result";
        }
    }
}
