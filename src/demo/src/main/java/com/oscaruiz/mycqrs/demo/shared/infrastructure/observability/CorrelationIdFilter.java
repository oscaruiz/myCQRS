package com.oscaruiz.mycqrs.demo.shared.infrastructure.observability;

import com.oscaruiz.mycqrs.core.infrastructure.observability.CorrelationIdMdc;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that seeds the correlation-id MDC key for the lifetime of an HTTP request.
 *
 * <p>Reads {@code X-Correlation-ID} from the incoming request. If absent, blank, or not a
 * valid {@link UUID}, generates a fresh UUID. The UUID restriction is deliberate: the value
 * ends up in log lines via {@code %X{correlationId:-}} and in the {@code correlation_id}
 * column of the outbox, so accepting arbitrary client input opens CWE-117 (CRLF log
 * injection) and allows unbounded strings. Internal contracts treat the correlation id as a
 * UUID end-to-end.
 *
 * <p>Echoes the final value back in the {@code X-Correlation-ID} response header so the
 * client can log it on its side. Always clears MDC in {@code finally} so request threads
 * returned to the pool do not carry a stale id.
 */
public final class CorrelationIdFilter implements Filter {

    public static final String HEADER = "X-Correlation-ID";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = resolveCorrelationId(httpRequest.getHeader(HEADER));

        MDC.put(CorrelationIdMdc.KEY, correlationId);
        httpResponse.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIdMdc.KEY);
        }
    }

    private String resolveCorrelationId(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(incoming.trim()).toString();
        } catch (IllegalArgumentException e) {
            log.debug("Rejected malformed X-Correlation-ID header; generating new UUID");
            return UUID.randomUUID().toString();
        }
    }
}
