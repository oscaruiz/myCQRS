package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookEventLogRepository extends MongoRepository<BookEventLog, String> {
}
