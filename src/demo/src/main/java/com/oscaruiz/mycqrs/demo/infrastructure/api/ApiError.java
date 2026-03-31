package com.oscaruiz.mycqrs.demo.infrastructure.api;

import java.time.Instant;

public record ApiError(
        int status,
        String error,
        String message,
        Instant timestamp
) {}
