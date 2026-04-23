package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateBookCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String id;

    @NotBlank
    private final String title;

    public CreateBookCommand(UUID commandId, String id, String title) {
        this.commandId = commandId;
        this.id = id;
        this.title = title;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
