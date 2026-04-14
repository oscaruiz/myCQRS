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
        BookEntity entity;
        // TODO Day 2: BookEntity PK becomes UUID; this parse-to-Long bridge disappears.
        Long pk = tryParsePk(bookAggregate.getId());

        if (pk == null) {
            entity = new BookEntity(bookAggregate.getTitle(), bookAggregate.getAuthor(), bookAggregate.isDeleted());
        } else {
            entity = springDataBookRepository.findById(pk)
                    .orElseThrow(() -> new NoSuchElementException("Book with id " + pk + " was not found"));
            entity.update(bookAggregate.getTitle(), bookAggregate.getAuthor(), bookAggregate.isDeleted());
        }

        springDataBookRepository.save(entity);

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
        // TODO Day 2: entity PK is UUID String; no stringification needed.
        return BookAggregate.rehydrate(String.valueOf(entity.getId()), entity.getTitle(), entity.getAuthor(), entity.isDeleted());
    }

    // TODO Day 2: remove — aggregate id and entity PK will both be UUID Strings.
    private Long tryParsePk(String aggregateId) {
        try {
            return Long.valueOf(aggregateId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
