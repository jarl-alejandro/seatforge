package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.ReservationIdempotencyConflictException;
import com.jarl.seatforge.inventory.application.port.in.ReserveTicketUseCase;
import com.jarl.seatforge.inventory.application.port.in.TicketNotFoundException;
import com.jarl.seatforge.inventory.application.port.in.TicketReservationConflictException;
import com.jarl.seatforge.inventory.application.port.out.ReservationStore;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public class ReserveTicketHandler implements ReserveTicketUseCase {
    private final CurrentActor currentActor;
    private final ReservationStore store;
    private final Clock clock;
    private final Duration ttl;

    public ReserveTicketHandler(CurrentActor currentActor, ReservationStore store,
                                Clock clock, Duration ttl) {
        this.currentActor = currentActor;
        this.store = store;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    @Transactional
    public ReservedTicket reserve(UUID ticketId, UUID idempotencyKey) {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        String buyerId = currentActor.get().id().value();
        String requestHash = requestHash(ticketId);

        store.lockIdempotencyKey(idempotencyKey);
        var previous = store.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            var reservation = previous.orElseThrow();
            if (!reservation.requestHash().equals(requestHash)
                    || !reservation.buyerId().equals(buyerId)) {
                throw new ReservationIdempotencyConflictException();
            }
            return new ReservedTicket(reservation.reservationId(), reservation.ticketId(),
                    "ACTIVE", reservation.expiresAt());
        }

        var ticket = store.findTicketForUpdate(ticketId).orElseThrow(TicketNotFoundException::new);
        final com.jarl.seatforge.inventory.domain.Reservation reservation;
        try {
            reservation = ticket.reserve(UUID.randomUUID(), buyerId, ttl, clock);
        } catch (IllegalStateException exception) {
            if (exception.getMessage().contains("not published")) {
                throw new TicketNotFoundException();
            }
            throw new TicketReservationConflictException(exception.getMessage());
        }
        store.save(ticket, reservation, idempotencyKey, requestHash);
        return new ReservedTicket(reservation.id(), reservation.ticketId(),
                reservation.status().name(), reservation.expiresAt());
    }

    static String requestHash(UUID ticketId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(ticketId.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
