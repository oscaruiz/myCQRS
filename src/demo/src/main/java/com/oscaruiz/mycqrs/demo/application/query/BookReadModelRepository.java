package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.demo.domain.model.Book;

import java.util.Optional;

public interface BookReadModelRepository {
    Optional<Book> findById(String id);
    Optional<Book> findByTitle(String title);
}
