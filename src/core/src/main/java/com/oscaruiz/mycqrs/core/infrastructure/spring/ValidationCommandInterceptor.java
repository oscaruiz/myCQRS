package com.oscaruiz.mycqrs.core.infrastructure.spring;
import com.oscaruiz.mycqrs.core.domain.command.Command;
import com.oscaruiz.mycqrs.core.domain.command.CommandInterceptor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;

public class ValidationCommandInterceptor implements CommandInterceptor {

    private final Validator validator;

    public ValidationCommandInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Object intercept(Command command, CommandHandlerInvoker next) {
        Set<ConstraintViolation<Command>> violations = validator.validate(command);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((m1, m2) -> m1 + "; " + m2)
                    .orElse("Validation error");
            throw new IllegalArgumentException("🚫 Invalid command: " + message);
        }

        return next.invoke(command);
    }
}
