package com.jarl.seatforge.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(
        name = "uk_tickets_event_number", columnNames = {"event_id", "ticket_number"}))
class TicketJpaEntity {

    @Id
    @Column(name = "ticket_id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    UUID eventId;

    @Column(name = "ticket_number", nullable = false, updatable = false)
    int number;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "price_amount", nullable = false, precision = 19, scale = 2)
    BigDecimal priceAmount;

    @Column(nullable = false, length = 3)
    String currency;

    protected TicketJpaEntity() {
    }

    TicketJpaEntity(UUID id, UUID eventId, int number, BigDecimal priceAmount, String currency) {
        this.id = id;
        this.eventId = eventId;
        this.number = number;
        this.status = "AVAILABLE";
        this.priceAmount = priceAmount;
        this.currency = currency;
    }
}
