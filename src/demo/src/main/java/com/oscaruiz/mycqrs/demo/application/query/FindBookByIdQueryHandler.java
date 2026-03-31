package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.domain.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.repository.BookReadRepository;

@QueryHandlerComponent
public class FindBookByIdQueryHandler implements QueryHandler<FindBookByIdQuery, Book> {

    private final BookReadRepository bookReadRepository;

    public FindBookByIdQueryHandler(BookReadRepository bookReadRepository) {
        this.bookReadRepository = bookReadRepository;
    }

    @Override
    public Book handle(FindBookByIdQuery query) {
        // TODO
        return null;
        // return bookReadRepository.findById(query.getId()).orElse(null);
    }
}
