package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.query.Query;

public class FindBookByTitleQuery implements Query<Book> {
    private final String title;

    public FindBookByTitleQuery(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
