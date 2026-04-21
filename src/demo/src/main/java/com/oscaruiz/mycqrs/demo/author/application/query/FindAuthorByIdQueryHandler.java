package com.oscaruiz.mycqrs.demo.author.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;

import java.util.NoSuchElementException;

@QueryHandlerComponent
public class FindAuthorByIdQueryHandler implements QueryHandler<FindAuthorByIdQuery, AuthorResponse> {

    private final AuthorReadModelRepository authorReadModelRepository;

    public FindAuthorByIdQueryHandler(AuthorReadModelRepository authorReadModelRepository) {
        this.authorReadModelRepository = authorReadModelRepository;
    }

    @Override
    public AuthorResponse handle(FindAuthorByIdQuery query) {
        return authorReadModelRepository.findById(query.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Author with id " + query.getId() + " not found"));
    }
}
