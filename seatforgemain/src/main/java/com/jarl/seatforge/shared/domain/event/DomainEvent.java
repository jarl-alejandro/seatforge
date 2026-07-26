package com.jarl.seatforge.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A business fact raised by a SeatForge module.
 *
 * <p>Concrete events belong to the module that owns the fact. This shared contract only
 * supplies the metadata required to publish them consistently.</p>
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}
