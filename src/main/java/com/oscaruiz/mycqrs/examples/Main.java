package com.oscaruiz.mycqrs.examples;

import com.oscaruiz.mycqrs.command.CommandInterceptor;
import com.oscaruiz.mycqrs.command.SimpleCommandBus;
import com.oscaruiz.mycqrs.event.SimpleEventBus;

public class Main {
    public static void main(String[] args) {
        var commandBus = new SimpleCommandBus();
        var eventBus = new SimpleEventBus();

        // 🔧 Interceptor de logging
        CommandInterceptor loggingInterceptor = (command, next) -> {
            System.out.println("🟡 Dispatching command: " + command.getClass().getSimpleName());
            Object result = next.invoke(command);
            System.out.println("🟢 Finished command: " + command.getClass().getSimpleName());
            return result;
        };

        commandBus.addInterceptor(loggingInterceptor);

        // 📩 Registrar handler de evento
        eventBus.registerHandler(BookCreatedEvent.class, new BookCreatedEventHandler());

        // 📤 Registrar handler de comando
        commandBus.registerHandler(CreateBookCommand.class, new CreateBookCommandHandler(eventBus));

        // 🧪 Enviar comando
        var command = new CreateBookCommand("Effective Java", "Joshua Bloch");
        commandBus.send(command);
    }
}
