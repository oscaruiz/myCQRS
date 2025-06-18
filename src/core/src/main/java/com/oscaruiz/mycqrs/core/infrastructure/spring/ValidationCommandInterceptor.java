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
            StringBuilder sb = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<Command> v : violations) {
                sb.append(String.format("[%s: %s] ", v.getPropertyPath(), v.getMessage()));
            }
            throw new IllegalArgumentException(sb.toString());
        }

        return next.invoke(command);
    }
}
