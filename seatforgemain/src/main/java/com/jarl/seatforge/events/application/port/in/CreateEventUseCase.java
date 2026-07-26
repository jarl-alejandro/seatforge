package com.jarl.seatforge.events.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface CreateEventUseCase {

    CreatedEvent create(CreateEventCommand command);

    record CreateEventCommand(String name, Instant startsAt, BigDecimal price, String currency, int capacity) {
    }

    record CreatedEvent(
            UUID eventId,
            String name,
            Instant startsAt,
            BigDecimal price,
            String currency,
            int capacity,
            String status,
            int createdTicketCount
    ) {
    }
}
