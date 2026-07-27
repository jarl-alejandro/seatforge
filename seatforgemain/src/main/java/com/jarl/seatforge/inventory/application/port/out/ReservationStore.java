package com.jarl.seatforge.inventory.application.port.out;

import com.jarl.seatforge.inventory.domain.Reservation;
import com.jarl.seatforge.inventory.domain.Ticket;

import java.util.Optional;
import java.util.UUID;

public interface ReservationStore {
    void lockIdempotencyKey(UUID idempotencyKey);

    Optional<StoredReservation> findByIdempotencyKey(UUID idempotencyKey);

    Optional<Ticket> findTicketForUpdate(UUID ticketId);

    void save(Ticket ticket, Reservation reservation, UUID idempotencyKey, String requestHash);

    record StoredReservation(UUID reservationId, UUID ticketId, String buyerId, String status,
                             java.time.Instant expiresAt, String requestHash) {
    }
}
