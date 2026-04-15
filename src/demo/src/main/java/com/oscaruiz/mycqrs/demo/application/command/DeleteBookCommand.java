package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;

public class DeleteBookCommand implements Command {

    @NotBlank
    private final String bookId;

    public DeleteBookCommand(String bookId) {
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }
}
