package com.oscaruiz.mycqrs.demo.author.infrastructure.api;

import com.oscaruiz.mycqrs.demo.author.application.command.CreateAuthorCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * HTTP-layer DTO. Validation annotations here exist solely to provide
 * field-level error feedback to API clients via
 * {@code MethodArgumentNotValidException}.
 *
 * <p>The authoritative validation lives in {@link CreateAuthorCommand} and is
 * enforced by {@code ValidationCommandInterceptor} for all entry points
 * (HTTP, tests, future async consumers). The duplication is deliberate.
 */
public record CreateAuthorRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        Integer birthYear

) {
    public CreateAuthorCommand toCommand(UUID commandId, UUID id) {
        return new CreateAuthorCommand(commandId, id.toString(), firstName, lastName, birthYear);
    }
}
