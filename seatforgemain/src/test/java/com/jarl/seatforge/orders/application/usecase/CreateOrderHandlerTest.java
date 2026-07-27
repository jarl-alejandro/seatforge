package com.jarl.seatforge.orders.application.usecase;

import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.ActorRole;
import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.GetReservationForOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.OrderIdempotencyConflictException;
import com.jarl.seatforge.orders.application.port.out.OrderStore;
import com.jarl.seatforge.orders.domain.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CreateOrderHandlerTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    private final CurrentActor actor = () -> new AuthenticatedActor(new ActorId("buyer"),
            Set.of(ActorRole.BUYER), Set.of("create:orders"));
    private final GetReservationForOrderUseCase reservations = mock(GetReservationForOrderUseCase.class);
    private final OrderStore orders = mock(OrderStore.class);
    private final CreateOrderHandler handler = new CreateOrderHandler(actor, reservations, orders,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void creates_a_pending_order_from_the_locked_reservation_snapshot() {
        UUID reservationId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        when(orders.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(orders.findByReservationId(reservationId)).thenReturn(Optional.empty());
        when(reservations.getActiveOwnedForUpdate(reservationId, "buyer", NOW)).thenReturn(
                new GetReservationForOrderUseCase.OrderReservation(reservationId, ticketId,
                        new BigDecimal("25.00"), "USD"));

        var result = handler.create(reservationId, key);

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.ticketId()).isEqualTo(ticketId);
        assertThat(result.createdAt()).isEqualTo(NOW);
        verify(orders).save(any(Order.class), eq(key), eq(CreateOrderHandler.requestHash(reservationId)));
    }

    @Test
    void replays_the_original_result_and_rejects_key_reuse_with_another_payload() {
        UUID reservationId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        var original = Order.pending(UUID.randomUUID(), reservationId, UUID.randomUUID(), "buyer",
                new BigDecimal("25.00"), "USD", NOW);
        when(orders.findByIdempotencyKey(key)).thenReturn(Optional.of(
                new OrderStore.StoredOrder(original, CreateOrderHandler.requestHash(reservationId))));

        assertThat(handler.create(reservationId, key).orderId()).isEqualTo(original.id());
        verifyNoInteractions(reservations);

        assertThatThrownBy(() -> handler.create(UUID.randomUUID(), key))
                .isInstanceOf(OrderIdempotencyConflictException.class);
    }
}
