package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.demo.domain.model.Book;

public record BookResponse(
        String title,
        String author
) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getTitle(), book.getAuthor());
    }
}
