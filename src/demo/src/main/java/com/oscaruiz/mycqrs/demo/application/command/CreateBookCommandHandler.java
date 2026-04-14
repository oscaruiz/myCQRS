package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;

@CommandHandlerComponent
public class CreateBookCommandHandler implements CommandHandler<CreateBookCommand> {

    private final BookRepository bookRepository;

    private final EventBus eventBus;

    public CreateBookCommandHandler(BookRepository bookRepository, EventBus eventBus) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    // Identity is known before persistence (client-generated UUID); no post-save rebinding needed.
    @Override
    public void handle(CreateBookCommand command) {

        BookAggregate aggregate = BookAggregate.create(command.getId(), command.getTitle(), command.getAuthor());
        bookRepository.save(aggregate);

        aggregate.pullDomainEvents().forEach(eventBus::publish);

    }
}
