package com.oscaruiz.mycqrs.demovanilla.order.application.query;

import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;

import java.util.Optional;

public class FindOrderQueryHandler implements QueryHandler<FindOrderQuery, Optional<OrderResponse>> {
    private final OrderReadModel readModel;

    public FindOrderQueryHandler(OrderReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public Optional<OrderResponse> handle(FindOrderQuery query) {
        return readModel.findById(query.orderId());
    }
}
