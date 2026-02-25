package com.oscaruiz.mycqrs.demo.application.event;

import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.repository.BookReadRepository;

@EventHandlerComponent
public class BookCreatedEventProjection implements EventHandler<BookCreatedEvent> {

    private final BookReadRepository bookReadRepository;

    public BookCreatedEventProjection(BookReadRepository bookReadRepository) {
        this.bookReadRepository = bookReadRepository;
    }

    @Override
    public void handle(BookCreatedEvent event) {
        Book book = new Book(event.getTitle(), event.getAuthor());
        bookReadRepository.save(book);
        System.out.printf("📥 Proyección: libro '%s' guardado en BookReadRepository%n", event.getTitle());
    }
}
