package com.jarl.seatforge.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Reservation(
        UUID id,
        UUID ticketId,
        String buyerId,
        ReservationStatus status,
        Instant expiresAt
) {
    public Reservation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (buyerId.isBlank()) {
            throw new IllegalArgumentException("buyerId must not be blank");
        }
    }
}
