package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateAuthorCommand implements Command {

    @NotNull
    private final UUID commandId;

    @NotBlank
    private final String id;

    @NotBlank
    private final String firstName;

    @NotBlank
    private final String lastName;

    private final Integer birthYear;

    public CreateAuthorCommand(UUID commandId, String id, String firstName, String lastName, Integer birthYear) {
        this.commandId = commandId;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }
}
