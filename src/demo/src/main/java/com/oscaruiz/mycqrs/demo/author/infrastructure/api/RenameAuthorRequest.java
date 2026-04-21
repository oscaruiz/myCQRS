package com.oscaruiz.mycqrs.demo.author.infrastructure.api;

import com.oscaruiz.mycqrs.demo.author.application.command.RenameAuthorCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * HTTP-layer DTO. Validation annotations here exist solely to provide
 * field-level error feedback to API clients via
 * {@code MethodArgumentNotValidException}.
 *
 * <p>The authoritative validation lives in {@link RenameAuthorCommand} and is
 * enforced by {@code ValidationCommandInterceptor} for all entry points.
 */
public record RenameAuthorRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName

) {
    public RenameAuthorCommand toCommand(String id) {
        return new RenameAuthorCommand(id, firstName, lastName);
    }
}
