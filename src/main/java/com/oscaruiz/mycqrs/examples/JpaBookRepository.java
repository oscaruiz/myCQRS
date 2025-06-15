package com.oscaruiz.mycqrs.examples;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBookRepository extends JpaRepository<BookEntity, String> {
}
