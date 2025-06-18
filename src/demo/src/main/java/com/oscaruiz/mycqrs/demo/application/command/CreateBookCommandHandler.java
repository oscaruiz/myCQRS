package com.oscaruiz.mycqrs.demo.application.command;

import com.oscaruiz.mycqrs.core.domain.command.CommandHandler;
import com.oscaruiz.mycqrs.core.domain.event.EventBus;
import com.oscaruiz.mycqrs.core.infrastructure.spring.CommandHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;

@CommandHandlerComponent
public class CreateBookCommandHandler implements CommandHandler<CreateBookCommand, Void> {

    private final EventBus eventBus;

    public CreateBookCommandHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Void handle(CreateBookCommand command) {
        System.out.printf("📚 Creating book: '%s' by %s%n", command.getTitle(), command.getAuthor());

        // Publicar evento
        BookCreatedEvent event = new BookCreatedEvent(command.getTitle(), command.getAuthor());
        eventBus.publish(event);

        return null;
    }
}
