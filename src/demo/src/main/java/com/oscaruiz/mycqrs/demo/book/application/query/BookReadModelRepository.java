package com.oscaruiz.mycqrs.demo.book.application.query;

import java.util.Optional;

public interface BookReadModelRepository {
    Optional<BookResponse> findById(String id);
    Optional<BookResponse> findByTitle(String title);
}
