package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import jakarta.validation.constraints.NotBlank;

public class DeleteBookCommand implements Command {

    @NotBlank(message = "Aggregate id is required")
    private final String aggregateId;

    public DeleteBookCommand(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateId() {
        return aggregateId;
    }
}
