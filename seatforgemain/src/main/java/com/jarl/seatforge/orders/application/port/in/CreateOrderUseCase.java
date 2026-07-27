package com.jarl.seatforge.orders.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface CreateOrderUseCase {
    OrderResult create(UUID reservationId, UUID idempotencyKey);

    record OrderResult(UUID orderId, UUID reservationId, UUID ticketId,
                       BigDecimal totalAmount, String currency, String status,
                       Instant createdAt) {
    }
}
