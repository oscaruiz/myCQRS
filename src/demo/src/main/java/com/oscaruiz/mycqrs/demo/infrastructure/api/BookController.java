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
    public ResponseEntity<BookResponse> getBookById(@PathVariable String id) {
        Book book = queryBus.handle(new FindBookByIdQuery(id));
        return ResponseEntity.ok(BookResponse.from(book));
    }

    @GetMapping
    public ResponseEntity<BookResponse> getBooksByTitle(@RequestParam String title) {
        BookResponse bookResponse = BookResponse.from(queryBus.handle(new FindBookByTitleQuery(title)));
        return ResponseEntity.ok(bookResponse );
    }

    @PostMapping
    public ResponseEntity<Void> createBook(@Valid @RequestBody CreateBookRequest request) {
        // TO-DO - Implement Location header with UUID logic
        commandBus.send(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateBook(@PathVariable Long id, @RequestBody UpdateBookRequest request) {
        commandBus.send(request.toCommand(id));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        commandBus.send(new DeleteBookCommand(id));
        return ResponseEntity.noContent().build();
    }


}
