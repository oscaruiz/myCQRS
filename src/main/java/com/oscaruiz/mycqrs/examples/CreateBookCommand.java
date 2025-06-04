package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.command.Command;

public class CreateBookCommand implements Command {
    private final String title;
    private final String author;

    public CreateBookCommand(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
