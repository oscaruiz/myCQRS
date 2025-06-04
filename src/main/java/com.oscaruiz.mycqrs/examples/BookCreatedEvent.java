package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.event.Event;

public class BookCreatedEvent implements Event {
    private final String title;
    private final String author;

    public BookCreatedEvent(String title, String author) {
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
