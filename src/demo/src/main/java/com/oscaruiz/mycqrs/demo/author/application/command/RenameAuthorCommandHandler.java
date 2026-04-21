package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;
import com.oscaruiz.mycqrs.demo.author.domain.repository.AuthorRepository;

@CommandHandlerComponent
public class RenameAuthorCommandHandler implements CommandHandler<RenameAuthorCommand> {

    private final AuthorRepository authorRepository;
    private final EventBus eventBus;

    public RenameAuthorCommandHandler(AuthorRepository authorRepository, EventBus eventBus) {
        this.authorRepository = authorRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(RenameAuthorCommand command) {
        AuthorAggregate aggregate = authorRepository.load(command.getAuthorId());
        aggregate.rename(command.getFirstName(), command.getLastName());
        authorRepository.save(aggregate);

        aggregate.pullDomainEvents().forEach(eventBus::publish);
    }
}
