package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class UpdateBookCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String bookId;

    @NotBlank
    private final String title;

    public UpdateBookCommand(UUID commandId, String bookId, String title) {
        this.commandId = commandId;
        this.bookId = bookId;
        this.title = title;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }
}
