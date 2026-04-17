package com.oscaruiz.mycqrs.demo.application.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FindBookByTitleQueryHandlerTest {

    private InMemoryBookReadModelRepository repository;
    private FindBookByTitleQueryHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookReadModelRepository();
        handler = new FindBookByTitleQueryHandler(repository);

        repository.save(new BookResponse(null, "Clean Code", "Robert C. Martin"));
    }

    @Test
    void shouldReturnBookByTitle() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Clean Code");
        BookResponse result = handler.handle(query);

        assertNotNull(result);
        assertEquals("Clean Code", result.title());
        assertEquals("Robert C. Martin", result.author());
    }

    @Test
    void shouldThrowWhenBookNotFound() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Unknown Book");

        assertThrows(NoSuchElementException.class, () -> handler.handle(query));
    }

    private static class InMemoryBookReadModelRepository implements BookReadModelRepository {
        private final Map<String, BookResponse> byTitle = new HashMap<>();

        void save(BookResponse book) {
            byTitle.put(book.title(), book);
        }

        @Override
        public Optional<BookResponse> findById(String id) {
            return byTitle.values().stream()
                    .filter(b -> id.equals(b.id()))
                    .findFirst();
        }

        @Override
        public Optional<BookResponse> findByTitle(String title) {
            return Optional.ofNullable(byTitle.get(title));
        }
    }
}
