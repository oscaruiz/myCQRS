package com.oscaruiz.mycqrs.demo.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "books")
public class BookReadModel {

    @Id
    private String aggregateId;
    private String title;
    private String author;

    public BookReadModel() {
    }

    public BookReadModel(String aggregateId, String title, String author) {
        this.aggregateId = aggregateId;
        this.title = title;
        this.author = author;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
