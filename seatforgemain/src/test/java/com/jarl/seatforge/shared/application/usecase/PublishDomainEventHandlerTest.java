package com.jarl.seatforge.shared.application.usecase;

import com.jarl.seatforge.shared.application.port.out.DomainEventPublisher;
import com.jarl.seatforge.shared.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublishDomainEventHandlerTest {

    @Test
    void t03_use_case_works_with_a_fake_output_port() {
        InMemoryDomainEventPublisher fakePublisher = new InMemoryDomainEventPublisher();
        PublishDomainEventHandler handler = new PublishDomainEventHandler(fakePublisher);
        DomainEvent event = new TestDomainEvent(UUID.randomUUID(), Instant.parse("2026-07-25T12:00:00Z"));

        handler.publish(event);

        assertThat(fakePublisher.publishedEvents).containsExactly(event);
    }

    private static final class InMemoryDomainEventPublisher implements DomainEventPublisher {
        private final List<DomainEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            publishedEvents.add(event);
        }
    }

    private record TestDomainEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
    }
}
