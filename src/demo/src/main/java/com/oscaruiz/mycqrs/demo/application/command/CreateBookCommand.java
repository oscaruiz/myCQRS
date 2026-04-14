package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.Command;

public class CreateBookCommand implements Command {

    private final String id;

    private final String title;

    private final String author;

    public CreateBookCommand(String id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
