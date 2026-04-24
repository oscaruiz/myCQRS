package com.oscaruiz.mycqrs.demovanilla;

import com.oscaruiz.mycqrs.demovanilla.order.application.command.ConfirmOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.CreateOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.FindOrderQuery;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.OrderResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VanillaBootstrapperTest {

    @Test
    void shouldCreateAndConfirmOrderEndToEnd() {
        var bootstrapper = new VanillaBootstrapper();
        var commandBus = bootstrapper.commandBus();
        var queryBus = bootstrapper.queryBus();

        var orderId = UUID.randomUUID();

        commandBus.send(new CreateOrderCommand(UUID.randomUUID(), orderId, "Libro de DDD"));

        Optional<OrderResponse> afterCreate = queryBus.handle(new FindOrderQuery(orderId));
        assertThat(afterCreate).isPresent();
        assertThat(afterCreate.get().status()).isEqualTo("PENDING");

        commandBus.send(new ConfirmOrderCommand(UUID.randomUUID(), orderId));

        Optional<OrderResponse> afterConfirm = queryBus.handle(new FindOrderQuery(orderId));
        assertThat(afterConfirm).isPresent();
        assertThat(afterConfirm.get().status()).isEqualTo("CONFIRMED");
    }

    @Test
    void shouldReturnEmptyWhenOrderDoesNotExist() {
        var bootstrapper = new VanillaBootstrapper();
        Optional<OrderResponse> result = bootstrapper.queryBus().handle(new FindOrderQuery(UUID.randomUUID()));
        assertThat(result).isEmpty();
    }
}
