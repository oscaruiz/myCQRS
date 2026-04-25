package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationCommandInterceptorTest {

    @Mock
    private Validator validator;

    @Mock
    private CommandInterceptor.CommandHandlerInvoker next;

    private ValidationCommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ValidationCommandInterceptor(validator);
    }

    @Test
    void passesToNextWhenNoViolations() {
        Command command = new FakeCommand();
        when(validator.validate(command)).thenReturn(Set.of());

        assertThatCode(() -> interceptor.intercept(command, next)).doesNotThrowAnyException();

        verify(next).invoke(command);
    }

    @Test
    void throwsConstraintViolationExceptionAndSkipsNextWhenViolationsPresent() {
        Command command = new FakeCommand();
        @SuppressWarnings("unchecked")
        ConstraintViolation<Command> violation = (ConstraintViolation<Command>) org.mockito.Mockito.mock(ConstraintViolation.class);
        when(validator.validate(command)).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> interceptor.intercept(command, next))
                .isInstanceOf(ConstraintViolationException.class);

        verify(next, never()).invoke(any());
    }

    private record FakeCommand(UUID commandId) implements Command {
        FakeCommand() {
            this(UUID.randomUUID());
        }
    }
}
