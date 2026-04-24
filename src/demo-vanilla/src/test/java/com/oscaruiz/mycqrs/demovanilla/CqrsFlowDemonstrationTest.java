package com.oscaruiz.mycqrs.demovanilla;

import com.oscaruiz.mycqrs.demovanilla.order.application.command.ConfirmOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.CreateOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.FindOrderQuery;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.OrderResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Narrated demonstration test. It does not cover edge cases (that is
 * VanillaBootstrapperTest); its value is that the output reads as a story
 * of the full CQRS flow: command → interceptor → handler → event →
 * projection → query.
 *
 * <p>Each phase prints a banner; between banners appear the DEBUG logs from
 * LoggingCommandInterceptor, SimpleEventBus, the ProjectionHandlers and LoggingQueryBus.
 */
class CqrsFlowDemonstrationTest {

    @Test
    void shouldDemonstrateFullCqrsFlow() {
        var bootstrapper = new VanillaBootstrapper();
        var commandBus = bootstrapper.commandBus();
        var queryBus = bootstrapper.queryBus();

        var orderId = UUID.randomUUID();

        banner("[1] Send CreateOrderCommand");
        commandBus.send(new CreateOrderCommand(UUID.randomUUID(), orderId, "Libro de DDD"));

        banner("[2] Query FindOrderQuery (expect PENDING)");
        Optional<OrderResponse> afterCreate = queryBus.handle(new FindOrderQuery(orderId));
        assertThat(afterCreate).isPresent();
        assertThat(afterCreate.get().status()).isEqualTo("PENDING");
        System.out.println("[ASSERT] status == PENDING");

        banner("[3] Send ConfirmOrderCommand");
        commandBus.send(new ConfirmOrderCommand(UUID.randomUUID(), orderId));

        banner("[4] Query FindOrderQuery (expect CONFIRMED)");
        Optional<OrderResponse> afterConfirm = queryBus.handle(new FindOrderQuery(orderId));
        assertThat(afterConfirm).isPresent();
        assertThat(afterConfirm.get().status()).isEqualTo("CONFIRMED");
        System.out.println("[ASSERT] status == CONFIRMED");

        banner("Flow complete");
    }

    private static void banner(String phase) {
        System.out.println();
        System.out.println("--- " + phase + " ---");
    }
}
