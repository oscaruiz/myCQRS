package com.oscaruiz.mycqrs.demo.book.application.query;

import java.util.List;

public record BookResponse(String id, String title, List<AuthorSummary> authors) {}
