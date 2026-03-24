package com.oscaruiz.mycqrs.demo.application.event;


import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;

@EventHandlerComponent
public class BookCreatedEventHandler implements EventHandler<BookCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(BookCreatedEventHandler.class);

    @Override
    public void handle(BookCreatedEvent event) {
        log.info("Event received: Book '{}' by {} created.", event.getTitle(), event.getAuthor());
    }
}

