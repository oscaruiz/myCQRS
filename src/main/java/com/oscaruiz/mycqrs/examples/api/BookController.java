package com.oscaruiz.mycqrs.api;

import com.oscaruiz.mycqrs.command.CommandBus;
import com.oscaruiz.mycqrs.examples.CreateBookCommand;
import com.oscaruiz.mycqrs.query.QueryBus;
import com.oscaruiz.mycqrs.examples.FindBookByTitleQuery;
import com.oscaruiz.mycqrs.examples.Book;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public BookController(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @PostMapping
    public void createBook(@RequestBody CreateBookCommand command) {
        commandBus.send(command);
    }

    @GetMapping("/{title}")
    public Book getBook(@PathVariable String title) {
        return queryBus.handle(new FindBookByTitleQuery(title));
    }
}
