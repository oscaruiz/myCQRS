package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.Set;

/**
 * Runs Bean Validation over the command before handing it to the next interceptor.
 *
 * <p>Mirrors the Spring adapter's validation interceptor but throws
 * {@link ConstraintViolationException} — the idiomatic jakarta.validation exception —
 * instead of {@link IllegalArgumentException}. Micronaut's error-mapping conventions
 * expect the former; consumers bridging to HTTP will translate it (e.g. to 400) in
 * their exception handler.
 */
public class ValidationCommandInterceptor implements CommandInterceptor {

    private final Validator validator;

    public ValidationCommandInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public void intercept(Command command, CommandHandlerInvoker next) {
        Set<ConstraintViolation<Command>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        next.invoke(command);
    }
}
