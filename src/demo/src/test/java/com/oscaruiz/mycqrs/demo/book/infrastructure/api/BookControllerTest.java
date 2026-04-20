package com.oscaruiz.mycqrs.demo.book.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demo.book.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

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
            UUID id = UUID.randomUUID();

            mockMvc.perform(put("/books/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/books/" + id));

            verify(commandBus, times(1)).send(any(CreateBookCommand.class));
        }

        @Test
        void createBook_withInvalidBody_returns400() throws Exception {
            CreateBookRequest invalid = new CreateBookRequest("", "");

            mockMvc.perform(put("/books/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(commandBus, never()).send(any());
        }

        @Test
        void createBook_withInvalidUuidInPath_returns400() throws Exception {
            CreateBookRequest request = new CreateBookRequest("Dune", "Frank Herbert");

            mockMvc.perform(put("/books/{id}", "not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(commandBus, never()).send(any());
        }
    }

    @Nested
    class UpdateBook {

        @Test
        void updateBook_whenOptimisticLockConflict_returns409() throws Exception {
            UpdateBookRequest request = new UpdateBookRequest("Dune", "Frank Herbert");
            UUID id = UUID.randomUUID();

            doThrow(new ObjectOptimisticLockingFailureException("BookEntity", id.toString()))
                    .when(commandBus).send(any(UpdateBookCommand.class));

            mockMvc.perform(patch("/books/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message")
                            .value("The resource was modified by another request. Please retry."));
        }
    }

    @Nested
    class GetBook {

        @Test
        void getBook_whenExists_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            BookResponse book = new BookResponse(id.toString(), "Dune", "Frank Herbert");
            when(queryBus.handle(any(FindBookByIdQuery.class))).thenReturn(book);

            mockMvc.perform(get("/books/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.title").value("Dune"))
                    .andExpect(jsonPath("$.author").value("Frank Herbert"));
        }

    }
}
