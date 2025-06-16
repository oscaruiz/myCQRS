package com.oscaruiz.mycqrs.demo.domain.repository;

import com.oscaruiz.mycqrs.demo.domain.model.Book;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BookRepository {

    private final Map<String, Book> books = new ConcurrentHashMap<>();

    public void save(Book book) {
        books.put(book.getTitle(), book);
    }

    public Book findByTitle(String title) {
        return books.get(title);
    }

    public void clear() {
        books.clear();
    }
}
