package com.oscaruiz.mycqrs.core.idempotency;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor.CommandHandlerInvoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyCommandInterceptorTest {

    @Mock
    private ProcessedCommandsStore store;

    @InjectMocks
    private IdempotencyCommandInterceptor interceptor;

    @Test
    void delegates_to_next_when_store_inserts_first_time() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);
        when(store.markProcessedIfAbsent(command.commandId(), "FakeCommand")).thenReturn(true);

        interceptor.intercept(command, next);

        verify(next).invoke(command);
    }

    @Test
    void skips_next_silently_when_command_already_processed() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);
        when(store.markProcessedIfAbsent(command.commandId(), "FakeCommand")).thenReturn(false);

        assertThatNoException().isThrownBy(() -> interceptor.intercept(command, next));

        verifyNoInteractions(next);
    }

    @Test
    void passes_commandId_and_simple_class_name_to_store() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);
        when(store.markProcessedIfAbsent(command.commandId(), "FakeCommand")).thenReturn(true);

        interceptor.intercept(command, next);

        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(store).markProcessedIfAbsent(idCaptor.capture(), typeCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(command.commandId());
        assertThat(typeCaptor.getValue()).isEqualTo("FakeCommand");
    }

    record FakeCommand(UUID commandId) implements Command {}
}
