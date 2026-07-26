package com.jarl.seatforge.events.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
class EventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "owner_id", nullable = false)
    String ownerId;

    @Column(nullable = false, length = 120)
    String name;

    @Column(name = "starts_at", nullable = false)
    Instant startsAt;

    @Column(name = "price_amount", nullable = false, precision = 19, scale = 2)
    BigDecimal priceAmount;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    com.jarl.seatforge.events.domain.EventStatus status;

    protected EventJpaEntity() {
    }

    EventJpaEntity(com.jarl.seatforge.events.domain.Event event) {
        id = event.id();
        ownerId = event.ownerId();
        name = event.name();
        startsAt = event.startsAt();
        priceAmount = event.price().amount();
        currency = event.price().currency();
        capacity = event.capacity();
        status = event.status();
    }

    com.jarl.seatforge.events.domain.Event toDomain() {
        return new com.jarl.seatforge.events.domain.Event(
                id, ownerId, name, startsAt,
                new com.jarl.seatforge.events.domain.Money(priceAmount, currency), capacity, status);
    }
}
