package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.query.Query;

public class GetBookByTitleQuery implements Query<String> {
    private final String title;

    public GetBookByTitleQuery(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
