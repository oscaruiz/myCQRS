package com.oscaruiz.mycqrs.demovanilla;

import com.oscaruiz.mycqrs.demovanilla.order.application.command.ConfirmOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.command.CreateOrderCommand;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.FindOrderQuery;
import com.oscaruiz.mycqrs.demovanilla.order.application.query.OrderResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Executable program that demonstrates the full CQRS flow of demo-vanilla
 * without an HTTP server or JUnit. Designed for live demos and code
 * reading. Run with:
 *
 * <pre>
 * mvn -pl src/demo-vanilla exec:java -Dexec.mainClass=com.oscaruiz.mycqrs.demovanilla.Main
 * </pre>
 *
 * <p>Prints banners between phases; between banners appear the DEBUG logs
 * from interceptors, event bus and projections, so the output reads as
 * a narrative of the journey command → event → projection → query.
 */
public class Main {

    public static void main(String[] args) {
        var bootstrapper = new VanillaBootstrapper();
        var commandBus = bootstrapper.commandBus();
        var queryBus = bootstrapper.queryBus();

        var orderId = UUID.randomUUID();

        banner("[1] Send CreateOrderCommand");
        commandBus.send(new CreateOrderCommand(UUID.randomUUID(), orderId, "Libro de DDD"));

        banner("[2] Query FindOrderQuery (expect PENDING)");
        Optional<OrderResponse> afterCreate = queryBus.handle(new FindOrderQuery(orderId));

        banner("[3] Send ConfirmOrderCommand");
        commandBus.send(new ConfirmOrderCommand(UUID.randomUUID(), orderId));

        banner("[4] Query FindOrderQuery (expect CONFIRMED)");
        Optional<OrderResponse> afterConfirm = queryBus.handle(new FindOrderQuery(orderId));

        banner("Flow complete");
        System.out.println("Flow completed. Final state: ORDER " + orderId
            + " is " + afterConfirm.map(OrderResponse::status).orElse("<missing>") + ".");
    }

    private static void banner(String phase) {
        System.out.println();
        System.out.println("--- " + phase + " ---");
    }
}
