package com.oscaruiz.mycqrs.demo.book.application.command;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.oscaruiz.mycqrs.core.contracts.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.book.domain.model.BookAggregate;
import com.oscaruiz.mycqrs.demo.book.domain.repository.BookRepository;

/**
 * Removing an author reference does not call {@code AuthorExistenceChecker}:
 * cleaning up a reference to an author that no longer exists is legitimate
 * (e.g., dangling link after a rare data-fix). Only the write-side of Book
 * is affected.
 */
@CommandHandlerComponent
public class RemoveAuthorFromBookCommandHandler implements CommandHandler<RemoveAuthorFromBookCommand> {

    private final BookRepository bookRepository;
    private final EventBus eventBus;

    public RemoveAuthorFromBookCommandHandler(BookRepository bookRepository, EventBus eventBus) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(RemoveAuthorFromBookCommand command) {
        BookAggregate aggregate = bookRepository.load(command.getBookId());
        aggregate.removeAuthor(command.getAuthorId());
        bookRepository.save(aggregate);

        aggregate.pullDomainEvents().forEach(eventBus::publish);
    }
}
