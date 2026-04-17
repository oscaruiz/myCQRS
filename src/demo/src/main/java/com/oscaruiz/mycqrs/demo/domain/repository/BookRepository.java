package com.oscaruiz.mycqrs.demo.domain.repository;

import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;

import java.util.Optional;

public interface BookRepository {

    void save(BookAggregate bookAggregate);

    BookAggregate load(String id);

    Optional<BookAggregate> findByTitle(String title);
}
