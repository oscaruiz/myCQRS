package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.event.EventHandler;

public class BookCreatedEventHandler implements EventHandler<BookCreatedEvent> {

    @Override
    public void on(BookCreatedEvent event) {
        System.out.printf("✅ Event received: Book '%s' by %s created.%n", event.getTitle(), event.getAuthor());
    }
}
