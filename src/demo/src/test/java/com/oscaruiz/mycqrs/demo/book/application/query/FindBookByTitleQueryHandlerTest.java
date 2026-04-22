package com.oscaruiz.mycqrs.demo.book.application.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FindBookByTitleQueryHandlerTest {

    private InMemoryBookReadModelRepository repository;
    private FindBookByTitleQueryHandler handler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookReadModelRepository();
        handler = new FindBookByTitleQueryHandler(repository);

        repository.save(new BookResponse(null, "Clean Code",
                List.of(new AuthorSummary("11111111-1111-1111-1111-111111111111", "Robert C. Martin", false))));
    }

    @Test
    void shouldReturnBookByTitle() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Clean Code");
        List<BookResponse> result = handler.handle(query);

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).title());
        assertEquals(1, result.get(0).authors().size());
        assertEquals("Robert C. Martin", result.get(0).authors().get(0).fullName());
    }

    @Test
    void shouldReturnEmptyListWhenNoMatch() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Unknown Book");

        List<BookResponse> result = handler.handle(query);

        assertTrue(result.isEmpty());
    }

    private static class InMemoryBookReadModelRepository implements BookReadModelRepository {
        private final List<BookResponse> books = new ArrayList<>();

        void save(BookResponse book) {
            books.add(book);
        }

        @Override
        public Optional<BookResponse> findById(String id) {
            return books.stream()
                    .filter(b -> id.equals(b.id()))
                    .findFirst();
        }

        @Override
        public List<BookResponse> findByTitle(String title) {
            String needle = title.toLowerCase();
            return books.stream()
                    .filter(b -> b.title() != null && b.title().toLowerCase().contains(needle))
                    .toList();
        }
    }
}
