package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;

public class UpdateBookCommand implements Command {

    @NotBlank
    private final String bookId;

    @NotBlank
    private final String title;

    public UpdateBookCommand(String bookId, String title) {
        this.bookId = bookId;
        this.title = title;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }
}
