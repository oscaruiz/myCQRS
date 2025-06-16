package com.oscaruiz.mycqrs.demo.infastructure.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BookEntity {

    @Id
    private String title;

    private String author;

    public BookEntity() {
    }

    public BookEntity(String title, String author) {
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
