package com.oscaruiz.mycqrs.demo.domain.repository;

import com.oscaruiz.mycqrs.demo.domain.model.Book;

import java.util.*;

public class BookReadRepository {

    private final Map<String, Book> books = new HashMap<>();

    public void save(Book book) {
        books.put(book.getTitle(), book);
    }

    public Optional<Book> findByTitle(String title) {
        return Optional.ofNullable(books.get(title));
    }

    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }
}
