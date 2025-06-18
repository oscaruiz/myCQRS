package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.demo.domain.model.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

@CommandHandlerComponent
public class CreateBookCommandHandler implements CommandHandler<CreateBookCommand, Void> {

    private final JpaRepository bookRepository;

    private final EventBus eventBus;

    public CreateBookCommandHandler(JpaRepository bookRepository, EventBus eventBus) {
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    @Override
    public Void handle(CreateBookCommand command) {

        BookEntity entity = new BookEntity(command.getTitle(), command.getAuthor());
        bookRepository.save(entity);

        eventBus.publish(new BookCreatedEvent(command.getTitle(), command.getAuthor()));

        return null;
    }
}
