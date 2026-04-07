package com.oscaruiz.mycqrs.core.infrastructure.bus.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.command.CommandHandlerNotFoundException;
import com.oscaruiz.mycqrs.core.domain.command.DuplicateCommandHandlerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCommandBusTest {

    private SimpleCommandBus bus;

    @BeforeEach
    void setUp() {
        bus = new SimpleCommandBus();
    }

    @Test
    void shouldExecuteHandler_whenRegisteredCommandIsSent() {
        FakeCommandHandler handler = new FakeCommandHandler();
        bus.registerHandler(FakeCommand.class, handler);

        bus.send(new FakeCommand());

        assertTrue(handler.wasExecuted);
    }

    @Test
    void shouldThrowWithCommandTypeName_whenNoHandlerRegistered() {
        FakeCommand command = new FakeCommand();

        CommandHandlerNotFoundException exception = assertThrows(
            CommandHandlerNotFoundException.class,
            () -> bus.send(command)
        );

        assertTrue(exception.getMessage().contains("FakeCommand"));
    }

    @Test
    void shouldThrowWithCommandTypeName_whenHandlerAlreadyRegistered() {
        FakeCommandHandler handler = new FakeCommandHandler();
        bus.registerHandler(FakeCommand.class, handler);

        DuplicateCommandHandlerException exception = assertThrows(
            DuplicateCommandHandlerException.class,
            () -> bus.registerHandler(FakeCommand.class, handler)
        );

        assertTrue(exception.getMessage().contains("FakeCommand"));
    }

    /**
     * Contract test: the bus must propagate handler exceptions transparently,
     * without wrapping or swallowing them. Domain exceptions thrown by handlers
     * (e.g. BookNotFoundException) need to reach the caller intact so they can
     * be mapped to the correct HTTP status by the controller advice.
     *
     * This guards against future refactors that might add a try/catch in
     * SimpleCommandBus.send() — for example to log errors before propagating —
     * which could accidentally change the exception type seen by callers.
     */
    @Test
    void shouldPropagateException_whenHandlerThrows() {
        CommandHandler<FakeCommand> handler = cmd -> { throw new TestException(); };
        bus.registerHandler(FakeCommand.class, handler);

        assertThrows(TestException.class, () -> bus.send(new FakeCommand()));
    }

    static class TestException extends RuntimeException {
    }

    static class FakeCommand implements Command {
    }

    static class FakeCommandHandler implements CommandHandler<FakeCommand> {

        boolean wasExecuted = false;

        @Override
        public void handle(FakeCommand command) {
            wasExecuted = true;
        }
    }
}
