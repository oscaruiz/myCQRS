package com.oscaruiz.mycqrs.core.infrastructure.bus.event;

import com.oscaruiz.mycqrs.core.contracts.event.Event;
import com.oscaruiz.mycqrs.core.contracts.event.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleEventBusTest {

    private SimpleEventBus bus;

    @BeforeEach
    void setUp() {
        bus = new SimpleEventBus();
    }

    @Test
    void shouldPublishEventToRegisteredHandler() {
        RecordingEventHandler handler = new RecordingEventHandler();
        bus.registerHandler(FakeEvent.class, handler);

        FakeEvent event = new FakeEvent();
        bus.publish(event);

        assertTrue(handler.wasCalled);
        assertSame(event, handler.receivedEvent);
    }

    @Test
    void shouldPublishEventToMultipleHandlersInRegistrationOrder() {
        List<String> callOrder = new ArrayList<>();
        EventHandler<FakeEvent> first = e -> callOrder.add("first");
        EventHandler<FakeEvent> second = e -> callOrder.add("second");

        bus.registerHandler(FakeEvent.class, first);
        bus.registerHandler(FakeEvent.class, second);

        bus.publish(new FakeEvent());

        assertEquals(List.of("first", "second"), callOrder);
    }

    @Test
    void shouldNotThrow_whenNoHandlersRegistered() {
        assertDoesNotThrow(() -> bus.publish(new FakeEvent()));
    }

    @Test
    void shouldInvokeRemainingHandlers_whenOneHandlerThrows() {
        RecordingEventHandler first = new RecordingEventHandler();
        RecordingEventHandler third = new RecordingEventHandler();

        bus.registerHandler(FakeEvent.class, first);
        bus.registerHandler(FakeEvent.class, new FailingEventHandler());
        bus.registerHandler(FakeEvent.class, third);

        assertDoesNotThrow(() -> bus.publish(new FakeEvent()));

        assertTrue(first.wasCalled);
        assertTrue(third.wasCalled);
    }

    @Test
    void shouldNotInvokeHandler_whenEventTypeDoesNotMatch() {
        RecordingEventHandler handler = new RecordingEventHandler();
        bus.registerHandler(FakeEvent.class, handler);

        bus.publish(new AnotherFakeEvent());

        assertFalse(handler.wasCalled);
    }

    static class FakeEvent implements Event {

    }

    static class AnotherFakeEvent implements Event {

    }

    static class RecordingEventHandler implements EventHandler<FakeEvent> {
        boolean wasCalled = false;
        FakeEvent receivedEvent = null;

        @Override
        public void handle(FakeEvent event) {
            wasCalled = true;
            receivedEvent = event;
        }
    }

    static class FailingEventHandler implements EventHandler<FakeEvent> {
        @Override
        public void handle(FakeEvent event) {
            throw new RuntimeException("boom");
        }
    }
}
