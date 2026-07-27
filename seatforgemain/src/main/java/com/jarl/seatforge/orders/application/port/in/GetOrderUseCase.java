package com.jarl.seatforge.orders.application.port.in;

import java.util.UUID;

public interface GetOrderUseCase {
    CreateOrderUseCase.OrderResult get(UUID orderId);
}
