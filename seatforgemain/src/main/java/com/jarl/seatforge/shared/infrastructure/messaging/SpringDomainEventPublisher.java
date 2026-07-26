package com.jarl.seatforge.shared.infrastructure.messaging;

import com.jarl.seatforge.shared.application.port.out.DomainEventPublisher;
import com.jarl.seatforge.shared.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Objects;

/** Dispatches domain events through Spring's in-process event mechanism. */
public final class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = Objects.requireNonNull(
                applicationEventPublisher,
                "applicationEventPublisher must not be null"
        );
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(Objects.requireNonNull(event, "event must not be null"));
    }
}
