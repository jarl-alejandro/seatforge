package com.jarl.seatforge.shared.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    void t01_domain_event_is_instantiated_and_tested_as_plain_java() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-25T12:00:00Z");

        DomainEvent domainEvent = new TestDomainEvent(eventId, occurredAt);

        assertThat(domainEvent.eventId()).isEqualTo(eventId);
        assertThat(domainEvent.occurredAt()).isEqualTo(occurredAt);
    }

    private record TestDomainEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
    }
}
