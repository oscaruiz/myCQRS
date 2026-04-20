package com.oscaruiz.mycqrs.demo.book.infrastructure.api;

import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * HTTP-layer DTO. Validation annotations here exist solely to provide
 * field-level error feedback to API clients via
 * {@code MethodArgumentNotValidException}.
 *
 * <p>The authoritative validation lives in {@link CreateBookCommand} and is
 * enforced by {@code ValidationCommandInterceptor} for all entry points
 * (HTTP, tests, future async consumers). The duplication is deliberate.
 */
public record CreateBookRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author

) {
    public CreateBookCommand toCommand(UUID id) {
        return new CreateBookCommand(id.toString(), title, author);
    }
}