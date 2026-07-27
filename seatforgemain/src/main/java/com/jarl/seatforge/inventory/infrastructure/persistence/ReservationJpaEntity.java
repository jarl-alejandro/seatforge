package com.jarl.seatforge.inventory.infrastructure.persistence;

import com.jarl.seatforge.inventory.domain.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations", uniqueConstraints = @UniqueConstraint(
        name = "uk_reservations_idempotency_key", columnNames = "idempotency_key"))
class ReservationJpaEntity {
    @Id
    @Column(name = "reservation_id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    UUID ticketId;

    @Column(name = "buyer_id", nullable = false, updatable = false)
    String buyerId;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    UUID idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    String requestHash;

    protected ReservationJpaEntity() {
    }

    ReservationJpaEntity(Reservation reservation, UUID idempotencyKey, String requestHash) {
        id = reservation.id();
        ticketId = reservation.ticketId();
        buyerId = reservation.buyerId();
        status = reservation.status().name();
        expiresAt = reservation.expiresAt();
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }
}
