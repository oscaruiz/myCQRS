package com.oscaruiz.mycqrs.demo.author.infrastructure.mongo;

import com.oscaruiz.mycqrs.demo.author.application.query.AuthorResponse;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "authors")
public class AuthorReadModel {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private Integer birthYear;
    private boolean deleted;
    private List<AuthorResponse.BookSummary> books = new ArrayList<>();

    public AuthorReadModel() {
    }

    public AuthorReadModel(String id, String firstName, String lastName, Integer birthYear,
                           boolean deleted, List<AuthorResponse.BookSummary> books) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
        this.deleted = deleted;
        this.books = books != null ? new ArrayList<>(books) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public List<AuthorResponse.BookSummary> getBooks() {
        return books;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setBooks(List<AuthorResponse.BookSummary> books) {
        this.books = books;
    }
}
