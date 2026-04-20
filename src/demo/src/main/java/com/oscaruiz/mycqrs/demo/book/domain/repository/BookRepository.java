package com.oscaruiz.mycqrs.demo.book.domain.repository;

import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;

import java.util.Optional;

public interface BookRepository {

    void save(BookAggregate bookAggregate);

    BookAggregate load(String id);

    Optional<BookAggregate> findByTitle(String title);
}
