package com.jarl.seatforge.events.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Event(
        UUID id,
        String ownerId,
        String name,
        Instant startsAt,
        Money price,
        int capacity,
        EventStatus status
) {

    public static final int MAX_CAPACITY = 100_000;

    public Event {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(status, "status must not be null");
        name = name.trim();
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        if (name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("name length must be between 1 and 120");
        }
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity must be between 1 and 100000");
        }
    }

    public static Event draft(
            UUID id,
            String ownerId,
            String name,
            Instant startsAt,
            Money price,
            int capacity,
            Clock clock
    ) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (startsAt == null || !startsAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("startsAt must be in the future");
        }
        return new Event(id, ownerId, name, startsAt, price, capacity, EventStatus.DRAFT);
    }
}
