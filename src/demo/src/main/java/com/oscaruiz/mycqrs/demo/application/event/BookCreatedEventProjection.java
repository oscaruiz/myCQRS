package com.oscaruiz.mycqrs.demo.application.event;

import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;

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
