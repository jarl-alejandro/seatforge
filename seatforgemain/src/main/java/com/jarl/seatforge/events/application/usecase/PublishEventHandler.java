package com.jarl.seatforge.events.application.usecase;

import com.jarl.seatforge.events.application.port.in.EventNotFoundException;
import com.jarl.seatforge.events.application.port.in.EventPublicationConflictException;
import com.jarl.seatforge.events.application.port.in.PublishEventUseCase;
import com.jarl.seatforge.events.application.port.out.EventStore;
import com.jarl.seatforge.events.domain.Event;
import com.jarl.seatforge.identity.application.port.in.ActorAccessDeniedException;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.QueryEventInventory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class PublishEventHandler implements PublishEventUseCase {
    private final CurrentActor currentActor;
    private final EventStore eventStore;
    private final QueryEventInventory inventory;
    private final Clock clock;

    public PublishEventHandler(CurrentActor currentActor, EventStore eventStore,
                               QueryEventInventory inventory, Clock clock) {
        this.currentActor = currentActor;
        this.eventStore = eventStore;
        this.inventory = inventory;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublishedEvent publish(UUID eventId) {
        Event event = eventStore.findById(eventId).orElseThrow(EventNotFoundException::new);
        if (!event.ownerId().equals(currentActor.get().id().value())) {
            throw new ActorAccessDeniedException();
        }

        Event published;
        try {
            published = event.publish(inventory.hasAvailableTickets(eventId), clock);
        } catch (IllegalStateException exception) {
            throw new EventPublicationConflictException(exception.getMessage());
        }
        if (published != event) {
            eventStore.save(published);
        }
        return toResult(published);
    }

    private static PublishedEvent toResult(Event event) {
        return new PublishedEvent(event.id(), event.name(), event.startsAt(),
                event.price().amount(), event.price().currency(), event.capacity(), event.status().name());
    }
}
