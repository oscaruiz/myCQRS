package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.demo.book.application.query.BookReadModelRepository;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoBookReadModelRepository implements BookReadModelRepository {

    private final BookMongoRepository bookMongoRepository;

    public MongoBookReadModelRepository(BookMongoRepository bookMongoRepository) {
        this.bookMongoRepository = bookMongoRepository;
    }

    @Override
    public Optional<BookResponse> findById(String id) {
        return bookMongoRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Optional<BookResponse> findByTitle(String title) {
        return bookMongoRepository.findFirstByTitle(title).map(this::toResponse);
    }

    private BookResponse toResponse(BookReadModel model) {
        return new BookResponse(
                model.getAggregateId(),
                model.getTitle(),
                model.getAuthors() != null ? model.getAuthors() : List.of()
        );
    }
}
