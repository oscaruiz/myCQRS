package com.oscaruiz.mycqrs.demo.book.domain.service;

/**
 * Domain service (port) used by Book command handlers before wiring an author
 * reference into a book. Lives in the Book domain because it expresses a Book
 * invariant ("references to authors must be live") — but is implemented in
 * infrastructure by consulting the Author aggregate's repository.
 */
public interface AuthorExistenceChecker {

    /**
     * @throws AuthorNotFoundException if no author with the given id exists.
     * @throws AuthorRetiredException  if the author exists but is soft-deleted.
     */
    void ensureExistsAndActive(String authorId);
}
