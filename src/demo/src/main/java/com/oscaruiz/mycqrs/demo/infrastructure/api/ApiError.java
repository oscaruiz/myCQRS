package com.oscaruiz.mycqrs.demo.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        Instant timestamp,
        List<String> details
) {
    public ApiError(int status, String error, String message, Instant timestamp) {
        this(status, error, message, timestamp, null);
    }
}
