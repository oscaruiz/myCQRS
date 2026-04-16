package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * HTTP-layer DTO. Validation annotations here exist solely to provide
 * field-level error feedback to API clients via
 * {@code MethodArgumentNotValidException}.
 *
 * <p>The authoritative validation lives in {@link UpdateBookCommand} and is
 * enforced by {@code ValidationCommandInterceptor} for all entry points
 * (HTTP, tests, future async consumers). The duplication is deliberate.
 */
public record UpdateBookRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author

) {
    public UpdateBookCommand toCommand(String id) {
        return new UpdateBookCommand(id, title, author);
    }
}
