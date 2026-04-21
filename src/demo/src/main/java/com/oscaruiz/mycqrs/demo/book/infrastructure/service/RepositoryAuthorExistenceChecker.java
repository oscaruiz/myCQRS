package com.oscaruiz.mycqrs.demo.book.infrastructure.service;

import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;
import com.oscaruiz.mycqrs.demo.author.domain.repository.AuthorRepository;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorExistenceChecker;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorNotFoundException;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorRetiredException;
import org.springframework.stereotype.Component;

/**
 * Book-side adapter of AuthorExistenceChecker. Crosses into the Author
 * aggregate via its domain port (AuthorRepository), not via any Author
 * infrastructure. ArchUnit permits this narrow edge (see Phase 8).
 */
@Component
public class RepositoryAuthorExistenceChecker implements AuthorExistenceChecker {

    private final AuthorRepository authorRepository;

    public RepositoryAuthorExistenceChecker(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Override
    public void ensureExistsAndActive(String authorId) {
        AuthorAggregate author = authorRepository.findById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException(authorId));
        if (author.isDeleted()) {
            throw new AuthorRetiredException(authorId);
        }
    }
}
