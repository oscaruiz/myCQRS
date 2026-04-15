package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.oscaruiz.mycqrs.demo.application.query.BookReadModelRepository;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoBookReadModelRepository implements BookReadModelRepository {

    private final BookMongoRepository bookMongoRepository;

    public MongoBookReadModelRepository(BookMongoRepository bookMongoRepository) {
        this.bookMongoRepository = bookMongoRepository;
    }

    @Override
    public Optional<Book> findById(String id) {
        return bookMongoRepository.findById(id).map(this::toBook);
    }

    @Override
    public Optional<Book> findByTitle(String title) {
        return bookMongoRepository.findFirstByTitle(title).map(this::toBook);
    }

    private Book toBook(BookReadModel model) {
        return new Book(model.getAggregateId(), model.getTitle(), model.getAuthor());
    }
}
