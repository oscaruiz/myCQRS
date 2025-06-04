package com.oscaruiz.mycqrs.api;

import com.oscaruiz.mycqrs.command.CommandBus;
import com.oscaruiz.mycqrs.examples.CreateBookCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {

    private final CommandBus commandBus;

    public BookController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping
    public ResponseEntity<Void> createBook(@RequestBody CreateBookRequest request) {
        var command = new CreateBookCommand(request.getTitle(), request.getAuthor());
        commandBus.send(command);
        return ResponseEntity.ok().build();
    }
}
