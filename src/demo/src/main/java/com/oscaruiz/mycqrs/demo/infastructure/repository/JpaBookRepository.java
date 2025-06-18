package com.oscaruiz.mycqrs.demo.infastructure.repository;
import com.oscaruiz.mycqrs.demo.domain.model.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBookRepository extends JpaRepository<BookEntity, String> {
}
