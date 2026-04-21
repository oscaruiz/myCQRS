package com.oscaruiz.mycqrs.demo.author.infrastructure.api;

import com.oscaruiz.mycqrs.core.contracts.command.CommandBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.demo.author.application.command.DeleteAuthorCommand;
import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import com.oscaruiz.mycqrs.demo.author.application.query.FindAuthorByIdQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public AuthorController(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryBus.handle(new FindAuthorByIdQuery(id.toString())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> createAuthor(@PathVariable UUID id, @Valid @RequestBody CreateAuthorRequest request) {
        commandBus.send(request.toCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/authors/" + id)
                .build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> renameAuthor(@PathVariable UUID id, @Valid @RequestBody RenameAuthorRequest request) {
        commandBus.send(request.toCommand(id.toString()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID id) {
        commandBus.send(new DeleteAuthorCommand(id.toString()));
        return ResponseEntity.noContent().build();
    }
}
