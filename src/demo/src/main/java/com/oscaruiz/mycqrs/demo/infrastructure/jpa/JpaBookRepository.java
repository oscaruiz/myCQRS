package com.oscaruiz.mycqrs.demo.infrastructure.jpa;

import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import org.springframework.stereotype.Repository;

import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class JpaBookRepository implements BookRepository {

    private final SpringDataBookRepository springDataBookRepository;

    public JpaBookRepository(SpringDataBookRepository springDataBookRepository) {
        this.springDataBookRepository = springDataBookRepository;
    }

    @Override
    public BookAggregate save(BookAggregate bookAggregate) {
        // Infrastructure maps and persists only. Domain lifecycle rules live in the aggregate.
        BookEntity entity;

        if (bookAggregate.getId() == null) {
            entity = new BookEntity(bookAggregate.getTitle(), bookAggregate.getAuthor(), bookAggregate.isDeleted());
        } else {
            entity = springDataBookRepository.findById(bookAggregate.getId())
                    .orElseThrow(() -> new NoSuchElementException("Book with id " + bookAggregate.getId() + " was not found"));
            entity.update(bookAggregate.getTitle(), bookAggregate.getAuthor(), bookAggregate.isDeleted());
        }

        BookEntity saved = springDataBookRepository.save(entity);

        if (bookAggregate.getId() == null) {
            bookAggregate.assignId(saved.getId());
        }

        return bookAggregate;
    }

    @Override
    public BookAggregate load(Long id) {
        BookEntity entity = springDataBookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book with id " + id + " was not found"));
        return toAggregate(entity);
    }

    @Override
    public Optional<BookAggregate> findByTitle(String title) {
        return springDataBookRepository.findByTitle(title).map(this::toAggregate);
    }

    private BookAggregate toAggregate(BookEntity entity) {
        return BookAggregate.rehydrate(entity.getId(), entity.getTitle(), entity.getAuthor(), entity.isDeleted());
    }
}
