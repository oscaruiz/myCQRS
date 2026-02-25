package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.domain.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.repository.BookReadRepository;

@QueryHandlerComponent
public class FindBookByTitleQueryHandler implements QueryHandler<FindBookByTitleQuery, Book> {

    private final BookReadRepository bookReadRepository;

    public FindBookByTitleQueryHandler(BookReadRepository bookReadRepository) {
        this.bookReadRepository = bookReadRepository;
    }

    @Override
    public Book handle(FindBookByTitleQuery query) {
        return bookReadRepository.findByTitle(query.getTitle()).orElse(null);
    }
}
