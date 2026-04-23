package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddAuthorToBookCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String bookId;

    @NotBlank
    private final String authorId;

    public AddAuthorToBookCommand(UUID commandId, String bookId, String authorId) {
        this.commandId = commandId;
        this.bookId = bookId;
        this.authorId = authorId;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getBookId() {
        return bookId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
