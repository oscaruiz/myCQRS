package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;
import jakarta.validation.constraints.NotBlank;

public class UpdateBookCommand implements Command {

    @NotBlank(message = "Aggregate id is required")
    private final String aggregateId;

    @NotBlank(message = "Title is required")
    private final String title;

    @NotBlank(message = "Author is required")
    private final String author;

    public UpdateBookCommand(String aggregateId, String title, String author) {
        this.aggregateId = aggregateId;
        this.title = title;
        this.author = author;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
