package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@ActiveProfiles("test")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommandBus commandBus;

    @MockBean
    private QueryBus queryBus;

    @Nested
    class CreateBook {

        @Test
        void createBook_withValidBody_returns201() throws Exception {
            CreateBookRequest request = new CreateBookRequest("Dune", "Frank Herbert");

            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(commandBus, times(1)).send(any(CreateBookCommand.class));
        }

        @Test
        void createBook_withInvalidBody_returns400() throws Exception {
            CreateBookRequest invalid = new CreateBookRequest("", "");

            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(commandBus, never()).send(any());
        }
    }

    @Nested
    class GetBook {

        @Test
        void getBook_whenExists_returns200() throws Exception {
            Book book = new Book("42", "Dune", "Frank Herbert");
            when(queryBus.handle(any(FindBookByIdQuery.class))).thenReturn(book);

            mockMvc.perform(get("/books/{id}", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("42"))
                    .andExpect(jsonPath("$.title").value("Dune"))
                    .andExpect(jsonPath("$.author").value("Frank Herbert"));
        }

        @Test
        void getBook_whenNotFound_returns404() throws Exception {
            when(queryBus.handle(any(FindBookByIdQuery.class)))
                    .thenThrow(new NoSuchElementException("Book not found"));

            mockMvc.perform(get("/books/{id}", "999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteBook {

        @Test
        void deleteBook_whenNotFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Book not found"))
                    .when(commandBus).send(any(DeleteBookCommand.class));

            mockMvc.perform(delete("/books/{id}", 999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteThenGet_returns404() throws Exception {
            doNothing().when(commandBus).send(any(DeleteBookCommand.class));

            mockMvc.perform(delete("/books/{id}", 1L))
                    .andExpect(status().isNoContent());

            when(queryBus.handle(any(FindBookByIdQuery.class)))
                    .thenThrow(new NoSuchElementException("Book not found"));

            mockMvc.perform(get("/books/{id}", "1"))
                    .andExpect(status().isNotFound());
        }
    }
}
