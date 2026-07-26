package com.jarl.seatforge.shared.application.port.out;

import com.jarl.seatforge.shared.domain.event.DomainEvent;

/** Output boundary for the mechanism that dispatches business facts. */
@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
