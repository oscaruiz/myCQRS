package com.oscaruiz.mycqrs.demo.book.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.Query;

public class FindBookByIdQuery implements Query<BookResponse> {

    private final String id;

    public FindBookByIdQuery(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
