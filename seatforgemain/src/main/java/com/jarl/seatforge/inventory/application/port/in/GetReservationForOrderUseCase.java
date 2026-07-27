package com.jarl.seatforge.inventory.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface GetReservationForOrderUseCase {
    OrderReservation getActiveOwnedForUpdate(UUID reservationId, String buyerId, Instant now);

    record OrderReservation(UUID reservationId, UUID ticketId, BigDecimal price, String currency) {
    }
}
