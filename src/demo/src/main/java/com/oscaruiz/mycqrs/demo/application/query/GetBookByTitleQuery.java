package com.oscaruiz.mycqrs.demo.application.query;


import com.oscaruiz.mycqrs.core.domain.query.Query;

public class GetBookByTitleQuery implements Query<String> {
    private final String title;

    public GetBookByTitleQuery(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
