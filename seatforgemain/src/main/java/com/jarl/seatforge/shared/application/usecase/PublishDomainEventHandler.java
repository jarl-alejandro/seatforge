package com.jarl.seatforge.shared.application.usecase;

import com.jarl.seatforge.shared.application.port.in.PublishDomainEventUseCase;
import com.jarl.seatforge.shared.application.port.out.DomainEventPublisher;
import com.jarl.seatforge.shared.domain.event.DomainEvent;

import java.util.Objects;

/** Framework-independent implementation of domain-event publication. */
public final class PublishDomainEventHandler implements PublishDomainEventUseCase {

    private final DomainEventPublisher eventPublisher;

    public PublishDomainEventHandler(DomainEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publish(Objects.requireNonNull(event, "event must not be null"));
    }
}
