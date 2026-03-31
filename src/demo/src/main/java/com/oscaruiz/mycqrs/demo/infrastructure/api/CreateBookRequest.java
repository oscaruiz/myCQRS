package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author

) {
    public CreateBookCommand toCommand() {
        return new CreateBookCommand(title, author);
    }
}