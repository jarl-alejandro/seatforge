package com.jarl.seatforge.events.application.usecase;

import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import com.jarl.seatforge.events.application.port.out.EventIdGenerator;
import com.jarl.seatforge.events.application.port.out.EventStore;
import com.jarl.seatforge.events.domain.Event;
import com.jarl.seatforge.events.domain.Money;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.CreateEventInventory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class CreateEventHandler implements CreateEventUseCase {

    private final CurrentActor currentActor;
    private final EventStore eventStore;
    private final EventIdGenerator eventIdGenerator;
    private final CreateEventInventory createEventInventory;
    private final Clock clock;

    public CreateEventHandler(
            CurrentActor currentActor,
            EventStore eventStore,
            EventIdGenerator eventIdGenerator,
            CreateEventInventory createEventInventory,
            Clock clock
    ) {
        this.currentActor = currentActor;
        this.eventStore = eventStore;
        this.eventIdGenerator = eventIdGenerator;
        this.createEventInventory = createEventInventory;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedEvent create(CreateEventCommand command) {
        Money price = new Money(command.price(), command.currency());
        Event event = Event.draft(
                eventIdGenerator.nextId(),
                currentActor.get().id().value(),
                command.name(),
                command.startsAt(),
                price,
                command.capacity(),
                clock
        );

        eventStore.save(event);
        int ticketCount = createEventInventory.create(new CreateEventInventory.InventoryCommand(
                event.id(), event.price().amount(), event.price().currency(), event.capacity()));

        if (ticketCount != event.capacity()) {
            throw new IllegalStateException("inventory did not create the expected ticket count");
        }

        return new CreatedEvent(
                event.id(), event.name(), event.startsAt(), event.price().amount(),
                event.price().currency(), event.capacity(), event.status().name(), ticketCount);
    }
}
