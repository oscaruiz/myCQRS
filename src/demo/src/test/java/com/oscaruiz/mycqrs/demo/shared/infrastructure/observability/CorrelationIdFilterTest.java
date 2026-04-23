package com.oscaruiz.mycqrs.demo.shared.infrastructure.observability;

import com.oscaruiz.mycqrs.core.infrastructure.observability.CorrelationIdMdc;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private static final Pattern UUID_REGEX = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void uses_incoming_header_when_valid_uuid() throws Exception {
        String incoming = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = capturingChain(captured);

        filter.doFilter(request, response, chain);

        assertThat(captured.get()).isEqualTo(incoming);
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(incoming);
    }

    @Test
    void generates_uuid_when_header_absent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = capturingChain(captured);

        filter.doFilter(request, response, chain);

        assertThat(captured.get()).isNotNull();
        assertThat(UUID_REGEX.matcher(captured.get()).matches()).isTrue();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(captured.get());
    }

    @Test
    void treats_blank_header_as_absent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = capturingChain(captured);

        filter.doFilter(request, response, chain);

        assertThat(captured.get()).isNotNull();
        assertThat(UUID_REGEX.matcher(captured.get()).matches()).isTrue();
    }

    @Test
    void generates_new_uuid_when_header_malformed() throws Exception {
        String malformed = "not-a-uuid";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, malformed);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = capturingChain(captured);

        filter.doFilter(request, response, chain);

        assertThat(captured.get())
            .as("MDC must carry a regenerated UUID, not the rejected header value")
            .isNotEqualTo(malformed);
        assertThat(UUID_REGEX.matcher(captured.get()).matches()).isTrue();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
            .as("echoed header must be a valid UUID, not the rejected input")
            .isNotEqualTo(malformed)
            .matches(UUID_REGEX);
    }

    @Test
    void rejects_malformed_header_with_crlf_and_regenerates_uuid() throws Exception {
        String injection = "abc\r\nINFO forged log entry";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, injection);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = capturingChain(captured);

        filter.doFilter(request, response, chain);

        assertThat(captured.get())
            .as("CRLF payload must never reach MDC")
            .doesNotContain("\r")
            .doesNotContain("\n")
            .matches(UUID_REGEX);
        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
            .as("CRLF payload must never reach the response header")
            .doesNotContain("\r")
            .doesNotContain("\n")
            .matches(UUID_REGEX);
    }

    @Test
    void clears_mdc_after_chain_and_echoes_header() throws Exception {
        String incoming = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(incoming);
        assertThat(MDC.get(CorrelationIdMdc.KEY)).isNull();
    }

    private FilterChain capturingChain(AtomicReference<String> target) {
        return new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                target.set(MDC.get(CorrelationIdMdc.KEY));
            }
        };
    }
}
