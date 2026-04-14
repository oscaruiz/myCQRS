package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

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