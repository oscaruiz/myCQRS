package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.oscaruiz.mycqrs.core.domain.command.CommandBus;
import com.oscaruiz.mycqrs.core.domain.query.QueryBus;
import com.oscaruiz.mycqrs.demo.application.command.CreateBookCommand;
import com.oscaruiz.mycqrs.demo.application.query.FindBookByTitleQuery;
import com.oscaruiz.mycqrs.demo.domain.model.Book;
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
