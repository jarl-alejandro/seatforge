package com.jarl.seatforge.shared.application.port.in;

import com.jarl.seatforge.shared.domain.event.DomainEvent;

/** Publishes a business fact to interested modules inside the monolith. */
@FunctionalInterface
public interface PublishDomainEventUseCase {

    void publish(DomainEvent event);
}
