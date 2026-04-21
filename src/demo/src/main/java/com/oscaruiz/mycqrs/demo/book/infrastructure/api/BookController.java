package com.oscaruiz.mycqrs.demo.book.infrastructure.api;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demo.book.application.command.AddAuthorToBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.command.RemoveAuthorFromBookCommand;
import com.oscaruiz.mycqrs.demo.book.application.query.BookResponse;
import com.oscaruiz.mycqrs.demo.book.application.query.FindBookByIdQuery;
import com.oscaruiz.mycqrs.demo.book.application.query.FindBookByTitleQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
        return ResponseEntity.ok(queryBus.handle(new FindBookByIdQuery(id.toString())));
    }

    @GetMapping
    public ResponseEntity<BookResponse> getBooksByTitle(@RequestParam String title) {
        return ResponseEntity.ok(queryBus.handle(new FindBookByTitleQuery(title)));
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

    @PostMapping("/{id}/authors/{authorId}")
    public ResponseEntity<Void> addAuthorToBook(@PathVariable UUID id, @PathVariable UUID authorId) {
        commandBus.send(new AddAuthorToBookCommand(id.toString(), authorId.toString()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/authors/{authorId}")
    public ResponseEntity<Void> removeAuthorFromBook(@PathVariable UUID id, @PathVariable UUID authorId) {
        commandBus.send(new RemoveAuthorFromBookCommand(id.toString(), authorId.toString()));
        return ResponseEntity.noContent().build();
    }
}
