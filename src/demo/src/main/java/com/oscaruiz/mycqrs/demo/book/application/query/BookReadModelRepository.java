package com.oscaruiz.mycqrs.demo.book.application.query;

import java.util.List;
import java.util.Optional;

public interface BookReadModelRepository {
    Optional<BookResponse> findById(String id);
    List<BookResponse> findByTitle(String title);
}
