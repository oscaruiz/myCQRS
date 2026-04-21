package com.oscaruiz.mycqrs.demo.author.application.query;

import java.util.List;

public record AuthorResponse(
        String id,
        String firstName,
        String lastName,
        Integer birthYear,
        boolean deleted,
        List<BookSummary> books
) {
    public record BookSummary(String bookId, String title) {}
}
