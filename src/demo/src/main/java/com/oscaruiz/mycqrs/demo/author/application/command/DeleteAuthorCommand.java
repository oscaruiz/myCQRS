package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import jakarta.validation.constraints.NotBlank;

public class DeleteAuthorCommand implements Command {

    @NotBlank
    private final String authorId;

    public DeleteAuthorCommand(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
