package com.oscaruiz.mycqrs.demo.book.infrastructure.mongo;

import com.oscaruiz.mycqrs.demo.book.application.query.AuthorSummary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "books")
public class BookReadModel {

    @Id
    private String aggregateId;
    private String title;
    private List<AuthorSummary> authors = new ArrayList<>();

    public BookReadModel() {
    }

    public BookReadModel(String aggregateId, String title, List<AuthorSummary> authors) {
        this.aggregateId = aggregateId;
        this.title = title;
        this.authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTitle() {
        return title;
    }

    public List<AuthorSummary> getAuthors() {
        return authors;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthors(List<AuthorSummary> authors) {
        this.authors = authors;
    }
}
