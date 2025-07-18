package com.oscaruiz.mycqrs.demo.infrastructure.jpa;
import com.oscaruiz.mycqrs.demo.domain.model.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBookRepository extends JpaRepository<BookEntity, String> {
}
