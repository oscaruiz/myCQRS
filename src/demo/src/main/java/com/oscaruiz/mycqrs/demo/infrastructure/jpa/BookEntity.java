package com.oscaruiz.mycqrs.demo.infrastructure.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private boolean deleted;

    protected BookEntity() {
        // for JPA
    }

    public BookEntity(String title, String author, boolean deleted) {
        this.title = title;
        this.author = author;
        this.deleted = deleted;
    }

    public void update(String title, String author, boolean deleted) {
        this.title = title;
        this.author = author;
        this.deleted = deleted;
    }

    public Long getId() {
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
