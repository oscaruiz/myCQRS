package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.model.Book;

import java.util.NoSuchElementException;

@QueryHandlerComponent
public class FindBookByTitleQueryHandler implements QueryHandler<FindBookByTitleQuery, Book> {

    private final BookReadModelRepository bookReadModelRepository;

    public FindBookByTitleQueryHandler(BookReadModelRepository bookReadModelRepository) {
        this.bookReadModelRepository = bookReadModelRepository;
    }

    @Override
    public Book handle(FindBookByTitleQuery query) {
        return bookReadModelRepository.findByTitle(query.getTitle())
                .orElseThrow(() -> new NoSuchElementException(
                        "Book with title " + query.getTitle() + " not found"));
    }
}
