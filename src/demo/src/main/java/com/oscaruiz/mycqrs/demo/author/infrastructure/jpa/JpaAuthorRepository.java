package com.oscaruiz.mycqrs.demo.author.infrastructure.jpa;

import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;
import com.oscaruiz.mycqrs.demo.author.domain.repository.AuthorRepository;
import org.springframework.stereotype.Repository;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuthorRepository implements AuthorRepository {

    private final SpringDataAuthorRepository springDataAuthorRepository;

    public JpaAuthorRepository(SpringDataAuthorRepository springDataAuthorRepository) {
        this.springDataAuthorRepository = springDataAuthorRepository;
    }

    @Override
    public void save(AuthorAggregate aggregate) {
        UUID id = UUID.fromString(aggregate.getId());
        AuthorEntity entity = springDataAuthorRepository.findById(id)
                .map(existing -> {
                    existing.update(aggregate.getFirstName(), aggregate.getLastName(),
                            aggregate.getBirthYear(), aggregate.isDeleted());
                    return existing;
                })
                .orElseGet(() -> new AuthorEntity(id, aggregate.getFirstName(), aggregate.getLastName(),
                        aggregate.getBirthYear(), aggregate.isDeleted()));
        springDataAuthorRepository.save(entity);
    }

    @Override
    public AuthorAggregate load(String id) {
        AuthorEntity entity = springDataAuthorRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new NoSuchElementException("Author with id " + id + " was not found"));
        return toAggregate(entity);
    }

    @Override
    public Optional<AuthorAggregate> findById(String id) {
        return springDataAuthorRepository.findById(UUID.fromString(id)).map(this::toAggregate);
    }

    private AuthorAggregate toAggregate(AuthorEntity entity) {
        return AuthorAggregate.rehydrate(
                entity.getId().toString(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getBirthYear(),
                entity.isDeleted()
        );
    }
}
