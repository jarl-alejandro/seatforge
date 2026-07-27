package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderAccessDeniedException;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderConflictException;
import com.jarl.seatforge.inventory.application.port.out.OrderReservationStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetReservationForOrderHandlerTest {
    private final UUID id = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();

    @Test
    void returns_an_active_owned_unexpired_reservation() {
        var handler = handler("buyer", "ACTIVE", Instant.parse("2030-01-01T00:01:00Z"));
        var result = handler.getActiveOwnedForUpdate(id, "buyer", Instant.parse("2030-01-01T00:00:00Z"));
        assertThat(result.ticketId()).isEqualTo(ticketId);
        assertThat(result.price()).isEqualByComparingTo("25.00");
    }

    @Test
    void denies_another_buyer_and_rejects_expiration_at_the_boundary() {
        var handler = handler("owner", "ACTIVE", Instant.parse("2030-01-01T00:00:00Z"));
        assertThatThrownBy(() -> handler.getActiveOwnedForUpdate(id, "other", Instant.EPOCH))
                .isInstanceOf(ReservationForOrderAccessDeniedException.class);
        assertThatThrownBy(() -> handler.getActiveOwnedForUpdate(id, "owner",
                Instant.parse("2030-01-01T00:00:00Z")))
                .isInstanceOf(ReservationForOrderConflictException.class);
    }

    private GetReservationForOrderHandler handler(String owner, String status, Instant expiresAt) {
        OrderReservationStore store = ignored -> Optional.of(new OrderReservationStore.StoredOrderReservation(
                id, ticketId, owner, status, expiresAt, new BigDecimal("25.00"), "USD"));
        return new GetReservationForOrderHandler(store);
    }
}
