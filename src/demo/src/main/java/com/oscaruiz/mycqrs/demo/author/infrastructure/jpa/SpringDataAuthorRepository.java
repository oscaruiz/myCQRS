package com.oscaruiz.mycqrs.demo.author.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAuthorRepository extends JpaRepository<AuthorEntity, UUID> {
}
