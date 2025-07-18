package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;


public interface BookMongoRepository extends MongoRepository<BookReadModel, String> {
    BookReadModel findFirstByTitle(String title);
}

