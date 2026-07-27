package com.jarl.seatforge.inventory.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrderReservationStore {
    Optional<StoredOrderReservation> findForUpdate(UUID reservationId);

    record StoredOrderReservation(UUID reservationId, UUID ticketId, String buyerId,
                                  String status, Instant expiresAt,
                                  BigDecimal price, String currency) {
    }
}
