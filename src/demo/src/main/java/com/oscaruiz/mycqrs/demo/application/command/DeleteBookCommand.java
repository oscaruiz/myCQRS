package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import jakarta.validation.constraints.NotNull;

public class DeleteBookCommand implements Command {

    @NotNull(message = "Book id is required")
    private final Long bookId;

    public DeleteBookCommand(Long bookId) {
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }
}
