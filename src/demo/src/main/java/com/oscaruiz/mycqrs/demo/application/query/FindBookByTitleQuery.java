package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.Query;

public class FindBookByTitleQuery implements Query<BookResponse> {

    private final String title;

    public FindBookByTitleQuery(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
