package com.oscaruiz.mycqrs.demo.book.domain.service;

public class AuthorNotFoundException extends RuntimeException {

    public AuthorNotFoundException(String authorId) {
        super("Author with id " + authorId + " was not found");
    }
}
