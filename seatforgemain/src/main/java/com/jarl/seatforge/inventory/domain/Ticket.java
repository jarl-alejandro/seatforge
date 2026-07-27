package com.jarl.seatforge.inventory.domain;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class Ticket {
    private final UUID id;
    private final UUID eventId;
    private final boolean eventPublished;
    private TicketStatus status;

    public Ticket(UUID id, UUID eventId, TicketStatus status, boolean eventPublished) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.eventPublished = eventPublished;
    }

    public Reservation reserve(UUID reservationId, String buyerId, Duration ttl, Clock clock) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("reservation ttl must be positive");
        }
        if (status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException("ticket is not available");
        }
        if (!eventPublished) {
            throw new IllegalStateException("ticket event is not published");
        }
        status = TicketStatus.RESERVED;
        return new Reservation(reservationId, id, buyerId, ReservationStatus.ACTIVE,
                clock.instant().plus(ttl));
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public TicketStatus status() {
        return status;
    }
}
