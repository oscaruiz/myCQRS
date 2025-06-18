package com.oscaruiz.mycqrs.demo.application.event;


import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;

@EventHandlerComponent
public class BookCreatedEventHandler implements EventHandler<BookCreatedEvent> {

    @Override
    public void handle(BookCreatedEvent event) {
        System.out.printf("✅ Event received: Book '%s' by %s created.%n", event.getTitle(), event.getAuthor());
    }
}

