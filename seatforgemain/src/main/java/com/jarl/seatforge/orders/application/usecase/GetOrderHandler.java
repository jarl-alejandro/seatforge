package com.jarl.seatforge.orders.application.usecase;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.orders.application.port.in.CreateOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.GetOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.OrderNotFoundException;
import com.jarl.seatforge.orders.application.port.out.OrderStore;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public class GetOrderHandler implements GetOrderUseCase {
    private final CurrentActor currentActor;
    private final OrderStore orders;

    public GetOrderHandler(CurrentActor currentActor, OrderStore orders) {
        this.currentActor = currentActor;
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public CreateOrderUseCase.OrderResult get(UUID orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        String buyerId = currentActor.get().id().value();
        return orders.findByIdAndBuyerId(orderId, buyerId)
                .map(OrderStore.StoredOrder::order)
                .map(CreateOrderHandler::result)
                .orElseThrow(OrderNotFoundException::new);
    }
}
