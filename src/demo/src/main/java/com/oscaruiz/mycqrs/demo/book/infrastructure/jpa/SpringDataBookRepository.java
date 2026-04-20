package com.oscaruiz.mycqrs.demo.book.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataBookRepository extends JpaRepository<BookEntity, UUID> {
    Optional<BookEntity> findByTitle(String title);
}
