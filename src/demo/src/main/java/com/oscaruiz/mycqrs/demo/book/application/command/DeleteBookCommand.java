package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class DeleteBookCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String bookId;

    public DeleteBookCommand(UUID commandId, String bookId) {
        this.commandId = commandId;
        this.bookId = bookId;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getBookId() {
        return bookId;
    }
}
