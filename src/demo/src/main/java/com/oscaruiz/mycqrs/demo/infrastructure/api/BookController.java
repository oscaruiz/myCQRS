package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByTitleQuery;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/books")
public class BookController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public BookController(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        Book book = queryBus.handle(new FindBookByIdQuery(id.toString()));
        return ResponseEntity.ok(BookResponse.from(book));
    }

    @GetMapping
    public ResponseEntity<BookResponse> getBooksByTitle(@RequestParam String title) {
        BookResponse bookResponse = BookResponse.from(queryBus.handle(new FindBookByTitleQuery(title)));
        return ResponseEntity.ok(bookResponse );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> createBook(@PathVariable UUID id, @Valid @RequestBody CreateBookRequest request) {
        commandBus.send(request.toCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/books/" + id)
                .build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateBook(@PathVariable UUID id, @Valid @RequestBody UpdateBookRequest request) {
        commandBus.send(request.toCommand(id.toString()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        commandBus.send(new DeleteBookCommand(id.toString()));
        return ResponseEntity.noContent().build();
    }


}
