package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.infrastructure.repository.BookReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FindBookByTitleQueryHandlerTest {

    private BookReadRepository bookReadRepository;
    private FindBookByTitleQueryHandler handler;

    @BeforeEach
    void setUp() {
        bookReadRepository = new BookReadRepository();
        handler = new FindBookByTitleQueryHandler(bookReadRepository);

        bookReadRepository.save(new Book(null, "Clean Code", "Robert C. Martin"));
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
    void shouldReturnNullIfBookNotFound() {
        FindBookByTitleQuery query = new FindBookByTitleQuery("Unknown Book");
        Book result = handler.handle(query);

        assertNull(result);
    }
}
