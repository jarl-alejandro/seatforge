package com.jarl.seatforge.events.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void creates_a_draft_when_all_invariants_hold() {
        Event event = Event.draft(
                UUID.randomUUID(), "organizer@clients", "Concert", NOW.plusSeconds(60),
                new Money(new BigDecimal("25.00"), "USD"), 100, CLOCK);

        assertThat(event.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.capacity()).isEqualTo(100);
    }

    @Test
    void rejects_a_start_date_that_is_not_strictly_in_the_future() {
        assertThatThrownBy(() -> draft(NOW, 1, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startsAt must be in the future");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 100001})
    void rejects_capacity_outside_the_supported_range(int capacity) {
        assertThatThrownBy(() -> draft(NOW.plusSeconds(1), capacity, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("capacity must be between 1 and 100000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "-100"})
    void rejects_a_non_positive_price(String amount) {
        assertThatThrownBy(() -> draft(NOW.plusSeconds(1), 1, new BigDecimal(amount)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price must be positive");
    }

    @Test
    void rejects_fractional_cents() {
        assertThatThrownBy(() -> draft(NOW.plusSeconds(1), 1, new BigDecimal("1.001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price must have at most two decimal places");
    }

    @Test
    void publishes_a_future_draft_with_available_inventory() {
        Event published = draft(NOW.plusSeconds(60), 1, new BigDecimal("1.00"))
                .publish(true, CLOCK);

        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void publishing_an_already_published_event_is_idempotent() {
        Event published = draft(NOW.plusSeconds(60), 1, new BigDecimal("1.00"))
                .publish(true, CLOCK);

        assertThat(published.publish(false, CLOCK)).isSameAs(published);
    }

    @Test
    void rejects_publication_without_available_inventory() {
        assertThatThrownBy(() -> draft(NOW.plusSeconds(60), 1, new BigDecimal("1.00"))
                .publish(false, CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("event inventory must contain available tickets");
    }

    @Test
    void rejects_publication_when_the_event_is_no_longer_in_the_future() {
        Event draft = new Event(UUID.randomUUID(), "organizer@clients", "Concert", NOW,
                new Money(new BigDecimal("1.00"), "USD"), 1, EventStatus.DRAFT);

        assertThatThrownBy(() -> draft.publish(true, CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("only future events can be published");
    }

    private static Event draft(Instant startsAt, int capacity, BigDecimal amount) {
        return Event.draft(
                UUID.randomUUID(), "organizer@clients", "Concert", startsAt,
                new Money(amount, "USD"), capacity, CLOCK);
    }
}
