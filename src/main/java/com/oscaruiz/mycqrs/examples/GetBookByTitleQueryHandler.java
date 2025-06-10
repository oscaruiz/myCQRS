package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.query.QueryHandler;
import com.oscaruiz.mycqrs.spring.QueryHandlerComponent;

@QueryHandlerComponent
public class GetBookByTitleQueryHandler implements QueryHandler<GetBookByTitleQuery, String> {

    @Override
    public String handle(GetBookByTitleQuery query) {
        return "Book found: " + query.getTitle(); // TODO
    }
}
