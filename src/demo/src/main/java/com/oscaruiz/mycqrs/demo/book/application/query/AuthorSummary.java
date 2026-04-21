package com.oscaruiz.mycqrs.demo.book.application.query;

/**
 * Denormalized snapshot of an Author as embedded inside a Book's read model.
 * Lives in the query package so both the API response shape and the Mongo
 * document structure can reuse it without duplication.
 *
 * {@code retired} mirrors the Author's soft-delete flag; it is kept in each
 * denormalization so a client fetching a Book can tell that the author is no
 * longer active without a second query.
 */
public record AuthorSummary(String authorId, String fullName, boolean retired) {}
