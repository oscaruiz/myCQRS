package com.oscaruiz.mycqrs.demo.author.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.Query;

public class FindAuthorByIdQuery implements Query<AuthorResponse> {

    private final String id;

    public FindAuthorByIdQuery(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
