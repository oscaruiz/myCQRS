package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class RenameAuthorCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String authorId;

    @NotBlank
    private final String firstName;

    @NotBlank
    private final String lastName;

    public RenameAuthorCommand(UUID commandId, String authorId, String firstName, String lastName) {
        this.commandId = commandId;
        this.authorId = authorId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
