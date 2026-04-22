package com.oscaruiz.mycqrs.demo.book.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;

import java.util.List;

@QueryHandlerComponent
public class FindBookByTitleQueryHandler implements QueryHandler<FindBookByTitleQuery, List<BookResponse>> {

    private final BookReadModelRepository bookReadModelRepository;

    public FindBookByTitleQueryHandler(BookReadModelRepository bookReadModelRepository) {
        this.bookReadModelRepository = bookReadModelRepository;
    }

    @Override
    public List<BookResponse> handle(FindBookByTitleQuery query) {
        return bookReadModelRepository.findByTitle(query.getTitle());
    }
}
