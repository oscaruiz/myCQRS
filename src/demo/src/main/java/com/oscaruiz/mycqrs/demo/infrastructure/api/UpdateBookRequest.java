package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateBookRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author

) {
    public UpdateBookCommand toCommand(Long id) {
        return new UpdateBookCommand(id, title, author);
    }
}
