package com.oscaruiz.mycqrs.demo.author.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.author.domain.model.AuthorAggregate;
import com.oscaruiz.mycqrs.demo.author.domain.repository.AuthorRepository;

@CommandHandlerComponent
public class CreateAuthorCommandHandler implements CommandHandler<CreateAuthorCommand> {

    private final AuthorRepository authorRepository;
    private final EventBus eventBus;

    public CreateAuthorCommandHandler(AuthorRepository authorRepository, EventBus eventBus) {
        this.authorRepository = authorRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(CreateAuthorCommand command) {
        AuthorAggregate aggregate = AuthorAggregate.create(
                command.getId(),
                command.getFirstName(),
                command.getLastName(),
                command.getBirthYear()
        );
        authorRepository.save(aggregate);

        aggregate.pullDomainEvents().forEach(eventBus::publish);
    }
}
