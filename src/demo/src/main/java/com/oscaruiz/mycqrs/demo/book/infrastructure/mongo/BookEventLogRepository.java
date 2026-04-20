package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookEventLogRepository extends MongoRepository<BookEventLog, String> {
}
