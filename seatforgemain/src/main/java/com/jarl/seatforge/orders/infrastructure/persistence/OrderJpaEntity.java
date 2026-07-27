package com.jarl.seatforge.orders.infrastructure.persistence;

import com.jarl.seatforge.orders.domain.Order;
import com.jarl.seatforge.orders.domain.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchase_orders_reservation", columnNames = "reservation_id"),
        @UniqueConstraint(name = "uk_purchase_orders_idempotency_key", columnNames = "idempotency_key")
})
class OrderJpaEntity {
    @Id @Column(name = "order_id", nullable = false, updatable = false) UUID id;
    @Column(name = "reservation_id", nullable = false, updatable = false) UUID reservationId;
    @Column(name = "ticket_id", nullable = false, updatable = false) UUID ticketId;
    @Column(name = "buyer_id", nullable = false, updatable = false) String buyerId;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2) BigDecimal totalAmount;
    @Column(nullable = false, length = 3) String currency;
    @Column(nullable = false, length = 20) String status;
    @Column(name = "created_at", nullable = false, updatable = false) Instant createdAt;
    @Column(name = "idempotency_key", nullable = false, updatable = false) UUID idempotencyKey;
    @Column(name = "request_hash", nullable = false, updatable = false, length = 64) String requestHash;

    protected OrderJpaEntity() {
    }

    OrderJpaEntity(Order order, UUID idempotencyKey, String requestHash) {
        id = order.id();
        reservationId = order.reservationId();
        ticketId = order.ticketId();
        buyerId = order.buyerId();
        totalAmount = order.totalAmount();
        currency = order.currency();
        status = order.status().name();
        createdAt = order.createdAt();
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    Order toDomain() {
        return new Order(id, reservationId, ticketId, buyerId, totalAmount, currency,
                OrderStatus.valueOf(status), createdAt);
    }
}
