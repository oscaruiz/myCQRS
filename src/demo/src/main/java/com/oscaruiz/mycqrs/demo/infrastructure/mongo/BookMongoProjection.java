package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import com.oscaruiz.mycqrs.core.infrastructure.spring.EventHandlerComponent;
import com.oscaruiz.mycqrs.demo.domain.event.BookCreatedEvent;
import com.oscaruiz.mycqrs.core.domain.event.EventHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

// TODO - ESTA CLASE NUNCA LLEGA A REGISTRARSE, POR QUÉ
@EventHandlerComponent
public class BookMongoProjection implements EventHandler<BookCreatedEvent> {

    private final BookMongoRepository repository;

    public BookMongoProjection(BookMongoRepository repository) {
        this.repository = repository;
        System.out.println("✅ BookMongoProjection instanciado");
    }

    @Override
    public void handle(BookCreatedEvent event) {
        System.out.print("HANDLE BOOKMONGO");
        BookReadModel model = new BookReadModel(
                UUID.randomUUID(),
                event.getTitle(),
                event.getAuthor()
        );
        System.out.println("📥 Guardando en Mongo: " + event.getTitle());
        repository.save(model);
    }
}
