package com.jarl.seatforge.events.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PublishEventUseCase {
    PublishedEvent publish(UUID eventId);

    record PublishedEvent(
            UUID eventId, String name, Instant startsAt, BigDecimal price,
            String currency, int capacity, String status) {
    }
}
