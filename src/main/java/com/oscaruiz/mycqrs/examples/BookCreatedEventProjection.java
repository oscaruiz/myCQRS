package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.event.EventHandler;
import com.oscaruiz.mycqrs.spring.EventHandlerComponent;

@EventHandlerComponent
public class BookCreatedEventProjection implements EventHandler<BookCreatedEvent> {

    private final BookRepository bookRepository;

    public BookCreatedEventProjection(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void handle(BookCreatedEvent event) {
        Book book = new Book(event.getTitle(), event.getAuthor());
        bookRepository.save(book);
        System.out.printf("📥 Proyección: libro '%s' guardado en BookRepository%n", event.getTitle());
    }
}
