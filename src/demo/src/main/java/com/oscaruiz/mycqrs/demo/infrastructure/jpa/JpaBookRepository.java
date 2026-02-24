package com.oscaruiz.mycqrs.demo.infrastructure.jpa;

import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaBookRepository implements BookRepository {

    private final SpringDataBookRepository springDataBookRepository;

    public JpaBookRepository(SpringDataBookRepository springDataBookRepository) {
        this.springDataBookRepository = springDataBookRepository;
    }

    @Override
    public BookAggregate save(BookAggregate bookAggregate) {
        BookEntity entity;

        if (bookAggregate.getId() != null) {
            entity = springDataBookRepository.findById(bookAggregate.getId())
                    .orElseGet(() -> new BookEntity(bookAggregate.getTitle(), bookAggregate.getAuthor()));
            entity.update(bookAggregate.getTitle(), bookAggregate.getAuthor());
        } else {
            entity = new BookEntity(bookAggregate.getTitle(), bookAggregate.getAuthor());
        }

        BookEntity saved = springDataBookRepository.save(entity);
        bookAggregate.assignId(saved.getId());

        return bookAggregate;
    }

    @Override
    public Optional<BookAggregate> findByTitle(String title) {
        return springDataBookRepository.findByTitle(title).map(this::toAggregate);
    }

    private BookAggregate toAggregate(BookEntity entity) {
        return BookAggregate.rehydrate(entity.getId(), entity.getTitle(), entity.getAuthor());
    }
}
