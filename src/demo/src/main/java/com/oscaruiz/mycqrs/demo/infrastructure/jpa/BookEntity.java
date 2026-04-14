package com.oscaruiz.mycqrs.demo.infrastructure.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class BookEntity {

    @Id
    private UUID id;

    private String title;
    private String author;
    private boolean deleted;

    protected BookEntity() {
        // for JPA
    }

    public BookEntity(UUID id, String title, String author, boolean deleted) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.deleted = deleted;
    }

    public void update(String title, String author, boolean deleted) {
        this.title = title;
        this.author = author;
        this.deleted = deleted;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
