package com.oscaruiz.mycqrs.demo.book.infrastructure.jpa;

import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import org.springframework.stereotype.Repository;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaBookRepository implements BookRepository {

    private final SpringDataBookRepository springDataBookRepository;

    public JpaBookRepository(SpringDataBookRepository springDataBookRepository) {
        this.springDataBookRepository = springDataBookRepository;
    }

    @Override
    public void save(BookAggregate bookAggregate) {
        UUID id = UUID.fromString(bookAggregate.getId());
        Set<UUID> authorIds = bookAggregate.getAuthorIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        BookEntity entity = springDataBookRepository.findById(id)
                .map(existing -> {
                    existing.update(bookAggregate.getTitle(), bookAggregate.isDeleted());
                    existing.replaceAuthorIds(authorIds);
                    return existing;
                })
                .orElseGet(() -> {
                    BookEntity created = new BookEntity(id, bookAggregate.getTitle(), bookAggregate.isDeleted());
                    created.replaceAuthorIds(authorIds);
                    return created;
                });
        springDataBookRepository.save(entity);
    }

    @Override
    public BookAggregate load(String id) {
        BookEntity entity = springDataBookRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new NoSuchElementException("Book with id " + id + " was not found"));
        return toAggregate(entity);
    }

    @Override
    public Optional<BookAggregate> findByTitle(String title) {
        return springDataBookRepository.findByTitle(title).map(this::toAggregate);
    }

    private BookAggregate toAggregate(BookEntity entity) {
        Set<String> authorIds = entity.getAuthorIds().stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());
        return BookAggregate.rehydrate(
                entity.getId().toString(),
                entity.getTitle(),
                entity.isDeleted(),
                authorIds
        );
    }
}
