package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.in.GetReservationForOrderUseCase;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderAccessDeniedException;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderConflictException;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderNotFoundException;
import com.jarl.seatforge.inventory.application.port.out.OrderReservationStore;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class GetReservationForOrderHandler implements GetReservationForOrderUseCase {
    private final OrderReservationStore store;

    public GetReservationForOrderHandler(OrderReservationStore store) {
        this.store = store;
    }

    @Override
    public OrderReservation getActiveOwnedForUpdate(UUID reservationId, String buyerId, Instant now) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        var reservation = store.findForUpdate(reservationId)
                .orElseThrow(ReservationForOrderNotFoundException::new);
        if (!reservation.buyerId().equals(buyerId)) {
            throw new ReservationForOrderAccessDeniedException();
        }
        if (!"ACTIVE".equals(reservation.status()) || !reservation.expiresAt().isAfter(now)) {
            throw new ReservationForOrderConflictException();
        }
        return new OrderReservation(reservation.reservationId(), reservation.ticketId(),
                reservation.price(), reservation.currency());
    }
}
