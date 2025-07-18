package com.oscaruiz.mycqrs.demo.infrastructure.api;

public class CreateBookRequest {
    private String title;
    private String author;

    public String getTitle() { return title; }
    public String getAuthor() { return author; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
}
