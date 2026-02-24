package com.oscaruiz.mycqrs.demo.domain.model;

public class BookAggregate {

    private Long id;
    private String title;
    private String author;

    public BookAggregate(Long id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public static BookAggregate create(String title, String author) {
        return new BookAggregate(null, title, author);
    }

    public static BookAggregate rehydrate(Long id, String title, String author) {
        return new BookAggregate(id, title, author);
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public void updateIfPresent(String title, String author) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (author != null && !author.isBlank()) {
            this.author = author;
        }
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
}
