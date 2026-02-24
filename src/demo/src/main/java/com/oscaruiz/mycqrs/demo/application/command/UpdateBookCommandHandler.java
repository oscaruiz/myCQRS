package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookUpdatedEvent;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.BookEntity;
import com.oscaruiz.mycqrs.demo.infrastructure.jpa.SpringDataBookRepository;

import java.util.NoSuchElementException;

@CommandHandlerComponent
public class UpdateBookCommandHandler implements CommandHandler<UpdateBookCommand, Void> {

    private final SpringDataBookRepository bookRepository;
    private final EventBus eventBus;

    public UpdateBookCommandHandler(SpringDataBookRepository bookRepository, EventBus eventBus) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    @Override
    public Void handle(UpdateBookCommand command) {
        Long id = parseAggregateId(command.getAggregateId());

        BookEntity entity = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book with id " + command.getAggregateId() + " was not found"));

        entity.update(command.getTitle(), command.getAuthor());
        BookEntity saved = bookRepository.save(entity);

        eventBus.publish(new BookUpdatedEvent(
                String.valueOf(saved.getId()),
                saved.getTitle(),
                saved.getAuthor()
        ));

        return null;
    }

    private Long parseAggregateId(String aggregateId) {
        try {
            return Long.parseLong(aggregateId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Aggregate id must be a numeric value: " + aggregateId, exception);
        }
    }
}
