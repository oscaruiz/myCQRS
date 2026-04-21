package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;

public class RemoveAuthorFromBookCommand implements Command {

    @NotBlank
    private final String bookId;

    @NotBlank
    private final String authorId;

    public RemoveAuthorFromBookCommand(String bookId, String authorId) {
        this.bookId = bookId;
        this.authorId = authorId;
    }

    public String getBookId() {
        return bookId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
