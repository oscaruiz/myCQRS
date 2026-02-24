package com.oscaruiz.mycqrs.demo.domain.repository;

import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;

import java.util.Optional;

public interface BookRepository {

    BookAggregate save(BookAggregate bookAggregate);

    Optional<BookAggregate> findByTitle(String title);
}
