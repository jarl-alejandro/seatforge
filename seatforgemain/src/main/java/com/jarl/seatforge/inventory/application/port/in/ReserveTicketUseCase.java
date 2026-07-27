package com.jarl.seatforge.inventory.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface ReserveTicketUseCase {
    ReservedTicket reserve(UUID ticketId, UUID idempotencyKey);

    record ReservedTicket(UUID reservationId, UUID ticketId, String status, Instant expiresAt) {
    }
}
