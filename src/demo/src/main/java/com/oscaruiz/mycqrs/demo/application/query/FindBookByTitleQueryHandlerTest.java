package com.oscaruiz.mycqrs.demo.application.query;

import com.oscaruiz.mycqrs.demo.domain.model.Book;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.testng.annotations.Test;

import static org.junit.jupiter.api.Assertions.*;

class FindBookByTitleQueryHandlerTest {

    private BookRepository bookRepository;
    private FindBookByTitleQueryHandler handler;

    @BeforeEach
    void setUp() {
        bookRepository = new BookRepository();
        handler = new FindBookByTitleQueryHandler(bookRepository);

        // Setup test data
        bookRepository.save(new Book("Clean Code", "Robert C. Martin"));
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
