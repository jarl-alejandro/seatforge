package com.jarl.seatforge.orders.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Order(UUID id, UUID reservationId, UUID ticketId, String buyerId,
                    BigDecimal totalAmount, String currency, OrderStatus status,
                    Instant createdAt) {
    public Order {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (buyerId.isBlank()) throw new IllegalArgumentException("buyerId must not be blank");
        if (totalAmount.signum() <= 0 || totalAmount.scale() > 2) {
            throw new IllegalArgumentException("total must be positive with at most two decimals");
        }
        if (!"USD".equals(currency)) throw new IllegalArgumentException("currency must be USD");
        totalAmount = totalAmount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Order pending(UUID id, UUID reservationId, UUID ticketId, String buyerId,
                                BigDecimal totalAmount, String currency, Instant createdAt) {
        return new Order(id, reservationId, ticketId, buyerId, totalAmount, currency,
                OrderStatus.PENDING, createdAt);
    }
}
