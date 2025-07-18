package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "books")
public class BookReadModel {

    @Id
    private UUID id;
    private String title;
    private String author;

    public BookReadModel() {
    }

    public BookReadModel( UUID id, String title, String author) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.author = author;
    }


    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
