package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;

import java.util.NoSuchElementException;

@QueryHandlerComponent
public class FindBookByIdQueryHandler implements QueryHandler<FindBookByIdQuery, BookResponse> {

    private final BookReadModelRepository bookReadModelRepository;

    public FindBookByIdQueryHandler(BookReadModelRepository bookReadModelRepository) {
        this.bookReadModelRepository = bookReadModelRepository;
    }

    @Override
    public BookResponse handle(FindBookByIdQuery query) {
        return bookReadModelRepository.findById(query.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Book with id " + query.getId() + " not found"));
    }
}
