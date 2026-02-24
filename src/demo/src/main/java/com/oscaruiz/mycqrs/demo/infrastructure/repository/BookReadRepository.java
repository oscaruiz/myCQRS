package com.oscaruiz.mycqrs.demo.infrastructure.repository;

import com.oscaruiz.mycqrs.demo.domain.model.Book;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
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

    public void clear() {
        books.clear();
    }
}
