package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.query.QueryHandler;
import com.oscaruiz.mycqrs.spring.QueryHandlerComponent;

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
