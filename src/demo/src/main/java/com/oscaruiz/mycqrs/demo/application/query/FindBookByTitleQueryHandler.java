package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.domain.query.QueryHandler;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;

@QueryHandlerComponent
public class FindBookByTitleQueryHandler implements QueryHandler<FindBookByTitleQuery, Book> {

    private final BookRepository bookRepository;

    public FindBookByTitleQueryHandler(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Book handle(FindBookByTitleQuery query) {
        return bookRepository.findByTitle(query.getTitle());
    }
}
