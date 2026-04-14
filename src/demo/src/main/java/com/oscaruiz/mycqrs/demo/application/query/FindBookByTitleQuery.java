package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.Query;
import com.oscaruiz.mycqrs.demo.domain.model.Book;

public class FindBookByTitleQuery implements Query<Book> {
    
    private final String title;

    public FindBookByTitleQuery(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
