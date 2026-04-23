package com.oscaruiz.mycqrs.core.infrastructure.observability;

import com.oscaruiz.mycqrs.core.contracts.command.Command;
import com.oscaruiz.mycqrs.core.contracts.command.CommandInterceptor.CommandHandlerInvoker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdCommandInterceptorTest {

    private static final Pattern UUID_REGEX = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private final CorrelationIdCommandInterceptor interceptor = new CorrelationIdCommandInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generates_uuid_when_mdc_empty() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        AtomicReference<String> captured = new AtomicReference<>();
        CommandHandlerInvoker next = cmd -> captured.set(MDC.get(CorrelationIdMdc.KEY));

        interceptor.intercept(command, next);

        assertThat(captured.get()).isNotNull();
        assertThat(UUID_REGEX.matcher(captured.get()).matches()).isTrue();
        assertThat(MDC.get(CorrelationIdMdc.KEY)).isNull();
    }

    @Test
    void propagates_existing_mdc_value() {
        String existing = "outer-scope-id-123";
        MDC.put(CorrelationIdMdc.KEY, existing);
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        AtomicReference<String> captured = new AtomicReference<>();
        CommandHandlerInvoker next = cmd -> captured.set(MDC.get(CorrelationIdMdc.KEY));

        interceptor.intercept(command, next);

        assertThat(captured.get()).isEqualTo(existing);
        assertThat(MDC.get(CorrelationIdMdc.KEY)).isEqualTo(existing);
    }

    @Test
    void clears_mdc_when_it_set_it() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = cmd -> { /* no-op */ };

        interceptor.intercept(command, next);

        assertThat(MDC.get(CorrelationIdMdc.KEY)).isNull();
    }

    @Test
    void does_not_clear_when_value_was_pre_existing() {
        String existing = "already-here";
        MDC.put(CorrelationIdMdc.KEY, existing);
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = cmd -> { /* no-op */ };

        interceptor.intercept(command, next);

        assertThat(MDC.get(CorrelationIdMdc.KEY)).isEqualTo(existing);
    }

    @Test
    void clears_mdc_even_when_chain_throws() {
        FakeCommand command = new FakeCommand(UUID.randomUUID());
        CommandHandlerInvoker next = cmd -> { throw new RuntimeException("boom"); };

        assertThatThrownBy(() -> interceptor.intercept(command, next))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        assertThat(MDC.get(CorrelationIdMdc.KEY)).isNull();
    }

    record FakeCommand(UUID commandId) implements Command {}
}
