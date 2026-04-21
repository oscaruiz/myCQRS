package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;
import com.oscaruiz.mycqrs.demo.book.domain.service.AuthorExistenceChecker;

@CommandHandlerComponent
public class AddAuthorToBookCommandHandler implements CommandHandler<AddAuthorToBookCommand> {

    private final BookRepository bookRepository;
    private final EventBus eventBus;
    private final AuthorExistenceChecker authorExistenceChecker;

    public AddAuthorToBookCommandHandler(BookRepository bookRepository,
                                         EventBus eventBus,
                                         AuthorExistenceChecker authorExistenceChecker) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
        this.authorExistenceChecker = authorExistenceChecker;
    }

    @Override
    public void handle(AddAuthorToBookCommand command) {
        authorExistenceChecker.ensureExistsAndActive(command.getAuthorId());

        BookAggregate aggregate = bookRepository.load(command.getBookId());
        aggregate.addAuthor(command.getAuthorId());
        bookRepository.save(aggregate);

        aggregate.pullDomainEvents().forEach(eventBus::publish);
    }
}
