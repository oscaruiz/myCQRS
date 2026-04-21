package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

public enum BookOperation {
    CREATE_BOOK,
    UPDATE_BOOK,
    DELETE_BOOK,
    ADD_AUTHOR_TO_BOOK,
    REMOVE_AUTHOR_FROM_BOOK
}
