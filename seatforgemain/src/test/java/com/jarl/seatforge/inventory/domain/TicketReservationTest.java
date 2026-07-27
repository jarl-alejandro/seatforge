package com.jarl.seatforge.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketReservationTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void reserves_an_available_ticket_for_the_buyer_using_the_injected_clock_and_ttl() {
        UUID ticketId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Ticket ticket = new Ticket(ticketId, UUID.randomUUID(), TicketStatus.AVAILABLE, true);

        Reservation reservation = ticket.reserve(
                reservationId, "buyer-1", Duration.ofMinutes(10), CLOCK);

        assertThat(ticket.status()).isEqualTo(TicketStatus.RESERVED);
        assertThat(reservation.id()).isEqualTo(reservationId);
        assertThat(reservation.ticketId()).isEqualTo(ticketId);
        assertThat(reservation.buyerId()).isEqualTo("buyer-1");
        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void rejects_reserved_and_sold_tickets_without_changing_their_state() {
        for (TicketStatus status : new TicketStatus[]{TicketStatus.RESERVED, TicketStatus.SOLD}) {
            Ticket ticket = new Ticket(UUID.randomUUID(), UUID.randomUUID(), status, true);

            assertThatThrownBy(() -> ticket.reserve(
                    UUID.randomUUID(), "buyer-1", Duration.ofMinutes(10), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("ticket is not available");
            assertThat(ticket.status()).isEqualTo(status);
        }
    }
}
