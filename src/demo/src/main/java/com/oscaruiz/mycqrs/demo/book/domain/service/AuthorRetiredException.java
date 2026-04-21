package com.oscaruiz.mycqrs.demo.book.domain.service;

public class AuthorRetiredException extends RuntimeException {

    public AuthorRetiredException(String authorId) {
        super("Author with id " + authorId + " is retired (soft-deleted) and cannot be referenced by a book");
    }
}
