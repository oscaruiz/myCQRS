package com.oscaruiz.mycqrs.demo.author.application.query;

import java.util.Optional;

public interface AuthorReadModelRepository {
    Optional<AuthorResponse> findById(String id);
}
