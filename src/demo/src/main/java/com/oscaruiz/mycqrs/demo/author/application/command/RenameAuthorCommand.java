package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;

public class RenameAuthorCommand implements Command {

    @NotBlank
    private final String authorId;

    @NotBlank
    private final String firstName;

    @NotBlank
    private final String lastName;

    public RenameAuthorCommand(String authorId, String firstName, String lastName) {
        this.authorId = authorId;
        this.firstName = firstName;
        this.lastName = lastName;
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
