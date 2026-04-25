package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor.CommandHandlerInvoker;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationCommandInterceptorTest {

    @Mock
    private Validator validator;

    @InjectMocks
    private ValidationCommandInterceptor interceptor;

    @Test
    void throws_when_validator_reports_violations() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);

        @SuppressWarnings("unchecked")
        ConstraintViolation<FakeCommand> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(Path.class));
        when(violation.getMessage()).thenReturn("must not be null");
        doReturn(Set.of(violation)).when(validator).validate(command);

        assertThatThrownBy(() -> interceptor.intercept(command, next))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Validation failed");

        verifyNoInteractions(next);
    }

    @Test
    void delegates_to_next_when_validation_passes() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = mock(CommandHandlerInvoker.class);

        when(validator.validate(command)).thenReturn(Set.of());

        interceptor.intercept(command, next);

        verify(next).invoke(command);
    }

    record FakeCommand(UUID commandId) implements Command {}
}
