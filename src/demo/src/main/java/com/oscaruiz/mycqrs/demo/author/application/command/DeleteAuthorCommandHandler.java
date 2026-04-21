package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;
import com.oscaruiz.mycqrs.demo.author.domain.repository.AuthorRepository;

@CommandHandlerComponent
public class DeleteAuthorCommandHandler implements CommandHandler<DeleteAuthorCommand> {

    private final AuthorRepository authorRepository;
    private final EventBus eventBus;

    public DeleteAuthorCommandHandler(AuthorRepository authorRepository, EventBus eventBus) {
        this.authorRepository = authorRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(DeleteAuthorCommand command) {
        AuthorAggregate aggregate = authorRepository.load(command.getAuthorId());
        aggregate.delete();
        authorRepository.save(aggregate);

        aggregate.pullDomainEvents().forEach(eventBus::publish);
    }
}
