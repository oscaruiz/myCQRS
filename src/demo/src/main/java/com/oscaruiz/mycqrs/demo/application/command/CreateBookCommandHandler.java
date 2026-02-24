package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.domain.repository.BookRepository;

@CommandHandlerComponent
public class CreateBookCommandHandler implements CommandHandler<CreateBookCommand, Void> {

    private final BookRepository bookRepository;

    private final EventBus eventBus;

    public CreateBookCommandHandler(BookRepository bookRepository, EventBus eventBus) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    @Override
    public Void handle(CreateBookCommand command) {

        BookAggregate aggregate = BookAggregate.create(command.getTitle(), command.getAuthor());
        BookAggregate saved = bookRepository.save(aggregate);

        saved.pullDomainEvents().forEach(eventBus::publish);

        return null;
    }
}
