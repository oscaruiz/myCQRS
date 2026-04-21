package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;

public class CreateAuthorCommand implements Command {

    @NotBlank
    private final String id;

    @NotBlank
    private final String firstName;

    @NotBlank
    private final String lastName;

    private final Integer birthYear;

    public CreateAuthorCommand(String id, String firstName, String lastName, Integer birthYear) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
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
