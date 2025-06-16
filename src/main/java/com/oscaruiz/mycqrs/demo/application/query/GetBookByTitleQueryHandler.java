package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.domain.query.QueryHandler;
import com.oscaruiz.mycqrs.core.infrastructure.spring.QueryHandlerComponent;

@QueryHandlerComponent
public class GetBookByTitleQueryHandler implements QueryHandler<GetBookByTitleQuery, String> {

    @Override
    public String handle(GetBookByTitleQuery query) {
        return "Book found: " + query.getTitle(); // TODO
    }
}
