package com.jarl.seatforge.orders.application.port.out;

import com.jarl.seatforge.orders.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderStore {
    void lockIdempotencyKey(UUID idempotencyKey);
    Optional<StoredOrder> findByIdempotencyKey(UUID idempotencyKey);
    Optional<StoredOrder> findByReservationId(UUID reservationId);
    Optional<StoredOrder> findByIdAndBuyerId(UUID orderId, String buyerId);
    void save(Order order, UUID idempotencyKey, String requestHash);

    record StoredOrder(Order order, String requestHash) {
    }
}
