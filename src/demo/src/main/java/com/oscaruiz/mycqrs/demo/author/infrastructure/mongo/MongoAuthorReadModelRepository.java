package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.oscaruiz.mycqrs.demo.author.application.query.AuthorReadModelRepository;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoAuthorReadModelRepository implements AuthorReadModelRepository {

    private final AuthorMongoRepository authorMongoRepository;

    public MongoAuthorReadModelRepository(AuthorMongoRepository authorMongoRepository) {
        this.authorMongoRepository = authorMongoRepository;
    }

    @Override
    public Optional<AuthorResponse> findById(String id) {
        return authorMongoRepository.findById(id).map(this::toResponse);
    }

    private AuthorResponse toResponse(AuthorReadModel model) {
        return new AuthorResponse(
                model.getId(),
                model.getFirstName(),
                model.getLastName(),
                model.getBirthYear(),
                model.isDeleted(),
                model.getBooks() != null ? model.getBooks() : List.of()
        );
    }
}
