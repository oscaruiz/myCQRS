package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class DeleteAuthorCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String authorId;

    public DeleteAuthorCommand(UUID commandId, String authorId) {
        this.commandId = commandId;
        this.authorId = authorId;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
