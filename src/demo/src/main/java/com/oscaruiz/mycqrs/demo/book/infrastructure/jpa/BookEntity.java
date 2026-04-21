package com.oscaruiz.mycqrs.demo.book.infrastructure.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Version;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class BookEntity {

    @Id
    private UUID id;

    private String title;
    private boolean deleted;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id")
    )
    @Column(name = "author_id")
    private Set<UUID> authorIds = new HashSet<>();

    protected BookEntity() {
        // for JPA
    }

    public BookEntity(UUID id, String title, boolean deleted) {
        this.id = id;
        this.title = title;
        this.deleted = deleted;
    }

    public void update(String title, boolean deleted) {
        this.title = title;
        this.deleted = deleted;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Set<UUID> getAuthorIds() {
        return authorIds;
    }

    public void replaceAuthorIds(Set<UUID> authorIds) {
        this.authorIds.clear();
        this.authorIds.addAll(authorIds);
    }
}
