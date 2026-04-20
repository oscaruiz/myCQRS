package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface BookMongoRepository extends MongoRepository<BookReadModel, String> {
    Optional<BookReadModel> findFirstByTitle(String title);
}
