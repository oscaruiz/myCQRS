package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;

@CommandHandlerComponent
public class UpdateBookCommandHandler implements CommandHandler<UpdateBookCommand> {

    private final BookRepository bookRepository;
    private final EventBus eventBus;

    public UpdateBookCommandHandler(BookRepository bookRepository, EventBus eventBus) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(UpdateBookCommand command) {
        // Application layer orchestrates use-cases through domain ports.
        BookAggregate aggregate = bookRepository.load(command.getBookId());
        aggregate.update(command.getTitle(), command.getAuthor());
        BookAggregate saved = bookRepository.save(aggregate);

        saved.pullDomainEvents().forEach(eventBus::publish);

    }
}
