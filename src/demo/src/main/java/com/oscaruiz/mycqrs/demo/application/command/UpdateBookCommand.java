package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateBookCommand implements Command {

    @NotNull(message = "Book id is required")
    private final Long bookId;

    @NotBlank(message = "Title is required")
    private final String title;

    @NotBlank(message = "Author is required")
    private final String author;

    public UpdateBookCommand(Long bookId, String title, String author) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
