package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface BookMongoRepository extends MongoRepository<BookReadModel, String> {
    List<BookReadModel> findByTitleContainingIgnoreCase(String title);
}
