package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.demo.domain.model.Book;
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

        repository.save(new Book(null, "Clean Code", "Robert C. Martin"));
    }

    @Test
    void shouldReturnBookByTitle() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Clean Code");
        Book result = handler.handle(query);

        assertNotNull(result);
        assertEquals("Clean Code", result.getTitle());
        assertEquals("Robert C. Martin", result.getAuthor());
    }

    @Test
    void shouldThrowWhenBookNotFound() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Unknown Book");

        assertThrows(NoSuchElementException.class, () -> handler.handle(query));
    }

    private static class InMemoryBookReadModelRepository implements BookReadModelRepository {
        private final Map<String, Book> byTitle = new HashMap<>();

        void save(Book book) {
            byTitle.put(book.getTitle(), book);
        }

        @Override
        public Optional<Book> findById(String id) {
            return byTitle.values().stream()
                    .filter(b -> id.equals(b.getId()))
                    .findFirst();
        }

        @Override
        public Optional<Book> findByTitle(String title) {
            return Optional.ofNullable(byTitle.get(title));
        }
    }
}
