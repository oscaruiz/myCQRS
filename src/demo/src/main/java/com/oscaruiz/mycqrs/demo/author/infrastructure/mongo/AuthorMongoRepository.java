package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthorMongoRepository extends MongoRepository<AuthorReadModel, String> {
}
