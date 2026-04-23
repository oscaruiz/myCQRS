package com.oscaruiz.mycqrs.core.infrastructure.bus.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.command.CommandHandlerNotFoundException;
import com.oscaruiz.mycqrs.core.contracts.command.DuplicateCommandHandlerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void interceptor_can_modify_command_before_handler_receives_it() {
        FakeCommandHandler handler = new FakeCommandHandler();
        bus.registerHandler(FakeCommand.class, handler);

        bus.addInterceptor((command, next) -> {
            FakeCommand original = (FakeCommand) command;
            FakeCommand modified = new FakeCommand(original.value() + "-modified");
            next.invoke(modified);
        });

        bus.send(new FakeCommand("original"));

        assertThat(handler.received.value()).isEqualTo("original-modified");
    }

    @Test
    void interceptors_execute_in_registration_order() {
        FakeCommandHandler handler = new FakeCommandHandler();
        bus.registerHandler(FakeCommand.class, handler);

        List<String> markers = new ArrayList<>();

        bus.addInterceptor((command, next) -> {
            markers.add("first-before");
            next.invoke(command);
            markers.add("first-after");
        });

        bus.addInterceptor((command, next) -> {
            markers.add("second-before");
            next.invoke(command);
            markers.add("second-after");
        });

        bus.send(new FakeCommand());

        assertThat(markers).containsExactly(
            "first-before", "second-before", "second-after", "first-after"
        );
    }

    static class FakeCommand implements Command {
        private final UUID commandId = UUID.randomUUID();
        private final String value;

        FakeCommand() { this(null); }
        FakeCommand(String value) { this.value = value; }

        @Override
        public UUID commandId() { return commandId; }

        String value() { return value; }
    }

    static class FakeCommandHandler implements CommandHandler<FakeCommand> {

        boolean wasExecuted = false;
        FakeCommand received;

        @Override
        public void handle(FakeCommand command) {
            wasExecuted = true;
            received = command;
        }
    }
}
