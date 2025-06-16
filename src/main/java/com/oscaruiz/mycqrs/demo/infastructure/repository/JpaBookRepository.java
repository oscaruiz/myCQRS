package com.oscaruiz.mycqrs.demo.infastructure.repository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBookRepository extends JpaRepository<BookEntity, String> {
}
