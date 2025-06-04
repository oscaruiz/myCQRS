package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.command.CommandHandler;
import com.oscaruiz.mycqrs.event.EventBus;
import com.oscaruiz.mycqrs.spring.CommandHandlerComponent;

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
