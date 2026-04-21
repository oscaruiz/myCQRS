package com.oscaruiz.mycqrs.demo.author.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "author_entity")
public class AuthorEntity {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(nullable = false)
    private boolean deleted;

    @Version
    private Long version;

    protected AuthorEntity() {
        // for JPA
    }

    public AuthorEntity(UUID id, String firstName, String lastName, Integer birthYear, boolean deleted) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
        this.deleted = deleted;
    }

    public void update(String firstName, String lastName, Integer birthYear, boolean deleted) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
        this.deleted = deleted;
    }

    public UUID getId() {
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
}
