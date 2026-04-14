package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.Query;
import com.oscaruiz.mycqrs.demo.domain.model.Book;

public class FindBookByIdQuery implements Query<Book> {

    private final String id;

    public FindBookByIdQuery(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
