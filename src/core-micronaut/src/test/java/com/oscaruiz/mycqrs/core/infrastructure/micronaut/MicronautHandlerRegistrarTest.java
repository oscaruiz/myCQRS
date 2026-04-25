package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots a real Micronaut context with {@link CqrsFactory} and
 * {@link MicronautHandlerRegistrar} and verifies that a handler declared as a
 * {@link Singleton} bean gets auto-wired to the {@link CommandBus} without any
 * explicit registration call.
 */
@MicronautTest
class MicronautHandlerRegistrarTest {

    @Inject
    CommandBus commandBus;

    @Inject
    FakeCommandHandler handler;

    @Test
    void autoRegistersCommandHandlerDiscoveredAsBean() {
        FakeCommand command = new FakeCommand();

        commandBus.send(command);

        assertThat(handler.lastSeen.get()).isSameAs(command);
    }

    @Introspected
    record FakeCommand(UUID commandId) implements Command {
        FakeCommand() {
            this(UUID.randomUUID());
        }
    }

    @Singleton
    static class FakeCommandHandler implements CommandHandler<FakeCommand> {
        final AtomicReference<FakeCommand> lastSeen = new AtomicReference<>();

        @Override
        public void handle(FakeCommand command) {
            lastSeen.set(command);
        }
    }
}
