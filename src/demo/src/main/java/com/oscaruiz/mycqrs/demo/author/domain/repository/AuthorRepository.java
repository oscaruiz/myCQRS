package com.oscaruiz.mycqrs.demo.author.domain.repository;

import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;

import java.util.Optional;

public interface AuthorRepository {

    void save(AuthorAggregate aggregate);

    AuthorAggregate load(String id);

    Optional<AuthorAggregate> findById(String id);
}
