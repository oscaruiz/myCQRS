package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import jakarta.validation.constraints.NotBlank;

public class CreateBookCommand implements Command {

    @NotBlank(message = "Title is required")
    private final String title;

    @NotBlank(message = "Author is required")
    private final String author;

    public CreateBookCommand(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
}
