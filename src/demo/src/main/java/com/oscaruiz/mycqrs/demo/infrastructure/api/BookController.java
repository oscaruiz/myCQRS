package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.DeleteBookCommand;
import com.oscaruiz.mycqrs.demo.application.command.UpdateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByTitleQuery;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public BookController(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    /*
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        Book book = queryBus.handle(new FindBookByIdQuery(id));
        return ResponseEntity.ok(book);
    }*/

    @GetMapping
    public ResponseEntity<BookResponse> getBooksByTitle(@RequestParam String title) {
        BookResponse bookResponse = BookResponse.from(queryBus.handle(new FindBookByTitleQuery(title)));
        return ResponseEntity.ok(bookResponse );
    }

    @PostMapping
    public ResponseEntity<Void> createBook(@Valid @RequestBody CreateBookRequest request) {

        String bookId = commandBus.send(request.toCommand());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(bookId)
                .toUri();

        return ResponseEntity.created(location).build();
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
