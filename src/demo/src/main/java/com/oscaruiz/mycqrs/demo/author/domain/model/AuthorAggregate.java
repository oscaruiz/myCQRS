package com.oscaruiz.mycqrs.demo.author.domain.model;

import com.oscaruiz.mycqrs.core.ddd.AggregateRoot;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorCreatedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorDeletedEvent;
import com.oscaruiz.mycqrs.demo.author.domain.event.AuthorRenamedEvent;

import java.time.Year;
import java.util.UUID;

public class AuthorAggregate extends AggregateRoot<String> {

    private final String id;
    private String firstName;
    private String lastName;
    private Integer birthYear;
    private boolean deleted;

    private AuthorAggregate(String id, String firstName, String lastName, Integer birthYear, boolean deleted) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
        this.deleted = deleted;
    }

    public static AuthorAggregate create(String id, String firstName, String lastName, Integer birthYear) {
        requireNonBlank(id, "id");
        requireNonBlank(firstName, "firstName");
        requireNonBlank(lastName, "lastName");
        UUID.fromString(id);
        validateBirthYear(birthYear);

        AuthorAggregate aggregate = new AuthorAggregate(id, firstName, lastName, birthYear, false);
        aggregate.recordEvent(new AuthorCreatedEvent(id, firstName, lastName, birthYear));

        return aggregate;
    }

    public static AuthorAggregate rehydrate(String id, String firstName, String lastName,
                                            Integer birthYear, boolean deleted) {
        return new AuthorAggregate(id, firstName, lastName, birthYear, deleted);
    }

    public void rename(String firstName, String lastName) {
        if (deleted) {
            throw new IllegalStateException("Cannot rename a deleted author");
        }

        requireNonBlank(firstName, "firstName");
        requireNonBlank(lastName, "lastName");

        if (this.firstName.equals(firstName) && this.lastName.equals(lastName)) {
            return;
        }

        this.firstName = firstName;
        this.lastName = lastName;

        recordEvent(new AuthorRenamedEvent(id, this.firstName, this.lastName));
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("Author is already deleted");
        }

        deleted = true;
        recordEvent(new AuthorDeletedEvent(id));
    }

    @Override
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

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }

    private static void validateBirthYear(Integer birthYear) {
        if (birthYear == null) {
            return;
        }
        int currentYear = Year.now().getValue();
        if (birthYear <= 0 || birthYear > currentYear) {
            throw new IllegalArgumentException("birthYear must be between 1 and " + currentYear);
        }
    }
}
